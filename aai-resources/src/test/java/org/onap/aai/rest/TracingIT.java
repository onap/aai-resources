package org.onap.aai.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.propagation.Propagator;
import io.opentelemetry.sdk.trace.SdkTracerProvider;

import java.lang.management.ManagementFactory;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.onap.aai.ResourcesApp;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.restclient.RestTemplateBuilder;
import org.springframework.context.ConfigurableApplicationContext;

public class TracingIT {
    private static final String TRACE_ID = "463ac35c9f6413ad48485a3953bb6124";
    private static final String PARENT_ID = "a2fb4a1d1a96d312";

    @BeforeAll
    static void setupConfig() {
        System.setProperty("AJSC_HOME", "./");
        System.setProperty("BUNDLECONFIG_DIR", "src/main/resources/");
    }

    @Test
    void enabledTracingExportsZipkinAndPropagatesW3cAndB3() throws Exception {
        assertFalse(ManagementFactory.getRuntimeMXBean().getInputArguments().stream()
                .anyMatch(argument -> argument.startsWith("-javaagent:") && argument.contains("opentelemetry")));
        BlockingQueue<Map<String, String>> headers = new LinkedBlockingQueue<>();
        BlockingQueue<String> exports = new LinkedBlockingQueue<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/request", exchange -> {
            headers.add(Map.of("traceparent", String.valueOf(exchange.getRequestHeaders().getFirst("traceparent")),
                    "b3", String.valueOf(exchange.getRequestHeaders().getFirst("b3"))));
            byte[] body = "ok".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            try (var output = exchange.getResponseBody()) {
                output.write(body);
            }
        });
        server.createContext("/api/v2/spans", exchange -> {
            try (var input = "gzip".equals(exchange.getRequestHeaders().getFirst("Content-Encoding"))
                    ? new GZIPInputStream(exchange.getRequestBody()) : exchange.getRequestBody()) {
                exports.add(new String(input.readAllBytes(), StandardCharsets.UTF_8));
            }
            exchange.sendResponseHeaders(202, -1);
            exchange.close();
        });
        server.start();
        String address = "http://127.0.0.1:" + server.getAddress().getPort();
        try (ConfigurableApplicationContext context = application(
                "--management.tracing.enabled=true", "--management.zipkin.tracing.endpoint=" + address + "/api/v2/spans")) {
            Tracer tracer = context.getBeanProvider(Tracer.class).getIfAvailable(() -> Tracer.NOOP);
            Propagator propagator = context.getBeanProvider(Propagator.class).getIfAvailable();
            Span span = propagator == null ? tracer.nextSpan().name("resources-tracing-test").start()
                    : propagator.extract(Map.of("traceparent", "00-" + TRACE_ID + "-" + PARENT_ID + "-01"), Map::get)
                        .name("resources-tracing-test").start();
            try (Tracer.SpanInScope scope = tracer.withSpan(span)) {
                assertEquals("ok", context.getBean(RestTemplateBuilder.class).build()
                        .getForObject(address + "/request", String.class));
            } finally {
                span.end();
            }
            Map<String, String> received = headers.poll(5, TimeUnit.SECONDS);
            assertNotNull(received);
            assertTrue(received.get("traceparent").startsWith("00-" + TRACE_ID + "-"), received.toString());
            assertTrue(received.get("b3").startsWith(TRACE_ID + "-"), received.toString());
            assertEquals(1, context.getBeansOfType(Tracer.class).size());
            assertEquals(1, context.getBeansOfType(SdkTracerProvider.class).size());
            assertTrue(context.getBean(SdkTracerProvider.class).forceFlush().join(10, TimeUnit.SECONDS).isSuccess());
            String exported = exports.poll(10, TimeUnit.SECONDS);
            assertNotNull(exported, "No Zipkin request reached the configured loopback endpoint");
            ObjectMapper mapper = context.getBean(ObjectMapper.class);
            boolean parentExported = false;
            boolean clientExported = false;
            do {
                for (JsonNode exportedSpan : mapper.readTree(exported)) {
                    if (TRACE_ID.equals(exportedSpan.path("traceId").asText())) {
                        parentExported |= "resources-tracing-test".equals(exportedSpan.path("name").asText())
                                && PARENT_ID.equals(exportedSpan.path("parentId").asText());
                        clientExported |= "CLIENT".equals(exportedSpan.path("kind").asText());
                    }
                }
                exported = exports.poll();
            } while (exported != null);
            assertTrue(parentExported, "The extracted W3C parent must be retained in the exported span");
            assertTrue(clientExported, "The instrumented HTTP client must export a span in the same trace");
        } finally {
            server.stop(0);
        }
    }

    @Test
    void defaultTracingDoesNotCreateSdkOrExportOrTraceHeaders() throws Exception {
        BlockingQueue<Map<String, String>> headers = new LinkedBlockingQueue<>();
        BlockingQueue<String> exports = new LinkedBlockingQueue<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/request", exchange -> {
            headers.add(Map.of("traceparent", String.valueOf(exchange.getRequestHeaders().getFirst("traceparent")),
                    "b3", String.valueOf(exchange.getRequestHeaders().getFirst("b3"))));
            byte[] body = "ok".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            try (var output = exchange.getResponseBody()) {
                output.write(body);
            }
        });
        server.createContext("/api/v2/spans", exchange -> {
            exports.add(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            exchange.sendResponseHeaders(202, -1);
            exchange.close();
        });
        server.start();
        String address = "http://127.0.0.1:" + server.getAddress().getPort();
        try (ConfigurableApplicationContext context = application(
                "--management.zipkin.tracing.endpoint=" + address + "/api/v2/spans")) {
            assertEquals("false", context.getEnvironment().getProperty("management.tracing.enabled"));
            assertEquals("false", context.getEnvironment().getProperty("management.opentelemetry.enabled"));
            assertEquals("false", context.getEnvironment().getProperty("management.tracing.export.enabled"));
            assertTrue(context.getBeansOfType(SdkTracerProvider.class).isEmpty());
            assertTrue(context.getBeansOfType(io.opentelemetry.sdk.trace.export.SpanExporter.class).isEmpty());
            Tracer tracer = context.getBeanProvider(Tracer.class).getIfAvailable(() -> Tracer.NOOP);
            Span span = tracer.nextSpan().name("resources-tracing-disabled-test").start();
            try (Tracer.SpanInScope scope = tracer.withSpan(span)) {
                assertEquals("ok", context.getBean(RestTemplateBuilder.class).build()
                        .getForObject(address + "/request", String.class));
            } finally {
                span.end();
            }
            assertEquals(Map.of("traceparent", "null", "b3", "null"), headers.poll(5, TimeUnit.SECONDS));
        } finally {
            server.stop(0);
        }
        assertTrue(exports.isEmpty());
    }

    private ConfigurableApplicationContext application(String... tracingProperties) {
        String[] properties = new String[] {
                "--server.address=127.0.0.1", "--server.port=0", "--server.ssl.enabled=false",
                "--management.server.address=127.0.0.1", "--management.server.port=0",
                "--spring.profiles.active=production", "--aai.notifications.enabled=false",
                "--aai.graph.properties.path=src/test/resources/etc/appprops/janusgraph-realtime.properties",
                "--schema.ingest.file=src/test/resources/application-test.properties"
        };
        String[] arguments = java.util.Arrays.copyOf(properties, properties.length + tracingProperties.length);
        System.arraycopy(tracingProperties, 0, arguments, properties.length, tracingProperties.length);
        return new SpringApplicationBuilder(ResourcesApp.class).run(arguments);
    }
}
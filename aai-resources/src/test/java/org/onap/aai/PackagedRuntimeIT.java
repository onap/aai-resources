/*
 * ============LICENSE_START=======================================================
 * org.onap.aai
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ============LICENSE_END=========================================================
 */

package org.onap.aai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarFile;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PackagedRuntimeIT {

    @TempDir
    Path temporaryDirectory;

    private Path packagedJar() throws Exception {
        try (var builds = Files.list(Path.of("target"))) {
            Path build = builds.filter(path -> path.getFileName().toString().endsWith("-build"))
                    .findFirst().orElseThrow();
            try (var jars = Files.list(build.resolve("lib"))) {
                return jars.filter(path -> path.toString().endsWith(".jar")).findFirst().orElseThrow().toAbsolutePath();
            }
        }
    }

    @Test
    void graphClientsLoadFromShippedLibraries() throws Exception {
        List<URL> libraries = new ArrayList<>();
        try (JarFile jar = new JarFile(packagedJar().toFile())) {
            assertEquals("org.onap.aai.ResourcesApp", jar.getManifest().getMainAttributes().getValue("Start-Class"));
            for (var entry : jar.stream().filter(entry -> entry.getName().startsWith("BOOT-INF/lib/")
                    && entry.getName().endsWith(".jar")).toList()) {
                Path library = temporaryDirectory.resolve(Path.of(entry.getName()).getFileName());
                try (var input = jar.getInputStream(entry)) {
                    Files.copy(input, library);
                }
                libraries.add(library.toUri().toURL());
            }
        }
        try (URLClassLoader loader = new URLClassLoader(libraries.toArray(URL[]::new), ClassLoader.getPlatformClassLoader())) {
            ClassLoader originalLoader = Thread.currentThread().getContextClassLoader();
            Thread.currentThread().setContextClassLoader(loader);
            try {
                loader.loadClass("io.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory");
                Class<?> clusterType = loader.loadClass("org.apache.tinkerpop.gremlin.driver.Cluster");
                Object builder = clusterType.getMethod("build").invoke(null);
                Object cluster = builder.getClass().getMethod("create").invoke(builder);
                clusterType.getMethod("close").invoke(cluster);
                loader.loadClass("com.datastax.oss.driver.api.core.CqlSession").getMethod("builder").invoke(null);
            } finally {
                Thread.currentThread().setContextClassLoader(originalLoader);
            }
        }
    }

    @Test
    void packagedManagementAndJerseyKeepTheirHttpContracts() throws Exception {
        Path log = Path.of("target", "packaged-runtime.log").toAbsolutePath();
        Process process = new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin", "java").toString(),
                "-DAJSC_HOME=.", "-DBUNDLECONFIG_DIR=src/main/resources", "-jar", packagedJar().toString(),
                "--server.address=127.0.0.1", "--server.port=0", "--server.ssl.enabled=false",
                "--management.server.address=127.0.0.1", "--management.server.port=0",
                "--logging.level.org.springframework.boot.tomcat=INFO",
                "--spring.profiles.active=production", "--aai.notifications.enabled=false",
                "--aai.graph.properties.path=src/test/resources/etc/appprops/janusgraph-realtime.properties",
                "--schema.ingest.file=src/test/resources/application-test.properties")
                .redirectErrorStream(true).start();
        CompletableFuture<List<Integer>> started = new CompletableFuture<>();
        Thread outputReader = new Thread(() -> {
            List<Integer> ports = new ArrayList<>();
            Pattern portPattern = Pattern.compile("Tomcat started on port (\\d+)");
            try (BufferedReader input = new BufferedReader(new InputStreamReader(process.getInputStream()));
                    BufferedWriter output = Files.newBufferedWriter(log)) {
                String line;
                while ((line = input.readLine()) != null) {
                    output.write(line);
                    output.newLine();
                    output.flush();
                    var matcher = portPattern.matcher(line);
                    if (matcher.find() && !ports.contains(Integer.valueOf(matcher.group(1)))) {
                        ports.add(Integer.valueOf(matcher.group(1)));
                    }
                    if (ports.size() == 2 && line.contains("Resources MicroService Started")) {
                        started.complete(List.copyOf(ports));
                    }
                }
                if (!started.isDone()) {
                    started.completeExceptionally(new IllegalStateException("Packaged startup failed; see " + log));
                }
            } catch (Exception exception) {
                started.completeExceptionally(exception);
            }
        }, "packaged-runtime-output");
        outputReader.setDaemon(true);
        outputReader.start();
        try {
            List<Integer> ports = started.get(90, TimeUnit.SECONDS);
            assertTrue(!ports.get(0).equals(ports.get(1)));
            HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
            HttpResponse<String> health = get(client, ports.get(1), "/actuator/health", "application/json", null);
            assertEquals(200, health.statusCode(), health.body());
            assertTrue(health.body().contains("UP"), health.body());
            HttpResponse<String> metrics = get(client, ports.get(1), "/actuator/prometheus", "text/plain", null);
            assertEquals(200, metrics.statusCode(), metrics.body());
            assertTrue(metrics.body().contains("group_id"), metrics.body());
            assertEquals(200, get(client, ports.get(0), "/aai/util/echo", "application/json", null).statusCode());
            String missingPserver = "/aai/v32/cloud-infrastructure/pservers/pserver/packaged-runtime-missing";
            HttpResponse<String> denied = get(client, ports.get(0), missingPserver, "application/json", null);
            assertEquals(403, denied.statusCode(), denied.body());
            assertTrue(denied.body().contains("3300"), denied.body());
            String authorization = "Basic " + Base64.getEncoder().encodeToString("AAI:AAI".getBytes(java.nio.charset.StandardCharsets.UTF_8));
            assertEquals(404, get(client, ports.get(0), missingPserver, "application/json", authorization).statusCode());
        } finally {
            process.destroy();
            if (!process.waitFor(15, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                assertTrue(process.waitFor(15, TimeUnit.SECONDS));
            }
            outputReader.join(5000);
        }
    }

    private HttpResponse<String> get(HttpClient client, int port, String path, String accept, String authorization)
            throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
                .timeout(Duration.ofSeconds(15)).header("Accept", accept)
                .header("X-FromAppId", "JUNIT").header("X-TransactionId", "packaged-runtime");
        if (authorization != null) {
            request.header("Authorization", authorization);
        }
        return client.send(request.GET().build(), HttpResponse.BodyHandlers.ofString());
    }
}
/**
 * ============LICENSE_START=======================================================
 * org.onap.aai
 * ================================================================================
 * Copyright © 2024 Deutsche Telekom. All rights reserved.
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

package org.onap.aai.it.performance;

import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.apache.commons.collections.IteratorUtils;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.janusgraph.core.JanusGraph;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.onap.aai.ResourcesApp;
import org.onap.aai.db.props.AAIProperties;
import org.onap.aai.dbmap.AAIGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.output.WaitingConsumer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.k6.K6Container;
import org.testcontainers.utility.MountableFile;

import lombok.SneakyThrows;

// import org.testcontainers.utility.MountableFile;

@Testcontainers
// @Import(WebClientConfiguration.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
public class K6PerformanceTest {

  private static final Logger logger = LoggerFactory.getLogger(ResourcesApp.class.getName());

  @LocalServerPort
  private int port;

  private boolean initialized = false;

  // @Autowired
  // WebTestClient webClient;

  // @BeforeAll
  // public static void setAjscHome() {
  //   System.setProperty("AJSC_HOME", ".");
  //   System.setProperty("BUNDLECONFIG_DIR", "aai-resources/src/main/resources");
  // }

  @BeforeEach
  public void setup() {
    if (!initialized) {
      initialized = true;
      AAIGraph.getInstance();

      // webClient = WebTestClient.bindToServer()
      // .baseUrl("http://localhost:" + port)
      // .responseTimeout(Duration.ofSeconds(300))
      // .defaultHeaders(headers -> {
      // headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
      // headers.set("X-FromAppId", "test");
      // headers.set("X-TransactionId", "someTransaction");
      // headers.setBasicAuth("AAI", "AAI");
      // })
      // .build();

      long startTime = System.currentTimeMillis();
      logger.info("Creating pserver nodes");
      loadPerformanceTestData();
      // loadPerformanceTestDataFromSnapshot();
      long endTime = System.currentTimeMillis();
      logger.info("Created pserver nodes in {} seconds", (endTime - startTime) / 1000);
    }
  }

  @AfterAll
  public static void cleanup() {
    JanusGraph graph = AAIGraph.getInstance().getGraph();
    graph.traversal().V().has("aai-node-type", "pserver").drop().iterate();
    graph.tx().commit();
  }

  @Test
  public void k6StandardTest() throws Exception {
    int testDuration = 5;

    try (
        K6Container container = new K6Container("grafana/k6:0.49.0")
            .withNetworkMode("host")
            .withAccessToHost(true)
            .withTestScript(MountableFile.forClasspathResource("k6/test.js"))
            .withScriptVar("API_PORT", String.valueOf(port))
            .withScriptVar("API_VERSION", "v29")
            .withScriptVar("DURATION_SECONDS", String.valueOf(testDuration))
            .withCmdOptions("--quiet", "--no-usage-report");) {
      container.start();

      WaitingConsumer consumer = new WaitingConsumer();
      container.followOutput(consumer);

      // Wait for test script results to be collected
      consumer.waitUntil(
          frame -> {
            return frame.getUtf8String().contains("iteration_duration");
          },
          testDuration + 5,
          TimeUnit.SECONDS);

      // assertEquals("k6 tests are cool!"container.getLogs());
      // assertThat(, null);
      // assertTrue();
      String sms = "";
      logger.debug(container.getLogs());
      assertTrue(container.getLogs().contains("smth"));
    }
    String sms = "";
  }

  @SneakyThrows
  public static void loadPerformanceTestData() {
    JanusGraph graph = AAIGraph.getInstance().getGraph();
    GraphTraversalSource g = graph.traversal();
    long n = 1000;
    for (long i = 0; i < n; i++) {
      createPServer(g, i);
    }
    graph.tx().commit();
  }

  private static void createPServer(GraphTraversalSource g, long i) {
    String hostname = "hostname" + i;
    String uri = "/cloud-infrastructure/pservers/pserver/" + hostname;
    g.addV()
        .property("aai-node-type", "pserver")
        .property("hostname", hostname)
        .property("resource-version", UUID.randomUUID().toString())
        .property(AAIProperties.AAI_URI, uri)
        .next();
  }

    // @Container
  // K6Container container = new K6Container("grafana/k6:0.49.0")
  // .withTestScript(MountableFile.forClasspathResource("k6/test.js"))
  // .withScriptVar("API_PORT", String.valueOf(port))
  // .withCmdOptions("--quiet", "--no-usage-report");

  // @Test
  // void smth() throws UnsupportedOperationException, IOException,
  // InterruptedException {
  // ExecResult execResult = container.execInContainer("k6", "run",
  // "/scripts/test.js");
  // int exitCode = execResult.getExitCode();
  // assertTrue(true);
  // }
}

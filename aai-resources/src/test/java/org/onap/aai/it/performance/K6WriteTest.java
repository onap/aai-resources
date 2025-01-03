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

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.janusgraph.core.JanusGraph;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.onap.aai.db.props.AAIProperties;
import org.onap.aai.dbmap.AAIGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.cassandra.CassandraAutoConfiguration;
import org.springframework.boot.autoconfigure.data.cassandra.CassandraDataAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.testcontainers.containers.output.WaitingConsumer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.k6.K6Container;
import org.testcontainers.utility.MountableFile;

import lombok.SneakyThrows;

@Testcontainers
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@EnableAutoConfiguration(exclude={CassandraDataAutoConfiguration.class, CassandraAutoConfiguration.class}) // there is no running cassandra instance for the test
public class K6WriteTest {

  private static final Logger logger = LoggerFactory.getLogger(K6ReadTest.class);
  private static final long nPservers = 10;

  @LocalServerPort
  private int port;

  @BeforeEach
  public void setup() {
    if (!AAIGraph.isInit()) {
      AAIGraph.getInstance();

      long startTime = System.currentTimeMillis();
      logger.info("Creating pserver nodes");
      loadPerformanceTestData();
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
  @Disabled
  public void writeWithoutRelations() throws Exception {
    int testDuration = 5;

    K6Container container = new K6Container("grafana/k6:0.49.0")
        .withNetworkMode("host")
        .withAccessToHost(true)
        .withTestScript(MountableFile.forClasspathResource("k6/writeWithoutRelations.js"))
        .withScriptVar("API_PORT", String.valueOf(port))
        .withScriptVar("API_VERSION", "v29")
        .withScriptVar("DURATION_SECONDS", String.valueOf(testDuration))
        .withScriptVar("N_PSERVERS", String.valueOf(nPservers))
        .withCmdOptions("--quiet", "--no-usage-report");
    container.start();

    WaitingConsumer consumer = new WaitingConsumer();
    container.followOutput(consumer);

    // Wait for test script results to be collected
    try {
      consumer.waitUntil(
          frame -> {
            return frame.getUtf8String().contains("iteration_duration");
          },
          testDuration + 10,
          TimeUnit.SECONDS);
    } catch (Exception e) {
      // log the container stdout in case of failure in the test script
      logger.error(container.getLogs());
    }

    String report = container.getLogs().substring(container.getLogs().indexOf("✓ status was 201"));
    logger.info(report);
    assertThat(report, containsString("✓ status was 201"));
    assertThat(report, containsString("✓ http_req_duration"));
    assertThat(report, containsString("✓ http_req_failed"));
    container.stop();
  }

  @SneakyThrows
  public static void loadPerformanceTestData() {
    JanusGraph graph = AAIGraph.getInstance().getGraph();
    GraphTraversalSource g = graph.traversal();
    long n = nPservers;
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
}

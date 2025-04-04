/**
 * ============LICENSE_START=======================================================
 * org.onap.aai
 * ================================================================================
 * Copyright © 2024 Deutsche Telekom AG Intellectual Property. All rights reserved.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ============LICENSE_END=========================================================
 */

package org.onap.aai.rest.resources;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.List;

import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.janusgraph.core.JanusGraph;
import org.janusgraph.core.JanusGraphTransaction;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.onap.aai.config.WebClientConfiguration;
import org.onap.aai.dbmap.AAIGraph;
import org.onap.aai.entities.AAIErrorResponse;
import org.onap.aai.entities.ServiceException;
import org.onap.aai.exceptions.AAIException;
import org.onap.aai.rest.db.HttpEntry;
import org.onap.aai.setup.SchemaVersions;
import org.onap.aai.util.AAIConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;

import org.springframework.http.HttpMethod;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.reactive.server.WebTestClient;

@DirtiesContext
// This currently has to be executed last, since the @MockBean is dirtying the context.
// Restarting the context leads to other test failures that would need to be investigated.
@Order(Integer.MAX_VALUE)
@Import(WebClientConfiguration.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
public class ResourcesControllerMockTest {

  @MockBean(name = "traversalUriHttpEntry")
  HttpEntry mockHttpEntry;

  @Autowired
  WebTestClient webClient;

  @Autowired
  SchemaVersions schemaVersions;

  @ParameterizedTest
  @CsvSource({
      "GET, /cloud-infrastructure/pservers/pserver/someHostname",
      "PUT, /cloud-infrastructure/pservers/pserver/someHostname",
      "PUT, /cloud-infrastructure/pservers/pserver/someHostname/relationship-list/relationship",
      "DELETE, /cloud-infrastructure/pservers/pserver/someHostname",
      "DELETE, /cloud-infrastructure/pservers/pserver/someHostname/relationship-list/relationship",
  })
  public void thatInternalServerErrorsAreMappedToAAIErrorResponse(String method, String uri) {
      // assure that any exception is mapped to an AAIErrorResponse
      when(mockHttpEntry.setHttpEntryProperties(any())).thenThrow(new IllegalArgumentException());
      when(mockHttpEntry.setHttpEntryProperties(any(), anyString())).thenThrow(new IllegalArgumentException());

      AAIErrorResponse errorResponse = webClient
          .method(HttpMethod.valueOf(method))
          .uri(uri)
          .bodyValue("{}")
          .exchange()
          .expectStatus().isEqualTo(500)
          .returnResult(AAIErrorResponse.class)
          .getResponseBody()
          .blockFirst();

      ServiceException serviceException = errorResponse.getRequestError().getServiceException();
      assertEquals("SVC3002", serviceException.getMessageId());
      assertEquals("Error writing output performing %1 on %2 (msg=%3) (ec=%4)", serviceException.getText());
      List<String> expected = List.of(
        method.toString(),
        schemaVersions.getDefaultVersion() + uri,
        "Internal Error:java.lang.IllegalArgumentException",
        "ERR.5.4.4000");
      assertIterableEquals(expected, serviceException.getVariables());
  }

  @BeforeEach
  public void setup() throws AAIException {
      if(!AAIGraph.isInit()) {
          AAIConfig.init();
          AAIGraph.getInstance();
      }
  }

  @AfterEach
  public void tearDown() {
      JanusGraph janusGraph = AAIGraph.getInstance().getGraph();
      JanusGraphTransaction transaction = janusGraph.newTransaction();
      boolean success = true;
      try {
          GraphTraversalSource g = transaction.traversal();
          g.V().drop().iterate();
      } catch (Exception ex) {
          success = false;
      } finally {
          if (success) {
              transaction.commit();
          } else {
              transaction.rollback();
          }
      }
  }
}

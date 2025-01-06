/**
 * ============LICENSE_START=======================================================
 * org.onap.aai
 * ================================================================================
 * Copyright © 2025 Deutsche Telekom. All rights reserved.
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

package org.onap.aai.rest;

import java.time.Duration;
import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.onap.aai.config.AuthProperties;
import org.onap.aai.config.WebClientConfiguration;
import org.onap.aai.setup.SchemaVersions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Import(WebClientConfiguration.class)
public class AuthenticationTest {

  @Autowired AuthProperties authProperties;
  @Autowired SchemaVersions schemaVersions;
  @LocalServerPort int port;

  WebTestClient webClient;

  @BeforeEach
  void setup() {
    webClient = WebTestClient.bindToServer()
      .baseUrl("http://localhost:" + port + "/aai/" + schemaVersions.getDefaultVersion())
      .responseTimeout(Duration.ofSeconds(300))
      .defaultHeaders(headers -> {
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.set("X-FromAppId", "test");
        headers.set("X-TransactionId", "someTransaction");
      })
      .build();
  }

  @Test
  void thatServiceIsAuthenticated() {
      webClient.get()
          .uri("/cloud-infrastructure/pservers/pserver/pserver-hostname-test")
          .exchange()
          .expectStatus()
          .isForbidden();

      webClient.get()
          .uri("/cloud-infrastructure/pservers/pserver/pserver-hostname-test")
          .headers(headers -> headers.setBasicAuth(authProperties.getUsers().get(0).getUsername(), authProperties.getUsers().get(0).getPassword()))
          .exchange()
          .expectStatus()
          .isNotFound();
  }

}

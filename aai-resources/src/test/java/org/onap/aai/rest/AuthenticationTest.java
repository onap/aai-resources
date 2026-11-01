/**
 * ============LICENSE_START=======================================================
 * org.onap.aai
 * ================================================================================
 * Copyright © 2025 Deutsche Telekom. All rights reserved.
 * Modifications Copyright (C) 2026 Deutsche Telekom AG.
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

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.onap.aai.config.AuthProperties;
import org.onap.aai.setup.SchemaVersions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, properties = {
    "aai.basic-auth.enabled=true",
    "management.server.port=0"
})
public class AuthenticationTest {

  private static final String PSERVER_URI = "/cloud-infrastructure/pservers/pserver/pserver-hostname-test";

  @Autowired AuthProperties authProperties;
  @Autowired SchemaVersions schemaVersions;
  @LocalServerPort int port;
  @Value("${local.management.port}") int managementPort;

  WebTestClient webClient;

  @BeforeEach
  void setup() {
    webClient = WebTestClient.bindToServer()
      .baseUrl("http://localhost:" + port + "/aai")
      .responseTimeout(Duration.ofSeconds(300))
      .defaultHeaders(headers -> {
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.set("X-FromAppId", "test");
        headers.set("X-TransactionId", "someTransaction");
      })
      .build();
  }

  @Test
  void thatRequestWithoutCredentialsIsRejected() {
    webClient.get()
        .uri(versioned(PSERVER_URI))
        .exchange()
        .expectStatus().isForbidden()
        .expectBody(String.class).value(body -> assertThat(body).contains("3300"));
  }

  @Test
  void thatRequestWithWrongPasswordIsRejected() {
    webClient.get()
        .uri(versioned(PSERVER_URI))
        .headers(headers -> headers.setBasicAuth(authProperties.getUsers().get(0).getUsername(), "wrong-password"))
        .exchange()
        .expectStatus().isForbidden()
        .expectBody(String.class).value(body -> assertThat(body).contains("3300"));
  }

  @Test
  void thatRequestWithUnknownUserIsRejected() {
    webClient.get()
        .uri(versioned(PSERVER_URI))
        .headers(headers -> headers.setBasicAuth("unknown-user", authProperties.getUsers().get(0).getPassword()))
        .exchange()
        .expectStatus().isForbidden();
  }

  @Test
  void thatNonBasicAuthorizationIsRejected() {
    webClient.get()
        .uri(versioned(PSERVER_URI))
        .header(HttpHeaders.AUTHORIZATION, "Bearer some-token")
        .exchange()
        .expectStatus().isForbidden();
  }

  @Test
  void thatRequestWithValidCredentialsIsAccepted() {
    webClient.get()
        .uri(versioned(PSERVER_URI))
        .headers(headers -> headers.setBasicAuth(authProperties.getUsers().get(0).getUsername(), authProperties.getUsers().get(0).getPassword()))
        .exchange()
        .expectStatus().isNotFound();
  }

  @Test
  void thatEchoIsAccessibleWithoutCredentials() {
    webClient.get()
        .uri("/util/echo")
        .exchange()
        .expectStatus().isOk();
  }

  @Test
  void thatActuatorIsAccessibleWithoutCredentials() {
    WebTestClient.bindToServer()
        .baseUrl("http://localhost:" + managementPort)
        .build()
        .get()
        .uri("/actuator/health")
        .exchange()
        .expectStatus().isOk();
  }

  private String versioned(String uri) {
    return "/" + schemaVersions.getDefaultVersion() + uri;
  }
}

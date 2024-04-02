/**
 * ============LICENSE_START==================================================
 * org.onap.aai
 * ===========================================================================
 * Copyright © 2017-2020 AT&T Intellectual Property. All rights reserved.
 * ===========================================================================
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
 * ============LICENSE_END====================================================
 */

package org.onap.aai.it.multitenancy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import dasniko.testcontainers.keycloak.KeycloakContainer;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.representations.AccessTokenResponse;
import org.onap.aai.PayloadUtil;
import org.onap.aai.rest.AbstractSpringRestTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

@Import(KeycloakTestConfiguration.class)
@TestPropertySource(locations = "classpath:it/application-keycloak-test.properties")
public class MultiTenancyIT extends AbstractSpringRestTest {

    @Autowired
    private KeycloakContainer keycloakContainer;
    @Autowired
    private RoleHandler roleHandler;
    @Autowired
    private KeycloakTestProperties properties;

    @Test
    public void testCreateAndGetPnf() throws Exception {
        baseUrl = "http://localhost:" + randomPort;
        String endpoint = baseUrl + "/aai/v23/network/pnfs/pnf/pnf-1";
        ResponseEntity<String> responseEntity = null;

        // create pnf with ran (operator)
        String username = "ran", password = "ran";
        headers = this.getHeaders(username, password);
        httpEntity = new HttpEntity<String>(PayloadUtil.getResourcePayload("pnf.json"), headers);
        responseEntity = restTemplate.exchange(endpoint, HttpMethod.PUT, httpEntity, String.class);
        assertEquals(HttpStatus.CREATED, responseEntity.getStatusCode());

        // get pnf with bob (operator_readOnly)
        username = "bob";
        password = "bob";
        headers = this.getHeaders(username, password);
        httpEntity = new HttpEntity<String>("", headers);
        responseEntity = restTemplate.exchange(endpoint, HttpMethod.GET, httpEntity, String.class);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());

        // get pnf with ted (selector)
        username = "ted";
        password = "ted";
        headers = this.getHeaders(username, password);
        httpEntity = new HttpEntity<String>("", headers);
        responseEntity = restTemplate.exchange(endpoint, HttpMethod.GET, httpEntity, String.class);
        assertEquals(HttpStatus.FORBIDDEN, responseEntity.getStatusCode());

        // add role to ted and try to get pnf again
        roleHandler.addToUser(RoleHandler.OPERATOR_READ_ONLY, username);
        headers = this.getHeaders(username, password);
        httpEntity = new HttpEntity<String>("", headers);
        responseEntity = restTemplate.exchange(endpoint, HttpMethod.GET, httpEntity, String.class);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());

        // get pnf with ran
        username = "ran";
        password = "ran";
        headers = this.getHeaders(username, password);
        httpEntity = new HttpEntity<String>("", headers);
        responseEntity = restTemplate.exchange(endpoint, HttpMethod.GET, httpEntity, String.class);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    }

    private HttpHeaders getHeaders(String username, String password) {
        HttpHeaders headers = new HttpHeaders();

        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.add("Real-Time", "true");
        headers.add("X-FromAppId", "JUNIT");
        headers.add("X-TransactionId", "JUNIT");
        headers.add("Authorization", "Bearer " + getStringToken(username, password));

        return headers;
    }

    private String getStringToken(String username, String password) {
        Keycloak keycloakClient = KeycloakBuilder.builder().serverUrl(keycloakContainer.getAuthServerUrl())
                .realm(properties.realm).clientId(properties.clientId).clientSecret(properties.clientSecret)
                .username(username).password(password).build();

        AccessTokenResponse tokenResponse = keycloakClient.tokenManager().getAccessToken();
        assertNotNull(tokenResponse);
        return tokenResponse.getToken();
    }
}

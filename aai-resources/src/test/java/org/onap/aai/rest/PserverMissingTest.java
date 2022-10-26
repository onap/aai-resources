/**
 * ============LICENSE_START=======================================================
 * org.onap.aai
 * ================================================================================
 * Copyright © 2017-2018 AT&T Intellectual Property. All rights reserved.
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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertEquals;

import java.util.*;

import org.junit.Test;
import org.onap.aai.PayloadUtil;
import org.springframework.http.*;

public class PserverMissingTest extends AbstractSpringRestTest {

    @Test
    public void testWhenContentTypeMissingItWillFunctionalAndCreateObjectWithPayloadInJson() throws Exception {

        String id = "test-" + UUID.randomUUID().toString();
        String endpoint = "/aai/v11/cloud-infrastructure/pservers/pserver/" + id;
        HttpHeaders headers = new HttpHeaders();

        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.add("Real-Time", "true");
        headers.add("X-FromAppId", "JUNIT");
        headers.add("X-TransactionId", "JUNIT");
        String authorization = Base64.getEncoder().encodeToString("AAI:AAI".getBytes("UTF-8"));
        headers.add("Authorization", "Basic " + authorization);

        Map<String, String> templateMap = new HashMap<>();

        templateMap.put("hostname", id);
        String body = PayloadUtil.getTemplatePayload("pserver.json", templateMap);

        httpEntity = new HttpEntity(body, headers);
        baseUrl = "http://localhost:" + randomPort;

        ResponseEntity responseEntity;
        responseEntity = restTemplate.exchange(baseUrl + endpoint, HttpMethod.PUT, httpEntity, String.class);

        assertEquals(HttpStatus.CREATED, responseEntity.getStatusCode());
    }

    @Test
    public void testWhenAcceptMissingItWillReturnThePayloadInXml() throws Exception {

        String id = "test-" + UUID.randomUUID().toString();
        String endpoint = "/aai/v11/cloud-infrastructure/pservers/pserver/" + id;
        HttpHeaders headers = new HttpHeaders();

        headers.add("Real-Time", "true");
        headers.add("X-FromAppId", "JUNIT");
        headers.add("X-TransactionId", "JUNIT");
        String authorization = Base64.getEncoder().encodeToString("AAI:AAI".getBytes("UTF-8"));
        headers.add("Authorization", "Basic " + authorization);

        httpEntity = new HttpEntity(headers);
        baseUrl = "http://localhost:" + randomPort;

        ResponseEntity responseEntity;
        responseEntity = restTemplate.exchange(baseUrl + endpoint, HttpMethod.GET, httpEntity, String.class);

        String body = responseEntity.getBody().toString();

        assertEquals(HttpStatus.NOT_FOUND, responseEntity.getStatusCode());
        assertThat(body, containsString("<Fault>"));
        assertThat(body, containsString("Resource not found for"));
        assertThat(body, containsString("Node Not Found"));
    }
}

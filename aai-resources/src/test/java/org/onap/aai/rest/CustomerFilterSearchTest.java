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

import org.junit.Test;
import org.onap.aai.PayloadUtil;
import org.springframework.http.*;

import java.util.Base64;
import java.util.Collections;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class CustomerFilterSearchTest extends AbstractSpringRestTest {

    @Test
    public void testWhenContentTypeMissingItWillFunctionalAndCreateObjectWithPayloadInJson() throws Exception {

        String id = "customer-987654321-91";
        String endpoint = "/aai/v11/business/customers/customer/"+ id;
        HttpHeaders headers = new HttpHeaders();

        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.add("Real-Time", "true");
        headers.add("X-FromAppId", "JUNIT");
        headers.add("X-TransactionId", "JUNIT");

        String authorization = Base64.getEncoder().encodeToString("AAI:AAI".getBytes("UTF-8"));
        headers.add("Authorization", "Basic " + authorization);
        String body = PayloadUtil.getResourcePayload("customer.json");

        httpEntity = new HttpEntity(body, headers);
        baseUrl = "http://localhost:" + randomPort;

        ResponseEntity responseEntity;
        responseEntity = restTemplate.exchange(baseUrl + endpoint, HttpMethod.PUT, httpEntity, String.class);

        assertEquals(HttpStatus.CREATED, responseEntity.getStatusCode());

        String searchUrl = baseUrl + "/aai/v11/business/customers?subscriber-name=subscriber-name-987654321-91&depth=0";
        httpEntity = new HttpEntity(headers);
        responseEntity = restTemplate.exchange(searchUrl, HttpMethod.GET, httpEntity, String.class);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertThat(responseEntity.getBody().toString(), containsString("global-customer-id"));
    }

}

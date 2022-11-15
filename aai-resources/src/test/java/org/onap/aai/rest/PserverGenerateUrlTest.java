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
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;

import com.jayway.jsonpath.JsonPath;

import java.io.UnsupportedEncodingException;
import java.util.Base64;
import java.util.Collections;
import java.util.UUID;

import org.junit.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

public class PserverGenerateUrlTest extends AbstractSpringRestTest {

    @Test
    public void testPutPserverExtractVertexGenerateUrlAndDeleteIt() throws UnsupportedEncodingException {

        String endpoint = "/aai/v11/cloud-infrastructure/pservers/pserver/test" + UUID.randomUUID().toString();

        ResponseEntity<String> responseEntity = null;

        responseEntity = restTemplate.exchange(baseUrl + endpoint, HttpMethod.GET, httpEntity, String.class);
        assertEquals(HttpStatus.NOT_FOUND, responseEntity.getStatusCode());

        responseEntity = restTemplate.exchange(baseUrl + endpoint, HttpMethod.PUT, httpEntity, String.class);
        assertEquals(HttpStatus.CREATED, responseEntity.getStatusCode());

        String vertexId = responseEntity.getHeaders().getFirst("vertex-id");
        responseEntity = restTemplate.exchange(baseUrl + "/aai/v11/generateurl/id/" + vertexId, HttpMethod.GET,
                plainHttpEntity(), String.class);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertThat(responseEntity.getBody().toString(), containsString(endpoint));

        responseEntity = restTemplate.exchange(baseUrl + endpoint, HttpMethod.GET, httpEntity, String.class);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());

        String body = responseEntity.getBody().toString();
        String resourceVersion = JsonPath.read(body, "$.resource-version");

        responseEntity = restTemplate.exchange(baseUrl + endpoint + "?resource-version=" + resourceVersion,
                HttpMethod.DELETE, httpEntity, String.class);
        assertEquals(HttpStatus.NO_CONTENT, responseEntity.getStatusCode());
    }

    private HttpEntity<String> plainHttpEntity() throws UnsupportedEncodingException {

        HttpHeaders headers = new HttpHeaders();

        headers.setAccept(Collections.singletonList(MediaType.TEXT_PLAIN));
        headers.add("Real-Time", "true");
        headers.add("X-FromAppId", "JUNIT");
        headers.add("X-TransactionId", "JUNIT");

        String authorization = Base64.getEncoder().encodeToString("AAI:AAI".getBytes("UTF-8"));
        headers.add("Authorization", "Basic " + authorization);
        return new HttpEntity<String>(headers);
    }
}

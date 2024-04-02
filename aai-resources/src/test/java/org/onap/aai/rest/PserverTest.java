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

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * A sample junit test using spring boot that provides the ability to spin
 * up the application from the junit layer and run rest requests against
 * SpringBootTest annotation with web environment requires which spring boot
 * class to load and the random port starts the application on a random port
 * and injects back into the application for the field with annotation LocalServerPort
 * <p>
 *
 * This can be used to potentially replace a lot of the fitnesse tests since
 * they will be testing against the same thing except fitnesse uses hbase
 */
public class PserverTest extends AbstractSpringRestTest {

    @Test
    public void testPutPserverExtractVertexAndThenDoGetByVertexIdAndThenDeleteIt() {

        String endpoint = "/aai/v11/cloud-infrastructure/pservers/pserver/test" + UUID.randomUUID().toString();

        ResponseEntity<String> responseEntity = null;

        responseEntity = restTemplate.exchange(baseUrl + endpoint, HttpMethod.GET, httpEntity, String.class);
        assertEquals(HttpStatus.NOT_FOUND, responseEntity.getStatusCode());

        responseEntity = restTemplate.exchange(baseUrl + endpoint, HttpMethod.PUT, httpEntity, String.class);
        assertEquals(HttpStatus.CREATED, responseEntity.getStatusCode());

        String vertexId = responseEntity.getHeaders().getFirst("vertex-id");
        responseEntity = restTemplate.exchange(baseUrl + "/aai/v11/resources/id/" + vertexId, HttpMethod.GET,
                httpEntity, String.class);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());

        responseEntity = restTemplate.exchange(baseUrl + endpoint, HttpMethod.GET, httpEntity, String.class);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());

        String body = responseEntity.getBody().toString();
        String resourceVersion = JsonPath.read(body, "$.resource-version");

        responseEntity = restTemplate.exchange(baseUrl + endpoint + "?resource-version=" + resourceVersion,
                HttpMethod.DELETE, httpEntity, String.class);
        assertEquals(HttpStatus.NO_CONTENT, responseEntity.getStatusCode());
    }
}

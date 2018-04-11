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
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.onap.aai.ResourcesApp;
import org.onap.aai.ResourcesTestConfiguration;
import org.onap.aai.config.PropertyPasswordConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.embedded.LocalServerPort;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.*;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.RestTemplate;

import java.io.UnsupportedEncodingException;
import java.util.Base64;
import java.util.Collections;
import java.util.UUID;

import static org.junit.Assert.assertEquals;

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
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = ResourcesApp.class)
@TestPropertySource(locations = "classpath:application-test.properties")
@ContextConfiguration(initializers = PropertyPasswordConfiguration.class)
@Import(ResourcesTestConfiguration.class)
public class PserverTest {

    @Autowired
    RestTemplate restTemplate;

    @LocalServerPort
    int randomPort;

    private HttpEntity httpEntity;

    private String baseUrl;

    @Before
    public void setup() throws UnsupportedEncodingException {

        HttpHeaders headers = new HttpHeaders();

        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("Real-Time", "true");
        headers.add("X-FromAppId", "JUNIT");
        headers.add("X-TransactionId", "JUNIT");

        String authorization = Base64.getEncoder().encodeToString("AAI:AAI".getBytes("UTF-8"));
        headers.add("Authorization", "Basic " + authorization);

        httpEntity = new HttpEntity(headers);
        baseUrl = "https://localhost:" + randomPort;
    }

    @Test
    public void testPutPserverExtractVertexAndThenDoGetByVertexIdAndThenDeleteIt() {

        String endpoint = "/aai/v11/cloud-infrastructure/pservers/pserver/test" + UUID.randomUUID().toString();

        ResponseEntity responseEntity = null;

        responseEntity = restTemplate.exchange(baseUrl + endpoint, HttpMethod.GET, httpEntity, String.class);
        assertEquals(HttpStatus.NOT_FOUND, responseEntity.getStatusCode());

        responseEntity = restTemplate.exchange(baseUrl + endpoint, HttpMethod.PUT, httpEntity, String.class);
        assertEquals(HttpStatus.CREATED, responseEntity.getStatusCode());

        String vertexId = responseEntity.getHeaders().getFirst("vertex-id");
        responseEntity = restTemplate.exchange(baseUrl + "/aai/v11/resources/id/" + vertexId, HttpMethod.GET, httpEntity, String.class);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());

        responseEntity = restTemplate.exchange(baseUrl + endpoint, HttpMethod.GET, httpEntity, String.class);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());

        String body = responseEntity.getBody().toString();
        String resourceVersion = JsonPath.read(body, "$.resource-version");

        responseEntity = restTemplate.exchange(baseUrl + endpoint+ "?resource-version=" + resourceVersion, HttpMethod.DELETE, httpEntity, String.class);
        assertEquals(HttpStatus.NO_CONTENT, responseEntity.getStatusCode());
    }
}

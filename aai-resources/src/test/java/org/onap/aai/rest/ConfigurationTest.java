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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jayway.jsonpath.JsonPath;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.onap.aai.ResourcesApp;
import org.onap.aai.ResourcesTestConfiguration;
import org.onap.aai.config.SpringContextAware;
import org.onap.aai.restclient.PropertyPasswordConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.cassandra.CassandraAutoConfiguration;
import org.springframework.boot.autoconfigure.data.cassandra.CassandraDataAutoConfiguration;
import org.springframework.boot.test.autoconfigure.actuate.metrics.AutoConfigureMetrics;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.client.RestTemplate;

/**
 * Test REST requests against configuration resource
 */

@AutoConfigureMetrics
@TestPropertySource(locations = "classpath:application-test.properties")
@ContextConfiguration(initializers = PropertyPasswordConfiguration.class, classes = {SpringContextAware.class})
@EnableAutoConfiguration(exclude={CassandraDataAutoConfiguration.class, CassandraAutoConfiguration.class}) // there is no running cassandra instance for the test
@Import(ResourcesTestConfiguration.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = {SpringContextAware.class, ResourcesApp.class})
public class ConfigurationTest extends AbstractSpringRestTest {

    @Autowired
    RestTemplate restTemplate;

    @LocalServerPort
    int randomPort;

    @Value("${local.management.port}")
    private int mgtPort;

    private HttpEntity<String> httpEntityGet;
    private HttpEntity<String> httpEntityPut;
    private HttpEntity<String> httpEntityPatch;
    private String baseUrl;
    private String actuatorurl;
    private HttpHeaders headersGet;
    private HttpHeaders headersPutPatch;

    @BeforeEach
    public void setup() throws UnsupportedEncodingException {

        headersGet = new HttpHeaders();
        headersGet.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headersGet.add("Real-Time", "true");
        headersGet.add("X-FromAppId", "JUNIT");
        headersGet.add("X-TransactionId", "JUNIT");

        headersGet.setBasicAuth("AAI", "AAI");

        headersPutPatch = new HttpHeaders();
        headersPutPatch.putAll(headersGet);
        headersPutPatch.setContentType(MediaType.APPLICATION_JSON);
        httpEntityGet = new HttpEntity<String>(headersGet);
        baseUrl = "http://localhost:" + randomPort;
        actuatorurl = "http://localhost:" + mgtPort;
    }

    @Test
    public void testGetPutPatchConfiguration() {
        String cid = "configtest" + UUID.randomUUID().toString();
        String endpoint = "/aai/v12/network/configurations/configuration/" + cid;

        ResponseEntity<String> responseEntity = null;

        responseEntity = restTemplate.exchange(baseUrl + endpoint, HttpMethod.GET, httpEntityGet, String.class);
        assertEquals(HttpStatus.NOT_FOUND, responseEntity.getStatusCode());

        // String putBody = " configuration-id, configuration-type
        // configuration-sub-type";
        String putBody = "{" + "\"configuration-id\": \"" + cid + "\"," + "\"configuration-type\": \"type1\","
                + "\"configuration-sub-type\": \"subtype1\", " + "\"operational-status\": \"example1\", "
                + "\"orchestration-status\": \"example1\", " + "\"configuration-selflink\": \"example1\", "
                + "\"model-customization-id\": \"example1\" " + "}";
        httpEntityPut = new HttpEntity<String>(putBody, headersPutPatch);
        responseEntity = restTemplate.exchange(baseUrl + endpoint, HttpMethod.PUT, httpEntityPut, String.class);
        assertEquals(HttpStatus.CREATED, responseEntity.getStatusCode());

        String vertexId = responseEntity.getHeaders().getFirst("vertex-id");
        responseEntity = restTemplate.exchange(baseUrl + "/aai/v12/resources/id/" + vertexId, HttpMethod.GET,
                httpEntityGet, String.class);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());

        responseEntity = restTemplate.exchange(baseUrl + endpoint, HttpMethod.GET, httpEntityGet, String.class);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());

        String patchBody = "{" + "\"configuration-id\": \"" + cid + "\"," + "\"configuration-type\": \"type2\","
                + "\"configuration-sub-type\": \"subtype2\", " + "\"operational-status\": \"example1\", "
                + "\"orchestration-status\": \"example1\", " + "\"configuration-selflink\": \"example1\", "
                + "\"model-customization-id\": \"example1\" " + "}";
        headersPutPatch.put("Content-Type", Arrays.asList("application/merge-patch+json"));
        headersPutPatch.add("X-HTTP-Method-Override", "PATCH");

        httpEntityPatch = new HttpEntity<String>(patchBody, headersPutPatch);

        responseEntity = restTemplate.exchange(baseUrl + endpoint, HttpMethod.POST, httpEntityPatch, String.class);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());

        responseEntity = restTemplate.exchange(baseUrl + endpoint, HttpMethod.GET, httpEntityGet, String.class);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());

        String body = responseEntity.getBody().toString();
        String configurationType = JsonPath.read(body, "$.configuration-type");

        assertEquals("type2", configurationType);

        patchBody = "{" + "\"configuration-id\": \"" + cid + "\"," + "\"configuration-type\": \"type3\","
                + "\"configuration-sub-type\": \"subtype3\" " + "}";

        httpEntityPatch = new HttpEntity<String>(patchBody, headersPutPatch);
        responseEntity = restTemplate.exchange(baseUrl + endpoint, HttpMethod.PATCH, httpEntityPatch, String.class);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());

        responseEntity = restTemplate.exchange(baseUrl + endpoint, HttpMethod.GET, httpEntityGet, String.class);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());

        body = responseEntity.getBody().toString();
        configurationType = JsonPath.read(body, "$.configuration-type");

        assertEquals("type3", configurationType);

    }

    @Test
    public void testManagementEndpointConfiguration() {
        ResponseEntity<String> responseEntity = null;
        String responseBody = null;

        // set Accept as text/plain in order to get access of endpoint
        // "/actuator/prometheus"
        headersGet.set("Accept", "text/plain");
        headersGet.setAccept(Arrays.asList(MediaType.TEXT_PLAIN));
        httpEntityGet = new HttpEntity<String>(headersGet);
        responseEntity = restTemplate.exchange(actuatorurl + "/actuator/prometheus", HttpMethod.GET, httpEntityGet,
                String.class);
        responseBody = (String) responseEntity.getBody();
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertTrue(responseBody.contains("group_id"));

        // Set Accept as MediaType.APPLICATION_JSON in order to get access of endpoint
        // "/actuator/info" and
        // "/actuator/health"
        headersGet.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        httpEntityGet = new HttpEntity<String>(headersGet);
        responseEntity = restTemplate.exchange(actuatorurl + "/actuator/info", HttpMethod.GET, httpEntityGet,
                String.class);
        responseBody = (String) responseEntity.getBody();
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertTrue(responseBody.contains("aai-resources"));

        responseEntity = restTemplate.exchange(actuatorurl + "/actuator/health", HttpMethod.GET, httpEntityGet,
                String.class);
        responseBody = (String) responseEntity.getBody();
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertTrue(responseBody.contains("UP"));
    }
}

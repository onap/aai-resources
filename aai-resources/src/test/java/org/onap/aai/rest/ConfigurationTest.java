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
import org.onap.aai.ResourcesApp;
import org.onap.aai.ResourcesTestConfiguration;
import org.onap.aai.restclient.PropertyPasswordConfiguration;
import org.onap.aai.config.SpringContextAware;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.*;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.client.RestTemplate;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Test REST requests against configuration resource
 */

@TestPropertySource(locations = "classpath:application-test.properties")
@ContextConfiguration(initializers = PropertyPasswordConfiguration.class, classes = {SpringContextAware.class})
@Import(ResourcesTestConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = {SpringContextAware.class, ResourcesApp.class})
public class ConfigurationTest extends AbstractSpringRestTest {
    @Autowired
    RestTemplate restTemplate;

    @LocalServerPort
    int randomPort;

    @Value("${local.management.port}")
    private int mgtPort;

    private HttpEntity<String> httpEntity;
    private HttpEntity<String> httpEntityPut;
    private HttpEntity<String> httpEntityPatch;
    private String baseUrl;
    private String actuatorurl;
    private HttpHeaders headers;
    @Before
    public void setup() throws UnsupportedEncodingException {

        headers = new HttpHeaders();

        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("Real-Time", "true");
        headers.add("X-FromAppId", "JUNIT");
        headers.add("X-TransactionId", "JUNIT");

        String authorization = Base64.getEncoder().encodeToString("AAI:AAI".getBytes("UTF-8"));
        headers.add("Authorization", "Basic " + authorization);

        httpEntity = new HttpEntity<String>(headers);
        baseUrl = "http://localhost:" + randomPort;
        actuatorurl = "http://localhost:" + mgtPort;
    }

    @Test
    public void testGetPutPatchConfiguration() {
    	String cid = "configtest" + UUID.randomUUID().toString();
        String endpoint = "/aai/v12/network/configurations/configuration/" + cid;

        ResponseEntity responseEntity = null;

        responseEntity = restTemplate.exchange(baseUrl + endpoint, HttpMethod.GET, httpEntity, String.class);
        assertEquals(HttpStatus.NOT_FOUND, responseEntity.getStatusCode());

        //String putBody = " configuration-id, configuration-type configuration-sub-type";
        String putBody = "{" +
        		"\"configuration-id\": \"" + cid + "\"," +
        		"\"configuration-type\": \"type1\"," +
        		"\"configuration-sub-type\": \"subtype1\", " +
                "\"operational-status\": \"example1\", " +
                "\"orchestration-status\": \"example1\", " +
                "\"configuration-selflink\": \"example1\", " +
                "\"model-customization-id\": \"example1\" " +
        	"}";
        httpEntityPut = new HttpEntity<String>(putBody, headers);
        responseEntity = restTemplate.exchange(baseUrl + endpoint, HttpMethod.PUT, httpEntityPut, String.class);
        assertEquals(HttpStatus.CREATED, responseEntity.getStatusCode());

        String vertexId = responseEntity.getHeaders().getFirst("vertex-id");
        responseEntity = restTemplate.exchange(baseUrl + "/aai/v12/resources/id/" + vertexId, HttpMethod.GET, httpEntity, String.class);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());

        responseEntity = restTemplate.exchange(baseUrl + endpoint, HttpMethod.GET, httpEntity, String.class);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        
        String patchBody = "{" +
        		"\"configuration-id\": \"" + cid + "\"," +
        		"\"configuration-type\": \"type2\"," +
        		"\"configuration-sub-type\": \"subtype2\", " +
                "\"operational-status\": \"example1\", " +
                "\"orchestration-status\": \"example1\", " +
                "\"configuration-selflink\": \"example1\", " +
                "\"model-customization-id\": \"example1\" " +
        	"}";
        headers.put("Content-Type", Arrays.asList("application/merge-patch+json"));
        headers.add("X-HTTP-Method-Override", "PATCH");
        
        httpEntityPatch = new HttpEntity<String>(patchBody, headers);
        
        responseEntity = restTemplate.exchange(baseUrl + endpoint, HttpMethod.POST, httpEntityPatch, String.class);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        
        responseEntity = restTemplate.exchange(baseUrl + endpoint, HttpMethod.GET, httpEntity, String.class);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        
        String body = responseEntity.getBody().toString();
        String configurationType = JsonPath.read(body, "$.configuration-type");

        assertEquals("type2", configurationType);
        
        patchBody = "{" +
        		"\"configuration-id\": \"" + cid + "\"," +
        		"\"configuration-type\": \"type3\"," +
        		"\"configuration-sub-type\": \"subtype3\" " +
        	"}";
        
        httpEntityPatch = new HttpEntity<String>(patchBody, headers);
        responseEntity = restTemplate.exchange(baseUrl + endpoint, HttpMethod.PATCH, httpEntityPatch, String.class);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        
        responseEntity = restTemplate.exchange(baseUrl + endpoint, HttpMethod.GET, httpEntity, String.class);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        
        body = responseEntity.getBody().toString();
        configurationType = JsonPath.read(body, "$.configuration-type");

        assertEquals("type3", configurationType);
        
    }
    

    @Test
    public void TestManagementEndpointConfiguration() {
        ResponseEntity responseEntity = null;
        String responseBody = null;

        //set Accept as text/plain in order to get access of endpoint "/actuator/prometheus"
        headers.set("Accept", "text/plain");
        httpEntity = new HttpEntity<String>(headers);
        responseEntity = restTemplate.exchange(actuatorurl + "/actuator/prometheus", HttpMethod.GET, httpEntity, String.class);
        responseBody = (String) responseEntity.getBody();
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertTrue(responseBody.contains("app_id"));
        assertTrue(responseBody.contains("group_id"));

        //Set Accept as MediaType.APPLICATION_JSON in order to get access of endpoint "/actuator/info" and "/actuator/health"
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        httpEntity = new HttpEntity<String>(headers);
        responseEntity = restTemplate.exchange(actuatorurl + "/actuator/info", HttpMethod.GET, httpEntity, String.class);
        responseBody = (String) responseEntity.getBody();
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertTrue(responseBody.contains("aai-resources"));

        responseEntity = restTemplate.exchange(actuatorurl + "/actuator/health", HttpMethod.GET, httpEntity, String.class);
        responseBody = (String) responseEntity.getBody();
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertTrue(responseBody.contains("UP"));
    }
}

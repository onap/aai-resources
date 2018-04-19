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
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.UUID;

import static org.junit.Assert.assertEquals;

/**
 * Test REST requests against configuration resource
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = ResourcesApp.class)
@TestPropertySource(locations = "classpath:application-test.properties")
@ContextConfiguration(initializers = PropertyPasswordConfiguration.class)
@Import(ResourcesTestConfiguration.class)
public class ConfigurationTest {

    @Autowired
    RestTemplate restTemplate;

    @LocalServerPort
    int randomPort;

    private HttpEntity<String> httpEntity;
    private HttpEntity<String> httpEntityPut;
    private HttpEntity<String> httpEntityPatch;
    private String baseUrl;
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
        baseUrl = "https://localhost:" + randomPort;
    }

    @Test
    public void testGetPutPatchConfiguration() {
    	String hostname = "pservertest" + UUID.randomUUID().toString();
        String endpoint = "/aai/v13/cloud-infrastructure/pservers/pserver/" + hostname;

        ResponseEntity responseEntity = null;

        responseEntity = restTemplate.exchange(baseUrl + endpoint, HttpMethod.GET, httpEntity, String.class);
        assertEquals(HttpStatus.NOT_FOUND, responseEntity.getStatusCode());

        String putBody = "{" +
        		"\"hostname\": \"" + hostname + "\"," +
        		"\"ptnii-equip-name\": \"type1\"," +
        		"\"equip-type\": \"subtype1\" " +
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
        		"\"hostname\": \"" + hostname + "\"," +
        		"\"ptnii-equip-name\": \"type2\"," +
        		"\"equip-type\": \"subtype2\" " +
        	"}";
        headers.put("Content-Type", Arrays.asList("application/merge-patch+json"));
        headers.add("X-HTTP-Method-Override", "PATCH");
        
        httpEntityPatch = new HttpEntity<String>(patchBody, headers);
        
        responseEntity = restTemplate.exchange(baseUrl + endpoint, HttpMethod.POST, httpEntityPatch, String.class);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        
        responseEntity = restTemplate.exchange(baseUrl + endpoint, HttpMethod.GET, httpEntity, String.class);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        
        String body = responseEntity.getBody().toString();
        String ptniiEquipName = JsonPath.read(body, "$.ptnii-equip-name");

        assertEquals("type2", ptniiEquipName);
        
        patchBody = "{" +
        		"\"hostname\": \"" + hostname + "\"," +
        		"\"ptnii-equip-name\": \"type3\"," +
        		"\"equip-type\": \"subtype3\" " +
        	"}";
        
        httpEntityPatch = new HttpEntity<String>(patchBody, headers);
        responseEntity = restTemplate.exchange(baseUrl + endpoint, HttpMethod.PATCH, httpEntityPatch, String.class);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        
        responseEntity = restTemplate.exchange(baseUrl + endpoint, HttpMethod.GET, httpEntity, String.class);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        
        body = responseEntity.getBody().toString();
        ptniiEquipName = JsonPath.read(body, "$.ptnii-equip-name");

        assertEquals("type3", ptniiEquipName);
        
    }
    
}

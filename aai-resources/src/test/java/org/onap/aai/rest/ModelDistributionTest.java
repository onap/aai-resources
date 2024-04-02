/**
 * ============LICENSE_START=======================================================
 * org.onap.aai
 * ================================================================================
 * Copyright © 2023 Deutsche Telekom AG. All rights reserved.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
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

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;

import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.onap.aai.rest.AbstractSpringRestTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.util.DefaultUriBuilderFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Test designed to imitate model-loader behaviour when distributing models via xml to aai-resources.
 * Similar to test in https://gerrit.onap.org/r/gitweb?p=aai/model-loader.git;a=blob;f=src/test/java/org/onap/aai/modelloader/restclient/TestAaiRestClient.java;h=ebdfcfe45285f14efc2f706caa49f0191b108619;hb=HEAD#l46
 */
public class ModelDistributionTest extends AbstractSpringRestTest {

    @Test
    public void thatModelsCanBeDistributed() throws Exception {
        final String MODEL_FILE = "src/test/resources/payloads/models/network-service.xml";
        String uri = baseUrl + "/aai/v28/service-design-and-creation/models/model/d821d1aa-8a69-47a4-aa63-3dae1742c47c";

        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_XML));
        headers.setContentType(null);
        ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.GET, new HttpEntity<>(headers), String.class);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());

        String modelPayload = new String(Files.readAllBytes(Paths.get(MODEL_FILE)));
        headers.setContentType(MediaType.APPLICATION_XML);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        response = restTemplate.exchange(uri, HttpMethod.PUT, new HttpEntity<>(modelPayload, headers), String.class);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());

        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        response = restTemplate.exchange(uri, HttpMethod.GET, new HttpEntity<>(headers), String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());

        ObjectMapper mapper = new ObjectMapper();
        String resourceVersion = mapper.readTree(response.getBody()).get("resource-version").asText();
        URI resourceVersionUri = new DefaultUriBuilderFactory(uri.toString()).builder().queryParam("resource-version", resourceVersion).build();
        response = restTemplate.exchange(resourceVersionUri, HttpMethod.DELETE, new HttpEntity<>(headers), String.class);
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }
}

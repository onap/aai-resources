/**
 * ============LICENSE_START=======================================================
 * org.onap.aai
 * ================================================================================
 * Copyright © 2017-2018 AT&T Intellectual Property. All rights reserved.
 * Copyright © 2017-2018 European Software Marketing Ltd.
 * Copyright © 2023 Deutsche Telekom AG.
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
package org.onap.aai.it;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Properties;

import javax.ws.rs.core.Response;

import java.time.Duration;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.onap.aai.rest.AbstractSpringRestTest;
import org.onap.aai.restclient.RestClient;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

public class TestAaiRestClient extends AbstractSpringRestTest {

    // private static final String MODEL_FILE = "src/test/resources/payloads/models/l3-network-widget.xml";
    private static final String MODEL_FILE = "src/test/resources/payloads/models/network-service.xml";
    // private static final String MODEL_FILE_JSON = "src/test/resources/payloads/models/l3-network-widget.json";
    private static final String MODEL_FILE_JSON = "src/test/resources/payloads/models/network-service.json";

    // RestTemplate restTemplate = new RestTemplate();
    // HttpHeaders headers = new HttpHeaders();

    @Test
    public void testRestClient() throws Exception {
        // String uri = baseUrl + "/aai/v25/service-design-and-creation/models/model/3d560d81-57d0-438b-a2a1-5334dba0651a"; // l3-network
        String uri = baseUrl + "/aai/v27/service-design-and-creation/models/model/d821d1aa-8a69-47a4-aa63-3dae1742c47c"; // network service
        // GET model
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_XML));
        headers.setContentType(null);
        ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.GET, new HttpEntity<>(headers), String.class);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());

        String modelPayload = new String(Files.readAllBytes(Paths.get(MODEL_FILE)));
        // PUT the model
        headers.setContentType(MediaType.APPLICATION_XML);
        response = restTemplate.exchange(uri, HttpMethod.PUT, new HttpEntity<>(modelPayload, headers), String.class);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        
        // String modelPayload = new String(Files.readAllBytes(Paths.get(MODEL_FILE)));
        // // PUT the model
        // headers.setContentType(MediaType.APPLICATION_XML);
        // response = restTemplate.exchange(uri, HttpMethod.PUT, new HttpEntity<>(modelPayload, headers), String.class);
        // assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatusCode());

        // DELETE the model
    //     opResult = aaiClient.getAndDeleteResource(uri, "example-trans-id-3");
    //     response = restTemplate.exchange(uri, HttpMethod.PUT, new HttpEntity<>(headers), String.class);
    //     assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getResultCode());
    }
}

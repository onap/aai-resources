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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.nio.file.Files;
import java.nio.file.Paths;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.onap.aai.config.WebClientConfiguration;
import org.onap.aai.entities.Model;
import org.onap.aai.entities.ModelVersion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Import;

import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

/**
 * Test designed to imitate model-loader behaviour when distributing models via
 * xml to aai-resources.
 * Similar to test in
 * https://gerrit.onap.org/r/gitweb?p=aai/model-loader.git;a=blob;f=src/test/java/org/onap/aai/modelloader/restclient/TestAaiRestClient.java;h=ebdfcfe45285f14efc2f706caa49f0191b108619;hb=HEAD#l46
 */
@Import(WebClientConfiguration.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
public class ModelDistributionTest {

    ObjectMapper objectMapper = new XmlMapper();

    @Autowired
    WebTestClient webClient;
    final String MODEL_FILE = "src/test/resources/payloads/models/network-service.xml";

    @Test
    @Order(1)
    public void thatModelsCanBeDistributed() throws Exception {
        String uri = "/aai/v29/service-design-and-creation/models/model/d821d1aa-8a69-47a4-aa63-3dae1742c47c";

        webClient.get()
                .uri(uri)
                .accept(MediaType.APPLICATION_XML)
                .exchange()
                .expectStatus()
                .isNotFound();

        String modelPayload = new String(Files.readAllBytes(Paths.get(MODEL_FILE)));
        webClient.put()
                .uri(uri)
                .accept(MediaType.APPLICATION_XML)
                .header("Content-Type", MediaType.APPLICATION_XML_VALUE)
                .bodyValue(modelPayload)
                .exchange()
                .expectStatus()
                .isCreated();

        String actual = webClient.get()
                .uri(uri)
                .accept(MediaType.APPLICATION_XML)
                .exchange()
                .expectStatus()
                .isOk()
                .returnResult(String.class)
                .getResponseBody()
                .blockFirst();

        Model expectedModel = objectMapper.readValue(modelPayload, Model.class);
        expectedModel.setModelVersions(null); // model versions are not being returned by the endpoint
        Model actualModel = objectMapper.readValue(actual, Model.class);
        Assertions.assertThat(expectedModel)
                .usingRecursiveComparison()
                .ignoringFields("resourceVersion")
                .isEqualTo(actualModel);

        webClient.delete()
                .uri(uriBuilder -> uriBuilder
                .path(uri)
                .queryParam("resource-version", actualModel.getResourceVersion()).build())
                .exchange()
                .expectStatus()
                .isNoContent();

    }

    @Test
    @Order(2)
    public void thatModelsCanBeRedistributed() throws Exception {
        final String UPDATE_MODEL_FILE = "src/test/resources/payloads/models/model-version.xml";
        String modelInvariantId = "d821d1aa-8a69-47a4-aa63-3dae1742c47c";
        String modelVersionId = "8b713350-90fc-44b1-8c6e-a2b3973aa9d3";
        String modelUri = "/aai/v29/service-design-and-creation/models/model/" + modelInvariantId;
        String modelVersionUri = modelUri + "/model-vers/model-ver/" + modelVersionId;
        webClient.get()
                .uri(modelUri)
                .accept(MediaType.APPLICATION_XML)
                .exchange()
                .expectStatus()
                .isNotFound();

        String modelPayload = new String(Files.readAllBytes(Paths.get(MODEL_FILE)));
        webClient.put()
                .uri(modelUri)
                .accept(MediaType.APPLICATION_XML)
                .header("Content-Type", MediaType.APPLICATION_XML_VALUE)
                .bodyValue(modelPayload)
                .exchange()
                .expectStatus()
                .isCreated();

        String modelVersionResponse = webClient.get()
                .uri(modelVersionUri)
                .accept(MediaType.APPLICATION_XML)
                .exchange()
                .expectStatus()
                .isOk()
                .returnResult(String.class)
                .getResponseBody()
                .blockFirst();
        ModelVersion modelVersion = objectMapper.readValue(modelVersionResponse, ModelVersion.class);
        assertNull(modelVersion.getModelElements());

        String updatePayload = new String(Files.readAllBytes(Paths.get(UPDATE_MODEL_FILE)))
                .replace("resourceVersion", modelVersion.getResourceVersion());
        webClient.put()
                .uri(modelVersionUri)
                .accept(MediaType.APPLICATION_XML)
                .header("Content-Type", MediaType.APPLICATION_XML_VALUE)
                .bodyValue(updatePayload)
                .exchange()
                .expectStatus()
                .isOk();

        modelVersionResponse = webClient.get()
                .uri(modelVersionUri)
                .accept(MediaType.APPLICATION_XML)
                .exchange()
                .expectStatus()
                .isOk()
                .returnResult(String.class)
                .getResponseBody()
                .blockFirst();
        modelVersion = objectMapper.readValue(modelVersionResponse, ModelVersion.class);
        assertEquals("2.0", modelVersion.getModelVersion());
    }
}

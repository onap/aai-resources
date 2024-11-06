/**
 * ============LICENSE_START=======================================================
 * org.onap.aai
 * ================================================================================
 * Copyright © 2017-2018 AT&T Intellectual Property. All rights reserved.
 * ================================================================================
 * Modifications Copyright © 2024 Deutsche Telekom.
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
import static org.hamcrest.Matchers.samePropertyValuesAs;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.io.IOUtils;
import org.apache.tinkerpop.gremlin.process.traversal.P;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.janusgraph.core.JanusGraph;
import org.janusgraph.core.JanusGraphTransaction;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.simple.parser.ParseException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.onap.aai.config.WebClientConfiguration;
import org.onap.aai.db.props.AAIProperties;
import org.onap.aai.dbmap.AAIGraph;
import org.onap.aai.entities.AAIErrorResponse;
import org.onap.aai.entities.PServer;
import org.onap.aai.entities.PServerListResponse;
import org.onap.aai.entities.PolicyException;
import org.onap.aai.entities.ServiceException;
import org.onap.aai.exceptions.AAIException;
import org.onap.aai.service.ResourcesService;
import org.onap.aai.setup.SchemaVersions;
import org.onap.aai.util.AAIConfig;
import org.onap.aai.util.AAIConstants;
import org.skyscreamer.jsonassert.JSONAssert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.springframework.web.util.UriBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;

import reactor.core.publisher.Mono;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Import(WebClientConfiguration.class)
public class ResourcesControllerTest {

    private static final Logger logger = LoggerFactory.getLogger(ResourcesController.class.getName());
    private static final Set<Integer> VALID_HTTP_STATUS_CODES = new HashSet<>();
    protected static final MediaType APPLICATION_JSON = MediaType.valueOf("application/json");

    static {
        VALID_HTTP_STATUS_CODES.add(200);
        VALID_HTTP_STATUS_CODES.add(201);
        VALID_HTTP_STATUS_CODES.add(204);
    }

    @Autowired WebTestClient webClient;
    @Autowired SchemaVersions schemaVersions;

    ObjectMapper mapper = new ObjectMapper();

    private ResourcesController resourcesController;
    private HttpHeaders httpHeaders;
    private UriInfo uriInfo;
    private MultivaluedMap<String, String> headersMultiMap;
    private MultivaluedMap<String, String> queryParameters;
    private List<String> aaiRequestContextList;
    private List<MediaType> outputMediaTypes;
    private String defaultSchemaVersion;

    @BeforeEach
    public void setup() {
        if(!AAIGraph.isInit()) {
            AAIGraph.getInstance();
        }
        logger.info("Starting the setup for the integration tests of Rest Endpoints");

        resourcesController = new ResourcesController(new ResourcesService());
        httpHeaders = Mockito.mock(HttpHeaders.class);
        uriInfo = Mockito.mock(UriInfo.class);

        headersMultiMap = new MultivaluedHashMap<>();
        queryParameters = Mockito.spy(new MultivaluedHashMap<>());

        headersMultiMap.add("X-FromAppId", "JUNIT");
        headersMultiMap.add("X-TransactionId", UUID.randomUUID().toString());
        headersMultiMap.add("Real-Time", "true");
        headersMultiMap.add("Accept", "application/json");
        headersMultiMap.add("aai-request-context", "");

        outputMediaTypes = new ArrayList<>();
        outputMediaTypes.add(APPLICATION_JSON);

        aaiRequestContextList = new ArrayList<>();
        aaiRequestContextList.add("");
        defaultSchemaVersion = schemaVersions.getDefaultVersion().toString();

        when(httpHeaders.getAcceptableMediaTypes()).thenReturn(outputMediaTypes);
        when(httpHeaders.getRequestHeaders()).thenReturn(headersMultiMap);

        when(httpHeaders.getRequestHeader("aai-request-context")).thenReturn(aaiRequestContextList);

        when(uriInfo.getQueryParameters()).thenReturn(queryParameters);
        when(uriInfo.getQueryParameters(false)).thenReturn(queryParameters);

        // TODO - Check if this is valid since RemoveDME2QueryParameters seems to be very unreasonable
        Mockito.doReturn(null).when(queryParameters).remove(any());

        when(httpHeaders.getMediaType()).thenReturn(APPLICATION_JSON);
    }

    @AfterEach
    public void tearDown() {
        JanusGraph janusGraph = AAIGraph.getInstance().getGraph();
        JanusGraphTransaction transaction = janusGraph.newTransaction();
        boolean success = true;
        try {
            GraphTraversalSource g = transaction.traversal();
            g.V().drop().iterate();
        } catch (Exception ex) {
            success = false;
        } finally {
            if (success) {
                transaction.commit();
            } else {
                transaction.rollback();
            }
        }
    }

    @Test
    public void testResponsePutGetDeleteOnResource() throws JSONException, IOException, AAIException {
        String payload = getResourcePayload(getObjectName());
        PServer expected = mapper.readValue(payload, PServer.class);

        webClient.get()
            .uri("/cloud-infrastructure/pservers/pserver/pserver-hostname-test?cleanup=false")
            .exchange()
            .expectStatus()
            .isNotFound();

        webClient.put()
            .uri("/cloud-infrastructure/pservers/pserver/pserver-hostname-test")
            .bodyValue(payload)
            .exchange()
            .expectStatus()
            .isCreated();

        PServer pserver = webClient.get()
            .uri("/cloud-infrastructure/pservers/pserver/pserver-hostname-test?cleanup=false&depth=10000")
            .exchange()
            .expectStatus()
            .isOk()
            .returnResult(PServer.class)
            .getResponseBody()
            .blockFirst();

        assertThat(pserver, samePropertyValuesAs(expected, "resourceVersion"));

        String resourceVersion = pserver.getResourceVersion();

        webClient.delete()
            .uri(uriBuilder -> uriBuilder
                .path("/cloud-infrastructure/pservers/pserver/pserver-hostname-test")
                .queryParam("resource-version", resourceVersion)
                .build())
            .exchange()
            .expectStatus()
            .isNoContent();

        webClient.get()
            .uri("/cloud-infrastructure/pservers/pserver/pserver-hostname-test?cleanup=false&depth=10000")
            .exchange()
            .expectStatus()
            .isNotFound();
    }

    @Test
    public void testResponseGetOnResourcePaginated() throws JSONException, IOException, AAIException {
        JanusGraph graph = AAIGraph.getInstance().getGraph();
        GraphTraversalSource g = graph.traversal();
        g.addV()
            .property("aai-node-type", "pserver")
            .property("hostname", "hostname1")
            .property("resource-version", UUID.randomUUID().toString())
            .property(AAIProperties.AAI_URI, "/cloud-infrastructure/pservers/pserver/hostname1")
         .addV()
            .property("aai-node-type", "pserver")
            .property("hostname", "hostname2")
            .property("resource-version", UUID.randomUUID().toString())
            .property(AAIProperties.AAI_URI, "/cloud-infrastructure/pservers/pserver/hostname2")
            .next();
        g.tx().commit();

        PServerListResponse pservers = webClient.get()
            .uri(uriBuilder ->
                uriBuilder
                    .path("/cloud-infrastructure/pservers")
                    .queryParam("resultIndex", "1")
                    .queryParam("resultSize", "10")
                    .build())
            .exchange()
            .expectStatus()
            .isOk()
            // TODO: Assert values here once test data is isolated to individual test
            .expectHeader().exists("total-results")
            .expectHeader().exists("total-pages")
            .returnResult(PServerListResponse.class)
            .getResponseBody()
            .blockFirst();

        assertTrue(pservers.getPserver().size() > 0);
    }

    @Test
    public void testPutPserverAndCloudRegionRelationship() throws IOException, JSONException {

        String pserverData = getRelationshipPayload("pserver");
        String complexData = getRelationshipPayload("complex");

        String hostname = "590a8943-1200-43b3-825b-75dde6b8f44a";
        String physicalLocationId = "e13d4587-19ad-4bf5-80f5-c021efb5b61c";

        String pserverUri = String.format("/cloud-infrastructure/pservers/pserver/%s", hostname);
        String cloudRegionUri = String.format("/cloud-infrastructure/complexes/complex/%s", physicalLocationId);

        doSetupResource(pserverUri, pserverData);
        doSetupResource(cloudRegionUri, complexData);

        String cloudToPserverRelationshipData = getRelationshipPayload("pserver-complex-relationship");
        String cloudToPserverRelationshipUri =
                String.format("/cloud-infrastructure/pservers/pserver/%s/relationship-list/relationship", hostname);

        webClient.put()
                .uri(cloudToPserverRelationshipUri)
                .bodyValue(cloudToPserverRelationshipData)
                .exchange()
                .expectStatus().isOk();

        webClient.method(HttpMethod.DELETE)
            .uri(cloudToPserverRelationshipUri)
            .body(Mono.just(cloudToPserverRelationshipData), String.class)
            .exchange()
            .expectStatus()
            .isNoContent();
    }

    @Test
    public void testPutPassWithEmptyData() throws JSONException {

        String payload = "{}";
        String pserverUri = String.format("/cloud-infrastructure/pservers/pserver/%s", UUID.randomUUID().toString());

        doSetupResource(pserverUri, payload);

        payload = "{}";
        pserverUri = String.format("/cloud-infrastructure/pservers/pserver/%s", UUID.randomUUID().toString());
        doSetupResource(pserverUri, payload);
    }

    @Test
    public void thatUnknownPathReturnsBadRequest() throws JSONException {
        String uri = "/fake-infrastructure/pservers/pserver/fajsidj";
        AAIErrorResponse errorResponse = webClient.put()
                .uri(uri)
                .bodyValue("{}")
                .exchange()
                .expectStatus().isBadRequest()
                .returnResult(AAIErrorResponse.class)
                .getResponseBody()
                .blockFirst();

        ServiceException serviceException = errorResponse.getRequestError().getServiceException();
        assertEquals("SVC3000", serviceException.getMessageId());
        assertEquals("Invalid input performing %1 on %2 (msg=%3) (ec=%4)", serviceException.getText());
        List<String> expected = List.of(
            "PUT",
            "v29/fake-infrastructure/pservers/pserver/fajsidj",
            "Invalid input performing %1 on %2:Unrecognized AAI object fake-infrastructure",
            "ERR.5.2.3000");
        assertIterableEquals(expected, serviceException.getVariables());
    }

    @Test
    public void testInvalidUriThrowRandomException() throws JSONException {

        String payload = "{}";
        String uri = "fake-infrastructure/pservers/pserver/fajsidj";

        // webClient.put()
        //     .uri(uri)
        //     .bodyValue(payload)
        //     .exchange()
        //     .expectStatus().isEqualTo(500)
        //     .expectBody(AAIErrorResponse.class)
        //     .value(responseBody -> {
        //         assertNotNull(responseBody, "Response body should not be null");
        //         assertEquals("SVC3002", responseBody.getRequestError().getServiceException().getMessageId());
        //     });

        when(uriInfo.getPath()).thenReturn(uri);
        when(uriInfo.getPath(false)).thenThrow(new IllegalArgumentException());
        MockHttpServletRequest mockReq = new MockHttpServletRequest("PUT", uri);
        Response response = resourcesController.update(payload, defaultSchemaVersion, uri,
                httpHeaders, uriInfo, mockReq);

        int code = response.getStatus();
        assertNotNull(response, "Response from the legacy moxy consumer returned null");
        assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), code);
        logger.info("Response Code: " + code + "\tEntity: " + response.getEntity());

        response = resourcesController.updateRelationship(payload, defaultSchemaVersion, uri,
                httpHeaders, uriInfo, mockReq);

        code = response.getStatus();
        assertNotNull(response, "Response from the legacy moxy consumer returned null");
        assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), code);
        logger.info("Response Code: " + code + "\tEntity: " + response.getEntity());
        mockReq = new MockHttpServletRequest("GET", uri);
        response = resourcesController.getLegacy(defaultSchemaVersion, uri, -1, -1, false,
                "all", "false", httpHeaders, uriInfo, mockReq);

        assertNotNull(response, "Response from the legacy moxy consumer returned null");
        assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), code);
        mockReq = new MockHttpServletRequest("DELETE", uri);
        response = resourcesController.delete(defaultSchemaVersion, uri, httpHeaders, uriInfo,
                "", mockReq);

        code = response.getStatus();
        assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), code);

        response = resourcesController.deleteRelationship(payload, defaultSchemaVersion, uri,
                httpHeaders, uriInfo, mockReq);
        code = response.getStatus();
        assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), code);
    }

    @Test
    public void testInvalidUriContainingRelatedToShouldThrowAAIException() throws JSONException {

        String payload = "{}";
        String uri = "/cloud-infrastructure/pservers/pserver/hostname/related-to/fsdf";

        AAIErrorResponse errorResponse = webClient.put()
                .uri(uri)
                .bodyValue(payload)
                .exchange()
                .expectStatus().isBadRequest()
                .returnResult(AAIErrorResponse.class)
                .getResponseBody()
                .blockFirst();

        ServiceException serviceException = errorResponse.getRequestError().getServiceException();
        assertEquals("SVC3002", serviceException.getMessageId());
        assertEquals("Error writing output performing %1 on %2 (msg=%3) (ec=%4)", serviceException.getText());
        List<String> expected = List.of(
            "PUT",
            "v29/cloud-infrastructure/pservers/pserver/hostname/related-to/fsdf",
            "Cannot write via this URL",
            "ERR.5.6.3010");
        assertIterableEquals(expected, serviceException.getVariables());

        uri = "/cloud-infrastructure/pservers/pserver/hostname/related-to/fsdf/relationship-list/relationship";

        errorResponse = webClient.put()
                .uri(uri)
                .bodyValue(payload)
                .exchange()
                .expectStatus().isBadRequest()
                .returnResult(AAIErrorResponse.class)
                .getResponseBody()
                .blockFirst();

        serviceException = errorResponse.getRequestError().getServiceException();
        expected = List.of(
            "PUT",
            "v29/cloud-infrastructure/pservers/pserver/hostname/related-to/fsdf/relationship-list/relationship",
            "Cannot write via this URL",
            "ERR.5.6.3010");
        assertIterableEquals(expected, serviceException.getVariables());


        when(uriInfo.getPath()).thenReturn(uri);
        when(uriInfo.getPath(false)).thenReturn(uri);
        MockHttpServletRequest mockReq = new MockHttpServletRequest("PUT", uri);
        Response response;
        int code;
        mockReq = new MockHttpServletRequest("GET", uri);
        response = resourcesController.getLegacy(defaultSchemaVersion, uri, -1, -1, false,
                "all", "false", httpHeaders, uriInfo, mockReq);
        code = response.getStatus();
        assertNotNull(response, "Response from the legacy moxy consumer returned null");
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), code);

        mockReq = new MockHttpServletRequest("DELETE", uri);
        queryParameters.add("resource-version", "3434394839483");
        response = resourcesController.delete(defaultSchemaVersion, uri, httpHeaders, uriInfo,
                "", mockReq);

        code = response.getStatus();
        logger.info("Response Code: " + code + "\tEntity: " + response.getEntity());
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), code);

        response = resourcesController.deleteRelationship(payload, defaultSchemaVersion, uri,
                httpHeaders, uriInfo, mockReq);
        code = response.getStatus();
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), code);
    }

    @Test
    @Disabled("Unable to test this method due to WRITE_BIGDECIMAL_AS_PLAIN error")
    public void testPatchWithValidData() throws IOException {

        String payload = getResourcePayload("pserver-patch-test");
        String uri = getUri("pserver-patch-test");

        if (uri.length() != 0 && uri.charAt(0) == '/') {
            uri = uri.substring(1);
        }

        when(uriInfo.getPath()).thenReturn(uri);
        when(uriInfo.getPath(false)).thenReturn(uri);

        MockHttpServletRequest mockReq = new MockHttpServletRequest("GET", uri);
        Response response = resourcesController.getLegacy(defaultSchemaVersion, uri, -1, -1,
                false, "all", "false", httpHeaders, uriInfo, mockReq);

        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
        mockReq = new MockHttpServletRequest("PUT", uri);
        response = resourcesController.update(payload, defaultSchemaVersion, uri, httpHeaders,
                uriInfo, mockReq);

        int code = response.getStatus();
        if (!VALID_HTTP_STATUS_CODES.contains(code)) {
            logger.info("Response Code: " + code + "\tEntity: " + response.getEntity());
        }

        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());

        String patchData = "{\"in-maint\": false}";

        headersMultiMap.add("Content-Type", "application/json");

        outputMediaTypes.remove(APPLICATION_JSON);
        outputMediaTypes.add(MediaType.valueOf("application/merge-patch+json"));

        mockReq = new MockHttpServletRequest("PATCH", uri);
        response = resourcesController.patch(patchData, defaultSchemaVersion, uri, httpHeaders,
                uriInfo, mockReq);

        code = response.getStatus();
        assertNotNull(response, "Response from the patch returned null");
        logger.info("Response Code: " + code + "\tEntity: " + response.getEntity());
        assertEquals(Response.Status.OK.getStatusCode(), code);

    }

    protected void doSetupResource(String uri, String payload) throws JSONException {
        webClient.get()
            .uri(uri)
            .exchange()
            .expectStatus()
            .isNotFound();

        webClient.put()
            .uri(uri)
            .bodyValue(payload)
            .exchange()
            .expectStatus()
            .isCreated();

        String responseBody = webClient.get()
            .uri(uriBuilder ->
                uriBuilder
                    .path(uri)
                    .queryParam("depth", "10000")
                    .build())
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody(String.class)
            .returnResult().getResponseBody();

        JSONAssert.assertEquals(payload, responseBody, false);
    }

    @Test
    public void testDeleteRelationshipWithEmptyBodyReturnsBadRequest() {
        String uri = "/cloud-infrastructure/pservers/pserver/testData/relationship-list/relationship";
        AAIErrorResponse errorResponse = webClient.method(HttpMethod.DELETE)
                .uri(uri)
                .body(Mono.just(""), String.class)
                .exchange()
                .expectStatus().isBadRequest()
                .returnResult(AAIErrorResponse.class)
                .getResponseBody()
                .blockFirst();

        PolicyException policyException = errorResponse.getRequestError().getPolicyException();
        assertEquals("POL3102", policyException.getMessageId());
        assertEquals("Error parsing input performing %1 on %2 (msg=%3) (ec=%4)", policyException.getText());
        List<String> expected = List.of(
            "DELETE",
            "v29/cloud-infrastructure/pservers/pserver/testData/relationship-list/relationship",
            "Error parsing input performing %1 on %2:You must supply a relationship",
            "ERR.5.1.3102");
        assertIterableEquals(expected, policyException.getVariables());
    }

    // TODO - Change this to be abstract and inheritable
    public String getObjectName() {
        return "pserver";
    }

    public String getResourcePayload(String resourceName) throws IOException {
        String rawPayload = IOUtils.toString(this.getClass().getResourceAsStream("/payloads/resource/" + resourceName + ".json"), StandardCharsets.UTF_8);
        return String.format(rawPayload, defaultSchemaVersion);
    }

    public String getRelationshipPayload(String relationshipName) throws IOException {
        String rawPayload = IOUtils.toString(this.getClass().getResourceAsStream("/payloads/relationship/" + relationshipName + ".json"), StandardCharsets.UTF_8);
        return String.format(rawPayload, defaultSchemaVersion);
    }

    public String getUri(String hostname) {
        return String.format("/cloud-infrastructure/pservers/pserver/%s", hostname);
    }

    public String getGetAllPserversURI() {
        return "/cloud-infrastructure/pservers";
    }

    public String getUri() {
        return getUri("pserver-hostname-test");
    }

    @Test
    public void checkTimeoutEnabled() throws Exception {
        boolean isTimeoutEnabled = resourcesController.isTimeoutEnabled("JUNITTESTAPP1",
                AAIConfig.get(AAIConstants.AAI_CRUD_TIMEOUT_ENABLED), AAIConfig.get(AAIConstants.AAI_CRUD_TIMEOUT_APP),
                AAIConfig.get(AAIConstants.AAI_CRUD_TIMEOUT_LIMIT));
        assertEquals(true, isTimeoutEnabled);
    }

    @Test
    public void checkTimeoutEnabledOverride() throws Exception {
        boolean isTimeoutEnabled = resourcesController.isTimeoutEnabled("JUNITTESTAPP2",
                AAIConfig.get(AAIConstants.AAI_CRUD_TIMEOUT_ENABLED), AAIConfig.get(AAIConstants.AAI_CRUD_TIMEOUT_APP),
                AAIConfig.get(AAIConstants.AAI_CRUD_TIMEOUT_LIMIT));
        assertEquals(false, isTimeoutEnabled);
    }

    @Test
    public void checkTimeoutEnabledDefaultLimit() throws Exception {
        boolean isTimeoutEnabled = resourcesController.isTimeoutEnabled("JUNITTESTAPP3",
                AAIConfig.get(AAIConstants.AAI_CRUD_TIMEOUT_ENABLED), AAIConfig.get(AAIConstants.AAI_CRUD_TIMEOUT_APP),
                AAIConfig.get(AAIConstants.AAI_CRUD_TIMEOUT_LIMIT));
        assertEquals(true, isTimeoutEnabled);
        int timeout = resourcesController.getTimeoutLimit("JUNITTESTAPP3",
                AAIConfig.get(AAIConstants.AAI_CRUD_TIMEOUT_APP), AAIConfig.get(AAIConstants.AAI_CRUD_TIMEOUT_LIMIT));
        assertEquals(100000, timeout);
    }

    @Test
    public void getTimeout() throws Exception {
        int timeout = resourcesController.getTimeoutLimit("JUNITTESTAPP1",
                AAIConfig.get(AAIConstants.AAI_CRUD_TIMEOUT_APP), AAIConfig.get(AAIConstants.AAI_CRUD_TIMEOUT_LIMIT));
        assertEquals(1, timeout);
    }

    @Test
    public void getTimeoutOverride() throws Exception {
        int timeout = resourcesController.getTimeoutLimit("JUNITTESTAPP2",
                AAIConfig.get(AAIConstants.AAI_CRUD_TIMEOUT_APP), AAIConfig.get(AAIConstants.AAI_CRUD_TIMEOUT_LIMIT));
        assertEquals(-1, timeout);
    }

    @Disabled("Time sensitive test only times out if the response takes longer than 1 second")
    @Test
    public void testTimeoutGetCall() throws Exception {
        String uri = getUri();

        if (uri.length() != 0 && uri.charAt(0) == '/') {
            uri = uri.substring(1);
        }

        when(uriInfo.getPath()).thenReturn(uri);
        when(uriInfo.getPath(false)).thenReturn(uri);
        headersMultiMap.putSingle("X-FromAppId", "JUNITTESTAPP1");
        when(httpHeaders.getRequestHeaders()).thenReturn(headersMultiMap);

        MockHttpServletRequest mockReqGet = new MockHttpServletRequest("GET", uri);
        Response response = resourcesController.getLegacy(defaultSchemaVersion, uri, -1, -1,
                false, "all", "false", httpHeaders, uriInfo, mockReqGet);

        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    @Test
    public void testBypassTimeoutGetCall() throws Exception {
        String uri = getUri();

        if (uri.length() != 0 && uri.charAt(0) == '/') {
            uri = uri.substring(1);
        }

        when(uriInfo.getPath()).thenReturn(uri);
        when(uriInfo.getPath(false)).thenReturn(uri);
        headersMultiMap.putSingle("X-FromAppId", "JUNITTESTAPP2");
        when(httpHeaders.getRequestHeaders()).thenReturn(headersMultiMap);

        MockHttpServletRequest mockReqGet = new MockHttpServletRequest("GET", uri);
        Response response = resourcesController.getLegacy(defaultSchemaVersion, uri, -1, -1,
                false, "all", "false", httpHeaders, uriInfo, mockReqGet);

        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
    }

    @Test
    public void testGetRelationshipWithoutFormat() throws IOException, JSONException {
        String payload = getRelationshipPayload("pserver-complex-relationship-list2");
        String pserverData = getRelationshipPayload("pserver2");
        String complexData = getRelationshipPayload("complex2");

        String hostname = "590a8943-1200-43b3-825b-75dde6b8f44b";
        String physicalLocationId = "e13d4587-19ad-4bf5-80f5-c021efb5b61d";

        String pserverUri = String.format("/cloud-infrastructure/pservers/pserver/%s", hostname);
        String cloudRegionUri = String.format("/cloud-infrastructure/complexes/complex/%s", physicalLocationId);

        doSetupResource(pserverUri, pserverData);
        doSetupResource(cloudRegionUri, complexData);

        String cloudToPserverRelationshipData = getRelationshipPayload("pserver-complex-relationship2");
        String cloudToPserverRelationshipUri =
                String.format("/cloud-infrastructure/pservers/pserver/%s/relationship-list/relationship", hostname);

        webClient.put()
            .uri(cloudToPserverRelationshipUri)
            .bodyValue(cloudToPserverRelationshipData)
            .exchange()
            .expectStatus()
            .isOk();

        String getRelationshipUri = String.format("/cloud-infrastructure/pservers/pserver/%s/relationship-list", hostname);
        String responseBody = webClient.get()
            .uri(uriBuilder ->
                uriBuilder
                    .path(getRelationshipUri)
                    .queryParam("resultIndex", "1")
                    .queryParam("resultSize", "1")
                    .queryParam("includeTotalCount", "false")
                    .build())
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody(String.class)
            .returnResult().getResponseBody();
        JSONAssert.assertEquals(payload, responseBody, false);
    }

    @Test
    public void testGetRelationshipWithFormat() throws IOException, JSONException, ParseException {
        String payload = getRelationshipPayload("pserver-complex-relationship-list3");
        String pserverData = getRelationshipPayload("pserver3");
        String complexData = getRelationshipPayload("complex3");

        String hostname = "590a8943-1200-43b3-825b-75dde6b8f44c";
        String physicalLocationId = "e13d4587-19ad-4bf5-80f5-c021efb5b61e";

        String pserverUri = String.format("/cloud-infrastructure/pservers/pserver/%s", hostname);
        String cloudRegionUri = String.format("/cloud-infrastructure/complexes/complex/%s", physicalLocationId);

        doSetupResource(pserverUri, pserverData);
        doSetupResource(cloudRegionUri, complexData);

        String cloudToPserverRelationshipData = getRelationshipPayload("pserver-complex-relationship3");
        String cloudToPserverRelationshipUri =
                String.format("/cloud-infrastructure/pservers/pserver/%s/relationship-list/relationship", hostname);

        webClient.put()
            .uri(cloudToPserverRelationshipUri)
            .bodyValue(cloudToPserverRelationshipData)
            .exchange()
            .expectStatus()
            .isOk();

        String getRelationshipUri = String.format("/cloud-infrastructure/pservers/pserver/%s", hostname);
        String responseBody = webClient.get()
            .uri(uriBuilder ->
                uriBuilder
                    .path(getRelationshipUri)
                    .queryParam("resultIndex", "1")
                    .queryParam("resultSize", "1")
                    .queryParam("includeTotalCount", "false")
                    .queryParam("format", "resource")
                    .build())
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody(String.class)
            .returnResult().getResponseBody();

        String responsePayload = responseBody.toString();
        JSONObject payloadJsonObject = new JSONObject(payload);
        JSONObject responseJsonObject = new JSONObject(responsePayload);

        JSONArray payloadResultsArray = payloadJsonObject.getJSONArray("results");
        JSONArray responseResultsArray = responseJsonObject.getJSONArray("results");
        assertEquals(payloadResultsArray.length(), responseResultsArray.length());

        for (int i = 0; i < payloadResultsArray.length(); i++) {
            String payloadResults = payloadResultsArray.get(i).toString();
            String responseResults = responseResultsArray.get(i).toString();

            JSONObject pserverPayloadObject = new JSONObject(payloadResults);
            JSONObject pserverResponseObject = new JSONObject(responseResults);
            String pserverPayload = pserverPayloadObject.get("pserver").toString();
            String pserverResponse = pserverResponseObject.get("pserver").toString();

            JSONObject pserverPayloadFields = new JSONObject(pserverPayload);
            JSONObject pserverResponseFields = new JSONObject(pserverResponse);
            String pserverPayloadHostname = pserverPayloadFields.get("hostname").toString();
            String pserverResponseHostname = pserverResponseFields.get("hostname").toString();
            String pserverPayloadInmaint = pserverPayloadFields.get("in-maint").toString();
            String pserverResponseInmaint = pserverResponseFields.get("in-maint").toString();
            String pserverPayloadRelationshipList = pserverPayloadFields.get("relationship-list").toString();
            String pserverResponseRelationshipList = pserverResponseFields.get("relationship-list").toString();

            assertEquals(pserverPayloadHostname, pserverResponseHostname);
            assertEquals(pserverPayloadInmaint, pserverResponseInmaint);
            assertEquals(pserverPayloadRelationshipList, pserverResponseRelationshipList);
        }
    }

    @Test
    public void testGetRelationshipWithoutSuppliedRelationship() throws IOException, JSONException {
        String pserverData = getRelationshipPayload("pserver4");
        String complexData = getRelationshipPayload("complex4");

        String hostname = "590a8943-1200-43b3-825b-75dde6b8f44d";
        String physicalLocationId = "e13d4587-19ad-4bf5-80f5-c021efb5b61f";

        String pserverUri = String.format("/cloud-infrastructure/pservers/pserver/%s", hostname);
        String cloudRegionUri = String.format("/cloud-infrastructure/complexes/complex/%s", physicalLocationId);

        doSetupResource(pserverUri, pserverData);
        doSetupResource(cloudRegionUri, complexData);

        String getRelationshipMockRequestUri =
                String.format("/cloud-infrastructure/pservers/pserver/%s/relationship-list", hostname);
        webClient.get()
            .uri(getRelationshipMockRequestUri)
            .exchange()
            .expectStatus()
            .isNotFound();
    }

    @Test
    public void testGetWithSkipRelatedToParam() throws IOException, JSONException {
        String pserverData = getResourcePayload("pserver1");
        String vserverData = getResourcePayload("vserver1");

        String hostname = "pserver-hostname-test01";
        String cloudRegionId = "testAIC01";
        String tenant = "tenant01";
        String vserver = "vserver01";

        String pserverUri = String.format("/cloud-infrastructure/pservers/pserver/%s?skip-related-to=true", hostname);
        String vserverUri = String.format(
                "/cloud-infrastructure/cloud-regions/cloud-region/test-aic/%s?skip-related-to=true", cloudRegionId);

        // PUT the resources
        putResourceWithQueryParam(pserverUri, pserverData);
        putResourceWithQueryParam(vserverUri, vserverData);

        String pserverMockRequestUri =
                String.format("/cloud-infrastructure/pservers/pserver/%s?skip-related-to=true", hostname);
        String vserverMockRequestUri = String.format(
                "/cloud-infrastructure/cloud-regions/cloud-region/test-aic/%s/tenants/tenant/%s/vservers/vserver/%s?skip-related-to=true",
                cloudRegionId, tenant, vserver);

        // === GET - related-to-property should not exist ===
        // pserver
        String pserverResponse = getResponse(pserverMockRequestUri);
        assertTrue(isRelatedToPropertiesFieldNullInResponse(pserverResponse, "pserver"));

        // vserver
        String vserverResponse = getResponse(vserverMockRequestUri);
        assertTrue(isRelatedToPropertiesFieldNullInResponse(vserverResponse, "vserver"));
        // ===

        // === Clean up (DELETE) ===
        // vserver
        String deleteUri = String.format(
                "/cloud-infrastructure/cloud-regions/cloud-region/test-aic/%s/tenants/tenant/%s/vservers/vserver/%s",
                cloudRegionId, tenant, vserver);
        deleteServerObject(vserverMockRequestUri, deleteUri, "vserver");

        // pserver
        deleteUri = String.format("/cloud-infrastructure/pservers/pserver/%s", hostname);
        deleteServerObject(pserverMockRequestUri, deleteUri, "pserver");
    }

    @Test
    public void testGetWithoutSkipRelatedToParam() throws IOException, JSONException {
        String pserverData = getResourcePayload("pserver2");
        String vserverData = getResourcePayload("vserver2");

        String hostname = "pserver-hostname-test02";
        String cloudRegionId = "testAIC02";
        String tenant = "tenant02";
        String vserver = "vserver02";

        String pserverUri = String.format("/cloud-infrastructure/pservers/pserver/%s", hostname);
        String vserverUri = String.format("/cloud-infrastructure/cloud-regions/cloud-region/test-aic/%s", cloudRegionId);

        // PUT the resources
        doSetupResource(pserverUri, pserverData);
        doSetupResource(vserverUri, vserverData);

        String pserverMockRequestUri = String.format("/cloud-infrastructure/pservers/pserver/%s", hostname);
        String vserverMockRequestUri = String.format(
                "/cloud-infrastructure/cloud-regions/cloud-region/test-aic/%s/tenants/tenant/%s/vservers/vserver/%s",
                cloudRegionId, tenant, vserver);

        // === GET - related-to-property should not exist ===
        // pserver
        String pserverResponse = getResponse(pserverMockRequestUri);
        assertFalse(isRelatedToPropertiesFieldNullInResponse(pserverResponse, "pserver"));

        // vserver
        String vserverResponse = getResponse(vserverMockRequestUri);
        assertFalse(isRelatedToPropertiesFieldNullInResponse(vserverResponse, "vserver"));
        // ===

        // === Clean up (DELETE) ===
        // vserver
        String deleteUri = String.format(
                "/cloud-infrastructure/cloud-regions/cloud-region/test-aic/%s/tenants/tenant/%s/vservers/vserver/%s",
                cloudRegionId, tenant, vserver);
        deleteServerObject(vserverMockRequestUri, deleteUri, "vserver");

        // pserver
        deleteUri = String.format("/cloud-infrastructure/pservers/pserver/%s", hostname);
        deleteServerObject(pserverMockRequestUri, deleteUri, "pserver");
    }

    @Test
    public void testGetWithSkipRelatedToParamAndFormatResource() throws IOException, JSONException {
        String pserverData = getResourcePayload("pserver3");
        String vserverData = getResourcePayload("vserver3");

        String hostname = "pserver-hostname-test03";
        String cloudRegionId = "testAIC03";
        String tenant = "tenant03";
        String vserver = "vserver03";

        String pserverUri = String
                .format("/cloud-infrastructure/pservers/pserver/%s?skip-related-to=true&format=resource", hostname);
        String vserverUri = String.format(
                "/cloud-infrastructure/cloud-regions/cloud-region/test-aic/%s?skip-related-to=true&format=resource",
                cloudRegionId);

        // PUT the resources
        putResourceWithQueryParam(pserverUri, pserverData);
        putResourceWithQueryParam(vserverUri, vserverData);

        String pserverMockRequestUri = String
                .format("/cloud-infrastructure/pservers/pserver/%s?skip-related-to=true&format=resource", hostname);
        String vserverMockRequestUri = String.format(
                "/cloud-infrastructure/cloud-regions/cloud-region/test-aic/%s/tenants/tenant/%s/vservers/vserver/%s?skip-related-to=true&format=resource",
                cloudRegionId, tenant, vserver);

        // === GET - related-to-property should not exist ===
        // pserver
        String pserverResponse = getResponse(pserverMockRequestUri);
        assertTrue(isRelatedToPropertiesFieldNullInResponse(pserverResponse, "pserver"));

        // vserver
        String vserverResponse = getResponse(vserverMockRequestUri);
        assertTrue(isRelatedToPropertiesFieldNullInResponse(vserverResponse, "vserver"));
        // ===

        // === Clean up (DELETE) ===
        // vserver
        String deleteUri = String.format(
                "/cloud-infrastructure/cloud-regions/cloud-region/test-aic/%s/tenants/tenant/%s/vservers/vserver/%s",
                cloudRegionId, tenant, vserver);
        String vserverMockRequestUriNoFormat = String.format(
                "/cloud-infrastructure/cloud-regions/cloud-region/test-aic/%s/tenants/tenant/%s/vservers/vserver/%s",
                cloudRegionId, tenant, vserver);
        deleteServerObject(vserverMockRequestUriNoFormat, deleteUri, "vserver");

        // pserver
        deleteUri = String.format("/cloud-infrastructure/pservers/pserver/%s", hostname);
        deleteServerObject(pserverMockRequestUri, deleteUri, "pserver");
    }

    @Test
    public void testGetWithSkipRelatedToParamAndFormatResourceAndUrl() throws IOException, JSONException {
        String pserverData = getResourcePayload("pserver4");
        String vserverData = getResourcePayload("vserver4");

        String hostname = "pserver-hostname-test04";
        String cloudRegionId = "testAIC04";
        String tenant = "tenant04";
        String vserver = "vserver04";

        String pserverUri = String.format(
                "/cloud-infrastructure/pservers/pserver/%s?skip-related-to=true&format=resource_and_url", hostname);
        String vserverUri = String.format(
                "/cloud-infrastructure/cloud-regions/cloud-region/test-aic/%s?skip-related-to=true&format=resource_and_url",
                cloudRegionId);

        // PUT the resources
        putResourceWithQueryParam(pserverUri, pserverData);
        putResourceWithQueryParam(vserverUri, vserverData);

        String pserverMockRequestUri = String.format(
                "/cloud-infrastructure/pservers/pserver/%s?skip-related-to=true&format=resource_and_url", hostname);
        String vserverMockRequestUri = String.format(
                "/cloud-infrastructure/cloud-regions/cloud-region/test-aic/%s/tenants/tenant/%s/vservers/vserver/%s?skip-related-to=true&format=resource_and_url",
                cloudRegionId, tenant, vserver);

        // === GET - related-to-property should not exist ===
        // pserver
        String pserverResponse = getResponse(pserverMockRequestUri);
        assertTrue(isRelatedToPropertiesFieldNullInResponse(pserverResponse, "pserver"));

        // vserver
        String vserverResponse = getResponse(vserverMockRequestUri);
        assertTrue(isRelatedToPropertiesFieldNullInResponse(vserverResponse, "vserver"));
        // ===

        // === Clean up (DELETE) ===
        // vserver
        String deleteUri = String.format(
                "/cloud-infrastructure/cloud-regions/cloud-region/test-aic/%s/tenants/tenant/%s/vservers/vserver/%s",
                cloudRegionId, tenant, vserver);
        String vserverMockRequestUriNoFormat = String.format(
                "/cloud-infrastructure/cloud-regions/cloud-region/test-aic/%s/tenants/tenant/%s/vservers/vserver/%s",
                cloudRegionId, tenant, vserver);
        deleteServerObject(vserverMockRequestUriNoFormat, deleteUri, "vserver");

        // pserver
        deleteUri = String.format("/cloud-infrastructure/pservers/pserver/%s", hostname);
        deleteServerObject(pserverMockRequestUri, deleteUri, "pserver");
    }

    @Test
    public void testGetVserversWithSkipRelatedToParam() throws IOException, JSONException {
        String pserverData = getResourcePayload("pserver5");
        String vserverData = getResourcePayload("vserver5");

        String hostname = "pserver-hostname-test05";
        String cloudRegionId = "testAIC05";
        String tenant = "tenant05";
        String vserver = "vserver05";

        String pserverUri = String.format("/cloud-infrastructure/pservers/pserver/%s?skip-related-to=true", hostname);
        String vserverUri = String.format(
                "/cloud-infrastructure/cloud-regions/cloud-region/test-aic/%s?skip-related-to=true", cloudRegionId);

        // PUT the resources
        putResourceWithQueryParam(pserverUri, pserverData);
        putResourceWithQueryParam(vserverUri, vserverData);

        String pserverMockRequestUri =
                String.format("/cloud-infrastructure/pservers/pserver/%s?skip-related-to=true", hostname);
        String vserverMockRequestUri = String.format(
                "/cloud-infrastructure/cloud-regions/cloud-region/test-aic/%s/tenants/tenant/%s/vservers?vserver-selflink=somelink05&skip-related-to=true",
                cloudRegionId, tenant, vserver);

        // === GET - related-to-property should not exist ===
        // pserver
        String pserverResponse = getResponse(pserverMockRequestUri);
        assertTrue(isRelatedToPropertiesFieldNullInResponse(pserverResponse, "pserver"));

        // vserver
        String vserverResponse = getResponse(vserverMockRequestUri);
        assertTrue(isRelatedToPropertiesFieldNullInResponse(vserverResponse, "vserver"));
        // ===

        // === Clean up (DELETE) ===
        // vserver
        String deleteUri = String.format(
                "/cloud-infrastructure/cloud-regions/cloud-region/test-aic/%s/tenants/tenant/%s/vservers/vserver/%s",
                cloudRegionId, tenant, vserver);
        String vserverMockRequestUriNoFormat = String.format(
                "/cloud-infrastructure/cloud-regions/cloud-region/test-aic/%s/tenants/tenant/%s/vservers/vserver/%s",
                cloudRegionId, tenant, vserver);
        deleteServerObject(vserverMockRequestUriNoFormat, deleteUri, "vserver");

        // pserver
        deleteUri = String.format("/cloud-infrastructure/pservers/pserver/%s", hostname);
        deleteServerObject(pserverMockRequestUri, deleteUri, "pserver");
    }

    private String deleteServerObject(String mockUri, String deleteUri, String nodeType)
            throws IOException, JSONException {
        String serverEntity = getResponse(mockUri);

        JSONObject serverJsonObject = new JSONObject(serverEntity);
        boolean noResultsArray = false;
        String resourceVersion = "";
        try {
            JSONArray resultsJsonArray = (JSONArray) serverJsonObject.get("results");
            if (resultsJsonArray != null) {
                JSONObject servers = (JSONObject) resultsJsonArray.get(0);
                JSONObject server = (JSONObject) servers.get(nodeType);
                resourceVersion = server.getString("resource-version");
            }
        } catch (Exception e) {
            noResultsArray = true;
        }
        if (noResultsArray) {
            resourceVersion = serverJsonObject.getString("resource-version");
        }
        return webClient.delete()
            .uri(deleteUri + "?resource-version=" + resourceVersion)
            .exchange()
            .expectStatus()
            .isNoContent()
            .expectBody(String.class)
            .returnResult().getResponseBody();

        // MockHttpServletRequest mockReq = new MockHttpServletRequest("DELETE", deleteUri);
        // Response deleteResponse = resourcesController.delete(defaultSchemaVersion, deleteUri,
        //         httpHeaders, uriInfo, resourceVersion, mockReq);
        // return deleteResponse;
    }

    private void putResourceWithQueryParam(String uri, String payload) {

        String[] uriSplit = uri.split("\\?");
        if (uriSplit[1] != null && !uriSplit[1].isEmpty()) {
            String[] params;
            if (!uriSplit[1].contains("&")) {
                String param = uriSplit[1];
                params = new String[] {param};
            } else {
                params = uriSplit[1].split("&");
            }
            for (String param : params) {
                String[] splitParam = param.split("=");
                String key = splitParam[0];
                String value = splitParam[1];
                uriInfo.getQueryParameters().add(key, value);
            }
        }
        uri = uriSplit[0];

        when(uriInfo.getPath()).thenReturn(uri);
        when(uriInfo.getPath(false)).thenReturn(uri);

        MockHttpServletRequest mockReq = new MockHttpServletRequest("PUT", uri);
        Response response = resourcesController.update(payload, defaultSchemaVersion, uri,
                httpHeaders, uriInfo, mockReq);

        assertNotNull(response, "Response from the legacy moxy consumer returned null");
        int code = response.getStatus();
        if (!VALID_HTTP_STATUS_CODES.contains(code)) {
            logger.info("Response Code: " + code + "\tEntity: " + response.getEntity());
        }

        assertEquals(Response.Status.CREATED.getStatusCode(),
                response.getStatus(),
                "Expected to return status created from the response");
        logger.info("Response Code: " + code + "\tEntity: " + response.getEntity());
    }

    private String getResponse(String uri) {
        return webClient.get()
            .uri(uri)
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody(String.class)
            .returnResult().getResponseBody();
    }

    private boolean isRelatedToPropertiesFieldNullInResponse(String responseEntity, String nodeType)
            throws IOException, JSONException {
        boolean noResultsArray = false;
        JSONObject responseJsonObj = new JSONObject(responseEntity);
        try {
            JSONArray resultsJsonArray = (JSONArray) responseJsonObj.get("results");
            if (resultsJsonArray != null) {
                JSONObject servers = (JSONObject) resultsJsonArray.get(0);
                JSONObject server = (JSONObject) servers.get(nodeType);
                JSONObject relationshipList = (JSONObject) server.get("relationship-list");
                if (relationshipList != null) {
                    JSONArray relationship = (JSONArray) relationshipList.get("relationship");
                    if (relationship != null) {
                        JSONObject relationshipObj = relationship.getJSONObject(0);
                        JSONArray relatedToProperty = (JSONArray) relationshipObj.get("related-to-property");
                        if (relatedToProperty != null) {
                            return false;
                        }
                    }
                }
            }
        } catch (Exception e) {
            noResultsArray = true;
        }

        if (noResultsArray) {
            try {
                JSONObject relationshipList = (JSONObject) responseJsonObj.get("relationship-list");
                if (relationshipList != null) {
                    JSONArray relationship = (JSONArray) relationshipList.get("relationship");
                    if (relationship != null) {
                        JSONObject relationshipObj = relationship.getJSONObject(0);
                        JSONArray relatedToProperty = (JSONArray) relationshipObj.get("related-to-property");
                        if (relatedToProperty != null) {
                            return false;
                        }
                    }
                }
            } catch (JSONException je) {
                logger.info("JSON Exception Error: " + je);
            } catch (Exception e) {
                logger.info("JSON Exception Error: " + e);
            }
        }
        return true;
    }
}
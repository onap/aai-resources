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

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.json.JSONArray;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;
import org.onap.aai.AAISetup;
import org.onap.aai.dbmap.AAIGraph;
import org.onap.aai.exceptions.AAIException;

import org.onap.aai.util.AAIConfig;
import org.onap.aai.util.AAIConstants;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.annotation.DirtiesContext;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.*;
import java.io.IOException;
import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

// TODO: Change the following test to use spring boot
public class LegacyMoxyConsumerTest extends AAISetup {

    protected static final MediaType APPLICATION_JSON = MediaType.valueOf("application/json");

    private static final Set<Integer> VALID_HTTP_STATUS_CODES = new HashSet<>();

    static {
        VALID_HTTP_STATUS_CODES.add(200);
        VALID_HTTP_STATUS_CODES.add(201);
        VALID_HTTP_STATUS_CODES.add(204);
    }

    private LegacyMoxyConsumer legacyMoxyConsumer;

    private HttpHeaders httpHeaders;

    private UriInfo uriInfo;

    private MultivaluedMap<String, String> headersMultiMap;
    private MultivaluedMap<String, String> queryParameters;

    private List<String> aaiRequestContextList;

    private List<MediaType> outputMediaTypes;
    private boolean initialized = false;
    

    private static final Logger logger = LoggerFactory.getLogger(LegacyMoxyConsumerTest.class.getName());

    @BeforeClass
    public static void setupRest(){
      //  AAIGraph.getInstance();
    }

    @Before
    public void setup(){
    	if(!initialized){
    		initialized = true;
    		AAIGraph.getInstance();
    	}
        logger.info("Starting the setup for the integration tests of Rest Endpoints");

        legacyMoxyConsumer  = new LegacyMoxyConsumer();
        httpHeaders         = Mockito.mock(HttpHeaders.class);
        uriInfo             = Mockito.mock(UriInfo.class);

        headersMultiMap     = new MultivaluedHashMap<>();
        queryParameters     = Mockito.spy(new MultivaluedHashMap<>());

        headersMultiMap.add("X-FromAppId", "JUNIT");
        headersMultiMap.add("X-TransactionId", UUID.randomUUID().toString());
        headersMultiMap.add("Real-Time", "true");
        headersMultiMap.add("Accept", "application/json");
        headersMultiMap.add("aai-request-context", "");

        outputMediaTypes = new ArrayList<>();
        outputMediaTypes.add(APPLICATION_JSON);

        aaiRequestContextList = new ArrayList<>();
        aaiRequestContextList.add("");

        when(httpHeaders.getAcceptableMediaTypes()).thenReturn(outputMediaTypes);
        when(httpHeaders.getRequestHeaders()).thenReturn(headersMultiMap);

        when(httpHeaders.getRequestHeader("aai-request-context")).thenReturn(aaiRequestContextList);


        when(uriInfo.getQueryParameters()).thenReturn(queryParameters);
        when(uriInfo.getQueryParameters(false)).thenReturn(queryParameters);

        // TODO - Check if this is valid since RemoveDME2QueryParameters seems to be very unreasonable
        Mockito.doReturn(null).when(queryParameters).remove(anyObject());

        when(httpHeaders.getMediaType()).thenReturn(APPLICATION_JSON);


    }

    @Test
    public void testResponsePutGetDeleteOnResource() throws JSONException, IOException, AAIException {

        String uri = getUri();
        String payload = getResourcePayload(getObjectName());

        assertNotNull("Introspector returned invalid string when marshalling the object", payload);
        assertNotNull("Introspector failed to return a valid uri", uri);

        if(uri.length() != 0 && uri.charAt(0) == '/'){
            uri = uri.substring(1);
        }

        when(uriInfo.getPath()).thenReturn(uri);
        when(uriInfo.getPath(false)).thenReturn(uri);

        MockHttpServletRequest mockReqGet = new MockHttpServletRequest("GET", uri);
        Response response = legacyMoxyConsumer.getLegacy(
                "",
                null,
                null,
                schemaVersions.getDefaultVersion().toString(),
                uri,
                "all",
                "false",
                httpHeaders,
                uriInfo,
                mockReqGet
        );

        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());

        MockHttpServletRequest mockReq = new MockHttpServletRequest("PUT", uri);
        response = legacyMoxyConsumer.update(
                payload,
                schemaVersions.getDefaultVersion().toString(),
                uri,
                httpHeaders,
                uriInfo,
                mockReq
        );

        int code = response.getStatus();
        if(!VALID_HTTP_STATUS_CODES.contains(code)){
            logger.info("Response Code: " + code + "\tEntity: " +  response.getEntity());
        }

        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());

        queryParameters.add("depth", "10000");

        response = legacyMoxyConsumer.getLegacy(
                "",
                null,
                null,
                schemaVersions.getDefaultVersion().toString(),
                uri,
                "10000",
                "false",
                httpHeaders,
                uriInfo,
                mockReqGet
        );

        code = response.getStatus();
        if(!VALID_HTTP_STATUS_CODES.contains(code)){
            logger.info("Response Code: " + code + "\tEntity: " +  response.getEntity());
        }

        String pserverEntity        = response.getEntity().toString();
        JSONObject pserverJsonbject = new JSONObject(pserverEntity);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        JSONAssert.assertEquals(payload, pserverEntity, false);

        String resourceVersion = pserverJsonbject.getString("resource-version");

        queryParameters.add("resource-version", resourceVersion);

        mockReq = new MockHttpServletRequest("DELETE", uri);
        response = legacyMoxyConsumer.delete(
                schemaVersions.getDefaultVersion().toString(),
                uri,
                httpHeaders,
                uriInfo,
                "",
                mockReq
        );

        code = response.getStatus();
        if(!VALID_HTTP_STATUS_CODES.contains(code)){
            logger.info("Response Code: " + code + "\tEntity: " +  response.getEntity());
        }

        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());

        response = legacyMoxyConsumer.getLegacy(
                "",
                null,
                null,
                schemaVersions.getDefaultVersion().toString(),
                uri,
                "all",
                "false",
                httpHeaders,
                uriInfo,
                mockReqGet
        );

        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
    }

    @Test
    public void testResponseGetOnResourcePaginated() throws JSONException, IOException, AAIException {

        String uri = getGetAllPserversURI();

        if(uri.length() != 0 && uri.charAt(0) == '/'){
            uri = uri.substring(1);
        }

        when(uriInfo.getPath()).thenReturn(uri);
        when(uriInfo.getPath(false)).thenReturn(uri);

        MockHttpServletRequest mockReqGet = new MockHttpServletRequest("GET", uri);
        Response response = legacyMoxyConsumer.getLegacy(
                "",
                "1",
                "10",
                schemaVersions.getDefaultVersion().toString(),
                uri,
                "all",
                "false",
                httpHeaders,
                uriInfo,
                mockReqGet
        );
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    }

    @Test
    public void testPutPserverAndCloudRegionRelationship() throws IOException, JSONException {

        String pserverData = getRelationshipPayload("pserver");
        String complexData = getRelationshipPayload("complex");

        String hostname = "590a8943-1200-43b3-825b-75dde6b8f44a";
        String physicalLocationId ="e13d4587-19ad-4bf5-80f5-c021efb5b61c";

        String pserverUri = String.format("cloud-infrastructure/pservers/pserver/%s", hostname);
        String cloudRegionUri = String.format("cloud-infrastructure/complexes/complex/%s", physicalLocationId);

        doSetupResource(pserverUri, pserverData);
        doSetupResource(cloudRegionUri, complexData);

        String cloudToPserverRelationshipData = getRelationshipPayload("pserver-complex-relationship");
        String cloudToPserverRelationshipUri = String.format(
                "cloud-infrastructure/pservers/pserver/%s/relationship-list/relationship", hostname);
        MockHttpServletRequest mockReq = new MockHttpServletRequest("PUT", cloudToPserverRelationshipUri);
        Response response = legacyMoxyConsumer.updateRelationship(
                cloudToPserverRelationshipData,
                schemaVersions.getDefaultVersion().toString(),
                cloudToPserverRelationshipUri,
                httpHeaders,
                uriInfo,
                mockReq
        );

        assertNotNull("Response from the legacy moxy consumer returned null", response);
        int code = response.getStatus();
        if(!VALID_HTTP_STATUS_CODES.contains(code)){
            logger.info("Response Code: " + code + "\tEntity: " +  response.getEntity());
        }

        assertEquals("Expected to return status created from the response",
                Response.Status.OK.getStatusCode(), response.getStatus());
        logger.info("Response Code: " + code + "\tEntity: " +  response.getEntity());

        // TODO - Need to actually verify the relationship between pserver and cloud-region
        mockReq = new MockHttpServletRequest("DELETE", cloudToPserverRelationshipUri);
        response = legacyMoxyConsumer.deleteRelationship(
                cloudToPserverRelationshipData,
                schemaVersions.getDefaultVersion().toString(),
                cloudToPserverRelationshipUri,
                httpHeaders,
                uriInfo,
                mockReq
        );

        code = response.getStatus();
        if(!VALID_HTTP_STATUS_CODES.contains(code)){
            logger.info("Response Code: " + code + "\tEntity: " +  response.getEntity());
        }

        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());
    }

    @Test
    public void testPutPassWithEmptyData() throws JSONException {

        String payload = "{}";
        String pserverUri = String.format("cloud-infrastructure/pservers/pserver/%s", UUID.randomUUID().toString());

        doSetupResource(pserverUri, payload);

        payload = "";
        pserverUri = String.format("cloud-infrastructure/pservers/pserver/%s", UUID.randomUUID().toString());
        doSetupResource(pserverUri, payload);
    }

    @Test
    public void testFailureWithInvalidUri() throws JSONException {

        String payload = "{}";
        String uri = "fake-infrastructure/pservers/pserver/fajsidj";

        when(uriInfo.getPath()).thenReturn(uri);
        when(uriInfo.getPath(false)).thenReturn(uri);
        MockHttpServletRequest mockReq = new MockHttpServletRequest("PUT", uri);
        Response response = legacyMoxyConsumer.update(
                payload,
                schemaVersions.getDefaultVersion().toString(),
                uri,
                httpHeaders,
                uriInfo,
                mockReq
        );

        int code = response.getStatus();
        logger.info("Response Code: " + code + "\tEntity: " +  response.getEntity());
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), code);
    }

    @Test
    public void testInvalidUriThrowRandomException() throws JSONException {

        String payload = "{}";
        String uri = "fake-infrastructure/pservers/pserver/fajsidj";

        when(uriInfo.getPath()).thenReturn(uri);
        when(uriInfo.getPath(false)).thenThrow(new IllegalArgumentException());
        MockHttpServletRequest mockReq = new MockHttpServletRequest("PUT", uri);
        Response response = legacyMoxyConsumer.update(
                payload,
                schemaVersions.getDefaultVersion().toString(),
                uri,
                httpHeaders,
                uriInfo,
                mockReq
        );

        int code = response.getStatus();
        assertNotNull("Response from the legacy moxy consumer returned null", response);
        assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), code);
        logger.info("Response Code: " + code + "\tEntity: " +  response.getEntity());

        response = legacyMoxyConsumer.updateRelationship(
                payload,
                schemaVersions.getDefaultVersion().toString(),
                uri,
                httpHeaders,
                uriInfo,
                mockReq
        );

        code = response.getStatus();
        assertNotNull("Response from the legacy moxy consumer returned null", response);
        assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), code);
        logger.info("Response Code: " + code + "\tEntity: " +  response.getEntity());
        mockReq = new MockHttpServletRequest("GET", uri);
        response = legacyMoxyConsumer.getLegacy(
                "",
                null,
                null,
                schemaVersions.getDefaultVersion().toString(),
                uri,
                "all",
                "false",
                httpHeaders,
                uriInfo,
                mockReq
        );

        assertNotNull("Response from the legacy moxy consumer returned null", response);
        assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), code);
        mockReq = new MockHttpServletRequest("DELETE", uri);
        response = legacyMoxyConsumer.delete(
                schemaVersions.getDefaultVersion().toString(),
                uri,
                httpHeaders,
                uriInfo,
                "",
                mockReq
        );

        code = response.getStatus();
        assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), code);

        response = legacyMoxyConsumer.deleteRelationship(
                payload,
                schemaVersions.getDefaultVersion().toString(),
                uri,
                httpHeaders,
                uriInfo,
                mockReq
        );
        code = response.getStatus();
        assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), code);
    }

    @Test
    public void testInvalidUriContainingRelatedToShouldThrowAAIException() throws JSONException {

        String payload = "{}";
        String uri = "cloud-infrastructure/related-to/fsdf";

        when(uriInfo.getPath()).thenReturn(uri);
        when(uriInfo.getPath(false)).thenReturn(uri);
        MockHttpServletRequest mockReq = new MockHttpServletRequest("PUT", uri);
        Response response = legacyMoxyConsumer.update(
                payload,
                schemaVersions.getDefaultVersion().toString(),
                uri,
                httpHeaders,
                uriInfo,
                mockReq
        );

        int code = response.getStatus();
        logger.info("Response Code: " + code + "\tEntity: " +  response.getEntity());
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), code);

        response = legacyMoxyConsumer.updateRelationship(
                payload,
                schemaVersions.getDefaultVersion().toString(),
                uri,
                httpHeaders,
                uriInfo,
                mockReq
        );

        code = response.getStatus();
        logger.info("Response Code: " + code + "\tEntity: " +  response.getEntity());
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), code);

        mockReq = new MockHttpServletRequest("GET", uri);
        response = legacyMoxyConsumer.getLegacy(
                "",
                null,
                null,
                schemaVersions.getDefaultVersion().toString(),
                uri,
                "all",
                "false",
                httpHeaders,
                uriInfo,
                mockReq
        );

        assertNotNull("Response from the legacy moxy consumer returned null", response);
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), code);

        mockReq = new MockHttpServletRequest("DELETE", uri);
        queryParameters.add("resource-version", "3434394839483");
        response = legacyMoxyConsumer.delete(
                schemaVersions.getDefaultVersion().toString(),
                uri,
                httpHeaders,
                uriInfo,
                "",
                mockReq
        );

        code = response.getStatus();
        logger.info("Response Code: " + code + "\tEntity: " +  response.getEntity());
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), code);

        response = legacyMoxyConsumer.deleteRelationship(
                payload,
                schemaVersions.getDefaultVersion().toString(),
                uri,
                httpHeaders,
                uriInfo,
                mockReq
        );
        code = response.getStatus();
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), code);
    }

    @Test
    @Ignore("Unable to test this method due to WRITE_BIGDECIMAL_AS_PLAIN error")
    public void testPatchWithValidData() throws IOException {

        String payload = getResourcePayload("pserver-patch-test");
        String uri     = getUri("pserver-patch-test");

        if(uri.length() != 0 && uri.charAt(0) == '/'){
            uri = uri.substring(1);
        }

        when(uriInfo.getPath()).thenReturn(uri);
        when(uriInfo.getPath(false)).thenReturn(uri);

        MockHttpServletRequest mockReq = new MockHttpServletRequest("GET", uri);
        Response response = legacyMoxyConsumer.getLegacy(
                "",
                null,
                null,
                schemaVersions.getDefaultVersion().toString(),
                uri,
                "all",
                "false",
                httpHeaders,
                uriInfo,
                mockReq
        );

        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
        mockReq = new MockHttpServletRequest("PUT", uri);
        response = legacyMoxyConsumer.update(
                payload,
                schemaVersions.getDefaultVersion().toString(),
                uri,
                httpHeaders,
                uriInfo,
                mockReq
        );

        int code = response.getStatus();
        if(!VALID_HTTP_STATUS_CODES.contains(code)){
            logger.info("Response Code: " + code + "\tEntity: " +  response.getEntity());
        }

        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());

        String patchData = "{\"in-maint\": false}";

        headersMultiMap.add("Content-Type", "application/json");

        outputMediaTypes.remove(APPLICATION_JSON);
        outputMediaTypes.add(MediaType.valueOf("application/merge-patch+json"));

        mockReq = new MockHttpServletRequest("PATCH", uri);
        response = legacyMoxyConsumer.patch(
                patchData,
                schemaVersions.getDefaultVersion().toString(),
                uri,
                httpHeaders,
                uriInfo,
                mockReq
        );

        code = response.getStatus();
        assertNotNull("Response from the patch returned null", response);
        logger.info("Response Code: " + code + "\tEntity: " +  response.getEntity());
        assertEquals(Response.Status.OK.getStatusCode(), code);

    }

    protected void doSetupResource(String uri, String payload) throws JSONException {

        when(uriInfo.getPath()).thenReturn(uri);
        when(uriInfo.getPath(false)).thenReturn(uri);

        HttpServletRequest mockRequest = Mockito.mock(HttpServletRequest.class);
        when(mockRequest.getRequestURL()).thenReturn(new StringBuffer("https://localhost:8447/aai/v15/" + uri));

        Response response = legacyMoxyConsumer.getLegacy(
                "",
                null,
                null,
                schemaVersions.getDefaultVersion().toString(),
                uri,
                "all",
                "false",
                httpHeaders,
                uriInfo,
                mockRequest
        );

        assertNotNull("Response from the legacy moxy consumer returned null", response);
        assertEquals("Expected to not have the data already in memory",
                Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());

        response = legacyMoxyConsumer.update(
                payload,
                schemaVersions.getDefaultVersion().toString(),
                uri,
                httpHeaders,
                uriInfo,
                mockRequest
        );

        assertNotNull("Response from the legacy moxy consumer returned null", response);
        int code = response.getStatus();
        if(!VALID_HTTP_STATUS_CODES.contains(code)){
            logger.info("Response Code: " + code + "\tEntity: " +  response.getEntity());
        }
        assertEquals("Expected to return status created from the response",
                Response.Status.CREATED.getStatusCode(), response.getStatus());

        queryParameters.add("depth", "10000");
        response = legacyMoxyConsumer.getLegacy(
                "",
                null,
                null,
                schemaVersions.getDefaultVersion().toString(),
                uri,
                "all",
                "false",
                httpHeaders,
                uriInfo,
                mockRequest
        );

        assertNotNull("Response from the legacy moxy consumer returned null", response);
        assertEquals("Expected to return the pserver data that was just put in memory",
                Response.Status.OK.getStatusCode(), response.getStatus());

        if("".equalsIgnoreCase(payload)){
            payload = "{}";
        }

        JSONAssert.assertEquals(payload, response.getEntity().toString(), false);
    }

    @Test
    public void testDeleteRelationshipThrowsException(){

        String payload = "";
        String hostname = "testData";
        String uri = String.format("cloud-infrastructure/pservers/pserver/%s/relationship-list/relationship", hostname);

        when(uriInfo.getPath()).thenReturn(uri);
        when(uriInfo.getPath(false)).thenReturn(uri);

        MockHttpServletRequest mockReq = new MockHttpServletRequest("DELETE", uri);
        Response response = legacyMoxyConsumer.deleteRelationship(
                payload,
                schemaVersions.getDefaultVersion().toString(),
                uri,
                httpHeaders,
                uriInfo,
                mockReq
        );

        int code = response.getStatus();
        logger.info("Response Code: " + code + "\tEntity: " +  response.getEntity());
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), code);
    }

    // TODO - Change this to be abstract and inheritable
    public String getObjectName(){
        return "pserver";
    }

    public String getResourcePayload(String resourceName) throws IOException {
        return getPayload("payloads/resource/" + resourceName + ".json");
    }

    public String getRelationshipPayload(String relationshipName) throws IOException {
        return getPayload("payloads/relationship/" + relationshipName + ".json");
    }

    public String getUri(String hostname){
        return String.format("cloud-infrastructure/pservers/pserver/%s", hostname);
    }
    public String getGetAllPserversURI(){
        return "cloud-infrastructure/pservers";
    }


    public String getUri(){
        return getUri("pserver-hostname-test");
    }

    @Test
    public void legacyMoxyCheckTimeoutEnabled() throws Exception{
        boolean isTimeoutEnabled = legacyMoxyConsumer.isTimeoutEnabled("JUNITTESTAPP1", AAIConfig.get(AAIConstants.AAI_CRUD_TIMEOUT_ENABLED), AAIConfig.get(AAIConstants.AAI_CRUD_TIMEOUT_APP), AAIConfig.get(AAIConstants.AAI_CRUD_TIMEOUT_LIMIT));
        assertEquals(true, isTimeoutEnabled);
    }

    @Test
    public void legacyMoxyCheckTimeoutEnabledOverride() throws Exception{
        boolean isTimeoutEnabled = legacyMoxyConsumer.isTimeoutEnabled("JUNITTESTAPP2", AAIConfig.get(AAIConstants.AAI_CRUD_TIMEOUT_ENABLED), AAIConfig.get(AAIConstants.AAI_CRUD_TIMEOUT_APP), AAIConfig.get(AAIConstants.AAI_CRUD_TIMEOUT_LIMIT));
        assertEquals(false, isTimeoutEnabled);
    }

    @Test
    public void legacyMoxyCheckTimeoutEnabledDefaultLimit() throws Exception{
        boolean isTimeoutEnabled = legacyMoxyConsumer.isTimeoutEnabled("JUNITTESTAPP3", AAIConfig.get(AAIConstants.AAI_CRUD_TIMEOUT_ENABLED), AAIConfig.get(AAIConstants.AAI_CRUD_TIMEOUT_APP), AAIConfig.get(AAIConstants.AAI_CRUD_TIMEOUT_LIMIT));
        assertEquals(true, isTimeoutEnabled);
        int timeout = legacyMoxyConsumer.getTimeoutLimit("JUNITTESTAPP3", AAIConfig.get(AAIConstants.AAI_CRUD_TIMEOUT_APP), AAIConfig.get(AAIConstants.AAI_CRUD_TIMEOUT_LIMIT));
        assertEquals(100000, timeout);
    }

    @Test
    public void legacyMoxyGetTimeout() throws Exception{
        int timeout = legacyMoxyConsumer.getTimeoutLimit("JUNITTESTAPP1", AAIConfig.get(AAIConstants.AAI_CRUD_TIMEOUT_APP), AAIConfig.get(AAIConstants.AAI_CRUD_TIMEOUT_LIMIT));
        assertEquals(1, timeout);
    }

    @Test
    public void legacyMoxyGetTimeoutOverride() throws Exception{
        int timeout = legacyMoxyConsumer.getTimeoutLimit("JUNITTESTAPP2", AAIConfig.get(AAIConstants.AAI_CRUD_TIMEOUT_APP), AAIConfig.get(AAIConstants.AAI_CRUD_TIMEOUT_LIMIT));
        assertEquals(-1, timeout);
    }
    @Ignore("Time sensitive test only times out if the response takes longer than 1 second")
    @Test
    public void testTimeoutGetCall() throws Exception{
        String uri = getUri();

        if(uri.length() != 0 && uri.charAt(0) == '/'){
            uri = uri.substring(1);
        }

        when(uriInfo.getPath()).thenReturn(uri);
        when(uriInfo.getPath(false)).thenReturn(uri);
        headersMultiMap.putSingle("X-FromAppId", "JUNITTESTAPP1");
        when(httpHeaders.getRequestHeaders()).thenReturn(headersMultiMap);

        MockHttpServletRequest mockReqGet = new MockHttpServletRequest("GET", uri);
        Response response = legacyMoxyConsumer.getLegacy(
                "",
                null,
                null,
                schemaVersions.getDefaultVersion().toString(),
                uri,
                "all",
                "false",
                httpHeaders,
                uriInfo,
                mockReqGet
        );

        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }
    @Test
    public void testBypassTimeoutGetCall() throws Exception{
        String uri = getUri();

        if(uri.length() != 0 && uri.charAt(0) == '/'){
            uri = uri.substring(1);
        }

        when(uriInfo.getPath()).thenReturn(uri);
        when(uriInfo.getPath(false)).thenReturn(uri);
        headersMultiMap.putSingle("X-FromAppId", "JUNITTESTAPP2");
        when(httpHeaders.getRequestHeaders()).thenReturn(headersMultiMap);

        MockHttpServletRequest mockReqGet = new MockHttpServletRequest("GET", uri);
        Response response = legacyMoxyConsumer.getLegacy(
                "",
                null,
                null,
                schemaVersions.getDefaultVersion().toString(),
                uri,
                "all",
                "false",
                httpHeaders,
                uriInfo,
                mockReqGet
        );

        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
    }

    @Test
    public void testGetRelationshipWithoutFormat() throws IOException, JSONException {
        String payload = getRelationshipPayload("pserver-complex-relationship-list2");
        String pserverData = getRelationshipPayload("pserver2");
        String complexData = getRelationshipPayload("complex2");

        String hostname = "590a8943-1200-43b3-825b-75dde6b8f44b";
        String physicalLocationId ="e13d4587-19ad-4bf5-80f5-c021efb5b61d";

        String pserverUri = String.format("cloud-infrastructure/pservers/pserver/%s", hostname);
        String cloudRegionUri = String.format("cloud-infrastructure/complexes/complex/%s", physicalLocationId);

        doSetupResource(pserverUri, pserverData);
        doSetupResource(cloudRegionUri, complexData);

        String cloudToPserverRelationshipData = getRelationshipPayload("pserver-complex-relationship2");
        String cloudToPserverRelationshipUri = String.format(
                "cloud-infrastructure/pservers/pserver/%s/relationship-list/relationship", hostname);
        MockHttpServletRequest mockReq = new MockHttpServletRequest("PUT", cloudToPserverRelationshipUri);
        Response response = legacyMoxyConsumer.updateRelationship(
                cloudToPserverRelationshipData,
                schemaVersions.getDefaultVersion().toString(),
                cloudToPserverRelationshipUri,
                httpHeaders,
                uriInfo,
                mockReq
        );

        assertNotNull("Response from the legacy moxy consumer returned null", response);
        int code = response.getStatus();
        if(!VALID_HTTP_STATUS_CODES.contains(code)){
            logger.info("Response Code: " + code + "\tEntity: " +  response.getEntity());
        }

        assertEquals("Expected to return status created from the response",
                Response.Status.OK.getStatusCode(), response.getStatus());
        logger.info("Response Code: " + code + "\tEntity: " +  response.getEntity());

        String getRelationshipMockRequestUri = String.format(
                "cloud-infrastructure/pservers/pserver/%s/relationship-list", hostname);
        String getRelationshipUri = String.format(
                "cloud-infrastructure/pservers/pserver/%s", hostname);
        HttpServletRequest mockRequest = Mockito.mock(HttpServletRequest.class);
        when(mockRequest.getRequestURL()).thenReturn(new StringBuffer("https://localhost:8447/aai/v15/" + getRelationshipUri));
        response = legacyMoxyConsumer.getRelationshipList(
                "1",
                "1",
                schemaVersions.getDefaultVersion().toString(),
                getRelationshipUri,
                "false",
                httpHeaders,
                mockRequest,
                uriInfo
        );

        String s = response.getEntity().toString();

        code = response.getStatus();
        if(!VALID_HTTP_STATUS_CODES.contains(code)){
            logger.info("Response Code: " + code + "\tEntity: " +  response.getEntity());
        }

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        JSONAssert.assertEquals(payload, response.getEntity().toString(), false);
    }

    @Test
    public void testGetRelationshipWithFormat() throws IOException, JSONException, ParseException {
        String payload = getRelationshipPayload("pserver-complex-relationship-list3");
        String pserverData = getRelationshipPayload("pserver3");
        String complexData = getRelationshipPayload("complex3");

        String hostname = "590a8943-1200-43b3-825b-75dde6b8f44c";
        String physicalLocationId ="e13d4587-19ad-4bf5-80f5-c021efb5b61e";

        String pserverUri = String.format("cloud-infrastructure/pservers/pserver/%s", hostname);
        String cloudRegionUri = String.format("cloud-infrastructure/complexes/complex/%s", physicalLocationId);

        doSetupResource(pserverUri, pserverData);
        doSetupResource(cloudRegionUri, complexData);

        String cloudToPserverRelationshipData = getRelationshipPayload("pserver-complex-relationship3");
        String cloudToPserverRelationshipUri = String.format(
                "cloud-infrastructure/pservers/pserver/%s/relationship-list/relationship", hostname);
        MockHttpServletRequest mockReq = new MockHttpServletRequest("PUT", cloudToPserverRelationshipUri);
        Response response = legacyMoxyConsumer.updateRelationship(
                cloudToPserverRelationshipData,
                schemaVersions.getDefaultVersion().toString(),
                cloudToPserverRelationshipUri,
                httpHeaders,
                uriInfo,
                mockReq
        );

        assertNotNull("Response from the legacy moxy consumer returned null", response);
        int code = response.getStatus();
        if(!VALID_HTTP_STATUS_CODES.contains(code)){
            logger.info("Response Code: " + code + "\tEntity: " +  response.getEntity());
        }

        assertEquals("Expected to return status created from the response",
                Response.Status.OK.getStatusCode(), response.getStatus());
        logger.info("Response Code: " + code + "\tEntity: " +  response.getEntity());

        String getRelationshipMockRequestUri = String.format(
                "cloud-infrastructure/pservers/pserver/%s/relationship-list", hostname);
        String getRelationshipUri = String.format(
                "cloud-infrastructure/pservers/pserver/%s", hostname);
        queryParameters.add("format", "resource");
        HttpServletRequest mockRequest = Mockito.mock(HttpServletRequest.class);
        when(mockRequest.getRequestURL()).thenReturn(new StringBuffer("https://localhost:8447/aai/v15/" + getRelationshipUri));
        response = legacyMoxyConsumer.getRelationshipList(
                "1",
                "1",
                schemaVersions.getDefaultVersion().toString(),
                getRelationshipUri,
                "false",
                httpHeaders,
                mockRequest,
                uriInfo
        );
        queryParameters.remove("format");

        code = response.getStatus();
        if(!VALID_HTTP_STATUS_CODES.contains(code)){
            logger.info("Response Code: " + code + "\tEntity: " +  response.getEntity());
        }

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        String responsePayload = response.getEntity().toString();
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
        String physicalLocationId ="e13d4587-19ad-4bf5-80f5-c021efb5b61f";

        String pserverUri = String.format("cloud-infrastructure/pservers/pserver/%s", hostname);
        String cloudRegionUri = String.format("cloud-infrastructure/complexes/complex/%s", physicalLocationId);

        doSetupResource(pserverUri, pserverData);
        doSetupResource(cloudRegionUri, complexData);

        String getRelationshipMockRequestUri = String.format(
                "cloud-infrastructure/pservers/pserver/%s/relationship-list", hostname);
        String getRelationshipUri = String.format(
                "cloud-infrastructure/pservers/pserver/%s", hostname);
        MockHttpServletRequest mockReq = new MockHttpServletRequest("GET_RELATIONSHIP", getRelationshipMockRequestUri);
        HttpServletRequest mockRequest = Mockito.mock(HttpServletRequest.class);
        when(mockRequest.getRequestURL()).thenReturn(new StringBuffer("https://localhost:8447/aai/v15/" + getRelationshipUri));
        Response response = legacyMoxyConsumer.getRelationshipList(
                "1",
                "1",
                schemaVersions.getDefaultVersion().toString(),
                getRelationshipUri,
                "false",
                httpHeaders,
                mockRequest,
                uriInfo
        );

        int code = response.getStatus();
        if(!VALID_HTTP_STATUS_CODES.contains(code)){
            logger.info("Response Code: " + code + "\tEntity: " +  response.getEntity());
        }

        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
    }

    @Test
    public void testGetWithSkipRelatedToParam() throws IOException, JSONException {
        String pserverData = getResourcePayload("pserver1");
        String vserverData = getResourcePayload("vserver1");

        String hostname = "pserver-hostname-test01";
        String cloudRegionId ="testAIC01";
        String tenant ="tenant01";
        String vserver ="vserver01";

        String pserverUri = String.format("cloud-infrastructure/pservers/pserver/%s?skip-related-to=true", hostname);
        String vserverUri = String.format("cloud-infrastructure/cloud-regions/cloud-region/test-aic/%s?skip-related-to=true", cloudRegionId);

        // PUT the resources
        putResourceWithQueryParam(pserverUri, pserverData);
        putResourceWithQueryParam(vserverUri, vserverData);

        String pserverMockRequestUri = String.format("cloud-infrastructure/pservers/pserver/%s?skip-related-to=true", hostname);
        String vserverMockRequestUri = String.format("cloud-infrastructure/cloud-regions/cloud-region/test-aic/%s/tenants/tenant/%s/vservers/vserver/%s?skip-related-to=true",
                cloudRegionId, tenant, vserver);

        // === GET - related-to-property should not exist ===
        // pserver
        Response pserverResponse = getMockResponse(pserverMockRequestUri);
        assertTrue(isRelatedToPropertiesFieldNullInResponse(pserverResponse, "pserver"));

        // vserver
        Response vserverResponse = getMockResponse(vserverMockRequestUri);
        assertTrue(isRelatedToPropertiesFieldNullInResponse(vserverResponse, "vserver"));
        // ===

        // === Clean up (DELETE) ===
        // vserver
        String deleteUri = String.format("cloud-infrastructure/cloud-regions/cloud-region/test-aic/%s/tenants/tenant/%s/vservers/vserver/%s",
                cloudRegionId, tenant, vserver);
        Response response = deleteServerObject(vserverMockRequestUri, deleteUri, "vserver");
        int code = response.getStatus();
        if(!VALID_HTTP_STATUS_CODES.contains(code)){
            logger.info("Response Code: " + code + "\tEntity: " +  response.getEntity());
        }
        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());

        // pserver
        deleteUri = String.format("cloud-infrastructure/pservers/pserver/%s", hostname);
        response = deleteServerObject(pserverMockRequestUri, deleteUri, "pserver");
        code = response.getStatus();
        if(!VALID_HTTP_STATUS_CODES.contains(code)){
            logger.info("Response Code: " + code + "\tEntity: " +  response.getEntity());
        }
        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());
    }

    @Test
    public void testGetWithoutSkipRelatedToParam() throws IOException, JSONException {
        String pserverData = getResourcePayload("pserver2");
        String vserverData = getResourcePayload("vserver2");

        String hostname = "pserver-hostname-test02";
        String cloudRegionId ="testAIC02";
        String tenant ="tenant02";
        String vserver ="vserver02";

        String pserverUri = String.format("cloud-infrastructure/pservers/pserver/%s", hostname);
        String vserverUri = String.format("cloud-infrastructure/cloud-regions/cloud-region/test-aic/%s", cloudRegionId);

        // PUT the resources
        doSetupResource(pserverUri, pserverData);
        doSetupResource(vserverUri, vserverData);

        String pserverMockRequestUri = String.format("cloud-infrastructure/pservers/pserver/%s", hostname);
        String vserverMockRequestUri = String.format("cloud-infrastructure/cloud-regions/cloud-region/test-aic/%s/tenants/tenant/%s/vservers/vserver/%s",
                cloudRegionId, tenant, vserver);

        // === GET - related-to-property should not exist ===
        // pserver
        Response pserverResponse = getMockResponse(pserverMockRequestUri);
        assertFalse(isRelatedToPropertiesFieldNullInResponse(pserverResponse, "pserver"));

        // vserver
        Response vserverResponse = getMockResponse(vserverMockRequestUri);
        assertFalse(isRelatedToPropertiesFieldNullInResponse(vserverResponse, "vserver"));
        // ===

        // === Clean up (DELETE) ===
        // vserver
        String deleteUri = String.format("cloud-infrastructure/cloud-regions/cloud-region/test-aic/%s/tenants/tenant/%s/vservers/vserver/%s",
                cloudRegionId, tenant, vserver);
        Response response = deleteServerObject(vserverMockRequestUri, deleteUri, "vserver");
        int code = response.getStatus();
        if(!VALID_HTTP_STATUS_CODES.contains(code)){
            logger.info("Response Code: " + code + "\tEntity: " +  response.getEntity());
        }
        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());

        // pserver
        deleteUri = String.format("cloud-infrastructure/pservers/pserver/%s", hostname);
        response = deleteServerObject(pserverMockRequestUri, deleteUri, "pserver");
        code = response.getStatus();
        if(!VALID_HTTP_STATUS_CODES.contains(code)){
            logger.info("Response Code: " + code + "\tEntity: " +  response.getEntity());
        }
        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());
    }

    @Test
    public void testGetWithSkipRelatedToParamAndFormatResource() throws IOException, JSONException {
        String pserverData = getResourcePayload("pserver3");
        String vserverData = getResourcePayload("vserver3");

        String hostname = "pserver-hostname-test03";
        String cloudRegionId ="testAIC03";
        String tenant ="tenant03";
        String vserver ="vserver03";

        String pserverUri = String.format("cloud-infrastructure/pservers/pserver/%s?skip-related-to=true&format=resource", hostname);
        String vserverUri = String.format("cloud-infrastructure/cloud-regions/cloud-region/test-aic/%s?skip-related-to=true&format=resource", cloudRegionId);

        // PUT the resources
        putResourceWithQueryParam(pserverUri, pserverData);
        putResourceWithQueryParam(vserverUri, vserverData);

        String pserverMockRequestUri = String.format("cloud-infrastructure/pservers/pserver/%s?skip-related-to=true&format=resource", hostname);
        String vserverMockRequestUri = String.format("cloud-infrastructure/cloud-regions/cloud-region/test-aic/%s/tenants/tenant/%s/vservers/vserver/%s?skip-related-to=true&format=resource",
                cloudRegionId, tenant, vserver);

        // === GET - related-to-property should not exist ===
        // pserver
        Response pserverResponse = getMockResponse(pserverMockRequestUri);
        assertTrue(isRelatedToPropertiesFieldNullInResponse(pserverResponse, "pserver"));

        // vserver
        Response vserverResponse = getMockResponse(vserverMockRequestUri);
        assertTrue(isRelatedToPropertiesFieldNullInResponse(vserverResponse, "vserver"));
        // ===

        // === Clean up (DELETE) ===
        // vserver
        String deleteUri = String.format("cloud-infrastructure/cloud-regions/cloud-region/test-aic/%s/tenants/tenant/%s/vservers/vserver/%s",
                cloudRegionId, tenant, vserver);
        String vserverMockRequestUriNoFormat = String.format("cloud-infrastructure/cloud-regions/cloud-region/test-aic/%s/tenants/tenant/%s/vservers/vserver/%s",
                cloudRegionId, tenant, vserver);
        Response response = deleteServerObject(vserverMockRequestUriNoFormat, deleteUri, "vserver");
        int code = response.getStatus();
        if(!VALID_HTTP_STATUS_CODES.contains(code)){
            logger.info("Response Code: " + code + "\tEntity: " +  response.getEntity());
        }
        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());

        // pserver
        deleteUri = String.format("cloud-infrastructure/pservers/pserver/%s", hostname);
        response = deleteServerObject(pserverMockRequestUri, deleteUri, "pserver");
        code = response.getStatus();
        if(!VALID_HTTP_STATUS_CODES.contains(code)){
            logger.info("Response Code: " + code + "\tEntity: " +  response.getEntity());
        }
        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());

    }

    @Test
    public void testGetWithSkipRelatedToParamAndFormatResourceAndUrl() throws IOException, JSONException {
        String pserverData = getResourcePayload("pserver4");
        String vserverData = getResourcePayload("vserver4");

        String hostname = "pserver-hostname-test04";
        String cloudRegionId ="testAIC04";
        String tenant ="tenant04";
        String vserver ="vserver04";

        String pserverUri = String.format("cloud-infrastructure/pservers/pserver/%s?skip-related-to=true&format=resource_and_url", hostname);
        String vserverUri = String.format("cloud-infrastructure/cloud-regions/cloud-region/test-aic/%s?skip-related-to=true&format=resource_and_url", cloudRegionId);

        // PUT the resources
        putResourceWithQueryParam(pserverUri, pserverData);
        putResourceWithQueryParam(vserverUri, vserverData);

        String pserverMockRequestUri = String.format("cloud-infrastructure/pservers/pserver/%s?skip-related-to=true&format=resource_and_url", hostname);
        String vserverMockRequestUri = String.format("cloud-infrastructure/cloud-regions/cloud-region/test-aic/%s/tenants/tenant/%s/vservers/vserver/%s?skip-related-to=true&format=resource_and_url",
                cloudRegionId, tenant, vserver);

        // === GET - related-to-property should not exist ===
        // pserver
        Response pserverResponse = getMockResponse(pserverMockRequestUri);
        assertTrue(isRelatedToPropertiesFieldNullInResponse(pserverResponse, "pserver"));

        // vserver
        Response vserverResponse = getMockResponse(vserverMockRequestUri);
        assertTrue(isRelatedToPropertiesFieldNullInResponse(vserverResponse, "vserver"));
        // ===

        // === Clean up (DELETE) ===
        // vserver
        String deleteUri = String.format("cloud-infrastructure/cloud-regions/cloud-region/test-aic/%s/tenants/tenant/%s/vservers/vserver/%s",
                cloudRegionId, tenant, vserver);
        String vserverMockRequestUriNoFormat = String.format("cloud-infrastructure/cloud-regions/cloud-region/test-aic/%s/tenants/tenant/%s/vservers/vserver/%s",
                cloudRegionId, tenant, vserver);
        Response response = deleteServerObject(vserverMockRequestUriNoFormat, deleteUri, "vserver");
        int code = response.getStatus();
        if(!VALID_HTTP_STATUS_CODES.contains(code)){
            logger.info("Response Code: " + code + "\tEntity: " +  response.getEntity());
        }
        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());

        // pserver
        deleteUri = String.format("cloud-infrastructure/pservers/pserver/%s", hostname);
        response = deleteServerObject(pserverMockRequestUri, deleteUri, "pserver");
        code = response.getStatus();
        if(!VALID_HTTP_STATUS_CODES.contains(code)){
            logger.info("Response Code: " + code + "\tEntity: " +  response.getEntity());
        }
        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());
    }

    @Test
    public void testGetVserversWithSkipRelatedToParam() throws IOException, JSONException {
        String pserverData = getResourcePayload("pserver5");
        String vserverData = getResourcePayload("vserver5");

        String hostname = "pserver-hostname-test05";
        String cloudRegionId ="testAIC05";
        String tenant ="tenant05";
        String vserver ="vserver05";

        String pserverUri = String.format("cloud-infrastructure/pservers/pserver/%s?skip-related-to=true", hostname);
        String vserverUri = String.format("cloud-infrastructure/cloud-regions/cloud-region/test-aic/%s?skip-related-to=true", cloudRegionId);

        // PUT the resources
        putResourceWithQueryParam(pserverUri, pserverData);
        putResourceWithQueryParam(vserverUri, vserverData);

        String pserverMockRequestUri = String.format("cloud-infrastructure/pservers/pserver/%s?skip-related-to=true", hostname);
        String vserverMockRequestUri = String.format("cloud-infrastructure/cloud-regions/cloud-region/test-aic/%s/tenants/tenant/%s/vservers?vserver-selflink=somelink05&skip-related-to=true",
                cloudRegionId, tenant, vserver);

        // === GET - related-to-property should not exist ===
        // pserver
        Response pserverResponse = getMockResponse(pserverMockRequestUri);
        assertTrue(isRelatedToPropertiesFieldNullInResponse(pserverResponse, "pserver"));

        // vserver
        Response vserverResponse = getMockResponse(vserverMockRequestUri);
        assertTrue(isRelatedToPropertiesFieldNullInResponse(vserverResponse, "vserver"));
        // ===

        // === Clean up (DELETE) ===
        // vserver
        String deleteUri = String.format("cloud-infrastructure/cloud-regions/cloud-region/test-aic/%s/tenants/tenant/%s/vservers/vserver/%s",
                cloudRegionId, tenant, vserver);
        String vserverMockRequestUriNoFormat = String.format("cloud-infrastructure/cloud-regions/cloud-region/test-aic/%s/tenants/tenant/%s/vservers/vserver/%s",
                cloudRegionId, tenant, vserver);
        Response response = deleteServerObject(vserverMockRequestUriNoFormat, deleteUri, "vserver");
        int code = response.getStatus();
        if(!VALID_HTTP_STATUS_CODES.contains(code)){
            logger.info("Response Code: " + code + "\tEntity: " +  response.getEntity());
        }
        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());

        // pserver
        deleteUri = String.format("cloud-infrastructure/pservers/pserver/%s", hostname);
        response = deleteServerObject(pserverMockRequestUri, deleteUri, "pserver");
        code = response.getStatus();
        if(!VALID_HTTP_STATUS_CODES.contains(code)){
            logger.info("Response Code: " + code + "\tEntity: " +  response.getEntity());
        }
        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());
    }

    private Response deleteServerObject(String mockUri, String deleteUri, String nodeType) throws IOException, JSONException {
        Response response = getMockResponse(mockUri);
        String serverEntity = response.getEntity().toString();
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
        if (noResultsArray)
            resourceVersion = serverJsonObject.getString("resource-version");
        queryParameters.clear();
        queryParameters.add("resource-version", resourceVersion);

        MockHttpServletRequest mockReq = new MockHttpServletRequest("DELETE", deleteUri);
        Response deleteResponse = legacyMoxyConsumer.delete(
                schemaVersions.getDefaultVersion().toString(),
                deleteUri,
                httpHeaders,
                uriInfo,
                resourceVersion,
                mockReq
        );
        return deleteResponse;
    }

    private void putResourceWithQueryParam(String uri, String payload) {

        String[] uriSplit = uri.split("\\?");
        if (uriSplit[1] != null && !uriSplit[1].isEmpty()) {
            String[] params;
            if (!uriSplit[1].contains("&")) {
                String param = uriSplit[1];
                params = new String[]{param};
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
        Response response = legacyMoxyConsumer.update(
                payload,
                schemaVersions.getDefaultVersion().toString(),
                uri,
                httpHeaders,
                uriInfo,
                mockReq
        );

        assertNotNull("Response from the legacy moxy consumer returned null", response);
        int code = response.getStatus();
        if(!VALID_HTTP_STATUS_CODES.contains(code)){
            logger.info("Response Code: " + code + "\tEntity: " +  response.getEntity());
        }

        assertEquals("Expected to return status created from the response",
                Response.Status.CREATED.getStatusCode(), response.getStatus());
        logger.info("Response Code: " + code + "\tEntity: " +  response.getEntity());
    }

    private Response getMockResponse(String mockUri) throws IOException, JSONException {
        MockHttpServletRequest mockReq = new MockHttpServletRequest("GET", mockUri);
        Response response = legacyMoxyConsumer.getLegacy(
                "",
                null,
                null,
                schemaVersions.getDefaultVersion().toString(),
                mockUri,
                "10000",
                "false",
                httpHeaders,
                uriInfo,
                mockReq
        );
        String responseEntity = response.getEntity().toString();
        int code = response.getStatus();
        if(!VALID_HTTP_STATUS_CODES.contains(code)){
            logger.info("Response Code: " + code + "\tEntity: " +  response.getEntity());
        }
        return response;
    }

    private boolean isRelatedToPropertiesFieldNullInResponse(Response response, String nodeType) throws IOException, JSONException {
        String responseEntity = response.getEntity().toString();
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
                        if (relatedToProperty != null)
                            return false;
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
                        if (relatedToProperty != null)
                            return false;
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

/*-
 * ============LICENSE_START=======================================================
 * org.openecomp.aai
 * ================================================================================
 * Copyright (C) 2017 AT&T Intellectual Property. All rights reserved.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ============LICENSE_END=========================================================
 */

package org.openecomp.aai.rest;

import com.att.eelf.configuration.EELFLogger;
import com.att.eelf.configuration.EELFManager;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;
import org.openecomp.aai.AAISetup;
import org.openecomp.aai.dbmap.AAIGraph;
import org.openecomp.aai.exceptions.AAIException;
import org.openecomp.aai.introspection.ModelInjestor;
import org.openecomp.aai.introspection.Version;
import org.skyscreamer.jsonassert.JSONAssert;

import javax.ws.rs.core.*;
import java.io.IOException;
import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.when;

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

    private static final EELFLogger logger = EELFManager.getInstance().getLogger(LegacyMoxyConsumerTest.class.getName());

    @BeforeClass
    public static void setupRest(){
        AAIGraph.getInstance();
        ModelInjestor.getInstance();
    }

    @Before
    public void setup(){
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

        Response response = legacyMoxyConsumer.getLegacy(
                "",
                Version.getLatest().toString(),
                uri,
                "all",
                "false",
                httpHeaders,
                uriInfo,
                null
        );

        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());

        response = legacyMoxyConsumer.update(
                payload,
                Version.getLatest().toString(),
                uri,
                httpHeaders,
                uriInfo,
                null
        );

        int code = response.getStatus();
        if(!VALID_HTTP_STATUS_CODES.contains(code)){
            System.out.println("Response Code: " + code + "\tEntity: " +  response.getEntity());
        }

        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());

        queryParameters.add("depth", "10000");

        response = legacyMoxyConsumer.getLegacy(
                "",
                Version.getLatest().toString(),
                uri,
                "10000",
                "false",
                httpHeaders,
                uriInfo,
                null
        );

        code = response.getStatus();
        if(!VALID_HTTP_STATUS_CODES.contains(code)){
            System.out.println("Response Code: " + code + "\tEntity: " +  response.getEntity());
        }

        String pserverEntity        = response.getEntity().toString();
        JSONObject pserverJsonbject = new JSONObject(pserverEntity);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        JSONAssert.assertEquals(payload, pserverEntity, false);

        String resourceVersion = pserverJsonbject.getString("resource-version");

        queryParameters.add("resource-version", resourceVersion);

        response = legacyMoxyConsumer.delete(
                "v11",
                uri,
                httpHeaders,
                uriInfo,
                "",
                null
        );

        code = response.getStatus();
        if(!VALID_HTTP_STATUS_CODES.contains(code)){
            System.out.println("Response Code: " + code + "\tEntity: " +  response.getEntity());
        }

        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());

        response = legacyMoxyConsumer.getLegacy(
                "",
                Version.getLatest().toString(),
                uri,
                "all",
                "false",
                httpHeaders,
                uriInfo,
                null
        );

        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
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

        Response response = legacyMoxyConsumer.updateRelationship(
                cloudToPserverRelationshipData,
                Version.getLatest().toString(),
                cloudToPserverRelationshipUri,
                httpHeaders,
                uriInfo,
                null
        );

        assertNotNull("Response from the legacy moxy consumer returned null", response);
        int code = response.getStatus();
        if(!VALID_HTTP_STATUS_CODES.contains(code)){
            System.out.println("Response Code: " + code + "\tEntity: " +  response.getEntity());
        }

        assertEquals("Expected to return status created from the response",
                Response.Status.OK.getStatusCode(), response.getStatus());
        logger.info("Response Code: " + code + "\tEntity: " +  response.getEntity());

        // TODO - Need to actually verify the relationship between pserver and cloud-region

        response = legacyMoxyConsumer.deleteRelationship(
                cloudToPserverRelationshipData,
                Version.getLatest().toString(),
                cloudToPserverRelationshipUri,
                httpHeaders,
                uriInfo,
                null
        );

        code = response.getStatus();
        if(!VALID_HTTP_STATUS_CODES.contains(code)){
            System.out.println("Response Code: " + code + "\tEntity: " +  response.getEntity());
            System.out.println("Response Code: " + code + "\tEntity: " +  response.getEntity());
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

        Response response = legacyMoxyConsumer.update(
                payload,
                Version.getLatest().toString(),
                uri,
                httpHeaders,
                uriInfo,
                null
        );

        int code = response.getStatus();
        System.out.println("Response Code: " + code + "\tEntity: " +  response.getEntity());
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), code);
    }

    @Test
    public void testInvalidUriThrowRandomException() throws JSONException {

        String payload = "{}";
        String uri = "fake-infrastructure/pservers/pserver/fajsidj";

        when(uriInfo.getPath()).thenReturn(uri);
        when(uriInfo.getPath(false)).thenThrow(new IllegalArgumentException());

        Response response = legacyMoxyConsumer.update(
                payload,
                Version.getLatest().toString(),
                uri,
                httpHeaders,
                uriInfo,
                null
        );

        int code = response.getStatus();
        assertNotNull("Response from the legacy moxy consumer returned null", response);
        assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), code);
        System.out.println("Response Code: " + code + "\tEntity: " +  response.getEntity());

        response = legacyMoxyConsumer.updateRelationship(
                payload,
                Version.getLatest().toString(),
                uri,
                httpHeaders,
                uriInfo,
                null
        );

        code = response.getStatus();
        assertNotNull("Response from the legacy moxy consumer returned null", response);
        assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), code);
        System.out.println("Response Code: " + code + "\tEntity: " +  response.getEntity());

        response = legacyMoxyConsumer.getLegacy(
                "",
                Version.getLatest().toString(),
                uri,
                "all",
                "false",
                httpHeaders,
                uriInfo,
                null
        );

        assertNotNull("Response from the legacy moxy consumer returned null", response);
        assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), code);

        response = legacyMoxyConsumer.delete(
                Version.getLatest().toString(),
                uri,
                httpHeaders,
                uriInfo,
                "",
                null
        );

        code = response.getStatus();
        assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), code);

        response = legacyMoxyConsumer.deleteRelationship(
                payload,
                Version.getLatest().toString(),
                uri,
                httpHeaders,
                uriInfo,
                null
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

        Response response = legacyMoxyConsumer.update(
                payload,
                Version.getLatest().toString(),
                uri,
                httpHeaders,
                uriInfo,
                null
        );

        int code = response.getStatus();
        System.out.println("Response Code: " + code + "\tEntity: " +  response.getEntity());
        assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), code);

        response = legacyMoxyConsumer.updateRelationship(
                payload,
                Version.getLatest().toString(),
                uri,
                httpHeaders,
                uriInfo,
                null
        );

        code = response.getStatus();
        System.out.println("Response Code: " + code + "\tEntity: " +  response.getEntity());
        assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), code);

        response = legacyMoxyConsumer.getLegacy(
                "",
                Version.getLatest().toString(),
                uri,
                "all",
                "false",
                httpHeaders,
                uriInfo,
                null
        );

        assertNotNull("Response from the legacy moxy consumer returned null", response);
        assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), code);

        queryParameters.add("resource-version", "3434394839483");
        response = legacyMoxyConsumer.delete(
                Version.getLatest().toString(),
                uri,
                httpHeaders,
                uriInfo,
                "",
                null
        );

        code = response.getStatus();
        System.out.println("Response Code: " + code + "\tEntity: " +  response.getEntity());
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), code);

        response = legacyMoxyConsumer.deleteRelationship(
                payload,
                Version.getLatest().toString(),
                uri,
                httpHeaders,
                uriInfo,
                null
        );
        code = response.getStatus();
        assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), code);
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

        Response response = legacyMoxyConsumer.getLegacy(
                "",
                Version.getLatest().toString(),
                uri,
                "all",
                "false",
                httpHeaders,
                uriInfo,
                null
        );

        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());

        response = legacyMoxyConsumer.update(
                payload,
                Version.getLatest().toString(),
                uri,
                httpHeaders,
                uriInfo,
                null
        );

        int code = response.getStatus();
        if(!VALID_HTTP_STATUS_CODES.contains(code)){
            System.out.println("Response Code: " + code + "\tEntity: " +  response.getEntity());
        }

        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());

        String patchData = "{\"in-maint\": false}";

        headersMultiMap.add("Content-Type", "application/json");

        outputMediaTypes.remove(APPLICATION_JSON);
        outputMediaTypes.add(MediaType.valueOf("application/merge-patch+json"));

        response = legacyMoxyConsumer.patch(
                patchData,
                Version.getLatest().toString(),
                uri,
                httpHeaders,
                uriInfo,
                null
        );

        code = response.getStatus();
        assertNotNull("Response from the patch returned null", response);
        System.out.println("Response Code: " + code + "\tEntity: " +  response.getEntity());
        assertEquals(Response.Status.OK.getStatusCode(), code);

    }

    protected void doSetupResource(String uri, String payload) throws JSONException {

        when(uriInfo.getPath()).thenReturn(uri);
        when(uriInfo.getPath(false)).thenReturn(uri);

        Response response = legacyMoxyConsumer.getLegacy(
                "",
                Version.getLatest().toString(),
                uri,
                "all",
                "false",
                httpHeaders,
                uriInfo,
                null
        );

        assertNotNull("Response from the legacy moxy consumer returned null", response);
        assertEquals("Expected to not have the data already in memory",
                Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());

        response = legacyMoxyConsumer.update(
                payload,
                Version.getLatest().toString(),
                uri,
                httpHeaders,
                uriInfo,
                null
        );

        assertNotNull("Response from the legacy moxy consumer returned null", response);
        int code = response.getStatus();
        if(!VALID_HTTP_STATUS_CODES.contains(code)){
            System.out.println("Response Code: " + code + "\tEntity: " +  response.getEntity());
        }
        assertEquals("Expected to return status created from the response",
                Response.Status.CREATED.getStatusCode(), response.getStatus());

        queryParameters.add("depth", "10000");
        response = legacyMoxyConsumer.getLegacy(
                "",
                Version.getLatest().toString(),
                uri,
                "all",
                "false",
                httpHeaders,
                uriInfo,
                null
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

        Response response = legacyMoxyConsumer.deleteRelationship(
                payload,
                Version.getLatest().toString(),
                uri,
                httpHeaders,
                uriInfo,
                null
        );

        int code = response.getStatus();
        System.out.println("Response Code: " + code + "\tEntity: " +  response.getEntity());
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

    public String getUri(){
        return getUri("pserver-hostname-test");
    }
}

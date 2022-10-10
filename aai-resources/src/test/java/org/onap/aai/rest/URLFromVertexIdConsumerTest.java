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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyObject;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.json.JSONException;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;
import org.onap.aai.AAISetup;
import org.onap.aai.dbmap.AAIGraph;
import org.onap.aai.exceptions.AAIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockHttpServletRequest;

public class URLFromVertexIdConsumerTest extends AAISetup {

    protected static final MediaType APPLICATION_JSON = MediaType.valueOf("application/json");

    private static final Set<Integer> VALID_HTTP_STATUS_CODES = new HashSet<>();

    static {
        VALID_HTTP_STATUS_CODES.add(200);
        VALID_HTTP_STATUS_CODES.add(201);
        VALID_HTTP_STATUS_CODES.add(204);
    }

    private URLFromVertexIdConsumer urlFromVertexIdConsumer;
    private LegacyMoxyConsumer legacyMoxyConsumer;

    private HttpHeaders httpHeaders;

    private UriInfo uriInfo;

    private MultivaluedMap<String, String> headersMultiMap;
    private MultivaluedMap<String, String> queryParameters;

    private List<String> aaiRequestContextList;

    private List<MediaType> outputMediaTypes;

    private static final Logger logger = LoggerFactory.getLogger(LegacyMoxyConsumerTest.class.getName());
    private boolean initialized = false;
    @BeforeClass
    public static void setupRest(){
       // AAIGraph.getInstance();
    }

    @Before
    public void setup(){
    	if(!initialized){
    		initialized = true;
    		AAIGraph.getInstance();
    	}
        logger.info("Starting the setup for the integration tests of Rest Endpoints");

        urlFromVertexIdConsumer = new URLFromVertexIdConsumer();
        legacyMoxyConsumer      = new LegacyMoxyConsumer();

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

        String uri = "cloud-infrastructure/pservers/pserver/" + UUID.randomUUID().toString();
        String payload = "{}";

        when(uriInfo.getPath()).thenReturn(uri);
        when(uriInfo.getPath(false)).thenReturn(uri);

        MockHttpServletRequest mockReqGet = new MockHttpServletRequest("GET", uri);
        Response response = legacyMoxyConsumer.getLegacy(
                "",
                "-1",
                "-1",
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
            System.out.println("Response Code: " + code + "\tEntity: " +  response.getEntity());
        }

        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());

        MultivaluedMap<String, Object> responseHeaders = response.getMetadata();

        assertNotNull("Unable to retrieve the response headers from response object", responseHeaders);
        assertTrue("Response doesn't contain the key vertexId", responseHeaders.containsKey("vertex-id"));

        String vertexId = responseHeaders.get("vertex-id").get(0).toString();
        response = urlFromVertexIdConsumer.generateUrlFromVertexId(
                "",
                schemaVersions.getDefaultVersion().toString(),
                Long.valueOf(vertexId).longValue(),
                httpHeaders,
                uriInfo,
                mockReqGet
                );

        assertNotNull(response);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    }

    @Test
    public void testObjectNotFoundInDBReturnsNotFoundStatus() throws JSONException, IOException, AAIException {

        String uri = "cloud-infrastructure/pservers/pserver/testRandom";

        when(uriInfo.getPath()).thenReturn(uri);
        when(uriInfo.getPath(false)).thenReturn(uri);

        String vertexId = "384584";
        MockHttpServletRequest mockReqGet = new MockHttpServletRequest("GET", uri);
        Response response = urlFromVertexIdConsumer.generateUrlFromVertexId(
                "",
                schemaVersions.getDefaultVersion().toString(),
                Long.valueOf(vertexId).longValue(),
                httpHeaders,
                uriInfo,
                mockReqGet
        );

        assertNotNull("Check if the response is not null", response);

        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
    }
}

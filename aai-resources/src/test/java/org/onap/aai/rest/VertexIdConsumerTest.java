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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.onap.aai.AAISetup;
import org.onap.aai.dbmap.AAIGraph;
import org.onap.aai.exceptions.AAIException;
import org.onap.aai.service.ResourcesService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockHttpServletRequest;

public class VertexIdConsumerTest extends AAISetup {

    protected static final MediaType APPLICATION_JSON = MediaType.valueOf("application/json");

    private static final Set<Integer> VALID_HTTP_STATUS_CODES = new HashSet<>();

    static {
        VALID_HTTP_STATUS_CODES.add(200);
        VALID_HTTP_STATUS_CODES.add(201);
        VALID_HTTP_STATUS_CODES.add(204);
    }

    private VertexIdConsumer vertexIdConsumer;
    private ResourcesController resourcesController;

    private HttpHeaders httpHeaders;

    private UriInfo uriInfo;

    private MultivaluedMap<String, String> headersMultiMap;
    private MultivaluedMap<String, String> queryParameters;

    private List<String> aaiRequestContextList;

    private List<MediaType> outputMediaTypes;

    private static final Logger logger = LoggerFactory.getLogger(VertexIdConsumerTest.class.getName());

    @BeforeEach
    public void setup() {
        if(!AAIGraph.isInit()) {
            AAIGraph.getInstance();
        }
        logger.info("Starting the setup for the integration tests of Rest Endpoints");

        vertexIdConsumer = new VertexIdConsumer();
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

        when(httpHeaders.getAcceptableMediaTypes()).thenReturn(outputMediaTypes);
        when(httpHeaders.getRequestHeaders()).thenReturn(headersMultiMap);

        when(httpHeaders.getRequestHeader("aai-request-context")).thenReturn(aaiRequestContextList);

        when(uriInfo.getQueryParameters()).thenReturn(queryParameters);
        when(uriInfo.getQueryParameters(false)).thenReturn(queryParameters);

        // TODO - Check if this is valid since RemoveDME2QueryParameters seems to be very unreasonable
        Mockito.doReturn(null).when(queryParameters).remove(any());

        when(httpHeaders.getMediaType()).thenReturn(APPLICATION_JSON);
    }

    @Test
    public void testResponsePutGetDeleteOnResource() throws JSONException, IOException, AAIException {

        String uri = "cloud-infrastructure/pservers/pserver/" + UUID.randomUUID().toString();
        String payload = "{}";

        when(uriInfo.getPath()).thenReturn(uri);
        when(uriInfo.getPath(false)).thenReturn(uri);

        MockHttpServletRequest mockReqGet = new MockHttpServletRequest("GET", uri);
        Response response = resourcesController.getLegacy(schemaVersions.getDefaultVersion().toString(), uri, -1, -1,
                false, "all", "false", httpHeaders, uriInfo, mockReqGet);

        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
        MockHttpServletRequest mockReq = new MockHttpServletRequest("PUT", uri);

        response = resourcesController.update(payload, schemaVersions.getDefaultVersion().toString(), uri, httpHeaders,
                uriInfo, mockReq);

        int code = response.getStatus();
        if (!VALID_HTTP_STATUS_CODES.contains(code)) {
            System.out.println("Response Code: " + code + "\tEntity: " + response.getEntity());
        }

        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());

        MultivaluedMap<String, Object> responseHeaders = response.getMetadata();

        assertNotNull(responseHeaders, "Unable to retrieve the response headers from response object");
        assertTrue(responseHeaders.containsKey("vertex-id"), "Response doesn't contain the key vertexId");

        String vertexId = responseHeaders.get("vertex-id").get(0).toString();

        response = vertexIdConsumer.getByVertexId("", schemaVersions.getDefaultVersion().toString(),
                Long.valueOf(vertexId).longValue(), "10000", httpHeaders, uriInfo, mockReqGet);

        assertNotNull(response);
        String pserverObject = response.getEntity().toString();

        System.out.println(pserverObject);
    }
}

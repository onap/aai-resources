/*
 * ============LICENSE_START=======================================================
 * org.onap.aai
 * ================================================================================
 * Copyright © 2017-2018 AT&T Intellectual Property. All rights reserved.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ============LICENSE_END=========================================================
 */

package org.onap.aai.rest.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.onap.aai.AAISetup;
import org.onap.aai.config.GraphConfig;
import org.onap.aai.util.GraphChecker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(classes = {GraphConfig.class, GraphChecker.class})
public class EchoResponseTest extends AAISetup {

    private static final Logger logger = LoggerFactory.getLogger(EchoResponseTest.class);
    protected static final MediaType APPLICATION_JSON = MediaType.valueOf("application/json");
    private final EchoResponse echoResponse;
    private final GraphChecker graphCheckerMock = mock(GraphChecker.class);

    private HttpHeaders httpHeaders;
    private List<MediaType> outputMediaTypes;

    public EchoResponseTest() {
        this.echoResponse = new EchoResponse(graphCheckerMock);
    }

    @BeforeEach
    public void setup() {
        logger.info("Starting the setup for the integration tests of Rest Endpoints");

        MultivaluedMap<String, String> headersMultiMap = new MultivaluedHashMap<>();
        MultivaluedMap<String, String> queryParameters = Mockito.spy(new MultivaluedHashMap<>());

        headersMultiMap.add("X-FromAppId", "JUNIT");
        headersMultiMap.add("X-TransactionId", UUID.randomUUID().toString());
        headersMultiMap.add("Real-Time", "true");
        headersMultiMap.add("Accept", "application/json");
        headersMultiMap.add("aai-request-context", "");

        outputMediaTypes = new ArrayList<>();
        outputMediaTypes.add(APPLICATION_JSON);

        List<String> aaiRequestContextList = new ArrayList<>();
        aaiRequestContextList.add("");

        httpHeaders = mock(HttpHeaders.class);
        when(httpHeaders.getAcceptableMediaTypes()).thenReturn(outputMediaTypes);
        when(httpHeaders.getRequestHeaders()).thenReturn(headersMultiMap);
        when(httpHeaders.getRequestHeader("X-FromAppId")).thenReturn(Collections.singletonList("JUNIT"));
        when(httpHeaders.getRequestHeader("X-TransactionId")).thenReturn(Collections.singletonList("JUNIT"));
        when(httpHeaders.getRequestHeader("aai-request-context")).thenReturn(aaiRequestContextList);
        when(httpHeaders.getMediaType()).thenReturn(APPLICATION_JSON);

        UriInfo uriInfo = mock(UriInfo.class);
        when(uriInfo.getQueryParameters()).thenReturn(queryParameters);
        when(uriInfo.getQueryParameters(false)).thenReturn(queryParameters);

        // TODO - Check if this is valid since RemoveDME2QueryParameters seems to be very unreasonable
        Mockito.doReturn(null).when(queryParameters).remove(any());

    }

    @Test
    public void testEchoResultWhenInValidHeadersThrowsBadRequest() {

        httpHeaders = mock(HttpHeaders.class);
        Response response = echoResponse.echoResult(httpHeaders, null);

        assertNotNull(response);
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    @Test
    public void testCheckDbNowAction_Success() {
        when(graphCheckerMock.isAaiGraphDbAvailable()).thenReturn(true);

        Response response = echoResponse.echoResult(httpHeaders, null);

        assertNotNull(response);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    }

    @Test
    public void testCheckDbNowAction_Failure() {
        when(graphCheckerMock.isAaiGraphDbAvailable()).thenReturn(false);

        Response response = echoResponse.echoResult(httpHeaders, null);

        assertNotNull(response);
        assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
    }

}

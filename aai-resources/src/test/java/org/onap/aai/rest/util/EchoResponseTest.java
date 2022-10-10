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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.anyObject;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.onap.aai.AAISetup;
import org.onap.aai.tasks.AaiGraphChecker;
import org.onap.aai.tasks.AaiGraphChecker.CheckerType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(classes = {AaiGraphChecker.class})
public class EchoResponseTest extends AAISetup {

    private static final Logger logger = LoggerFactory.getLogger(EchoResponseTest.class);
    protected static final MediaType APPLICATION_JSON = MediaType.valueOf("application/json");
    private static final String CHECK_DB_ACTION = "checkDB";
    private static final String CHECK_DB_STATUS_NOW_ACTION = "checkDBNow";

    private final EchoResponse echoResponse;
    private final AaiGraphChecker aaiGraphCheckerMock = mock(AaiGraphChecker.class);

    private HttpHeaders httpHeaders;
    private List<MediaType> outputMediaTypes;

    public EchoResponseTest() {
        this.echoResponse = new EchoResponse(aaiGraphCheckerMock);
    }

    @Before
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
        Mockito.doReturn(null).when(queryParameters).remove(anyObject());

    }

    @Test
    public void testEchoResultWhenValidHeaders() {

        Response response = echoResponse.echoResult(httpHeaders, null, "");

        assertNotNull(response);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    }

    @Test
    public void testEchoResultWhenInValidHeadersThrowsBadRequest() {

        httpHeaders = mock(HttpHeaders.class);
        Response response = echoResponse.echoResult(httpHeaders, null, "");

        assertNotNull(response);
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    @Test
    public void testEchoResultWhenValidHeadersButMediaTypeWrong() {

        when(httpHeaders.getAcceptableMediaTypes()).thenThrow(new IllegalStateException()).thenReturn(outputMediaTypes);

        Response response = echoResponse.echoResult(httpHeaders, null, "");

        assertNotNull(response);
        assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
    }

    @Test
    public void testCheckDbAction_CachedSuccess() {
        // Prepare
        when(aaiGraphCheckerMock.isAaiGraphDbAvailable(CheckerType.CACHED)).thenReturn(Boolean.TRUE);
        // Run
        Response response = echoResponse.echoResult(httpHeaders, null, CHECK_DB_ACTION);
        // Verify
        verify(aaiGraphCheckerMock, never()).isAaiGraphDbAvailable(CheckerType.ACTUAL);
        assertNotNull(response);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    }

    @Test
    public void testCheckDbAction_CachedFailure() {
        // Prepare
        when(aaiGraphCheckerMock.isAaiGraphDbAvailable(CheckerType.CACHED)).thenReturn(Boolean.FALSE);
        // Run
        Response response = echoResponse.echoResult(httpHeaders, null, CHECK_DB_ACTION);
        // Verify
        verify(aaiGraphCheckerMock, never()).isAaiGraphDbAvailable(CheckerType.ACTUAL);
        assertNotNull(response);
        assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
    }

    @Test
    public void testCheckDbNowAction_Success() {
        // Prepare
        when(aaiGraphCheckerMock.isAaiGraphDbAvailable(CheckerType.ACTUAL)).thenReturn(Boolean.TRUE);
        // Run
        Response response = echoResponse.echoResult(httpHeaders, null, CHECK_DB_STATUS_NOW_ACTION);
        // Verify
        assertNotNull(response);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    }

    @Test
    public void testCheckDbNowAction_Failure() {
        // Prepare
        when(aaiGraphCheckerMock.isAaiGraphDbAvailable(CheckerType.ACTUAL)).thenReturn(Boolean.FALSE);
        // Run
        Response response = echoResponse.echoResult(httpHeaders, null, CHECK_DB_STATUS_NOW_ACTION);
        // Verify
        assertNotNull(response);
        assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
    }

    @Test
    public void testCheckDbNowAction_Unknown() {
        // Prepare
        when(aaiGraphCheckerMock.isAaiGraphDbAvailable(CheckerType.ACTUAL)).thenReturn(null);
        // Run
        Response response = echoResponse.echoResult(httpHeaders, null, CHECK_DB_STATUS_NOW_ACTION);
        // Verify
        assertNotNull(response);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    }

}

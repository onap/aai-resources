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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;
import org.onap.aai.AAISetup;
import org.onap.aai.dbmap.AAIGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockHttpServletRequest;

public abstract class BulkProcessorTestAbstraction extends AAISetup {

    protected static final MediaType APPLICATION_JSON = MediaType.valueOf("application/json");

    protected static final Set<Integer> VALID_HTTP_STATUS_CODES = new HashSet<>();

    static {
        VALID_HTTP_STATUS_CODES.add(200);
        VALID_HTTP_STATUS_CODES.add(201);
        VALID_HTTP_STATUS_CODES.add(204);
    }

    protected BulkConsumer bulkConsumer;

    protected HttpHeaders httpHeaders;

    protected UriInfo uriInfo;

    protected MultivaluedMap<String, String> headersMultiMap;
    protected MultivaluedMap<String, String> queryParameters;

    protected List<String> aaiRequestContextList;

    protected List<MediaType> outputMediaTypes;

    protected String uri;

    private static final Logger logger = LoggerFactory.getLogger(BulkProcessorTestAbstraction.class.getName());

    @BeforeEach
    public void setup() {
        if (!AAIGraph.isInit()) {
            AAIGraph.getInstance();
        }
        logger.info("Starting the setup for the integration tests of Rest Endpoints");

        bulkConsumer = getConsumer();
        uri = getUri();
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

    protected Response executeRequest(String payload) {
        MockHttpServletRequest mockReq = new MockHttpServletRequest("PUT", "http://www.test.com");

        return bulkConsumer.bulkProcessor(payload.replaceAll("<UUID>", UUID.randomUUID().toString()),
                schemaVersions.getDefaultVersion().toString(), httpHeaders, uriInfo, mockReq);
    }

    protected String getBulkPayload(String bulkPayloadName) throws IOException {
        return getPayload("payloads/bulk/" + bulkPayloadName + ".json");
    }

    protected abstract BulkConsumer getConsumer();

    protected abstract String getUri();
}

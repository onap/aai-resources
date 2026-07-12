/**
 * ============LICENSE_START=======================================================
 * org.onap.aai
 * ================================================================================
 * Copyright © 2026 Deutsche Telekom. All rights reserved.
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

package org.onap.aai.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

import org.janusgraph.core.JanusGraphException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.onap.aai.AAISetup;
import org.onap.aai.restcore.HttpMethod;
import org.onap.aai.serialization.engines.TransactionalGraphEngine;

public class ResourcesServiceTest extends AAISetup {

    protected static final MediaType APPLICATION_JSON = MediaType.valueOf("application/json");

    private ResourcesService resourcesService;
    private HttpHeaders httpHeaders;
    private UriInfo uriInfo;

    @BeforeEach
    public void setup() {
        resourcesService = new ResourcesService();

        httpHeaders = mock(HttpHeaders.class);
        uriInfo = mock(UriInfo.class);

        MultivaluedHashMap<String, String> headersMultiMap = new MultivaluedHashMap<>();
        headersMultiMap.add("X-FromAppId", "JUNIT");
        headersMultiMap.add("X-TransactionId", UUID.randomUUID().toString());
        headersMultiMap.add("Accept", "application/json");

        List<MediaType> outputMediaTypes = new ArrayList<>();
        outputMediaTypes.add(APPLICATION_JSON);
        when(httpHeaders.getAcceptableMediaTypes()).thenReturn(outputMediaTypes);
        when(httpHeaders.getRequestHeaders()).thenReturn(headersMultiMap);
        when(uriInfo.getPath()).thenReturn("network/generic-vnfs/generic-vnf/test-vnf-id");
    }

    /**
     * When the graph transaction commit fails (e.g. JanusGraph lock contention flushing
     * to Cassandra under concurrent writes), the failure must be mapped to a proper AAI
     * error envelope (AAI_6134, HTTP 500) instead of escaping uncaught and surfacing to
     * the caller as a bare Spring Boot /error 500 that clients cannot classify or retry.
     */
    @Test
    public void finalizeTransactionMapsCommitFailureToAaiErrorResponse() {
        TransactionalGraphEngine dbEngine = mock(TransactionalGraphEngine.class);
        doThrow(new JanusGraphException("Could not commit transaction due to exception during persistence"))
                .when(dbEngine).commit();

        Response originalResponse = Response.status(Response.Status.CREATED).build();

        Response result = resourcesService.finalizeTransaction(dbEngine, true, originalResponse, httpHeaders, uriInfo,
                HttpMethod.PUT);

        assertNotNull(result);
        assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), result.getStatus(),
                "a failed commit must return an HTTP 500");
        assertNotNull(result.getEntity(), "the response must carry an AAI error body, not be empty");
        assertEquals(true, result.getEntity().toString().contains("6134"),
                "the error body must be the classified AAI_6134 serviceException envelope");
        verify(dbEngine).rollback();
    }

    /**
     * A successful commit must return the original response untouched.
     */
    @Test
    public void finalizeTransactionReturnsOriginalResponseOnSuccessfulCommit() {
        TransactionalGraphEngine dbEngine = mock(TransactionalGraphEngine.class);
        Response originalResponse = Response.status(Response.Status.CREATED).build();

        Response result = resourcesService.finalizeTransaction(dbEngine, true, originalResponse, httpHeaders, uriInfo,
                HttpMethod.PUT);

        assertEquals(originalResponse, result);
        verify(dbEngine).commit();
        verify(dbEngine, never()).rollback();
    }

    /**
     * When the request was not successful the transaction must be rolled back, never committed.
     */
    @Test
    public void finalizeTransactionRollsBackWhenNotCommitting() {
        TransactionalGraphEngine dbEngine = mock(TransactionalGraphEngine.class);
        Response originalResponse = Response.status(Response.Status.BAD_REQUEST).build();

        Response result = resourcesService.finalizeTransaction(dbEngine, false, originalResponse, httpHeaders, uriInfo,
                HttpMethod.PUT);

        assertEquals(originalResponse, result);
        verify(dbEngine).rollback();
        verify(dbEngine, never()).commit();
    }

    /**
     * A null dbEngine (transaction never established) must be a no-op that returns the response.
     */
    @Test
    public void finalizeTransactionIsNoOpWhenDbEngineNull() {
        Response originalResponse = Response.status(Response.Status.OK).build();

        Response result = resourcesService.finalizeTransaction(null, true, originalResponse, httpHeaders, uriInfo,
                HttpMethod.GET);

        assertEquals(originalResponse, result);
    }

    /**
     * On the rollback path (request was not successful, or a read transaction with no cleanup),
     * nothing was meant to persist, so a rollback failure must NOT overwrite the original response
     * with a spurious AAI_6134 500 — the caller must still see its real result (a valid read or the
     * genuine error). The rollback failure is only logged.
     */
    @Test
    public void finalizeTransactionReturnsOriginalResponseWhenRollbackPathFails() {
        TransactionalGraphEngine dbEngine = mock(TransactionalGraphEngine.class);
        doThrow(new IllegalStateException("transaction already closed")).when(dbEngine).rollback();

        Response originalResponse = Response.status(Response.Status.OK).build();

        Response result = resourcesService.finalizeTransaction(dbEngine, false, originalResponse, httpHeaders, uriInfo,
                HttpMethod.GET);

        assertEquals(originalResponse, result,
                "a failed rollback must not mask the original response with an AAI_6134 error");
    }

    /**
     * If rollback itself throws after a failed commit (JanusGraph may already have aborted the
     * transaction), the primary AAI_6134 error must still be returned rather than the rollback
     * exception masking it.
     */
    @Test
    public void finalizeTransactionStillReturnsErrorWhenRollbackAlsoFails() {
        TransactionalGraphEngine dbEngine = mock(TransactionalGraphEngine.class);
        doThrow(new JanusGraphException("commit failed")).when(dbEngine).commit();
        doThrow(new IllegalStateException("transaction already closed")).when(dbEngine).rollback();

        Response originalResponse = Response.status(Response.Status.CREATED).build();

        Response result = resourcesService.finalizeTransaction(dbEngine, true, originalResponse, httpHeaders, uriInfo,
                HttpMethod.PUT);

        assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), result.getStatus());
        assertEquals(true, result.getEntity().toString().contains("6134"));
        verify(dbEngine, times(1)).rollback();
    }
}

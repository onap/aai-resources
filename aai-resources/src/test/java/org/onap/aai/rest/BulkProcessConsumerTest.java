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
import static org.mockito.Mockito.when;

import java.io.IOException;

import javax.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.onap.aai.dbmap.AAIGraph;
import org.onap.aai.exceptions.AAIException;
import org.onap.aai.service.ResourcesService;
import org.skyscreamer.jsonassert.JSONAssert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockHttpServletRequest;

public class BulkProcessConsumerTest extends BulkProcessorTestAbstraction {

    private static final Logger logger = LoggerFactory.getLogger(BulkProcessConsumerTest.class.getName());
    private ResourcesController resourcesController;

    @Test
    public void bulkAddPayloadInBulkProcessTest() throws IOException {

        when(uriInfo.getPath()).thenReturn(uri);
        when(uriInfo.getPath(false)).thenReturn(uri);

        String payload = getBulkPayload("pserver-transactions");
        Response response = executeRequest(payload);

        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus(), "Valid Response Code");
        assertEquals(3,
                StringUtils.countMatches(response.getEntity().toString(), "{\"201\":null}"),
                "Contains 3 {\"201\":null}");
    }

    @Test
    public void bulkProcessPayloadTest() throws IOException {

        when(uriInfo.getPath()).thenReturn(uri);
        when(uriInfo.getPath(false)).thenReturn(uri);

        String payload = getBulkPayload("pserver-bulk-process-transactions");
        Response response = executeRequest(payload);

        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus(), "Valid Response Code");
        assertEquals(1,
                StringUtils.countMatches(response.getEntity().toString(), "{\"201\":null}"),
                "Contains 1 {\"201\":null}");
        assertEquals(1,
                StringUtils.countMatches(response.getEntity().toString(), "{\"404\":\"{"),
                "Contains 1 {\"404\":\"{");
        assertEquals(1,
                StringUtils.countMatches(response.getEntity().toString(), "ERR.5.4.6114"),
                "Contains 1 ERR.5.4.6114");
    }

    @Test
    public void bulkProcessPayloadWithPatchTest() throws IOException {

        when(uriInfo.getPath()).thenReturn(uri);
        when(uriInfo.getPath(false)).thenReturn(uri);

        String payload = getBulkPayload("pserver-bulk-process-transactions-with-patch");
        Response response = executeRequest(payload);
        System.out.println(response.getEntity());
        System.out.println(
                AAIGraph.getInstance().getGraph().newTransaction().traversal().V().has("fqdn", "NEW").count().next());
        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus(), "Valid Response Code");
    }

    @Test
    public void bulkProcessComplexDeletePayloadTest() throws IOException {

        when(uriInfo.getPath()).thenReturn(uri);
        when(uriInfo.getPath(false)).thenReturn(uri);

        String payload = getBulkPayload("complex-bulk-process-transactions");
        Response response = executeRequest(payload);

        System.out.println(response.getEntity().toString());
        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus(), "Valid Response Code");
        assertEquals(0,
                StringUtils.countMatches(response.getEntity().toString(), "{\"201\":null}"),
                "Contains 0 {\"201\":null}");
        assertEquals(1,
                StringUtils.countMatches(response.getEntity().toString(), "{\"404\":\"{"),
                "Contains 1 {\"404\":\"{");
        assertEquals(1,
                StringUtils.countMatches(response.getEntity().toString(), "ERR.5.4.6114"),
                "Contains 1 ERR.5.4.6114");
    }

    @Test
    public void testBulkDeletePserverAndComplexRelationship() throws IOException, JSONException, AAIException {


        resourcesController = new ResourcesController(new ResourcesService());

        String pserverData = getPayload("payloads/relationship/pserver-bugfix.json");
        String complexData = getPayload("payloads/relationship/complex-bugfix.json");

        String hostname = "pserver-9876543210-77-jenkins";
        String physicalLocationId = "complex-987654321-77-jenkins";

        String pserverUri = String.format("cloud-infrastructure/pservers/pserver/%s", hostname);
        String complexUri = String.format("cloud-infrastructure/complexes/complex/%s", physicalLocationId);

        doSetupResource(pserverUri, pserverData);
        doSetupResource(complexUri, complexData);

        String complexToPserverRelationshipData =
                getPayload("payloads/relationship/pserver-complex-relationship-for-bulk.json");
        String complexToPserverRelationshipUri =
                String.format("cloud-infrastructure/pservers/pserver/%s/relationship-list/relationship", hostname);

        Response response = resourcesController.updateRelationship(complexToPserverRelationshipData,
                schemaVersions.getDefaultVersion().toString(), complexToPserverRelationshipUri, httpHeaders, uriInfo,
                new MockHttpServletRequest("DELETE", "http://www.test.com"));

        assertNotNull(response, "Response from the legacy moxy consumer returned null");
        int code = response.getStatus();
        if (!VALID_HTTP_STATUS_CODES.contains(code)) {
            System.out.println("Response Code: " + code + "\tEntity: " + response.getEntity());
        }

        assertEquals(Response.Status.OK.getStatusCode(),
                response.getStatus(),
                "Expected to return status created from the response");
        logger.info("Response Code: " + code + "\tEntity: " + response.getEntity());

        // TODO - Need to actually verify the relationship between pserver and cloud-region

        String payload = getBulkPayload("complex-bulk-process-delete-transactions");
        Response responseBulkDelete = executeRequest(payload);

        System.out.println(responseBulkDelete.getEntity().toString());

        code = responseBulkDelete.getStatus();

        if (!VALID_HTTP_STATUS_CODES.contains(code)) {
            System.out.println("Response Code: " + code + "\tEntity: " + responseBulkDelete.getEntity());
            System.out.println("Response Code: " + code + "\tEntity: " + responseBulkDelete.getEntity());
        }
        assertEquals(Response.Status.CREATED.getStatusCode(),
                responseBulkDelete.getStatus(),
                "Expected to return status created from the response");
        assertEquals(1,
                StringUtils.countMatches(responseBulkDelete.getEntity().toString(), "{\"204\":null}"),
                "Contains 0 {\"204\":null}");

    }

    protected void doSetupResource(String uri, String payload) throws JSONException, AAIException {

        when(uriInfo.getPath()).thenReturn(uri);
        when(uriInfo.getPath(false)).thenReturn(uri);

        Response response = resourcesController.getLegacy(schemaVersions.getDefaultVersion().toString(), uri, -1, -1, false,
                "all", "false", httpHeaders, uriInfo, new MockHttpServletRequest("GET", "http://www.test.com"));

        assertNotNull(response, "Response from the legacy moxy consumer returned null");
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(),
                response.getStatus(),
                "Expected to not have the data already in memory");

        response = resourcesController.update(payload, schemaVersions.getDefaultVersion().toString(), uri, httpHeaders,
                uriInfo, new MockHttpServletRequest("PUT", "http://www.test.com"));

        assertNotNull(response, "Response from the legacy moxy consumer returned null");
        int code = response.getStatus();
        if (!VALID_HTTP_STATUS_CODES.contains(code)) {
            System.out.println("Response Code: " + code + "\tEntity: " + response.getEntity());
        }
        assertEquals(Response.Status.CREATED.getStatusCode(),
                response.getStatus(),
                "Expected to return status created from the response");

        queryParameters.add("depth", "10000");
        response = resourcesController.getLegacy(schemaVersions.getDefaultVersion().toString(), uri, -1, -1, false,
                "all", "false", httpHeaders, uriInfo, new MockHttpServletRequest("GET", "http://www.test.com"));

        assertNotNull(response, "Response from the legacy moxy consumer returned null");
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus(), "Expected to return the pserver data that was just put in memory");

        if ("".equalsIgnoreCase(payload)) {
            payload = "{}";
        }

        JSONAssert.assertEquals(payload, response.getEntity().toString(), false);
    }

    @Test
    public void bulkAddInvalidMethodTest() throws IOException {

        when(uriInfo.getPath()).thenReturn(uri);
        when(uriInfo.getPath(false)).thenReturn(uri);

        String payload = getBulkPayload("pserver-transactions-invalid-method");
        Response response = executeRequest(payload);

        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus(), "Valid Response Code");
        assertEquals(1,
                StringUtils.countMatches(response.getEntity().toString(), "{\"400\":\"{"),
                "Contains 1 {\"400\":\"{");
        assertEquals(1,
                StringUtils.countMatches(response.getEntity().toString(), "ERR.5.4.6118"),
                "Contains 1 ERR.5.4.6118");
    }

    @Test
    public void bulkAddThrowExceptionWhenPayloadContainsNoTransactionsTest() {

        when(uriInfo.getPath()).thenReturn(uri);
        when(uriInfo.getPath(false)).thenReturn(uri);

        String payload = "{\"transactions\":[]}";
        Response response = executeRequest(payload);

        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus(), "Bad Request");
        assertEquals(true, response.getEntity().toString().contains("ERR.5.4.6118"), "Contains error code");
    }

    @Test
    public void bulkAddThrowExceptionWhenInvalidJsonTest() throws IOException {

        when(uriInfo.getPath()).thenReturn(uri);
        when(uriInfo.getPath(false)).thenReturn(uri);

        String payload = "{";
        Response response = executeRequest(payload);

        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus(), "Bad Request");
        assertEquals(true, response.getEntity().toString().contains("ERR.5.4.6111"), "Contains error code");
    }

    @Test
    public void bulkProcessCheckMeetsLimit() throws IOException {
        when(uriInfo.getPath()).thenReturn(uri);
        when(uriInfo.getPath(false)).thenReturn(uri);

        String payload = getBulkPayload("pserver-bulk-limit-meet");
        Response response = executeRequest(payload);

        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus(), "Created");
        assertEquals(30,
                StringUtils.countMatches(response.getEntity().toString(), "{\"201\":null}"),
                "Contains 30 {\"201\":null}");
    }

    @Test
    public void bulkProcessCheckExceedsLimit() throws IOException {
        when(uriInfo.getPath()).thenReturn(uri);
        when(uriInfo.getPath(false)).thenReturn(uri);

        String payload = getBulkPayload("pserver-bulk-limit-exceed");
        Response response = executeRequest(payload);

        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus(), "Bad Request");
        assertEquals(true, response.getEntity().toString().contains("ERR.5.4.6147"), "Contains error code");
    }

    @Override
    protected BulkConsumer getConsumer() {
        return new BulkProcessConsumer();
    }

    @Override
    protected String getUri() {
        return "/aai/" + schemaVersions.getDefaultVersion().toString() + "/bulkprocess";
    }
}

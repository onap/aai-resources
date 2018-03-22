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
import static org.mockito.Mockito.when;

import java.io.IOException;

import javax.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import org.junit.Test;
import org.onap.aai.introspection.Version;
import org.skyscreamer.jsonassert.JSONAssert;

import com.att.eelf.configuration.EELFLogger;
import com.att.eelf.configuration.EELFManager;
import org.springframework.mock.web.MockHttpServletRequest;

public class BulkProcessConsumerTest extends BulkProcessorTestAbstraction {

    private static final EELFLogger logger = EELFManager.getInstance().getLogger(BulkProcessConsumerTest.class.getName());
    private LegacyMoxyConsumer legacyMoxyConsumer;

	@Test
    public void bulkAddPayloadInBulkProcessTest() throws IOException {

        when(uriInfo.getPath()).thenReturn(uri);
        when(uriInfo.getPath(false)).thenReturn(uri);

        String payload = getBulkPayload("pserver-transactions");
        Response response = executeRequest(payload);

        assertEquals("Valid Response Code", Response.Status.CREATED.getStatusCode(), response.getStatus());
        assertEquals("Contains 3 {\"201\":null}", 3, StringUtils.countMatches(response.getEntity().toString(), "{\"201\":null}"));
    }
	
	@Test
    public void bulkProcessPayloadTest() throws IOException {

        when(uriInfo.getPath()).thenReturn(uri);
        when(uriInfo.getPath(false)).thenReturn(uri);

        String payload = getBulkPayload("pserver-bulk-process-transactions");
        Response response = executeRequest(payload);

        assertEquals("Valid Response Code", Response.Status.CREATED.getStatusCode(), response.getStatus());
        assertEquals("Contains 1 {\"201\":null}", 1, StringUtils.countMatches(response.getEntity().toString(), "{\"201\":null}"));
        assertEquals("Contains 1 {\"404\":\"{", 1, StringUtils.countMatches(response.getEntity().toString(), "{\"404\":\"{"));
        assertEquals("Contains 1 ERR.5.4.6114", 1, StringUtils.countMatches(response.getEntity().toString(), "ERR.5.4.6114"));
    }
	
	@Test
    public void bulkProcessComplexDeletePayloadTest() throws IOException {

        when(uriInfo.getPath()).thenReturn(uri);
        when(uriInfo.getPath(false)).thenReturn(uri);

        String payload = getBulkPayload("complex-bulk-process-transactions");
        Response response = executeRequest(payload);

        System.out.println(response.getEntity().toString());
        assertEquals("Valid Response Code", Response.Status.CREATED.getStatusCode(), response.getStatus());
        assertEquals("Contains 0 {\"201\":null}", 0, StringUtils.countMatches(response.getEntity().toString(), "{\"201\":null}"));
        assertEquals("Contains 1 {\"404\":\"{", 1, StringUtils.countMatches(response.getEntity().toString(), "{\"404\":\"{"));
        assertEquals("Contains 1 ERR.5.4.6114", 1, StringUtils.countMatches(response.getEntity().toString(), "ERR.5.4.6114"));
    }
	
	@Test
    public void testBulkDeletePserverAndComplexRelationship() throws IOException, JSONException {
		
		legacyMoxyConsumer  = new LegacyMoxyConsumer();

        String pserverData = getPayload("payloads/relationship/pserver-bugfix.json");
        String complexData = getPayload("payloads/relationship/complex-bugfix.json");

        String hostname = "pserver-9876543210-77-jenkins";
        String physicalLocationId ="complex-987654321-77-jenkins";

        String pserverUri = String.format("cloud-infrastructure/pservers/pserver/%s", hostname);
        String complexUri = String.format("cloud-infrastructure/complexes/complex/%s", physicalLocationId);

        doSetupResource(pserverUri, pserverData);
        doSetupResource(complexUri, complexData);

        String complexToPserverRelationshipData = getPayload("payloads/relationship/pserver-complex-relationship-for-bulk.json");
        String complexToPserverRelationshipUri = String.format(
                "cloud-infrastructure/pservers/pserver/%s/relationship-list/relationship", hostname);

        Response response = legacyMoxyConsumer.updateRelationship(
                complexToPserverRelationshipData,
                Version.getLatest().toString(),
                complexToPserverRelationshipUri,
                httpHeaders,
                uriInfo,
                new MockHttpServletRequest("DELETE", "http://www.test.com")
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
        
        String payload = getBulkPayload("complex-bulk-process-delete-transactions");
        Response responseBulkDelete = executeRequest(payload);

        System.out.println(responseBulkDelete.getEntity().toString());
        
        code = responseBulkDelete.getStatus();
        
        if(!VALID_HTTP_STATUS_CODES.contains(code)){
            System.out.println("Response Code: " + code + "\tEntity: " +  responseBulkDelete.getEntity());
            System.out.println("Response Code: " + code + "\tEntity: " +  responseBulkDelete.getEntity());
        } 
        assertEquals("Expected to return status created from the response",
        		Response.Status.CREATED.getStatusCode(), responseBulkDelete.getStatus());
        assertEquals("Contains 0 {\"204\":null}", 1, StringUtils.countMatches(responseBulkDelete.getEntity().toString(), "{\"204\":null}"));
        
    }
	
    protected void doSetupResource(String uri, String payload) throws JSONException {

        when(uriInfo.getPath()).thenReturn(uri);
        when(uriInfo.getPath(false)).thenReturn(uri);

        Response response = legacyMoxyConsumer.getLegacy(
                "",
                "-1",
                "-1",
                Version.getLatest().toString(),
                uri,
                "all",
                "false",
                httpHeaders,
                uriInfo,
                new MockHttpServletRequest("GET", "http://www.test.com")
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
                new MockHttpServletRequest("PUT", "http://www.test.com")
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
                "-1",
                "-1",
                Version.getLatest().toString(),
                uri,
                "all",
                "false",
                httpHeaders,
                uriInfo,
                new MockHttpServletRequest("GET", "http://www.test.com")
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
    public void bulkAddInvalidMethodTest() throws IOException {

        when(uriInfo.getPath()).thenReturn(uri);
        when(uriInfo.getPath(false)).thenReturn(uri);

        String payload = getBulkPayload("pserver-transactions-invalid-method");
        Response response = executeRequest(payload);

        assertEquals("Valid Response Code", Response.Status.CREATED.getStatusCode(), response.getStatus());
        assertEquals("Contains 1 {\"400\":\"{", 1, StringUtils.countMatches(response.getEntity().toString(), "{\"400\":\"{"));
        assertEquals("Contains 1 ERR.5.4.6118", 1, StringUtils.countMatches(response.getEntity().toString(), "ERR.5.4.6118"));
    }

    @Test
    public void bulkAddThrowExceptionWhenPayloadContainsNoTransactionsTest(){

        when(uriInfo.getPath()).thenReturn(uri);
        when(uriInfo.getPath(false)).thenReturn(uri);

        String payload = "{\"transactions\":[]}";
        Response response = executeRequest(payload);

        assertEquals("Bad Request", Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        assertEquals("Contains error code", true, response.getEntity().toString().contains("ERR.5.4.6118"));
    }

    @Test
    public void bulkAddThrowExceptionWhenInvalidJsonTest() throws IOException {

        when(uriInfo.getPath()).thenReturn(uri);
        when(uriInfo.getPath(false)).thenReturn(uri);

        String payload = "{";
        Response response = executeRequest(payload);

        assertEquals("Bad Request", Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        assertEquals("Contains error code", true, response.getEntity().toString().contains("ERR.5.4.6111"));
    }
    @Test
    public void bulkProcessCheckMeetsLimit() throws IOException{
        when(uriInfo.getPath()).thenReturn(uri);
        when(uriInfo.getPath(false)).thenReturn(uri);

        String payload = getBulkPayload("pserver-bulk-limit-meet");
        Response response = executeRequest(payload);

        assertEquals("Created", Response.Status.CREATED.getStatusCode(), response.getStatus());
        assertEquals("Contains 30 {\"201\":null}", 30, StringUtils.countMatches(response.getEntity().toString(), "{\"201\":null}"));
    }

    @Test
    public void bulkProcessCheckExceedsLimit() throws IOException{
        when(uriInfo.getPath()).thenReturn(uri);
        when(uriInfo.getPath(false)).thenReturn(uri);

        String payload = getBulkPayload("pserver-bulk-limit-exceed");
        Response response = executeRequest(payload);

        assertEquals("Bad Request", Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        assertEquals("Contains error code", true, response.getEntity().toString().contains("ERR.5.4.6147"));
    }
    
    @Override
    protected BulkConsumer getConsumer(){
        return new BulkProcessConsumer();
    }
  
    @Override
    protected String getUri() {
		return "/aai/" + Version.getLatest().toString() + "/bulkprocess";
	}
}

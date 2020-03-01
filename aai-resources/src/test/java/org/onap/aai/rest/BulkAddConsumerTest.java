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
import static org.mockito.Mockito.when;

import java.io.IOException;

import javax.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
public class BulkAddConsumerTest extends BulkProcessorTestAbstraction {

    private static final Logger logger = LoggerFactory.getLogger(BulkAddConsumerTest.class.getName());


	@Test
    public void validBulkAddTest() throws IOException {

        when(uriInfo.getPath()).thenReturn(uri);
        when(uriInfo.getPath(false)).thenReturn(uri);

        String payload = getBulkPayload("pserver-transactions");
        Response response = executeRequest(payload);

        assertEquals("Valid Response Code", Response.Status.CREATED.getStatusCode(), response.getStatus());
        assertEquals("Contains 3 {\"201\":null}", 3, StringUtils.countMatches(response.getEntity().toString(), "{\"201\":null}"));
    }
	
	@Test
    public void bulkProcessPayloadInBulkAddTest() throws IOException {

        when(uriInfo.getPath()).thenReturn(uri);
        when(uriInfo.getPath(false)).thenReturn(uri);

        String payload = getBulkPayload("pserver-bulk-process-transactions");
        Response response = executeRequest(payload);

        assertEquals("Valid Response Code", Response.Status.CREATED.getStatusCode(), response.getStatus());
        assertEquals("Contains 1 {\"201\":null}", 1, StringUtils.countMatches(response.getEntity().toString(), "{\"201\":null}"));
        assertEquals("Contains 1 {\"400\":\"{", 1, StringUtils.countMatches(response.getEntity().toString(), "{\"400\":\"{"));
        assertEquals("Contains 1 ERR.5.4.6118", 1, StringUtils.countMatches(response.getEntity().toString(), "ERR.5.4.6118"));
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
    public void bulkAddCheckMeetsLimit() throws IOException{
        when(uriInfo.getPath()).thenReturn(uri);
        when(uriInfo.getPath(false)).thenReturn(uri);

        String payload = getBulkPayload("pserver-bulk-limit-meet");
        Response response = executeRequest(payload);

        assertEquals("Created", Response.Status.CREATED.getStatusCode(), response.getStatus());
        assertEquals("Contains 30 {\"201\":null}", 30, StringUtils.countMatches(response.getEntity().toString(), "{\"201\":null}"));
    }

    @Test
    public void bulkAddCheckExceedsLimit() throws IOException{
        when(uriInfo.getPath()).thenReturn(uri);
        when(uriInfo.getPath(false)).thenReturn(uri);

        String payload = getBulkPayload("pserver-bulk-limit-exceed");
        Response response = executeRequest(payload);

        assertEquals("Bad Request", Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        assertEquals("Contains error code", true, response.getEntity().toString().contains("ERR.5.4.6147"));
    }
    
    @Override
    protected BulkConsumer getConsumer(){
        return new BulkAddConsumer();
    }
  
    @Override
    protected String getUri() {
		return "/aai/" + schemaVersions.getDefaultVersion().toString() + "/bulkadd";
	}
}

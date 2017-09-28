/**
 * ============LICENSE_START=======================================================
 * org.onap.aai
 * ================================================================================
 * Copyright © 2017 AT&T Intellectual Property. All rights reserved.
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
 *
 * ECOMP is a trademark and service mark of AT&T Intellectual Property.
 */
package org.onap.aai.rest;

import com.att.eelf.configuration.EELFLogger;
import com.att.eelf.configuration.EELFManager;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;
import org.onap.aai.AAISetup;
import org.onap.aai.dbmap.AAIGraph;
import org.onap.aai.introspection.ModelInjestor;
import org.onap.aai.introspection.Version;

import javax.ws.rs.core.*;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.when;

public class BulkProcessConsumerTest extends BulkAddConsumerTest {

    private static final EELFLogger logger = EELFManager.getInstance().getLogger(BulkProcessConsumerTest.class.getName());

    @Test
    public void testBulkAddCreatedWhenOneTransactionInPayloadContainsNotAllowedVerb() throws IOException {

        String uri = "/aai/v11/bulkadd";

        when(uriInfo.getPath()).thenReturn(uri);
        when(uriInfo.getPath(false)).thenReturn(uri);

        String payload = getBulkPayload("pserver-transactions-invalid");
        Response response = bulkConsumer.bulkAdd(
                payload,
                Version.getLatest().toString(),
                httpHeaders,
                uriInfo,
                null
        );

        System.out.println("Code: " + response.getStatus() + "\tResponse: " + response.getEntity());
        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
    }

    @Override
    public BulkConsumer getConsumer(){
        return new BulkProcessConsumer();
    }
}

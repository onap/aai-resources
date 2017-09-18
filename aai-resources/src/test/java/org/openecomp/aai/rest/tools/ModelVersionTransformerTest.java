/*-
 * ============LICENSE_START=======================================================
 * org.openecomp.aai
 * ================================================================================
 * Copyright (C) 2017 AT&T Intellectual Property. All rights reserved.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ============LICENSE_END=========================================================
 */

package org.openecomp.aai.rest.tools;

import com.att.eelf.configuration.EELFLogger;
import com.att.eelf.configuration.EELFManager;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;
import org.openecomp.aai.AAISetup;
import org.openecomp.aai.dbmap.AAIGraph;
import org.openecomp.aai.introspection.ModelInjestor;
import org.skyscreamer.jsonassert.JSONAssert;

import javax.ws.rs.core.*;
import java.util.*;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.when;

public class ModelVersionTransformerTest extends AAISetup {


    protected static final MediaType APPLICATION_JSON = MediaType.valueOf("application/json");

    private static final Set<Integer> VALID_HTTP_STATUS_CODES = new HashSet<>();

    static {
        VALID_HTTP_STATUS_CODES.add(200);
        VALID_HTTP_STATUS_CODES.add(201);
        VALID_HTTP_STATUS_CODES.add(204);
    }

    private ModelVersionTransformer modelVersionTransformer;

    private HttpHeaders httpHeaders;

    private UriInfo uriInfo;

    private MultivaluedMap<String, String> headersMultiMap;
    private MultivaluedMap<String, String> queryParameters;

    private List<String> aaiRequestContextList;

    private List<MediaType> outputMediaTypes;

    private static final EELFLogger logger = EELFManager.getInstance().getLogger(ModelVersionTransformerTest.class.getName());

    @BeforeClass
    public static void setupRest(){
        AAIGraph.getInstance();
        ModelInjestor.getInstance();
    }

    @Before
    public void setup(){
        logger.info("Starting the setup for the integration tests of Rest Endpoints");

        modelVersionTransformer  = new ModelVersionTransformer();
        httpHeaders         = Mockito.mock(HttpHeaders.class);
        uriInfo             = Mockito.mock(UriInfo.class);

        headersMultiMap     = new MultivaluedHashMap<>();
        queryParameters     = Mockito.spy(new MultivaluedHashMap<>());

        headersMultiMap.add("X-FromAppId", "JUNIT");
        headersMultiMap.add("X-TransactionId", UUID.randomUUID().toString());
        headersMultiMap.add("Real-Time", "true");
        headersMultiMap.add("Content-Type", "application/xml");
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
    public void modelTransform() throws Exception {

        String modelXmlPayload  = getPayload("payloads/modeltransforms/model.xml");
        String modelJsonPayload = getPayload("payloads/modeltransforms/model.json");

        String uri = "modeltransform";

        when(uriInfo.getPath()).thenReturn(uri);
        // Comment the following line for the null pointer exception
        when(uriInfo.getPath(false)).thenReturn(uri);

        outputMediaTypes = new ArrayList<>();
        outputMediaTypes.add(MediaType.valueOf("application/xml"));

        when(httpHeaders.getAcceptableMediaTypes()).thenReturn(outputMediaTypes);
        when(httpHeaders.getRequestHeaders()).thenReturn(headersMultiMap);
        when(httpHeaders.getMediaType()).thenReturn(MediaType.valueOf("application/xml"));

        Response response = modelVersionTransformer.modelTransform(modelXmlPayload,uri, httpHeaders, uriInfo, null);

        assertNotNull(response);
    }
}

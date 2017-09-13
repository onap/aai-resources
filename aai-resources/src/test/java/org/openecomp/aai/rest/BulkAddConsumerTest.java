package org.openecomp.aai.rest;

import com.att.eelf.configuration.EELFLogger;
import com.att.eelf.configuration.EELFManager;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;
import org.openecomp.aai.AAISetup;
import org.openecomp.aai.dbmap.AAIGraph;
import org.openecomp.aai.introspection.ModelInjestor;
import org.openecomp.aai.introspection.Version;

import javax.ws.rs.core.*;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.when;

public class BulkAddConsumerTest extends AAISetup {

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

    private static final EELFLogger logger = EELFManager.getInstance().getLogger(BulkAddConsumerTest.class.getName());

    @BeforeClass
    public static void setupRest(){
        AAIGraph.getInstance();
        ModelInjestor.getInstance();
    }

    @Before
    public void setup(){
        logger.info("Starting the setup for the integration tests of Rest Endpoints");

        bulkConsumer     = getConsumer();
        httpHeaders         = Mockito.mock(HttpHeaders.class);
        uriInfo             = Mockito.mock(UriInfo.class);

        headersMultiMap     = new MultivaluedHashMap<>();
        queryParameters     = Mockito.spy(new MultivaluedHashMap<>());

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
        Mockito.doReturn(null).when(queryParameters).remove(anyObject());

        when(httpHeaders.getMediaType()).thenReturn(APPLICATION_JSON);
    }

    @Test
    public void testBulkAdd() throws IOException {

        String uri = "/aai/v11/bulkadd";

        when(uriInfo.getPath()).thenReturn(uri);
        when(uriInfo.getPath(false)).thenReturn(uri);

        String payload = getBulkPayload("pserver-transactions");
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

    @Test
    public void testBulkAddThrowExceptionWhenPayloadContainsNoTransactions(){

        String uri = "/aai/v11/bulkadd";

        when(uriInfo.getPath()).thenReturn(uri);
        when(uriInfo.getPath(false)).thenReturn(uri);

        String payload = "{\"transactions\":[]}";
        Response response = bulkConsumer.bulkAdd(
                payload,
                Version.getLatest().toString(),
                httpHeaders,
                uriInfo,
                null
        );

        System.out.println("Code: " + response.getStatus() + "\tResponse: " + response.getEntity());
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    @Test
    public void testBulkAddThrowExceptionWhenInvalidJson() throws IOException {

        String uri = "/aai/v11/bulkadd";

        when(uriInfo.getPath()).thenReturn(uri);
        when(uriInfo.getPath(false)).thenReturn(uri);

        String payload = "{";
        Response response = bulkConsumer.bulkAdd(
                payload,
                Version.getLatest().toString(),
                httpHeaders,
                uriInfo,
                null
        );

        System.out.println("Code: " + response.getStatus() + "\tResponse: " + response.getEntity());
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());

        // TODO - Verify the result output and check if it contains an 400 in the list
    }

    public String getBulkPayload(String bulkName) throws IOException {
        return getPayload("payloads/bulk/" + bulkName + ".json");
    }

    public BulkConsumer getConsumer(){
        return new BulkAddConsumer();
    }
}
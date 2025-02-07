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

package org.onap.aai.rest.bulk;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.Optional;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.Response;

import org.apache.tinkerpop.gremlin.structure.Property;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.structure.VertexProperty;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.onap.aai.db.props.AAIProperties;
import org.onap.aai.dbmap.AAIGraph;
import org.onap.aai.interceptors.pre.UTF8ValidationFilter;
import org.onap.aai.rest.BulkConsumer;
import org.onap.aai.rest.BulkProcessorTestAbstraction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;

@TestPropertySource(properties = {"delta.events.enabled=true",})
public class BulkSingleTransactionConsumerTest extends BulkProcessorTestAbstraction {

    private BulkSingleTransactionConsumer bulkSingleTransactionConsumer = new BulkSingleTransactionConsumer("/aai");

    
    public String name;

    private String sot = "Junit";

    @BeforeEach
    public void before(TestInfo testInfo) {
        Optional<Method> testMethod = testInfo.getTestMethod();
        if (testMethod.isPresent()) {
            this.name = testMethod.get().getName();
        }
        sot = "JUNIT-" + name;
        when(uriInfo.getPath()).thenReturn(uri);
        when(uriInfo.getPath(false)).thenReturn(uri);
        headersMultiMap.addFirst("X-FromAppId", sot);

    }

    @Test
    public void addPserverPatchSamePserverTest() throws IOException {

        String payload = getBulkPayload("single-transaction/put-patch-same-pserver").replaceAll("<methodName>", name);
        Response response = executeRequest(payload);

        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus(), "Request success");
        assertEquals(Long.valueOf(1L), AAIGraph.getInstance().getGraph()
                .newTransaction().traversal().V().has(AAIProperties.SOURCE_OF_TRUTH, sot).count().next(), "1 vertex from this test in graph");
        assertEquals(Long.valueOf(1L),
                AAIGraph.getInstance().getGraph().newTransaction().traversal().V()
                        .has(AAIProperties.SOURCE_OF_TRUTH, sot).has("fqdn", "patched-fqdn").count().next(),
                "1 vertex from this test  with fqdn = patched-fqdn");

    }

    @Test
    public void putPserverComplexRelBetween() throws IOException {

        String payload = getBulkPayload("single-transaction/put-pserver-complex-rel-between").replaceAll("<methodName>", name);
        Response response = executeRequest(payload);

        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus(), "Request success");
        assertEquals(Long.valueOf(2L), AAIGraph.getInstance().getGraph()
                .newTransaction().traversal().V().has(AAIProperties.SOURCE_OF_TRUTH, sot).count().next(), "2 vertex from this test in graph");
        assertEquals(Long.valueOf(1L),
                AAIGraph.getInstance().getGraph().newTransaction().traversal().V()
                        .has(AAIProperties.SOURCE_OF_TRUTH, sot).has(AAIProperties.NODE_TYPE, "complex").count()
                        .next(),
                "1 complex vertex");
        assertEquals(Long.valueOf(1L),
                AAIGraph.getInstance().getGraph().newTransaction().traversal().V()
                        .has(AAIProperties.SOURCE_OF_TRUTH, sot).has(AAIProperties.NODE_TYPE, "pserver").count()
                        .next(),
                "1 pserver vertex");
        assertEquals(Long.valueOf(1L),
                AAIGraph.getInstance().getGraph().newTransaction().traversal().V()
                        .has(AAIProperties.SOURCE_OF_TRUTH, sot).has(AAIProperties.NODE_TYPE, "pserver").bothE()
                        .otherV().has(AAIProperties.NODE_TYPE, "complex").has(AAIProperties.SOURCE_OF_TRUTH, sot)
                        .count().next(),
                "pserver has edge to complex");

    }

    @Test
    public void putPatchSamePserverPutAnotherPserver() throws IOException {
        String payload = getBulkPayload("single-transaction/put-patch-same-pserver-put-another-pserver")
                .replaceAll("<methodName>", name);
        Response response = executeRequest(payload);

        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus(), "Request success");
        assertEquals(Long.valueOf(2L), AAIGraph.getInstance().getGraph()
                .newTransaction().traversal().V().has(AAIProperties.SOURCE_OF_TRUTH, sot).count().next(), "2 vertex from this test in graph");
        assertEquals(Long.valueOf(1L),
                AAIGraph.getInstance().getGraph().newTransaction().traversal().V()
                        .has(AAIProperties.SOURCE_OF_TRUTH, sot).has("hostname", "pserver-1-" + name)
                        .has("fqdn", "patched-fqdn").count().next(),
                "pserver 1 has hostname pserver-1-" + name + " fqdn = patched-fqdn");
        assertEquals(Long.valueOf(1L),
                AAIGraph.getInstance().getGraph().newTransaction().traversal().V()
                        .has(AAIProperties.SOURCE_OF_TRUTH, sot).has("hostname", "pserver-2-" + name)
                        .count().next(),
                "pserver 2 has hostname pserver-2-" + name);
    }

    protected String asString(Vertex v) {
        final JSONObject result = new JSONObject();
        Iterator<VertexProperty<Object>> properties = v.properties();
        Property<Object> pk = null;
        try {
            while (properties.hasNext()) {
                pk = properties.next();
                result.put(pk.key(), pk.value());
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return result.toString();
    }

    @Test
    public void putPserverComplexRelBetweenDelExistingGvnf() throws IOException {

        AAIGraph.getInstance().getGraph().traversal().addV().property(AAIProperties.NODE_TYPE, "generic-vnf")
                .property(AAIProperties.SOURCE_OF_TRUTH, sot)
                .property(AAIProperties.AAI_URI,
                        "/network/generic-vnfs/generic-vnf/gvnf-putPserverComplexRelBetweenDelExistingGvnf")
                .property(AAIProperties.RESOURCE_VERSION, "0").property("vnf-id", "gvnf-" + name)
                .next();
        AAIGraph.getInstance().getGraph().tx().commit();

        assertEquals(Long.valueOf(1L),
                AAIGraph.getInstance().getGraph().newTransaction().traversal().V()
                        .has(AAIProperties.SOURCE_OF_TRUTH, sot).has(AAIProperties.NODE_TYPE, "generic-vnf").count()
                        .next(),
                "1 generic-vnf vertex exists before payload");

        String payload = getBulkPayload("single-transaction/put-pserver-complex-rel-between-del-existing-gvnf")
                .replaceAll("<methodName>", name);
        Response response = executeRequest(payload);

        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus(), "Request success");
        assertEquals(Long.valueOf(2L), AAIGraph.getInstance().getGraph()
                .newTransaction().traversal().V().has(AAIProperties.SOURCE_OF_TRUTH, sot).count().next(), "2 vertex from this test in graph");
        assertEquals(Long.valueOf(1L),
                AAIGraph.getInstance().getGraph().newTransaction().traversal().V()
                        .has(AAIProperties.SOURCE_OF_TRUTH, sot).has(AAIProperties.NODE_TYPE, "complex").count()
                        .next(),
                "1 complex vertex");
        assertEquals(Long.valueOf(1L),
                AAIGraph.getInstance().getGraph().newTransaction().traversal().V()
                        .has(AAIProperties.SOURCE_OF_TRUTH, sot).has(AAIProperties.NODE_TYPE, "pserver").count()
                        .next(),
                "1 pserver vertex");
        assertEquals(Long.valueOf(1L),
                AAIGraph.getInstance().getGraph().newTransaction().traversal().V()
                        .has(AAIProperties.SOURCE_OF_TRUTH, sot).has(AAIProperties.NODE_TYPE, "pserver").bothE()
                        .otherV().has(AAIProperties.NODE_TYPE, "complex").has(AAIProperties.SOURCE_OF_TRUTH, sot)
                        .count().next(),
                "pserver has edge to complex");
        assertEquals(Long.valueOf(0L),
                AAIGraph.getInstance().getGraph().newTransaction().traversal().V()
                        .has(AAIProperties.SOURCE_OF_TRUTH, sot).has(AAIProperties.NODE_TYPE, "generic-vnf").count()
                        .next(),
                "0 generic-vnf vertex exists after payload");

        assertThat("Response contains 204 status.", response.getEntity().toString(),
                containsString("\"response-status-code\":204"));

    }

    @Test
    public void putPserverComplexRelBetweenDelExistingGvnfFail() throws IOException {

        AAIGraph.getInstance().getGraph().traversal().addV().property(AAIProperties.NODE_TYPE, "generic-vnf")
                .property(AAIProperties.SOURCE_OF_TRUTH, sot)
                .property(AAIProperties.AAI_URI,
                        "/network/generic-vnfs/generic-vnf/gvnf-putPserverComplexRelBetweenDelExistingGvnfFail")
                .property(AAIProperties.RESOURCE_VERSION, "0").property("vnf-id", "gvnf-" + name)
                .next();
        AAIGraph.getInstance().getGraph().tx().commit();

        assertEquals(Long.valueOf(1L),
                AAIGraph.getInstance().getGraph().newTransaction().traversal().V()
                        .has(AAIProperties.SOURCE_OF_TRUTH, sot).has(AAIProperties.NODE_TYPE, "generic-vnf").count()
                        .next(),
                "1 generic-vnf vertex exists before payload");

        String payload = getBulkPayload("single-transaction/put-pserver-complex-rel-between-del-existing-gvnf-fail")
                .replaceAll("<methodName>", name);
        Response response = executeRequest(payload);

        System.out.println(response.getEntity().toString());

        assertEquals(Response.Status.PRECONDITION_FAILED.getStatusCode(), response.getStatus(), "Request failed");

        assertEquals(Long.valueOf(1L), AAIGraph.getInstance().getGraph()
                .newTransaction().traversal().V().has(AAIProperties.SOURCE_OF_TRUTH, sot).count().next(), "1 vertex exists after payload due to failure");

        assertThat("Response contains resource version msg for failed transaction.", response.getEntity().toString(),
                containsString("Precondition Failed:resource-version MISMATCH for delete of generic-vnf"));

        assertThat("Response contains correct index of failed operation.", response.getEntity().toString(),
                containsString("Operation 3"));

        assertThat("Response contains correct status code.", response.getEntity().toString(),
                containsString("failed with status code (412"));

    }

    @Test
    public void checkExceedsLimit() throws IOException {

        String payload = getBulkPayload("single-transaction/pserver-bulk-limit-exceed");
        Response response = executeRequest(payload);

        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus(), "Request fails with 400");
        assertThat("Response contains payload limit msg.", response.getEntity().toString(),
                containsString("Payload Limit Reached, reduce payload: Allowed limit = "));
    }

    @Test
    public void invalidJson() {

        String payload = "{]}";// malformed json
        Response response = executeRequest(payload);

        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus(), "Request fails with 400");
        assertThat("Response contains invalid payload msg.", response.getEntity().toString(), containsString(
                "JSON processing error:Input payload does not follow bulk/single-transaction interface"));
    }

    @Test
    public void noOperations() {

        String payload = "{'operations':[]}";
        Response response = executeRequest(payload);

        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus(), "Request fails with 400");
        assertThat("Response contains invalid payload msg.", response.getEntity().toString(),
                containsString("Required Field not passed.: Payload has no objects to operate on"));
    }

    @Test
    public void invalidAction() throws IOException {

        String payload = getBulkPayload("single-transaction/invalid-action");
        Response response = executeRequest(payload);

        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus(), "Request fails with 400");
        assertThat("Response contains invalid payload msg.", response.getEntity().toString(),
                containsString("JSON processing error:input payload missing required properties"));
        assertThat("Response contains invalid payload details.", response.getEntity().toString(),
                containsString("[Operation 0 has invalid action 'create', Operation 1 has invalid action 'destroy']"));

    }

    @Test
    public void missingFields() throws IOException {

        String payload = getBulkPayload("single-transaction/missing-fields");
        Response response = executeRequest(payload);

        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus(), "Request fails with 400");
        assertThat("Response contains invalid payload msg.", response.getEntity().toString(),
                containsString("JSON processing error:input payload missing required properties"));
        assertThat("Response contains invalid payload details.", response.getEntity().toString(), containsString(
                "[Operation 0 missing 'body', Operation 1 missing 'action', Operation 2 missing 'uri']"));

    }

    @Test
    public void putComplexWithRelToNonExistentPserverBetween() throws IOException {

        String payload = getBulkPayload("single-transaction/put-complex-with-rel-to-non-existent")
                .replaceAll("<methodName>", name);
        Response response = executeRequest(payload);

        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus(), "Request success");
        assertEquals(Long.valueOf(0L), AAIGraph.getInstance().getGraph()
                .newTransaction().traversal().V().has(AAIProperties.SOURCE_OF_TRUTH, sot).count().next(), "0 vertex from this test in graph");
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus(), "Request fails with 404");

        assertThat("Response contains correct index of failed operation.", response.getEntity().toString(),
                containsString("Operation 0"));

        assertThat("Response contains correct status code.", response.getEntity().toString(),
                containsString("failed with status code (404"));

        assertThat("Response contains correct msg.", response.getEntity().toString(),
                containsString("target node:Node of type pserver. Could not find"));

        assertThat("Response contains correct Error Code.", response.getEntity().toString(),
                containsString("ERR.5.4.6129"));

    }

    @Test
    public void deleteChildRecreateChildTest() throws IOException {
        JsonArray requests =
                JsonParser
                        .parseString(getBulkPayload("single-transaction/delete-child-recreate-child")
                                .replaceAll("<methodName>", name))
                        .getAsJsonObject().getAsJsonArray("array");
        String payload = requests.get(0).toString();
        Response response = executeRequest(payload);
        System.out.println(response.getEntity().toString());
        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus(), "Request success");

        payload = requests.get(1).toString();
        response = executeRequest(payload);
        System.out.println(response.getEntity().toString());
        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus(), "Request success");
    }

    @Test
    public void deleteNodeRecreateNodeTest() throws IOException {
        JsonArray requests =
                JsonParser
                        .parseString(getBulkPayload("single-transaction/delete-node-recreate-node")
                                .replaceAll("<methodName>", name))
                        .getAsJsonObject().getAsJsonArray("array");
        String payload = requests.get(0).toString();
        Response response = executeRequest(payload);
        System.out.println(response.getEntity().toString());
        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus(), "Request success");

        payload = requests.get(1).toString();
        response = executeRequest(payload);
        System.out.println(response.getEntity().toString());
        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus(), "Request success");
    }

    @Test
    public void invalidNodeCreationPaylodTest() throws IOException {
        String payload = getBulkPayload("single-transaction/put-complex-with-missing-properties")
                .replaceAll("<methodName>", name);
        Response response = executeRequest(payload);

        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus(), "Request fails with 400");
        assertThat("Response contains correct index of failed operation.", response.getEntity().toString(),
                containsString("Error with operation 0"));
        assertThat("Response contains information about missing properties.", response.getEntity().toString(),
                containsString("Missing required property:"));
    }

    protected Response executeRequest(String finalPayload) {
        MockHttpServletRequest mockReq = new MockHttpServletRequest(HttpMethod.POST, "http://www.test.com");

        return bulkSingleTransactionConsumer.process(finalPayload, schemaVersions.getDefaultVersion().toString(),
                httpHeaders, uriInfo, mockReq);
    }

    @Override
    protected BulkConsumer getConsumer() {
        return null;
    }

    @Override
    protected String getUri() {
        return "/aai/" + schemaVersions.getDefaultVersion().toString() + "/bulk/single-transaction";
    }
}

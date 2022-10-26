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

import io.micrometer.core.annotation.Timed;

import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.*;

import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.javatuples.Pair;
import org.onap.aai.config.SpringContextAware;
import org.onap.aai.db.props.AAIProperties;
import org.onap.aai.exceptions.AAIException;
import org.onap.aai.introspection.Introspector;
import org.onap.aai.introspection.Loader;
import org.onap.aai.introspection.MarshallerProperties;
import org.onap.aai.introspection.ModelType;
import org.onap.aai.parsers.query.QueryParser;
import org.onap.aai.rest.db.DBRequest;
import org.onap.aai.rest.db.HttpEntry;
import org.onap.aai.restcore.HttpMethod;
import org.onap.aai.restcore.RESTAPI;
import org.onap.aai.serialization.engines.TransactionalGraphEngine;
import org.onap.aai.setup.SchemaVersion;

/**
 * The Class VertexIdConsumer.
 */
@Path("{version: v[1-9][0-9]*|latest}/resources")
@Timed
public class VertexIdConsumer extends RESTAPI {

    private ModelType introspectorFactoryType = ModelType.MOXY;

    private final String ID_ENDPOINT = "/id/{vertexid: \\d+}";

    private HttpEntry resourceHttpEntry;

    /**
     * Gets the by vertex id.
     *
     * @param content the content
     * @param versionParam the version param
     * @param vertexid the vertexid
     * @param depthParam the depth param
     * @param headers the headers
     * @param info the info
     * @param req the req
     * @return the by vertex id
     */
    @GET
    @Path(ID_ENDPOINT)
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public Response getByVertexId(String content, @PathParam("version") String versionParam,
            @PathParam("vertexid") long vertexid, @DefaultValue("all") @QueryParam("depth") String depthParam,
            @Context HttpHeaders headers, @Context UriInfo info, @Context HttpServletRequest req) {

        String outputMediaType = getMediaType(headers.getAcceptableMediaTypes());
        String sourceOfTruth = headers.getRequestHeaders().getFirst("X-FromAppId");
        String transId = headers.getRequestHeaders().getFirst("X-TransactionId");
        SchemaVersion version = new SchemaVersion(versionParam);
        Response response = null;
        TransactionalGraphEngine dbEngine = null;
        try {
            resourceHttpEntry = SpringContextAware.getBean("traversalUriHttpEntry", HttpEntry.class);
            resourceHttpEntry.setHttpEntryProperties(version);
            dbEngine = resourceHttpEntry.getDbEngine();
            Loader loader = resourceHttpEntry.getLoader();

            // get type of the object represented by the given id
            Vertex thisVertex = null;
            Iterator<Vertex> itr = dbEngine.asAdmin().getTraversalSource().V(vertexid);

            if (!itr.hasNext()) {
                throw new AAIException("AAI_6114", "no node at that vertex id");
            }
            thisVertex = itr.next();
            String objName = thisVertex.<String>property(AAIProperties.NODE_TYPE).orElse(null);

            QueryParser query = dbEngine.getQueryBuilder(thisVertex).createQueryFromObjectName(objName);

            Introspector obj = loader.introspectorFromName(query.getResultType());

            URI uriObject = UriBuilder.fromPath(info.getPath()).build();

            DBRequest request =
                    new DBRequest.Builder(HttpMethod.GET, uriObject, query, obj, headers, info, transId)
                            .customMarshaller(new MarshallerProperties.Builder(
                                    org.onap.aai.restcore.MediaType.getEnum(outputMediaType)).includeRoot(true).build())
                            .build();

            List<DBRequest> requests = new ArrayList<>();
            requests.add(request);
            Pair<Boolean, List<Pair<URI, Response>>> responsesTuple =
                    resourceHttpEntry.process(requests, sourceOfTruth);
            response = responsesTuple.getValue1().get(0).getValue1();
        } catch (AAIException e) {
            response = consumerExceptionResponseGenerator(headers, info, HttpMethod.GET, e);
        } catch (Exception e) {
            AAIException ex = new AAIException("AAI_4000", e);
            response = consumerExceptionResponseGenerator(headers, info, HttpMethod.GET, ex);
        } finally { // to close the janusgraph transaction (I think)
            if (dbEngine != null) {
                dbEngine.rollback();
            }
        }
        return response;
    }
}

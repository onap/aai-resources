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
import java.util.Iterator;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.*;
import jakarta.ws.rs.core.Response.Status;

import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.onap.aai.config.SpringContextAware;
import org.onap.aai.exceptions.AAIException;
import org.onap.aai.introspection.ModelType;
import org.onap.aai.rest.db.HttpEntry;
import org.onap.aai.restcore.HttpMethod;
import org.onap.aai.restcore.RESTAPI;
import org.onap.aai.serialization.db.DBSerializer;
import org.onap.aai.serialization.engines.TransactionalGraphEngine;
import org.onap.aai.setup.SchemaVersion;

/**
 * The Class URLFromVertexIdConsumer.
 */
@Timed
@Path("{version: v[1-9][0-9]*|latest}/generateurl")
public class URLFromVertexIdConsumer extends RESTAPI {
    private ModelType introspectorFactoryType = ModelType.MOXY;

    private final String ID_ENDPOINT = "/id/{vertexid: \\d+}";

    /**
     * Generate url from vertex id.
     *
     * @param versionParam the version param
     * @param vertexid the vertexid
     * @param headers the headers
     * @param info the info
     * @param req the req
     * @return the response
     */
    @GET
    @Path(ID_ENDPOINT)
    @Produces({MediaType.WILDCARD})
    public Response generateUrlFromVertexId(@PathParam("version") String versionParam,
            @PathParam("vertexid") long vertexid, @Context HttpHeaders headers, @Context UriInfo info,
            @Context HttpServletRequest req) {

        String sourceOfTruth = headers.getRequestHeaders().getFirst("X-FromAppId");

        SchemaVersion version = new SchemaVersion(versionParam);
        StringBuilder result = new StringBuilder();
        Response response;
        TransactionalGraphEngine dbEngine = null;
        try {
            HttpEntry resourceHttpEntry = SpringContextAware.getBean("traversalUriHttpEntry", HttpEntry.class);
            String serverBase = req.getRequestURL().toString().replaceAll("/(v[0-9]+|latest)/.*", "/");
            resourceHttpEntry.setHttpEntryProperties(version, serverBase);
            dbEngine = resourceHttpEntry.getDbEngine();

            DBSerializer serializer = new DBSerializer(version, dbEngine, introspectorFactoryType, sourceOfTruth);

            Iterator<Vertex> thisVertex = dbEngine.asAdmin().getTraversalSource().V(vertexid);

            if (!thisVertex.hasNext()) {
                throw new AAIException("AAI_6114", "no node at that vertex id");
            }
            URI uri = serializer.getURIForVertex(thisVertex.next());

            result.append(uri.getRawPath());
            result.insert(0, version);
            result.insert(0, serverBase);
            response = Response.ok().entity(result.toString()).status(Status.OK).type(MediaType.TEXT_PLAIN).build();
        } catch (AAIException e) {
            // TODO check that the details here are sensible
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

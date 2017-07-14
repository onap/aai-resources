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

package org.openecomp.aai.rest;

import java.net.URI;
import java.net.URL;
import java.util.Iterator;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.apache.tinkerpop.gremlin.structure.Vertex;

import org.openecomp.aai.dbmap.DBConnectionType;
import org.openecomp.aai.exceptions.AAIException;
import org.openecomp.aai.introspection.ModelType;
import org.openecomp.aai.introspection.Version;
import org.openecomp.aai.rest.db.HttpEntry;
import org.openecomp.aai.restcore.HttpMethod;
import org.openecomp.aai.restcore.RESTAPI;
import org.openecomp.aai.serialization.db.DBSerializer;
import org.openecomp.aai.serialization.engines.QueryStyle;
import org.openecomp.aai.serialization.engines.TransactionalGraphEngine;
import org.openecomp.aai.util.AAIConfig;
import org.openecomp.aai.workarounds.LegacyURITransformer;

/**
 * The Class URLFromVertexIdConsumer.
 */
@Path("{version: v[2789]|v1[01]}/generateurl")
public class URLFromVertexIdConsumer extends RESTAPI {
	private ModelType introspectorFactoryType = ModelType.MOXY;
	private QueryStyle queryStyle = QueryStyle.TRAVERSAL;
	
	private final String ID_ENDPOINT = "/id/{vertexid: \\d+}";
	
	/**
	 * Generate url from vertex id.
	 *
	 * @param content the content
	 * @param versionParam the version param
	 * @param vertexid the vertexid
	 * @param headers the headers
	 * @param info the info
	 * @param req the req
	 * @return the response
	 */
	@Path(ID_ENDPOINT)
	@Produces({ MediaType.TEXT_PLAIN })
	public Response generateUrlFromVertexId(String content, @PathParam("version")String versionParam, @PathParam("vertexid")long vertexid, @Context HttpHeaders headers, @Context UriInfo info, @Context HttpServletRequest req) {
		
		String sourceOfTruth = headers.getRequestHeaders().getFirst("X-FromAppId");
		String realTime = headers.getRequestHeaders().getFirst("Real-Time");

		Version version = Version.valueOf(versionParam);
		StringBuilder result = new StringBuilder();
		Response response = null;
		TransactionalGraphEngine dbEngine = null;
		try {
			DBConnectionType type = this.determineConnectionType(sourceOfTruth, realTime);
			HttpEntry httpEntry = new HttpEntry(version, introspectorFactoryType, queryStyle, type);
			dbEngine = httpEntry.getDbEngine();
			
			DBSerializer serializer = new DBSerializer(version, dbEngine, introspectorFactoryType, sourceOfTruth);

			Iterator<Vertex> thisVertex = dbEngine.asAdmin().getTraversalSource().V(vertexid);
			
			if (!thisVertex.hasNext()) {
				throw new AAIException("AAI_6114", "no node at that vertex id");
			}
			URI uri = serializer.getURIForVertex(thisVertex.next());

			result.append(uri.getRawPath());
			result.insert(0, version);
			result.insert(0, AAIConfig.get("aai.server.url.base"));
			LegacyURITransformer urlTransformer = LegacyURITransformer.getInstance();
			URI output = new URI(result.toString());

			response = Response.ok().entity(result.toString()).status(Status.OK).type(MediaType.TEXT_PLAIN).build();
		} catch (AAIException e) {
			//TODO check that the details here are sensible
			response = consumerExceptionResponseGenerator(headers, info, HttpMethod.GET, e);
		} catch (Exception e) {
			AAIException ex = new AAIException("AAI_4000", e);
			response = consumerExceptionResponseGenerator(headers, info, HttpMethod.GET, ex);
		} finally { //to close the titan transaction (I think)
			if (dbEngine != null) {
				dbEngine.rollback();
			}
		}
		return response;
	}
}

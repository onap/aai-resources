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

import org.onap.aai.config.SpringContextAware;
import org.onap.aai.exceptions.AAIException;
import org.onap.aai.introspection.Introspector;
import org.onap.aai.introspection.Loader;
import org.onap.aai.introspection.LoaderFactory;
import org.onap.aai.introspection.MarshallerProperties;
import org.onap.aai.introspection.ModelType;
import org.onap.aai.setup.SchemaVersion;
import org.onap.aai.introspection.generator.CreateExample;
import org.onap.aai.restcore.HttpMethod;
import org.onap.aai.restcore.RESTAPI;

/**
 * The Class ExampleConsumer.
 */
@Path("{version: v[1-9][0-9]*|latest}/examples")
public class ExampleConsumer extends RESTAPI {

	
	/**
	 * Gets the example.
	 *
	 * @param versionParam the version param
	 * @param type the type
	 * @param headers the headers
	 * @param info the info
	 * @param req the req
	 * @return the example
	 */
	@GET
	@Path("/{objectType: [^\\/]+}")
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	public Response getExample(@PathParam("version")String versionParam,  @PathParam("objectType")String type, @Context HttpHeaders headers, @Context UriInfo info, @Context HttpServletRequest req) {
		
		Status status = Status.INTERNAL_SERVER_ERROR;
		Response response = null;

		try {
			String mediaType = getMediaType(headers.getAcceptableMediaTypes());
			org.onap.aai.restcore.MediaType outputMediaType = org.onap.aai.restcore.MediaType.getEnum(mediaType);
			
			SchemaVersion version = new SchemaVersion(versionParam);
			Loader loader = SpringContextAware.getBean( LoaderFactory.class).createLoaderForVersion(ModelType.MOXY, version);
			
			CreateExample example = new CreateExample(loader, type);
			
			Introspector obj = example.getExampleObject();
			String result = "";
			if (obj != null) {
				status = Status.OK;
				MarshallerProperties properties = 
						new MarshallerProperties.Builder(outputMediaType).build();
				result = obj.marshal(properties);
			} else {
				
			}
			response = Response
					.ok(obj)
					.entity(result)
					.status(status)
					.type(outputMediaType.toString()).build();
		} catch (AAIException e) {
			//TODO check that the details here are sensible
			response = consumerExceptionResponseGenerator(headers, info, HttpMethod.GET, e);
		} catch (Exception e) {
			AAIException ex = new AAIException("AAI_4000", e);
			response = consumerExceptionResponseGenerator(headers, info, HttpMethod.GET, ex);
		}
		return response;
	}
}

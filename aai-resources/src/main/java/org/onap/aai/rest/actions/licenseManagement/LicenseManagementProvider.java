/*-
 * ============LICENSE_START=======================================================
 * org.onap.aai
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

package org.onap.aai.rest.actions.licenseManagement;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.javatuples.Pair;
import org.onap.aai.dbmap.DBConnectionType;
import org.onap.aai.exceptions.AAIException;
import org.onap.aai.extensions.AAIExtensionMap;
import org.onap.aai.introspection.Introspector;
import org.onap.aai.introspection.Loader;
import org.onap.aai.introspection.ModelType;
import org.onap.aai.introspection.Version;
import org.onap.aai.logging.ErrorLogHelper;
import org.onap.aai.parsers.query.QueryParser;
import org.onap.aai.rest.db.DBRequest;
import org.onap.aai.rest.db.HttpEntry;
import org.onap.aai.restcore.HttpMethod;
import org.onap.aai.restcore.RESTAPI;
import org.onap.aai.restcore.util.URITools;
import org.onap.aai.serialization.engines.QueryStyle;
import org.onap.aai.serialization.engines.TransactionalGraphEngine;
import org.onap.aai.util.AAIApiVersion;
import org.onap.aai.util.FormatDate;

import com.att.eelf.configuration.EELFLogger;
import com.att.eelf.configuration.EELFManager;

/**
 * The Class LicenseManagementProvider.
 */
@Path("/{parameter: v[789]|v1[012]}/actions/assignment")
public class LicenseManagementProvider extends RESTAPI {

	private static final EELFLogger LOGGER = EELFManager.getInstance().getLogger(LicenseManagementProvider.class);
	private static final String authPolicyFunctionName = "actions";

	public final String NEWADD = "/license-management";
	public final String UUIDENDPOINT = "/license-management/att-uuid/{att-uuid}";
	public final String RETURNENDPOINT = "/license-management/license-key/{license-key}";
	public final String ASSIGNENDPOINT = "/license-management/assignment-group-uuid/{assignment-group-uuid}";

	/**
	 * Assign license.
	 *
	 * @param headers the headers
	 * @param req the req
	 * @param group the group
	 * @param amount the amount
	 * @return the response
	 */
	@GET
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	@Path(ASSIGNENDPOINT)
	public Response assignLicense(@Context HttpHeaders headers,
			@Context HttpServletRequest req,
			@Context UriInfo info,
			@PathParam("assignment-group-uuid") String group,
			@DefaultValue("1") @QueryParam("amount") long amount) {
		
		String fromAppId = null;
		String transId = null;
		AAIException ex;

		try { 
			fromAppId = getFromAppId(headers);
			transId = getTransId(headers);
		} catch (AAIException e) { 
			ArrayList<String> templateVars = new ArrayList<String>();
			templateVars.add("PUT LicenseManagement");
			templateVars.add("assignLicense");
			return Response
					.status(e.getErrorObject().getHTTPResponseCode())
					.entity(ErrorLogHelper.getRESTAPIErrorResponse(headers.getAcceptableMediaTypes(), e, templateVars))
					.build();
		}
		Response response = null;
		HashMap<String, Object> lookupHash = new HashMap<>();
		String realTime = headers.getRequestHeaders().getFirst("Real-Time");

		lookupHash.put("assignment-group-uuid", group);
		lookupHash.put("assignment-status", "Available");
		TransactionalGraphEngine dbEngine = null;
		boolean success = true;
		try {
			String apiVersion = AAIApiVersion.get();
			Version version = Version.valueOf(apiVersion);
			DBConnectionType type = this.determineConnectionType(fromAppId, realTime);
			HttpEntry httpEntry = new HttpEntry(version, ModelType.MOXY, QueryStyle.TRAVERSAL, type);
			dbEngine = httpEntry.getDbEngine();
			Loader loader = httpEntry.getLoader();
			URI uri = UriBuilder.fromPath("/license-management/license-key-resources")
				.queryParam("assignment-group-uuid", group)
				.queryParam("assignment-status", "Available").build();
			
			MultivaluedMap<String, String> map = URITools.getQueryMap(uri);
			QueryParser query = dbEngine.getQueryBuilder().createQueryFromURI(uri, map);
			QueryParser localQuery = dbEngine.getQueryBuilder().createQueryFromURI(uri, map);
			query.getQueryBuilder().limit(amount);
			localQuery.getQueryBuilder().limit(amount);
			List<Vertex> list = localQuery.getQueryBuilder().toList();
			String objType = "";
	        if (!query.getContainerType().equals("")) {
	        	objType = query.getContainerType();
	        } else {
	        	objType = query.getResultType();
	        }
			Introspector wrappedEntity = loader.introspectorFromName(objType);
			DBRequest request = new DBRequest.Builder(HttpMethod.GET, uri, query, wrappedEntity, headers, info, transId).build();
			
			List<DBRequest> requests = new ArrayList<>();
			requests.add(request);
			Pair<Boolean, List<Pair<URI, Response>>> responsesTuple  = httpEntry.process(requests, fromAppId);
	        
			Response dbResponse = responsesTuple.getValue1().get(0).getValue1();
			Object entity = dbResponse.getEntity();
			
			if (responsesTuple.getValue0()) {
				this.updateKeys(amount, list);
			}
			if (dbResponse.getStatus() >= 400) {
				entity = "";
			}
			response = Response.ok().entity(entity).build();

			success = true;
		} catch (AAIException e) {

			ArrayList<String> templateVars = new ArrayList<String>(2);
			templateVars.add("GETlicenseKeys");
			templateVars.add((String) lookupHash
					.get("assignment-group-uuid"));
	        ErrorLogHelper.logException(e);
			response = Response
					.status(e.getErrorObject().getHTTPResponseCode())
					.entity(ErrorLogHelper.getRESTAPIErrorResponse(
							headers.getAcceptableMediaTypes(), e,
							templateVars)).build();
			success = false;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			ArrayList<String> templateVars = new ArrayList<String>(2);
			templateVars.add("GETlicenseKeys");
			templateVars.add((String) lookupHash
					.get("assignment-group-uuid"));
			ex = new AAIException("AAI_4000", e);
			ErrorLogHelper.logException(ex);
			response = Response
					.status(Status.INTERNAL_SERVER_ERROR)
					.entity(ErrorLogHelper.getRESTAPIErrorResponse(headers.getAcceptableMediaTypes(), ex, templateVars))
					.build();
			e.printStackTrace();
			success = false;
		} finally {
			if (dbEngine != null) {
				if (success) {
					dbEngine.commit();
				} else {
					dbEngine.rollback();
				}
			}
		}

		return response;
	}

	/**
	 * Return key.
	 *
	 * @param headers the headers
	 * @param req the req
	 * @param key the key
	 * @return the response
	 */
	@DELETE
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	@Path(RETURNENDPOINT)
	public Response returnKey(@Context HttpHeaders headers,
			@Context HttpServletRequest req,
			@Context UriInfo info,
			@PathParam("license-key") String key) {
		String fromAppId = null;
		String transId = null;

		try { 
			fromAppId = getFromAppId(headers);
			transId = getTransId(headers);
		} catch (AAIException e) { 
			ArrayList<String> templateVars = new ArrayList<String>();
			templateVars.add("DELETE LicenseManagement");
			templateVars.add("returnKey");
			ErrorLogHelper.logException(e);
			return Response
					.status(e.getErrorObject().getHTTPResponseCode())
					.entity(ErrorLogHelper.getRESTAPIErrorResponse(headers.getAcceptableMediaTypes(), e, templateVars))
					.build();
		}
		Response response = null;

		HashMap<String, Object> lookupHash = new HashMap<>();
		lookupHash.put("license-key", key);

		AAIExtensionMap aaiExtMap = new AAIExtensionMap();

		aaiExtMap.setFromAppId(fromAppId);
		aaiExtMap.setTransId(transId);
		aaiExtMap.setLookupHashMap(lookupHash);
		boolean success = true;
		TransactionalGraphEngine dbEngine = null;
		try {
			String realTime = headers.getRequestHeaders().getFirst("Real-Time");
			String apiVersion = AAIApiVersion.get();
			Version version = Version.valueOf(apiVersion);
			DBConnectionType type = this.determineConnectionType(fromAppId, realTime);
			HttpEntry httpEntry = new HttpEntry(version, ModelType.MOXY, QueryStyle.TRAVERSAL, type);
			dbEngine = httpEntry.getDbEngine();
			Loader loader = httpEntry.getLoader();
			URI uri = UriBuilder.fromPath("/license-management/license-key-resources/license-key-resource/" + key).build();
			
			QueryParser query = dbEngine.getQueryBuilder().createQueryFromURI(uri);
			QueryParser localQuery = dbEngine.getQueryBuilder().createQueryFromURI(uri);
			String objType = "";
	        if (!query.getContainerType().equals("")) {
	        	objType = query.getContainerType();
	        } else {
	        	objType = query.getResultType();
	        }
			Introspector wrappedEntity = loader.introspectorFromName(objType);
			DBRequest request = new DBRequest.Builder(HttpMethod.GET, uri, query, wrappedEntity, headers, info, transId).build();
			List<DBRequest> requests = new ArrayList<>();
			requests.add(request);
			Pair<Boolean, List<Pair<URI, Response>>> responsesTuple  = httpEntry.process(requests, fromAppId);
	        			
			if (responsesTuple.getValue0()) {
				List<Vertex> list = localQuery.getQueryBuilder().toList();
				if (list.size() > 0) {
					Vertex v = list.get(0);
					v.property("assignment-date", "");
					v.property("assignment-status", "Available");

					response = Response.ok()
							.type(getMediaType(headers.getAcceptableMediaTypes()))
							.build();
					
				}
			}
		
			success = true;
		} catch (AAIException e) {
			ArrayList<String> templateVars = new ArrayList<String>(2);
			templateVars.add("DELETE licenseKey");
			templateVars.add(key); 
			ErrorLogHelper.logException(e);
			response = Response
					.status(e.getErrorObject().getHTTPResponseCode())
					.entity(ErrorLogHelper.getRESTAPIErrorResponse(
							headers.getAcceptableMediaTypes(), e, templateVars)).build();
			success = false;
		} catch (Exception e) {

			AAIException ex = new AAIException("AAI_5105", e);
			ArrayList<String> templateVars = new ArrayList<String>(2);
			templateVars.add("DELETE licenseKey");
			templateVars.add(key);
			ErrorLogHelper.logException(ex);
			response = Response
					.status(ex.getErrorObject().getHTTPResponseCode())
					.entity(ErrorLogHelper.getRESTAPIErrorResponse(
							headers.getAcceptableMediaTypes(), ex, templateVars)).build();
			success = false;
		} finally {
			if (dbEngine != null) {
				if (success) {
					dbEngine.commit();
				} else {
					dbEngine.rollback();
				}
			}
		}

		return response;
	}

	/**
	 * Update keys.
	 *
	 * @param g the g
	 * @param fromAppId the from app id
	 * @param transId the trans id
	 * @param logline the logline
	 * @param keys the keys
	 * @param headers the headers
	 * @param req the req
	 * @throws AAIException the AAI exception
	 */
	private void updateKeys(long amount, List<Vertex> vertices) throws AAIException {

		FormatDate fd = new FormatDate("yyyy-MM-dd HH:mm:ss.SSS");
        String dateString = fd.getDateTime();
		long limit = 0;
		for (Vertex v : vertices) {
			if (limit == amount) {
				break;
			}
			String assignmentType = v.<String>property("assignment-type").orElse(null);
			if ("Unique".equalsIgnoreCase(assignmentType)) {
				v.property("assignment-date", dateString);
				v.property("assignment-status", "Assigned");
			} else if ("Universal".equalsIgnoreCase(assignmentType)) {
				// key.setAssignmentCapacity(key.getAssignmentCapacity() - 1);
				// if (key.getAssignmentCapacity() <= 0) {
				//key.setAssignmentStatus("Assigned");
				// }
			}
			limit++;
		}

	}
}

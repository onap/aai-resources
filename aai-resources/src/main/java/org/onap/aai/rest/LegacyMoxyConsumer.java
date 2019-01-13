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

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.Encoded;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import io.swagger.jaxrs.PATCH;
import org.javatuples.Pair;
import org.json.JSONArray;
import org.json.JSONObject;
import org.onap.aai.concurrent.AaiCallable;
import org.onap.aai.config.SpringContextAware;
import org.onap.aai.dbmap.DBConnectionType;
import org.onap.aai.exceptions.AAIException;
import org.onap.aai.introspection.Introspector;
import org.onap.aai.introspection.Loader;
import org.onap.aai.setup.SchemaVersion;
import org.onap.aai.parsers.query.QueryParser;
import org.onap.aai.rest.db.DBRequest;
import org.onap.aai.rest.db.HttpEntry;
import org.onap.aai.rest.exceptions.AAIInvalidXMLNamespace;
import org.onap.aai.rest.util.ValidateEncoding;
import org.onap.aai.restcore.HttpMethod;
import org.onap.aai.restcore.RESTAPI;
import org.onap.aai.serialization.engines.TransactionalGraphEngine;
import org.onap.aai.util.AAIConfig;
import org.onap.aai.util.AAIConstants;
import org.springframework.stereotype.Controller;

import com.att.eelf.configuration.EELFLogger;
import com.att.eelf.configuration.EELFManager;
import com.google.common.base.Joiner;

/**
 * The Class LegacyMoxyConsumer.
 */
@Controller
@Path("{version: v[1-9][0-9]*|latest}")
public class LegacyMoxyConsumer extends RESTAPI {

	private static final EELFLogger logger = EELFManager.getInstance().getLogger(LegacyMoxyConsumer.class.getName());

//	private HttpEntry traversalUriHttpEntry;

//	@Autowired
//	public LegacyMoxyConsumer(HttpEntry traversalUriHttpEntry){
//		this.traversalUriHttpEntry = traversalUriHttpEntry;
//	}

	public LegacyMoxyConsumer(){

	}

	/**
	 *
	 * @param content
	 * @param versionParam
	 * @param uri
	 * @param headers
	 * @param info
	 * @param req
	 * @return
	 */
	@PUT
	@Path("/{uri: .+}")
	@Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	public Response update (String content, @PathParam("version")String versionParam, @PathParam("uri") @Encoded String uri, @Context HttpHeaders headers, @Context UriInfo info, @Context HttpServletRequest req) {
		MediaType mediaType = headers.getMediaType();
		return this.handleWrites(mediaType, HttpMethod.PUT, content, versionParam, uri, headers, info);
	}

	/**
	 * Update relationship.
	 *
	 * @param content the content
	 * @param versionParam the version param
	 * @param uri the uri
	 * @param headers the headers
	 * @param info the info
	 * @param req the req
	 * @return the response
	 */
	@PUT
	@Path("/{uri: .+}/relationship-list/relationship")
	@Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	public Response updateRelationship (String content, @PathParam("version")String versionParam, @PathParam("uri") @Encoded String uri, @Context HttpHeaders headers, @Context UriInfo info, @Context HttpServletRequest req) {

		String sourceOfTruth = headers.getRequestHeaders().getFirst("X-FromAppId");
		String transId = headers.getRequestHeaders().getFirst("X-TransactionId");
		String realTime = headers.getRequestHeaders().getFirst("Real-Time");
		MediaType inputMediaType = headers.getMediaType();
		Response response = null;
		Loader loader = null;
		TransactionalGraphEngine dbEngine = null;
		boolean success = true;

		try {
			validateRequest(info);
			SchemaVersion version = new SchemaVersion(versionParam);
			DBConnectionType type = null;
			if(AAIConfig.get("aai.use.realtime", "true").equals("true")){
				type = DBConnectionType.REALTIME;
			} else {
				type = this.determineConnectionType(sourceOfTruth, realTime);
			}
			HttpEntry traversalUriHttpEntry = SpringContextAware.getBean("traversalUriHttpEntry", HttpEntry.class);
			traversalUriHttpEntry.setHttpEntryProperties(version, type);
			loader = traversalUriHttpEntry.getLoader();
			dbEngine = traversalUriHttpEntry.getDbEngine();

			URI uriObject = UriBuilder.fromPath(uri).build();
			this.validateURI(uriObject);

			QueryParser uriQuery = dbEngine.getQueryBuilder().createQueryFromURI(uriObject);

			Introspector wrappedEntity = loader.unmarshal("relationship", content, org.onap.aai.restcore.MediaType.getEnum(this.getInputMediaType(inputMediaType)));

			DBRequest request = new DBRequest.Builder(HttpMethod.PUT_EDGE, uriObject, uriQuery, wrappedEntity, headers, info, transId).build();
			List<DBRequest> requests = new ArrayList<>();
			requests.add(request);
			Pair<Boolean, List<Pair<URI, Response>>> responsesTuple  = traversalUriHttpEntry.process(requests, sourceOfTruth);

			response = responsesTuple.getValue1().get(0).getValue1();
			success = responsesTuple.getValue0();

		} catch (AAIException e) {
			response = consumerExceptionResponseGenerator(headers, info, HttpMethod.PUT, e);
			success = false;
		} catch (Exception e) {
			AAIException aaiException = new AAIException("AAI_4000", e);
			response = consumerExceptionResponseGenerator(headers, info, HttpMethod.PUT, aaiException);
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
	 * Patch.
	 *
	 * @param content the content
	 * @param versionParam the version param
	 * @param uri the uri
	 * @param headers the headers
	 * @param info the info
	 * @param req the req
	 * @return the response
	 */
	@PATCH
	@Path("/{uri: .+}")
	@Consumes({ "application/merge-patch+json" })
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	public Response patch (String content, @PathParam("version")String versionParam, @PathParam("uri") @Encoded String uri, @Context HttpHeaders headers, @Context UriInfo info, @Context HttpServletRequest req) {

		MediaType mediaType = MediaType.APPLICATION_JSON_TYPE;
		return this.handleWrites(mediaType, HttpMethod.MERGE_PATCH, content, versionParam, uri, headers, info);

	}

	/**
	 * Gets the legacy.
	 *
	 * @param content the content
	 * @param versionParam the version param
	 * @param uri the uri
	 * @param depthParam the depth param
	 * @param cleanUp the clean up
	 * @param headers the headers
	 * @param info the info
	 * @param req the req
	 * @return the legacy
	 */
	@GET
	@Path("/{uri: .+}")
	@Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	public Response getLegacy (String content, @DefaultValue("-1") @QueryParam("resultIndex") String resultIndex, @DefaultValue("-1") @QueryParam("resultSize") String resultSize, @PathParam("version")String versionParam, @PathParam("uri") @Encoded String uri, @DefaultValue("all") @QueryParam("depth") String depthParam, @DefaultValue("false") @QueryParam("cleanup") String cleanUp, @Context HttpHeaders headers, @Context UriInfo info, @Context HttpServletRequest req) {
		return runner(AAIConstants.AAI_CRUD_TIMEOUT_ENABLED,
				AAIConstants.AAI_CRUD_TIMEOUT_APP,
				AAIConstants.AAI_CRUD_TIMEOUT_LIMIT,
				headers,
				info,
				HttpMethod.GET,
				new AaiCallable<Response>() {
					@Override
					public Response process() {
						return getLegacy(content, versionParam, uri, depthParam, cleanUp, headers, info, req, new HashSet<String>(), resultIndex, resultSize);
					}
				}
		);
	}

	/**
	 * This method exists as a workaround for filtering out undesired query params while routing between REST consumers
	 *
	 * @param content
	 * @param versionParam
	 * @param uri
	 * @param depthParam
	 * @param cleanUp
	 * @param headers
	 * @param info
	 * @param req
	 * @param removeQueryParams
	 * @return
	 */
	public Response getLegacy(String content, String versionParam, String uri, String depthParam, String cleanUp,  HttpHeaders headers, UriInfo info, HttpServletRequest req, Set<String> removeQueryParams, String resultIndex, String resultSize) {
		String sourceOfTruth = headers.getRequestHeaders().getFirst("X-FromAppId");
		String transId = headers.getRequestHeaders().getFirst("X-TransactionId");
		String realTime = headers.getRequestHeaders().getFirst("Real-Time");
		Response response = null;
		TransactionalGraphEngine dbEngine = null;
		Loader loader = null;

		try {
			validateRequest(info);
			SchemaVersion version = new SchemaVersion(versionParam);
			DBConnectionType type = null;
			if(AAIConfig.get("aai.use.realtime", "true").equals("true")){
				type = DBConnectionType.REALTIME;
			} else {
				type = this.determineConnectionType(sourceOfTruth, realTime);
			}
			final HttpEntry traversalUriHttpEntry = SpringContextAware.getBean("traversalUriHttpEntry", HttpEntry.class);
			traversalUriHttpEntry.setHttpEntryProperties(version, type);
			dbEngine = traversalUriHttpEntry.getDbEngine();
			loader = traversalUriHttpEntry.getLoader();
			MultivaluedMap<String, String> params = info.getQueryParameters();

			params = removeNonFilterableParams(params);

			uri = uri.split("\\?")[0];

			URI uriObject = UriBuilder.fromPath(uri).build();

			QueryParser uriQuery = dbEngine.getQueryBuilder().createQueryFromURI(uriObject, params);

			String objType = "";
			if (!uriQuery.getContainerType().equals("")) {
				objType = uriQuery.getContainerType();
			} else {
				objType = uriQuery.getResultType();
			}
			Introspector obj = loader.introspectorFromName(objType);
			DBRequest request =
					new DBRequest.Builder(HttpMethod.GET, uriObject, uriQuery, obj, headers, info, transId).build();
			List<DBRequest> requests = new ArrayList<>();
			requests.add(request);
			if (resultIndex != null && resultIndex != "-1" && resultSize != null && resultSize != "-1") {
				traversalUriHttpEntry.setPaginationIndex(Integer.parseInt(resultIndex));
				traversalUriHttpEntry.setPaginationBucket(Integer.parseInt(resultSize));
			}
			Pair<Boolean, List<Pair<URI, Response>>> responsesTuple = traversalUriHttpEntry.process(requests, sourceOfTruth);

			response = responsesTuple.getValue1().get(0).getValue1();

		} catch (AAIException e) {
			response = consumerExceptionResponseGenerator(headers, info, HttpMethod.GET, e);
		} catch (Exception e ) {
			AAIException ex = new AAIException("AAI_4000", e);

			response = consumerExceptionResponseGenerator(headers, info, HttpMethod.GET, ex);
		} finally {
			if (dbEngine != null) {
				if (cleanUp.equals("true")) {
					dbEngine.commit();
				} else {
					dbEngine.rollback();
				}
			}
		}

		return response;
	}

	private MultivaluedMap<String, String> removeNonFilterableParams(MultivaluedMap<String, String> params) {

		String[] toRemove = { "depth", "cleanup", "nodes-only", "format", "resultIndex", "resultSize"};
		Set<String> toRemoveSet = Arrays.stream(toRemove).collect(Collectors.toSet());

		MultivaluedMap<String, String> cleanedParams = new MultivaluedHashMap<>();
		params.keySet().stream().forEach(k -> {
			if (!toRemoveSet.contains(k)) {
				cleanedParams.addAll(k, params.get(k));
			}
		});

		return cleanedParams;
	}
	/**
	 * Delete.
	 *
	 * @param versionParam the version param
	 * @param uri the uri
	 * @param headers the headers
	 * @param info the info
	 * @param resourceVersion the resource version
	 * @param req the req
	 * @return the response
	 */
	@DELETE
	@Path("/{uri: .+}")
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	public Response delete (@PathParam("version")String versionParam, @PathParam("uri") @Encoded String uri, @Context HttpHeaders headers, @Context UriInfo info, @QueryParam("resource-version")String resourceVersion, @Context HttpServletRequest req) {


		String outputMediaType = getMediaType(headers.getAcceptableMediaTypes());
		String sourceOfTruth = headers.getRequestHeaders().getFirst("X-FromAppId");
		String transId = headers.getRequestHeaders().getFirst("X-TransactionId");
		String realTime = headers.getRequestHeaders().getFirst("Real-Time");

		TransactionalGraphEngine dbEngine = null;
		Response response = Response.status(404)
				.type(outputMediaType).build();

		boolean success = true;

		try {

			validateRequest(info);
			SchemaVersion version = new SchemaVersion(versionParam);
			DBConnectionType type = null;
			if(AAIConfig.get("aai.use.realtime", "true").equals("true")){
				type = DBConnectionType.REALTIME;
			} else {
				type = this.determineConnectionType(sourceOfTruth, realTime);
			}
			HttpEntry traversalUriHttpEntry = SpringContextAware.getBean("traversalUriHttpEntry", HttpEntry.class);
			traversalUriHttpEntry.setHttpEntryProperties(version, type);
			dbEngine = traversalUriHttpEntry.getDbEngine();
			Loader loader = traversalUriHttpEntry.getLoader();

			URI uriObject = UriBuilder.fromPath(uri).build();

			QueryParser uriQuery = dbEngine.getQueryBuilder().createQueryFromURI(uriObject);
			String objType = uriQuery.getResultType();
			Introspector obj = loader.introspectorFromName(objType);

			DBRequest request = new DBRequest.Builder(HttpMethod.DELETE, uriObject, uriQuery, obj, headers, info, transId).build();
			List<DBRequest> requests = new ArrayList<>();
			requests.add(request);
			Pair<Boolean, List<Pair<URI, Response>>> responsesTuple  = traversalUriHttpEntry.process(requests, sourceOfTruth);

			response = responsesTuple.getValue1().get(0).getValue1();
			success = responsesTuple.getValue0();

		} catch (AAIException e) {
			response = consumerExceptionResponseGenerator(headers, info, HttpMethod.DELETE, e);
			success = false;
		} catch (Exception e) {
			AAIException ex = new AAIException("AAI_4000", e);
			response = consumerExceptionResponseGenerator(headers, info, HttpMethod.DELETE, ex);
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
	 * This whole method does nothing because the body is being dropped while fielding the request.
	 *
	 * @param content the content
	 * @param versionParam the version param
	 * @param uri the uri
	 * @param headers the headers
	 * @param info the info
	 * @param req the req
	 * @return the response
	 */
	@DELETE
	@Path("/{uri: .+}/relationship-list/relationship")
	@Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	public Response deleteRelationship (String content, @PathParam("version")String versionParam, @PathParam("uri") @Encoded String uri, @Context HttpHeaders headers, @Context UriInfo info, @Context HttpServletRequest req) {

		MediaType inputMediaType = headers.getMediaType();

		String outputMediaType = getMediaType(headers.getAcceptableMediaTypes());
		String sourceOfTruth = headers.getRequestHeaders().getFirst("X-FromAppId");
		String transId = headers.getRequestHeaders().getFirst("X-TransactionId");
		String realTime = headers.getRequestHeaders().getFirst("Real-Time");

		Loader loader = null;
		TransactionalGraphEngine dbEngine = null;
		Response response = Response.status(404)
				.type(outputMediaType).build();

		boolean success = true;

		try {
			this.validateRequest(info);
			SchemaVersion version = new SchemaVersion(versionParam);
			DBConnectionType type = null;
			if(AAIConfig.get("aai.use.realtime", "true").equals("true")){
				type = DBConnectionType.REALTIME;
			} else {
				type = this.determineConnectionType(sourceOfTruth, realTime);
			}
			HttpEntry traversalUriHttpEntry = SpringContextAware.getBean("traversalUriHttpEntry", HttpEntry.class);
			traversalUriHttpEntry.setHttpEntryProperties(version, type);
			loader = traversalUriHttpEntry.getLoader();
			dbEngine = traversalUriHttpEntry.getDbEngine();

			if (content.equals("")) {
				throw new AAIException("AAI_3102", "You must supply a relationship");
			}
			URI uriObject = UriBuilder.fromPath(uri).build();
			this.validateURI(uriObject);

			QueryParser uriQuery = dbEngine.getQueryBuilder().createQueryFromURI(uriObject);

			Introspector wrappedEntity = loader.unmarshal("relationship", content, org.onap.aai.restcore.MediaType.getEnum(this.getInputMediaType(inputMediaType)));

			DBRequest request = new DBRequest.Builder(HttpMethod.DELETE_EDGE, uriObject, uriQuery, wrappedEntity, headers, info, transId).build();
			List<DBRequest> requests = new ArrayList<>();
			requests.add(request);
			Pair<Boolean, List<Pair<URI, Response>>> responsesTuple  = traversalUriHttpEntry.process(requests, sourceOfTruth);

			response = responsesTuple.getValue1().get(0).getValue1();
			success = responsesTuple.getValue0();
		} catch (AAIException e) {
			response = consumerExceptionResponseGenerator(headers, info, HttpMethod.DELETE, e);
			success = false;
		} catch (Exception e) {
			AAIException ex = new AAIException("AAI_4000", e);
			response = consumerExceptionResponseGenerator(headers, info, HttpMethod.DELETE, ex);
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

	@GET
	@Path("/{uri: .+}/relationship-list")
	@Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	public Response getRelationshipList (@DefaultValue("-1") @QueryParam("resultIndex") String resultIndex, @DefaultValue("-1") @QueryParam("resultSize") String resultSize, @PathParam("version")String versionParam, @PathParam("uri") @Encoded String uri, @DefaultValue("false") @QueryParam("cleanup") String cleanUp, @Context HttpHeaders headers, @Context UriInfo info) {
		return runner(AAIConstants.AAI_CRUD_TIMEOUT_ENABLED,
				AAIConstants.AAI_CRUD_TIMEOUT_APP,
				AAIConstants.AAI_CRUD_TIMEOUT_LIMIT,
				headers,
				info,
				HttpMethod.GET,
				new AaiCallable<Response>() {
					@Override
					public Response process() {
						return getRelationshipList(versionParam, uri, cleanUp, headers, info, resultIndex, resultSize);
					}
				}
		);
	}

	/**
	 *
	 * @param versionParam
	 * @param uri
	 * @param cleanUp
	 * @param headers
	 * @param info
	 * @return
	 */
	public Response getRelationshipList(String versionParam, String uri, String cleanUp, HttpHeaders headers, UriInfo info, String resultIndex, String resultSize) {
		String sourceOfTruth = headers.getRequestHeaders().getFirst("X-FromAppId");
		String transId = headers.getRequestHeaders().getFirst("X-TransactionId");
		String realTime = headers.getRequestHeaders().getFirst("Real-Time");
		Response response = null;
		TransactionalGraphEngine dbEngine = null;
		Loader loader = null;

		try {
			validateRequest(info);
			SchemaVersion version = new SchemaVersion(versionParam);
			DBConnectionType type = null;
			if(AAIConfig.get("aai.use.realtime", "true").equals("true")){
				type = DBConnectionType.REALTIME;
			} else {
				type = this.determineConnectionType(sourceOfTruth, realTime);
			}
			final HttpEntry traversalUriHttpEntry = SpringContextAware.getBean("traversalUriHttpEntry", HttpEntry.class);
			traversalUriHttpEntry.setHttpEntryProperties(version, type);
			dbEngine = traversalUriHttpEntry.getDbEngine();
			loader = traversalUriHttpEntry.getLoader();
			MultivaluedMap<String, String> params = info.getQueryParameters();

			params = removeNonFilterableParams(params);

			uri = uri.split("\\?")[0];

			URI uriObject = UriBuilder.fromPath(uri).build();

			QueryParser uriQuery = dbEngine.getQueryBuilder().createQueryFromURI(uriObject, params);

			String objType = "";
			if (!uriQuery.getContainerType().equals("")) {
				objType = uriQuery.getContainerType();
			} else {
				objType = uriQuery.getResultType();
			}
			Introspector obj = loader.introspectorFromName(objType);
			DBRequest request =
					new DBRequest.Builder(HttpMethod.GET_RELATIONSHIP, uriObject, uriQuery, obj, headers, info, transId).build();
			List<DBRequest> requests = new ArrayList<>();
			requests.add(request);
			if (resultIndex != null && resultIndex != "-1" && resultSize != null && resultSize != "-1") {
				traversalUriHttpEntry.setPaginationIndex(Integer.parseInt(resultIndex));
				traversalUriHttpEntry.setPaginationBucket(Integer.parseInt(resultSize));
			}
			Pair<Boolean, List<Pair<URI, Response>>> responsesTuple = traversalUriHttpEntry.process(requests, sourceOfTruth);

			response = responsesTuple.getValue1().get(0).getValue1();
		} catch (AAIException e) {
			response = consumerExceptionResponseGenerator(headers, info, HttpMethod.GET_RELATIONSHIP, e);
		} catch (Exception e ) {
			AAIException ex = new AAIException("AAI_4000", e);

			response = consumerExceptionResponseGenerator(headers, info, HttpMethod.GET_RELATIONSHIP, ex);
		} finally {
			if (dbEngine != null) {
				if (cleanUp.equals("true")) {
					dbEngine.commit();
				} else {
					dbEngine.rollback();
				}
			}
		}
		return response;
	}

	/**
	 * Validate request.
	 *
	 * @param info the info
	 * @throws AAIException the AAI exception
	 * @throws UnsupportedEncodingException the unsupported encoding exception
	 */
	private void validateRequest(UriInfo info) throws AAIException, UnsupportedEncodingException {

		if (!ValidateEncoding.getInstance().validate(info)) {
			throw new AAIException("AAI_3008", "uri=" + getPath(info));
		}
	}

	/**
	 * Gets the path.
	 *
	 * @param info the info
	 * @return the path
	 */
	private String getPath(UriInfo info) {
		String path = info.getPath(false);
		MultivaluedMap<String, String> map = info.getQueryParameters(false);
		String params = "?";
		List<String> parmList = new ArrayList<>();
		for (String key : map.keySet()) {
			for (String value : map.get(key)) {
				parmList.add(key + "=" + value);
			}
		}
		String queryParams = Joiner.on("&").join(parmList);
		if (map.keySet().size() > 0) {
			path += params + queryParams;
		}

		return path;

	}

	/**
	 * Handle writes.
	 *
	 * @param mediaType the media type
	 * @param method the method
	 * @param content the content
	 * @param versionParam the version param
	 * @param uri the uri
	 * @param headers the headers
	 * @param info the info
	 * @return the response
	 */
	private Response handleWrites(MediaType mediaType, HttpMethod method, String content, String versionParam, String uri, HttpHeaders headers, UriInfo info) {

		Response response = null;
		TransactionalGraphEngine dbEngine = null;
		Loader loader = null;
		SchemaVersion version = null;
		String sourceOfTruth = headers.getRequestHeaders().getFirst("X-FromAppId");
		String transId = headers.getRequestHeaders().getFirst("X-TransactionId");
		String realTime = headers.getRequestHeaders().getFirst("Real-Time");
		Boolean success = true;

		try {
			validateRequest(info);

			version = new SchemaVersion(versionParam);
			DBConnectionType type = null;
			if(AAIConfig.get("aai.use.realtime", "true").equals("true")){
				type = DBConnectionType.REALTIME;
			} else {
				type = this.determineConnectionType(sourceOfTruth, realTime);
			}
			HttpEntry traversalUriHttpEntry = SpringContextAware.getBean("traversalUriHttpEntry", HttpEntry.class);
			traversalUriHttpEntry.setHttpEntryProperties(version, type);
			loader = traversalUriHttpEntry.getLoader();
			dbEngine = traversalUriHttpEntry.getDbEngine();
			URI uriObject = UriBuilder.fromPath(uri).build();
			this.validateURI(uriObject);
			QueryParser uriQuery = dbEngine.getQueryBuilder().createQueryFromURI(uriObject);
			String objName = uriQuery.getResultType();
			if (content.length() == 0) {
				if (mediaType.toString().contains(MediaType.APPLICATION_JSON)) {
					content = "{}";
				} else {
					content = "<empty/>";
				}
			}
			Introspector obj = loader.unmarshal(objName, content, org.onap.aai.restcore.MediaType.getEnum(this.getInputMediaType(mediaType)));
			if (obj == null) {
				throw new AAIException("AAI_3000", "object could not be unmarshalled:" + content);
			}

			if (mediaType.toString().contains(MediaType.APPLICATION_XML) && !content.equals("<empty/>") && isEmptyObject(obj)) {
				throw new AAIInvalidXMLNamespace(content);
			}

			this.validateIntrospector(obj, loader, uriObject, method);

			DBRequest request =
					new DBRequest.Builder(method, uriObject, uriQuery, obj, headers, info, transId)
							.rawRequestContent(content).build();
			List<DBRequest> requests = new ArrayList<>();
			requests.add(request);
			Pair<Boolean, List<Pair<URI, Response>>> responsesTuple  = traversalUriHttpEntry.process(requests,  sourceOfTruth);

			response = responsesTuple.getValue1().get(0).getValue1();
			success = responsesTuple.getValue0();
		} catch (AAIException e) {
			response = consumerExceptionResponseGenerator(headers, info, method, e);
			success = false;
		} catch (Exception e ) {
			AAIException ex = new AAIException("AAI_4000", e);
			response = consumerExceptionResponseGenerator(headers, info, method, ex);
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

	private void validateURI(URI uri) throws AAIException {
		if (hasRelatedTo(uri)) {
			throw new AAIException("AAI_3010");
		}
	}
	private boolean hasRelatedTo(URI uri) {

		return uri.toString().contains("/" + RestTokens.COUSIN + "/");
	}

	protected boolean isEmptyObject(Introspector obj) {
		return "{}".equals(obj.marshal(false));
	}
}

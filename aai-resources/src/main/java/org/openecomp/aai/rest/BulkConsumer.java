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

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.PUT;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.javatuples.Pair;
import org.javatuples.Triplet;

import org.openecomp.aai.dbmap.DBConnectionType;
import org.openecomp.aai.exceptions.AAIException;
import org.openecomp.aai.introspection.Introspector;
import org.openecomp.aai.introspection.Loader;
import org.openecomp.aai.introspection.ModelType;
import org.openecomp.aai.introspection.Version;
import org.openecomp.aai.introspection.exceptions.AAIUnmarshallingException;
import org.openecomp.aai.logging.ErrorObjectNotFoundException;
import org.openecomp.aai.parsers.query.QueryParser;
import org.openecomp.aai.rest.db.DBRequest;
import org.openecomp.aai.rest.db.HttpEntry;
import org.openecomp.aai.rest.util.ValidateEncoding;
import org.openecomp.aai.restcore.HttpMethod;
import org.openecomp.aai.restcore.RESTAPI;
import org.openecomp.aai.serialization.engines.QueryStyle;
import org.openecomp.aai.serialization.engines.TransactionalGraphEngine;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

/**
 * The Class BulkAddConsumer.
 */
/*
 * The purpose of this endpoint is to allow a client to add
 * multiple objects with one request. It may take
 * one or more transaction objects containing one or more
 * objects to add.
 * The transactions are independent of each other - 
 * if one fails, its effects are rolled back, but the others' aren't.
 * Within a single transaction, if adding one object fails, all the others'
 * changes are rolled back.
 */
public abstract class BulkConsumer extends RESTAPI {

	/** The introspector factory type. */
	private ModelType introspectorFactoryType = ModelType.MOXY;
	
	/** The query style. */
	private QueryStyle queryStyle = QueryStyle.TRAVERSAL;
	
	/**
	 * Bulk add.
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
	@Consumes({ MediaType.APPLICATION_JSON})
	@Produces({ MediaType.APPLICATION_JSON})
	public Response bulkAdd(String content, @PathParam("version")String versionParam, @Context HttpHeaders headers, @Context UriInfo info, @Context HttpServletRequest req){
		
		String transId = headers.getRequestHeaders().getFirst("X-TransactionId");
		String sourceOfTruth = headers.getRequestHeaders().getFirst("X-FromAppId");
		String realTime = headers.getRequestHeaders().getFirst("Real-Time");
		String outputMediaType = getMediaType(headers.getAcceptableMediaTypes());
		Version version = Version.valueOf(versionParam);
		DBConnectionType type = this.determineConnectionType(sourceOfTruth, realTime);

		Response response = null;
		
		/* A Response will be generated for each object in each transaction.
		 * To keep track of what came from where to give organized feedback to the client,
		 * we keep responses from a given transaction together in one list (hence all being a list of lists)
		 * and pair each response with its matching URI (which will be null if there wasn't one).
		 */ 
 		List<List<Pair<URI, Response>>> allResponses = new ArrayList<List<Pair<URI, Response>>>();
		
		try {
			//TODO add auth check when this endpoint added to that auth properties files
			
			
			JsonArray transactions = getTransactions(content);
			
			for (int i = 0; i < transactions.size(); i++){
				HttpEntry httpEntry = new HttpEntry(version, introspectorFactoryType, queryStyle, type);
				Loader loader = httpEntry.getLoader();
				TransactionalGraphEngine dbEngine = httpEntry.getDbEngine();
				URI thisUri = null;
				List<Triplet<URI, Introspector,HttpMethod>> triplet = new ArrayList<Triplet<URI, Introspector,HttpMethod>>();
				HttpMethod method = null;
				try {
					JsonElement transObj = transactions.get(i);
					if (!(transObj instanceof JsonObject)) {
						throw new AAIException("AAI_6111", "input payload does not follow bulk add interface");
					}
					//JsonObject transaction = transObj.getAsJsonObject();
					
					fillObjectTuplesFromTransaction(triplet, transObj.getAsJsonObject(), loader, dbEngine, outputMediaType);
					if (triplet.size() == 0) {
						//case where user sends a validly formatted transactions object but
						//which has no actual things in it for A&AI to do anything with
						//assuming we should count this as a user error
						throw new AAIException("AAI_6118", "payload had no objects to operate on");
					}
				
					List<DBRequest> requests = new ArrayList<>();
					for (Triplet<URI, Introspector, HttpMethod> tuple : triplet){
						thisUri = tuple.getValue0(); 
						method = tuple.getValue2();
						QueryParser uriQuery = dbEngine.getQueryBuilder().createQueryFromURI(thisUri);
						DBRequest request = new DBRequest.Builder(method, thisUri, uriQuery, tuple.getValue1(), headers, info, transId).build();
						requests.add(request);
					}
					
					Pair<Boolean, List<Pair<URI, Response>>> results = httpEntry.process(requests, sourceOfTruth, this.enableResourceVersion());
					List<Pair<URI,    Response>> responses = results.getValue1();
					allResponses.add(responses);
					if (results.getValue0()) { //everything was processed without error
						dbEngine.commit();
					} else { //something failed
						dbEngine.rollback();
					}
				} catch (Exception e) {
					/* While httpEntry.process handles its exceptions, exceptions thrown in earlier helpers
					 * bubbles up to here. As we want to tie error messages to the URI of the object that caused
					 * them, we catch here, generate a Response, bundle it with that URI, and move on.
					 */
					method = HttpMethod.PUT;
					if (triplet.size() != 0) { //failed somewhere in the middle of tuple-filling
						Triplet<URI, Introspector, HttpMethod> lastTuple = triplet.get(triplet.size()-1); //last one in there was the problem
						if (lastTuple.getValue1() == null){
							//failed out before thisUri could be set but after tuples started being filled
							thisUri = lastTuple.getValue0();
							method = lastTuple.getValue2();
						}
					} //else failed out on empty payload so tuples never filled (or failed out even earlier than tuple-filling)
					addExceptionCaseFailureResponse(allResponses, e, i, thisUri, headers, info, method);
					dbEngine.rollback();
					continue; /* if an exception gets thrown within a transaction we want to keep going to 
					   			the next transaction, not break out of the whole request */
				}
			}
			
			String returnPayload = generateResponsePayload(allResponses);
			
			//unless a top level error gets thrown, we want to 201 bc the client wanted a "fire and forget" kind of setup
			response = Response
					.status(Status.CREATED)
					.entity(returnPayload)
					.build();
		} catch (AAIException e) { //these catches needed for handling top level errors in payload parsing where the whole request must fail out
			response = consumerExceptionResponseGenerator(headers, info, HttpMethod.PUT, e);
		} catch(JsonSyntaxException e) {
			AAIException ex = new AAIException("AAI_6111");
			response = consumerExceptionResponseGenerator(headers, info, HttpMethod.PUT, ex);
		} catch (Exception e ) {
			AAIException ex = new AAIException("AAI_4000", e);
			response = consumerExceptionResponseGenerator(headers, info, HttpMethod.PUT, ex);
		} 
		
		return response;
	}
	
	
	/**
	 * Gets the transactions.
	 *
	 * @param content - input JSON payload string
	 * @return JsonArray - the array of transactions
	 * @throws AAIException the AAI exception
	 * @throws JsonSyntaxException Parses and breaks the single payload into an array of individual transaction 
	 * bodies to be processed.
	 */
	private JsonArray getTransactions(String content) throws AAIException, JsonSyntaxException {
		JsonParser parser = new JsonParser();
		
		JsonObject input = parser.parse(content).getAsJsonObject();
		
		if (!(input.has("transactions"))) {
			throw new AAIException("AAI_6118", "input payload does not follow bulk add interface - missing \"transactions\"");
		}
		JsonElement transactionsObj = input.get("transactions");
		
		if (!(transactionsObj.isJsonArray())){
			throw new AAIException("AAI_6111", "input payload does not follow bulk add interface");
		}
		JsonArray transactions = transactionsObj.getAsJsonArray();
		if (transactions.size() == 0) {
			//case where user sends a validly formatted transactions object but
			//which has no actual things in it for A&AI to do anything with
			//assuming we should count this as a user error
			throw new AAIException("AAI_6118", "payload had no objects to operate on");
		}
		return transactions;
	}
	
	/**
	 * Fill object tuples from transaction.
	 *
	 * @param tuples the tuples
	 * @param transaction - JSON body containing the objects to be added
	 * 							each object must have a URI and an object body
	 * @param loader the loader
	 * @param dbEngine the db engine
	 * @param inputMediaType the input media type
	 * @return list of tuples containing each introspector-wrapped object and its given URI
	 * @throws AAIException the AAI exception
	 * @throws JsonSyntaxException the json syntax exception
	 * @throws UnsupportedEncodingException Walks through the given transaction and unmarshals each object in it, then bundles each
	 * with its URI.
	 */
	private void fillObjectTuplesFromTransaction(List<Triplet<URI, Introspector, HttpMethod>> triplet,
			JsonObject transaction, Loader loader, TransactionalGraphEngine dbEngine, String inputMediaType)
			throws AAIException, JsonSyntaxException, UnsupportedEncodingException {

	
			if (transaction.has("put") && this.functionAllowed(HttpMethod.PUT)) {
				pairUp(triplet, transaction, loader, dbEngine, inputMediaType, HttpMethod.PUT);
			}
			else if (transaction.has("delete") && this.functionAllowed(HttpMethod.DELETE)) {
				pairUp(triplet, transaction, loader, dbEngine, inputMediaType, HttpMethod.DELETE);
			}
			else if (transaction.has("patch") && this.functionAllowed(HttpMethod.MERGE_PATCH)) {
				pairUp(triplet, transaction, loader, dbEngine, inputMediaType, HttpMethod.MERGE_PATCH);
			}
							
			else{
				
				throw new AAIException("AAI_6118", "input payload does not follow bulk add interface - missing put delete or patch");                                                    
			}

			

	}

	
	
	private void pairUp(List<Triplet<URI, Introspector, HttpMethod>> triplet, JsonObject item, Loader loader, TransactionalGraphEngine dbEngine, String inputMediaType, HttpMethod method) throws AAIException, JsonSyntaxException, UnsupportedEncodingException{
	
		
		for (int i=0; i<item.size(); i++) {
			Triplet<URI, Introspector, HttpMethod> tuple =  Triplet.with(null, null,null);
			
			tuple = tuple.setAt2(method);
			try {
				if (!(item.isJsonObject())) {
					throw new AAIException("AAI_6111", "input payload does not follow bulk add interface");
				}
				
				
				JsonElement actionElement = null;
				
				if(item.has("put")){
					actionElement = item.get("put");
				} else if(item.has("patch")){
					actionElement = item.get("patch");
				} else if(item.has("delete")){
					actionElement = item.get("delete");
				}
				
				if ((actionElement == null) || !actionElement.isJsonArray()) {
					throw new AAIException("AAI_6111", "input payload does not follow bulk add interface");
				}
				JsonArray httpArray = actionElement.getAsJsonArray();
				for(int j = 0; j < httpArray.size(); ++j){
					JsonObject it = httpArray.get(j).getAsJsonObject();
					JsonElement itemURIfield = it.get("uri");
					if (itemURIfield == null) {
						throw new AAIException("AAI_6118", "must include object uri");
					}
					String uriStr = itemURIfield.getAsString();
					if (uriStr.endsWith("/relationship-list/relationship")) {
						if (method.equals(HttpMethod.PUT)) {
							tuple = tuple.setAt2(HttpMethod.PUT_EDGE);
						} else if (method.equals(HttpMethod.DELETE)) {
							tuple = tuple.setAt2(HttpMethod.DELETE_EDGE);
						}
					} else {
						tuple = tuple.setAt2(method);
					}
									
					URI uri = UriBuilder.fromPath(uriStr).build();
					
					/* adding the uri as soon as we have one (valid or not) lets us
					 * keep any errors with their corresponding uris for client feedback  
					 */
					tuple = tuple.setAt0(uri);
					
					if (!ValidateEncoding.getInstance().validate(uri)) {
						throw new AAIException("AAI_3008", "uri=" + uri.getPath());
					}
					
					if (!(it.has("body"))){
						throw new AAIException("AAI_6118", "input payload does not follow bulk add interface - missing \"body\"");
					}
					JsonElement bodyObj = it.get("body");
					if (!(bodyObj.isJsonObject())) {
						throw new AAIException("AAI_6111", "input payload does not follow bulk add interface");
					}
					
					Gson gson = new Gson();
					
					String bodyStr = gson.toJson(bodyObj);
					
					if (tuple.getValue2().equals(HttpMethod.PUT_EDGE)) {
						Introspector obj;
						try {
							obj = loader.unmarshal("relationship", bodyStr, org.openecomp.aai.restcore.MediaType.getEnum(inputMediaType));
						} catch (AAIUnmarshallingException e) {
							throw new AAIException("AAI_3000", "object could not be unmarshalled:" + bodyStr);

						}
						
					
						tuple = tuple.setAt1(obj);

					} else {
						QueryParser uriQuery = dbEngine.getQueryBuilder().createQueryFromURI(uri);
						String objName = uriQuery.getResultType();
						
						Introspector obj;
						try {
							obj = loader.unmarshal(objName, bodyStr, org.openecomp.aai.restcore.MediaType.getEnum(inputMediaType));
						} catch (AAIUnmarshallingException e) {
							throw new AAIException("AAI_3000", "object could not be unmarshalled:" + bodyStr);

						}
					
						this.validateIntrospector(obj, loader, uri, tuple.getValue2());
						tuple = tuple.setAt1(obj);
					}
					triplet.add(tuple);
				}
//				JsonElement itemURIfield = item.get("uri");
				
			} catch (AAIException e) {
				// even if tuple doesn't have a uri or body, this way we keep all information associated with this error together
				// even if both are null, that indicates how the input was messed up, so still useful to carry around like this
				triplet.add(tuple);
				throw e; //rethrow so the right response is generated on the level above
			}
		}
	}
	
	
	
	/**
	 * Generate response payload.
	 *
	 * @param allResponses - the list of the lists of responses from every action in every transaction requested
	 * @return A json string of similar format to the bulk add interface which for each response includes
	 * 				the original URI and a body with the status code of the response and the error message.
	 * 
	 * Creates the payload for a single unified response from all responses generated
	 */
	private String generateResponsePayload(List<List<Pair<URI,Response>>> allResponses){
		JsonObject ret = new JsonObject();
		JsonArray retArr = new JsonArray();
		
		for(List<Pair<URI,Response>> responses : allResponses){
			JsonObject tResp = new JsonObject();
			JsonArray tArrResp = new JsonArray();
			
			for (Pair<URI,Response> r : responses) {
				JsonObject indPayload = new JsonObject();
				
				URI origURI = r.getValue0();
				if (origURI != null) {
					indPayload.addProperty("uri", origURI.getPath());
				} else {
					indPayload.addProperty("uri", (String)null);
				}
				
				JsonObject body = new JsonObject();
				
				int rStatus = r.getValue1().getStatus();
				String rContents = null;
				
				rContents = (String)r.getValue1().getEntity();
				
				body.addProperty(new Integer(rStatus).toString(), rContents);
				indPayload.add("body", body);
				
				tArrResp.add(indPayload);
			}
			
			tResp.add("put", tArrResp);
			retArr.add(tResp);
		}
		ret.add("transaction", retArr);
		Gson gson = new GsonBuilder().serializeNulls().create();
		String jsonStr = gson.toJson(ret);
		return jsonStr;
	}
	
	/**
	 * Adds the exception case failure response.
	 *
	 * @param allResponses the all responses
	 * @param e the e
	 * @param index - index of which transaction was being processed when the exception was thrown
	 * @param thisUri the this uri
	 * @param headers the headers
	 * @param info the info
	 * @param templateAction the template action
	 * @param logline Generates a Response based on the given exception and adds it to the collection of responses for this request.
	 * @throws ErrorObjectNotFoundException 
	 */
	private void addExceptionCaseFailureResponse(List<List<Pair<URI, Response>>> allResponses, Exception e, int index, URI thisUri, HttpHeaders headers, UriInfo info, HttpMethod templateAction) {
		AAIException ex = null;
		
		if (!(e instanceof AAIException)){
			ex = new AAIException("AAI_4000", e);
		} else {
			ex = (AAIException)e;
		}
		
		if (allResponses.size() != (index+1)) {
			//index+1 bc if all transactions thus far have had a response list added
			//the size will be one more than the current index (since those are offset by 1)
			
			//this transaction doesn't have a response list yet, so create one
			Response failResp = consumerExceptionResponseGenerator(headers, info, templateAction, ex);
			Pair<URI, Response> uriResp = Pair.with(thisUri, failResp);
			List<Pair<URI, Response>> transRespList = new ArrayList<Pair<URI,Response>>();
			transRespList.add(uriResp);
			allResponses.add(transRespList);
		} else {
			//this transaction already has a response list, so add this failure response to it
			Response failResp = consumerExceptionResponseGenerator(headers, info, templateAction, ex);
			Pair<URI, Response> uriResp = Pair.with(thisUri, failResp);
			List<Pair<URI, Response>> tResps = allResponses.get(index);
			tResps.add(uriResp);
		}
	}
	
	protected abstract boolean functionAllowed(HttpMethod method);
	
	protected abstract boolean enableResourceVersion();

}

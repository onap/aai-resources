/**
 * ============LICENSE_START=======================================================
 * org.onap.aai
 * ================================================================================
 * Copyright © 2017 AT&T Intellectual Property. All rights reserved.
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
 *
 * ECOMP is a trademark and service mark of AT&T Intellectual Property.
 */
package org.onap.aai.rest;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
import org.onap.aai.dbmap.DBConnectionType;
import org.onap.aai.exceptions.AAIException;
import org.onap.aai.introspection.Introspector;
import org.onap.aai.introspection.Loader;
import org.onap.aai.introspection.ModelType;
import org.onap.aai.introspection.Version;
import org.onap.aai.introspection.exceptions.AAIUnmarshallingException;
import org.onap.aai.logging.ErrorObjectNotFoundException;
import org.onap.aai.parsers.query.QueryParser;
import org.onap.aai.rest.bulk.BulkOperation;
import org.onap.aai.rest.bulk.BulkOperationResponse;
import org.onap.aai.rest.db.DBRequest;
import org.onap.aai.rest.db.HttpEntry;
import org.onap.aai.rest.util.ValidateEncoding;
import org.onap.aai.restcore.HttpMethod;
import org.onap.aai.restcore.RESTAPI;
import org.onap.aai.serialization.engines.QueryStyle;
import org.onap.aai.serialization.engines.TransactionalGraphEngine;
import org.onap.aai.util.AAIConfig;
import org.onap.aai.util.AAIConstants;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

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

	private static final String BULK_PATCH_METHOD = "patch";
	private static final String BULK_DELETE_METHOD = "delete";
	private static final String BULK_PUT_METHOD = "put";

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
	public Response bulkProcessor(String content, @PathParam("version")String versionParam, @Context HttpHeaders headers, @Context UriInfo info, @Context HttpServletRequest req){
		
		String transId = headers.getRequestHeaders().getFirst("X-TransactionId");
		String sourceOfTruth = headers.getRequestHeaders().getFirst("X-FromAppId");
		String realTime = headers.getRequestHeaders().getFirst("Real-Time");
		String outputMediaType = getMediaType(headers.getAcceptableMediaTypes());
		Version version = Version.valueOf(versionParam);

		Response response = null;
		
		/* A Response will be generated for each object in each transaction.
		 * To keep track of what came from where to give organized feedback to the client,
		 * we keep responses from a given transaction together in one list (hence all being a list of lists)
		 * and BulkOperationResponse each response with its matching URI (which will be null if there wasn't one).
		 */ 
 		List<List<BulkOperationResponse>> allResponses = new ArrayList<>();
		try {
			DBConnectionType type = this.determineConnectionType(sourceOfTruth, realTime);
		
			JsonArray transactions = getTransactions(content, headers);
			
			for (int i = 0; i < transactions.size(); i++){
				HttpEntry httpEntry = new HttpEntry(version, introspectorFactoryType, queryStyle, type);
				Loader loader = httpEntry.getLoader();
				TransactionalGraphEngine dbEngine = httpEntry.getDbEngine();
				URI thisUri = null;
				List<BulkOperation> bulkOperations = new ArrayList<>();
				HttpMethod method = null;
				JsonElement transObj = new JsonObject();
				try {
					transObj = transactions.get(i);
					if (!transObj.isJsonObject()) {
						throw new AAIException("AAI_6111", "input payload does not follow bulk interface");
					}
					
					fillBulkOperationObjectFromTransaction(bulkOperations, transObj.getAsJsonObject(), loader, dbEngine, outputMediaType);
					if (bulkOperations.isEmpty()) {
						//case where user sends a validly formatted transactions object but
						//which has no actual things in it for A&AI to do anything with
						//assuming we should count this as a user error
						throw new AAIException("AAI_6118", "payload had no objects to operate on");
					}
				
					List<DBRequest> requests = new ArrayList<>();
					for (BulkOperation bulkOperation : bulkOperations){
						thisUri = bulkOperation.getUri(); 
						method = bulkOperation.getHttpMethod();
						QueryParser uriQuery = dbEngine.getQueryBuilder().createQueryFromURI(thisUri);
						DBRequest request = new DBRequest.Builder(method, thisUri, uriQuery, bulkOperation.getIntrospector(), headers, bulkOperation.getUriInfo(), transId).rawRequestContent(bulkOperation.getRawReq()).build();
						requests.add(request);
					}
					
					Pair<Boolean, List<Pair<URI, Response>>> results = httpEntry.process(requests, sourceOfTruth, this.enableResourceVersion());
					List<BulkOperationResponse> responses = BulkOperationResponse.processPairList(method, results.getValue1());
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
					if (!bulkOperations.isEmpty()) { //failed somewhere in the middle of bulkOperation-filling
						BulkOperation lastBulkOperation = bulkOperations.get(bulkOperations.size()-1); //last one in there was the problem
						if (lastBulkOperation.getIntrospector() == null){
							//failed out before thisUri could be set but after bulkOperation started being filled
							thisUri = lastBulkOperation.getUri();
							method = lastBulkOperation.getHttpMethod();
						}
					} //else failed out on empty payload so bulkOperations never filled (or failed out even earlier than bulkOperations-filling)
					
					if (method == null) {
						List<String> methods = transObj.getAsJsonObject().entrySet().stream().map(Entry::getKey).collect(Collectors.toCollection(ArrayList::new));
						
						if (methods.contains(BULK_PUT_METHOD)) {
							method = HttpMethod.PUT; 
						} else if (methods.contains(BULK_DELETE_METHOD)) {
							method = HttpMethod.DELETE;
						} else if (methods.contains(BULK_PATCH_METHOD)) {
							method = HttpMethod.MERGE_PATCH;
						} else {
							method = HttpMethod.PUT;
						}
					}
					
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
	private JsonArray getTransactions(String content, HttpHeaders headers) throws AAIException, JsonSyntaxException {
		JsonParser parser = new JsonParser();
		
		JsonObject input = parser.parse(content).getAsJsonObject();
		String module = getModule();

		if (!(input.has("transactions"))) {
			throw new AAIException("AAI_6118", String.format("input payload does not follow %s interface - missing \"transactions\"", module));
		}
		JsonElement transactionsObj = input.get("transactions");
		
		if (!(transactionsObj.isJsonArray())){
			throw new AAIException("AAI_6111", String.format("input payload does not follow %s interface", module));
		}
		JsonArray transactions = transactionsObj.getAsJsonArray();
		validateRequest(transactions, headers);
		return transactions;
	}
	
	/**
	 * Fill object bulkOperations from transaction.
	 *
	 * @param bulkOperations the bulk Operations
	 * @param transaction - JSON body containing the objects to be added
	 * 							each object must have a URI and an object body
	 * @param loader the loader
	 * @param dbEngine the db engine
	 * @param inputMediaType the input media type
	 * @return list of bulkOperations containing each introspector-wrapped object and its given URI
	 * @throws AAIException the AAI exception
	 * @throws JsonSyntaxException the json syntax exception
	 * @throws UnsupportedEncodingException Walks through the given transaction and unmarshals each object in it, then bundles each
	 * with its URI.
	 */
	private void fillBulkOperationObjectFromTransaction(List<BulkOperation> bulkOperations,
			JsonObject transaction, Loader loader, TransactionalGraphEngine dbEngine, String inputMediaType)
			throws AAIException,UnsupportedEncodingException {

	
			if (transaction.has(BULK_PUT_METHOD) && this.functionAllowed(HttpMethod.PUT)) {
				populateBulkOperations(bulkOperations, transaction, loader, dbEngine, inputMediaType, HttpMethod.PUT);
			} else if (transaction.has(BULK_DELETE_METHOD) && this.functionAllowed(HttpMethod.DELETE)) {
				populateBulkOperations(bulkOperations, transaction, loader, dbEngine, inputMediaType, HttpMethod.DELETE);
			} else if (transaction.has(BULK_PATCH_METHOD) && this.functionAllowed(HttpMethod.MERGE_PATCH)) {
				populateBulkOperations(bulkOperations, transaction, loader, dbEngine, inputMediaType, HttpMethod.MERGE_PATCH);
			} else {
				String msg = "input payload does not follow bulk %s interface - missing %s";
				String type = "process";
				String operations = "put delete or patch";
				
				if (this instanceof BulkAddConsumer) {
					type = "add";
					operations = BULK_PUT_METHOD;
				}
				throw new AAIException("AAI_6118", String.format(msg, type, operations));                                                    
			}

			

	}

	
	
	private void populateBulkOperations(List<BulkOperation> bulkOperations, JsonObject item, Loader loader, TransactionalGraphEngine dbEngine, String inputMediaType, HttpMethod method) throws AAIException, JsonSyntaxException, UnsupportedEncodingException{
		String module = getModule();
		for (int i=0; i<item.size(); i++) {
			BulkOperation bulkOperation = new BulkOperation();
			try {
				
				if (!(item.isJsonObject())) {
					throw new AAIException("AAI_6111", String.format("input payload does not follow %s interface", module));
				}
				
				JsonElement actionElement = null;
				
				if(item.has(BULK_PUT_METHOD)){
					actionElement = item.get(BULK_PUT_METHOD);
				} else if(item.has(BULK_PATCH_METHOD)){
					actionElement = item.get(BULK_PATCH_METHOD);
				} else if(item.has(BULK_DELETE_METHOD)){
					actionElement = item.get(BULK_DELETE_METHOD);
				}
				
				if ((actionElement == null) || !actionElement.isJsonArray()) {
					throw new AAIException("AAI_6111", String.format("input payload does not follow %s interface", module));
				}
				
				JsonArray httpArray = actionElement.getAsJsonArray();
				for (int j = 0; j < httpArray.size(); ++j) {
					
					bulkOperation = new BulkOperation();
					bulkOperation.setHttpMethod(method);
					
					JsonObject it = httpArray.get(j).getAsJsonObject();
					JsonElement itemURIfield = it.get("uri");
					if (itemURIfield == null) {
						throw new AAIException("AAI_6118", "must include object uri");
					}
					
					UriComponents uriComponents = UriComponentsBuilder.fromUriString(itemURIfield.getAsString()).build();
					if (uriComponents.getPath().endsWith("/relationship-list/relationship")) {
						if (method.equals(HttpMethod.PUT)) {
							bulkOperation.setHttpMethod(HttpMethod.PUT_EDGE);
						} else if (method.equals(HttpMethod.DELETE)) {
							bulkOperation.setHttpMethod(HttpMethod.DELETE_EDGE);
						}
					} else {
						bulkOperation.setHttpMethod(method);
					}
									
					URI uri = UriBuilder.fromPath(uriComponents.getPath()).build();
					
					/* adding the uri as soon as we have one (valid or not) lets us
					 * keep any errors with their corresponding uris for client feedback  
					 */
					bulkOperation.setUri(uri);
					
					bulkOperation.addUriInfoQueryParams(uriComponents.getQueryParams());
					
					if (!ValidateEncoding.getInstance().validate(uri)) {
						throw new AAIException("AAI_3008", "uri=" + uri.getPath());
					}
					
					JsonElement bodyObj = new JsonObject();
					if (!bulkOperation.getHttpMethod().equals(HttpMethod.DELETE)) {
						if (!(it.has("body"))){
							throw new AAIException("AAI_6118", String.format("input payload does not follow %s interface - missing \"body\"", module));
						}
						bodyObj = it.get("body");
						if (!(bodyObj.isJsonObject())) {
							throw new AAIException("AAI_6111", String.format("input payload does not follow %s interface", module));
						} 
					}
					
					Gson gson = new Gson();
					
					String bodyStr = gson.toJson(bodyObj);
					bulkOperation.setRawReq(bodyStr);
					
					if (bulkOperation.getHttpMethod().equals(HttpMethod.PUT_EDGE) || bulkOperation.getHttpMethod().equals(HttpMethod.DELETE_EDGE)) {
						Introspector obj;
						try {
							obj = loader.unmarshal("relationship", bodyStr, org.onap.aai.restcore.MediaType.getEnum(inputMediaType));
						} catch (AAIUnmarshallingException e) {
							throw new AAIException("AAI_3000", "object could not be unmarshalled:" + bodyStr);

						}
						
					
						bulkOperation.setIntrospector(obj);

					} else {
						QueryParser uriQuery = dbEngine.getQueryBuilder().createQueryFromURI(uri);
						String objName = uriQuery.getResultType();
						
						Introspector obj;
						
						if (bulkOperation.getHttpMethod().equals(HttpMethod.DELETE)) {
							obj = loader.introspectorFromName(objName);
						} else {
							try {
								obj = loader.unmarshal(objName, bodyStr, org.onap.aai.restcore.MediaType.getEnum(inputMediaType));
							} catch (AAIUnmarshallingException e) {
								throw new AAIException("AAI_3000", "object could not be unmarshalled:" + bodyStr);
	
							}
							this.validateIntrospector(obj, loader, uri, bulkOperation.getHttpMethod());

						}
					
						bulkOperation.setIntrospector(obj);
					}
					bulkOperations.add(bulkOperation);
				}
				
			} catch (AAIException e) {
				// even if bulkOperations doesn't have a uri or body, this way we keep all information associated with this error together
				// even if both are null, that indicates how the input was messed up, so still useful to carry around like this
				bulkOperations.add(bulkOperation);
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
	private String generateResponsePayload(List<List<BulkOperationResponse>> allResponses){
		JsonObject ret = new JsonObject();
		JsonArray retArr = new JsonArray();
		
		for(List<BulkOperationResponse> responses : allResponses){
			JsonObject tResp = new JsonObject();
			JsonArray tArrResp = new JsonArray();
			HttpMethod method = HttpMethod.PUT;
			
			for (BulkOperationResponse r : responses) {
				
				JsonObject indPayload = new JsonObject();
				method = r.getHttpMethod();
				
				URI origURI = r.getUri();
				if (origURI != null) {
					indPayload.addProperty("uri", origURI.getPath());
				} else {
					indPayload.addProperty("uri", (String)null);
				}
				
				JsonObject body = new JsonObject();
				
				int rStatus = r.getResponse().getStatus();
				String rContents = null;
				
				rContents = (String)r.getResponse().getEntity();
				
				body.addProperty(Integer.toString(rStatus), rContents);
				indPayload.add("body", body);
				
				tArrResp.add(indPayload);
			}
			
			tResp.add(this.mapHttpMethodToBulkMethod(method), tArrResp);
			retArr.add(tResp);
		}
		ret.add("transaction", retArr);
		Gson gson = new GsonBuilder().serializeNulls().create();
		return gson.toJson(ret);
	}
	
	private String mapHttpMethodToBulkMethod(HttpMethod method) {
		if (HttpMethod.PUT.equals(method) || HttpMethod.PUT_EDGE.equals(method)) {
			return BULK_PUT_METHOD;
		} else if (HttpMethod.DELETE.equals(method) || HttpMethod.DELETE_EDGE.equals(method)) {
			return BULK_DELETE_METHOD;
		} else if (HttpMethod.MERGE_PATCH.equals(method)) {
			return BULK_PATCH_METHOD;
		} else {
			return "";
		}
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
	private void addExceptionCaseFailureResponse(List<List<BulkOperationResponse>> allResponses, Exception e, int index, URI thisUri, HttpHeaders headers, UriInfo info, HttpMethod templateAction) {
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
			BulkOperationResponse uriResp =  new BulkOperationResponse(templateAction, thisUri, failResp);
			List<BulkOperationResponse> transRespList = new ArrayList<>();
			transRespList.add(uriResp);
			allResponses.add(transRespList);
		} else {
			//this transaction already has a response list, so add this failure response to it
			Response failResp = consumerExceptionResponseGenerator(headers, info, templateAction, ex);
			BulkOperationResponse uriResp = new BulkOperationResponse(templateAction, thisUri, failResp);
			List<BulkOperationResponse> tResps = allResponses.get(index);
			tResps.add(uriResp);
		}
	}

	/**
	 * Pulls the config value for the limit of operations allowed in a bulk add/process request
	 *
	 * @throws AAIException
	 */
	private int getPayLoadLimit() throws AAIException{
		return Integer.parseInt(AAIConfig.get(AAIConstants.AAI_BULKCONSUMER_LIMIT));
	}

	/**
	 * Validates the amount of operations in a request payload is allowed
	 *
	 * @param transactions - a JsonArray of all the transactions in the request payload
	 * @throws AAIException
	 */
	private void validateRequest(JsonArray transactions, HttpHeaders headers) throws AAIException{
		String overrideLimit = headers.getRequestHeaders().getFirst("X-OverrideLimit");
		boolean isOverride = overrideLimit != null && !AAIConfig.get(AAIConstants.AAI_BULKCONSUMER_OVERRIDE_LIMIT).equals("false")
				&& overrideLimit.equals(AAIConfig.get(AAIConstants.AAI_BULKCONSUMER_OVERRIDE_LIMIT));
		if (transactions.size() == 0) {
			//case where user sends a validly formatted transactions object but
			//which has no actual things in it for A&AI to do anything with
			//assuming we should count this as a user error
			throw new AAIException("AAI_6118", "payload had no objects to operate on");
		}else if(!isOverride && transactions.size() > getPayLoadLimit()) {
			throw new AAIException("AAI_6147", String.format("Payload limit of %s reached, please reduce payload.", getPayLoadLimit()));
		}
		if(!isOverride) {
			int operationCount = 0;
			int payLoadLimit = getPayLoadLimit();
			for (int i = 0; i < transactions.size(); i++) {
				Set<Entry<String, JsonElement>> entrySet = transactions.get(i).getAsJsonObject().entrySet();
				Iterator<Entry<String, JsonElement>> it = entrySet.iterator();
				while (it.hasNext()) {
					Map.Entry<String, JsonElement> element = it.next();
					if (element.getValue() instanceof JsonArray) {
						operationCount += ((JsonArray) element.getValue()).size();
					} else {
						operationCount++;
					}
				}
				if (operationCount > payLoadLimit) {
					throw new AAIException("AAI_6147", String.format("Payload limit of %s reached, please reduce payload.", payLoadLimit));
				}
			}
		}
	}

	protected abstract String getModule();
	
	protected abstract boolean functionAllowed(HttpMethod method);
	
	protected abstract boolean enableResourceVersion();

}

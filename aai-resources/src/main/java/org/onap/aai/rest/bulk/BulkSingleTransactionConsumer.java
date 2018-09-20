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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.javatuples.Pair;
import org.onap.aai.config.SpringContextAware;
import org.onap.aai.dbmap.DBConnectionType;
import org.onap.aai.exceptions.AAIException;
import org.onap.aai.introspection.Introspector;
import org.onap.aai.introspection.Loader;
import org.onap.aai.logging.ErrorLogHelper;
import org.onap.aai.logging.LoggingContext;
import org.onap.aai.rest.bulk.pojos.Operation;
import org.onap.aai.rest.bulk.pojos.OperationResponse;
import org.onap.aai.rest.bulk.pojos.Transaction;
import org.onap.aai.rest.bulk.pojos.TransactionResponse;
import org.onap.aai.rest.db.DBRequest;
import org.onap.aai.rest.db.HttpEntry;
import org.onap.aai.restcore.HttpMethod;
import org.onap.aai.restcore.MediaType;
import org.onap.aai.restcore.RESTAPI;
import org.onap.aai.serialization.engines.TransactionalGraphEngine;
import org.onap.aai.setup.SchemaVersion;
import org.onap.aai.util.AAIConfig;
import org.onap.aai.util.AAIConstants;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.*;

@Path(value = "{version: v[1-9][0-9]*|latest}/bulk/single-transaction")
public class BulkSingleTransactionConsumer extends RESTAPI {

	private static final String TARGET_ENTITY = "aai-resources";
	private static final Set<String> validOperations = Collections.unmodifiableSet(new HashSet<>(Arrays.asList("put", "patch", "delete")));
	private int allowedOperationCount = 30;
	private TransactionalGraphEngine dbEngine = null;

	@POST
	@Consumes(value = javax.ws.rs.core.MediaType.APPLICATION_JSON)
	@Produces(value = javax.ws.rs.core.MediaType.APPLICATION_JSON)
	public Response process(String content, @PathParam(value = "version")String versionParam, @Context HttpHeaders headers, @Context UriInfo info, @Context HttpServletRequest req){

		String transId = headers.getRequestHeaders().getFirst("X-TransactionId");
		String sourceOfTruth = headers.getRequestHeaders().getFirst("X-FromAppId");
		String realTime = headers.getRequestHeaders().getFirst("Real-Time");
		SchemaVersion version = new SchemaVersion(versionParam);
		DBConnectionType type;

		initLogging(req, transId, sourceOfTruth);

		try {
			type = DBConnectionType.REALTIME;

			// unmarshall the payload.
			Gson gson = new Gson();
			Transaction transaction;
			try {
				transaction = gson.fromJson(content, Transaction.class);
			} catch (Exception e) {
				throw new AAIException("AAI_6111", "Input payload does not follow bulk/single-transaction interface");
			}

			//set the operation count limit
			setOperationCount(headers);
			//validate
			validate(transaction);

			//generate bulkoperations
			List<BulkOperation> bulkOperations = generateBulkOperations(transaction);

			//init http entry
			HttpEntry resourceHttpEntry = SpringContextAware.getBean("traversalUriHttpEntry", HttpEntry.class);
			resourceHttpEntry.setHttpEntryProperties(version, type);
			Loader loader = resourceHttpEntry.getLoader();
			TransactionalGraphEngine dbEngine = resourceHttpEntry.getDbEngine();

			//populate uri query
			populateUriQuery(bulkOperations, dbEngine);

			//populate introspector for operations
			populateIntrospectors(bulkOperations, loader);

			//generate db process from bulk operation
			List<DBRequest> dbRequests = bulkOperationToDbRequests(headers, transId, bulkOperations);

			//process db requests
			Pair<Boolean, List<Pair<URI, Response>>> results = resourceHttpEntry.process(dbRequests, sourceOfTruth, this.enableResourceVersion());

			//process result of db requests
			TransactionResponse transactionResponse = buildTransactionResponse(transaction, results.getValue1());

			//commit/rollback based on results
			if (results.getValue0()) { //everything was processed without error
				dbEngine.commit();
			} else { //something failed
				dbEngine.rollback();
			}

			return Response
					.status(Response.Status.CREATED)
					.entity(new GsonBuilder().serializeNulls().create().toJson(transactionResponse))
					.build();

		} catch (AAIException e) {
			return consumerExceptionResponseGenerator(headers, info, javax.ws.rs.HttpMethod.POST, e);
		} finally {
			if (dbEngine != null) {
				dbEngine.rollback();
			}
		}

	}


	/**
	 * Builds the response
	 * @param transaction the input transactions
	 * @param results the response of all of he operations
	 * @return TansactionResponse obj representing the result of the transaction
	 * @throws AAIException thrown if there is a failure in the result. Msg contains the details of the first failure.
	 */
	private TransactionResponse buildTransactionResponse(Transaction transaction, List<Pair<URI, Response>> results) throws AAIException {
		TransactionResponse transactionResponse = new TransactionResponse();
		transactionResponse.setOperationResponses(new ArrayList<>(transaction.getOperations().size()));
		final String failureInResponse = "Operation %s failed with status code (%s) and msg (%s)";
		for (int i = 0; i < transaction.getOperations().size(); i++) {
			if (!Response.Status.Family.familyOf(results.get(i).getValue1().getStatus())
					.equals(Response.Status.Family.SUCCESSFUL)) {
				throw new AAIException("AAI_3000",
						String.format(
								failureInResponse,
								i,
								results.get(i).getValue1().getStatus(),
								results.get(i).getValue1().getEntity()));
			}
			OperationResponse operationResponse = new OperationResponse();
			operationResponse.setResponseStatusCode(results.get(i).getValue1().getStatus());
			operationResponse.setAction(transaction.getOperations().get(i).getAction());
			operationResponse.setUri(transaction.getOperations().get(i).getUri());
			operationResponse.setBody(results.get(i).getValue1().getEntity());
			transactionResponse.getOperationResponsess().add(i, operationResponse);
		}
		return transactionResponse;
	}

	/**
	 * Generate one DBRequest per BulkOperation
	 * @param headers request headers
	 * @param transId transaction id
	 * @param bulkOperations operations to convert
	 * @return One DBRequest per BulkOperation
	 * @throws AAIException thrown if there are any issues with the transform
	 */
	private List<DBRequest> bulkOperationToDbRequests(@Context HttpHeaders headers, String transId, List<BulkOperation> bulkOperations) throws AAIException {
		List<DBRequest> requests = new ArrayList<>();
		for (int i = 0; i < bulkOperations.size(); i++) {
			try {
				BulkOperation bulkOperation = bulkOperations.get(i);
				DBRequest request = new DBRequest.Builder(
						bulkOperation.getHttpMethod(),
						bulkOperation.getUri(),
						bulkOperation.getUriQuery(),
						bulkOperation.getIntrospector(),
						headers,
						bulkOperation.getUriInfo(),
						transId
				).rawRequestContent(bulkOperation.getRawReq()).build();
				requests.add(request);
			} catch (Exception e) {
				throw new AAIException("AAI_3000", "Error with operation " + i + ": " +e.getMessage());
			}
		}
		return requests;
	}

	/**
	 * Sets the uriquery for each bulk operation
	 * @param bulkOperations operations to generate queries for
	 * @param dbEngine engine for query builder generation
	 * @throws AAIException thrown for issues with generating uri query
	 */
	private void populateUriQuery(List<BulkOperation> bulkOperations, TransactionalGraphEngine dbEngine) throws AAIException {
		for (int i = 0; i < bulkOperations.size(); i++) {
			try {
				bulkOperations.get(i).setUriQuery(dbEngine.getQueryBuilder().createQueryFromURI(bulkOperations.get(i).getUri()));
			} catch (AAIException e) {
				throw new AAIException(e.getCode(), "Error with operation " + i + ": " +e.getMessage());
			} catch (UnsupportedEncodingException e) {
				throw new AAIException("AAI_3000", "Error with operation " + i + ": " +e.getMessage());
			}
		}
	}

	/**
	 * Sets the introspector for each bulk operation. requires that uriquery is set per operation
	 * @param bulkOperations operations to generate introspector for
	 * @param loader Loader for generating introspector
	 * @throws AAIException thrown for issues with generating introspector
	 */
	private void populateIntrospectors(List<BulkOperation> bulkOperations, Loader loader) throws AAIException {

		final String objectUnMarshallMsg = "Body of operation %s could not be unmarshalled: %s";
		Introspector obj;
		for (int i = 0; i < bulkOperations.size(); i++) {
			BulkOperation bulkOperation = bulkOperations.get(i);
			try {
				if (bulkOperation.getHttpMethod().equals(HttpMethod.PUT_EDGE)
						|| bulkOperation.getHttpMethod().equals(HttpMethod.DELETE_EDGE)) {
					obj = loader.unmarshal("relationship", bulkOperation.getRawReq(), MediaType.APPLICATION_JSON_TYPE);
					bulkOperation.setIntrospector(obj);
				} else {
					String objName = bulkOperation.getUriQuery().getResultType();
					if (bulkOperation.getHttpMethod().equals(HttpMethod.DELETE)) {
						obj = loader.introspectorFromName(objName);
					} else {
						obj = loader.unmarshal(objName, bulkOperation.getRawReq(), MediaType.APPLICATION_JSON_TYPE);
						this.validateIntrospector(obj, loader, bulkOperation.getUri(), bulkOperation.getHttpMethod());
					}
					bulkOperation.setIntrospector(obj);
				}
			} catch (UnsupportedEncodingException e) {
				throw new AAIException("AAI_3000", String.format(objectUnMarshallMsg, i, bulkOperation.getRawReq()));
			}
		}

	}

	/**
	 * Sets the allowedOperationCount to one of the following
	 *     - Integer.MAX_VALUE if override limit is configured
	 *     - Property in aaiconfig
	 *     - 30 by default
	 * @param headers request header
	 */
	private void setOperationCount(HttpHeaders headers) {
		try {
			String overrideLimit = headers.getRequestHeaders().getFirst("X-OverrideLimit");
			boolean isOverride = overrideLimit != null && !AAIConfig.get(AAIConstants.AAI_BULKCONSUMER_OVERRIDE_LIMIT).equals("false")
					&& overrideLimit.equals(AAIConfig.get(AAIConstants.AAI_BULKCONSUMER_OVERRIDE_LIMIT));
			if (isOverride) {
				allowedOperationCount = Integer.MAX_VALUE;
			} else {
				allowedOperationCount = AAIConfig.getInt(AAIConstants.AAI_BULKCONSUMER_LIMIT);
			}
		} catch (AAIException e) {
			allowedOperationCount = 30;
		}
	}

	/**
	 * Converts the request transaction into a list of bulk operations
	 * @param transaction transaction to extract bulk operations from
	 * @return list of bulk operations
	 */
	private List<BulkOperation> generateBulkOperations(Transaction transaction) {
		List<BulkOperation> bulkOperations = new ArrayList<>(transaction.getOperations().size());

		BulkOperation bulkOperation;
		for (int i = 0; i < transaction.getOperations().size(); i++) {
			final Operation operation = transaction.getOperations().get(i);
			bulkOperation = new BulkOperation();

			UriComponents uriComponents = UriComponentsBuilder.fromUriString(operation.getUri()).build();
			bulkOperation.setUri(UriBuilder.fromPath(uriComponents.getPath()).build());
			bulkOperation.addUriInfoQueryParams(uriComponents.getQueryParams());
			bulkOperation.setHttpMethod(getHttpMethod(operation.getAction(), bulkOperation.getUri()));
			bulkOperation.setRawReq(operation.getBody().toString());
			bulkOperations.add(bulkOperation);
		}

		return bulkOperations;
	}

	/**
	 * Map action to httpmethod
	 * @param action action to be mapped
	 * @param uri uri of the action
	 * @return HttpMethod thats action/uri maps to
	 */
	private HttpMethod getHttpMethod(String action, URI uri) {
		HttpMethod method = HttpMethod.GET;
		switch (action) {
			case "put":
				method = HttpMethod.PUT;
				break;
			case "delete":
				method = HttpMethod.DELETE;
				break;
			case "patch":
				method = HttpMethod.MERGE_PATCH;
				break;
		}
		if (uri.getPath().endsWith("/relationship-list/relationship")) {
			if (method.equals(HttpMethod.PUT)) {
				method = HttpMethod.PUT_EDGE;
			} else if (method.equals(HttpMethod.DELETE)) {
				method = HttpMethod.DELETE_EDGE;
			}
		}

		return method;
	}


	/**
	 * For each operation validates:
	 *     - action is provided and correct.
	 *     - uri exists
	 *     - body exists
	 * @param transaction parsed payload
	 * @throws AAIException with the violations in the msg
	 */
	private void validate(Transaction transaction) throws AAIException {
		if (transaction == null) {
			throw new AAIException("AAI_6111", "input payload does not follow /bulk/single-transaction interface");
		}

		if (transaction.getOperations() == null
				|| transaction.getOperations().isEmpty()) {
			throw new AAIException("AAI_6118", " Payload has no objects to operate on");
		} else if (transaction.getOperations().size() > allowedOperationCount){
			throw new AAIException("AAI_6147", " Allowed limit = " + allowedOperationCount);
		}

		final String missingFieldMsgFormat = "Operation %s missing '%s'";
		final String invalidActionMsgFormat = "Operation %s has invalid action '%s'";
		List<String> msgs = new ArrayList<>();
		for (int i = 0; i < transaction.getOperations().size(); i++) {
			final Operation operation = transaction.getOperations().get(i);
			if (operation.getAction() == null || operation.getAction().isEmpty()) {
				msgs.add(String.format(missingFieldMsgFormat, i, "action"));
			} else if (!validOperations.contains(operation.getAction())) {
				msgs.add(String.format(invalidActionMsgFormat, i, operation.getAction()));
			}
			if (operation.getUri() == null || operation.getUri().isEmpty()) {
				msgs.add(String.format(missingFieldMsgFormat, i, "uri"));
			}
			if (operation.getBody() == null) {
				msgs.add(String.format(missingFieldMsgFormat, i, "body"));
			}
		}
		if (!msgs.isEmpty()) {
			throw new AAIException("AAI_6111", "input payload missing required properties. [" + String.join(", ", msgs) + "]");
		}

	}

	/**
	 * Initialize logging context
	 * @param req requestContext
	 * @param transId transaction id
	 * @param sourceOfTruth application source
	 */
	private void initLogging(@Context HttpServletRequest req, String transId, String sourceOfTruth) {
		String serviceName = req.getMethod() + " " + req.getRequestURI().toString();
		LoggingContext.requestId(transId);
		LoggingContext.partnerName(sourceOfTruth);
		LoggingContext.serviceName(serviceName);
		LoggingContext.targetEntity(TARGET_ENTITY);
		LoggingContext.targetServiceName(serviceName);
	}

	protected boolean enableResourceVersion() {
		return true;
	}


	/**
	 * Consumer exception response generator.
	 *
	 * @param headers the headers
	 * @param info the info
	 * @param action type of request
	 * @param e the e
	 * @return the response
	 */
	protected Response consumerExceptionResponseGenerator(HttpHeaders headers, UriInfo info, String action, AAIException e) {
		ArrayList<String> templateVars = new ArrayList<>();
		templateVars.add(action); //GET, PUT, etc
		templateVars.add(info.getPath());
		templateVars.addAll(e.getTemplateVars());

		ErrorLogHelper.logException(e);
		return Response
				.status(e.getErrorObject().getHTTPResponseCode())
				.entity(ErrorLogHelper.getRESTAPIErrorResponseWithLogging(headers.getAcceptableMediaTypes(), e, templateVars))
				.build();
	}
}

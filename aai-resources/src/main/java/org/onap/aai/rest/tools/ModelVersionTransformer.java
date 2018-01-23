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
package org.onap.aai.rest.tools;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.Encoded;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.onap.aai.db.props.AAIProperties;
import org.onap.aai.dbmap.DBConnectionType;
import org.onap.aai.exceptions.AAIException;
import org.onap.aai.introspection.Introspector;
import org.onap.aai.introspection.Loader;
import org.onap.aai.introspection.MarshallerProperties;
import org.onap.aai.introspection.ModelType;
import org.onap.aai.introspection.Version;
import org.onap.aai.introspection.exceptions.AAIUnknownObjectException;
import org.onap.aai.logging.ErrorLogHelper;
import org.onap.aai.rest.db.HttpEntry;
import org.onap.aai.rest.exceptions.AAIInvalidXMLNamespace;
import org.onap.aai.rest.util.ValidateEncoding;
import org.onap.aai.restcore.RESTAPI;
import org.onap.aai.serialization.db.EdgeType;
import org.onap.aai.serialization.db.exceptions.NoEdgeRuleFoundException;
import org.onap.aai.serialization.engines.QueryStyle;
import org.onap.aai.serialization.engines.TransactionalGraphEngine;

import com.att.eelf.configuration.EELFLogger;
import com.att.eelf.configuration.EELFManager;
import com.google.common.base.Joiner;


/**
 * The Class ModelVersionTransformer.
 */
@Path("tools")
public class ModelVersionTransformer extends RESTAPI {

	private static final EELFLogger LOGGER = EELFManager.getInstance().getLogger(ModelVersionTransformer.class.getName());
	protected static String authPolicyFunctionName = "REST";
	private ModelType introspectorFactoryType = ModelType.MOXY;
	private QueryStyle queryStyle = QueryStyle.TRAVERSAL;
	protected static String MODEL_ELEMENTS = "model-elements";
	private static final String RELATIONSHIP="relationship";


	/**
	 * POST for model transformation.
	 *
	 * @param content the content
	 * @param uri the uri
	 * @param headers the headers
	 * @param info the info
	 * @param req the req
	 * @return the transformed model
	 * @Path("/{uri: modeltransform}")
	 * @throws UnsupportedEncodingException 
	 */
	@POST
	@Path("/{uri: modeltransform}")
	@Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	public Response modelTransform (String content, @PathParam("uri") @Encoded String uri, @Context HttpHeaders headers, @Context UriInfo info, @Context HttpServletRequest req) throws UnsupportedEncodingException {
		Response response = null;
		TransactionalGraphEngine dbEngine = null;
		Loader loader = null;
		MediaType mediaType = headers.getMediaType();
		String sourceOfTruth = headers.getRequestHeaders().getFirst("X-FromAppId");
		String transId = headers.getRequestHeaders().getFirst("X-TransactionId");
		String realTime = headers.getRequestHeaders().getFirst("Real-Time");
		Boolean success = true;
		AAIException ex;
		
		try {
			validateRequest(info);

			DBConnectionType type = this.determineConnectionType(sourceOfTruth, realTime);
			HttpEntry httpEntry = new HttpEntry(Version.v8, introspectorFactoryType, queryStyle, type);
			loader = httpEntry.getLoader();
			dbEngine = httpEntry.getDbEngine();
			if (content.length() == 0) {
				if (mediaType.toString().contains(MediaType.APPLICATION_JSON)) {
					content = "{}";
				} else {
					content = "<empty/>";
				}
			}

			//Unmarshall the received model and store properties and values in a map.
			Introspector obj = loader.unmarshal("Model", content, org.onap.aai.restcore.MediaType.getEnum(this.getInputMediaType(mediaType)));
			if (obj == null) {
				throw new AAIException("AAI_3000", "object could not be unmarshalled:" + content);
			}

			if (mediaType.toString().contains(MediaType.APPLICATION_XML) && !content.equals("<empty/>") && isEmptyObject(obj)) {
				throw new AAIInvalidXMLNamespace(content);
			}

			Set<String> properties = obj.getProperties();
			java.util.Iterator<String> propItr = properties.iterator();

			Map<String, Object> v8PropMap = new HashMap<String, Object>();
			while (propItr.hasNext()){
				String property = propItr.next();
				Object propertyValue = obj.getValue(property);
				if (propertyValue != null) {
					v8PropMap.put(propItr.next(), propertyValue);
				}
			}
			// Get the current models and create a map of model-ver to model keys, this allows us
			// to easily figure out and construct the relationships on the supplied v8 model
			Map<String,String> modelVersionIdToModelInvariantIdMap = getCurrentModelsFromGraph(headers, transId, info);

			//	        Build the v10 - TODO
			HttpEntry newHttpEntry = new HttpEntry(Version.v10, introspectorFactoryType, queryStyle, type);
			Loader newLoader = newHttpEntry.getLoader();
			Introspector newModelObj = newLoader.introspectorFromName("Model");

			// pull the attributes we need to apply to the model + model-ver objects
			// model specific attrs
			String oldModelInvariantId = obj.getValue("model-id");
			String oldModelType = obj.getValue("model-type");
			// model-ver specific
			String oldModelName = obj.getValue("model-name");
			String oldModelVersionId = obj.getValue("model-name-version-id");
			String oldModelVersion = obj.getValue("model-version");

			// copy attributes from the v8 model object to the v10 model object
			newModelObj.setValue("model-invariant-id", oldModelInvariantId);
			newModelObj.setValue("model-type", oldModelType);

			Introspector modelVersObj = newModelObj.newIntrospectorInstanceOfProperty("model-vers");

			newModelObj.setValue("model-vers", modelVersObj.getUnderlyingObject());

			List<Object> modelVerList = (List<Object>)modelVersObj.getValue("model-ver");

			//create a model-ver object
			Introspector modelVerObj = newLoader.introspectorFromName("ModelVer");
			// load attributes from the v8 model object into the v10 model-ver object
			modelVerObj.setValue("model-version-id", oldModelVersionId);
			modelVerObj.setValue("model-name", oldModelName);
			modelVerObj.setValue("model-version", oldModelVersion);


			if (obj.hasProperty(MODEL_ELEMENTS)) { 
				Introspector oldModelElements = obj.getWrappedValue(MODEL_ELEMENTS);
				if (oldModelElements != null) {
					Introspector newModelElements = modelVerObj.newIntrospectorInstanceOfProperty(MODEL_ELEMENTS);
					modelVerObj.setValue(MODEL_ELEMENTS, newModelElements.getUnderlyingObject());
					repackModelElements(oldModelElements, newModelElements, modelVersionIdToModelInvariantIdMap);
				}
			}

			modelVerList.add(modelVerObj.getUnderlyingObject());

			String outputMediaType = getMediaType(headers.getAcceptableMediaTypes());
			MarshallerProperties marshallerProperties = 
					new MarshallerProperties.Builder(org.onap.aai.restcore.MediaType.getEnum(outputMediaType)).build();

			String result = newModelObj.marshal(marshallerProperties);
			response = Response.ok(result).build();

		} catch (AAIException e) {

			ArrayList<String> templateVars = new ArrayList<>(2);
			templateVars.add("POST modeltransform");
			templateVars.add("model-ver.model-version-id");
			response = Response
					.status(e.getErrorObject().getHTTPResponseCode())
					.entity(ErrorLogHelper.getRESTAPIErrorResponse(
							headers.getAcceptableMediaTypes(), e,
							templateVars)).build();
			success = false;
		} catch (Exception e) {
			ArrayList<String> templateVars = new ArrayList<String>(2);
			templateVars.add("POST modeltransform");
			templateVars.add("model-ver.model-version-id");
			ex = new AAIException("AAI_4000", e);
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


	private void repackModelElements(Introspector oldModelElements, Introspector newModelElements, Map<String, String> modelVersionIdToModelInvariantIdMap) 
			throws AAIUnknownObjectException, AAIException {

		List<Introspector> oldModelElementList = oldModelElements.getWrappedListValue("model-element");
		List<Object> newModelElementList = (List<Object>)newModelElements.getValue("model-element");

		for (Introspector oldModelElement : oldModelElementList) { 
			Introspector newModelElement = newModelElements.getLoader().introspectorFromName("model-element");

			ArrayList<String> attrs = new ArrayList<String>();

			attrs.add("model-element-uuid");
			attrs.add("new-data-del-flag");
			attrs.add("cardinality");
			attrs.add("linkage-points");

			for (String attr : attrs) { 
				if (oldModelElement.hasProperty(attr)) {
					newModelElement.setValue(attr, oldModelElement.getValue(attr));
				}
			}
			
			if (oldModelElement.hasProperty("relationship-list")) { 
				
				Introspector oldRelationshipList = oldModelElement.getWrappedValue("relationship-list");
				Introspector newRelationshipList = newModelElements.getLoader().introspectorFromName("relationship-list");
				newModelElement.setValue("relationship-list", newRelationshipList.getUnderlyingObject());

				List<Introspector> oldRelationshipListList = oldRelationshipList.getWrappedListValue(RELATIONSHIP);
				List<Object> newRelationshipListList = (List<Object>)newRelationshipList.getValue(RELATIONSHIP);

				for (Introspector oldRelationship : oldRelationshipListList) { 

					Introspector newRelationship = newModelElements.getLoader().introspectorFromName(RELATIONSHIP);
					newRelationshipListList.add(newRelationship.getUnderlyingObject());

					List<Introspector> oldRelationshipData = oldRelationship.getWrappedListValue(RELATIONSHIP);
					List<Object> newRelationshipData = (List<Object>)newRelationship.getValue("relationship-data");

					newRelationship.setValue("related-to", "model-ver");

					for (Introspector oldRelationshipDatum : oldRelationshipData) { 

						String oldProp = null;
						String oldVal = null;

						if (oldRelationshipDatum.hasProperty("relationship-key")) { 
							oldProp = oldRelationshipDatum.getValue("relationship-key");
						} 
						if (oldRelationshipDatum.hasProperty("relationship-value")) { 
							oldVal = oldRelationshipDatum.getValue("relationship-value");
						}

						if ("model.model-name-version-id".equals(oldProp)) {
							// make two new relationshipDatum for use w/ the new style model

							// you should have the model in the list of models we collected earlier
							if (modelVersionIdToModelInvariantIdMap.containsKey(oldVal)) { 
								Introspector newRelationshipDatum1 = newModelElements.getLoader().introspectorFromName("relationship-data");
								Introspector newRelationshipDatum2 = newModelElements.getLoader().introspectorFromName("relationship-data");

								String modelId = modelVersionIdToModelInvariantIdMap.get(oldVal);

								// the first one points at the model-invariant-id of found model
								newRelationshipDatum1.setValue("relationship-key", "model.model-invariant-id");
								newRelationshipDatum1.setValue("relationship-value", modelId);

								// the second one points at the model-version-id which corresponds to the old model-name-version-id
								newRelationshipDatum2.setValue("relationship-key", "model-ver.model-version-id");
								newRelationshipDatum2.setValue("relationship-value", oldVal);

								newRelationshipData.add(newRelationshipDatum1.getUnderlyingObject());
								newRelationshipData.add(newRelationshipDatum2.getUnderlyingObject());
							} else { 
								throw new AAIException("AAI_6114", "No model-ver found using model-ver.model-version-id=" + oldVal);
							}
						}
					}

				}
			}

			if (oldModelElement.hasProperty(MODEL_ELEMENTS)) {
				Introspector nextOldModelElements = oldModelElement.getWrappedValue(MODEL_ELEMENTS);
				if (nextOldModelElements != null) {
					Introspector nextNewModelElements = newModelElement.newIntrospectorInstanceOfProperty(MODEL_ELEMENTS);
					newModelElement.setValue(MODEL_ELEMENTS, nextNewModelElements.getUnderlyingObject());
					repackModelElements(nextOldModelElements, nextNewModelElements, modelVersionIdToModelInvariantIdMap);
				}
			}
			newModelElementList.add(newModelElement.getUnderlyingObject());
		}
		return;

	}

	private Map<String, String> getCurrentModelsFromGraph(HttpHeaders headers, String transactionId, UriInfo info) throws  AAIException {

		TransactionalGraphEngine dbEngine = null;
		Map<String, String> modelVerModelMap = new HashMap<>() ;
		try {

			Version version = AAIProperties.LATEST;
			DBConnectionType type = DBConnectionType.REALTIME;

			final HttpEntry httpEntry = new HttpEntry(version, introspectorFactoryType, queryStyle, type);
			dbEngine = httpEntry.getDbEngine();

			List<Vertex> modelVertices =  dbEngine.asAdmin().getTraversalSource().V().has(AAIProperties.NODE_TYPE,"model").toList();
			for (Vertex modelVtx : modelVertices) { 

				List<Vertex> modelVerVerts = dbEngine.getQueryBuilder(modelVtx).createEdgeTraversal(EdgeType.TREE, "model", "model-ver").toList();
				for (Vertex v : modelVerVerts) { 
					modelVerModelMap.put(v.value("model-version-id"), modelVtx.value("model-invariant-id"));
				}
			}
		} catch (NoSuchElementException e) {
			throw new NoSuchElementException();
		} catch (Exception e1) { 
			LOGGER.error("Exception while getting current models from graph"+e1);
		}
		return modelVerModelMap;

	}

	/**
	 * Validate request.
	 *
	 * @param uri the uri
	 * @param headers the headers
	 * @param req the req
	 * @param action the action
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
		if (map.keySet().isEmpty()) {
			path += params + queryParams;
		}

		return path;

	}

	protected boolean isEmptyObject(Introspector obj) {
		return "{}".equals(obj.marshal(false));
	}


}

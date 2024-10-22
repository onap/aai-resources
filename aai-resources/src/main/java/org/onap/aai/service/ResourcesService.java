/**
 * ============LICENSE_START=======================================================
 * org.onap.aai
 * ================================================================================
 * Copyright © 2024 Deutsche Telekom. All rights reserved.
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
package org.onap.aai.service;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.javatuples.Pair;
import org.onap.aai.config.SpringContextAware;
import org.onap.aai.db.props.AAIProperties;
import org.onap.aai.exceptions.AAIException;
import org.onap.aai.introspection.Introspector;
import org.onap.aai.introspection.Loader;
import org.onap.aai.introspection.tools.CreateUUID;
import org.onap.aai.introspection.tools.DefaultFields;
import org.onap.aai.introspection.tools.InjectKeysFromURI;
import org.onap.aai.introspection.tools.IntrospectorValidator;
import org.onap.aai.introspection.tools.Issue;
import org.onap.aai.introspection.tools.RemoveNonVisibleProperty;
import org.onap.aai.logging.ErrorLogHelper;
import org.onap.aai.parsers.query.QueryParser;
import org.onap.aai.query.builder.Pageable;
import org.onap.aai.query.builder.QueryOptions;
import org.onap.aai.rest.RestTokens;
import org.onap.aai.rest.db.DBRequest;
import org.onap.aai.rest.db.HttpEntry;
import org.onap.aai.rest.exceptions.AAIInvalidXMLNamespace;
import org.onap.aai.rest.util.ValidateEncoding;
import org.onap.aai.restcore.HttpMethod;
import org.onap.aai.serialization.engines.TransactionalGraphEngine;
import org.onap.aai.setup.SchemaVersion;
import org.springframework.stereotype.Service;

@Service
public class ResourcesService {

  /**
   * This method exists as a workaround for filtering out undesired query params
   * while routing between REST consumers
   */
  public Response getLegacy(String versionParam, String uri, String depthParam, String cleanUp,
      HttpHeaders headers, UriInfo info, HttpServletRequest req, Set<String> removeQueryParams,
      Pageable pageable, Set<String> roles) {
    String sourceOfTruth = headers.getRequestHeaders().getFirst("X-FromAppId");
    String transId = headers.getRequestHeaders().getFirst("X-TransactionId");
    Response response;
    TransactionalGraphEngine dbEngine = null;
    Loader loader;

    try {
      validateRequest(info);
      SchemaVersion version = new SchemaVersion(versionParam);

      final HttpEntry traversalUriHttpEntry = SpringContextAware.getBean("traversalUriHttpEntry", HttpEntry.class);
      String serverBase = req.getRequestURL().toString().replaceAll("/(v[0-9]+|latest)/.*", "/");
      traversalUriHttpEntry.setHttpEntryProperties(version, serverBase);
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
      DBRequest request = new DBRequest.Builder(HttpMethod.GET, uriObject, uriQuery, obj, headers, info, transId)
          .build();
      List<DBRequest> requests = Collections.singletonList(request);

      Pair<Boolean, List<Pair<URI, Response>>> responsesTuple = null;
      if (hasValidPaginationParams(pageable)) {
        responsesTuple = traversalUriHttpEntry.process(requests, sourceOfTruth, roles, true,
            new QueryOptions(pageable));
      } else {
        responsesTuple = traversalUriHttpEntry.process(requests, sourceOfTruth, roles);
      }

      response = responsesTuple.getValue1().get(0).getValue1();

    } catch (AAIException e) {
      response = consumerExceptionResponseGenerator(headers, info, HttpMethod.GET, e);
    } catch (Exception e) {
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

  public Response updateRelationship(String content, String versionParam, String uri, HttpHeaders headers,
      UriInfo info) {
    String sourceOfTruth = headers.getRequestHeaders().getFirst("X-FromAppId");
    String transId = headers.getRequestHeaders().getFirst("X-TransactionId");
    MediaType inputMediaType = headers.getMediaType();
    Response response;
    Loader loader;
    TransactionalGraphEngine dbEngine = null;
    boolean success = true;

    try {
      validateRequest(info);
      SchemaVersion version = new SchemaVersion(versionParam);

      HttpEntry traversalUriHttpEntry = SpringContextAware.getBean("traversalUriHttpEntry", HttpEntry.class);
      traversalUriHttpEntry.setHttpEntryProperties(version);
      loader = traversalUriHttpEntry.getLoader();
      dbEngine = traversalUriHttpEntry.getDbEngine();

      URI uriObject = UriBuilder.fromPath(uri).build();
      validateURI(uriObject);

      QueryParser uriQuery = dbEngine.getQueryBuilder().createQueryFromURI(uriObject);

      Introspector wrappedEntity = loader.unmarshal("relationship", content,
          org.onap.aai.restcore.MediaType.getEnum(this.getInputMediaType(inputMediaType)));

      DBRequest request = new DBRequest.Builder(HttpMethod.PUT_EDGE, uriObject, uriQuery, wrappedEntity, headers,
          info, transId).build();
      List<DBRequest> requests = new ArrayList<>();
      requests.add(request);
      Pair<Boolean, List<Pair<URI, Response>>> responsesTuple = traversalUriHttpEntry.process(requests, sourceOfTruth);

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

  public Response handleWrites(MediaType mediaType, HttpMethod method, String content, String versionParam,
      String uri, HttpHeaders headers, UriInfo info, Set<String> roles) {

    Response response;
    TransactionalGraphEngine dbEngine = null;
    Loader loader;
    SchemaVersion version;
    String sourceOfTruth = headers.getRequestHeaders().getFirst("X-FromAppId");
    String transId = headers.getRequestHeaders().getFirst("X-TransactionId");
    boolean success = true;

    try {
      validateRequest(info);

      version = new SchemaVersion(versionParam);

      HttpEntry traversalUriHttpEntry = SpringContextAware.getBean("traversalUriHttpEntry", HttpEntry.class);
      traversalUriHttpEntry.setHttpEntryProperties(version);
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
      Introspector obj = loader.unmarshal(objName, content,
          org.onap.aai.restcore.MediaType.getEnum(this.getInputMediaType(mediaType)));
      if (obj == null) {
        throw new AAIException("AAI_3000", "object could not be unmarshalled:" + content);
      }

      if (mediaType.toString().contains(MediaType.APPLICATION_XML) && !content.equals("<empty/>")
          && isEmptyObject(obj)) {
        throw new AAIInvalidXMLNamespace(content);
      }

      validateIntrospector(obj, loader, uriObject, method);

      DBRequest request = new DBRequest.Builder(method, uriObject, uriQuery, obj, headers, info, transId)
          .rawRequestContent(content).build();
      List<DBRequest> requests = new ArrayList<>();
      requests.add(request);
      Pair<Boolean, List<Pair<URI, Response>>> responsesTuple = traversalUriHttpEntry.process(requests, sourceOfTruth,
          roles);

      response = responsesTuple.getValue1().get(0).getValue1();
      success = responsesTuple.getValue0();
    } catch (AAIException e) {
      response = consumerExceptionResponseGenerator(headers, info, method, e);
      success = false;
    } catch (Exception e) {
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

  public Response deleteRelationship(String content, String versionParam, String uri, HttpHeaders headers,
      UriInfo info) {
    MediaType inputMediaType = headers.getMediaType();
    String sourceOfTruth = headers.getRequestHeaders().getFirst("X-FromAppId");
    String transId = headers.getRequestHeaders().getFirst("X-TransactionId");
    Loader loader;
    TransactionalGraphEngine dbEngine = null;
    Response response;

    boolean success = true;

    try {
      this.validateRequest(info);
      SchemaVersion version = new SchemaVersion(versionParam);

      HttpEntry traversalUriHttpEntry = SpringContextAware.getBean("traversalUriHttpEntry", HttpEntry.class);
      traversalUriHttpEntry.setHttpEntryProperties(version);
      loader = traversalUriHttpEntry.getLoader();
      dbEngine = traversalUriHttpEntry.getDbEngine();

      if (content.equals("")) {
        throw new AAIException("AAI_3102", "You must supply a relationship");
      }
      URI uriObject = UriBuilder.fromPath(uri).build();
      this.validateURI(uriObject);

      QueryParser uriQuery = dbEngine.getQueryBuilder().createQueryFromURI(uriObject);

      Introspector wrappedEntity = loader.unmarshal("relationship", content,
          org.onap.aai.restcore.MediaType.getEnum(this.getInputMediaType(inputMediaType)));

      DBRequest request = new DBRequest.Builder(HttpMethod.DELETE_EDGE, uriObject, uriQuery, wrappedEntity,
          headers, info, transId).build();
      List<DBRequest> requests = new ArrayList<>();
      requests.add(request);
      Pair<Boolean, List<Pair<URI, Response>>> responsesTuple = traversalUriHttpEntry.process(requests, sourceOfTruth);

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

  public Response getRelationshipList(String versionParam, HttpServletRequest req, String uri, String cleanUp,
      HttpHeaders headers, UriInfo info, Pageable pageable) {
    String sourceOfTruth = headers.getRequestHeaders().getFirst("X-FromAppId");
    String transId = headers.getRequestHeaders().getFirst("X-TransactionId");
    Response response = null;
    TransactionalGraphEngine dbEngine = null;
    Loader loader = null;

    try {
      validateRequest(info);
      SchemaVersion version = new SchemaVersion(versionParam);

      final HttpEntry traversalUriHttpEntry = SpringContextAware.getBean("traversalUriHttpEntry", HttpEntry.class);
      String serverBase = req.getRequestURL().toString().replaceAll("/(v[0-9]+|latest)/.*", "/");
      traversalUriHttpEntry.setHttpEntryProperties(version, serverBase);
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
      DBRequest request = new DBRequest.Builder(HttpMethod.GET_RELATIONSHIP, uriObject, uriQuery, obj, headers, info,
          transId)
          .build();
      List<DBRequest> requests = new ArrayList<>();
      requests.add(request);

      Pair<Boolean, List<Pair<URI, Response>>> responsesTuple = null;
      if (hasValidPaginationParams(pageable)) {
        responsesTuple = traversalUriHttpEntry.process(requests, sourceOfTruth, Collections.emptySet(), true,
            new QueryOptions(pageable));
      } else {
        responsesTuple = traversalUriHttpEntry.process(requests, sourceOfTruth);
      }
      response = responsesTuple.getValue1().get(0).getValue1();
    } catch (AAIException e) {
      response = consumerExceptionResponseGenerator(headers, info, HttpMethod.GET_RELATIONSHIP, e);
    } catch (Exception e) {
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

  public Response delete(String versionParam, String uri, HttpHeaders headers, UriInfo info,
      HttpServletRequest req, Set<String> roles) {

    String outputMediaType = getMediaType(headers.getAcceptableMediaTypes());
    String sourceOfTruth = headers.getRequestHeaders().getFirst("X-FromAppId");
    String transId = headers.getRequestHeaders().getFirst("X-TransactionId");

    TransactionalGraphEngine dbEngine = null;
    Response response;

    boolean success = true;

    try {

      validateRequest(info);
      SchemaVersion version = new SchemaVersion(versionParam);

      HttpEntry traversalUriHttpEntry = SpringContextAware.getBean("traversalUriHttpEntry", HttpEntry.class);
      traversalUriHttpEntry.setHttpEntryProperties(version);
      dbEngine = traversalUriHttpEntry.getDbEngine();
      Loader loader = traversalUriHttpEntry.getLoader();

      URI uriObject = UriBuilder.fromPath(uri).build();

      QueryParser uriQuery = dbEngine.getQueryBuilder().createQueryFromURI(uriObject);
      String objType = uriQuery.getResultType();
      Introspector obj = loader.introspectorFromName(objType);

      DBRequest request = new DBRequest.Builder(HttpMethod.DELETE, uriObject, uriQuery, obj, headers, info, transId)
          .build();
      List<DBRequest> requests = new ArrayList<>();
      requests.add(request);
      Pair<Boolean, List<Pair<URI, Response>>> responsesTuple = traversalUriHttpEntry.process(requests, sourceOfTruth,
          roles);

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

  private String getMediaType(List<MediaType> mediaTypeList) {
    String mediaType = "application/json";
    Iterator<MediaType> mediaTypes = mediaTypeList.iterator();

    while(mediaTypes.hasNext()) {
       if (MediaType.APPLICATION_XML_TYPE.isCompatible(mediaTypes.next())) {
          mediaType = "application/xml";
       }
    }

    return mediaType;
 }

  private boolean hasValidPaginationParams(Pageable pageable) {
    return pageable.getPage() >= 0 && pageable.getPageSize() > 0;
  }

  private MultivaluedMap<String, String> removeNonFilterableParams(MultivaluedMap<String, String> params) {

    String[] toRemove = { "depth", "cleanup", "nodes-only", "format", "resultIndex", "resultSize", "includeTotalCount",
        "skip-related-to" };
    Set<String> toRemoveSet = Arrays.stream(toRemove).collect(Collectors.toSet());

    MultivaluedMap<String, String> cleanedParams = new MultivaluedHashMap<>();
    params.keySet().forEach(k -> {
      if (!toRemoveSet.contains(k)) {
        cleanedParams.addAll(k, params.get(k));
      }
    });

    return cleanedParams;
  }

  private void validateURI(URI uri) throws AAIException {
    if (hasRelatedTo(uri)) {
      throw new AAIException("AAI_3010");
    }
  }

  private boolean hasRelatedTo(URI uri) {

    return uri.toString().contains("/" + RestTokens.COUSIN + "/");
  }

  private void validateRequest(UriInfo info) throws AAIException, UnsupportedEncodingException {

    if (!ValidateEncoding.getInstance().validate(info)) {
      throw new AAIException("AAI_3008", "uri=" + getPath(info));
    }
  }

  private void validateIntrospector(Introspector obj, Loader loader, URI uri, HttpMethod method)
      throws AAIException, UnsupportedEncodingException {
    int maximumDepth = AAIProperties.MAXIMUM_DEPTH;
    boolean validateRequired = true;
    if (method.equals(HttpMethod.MERGE_PATCH)) {
      validateRequired = false;
      maximumDepth = 0;
    }

    IntrospectorValidator validator = (new IntrospectorValidator.Builder()).validateRequired(validateRequired)
        .restrictDepth(maximumDepth).addResolver(new RemoveNonVisibleProperty()).addResolver(new CreateUUID())
        .addResolver(new DefaultFields()).addResolver(new InjectKeysFromURI(loader, uri)).build();
    boolean result = validator.validate(obj);
    if (!result) {
      result = validator.resolveIssues();
    }

    String errors;
    if (!result) {
      List<String> messages = new ArrayList<>();
      Iterator<Issue> issues = validator.getIssues().iterator();

      while (issues.hasNext()) {
        Issue issue = (Issue) issues.next();
        if (!issue.isResolved()) {
          messages.add(issue.getDetail());
        }
      }

      errors = String.join(",", messages);
      throw new AAIException("AAI_3000", errors);
    } else {
      String objURI = obj.getURI();
      errors = "/" + uri.getRawPath();
      if (!errors.endsWith(objURI)) {
        throw new AAIException("AAI_3000", "uri and payload keys don't match");
      }
    }
  }

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
    String queryParams = String.join("&", parmList);
    if (!map.isEmpty()) {
      path += params + queryParams;
    }

    return path;
  }

  protected String getInputMediaType(MediaType mediaType) {
    String type = mediaType.getType();
    return type + "/" + mediaType.getSubtype();
  }

  private Response consumerExceptionResponseGenerator(HttpHeaders headers, UriInfo info, HttpMethod templateAction,
      AAIException e) {
    ArrayList<String> templateVars = new ArrayList<>();
    templateVars.add(templateAction.toString());
    templateVars.add(info.getPath());
    templateVars.addAll(e.getTemplateVars());
    ErrorLogHelper.logException(e);
    return Response.status(e.getErrorObject().getHTTPResponseCode())
        .entity(ErrorLogHelper.getRESTAPIErrorResponseWithLogging(headers.getAcceptableMediaTypes(), e, templateVars))
        .build();
  }

  protected boolean isEmptyObject(Introspector obj) {
    return "{}".equals(obj.marshal(false));
  }

}

/**
 * ============LICENSE_START=======================================================
 * org.onap.aai
 * ================================================================================
 * Copyright © 2017-2018 AT&T Intellectual Property. All rights reserved.
 * Modifications Copyright © 2024 Deutsche Telekom.
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

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.security.Principal;
import java.util.*;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.lang3.ObjectUtils;
import org.javatuples.Pair;
import org.keycloak.adapters.springsecurity.account.SimpleKeycloakAccount;
import org.keycloak.adapters.springsecurity.token.KeycloakAuthenticationToken;
import org.onap.aai.concurrent.AaiCallable;
import org.onap.aai.config.SpringContextAware;
import org.onap.aai.exceptions.AAIException;
import org.onap.aai.introspection.Introspector;
import org.onap.aai.introspection.Loader;
import org.onap.aai.introspection.sideeffect.OwnerCheck;
import org.onap.aai.query.builder.Pageable;
import org.onap.aai.parsers.query.QueryParser;
import org.onap.aai.query.builder.QueryOptions;
import org.onap.aai.rest.db.DBRequest;
import org.onap.aai.rest.db.HttpEntry;
import org.onap.aai.rest.exceptions.AAIInvalidXMLNamespace;
import org.onap.aai.rest.util.ValidateEncoding;
import org.onap.aai.restcore.HttpMethod;
import org.onap.aai.restcore.RESTAPI;
import org.onap.aai.serialization.engines.TransactionalGraphEngine;
import org.onap.aai.setup.SchemaVersion;
import org.onap.aai.util.AAIConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;

/**
 * The Class LegacyMoxyConsumer.
 */
@Controller
@Path("{version: v[1-9][0-9]*|latest}")
@Timed
public class LegacyMoxyConsumer extends RESTAPI {

    private static final Logger logger = LoggerFactory.getLogger(LegacyMoxyConsumer.class.getName());

    @PUT
    @Path("/{uri: .+}")
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public Response update(String content, @PathParam("version") String versionParam,
            @PathParam("uri") @Encoded String uri, @Context HttpHeaders headers, @Context UriInfo info,
            @Context HttpServletRequest req) {
        Set<String> roles = getRoles(req.getUserPrincipal(), req.getMethod());
        MediaType mediaType = headers.getMediaType();
        return this.handleWrites(mediaType, HttpMethod.PUT, content, versionParam, uri, headers, info, roles);
    }

    @PUT
    @Path("/{uri: .+}/relationship-list/relationship")
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public Response updateRelationship(String content, @PathParam("version") String versionParam,
            @PathParam("uri") @Encoded String uri, @Context HttpHeaders headers, @Context UriInfo info,
            @Context HttpServletRequest req) {

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
            this.validateURI(uriObject);

            QueryParser uriQuery = dbEngine.getQueryBuilder().createQueryFromURI(uriObject);

            Introspector wrappedEntity = loader.unmarshal("relationship", content,
                    org.onap.aai.restcore.MediaType.getEnum(this.getInputMediaType(inputMediaType)));

            DBRequest request = new DBRequest.Builder(HttpMethod.PUT_EDGE, uriObject, uriQuery, wrappedEntity, headers,
                    info, transId).build();
            List<DBRequest> requests = new ArrayList<>();
            requests.add(request);
            Pair<Boolean, List<Pair<URI, Response>>> responsesTuple =
                    traversalUriHttpEntry.process(requests, sourceOfTruth);

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

    @PATCH
    @Path("/{uri: .+}")
    @Consumes({"application/merge-patch+json"})
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public Response patch(String content, @PathParam("version") String versionParam,
            @PathParam("uri") @Encoded String uri, @Context HttpHeaders headers, @Context UriInfo info,
            @Context HttpServletRequest req) {
        Set<String> roles = getRoles(req.getUserPrincipal(), req.getMethod());
        MediaType mediaType = MediaType.APPLICATION_JSON_TYPE;
        return this.handleWrites(mediaType, HttpMethod.MERGE_PATCH, content, versionParam, uri, headers, info, roles);

    }

    /**
     * Only PUT, DELETE and OPTIONS methods are allowed for /relationship-list/relationship endpoints
     * This prevents the GET Path matching for "/{uri: .+}" to match for paths ending with /relationship-list/relationship
     * The METHOD_NOT_ALLOWED code will be mapped to a BadRequest in the InvalidResponseStatus interceptor
     */
    @GET
    @Path("/{uri: .+}/relationship-list/relationship")
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public Response disallowGetOnRelationshipListRelationship() {
        return Response
            .status(Status.METHOD_NOT_ALLOWED)
            .allow("PUT","DELETE","OPTIONS")
            .build();
    }

    @GET
    @Path("/{uri: .+}")
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public Response getLegacy(
            @PathParam("version") String versionParam,
            @PathParam("uri") @Encoded String uri,
            @DefaultValue("-1") @QueryParam("resultIndex") int resultIndex,
            @DefaultValue("-1") @QueryParam("resultSize") int resultSize,
            @DefaultValue("true") @QueryParam("includeTotalCount") boolean includeTotalCount,
            // @DefaultValue("false") @QueryParam("sort") Sort sort,
            @DefaultValue("all") @QueryParam("depth") String depthParam,
            @DefaultValue("false") @QueryParam("cleanup") String cleanUp,
            @Context HttpHeaders headers,
            @Context UriInfo info,
            @Context HttpServletRequest req) {
        Set<String> roles = getRoles(req.getUserPrincipal(), req.getMethod());
        Pageable pageable = includeTotalCount == false
            ? new Pageable(resultIndex -1, resultSize)
            : new Pageable(resultIndex -1, resultSize).includeTotalCount();

        return runner(AAIConstants.AAI_CRUD_TIMEOUT_ENABLED, AAIConstants.AAI_CRUD_TIMEOUT_APP,
                AAIConstants.AAI_CRUD_TIMEOUT_LIMIT, headers, info, HttpMethod.GET, new AaiCallable<Response>() {
                    @Override
                    public Response process() {
                        return getLegacy(versionParam, uri, depthParam, cleanUp, headers, info, req,
                                new HashSet<String>(), pageable, roles);
                    }
                });
    }

    /**
     * This method exists as a workaround for filtering out undesired query params while routing between REST consumers
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

            final HttpEntry traversalUriHttpEntry =
                    SpringContextAware.getBean("traversalUriHttpEntry", HttpEntry.class);
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
            DBRequest request =
                    new DBRequest.Builder(HttpMethod.GET, uriObject, uriQuery, obj, headers, info, transId).build();
            List<DBRequest> requests = Collections.singletonList(request);

            Pair<Boolean, List<Pair<URI, Response>>> responsesTuple = null;
            if (hasValidPaginationParams(pageable)) {
                responsesTuple = traversalUriHttpEntry.process(requests, sourceOfTruth, roles, true, new QueryOptions(pageable));
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

    private boolean hasValidPaginationParams(Pageable pageable) {
        return pageable.getPage() >= 0 && pageable.getPageSize() > 0;
    }

    private MultivaluedMap<String, String> removeNonFilterableParams(MultivaluedMap<String, String> params) {

        String[] toRemove =
                {"depth", "cleanup", "nodes-only", "format", "resultIndex", "resultSize", "includeTotalCount", "skip-related-to"};
        Set<String> toRemoveSet = Arrays.stream(toRemove).collect(Collectors.toSet());

        MultivaluedMap<String, String> cleanedParams = new MultivaluedHashMap<>();
        params.keySet().forEach(k -> {
            if (!toRemoveSet.contains(k)) {
                cleanedParams.addAll(k, params.get(k));
            }
        });

        return cleanedParams;
    }

    @DELETE
    @Path("/{uri: .+}")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public Response delete(@PathParam("version") String versionParam, @PathParam("uri") @Encoded String uri,
            @Context HttpHeaders headers, @Context UriInfo info, @QueryParam("resource-version") String resourceVersion,
            @Context HttpServletRequest req) {

        Set<String> roles = getRoles(req.getUserPrincipal(), req.getMethod());
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

            DBRequest request =
                    new DBRequest.Builder(HttpMethod.DELETE, uriObject, uriQuery, obj, headers, info, transId).build();
            List<DBRequest> requests = new ArrayList<>();
            requests.add(request);
            Pair<Boolean, List<Pair<URI, Response>>> responsesTuple =
                    traversalUriHttpEntry.process(requests, sourceOfTruth, roles);

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
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public Response deleteRelationship(String content, @PathParam("version") String versionParam,
            @PathParam("uri") @Encoded String uri, @Context HttpHeaders headers, @Context UriInfo info,
            @Context HttpServletRequest req) {

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
            Pair<Boolean, List<Pair<URI, Response>>> responsesTuple =
                    traversalUriHttpEntry.process(requests, sourceOfTruth);

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
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public Response getRelationshipList(
            @PathParam("version") String versionParam,
            @PathParam("uri") @Encoded String uri,
            @DefaultValue("-1") @QueryParam("resultIndex") int resultIndex,
            @DefaultValue("-1") @QueryParam("resultSize") int resultSize,
            @DefaultValue("true") @QueryParam("includeTotalCount") boolean includeTotalCount,
            @DefaultValue("false") @QueryParam("cleanup") String cleanUp,
            @Context HttpHeaders headers,
            @Context HttpServletRequest req,
            @Context UriInfo info) {
        Pageable pageable = includeTotalCount == false
            ? new Pageable(resultIndex -1, resultSize)
            : new Pageable(resultIndex -1, resultSize).includeTotalCount();
        return runner(AAIConstants.AAI_CRUD_TIMEOUT_ENABLED, AAIConstants.AAI_CRUD_TIMEOUT_APP,
                AAIConstants.AAI_CRUD_TIMEOUT_LIMIT, headers, info, HttpMethod.GET, new AaiCallable<Response>() {
                    @Override
                    public Response process() {
                        return getRelationshipList(versionParam, req, uri, cleanUp, headers, info, pageable);
                    }
                });
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

            final HttpEntry traversalUriHttpEntry =
                    SpringContextAware.getBean("traversalUriHttpEntry", HttpEntry.class);
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
            DBRequest request =
                    new DBRequest.Builder(HttpMethod.GET_RELATIONSHIP, uriObject, uriQuery, obj, headers, info, transId)
                            .build();
            List<DBRequest> requests = new ArrayList<>();
            requests.add(request);

            Pair<Boolean, List<Pair<URI, Response>>> responsesTuple = null;
            if (hasValidPaginationParams(pageable)) {
                responsesTuple = traversalUriHttpEntry.process(requests, sourceOfTruth, Collections.emptySet(), true, new QueryOptions(pageable));
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

    private void validateRequest(UriInfo info) throws AAIException, UnsupportedEncodingException {

        if (!ValidateEncoding.getInstance().validate(info)) {
            throw new AAIException("AAI_3008", "uri=" + getPath(info));
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

    private Response handleWrites(MediaType mediaType, HttpMethod method, String content, String versionParam,
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

            this.validateIntrospector(obj, loader, uriObject, method);

            DBRequest request = new DBRequest.Builder(method, uriObject, uriQuery, obj, headers, info, transId)
                    .rawRequestContent(content).build();
            List<DBRequest> requests = new ArrayList<>();
            requests.add(request);
            Pair<Boolean, List<Pair<URI, Response>>> responsesTuple =
                    traversalUriHttpEntry.process(requests, sourceOfTruth, roles);

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

    private Set<String> getRoles(Principal userPrincipal, String method) {
        KeycloakAuthenticationToken token = (KeycloakAuthenticationToken) userPrincipal;
        if (ObjectUtils.isEmpty(token)) {
            return Collections.EMPTY_SET;
        }
        SimpleKeycloakAccount account = (SimpleKeycloakAccount) token.getDetails();
        if (ObjectUtils.isEmpty(account)) {
            return Collections.EMPTY_SET;
        }
        // When the request is not a GET, we need to exclude ReadOnly access roles
        if (isNotGetRequest(method)) {
            return getExcludedReadOnlyAccessRoles(account);
        }
        return account.getRoles();
    }

    private Set<String> getExcludedReadOnlyAccessRoles(SimpleKeycloakAccount account) {
        return account.getRoles().stream().filter(role -> !role.endsWith(OwnerCheck.READ_ONLY_SUFFIX))
                .collect(Collectors.toSet());
    }

    private boolean isNotGetRequest(String method) {
        return !Action.GET.name().equalsIgnoreCase(method);
    }
}

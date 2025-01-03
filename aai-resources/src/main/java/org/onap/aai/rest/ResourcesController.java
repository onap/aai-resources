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
import lombok.RequiredArgsConstructor;

import java.security.Principal;
import java.util.*;
import java.util.stream.Collectors;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import jakarta.ws.rs.core.Response.Status;

import org.apache.commons.lang3.ObjectUtils;
import org.onap.aai.concurrent.AaiCallable;
import org.onap.aai.introspection.Introspector;
import org.onap.aai.introspection.sideeffect.OwnerCheck;
import org.onap.aai.query.builder.Pageable;
import org.onap.aai.restcore.HttpMethod;
import org.onap.aai.restcore.RESTAPI;
import org.onap.aai.service.ResourcesService;
import org.onap.aai.util.AAIConstants;
import org.springframework.stereotype.Controller;

@Timed
@Controller
@RequiredArgsConstructor
@Path("{version: v[1-9][0-9]*|latest}")
public class ResourcesController extends RESTAPI {

    private final ResourcesService resourcesService;

    @PUT
    @Path("/{uri: .+}")
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public Response update(
            String content,
            @PathParam("version") String versionParam,
            @PathParam("uri") @Encoded String uri,
            @Context HttpHeaders headers,
            @Context UriInfo info,
            @Context HttpServletRequest req) {
        MediaType mediaType = headers.getMediaType();
        return resourcesService.handleWrites(mediaType, HttpMethod.PUT, content, versionParam, uri, headers, info);
    }

    @PUT
    @Path("/{uri: .+}/relationship-list/relationship")
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public Response updateRelationship(
            String content,
            @PathParam("version") String versionParam,
            @PathParam("uri") @Encoded String uri,
            @Context HttpHeaders headers,
            @Context UriInfo info,
            @Context HttpServletRequest req) {

        return resourcesService.updateRelationship(content, versionParam, uri, headers, info);
    }

    @PATCH
    @Path("/{uri: .+}")
    @Consumes({"application/merge-patch+json"})
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public Response patch(
            String content,
            @PathParam("version") String versionParam,
            @PathParam("uri") @Encoded String uri,
            @Context HttpHeaders headers,
            @Context UriInfo info,
            @Context HttpServletRequest req) {
        MediaType mediaType = MediaType.APPLICATION_JSON_TYPE;
        return resourcesService.handleWrites(mediaType, HttpMethod.MERGE_PATCH, content, versionParam, uri, headers, info);

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
        Pageable pageable = includeTotalCount == false
            ? new Pageable(resultIndex -1, resultSize)
            : new Pageable(resultIndex -1, resultSize).includeTotalCount();

        return runner(AAIConstants.AAI_CRUD_TIMEOUT_ENABLED, AAIConstants.AAI_CRUD_TIMEOUT_APP,
                AAIConstants.AAI_CRUD_TIMEOUT_LIMIT, headers, info, HttpMethod.GET, new AaiCallable<Response>() {
                    @Override
                    public Response process() {
                        return resourcesService.getLegacy(versionParam, uri, depthParam, cleanUp, headers, info, req,
                                new HashSet<String>(), pageable);
                    }
                });
    }

    @DELETE
    @Path("/{uri: .+}")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public Response delete(
            @PathParam("version") String versionParam,
            @PathParam("uri") @Encoded String uri,
            @Context HttpHeaders headers,
            @Context UriInfo info,
            @QueryParam("resource-version") String resourceVersion,
            @Context HttpServletRequest req) {
        return resourcesService.delete(versionParam, uri, headers, info, req);
    }

    /**
     * This whole method does nothing because the body is being dropped while fielding the request.
     */
    @DELETE
    @Path("/{uri: .+}/relationship-list/relationship")
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public Response deleteRelationship(
            String content,
            @PathParam("version") String versionParam,
            @PathParam("uri") @Encoded String uri,
            @Context HttpHeaders headers,
            @Context UriInfo info,
            @Context HttpServletRequest req) {

        return resourcesService.deleteRelationship(content, versionParam, uri, headers, info);
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
                        return resourcesService.getRelationshipList(versionParam, req, uri, cleanUp, headers, info, pageable);
                    }
                });
    }

    protected boolean isEmptyObject(Introspector obj) {
        return "{}".equals(obj.marshal(false));
    }
}

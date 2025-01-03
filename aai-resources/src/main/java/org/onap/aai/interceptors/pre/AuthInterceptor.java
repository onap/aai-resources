/**
 * ============LICENSE_START=======================================================
 * org.onap.aai
 * ================================================================================
 * Copyright © 2025 Deutsche Telekom. All rights reserved.
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

package org.onap.aai.interceptors.pre;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import org.onap.aai.ResourcesProfiles;
import org.onap.aai.exceptions.AAIException;
import org.onap.aai.interceptors.AAIContainerFilter;
import org.onap.aai.logging.ErrorLogHelper;
import org.onap.aai.service.AuthorizationService;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import jakarta.annotation.Priority;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;

@Component
@PreMatching
@RequiredArgsConstructor
@Profile(ResourcesProfiles.ONE_WAY_SSL) // this mainly serves the purpose of making the tests work
@Priority(AAIRequestFilterPriority.AUTHORIZATION)
public class AuthInterceptor extends AAIContainerFilter implements ContainerRequestFilter {

  private static final Pattern PATTERN_ECHO = Pattern.compile("^.*/util/echo$");
  private static final Pattern PATTERN_ACTUATOR = Pattern.compile("^.*/actuator/.*$");
  private static final AAIException AAI_EXCEPTION = new AAIException("AAI_3300");
  private final AuthorizationService authorizationService;

  @Override
  public void filter(ContainerRequestContext requestContext) throws IOException {
    String path = requestContext.getUriInfo().getRequestUri().getPath();
    if (PATTERN_ECHO.matcher(path).matches() || PATTERN_ACTUATOR.matcher(path).matches()) {
      return;
    }

    String basicAuth = requestContext.getHeaders().getFirst("Authorization");
    if (basicAuth == null || !basicAuth.startsWith("Basic ")) {
      Response errorResponse = errorResponse("AAI_3300", requestContext.getAcceptableMediaTypes());
      requestContext.abortWith(errorResponse);
      return;
    }

    if (!authorizationService.isAuthorized(basicAuth)) {
      Response errorResponse = errorResponse("AAI_3300", requestContext.getAcceptableMediaTypes());
      requestContext.abortWith(errorResponse);
      return;
    }
  }

  private Response errorResponse(String errorCode, List<MediaType> acceptHeaderValues) {

    return Response
      .status(AAI_EXCEPTION.getErrorObject().getHTTPResponseCode())
      .entity(ErrorLogHelper.getRESTAPIErrorResponse(acceptHeaderValues, AAI_EXCEPTION, new ArrayList<>()))
      .build();
  }

}

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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.onap.aai.exceptions.AAIException;
import org.onap.aai.logging.ErrorLogHelper;
import org.onap.aai.logging.ErrorObject;
import org.onap.aai.setup.SchemaVersion;
import org.onap.aai.setup.SchemaVersions;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class VersionInterceptorTest {

	@Mock
	private ContainerRequestContext requestContext;

	@Mock
	private UriInfo uriInfo;

	@Mock
	private SchemaVersions schemaVersions;

	@Mock
	private AAIException aaiException;

	@Mock
	private ErrorObject errorObject;

	private VersionInterceptor versionInterceptor;

	@BeforeEach
	void setUp() {
		when(requestContext.getUriInfo()).thenReturn(uriInfo);
		when(requestContext.getMethod()).thenReturn("GET");
		when(uriInfo.getPath()).thenReturn("v1/test");
		// Setup allowed versions
		when(schemaVersions.getVersions()).thenReturn(
				new ArrayList<>(Arrays.asList(
						new SchemaVersion("v1"), 
						new SchemaVersion("v2")
						))
				);

		// Setup error object
    	when(errorObject.getHTTPResponseCode()).thenReturn(Response.Status.BAD_REQUEST.BAD_GATEWAY);

		versionInterceptor = new VersionInterceptor(schemaVersions);
	}

	@Test
	@DisplayName("Should create response with AAI_3016")
	void shouldCreateResponseWithAAI3016() {
		// Given
		String errorCode = "AAI_3016";
    	List<MediaType> mediaTypes = Collections.singletonList(MediaType.APPLICATION_JSON_TYPE);
		when(requestContext.getAcceptableMediaTypes()).thenReturn(mediaTypes);

		try (MockedStatic<ErrorLogHelper> mockedHelper = mockStatic(ErrorLogHelper.class)) {
			// Mock static methods
			mockedHelper.when(() -> ErrorLogHelper.getRESTAPIErrorResponse(
					any(), any(AAIException.class), any()))
			.thenReturn("Error response");

			mockedHelper.when(() -> ErrorLogHelper.getErrorObject(errorCode))
			.thenReturn(errorObject);

			// When
			Response response = versionInterceptor.createInvalidVersionResponse(
					errorCode, requestContext, "v1");

			// Then
			assertNotNull(response);
			assertEquals(Response.Status.BAD_GATEWAY.getStatusCode(), response.getStatus());
			assertEquals("Error response", response.getEntity());

		}
	}

}


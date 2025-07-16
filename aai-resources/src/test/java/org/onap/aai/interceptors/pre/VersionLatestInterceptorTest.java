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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.UriInfo;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import org.onap.aai.setup.SchemaVersions;
import org.onap.aai.setup.SchemaVersion;

@ExtendWith(MockitoExtension.class)
class VersionLatestInterceptorTest {

    @Mock
    private ContainerRequestContext requestContext;
    
    @Mock
    private UriInfo uriInfo;
    
    @Mock
    private SchemaVersions schemaVersions;
    
    private VersionLatestInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new VersionLatestInterceptor(schemaVersions);
        when(requestContext.getUriInfo()).thenReturn(uriInfo);
    }

    @Test
    void shouldReplaceLatestWithDefaultVersion() {
        // Given
        String path = "latest/some/resource";
        String absolutePath = "http://example.com/latest/some/resource";
        SchemaVersion defaultVersion = new SchemaVersion("v2");
        String expectedUri = "http://example.com/v2/some/resource";
        
        when(uriInfo.getPath()).thenReturn(path);
        when(uriInfo.getAbsolutePath()).thenReturn(URI.create(absolutePath));
        when(schemaVersions.getDefaultVersion()).thenReturn(defaultVersion);

        // When
        interceptor.filter(requestContext);

        // Then
        verify(requestContext).setRequestUri(URI.create(expectedUri));
    }

    @Test
    void shouldNotModifyUriWhenNotStartingWithLatest() {
        // Given
        String path = "v1/some/resource";
        when(uriInfo.getPath()).thenReturn(path);

        // When
        interceptor.filter(requestContext);

        // Then
        verify(requestContext, never()).setRequestUri(any());
    }
}

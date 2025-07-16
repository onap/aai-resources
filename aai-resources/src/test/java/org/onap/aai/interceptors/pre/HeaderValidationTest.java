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
import org.onap.aai.interceptors.AAIHeaderProperties;
import org.onap.logging.filter.base.Constants;
import org.onap.logging.ref.slf4j.ONAPLogConstants;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HeaderValidationTest {

    @Mock
    private ContainerRequestContext requestContext;

    private HeaderValidation headerValidation;
    private MultivaluedMap<String, String> headers;

    @BeforeEach
    void setUp() {
        headerValidation = new HeaderValidation();
        headers = new MultivaluedHashMap<>();
        when(requestContext.getHeaders()).thenReturn(headers);
    }

   

    @Test
    void testGetRequestId_ClearsExistingHeaders() {
        // Arrange
        String expectedRequestId = "test-request-id";
        headers.put(ONAPLogConstants.Headers.REQUEST_ID, new ArrayList<>());
        headers.put(Constants.HttpHeaders.TRANSACTION_ID, new ArrayList<>());
        headers.put(Constants.HttpHeaders.HEADER_REQUEST_ID, new ArrayList<>());
        headers.put(Constants.HttpHeaders.ECOMP_REQUEST_ID, new ArrayList<>());

        when(requestContext.getHeaderString(ONAPLogConstants.Headers.REQUEST_ID))
            .thenReturn(expectedRequestId);

        // Act
        String actualRequestId = headerValidation.getRequestId(requestContext);

        // Assert
        assertEquals(expectedRequestId, actualRequestId);
        verify(requestContext, atLeastOnce()).getHeaders();
        assertTrue(headers.get(ONAPLogConstants.Headers.REQUEST_ID).isEmpty());
        assertTrue(headers.get(Constants.HttpHeaders.TRANSACTION_ID).contains(expectedRequestId));
        assertTrue(headers.get(Constants.HttpHeaders.HEADER_REQUEST_ID).isEmpty());
        assertTrue(headers.get(Constants.HttpHeaders.ECOMP_REQUEST_ID).isEmpty());
    }
    
   
    @Test
    void testGetRequestId_WhenONAPRequestIdExists() {
        // Arrange
        String expectedRequestId = "onap-123";
        when(requestContext.getHeaderString(ONAPLogConstants.Headers.REQUEST_ID))
            .thenReturn(expectedRequestId);

        // Act
        String result = headerValidation.getRequestId(requestContext);

        // Assert
        assertEquals(expectedRequestId, result);
    }

    @Test
    void testGetRequestId_WhenHeaderRequestIdExists() {
        // Arrange
        String expectedRequestId = "header-123";
        when(requestContext.getHeaderString(ONAPLogConstants.Headers.REQUEST_ID))
            .thenReturn(null);
        when(requestContext.getHeaderString(Constants.HttpHeaders.HEADER_REQUEST_ID))
            .thenReturn(expectedRequestId);

        // Act
        String result = headerValidation.getRequestId(requestContext);

        // Assert
        assertEquals(expectedRequestId, result);
    }

    @Test
    void testGetRequestId_WhenTransactionIdExists() {
        // Arrange
        String expectedRequestId = "trans-123";
        when(requestContext.getHeaderString(ONAPLogConstants.Headers.REQUEST_ID))
            .thenReturn(null);
        when(requestContext.getHeaderString(Constants.HttpHeaders.HEADER_REQUEST_ID))
            .thenReturn(null);
        when(requestContext.getHeaderString(Constants.HttpHeaders.TRANSACTION_ID))
            .thenReturn(expectedRequestId);

        // Act
        String result = headerValidation.getRequestId(requestContext);

        // Assert
        assertEquals(expectedRequestId, result);
    }

    @Test
    void testGetRequestId_WhenEcompRequestIdExists() {
        // Arrange
        String expectedRequestId = "ecomp-123";
        when(requestContext.getHeaderString(ONAPLogConstants.Headers.REQUEST_ID))
            .thenReturn(null);
        when(requestContext.getHeaderString(Constants.HttpHeaders.HEADER_REQUEST_ID))
            .thenReturn(null);
        when(requestContext.getHeaderString(Constants.HttpHeaders.TRANSACTION_ID))
            .thenReturn(null);
        when(requestContext.getHeaderString(Constants.HttpHeaders.ECOMP_REQUEST_ID))
            .thenReturn(expectedRequestId);

        // Act
        String result = headerValidation.getRequestId(requestContext);

        // Assert
        assertEquals(expectedRequestId, result);
    }
   

    @Test
    void whenPartnerNameHasValidComponents_shouldReturnFirstComponent() {
        // Given
        when(requestContext.getHeaderString(AAIHeaderProperties.SOURCE_OF_TRUTH)).thenReturn(null);
        when(requestContext.getHeaderString(ONAPLogConstants.Headers.PARTNER_NAME)).thenReturn("TEST.COMPONENT");

        // When
        String result = headerValidation.getPartnerName(requestContext);

        // Then
        assertEquals("TEST", result);
    }

    @Test
    void whenPartnerNameStartsWithAAI_shouldUseFromAppId() {
        // Given
        when(requestContext.getHeaderString(AAIHeaderProperties.SOURCE_OF_TRUTH)).thenReturn(null);
        when(requestContext.getHeaderString(ONAPLogConstants.Headers.PARTNER_NAME)).thenReturn("AAI.COMPONENT");
        when(requestContext.getHeaderString(AAIHeaderProperties.FROM_APP_ID)).thenReturn("TEST-APP");

        // When
        String result = headerValidation.getPartnerName(requestContext);

        // Then
        assertEquals("TEST-APP", result);
    }

    @Test
    void shouldClearAndUpdateHeaders() {
        // Given
        List<String> oldValues = new ArrayList<>();
        oldValues.add("OLD-VALUE");
        headers.put(ONAPLogConstants.Headers.PARTNER_NAME, oldValues);
        headers.put(AAIHeaderProperties.FROM_APP_ID, oldValues);

        when(requestContext.getHeaderString(AAIHeaderProperties.SOURCE_OF_TRUTH)).thenReturn("NEW-SOT");

        // When
        String result = headerValidation.getPartnerName(requestContext);

        // Then
        assertEquals("NEW-SOT", result);
        assertEquals("NEW-SOT", headers.getFirst(AAIHeaderProperties.FROM_APP_ID));
        
    }


}
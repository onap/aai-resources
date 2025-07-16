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

package org.onap.aai.interceptors;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.UUID;

public class AAIContainerFilterTest {
	
	 // A concrete subclass to test the abstract methods
    private class TestAAIContainerFilter extends AAIContainerFilter {
    }

    private final AAIContainerFilter filter = new TestAAIContainerFilter();

    @Test
    void testGenDateFormat() {
        // Get the generated date string
        String generatedDate = filter.genDate();

        // Validate that the generated date matches the expected format
        assertNotNull(generatedDate, "Generated date should not be null");

        // Check if the generated string matches the format YYMMdd-HH:mm:ss:SSS
        // For example: "241118-14:56:12:456"
        assertTrue(generatedDate.matches("\\d{6}-\\d{2}:\\d{2}:\\d{2}:\\d{3}"), 
            "Generated date should match the format YYMMdd-HH:mm:ss:SSS");
    }

    @Test
    void testValidUUID() {
        // Generate a valid UUID
        String validUUID = UUID.randomUUID().toString();

        // Validate that the UUID is valid
        assertTrue(filter.isValidUUID(validUUID), "Valid UUID should return true");
    }

    @Test
    void testInvalidUUID() {
        // Invalid UUID string (wrong format)
        String invalidUUID = "invalid-uuid-string";

        // Validate that the UUID is invalid
        assertFalse(filter.isValidUUID(invalidUUID), "Invalid UUID should return false");
    }

    @Test
    void testEmptyStringForUUID() {
        // Test an empty string, which should not be a valid UUID
        assertFalse(filter.isValidUUID(""), "Empty string should return false");
    }

    @Test
    void testUUIDWithExtraCharacters() {
        // A valid UUID with extra characters (should be invalid)
        String invalidUUIDWithExtraChars = UUID.randomUUID().toString() + "extra";

        // Validate that the UUID with extra characters is invalid
        assertFalse(filter.isValidUUID(invalidUUIDWithExtraChars), 
            "UUID with extra characters should return false");
    }
}

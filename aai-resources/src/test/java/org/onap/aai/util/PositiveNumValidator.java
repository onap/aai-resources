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
package org.onap.aai.util;

import org.junit.jupiter.api.Test;

import com.beust.jcommander.ParameterException;

import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

class PositiveNumValidatorTest {

    private final PositiveNumValidator validator = new PositiveNumValidator();

    @Test
    @DisplayName("Should accept zero as valid input")
    void validateZero() {
        assertDoesNotThrow(() -> validator.validate("testParam", "0"));
    }

    @Test
    @DisplayName("Should accept positive number as valid input")
    void validatePositiveNumber() {
        assertDoesNotThrow(() -> validator.validate("testParam", "42"));
    }

    @Test
    @DisplayName("Should throw ParameterException for negative number")
    void validateNegativeNumber() {
        ParameterException exception = assertThrows(ParameterException.class,
            () -> validator.validate("testParam", "-1"));
        
        assertEquals("Parameter testParam should be >= 0", exception.getMessage());
    }

    @Test
    @DisplayName("Should throw NumberFormatException for non-numeric input")
    void validateNonNumericInput() {
        assertThrows(NumberFormatException.class,
            () -> validator.validate("testParam", "abc"));
    }
}


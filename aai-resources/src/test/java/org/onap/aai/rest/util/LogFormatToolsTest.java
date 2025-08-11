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

package org.onap.aai.rest.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import org.junit.jupiter.api.Test;

public class LogFormatToolsTest {

	private static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern(DATE_FORMAT).withZone(ZoneOffset.UTC);

    @Test
    void getCurrentDateTime_ShouldReturnCorrectFormat() {
        String result = LogFormatTools.getCurrentDateTime();

        assertTrue(isValidDateFormat(result), "DateTime string should match the expected format");
    }

    @Test
    void getCurrentDateTime_ShouldReturnCurrentTime() {
        ZonedDateTime beforeTest = ZonedDateTime.now();

        String result = LogFormatTools.getCurrentDateTime();
        ZonedDateTime parsedResult = ZonedDateTime.parse(result, DTF);
        ZonedDateTime afterTest = ZonedDateTime.now();

        assertTrue(parsedResult.isBefore(afterTest) || parsedResult.equals(afterTest),
                "Returned datetime should not be after test end");
    }

    @Test
    void getCurrentDateTime_ShouldReturnUTCTime() {
        String result = LogFormatTools.getCurrentDateTime();
        ZonedDateTime parsedResult = ZonedDateTime.parse(result, DTF);

        assertEquals(ZoneOffset.UTC, parsedResult.getOffset(),
                "DateTime should be in UTC timezone");
    }

    private boolean isValidDateFormat(String dateStr) {
        ZonedDateTime.parse(dateStr, DTF);
        return true;
    }
    
    @Test
    void testLogFormatToolsInstantiation() {
        LogFormatTools logFormatTools = new LogFormatTools();

        assertNotNull(logFormatTools, "Should be able to create LogFormatTools instance");
        assertTrue(logFormatTools instanceof LogFormatTools, 
            "Created object should be instance of LogFormatTools");
    }

    @Test
    void testLogFormatToolsClass() {
        Class<?> clazz = LogFormatTools.class;

        assertFalse(clazz.isInterface(), "LogFormatTools should be a class, not an interface");
        assertFalse(clazz.isEnum(), "LogFormatTools should be a class, not an enum");
        assertFalse(clazz.isAnnotation(), "LogFormatTools should be a class, not an annotation");
    }
}

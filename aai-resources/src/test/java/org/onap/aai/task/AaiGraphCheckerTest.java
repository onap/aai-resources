/*
 * ============LICENSE_START=======================================================
 * Copyright (C) 2022 Bell Canada
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ============LICENSE_END=========================================================
 */

package org.onap.aai.task;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.onap.aai.AAISetup;
import org.onap.aai.tasks.AaiGraphChecker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

@ContextConfiguration(classes = {AaiGraphChecker.class})
@TestPropertySource(properties = "aai.graph.checker.task.enabled=true")
public class AaiGraphCheckerTest extends AAISetup {

    @Autowired
    private AaiGraphChecker subject;

    @Test
    public void testIsAaiGraphDbAvailable_Actual() {
        // Run
        Boolean result = subject.isAaiGraphDbAvailable(AaiGraphChecker.CheckerType.ACTUAL);
        // Verify
        assertNotNull(result);
        assertTrue(result);
    }

    @Test
    public void testIsAaiGraphDbAvailable_CachedAfterClear() {
        // Prepare
        subject.clearDbAvailabilityCachedIndicator();
        // Run
        Boolean result = subject.isAaiGraphDbAvailable(AaiGraphChecker.CheckerType.CACHED);
        // Verify
        assertNull(result);
    }

    @Test
    public void testIsAaiGraphDbAvailable_CachedAfterActual() {
        // Prepare
        subject.clearDbAvailabilityCachedIndicator();
        subject.isAaiGraphDbAvailable(AaiGraphChecker.CheckerType.ACTUAL);
        // Run
        Boolean result = subject.isAaiGraphDbAvailable(AaiGraphChecker.CheckerType.CACHED);
        // Verify
        assertNotNull(result);
        assertTrue(result);
    }

}

/*-
 * ============LICENSE_START=======================================================
 * org.openecomp.aai
 * ================================================================================
 * Copyright (C) 2017 AT&T Intellectual Property. All rights reserved.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ============LICENSE_END=========================================================
 */

package org.openecomp.aai;

import org.apache.commons.io.IOUtils;
import org.junit.BeforeClass;
import org.openecomp.aai.dbmap.AAIGraph;
import org.openecomp.aai.introspection.ModelInjestor;
import org.openecomp.aai.serialization.queryformats.QueryFormatTestHelper;
import org.openecomp.aai.util.AAIConstants;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.assertNotNull;

public abstract class AAISetup {

    @BeforeClass
    public static void setupBundleconfig() throws Exception {
        System.setProperty("AJSC_HOME", ".");
        System.setProperty("BUNDLECONFIG_DIR", "src/test/resources/bundleconfig-local");
        QueryFormatTestHelper.setFinalStatic(AAIConstants.class.getField("AAI_HOME_ETC_OXM"),
                "src/test/resources/bundleconfig-local/etc/oxm/");
    }

    public String getPayload(String filename) throws IOException {

        InputStream inputStream = getClass()
                .getClassLoader()
                .getResourceAsStream(filename);

        String message = String.format("Unable to find the %s in src/test/resources", filename);
        assertNotNull(message, inputStream);

        String resource = IOUtils.toString(inputStream);
        return resource;
    }
}

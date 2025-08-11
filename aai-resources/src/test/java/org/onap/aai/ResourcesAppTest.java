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
package org.onap.aai;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.onap.aai.config.SpringContextAware;
import org.onap.aai.dbmap.AAIGraph;
import org.onap.aai.exceptions.AAIException;
import org.onap.aai.nodes.NodeIngestor;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

@ExtendWith(MockitoExtension.class)
class ResourcesAppTest {

	private ResourcesApp resourcesApp;

	@Mock
	private Environment environment;

	@Mock
	private NodeIngestor nodeIngestor;

	@Mock
	private SpringContextAware context;

	@Mock
	private SpringContextAware loaderFactory;

	@InjectMocks
	private ResourcesApp resourceApp;

	@MockBean
	private ConfigurableApplicationContext applicationContext;


	@Mock
	private Environment env;

	@BeforeEach
	void setUp() {
		resourcesApp = new ResourcesApp();
		System.setProperty("AJSC_HOME", ".");
        System.setProperty("BUNDLECONFIG_DIR", "src/main/resources");
	}

	@Test
	void testSetDefaultProps() {
		// Clear any existing system properties
		System.clearProperty("file.separator");
		System.clearProperty("AJSC_HOME");
		System.clearProperty("BUNDLECONFIG_DIR");

		ResourcesApp.setDefaultProps();

		assertEquals("/", System.getProperty("file.separator"));
		assertEquals(".", System.getProperty("AJSC_HOME"));
		assertNotNull(System.getProperty("BUNDLECONFIG_DIR"));
		assertEquals("ResourcesApp", System.getProperty("aai.service.name"));
	}

	@Test
	void init_ShouldSetupPropertiesAndConfiguration() throws AAIException {
		when(env.acceptsProfiles(any(Profiles.class))).thenReturn(false);

		resourceApp.init();

		assertAll(
				() -> assertEquals("false", System.getProperty("org.onap.aai.serverStarted")),
				() -> assertEquals("true", System.getProperty("org.apache.tomcat.util.buf.UDecoder.ALLOW_ENCODED_SLASH"))
				);
	}
	
	@Test
	void testBundleConfigDirWithAppName() {
		System.clearProperty("BUNDLECONFIG_DIR");
		String originalUserDir = System.getProperty("user.dir");

		System.setProperty("user.dir", originalUserDir + "/aai-resources");
		ResourcesApp.setDefaultProps();
		assertEquals("src/main/resources", System.getProperty("BUNDLECONFIG_DIR"));

		System.setProperty("user.dir", originalUserDir);
	}

	@Test
	void testBundleConfigDirWithoutAppName() {
		// Clear and set specific properties for this test
		System.clearProperty("BUNDLECONFIG_DIR");
		String originalUserDir = System.getProperty("user.dir");

		System.setProperty("user.dir", "/different/path");
		ResourcesApp.setDefaultProps();
		assertEquals("aai-resources/src/main/resources", System.getProperty("BUNDLECONFIG_DIR"));

		System.setProperty("user.dir", originalUserDir);
	}
	
    @Test
    void testCleanup() {
        try (MockedStatic<AAIGraph> aaiGraphMock = mockStatic(AAIGraph.class)) {
            AAIGraph mockGraph = mock(AAIGraph.class);
            aaiGraphMock.when(AAIGraph::getInstance).thenReturn(mockGraph);

           resourcesApp.cleanup();
            verify(mockGraph).graphShutdown();
        }
    }
   
    @Test
    @DisplayName("Should return AAI_3026 exception when NodeIngestor error occurs")
    void testNodeIngestorError() {
        Exception cause = new RuntimeException("Failed to process NodeIngestor data");
        Exception originalException = new Exception("Wrapper", cause);
        
        AAIException result = ResourcesApp.schemaServiceExceptionTranslator(originalException);
        
        assertNotNull(result);
        assertEquals("AAI_3026", result.getCode());
    }

    @Test
    @DisplayName("Should return AAI_3027 exception when EdgeIngestor error occurs")
    void testEdgeIngestorError() {
        Exception cause = new RuntimeException("EdgeIngestor failed to process");
        Exception originalException = new Exception("Wrapper", cause);
        
        AAIException result = ResourcesApp.schemaServiceExceptionTranslator(originalException);
        
        assertNotNull(result);
        assertEquals("AAI_3027", result.getCode());
    }

    @Test
    @DisplayName("Should return AAI_3025 exception when connection refused")
    void testConnectionRefusedError() {
        Exception cause = new RuntimeException("Connection refused");
        Exception originalException = new Exception("Wrapper", cause);
        
        AAIException result = ResourcesApp.schemaServiceExceptionTranslator(originalException);
        
        assertNotNull(result);
        assertEquals("AAI_3025", result.getCode());
    }

    @Test
    @DisplayName("Should return default AAI_3025 exception for unknown errors")
    void testUnknownError() {
        Exception cause = new RuntimeException("Some unexpected error");
        Exception originalException = new Exception("Wrapper", cause);
        
        AAIException result = ResourcesApp.schemaServiceExceptionTranslator(originalException);
        
        assertNotNull(result);
        assertEquals("AAI_3025", result.getCode());
    }
}







/*
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

package org.onap.aai;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.ExtendWith;
import org.onap.aai.config.ConfigConfiguration;
import org.onap.aai.config.IntrospectionConfig;
import org.onap.aai.config.KafkaConfig;
import org.onap.aai.config.RestBeanConfig;
import org.onap.aai.config.SpringContextAware;
import org.onap.aai.config.XmlFormatTransformerConfiguration;
import org.onap.aai.edges.EdgeIngestor;
import org.onap.aai.introspection.LoaderFactory;
import org.onap.aai.introspection.MoxyLoader;
import org.onap.aai.nodes.NodeIngestor;
import org.onap.aai.rest.db.HttpEntry;
import org.onap.aai.rest.notification.DeltaEventsService;
import org.onap.aai.util.delta.DeltaEventsConfig;
import org.onap.aai.rest.notification.NotificationService;
import org.onap.aai.serialization.db.EdgeSerializer;
import org.onap.aai.setup.AAIConfigTranslator;
import org.onap.aai.setup.SchemaVersion;
import org.onap.aai.setup.SchemaVersions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@ExtendWith(SpringExtension.class)
@SpringJUnitConfig(
        classes = {ConfigConfiguration.class, AAIConfigTranslator.class, NodeIngestor.class, EdgeIngestor.class,
                EdgeSerializer.class, SpringContextAware.class, IntrospectionConfig.class,
                XmlFormatTransformerConfiguration.class, RestBeanConfig.class, LoaderFactory.class, NotificationService.class, KafkaConfig.class, DeltaEventsService.class})
@TestPropertySource(
        properties = {"schema.uri.base.path = /aai",
                "schema.ingest.file = src/test/resources/application-test.properties"})
@EnableConfigurationProperties(DeltaEventsConfig.class)
public abstract class AAISetup {

    @Autowired
    protected NodeIngestor nodeIngestor;

    @Autowired
    protected LoaderFactory loaderFactory;

    @Autowired
    protected Map<SchemaVersion, MoxyLoader> moxyLoaderInstance;

    @Autowired
    protected HttpEntry traversalHttpEntry;

    @Autowired
    protected HttpEntry traversalUriHttpEntry;

    @Autowired
    protected SchemaVersions schemaVersions;

    @BeforeAll
    public static void setupBundleconfig() throws Exception {
        System.setProperty("AJSC_HOME", "./");
        System.setProperty("BUNDLECONFIG_DIR", "src/main/resources/");
    }

    public String getPayload(String filename) throws IOException {

        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(filename);

        String message = "Unable to find the %s in src/test/resources".formatted(filename);
        assertNotNull(inputStream, message);

        String resource = IOUtils.toString(inputStream, Charset.defaultCharset());
        return resource;
    }
}

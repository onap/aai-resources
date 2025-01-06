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

package org.onap.aai.rest;

import java.io.UnsupportedEncodingException;
import java.util.Base64;
import java.util.Collections;

import org.apache.tinkerpop.gremlin.process.traversal.P;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.janusgraph.core.JanusGraph;
import org.janusgraph.core.JanusGraphTransaction;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.onap.aai.ResourcesApp;
import org.onap.aai.ResourcesTestConfiguration;
import org.onap.aai.dbmap.AAIGraph;
import org.onap.aai.exceptions.AAIException;
import org.onap.aai.nodes.NodeIngestor;
import org.onap.aai.util.AAIConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.cassandra.CassandraAutoConfiguration;
import org.springframework.boot.autoconfigure.data.cassandra.CassandraDataAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.client.RestTemplate;

@Import(ResourcesTestConfiguration.class)
@TestPropertySource(locations = "classpath:application-test.properties")
@ContextConfiguration
@EnableAutoConfiguration(exclude = {CassandraDataAutoConfiguration.class, CassandraAutoConfiguration.class}) // there is no running cassandra instance for the test
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = ResourcesApp.class)
public abstract class AbstractSpringRestTest {

    @Autowired
    protected RestTemplate restTemplate;

    @Autowired
    protected NodeIngestor nodeIngestor;

    @LocalServerPort
    protected int randomPort;

    protected HttpEntity<String> httpEntity;

    protected String baseUrl;
    protected HttpHeaders headers;

    @BeforeAll
    public static void setupConfig() throws AAIException {
        System.setProperty("AJSC_HOME", "./");
        System.setProperty("BUNDLECONFIG_DIR", "src/main/resources/");
    }

    @BeforeEach
    public void setup() throws AAIException, UnsupportedEncodingException {

        AAIConfig.init();
        AAIGraph.getInstance();

        headers = new HttpHeaders();

        String authorization = Base64.getEncoder().encodeToString("AAI:AAI".getBytes("UTF-8"));

        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("Real-Time", "true");
        headers.add("X-FromAppId", "JUNIT");
        headers.add("X-TransactionId", "JUNIT");

        headers.add("Authorization", "Basic " + authorization);
        httpEntity = new HttpEntity<String>(headers);
        baseUrl = "http://localhost:" + randomPort;
    }

    @AfterEach
    public void tearDown() {

        JanusGraph janusGraph = AAIGraph.getInstance().getGraph();
        JanusGraphTransaction transaction = janusGraph.newTransaction();

        boolean success = true;

        try {
            GraphTraversalSource g = transaction.traversal();
            g.V().has("source-of-truth", P.within("JUNIT", "AAI-EXTENSIONS")).toList().stream()
                    .forEach(v -> v.remove());
        } catch (Exception ex) {
            success = false;
        } finally {
            if (success) {
                transaction.commit();
            } else {
                transaction.rollback();
            }
        }
    }
}

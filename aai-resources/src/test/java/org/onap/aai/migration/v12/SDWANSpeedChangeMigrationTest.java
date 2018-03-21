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
package org.onap.aai.migration.v12;

import org.janusgraph.core.JanusGraphFactory;
import org.janusgraph.core.JanusGraph;
import org.janusgraph.core.JanusGraphTransaction;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onap.aai.AAISetup;
import org.onap.aai.dbmap.DBConnectionType;
import org.onap.aai.introspection.Loader;
import org.onap.aai.introspection.LoaderFactory;
import org.onap.aai.introspection.ModelType;
import org.onap.aai.introspection.Version;
import org.onap.aai.serialization.db.EdgeRules;
import org.onap.aai.serialization.engines.QueryStyle;
import org.onap.aai.serialization.engines.JanusGraphDBEngine;
import org.onap.aai.serialization.engines.TransactionalGraphEngine;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

public class SDWANSpeedChangeMigrationTest extends AAISetup {

    private final static Version version = Version.v12;
    private final static ModelType introspectorFactoryType = ModelType.MOXY;
    private final static QueryStyle queryStyle = QueryStyle.TRAVERSAL;
    private final static DBConnectionType type = DBConnectionType.REALTIME;
    private Loader loader;
    private TransactionalGraphEngine dbEngine;
    private JanusGraph graph;
    private SDWANSpeedChangeMigration migration;
    private EdgeRules rules;
    private GraphTraversalSource g;
    private JanusGraphTransaction tx;
    Vertex pLinkWan1;
    Vertex pLinkWan3;
    Vertex pLinkWan5;
    Vertex pLinkWan7;



    @Before
    public void setUp() throws Exception {
        graph = JanusGraphFactory.build().set("storage.backend", "inmemory").open();
        tx = graph.newTransaction();
        g = tx.traversal();
        loader = LoaderFactory.createLoaderForVersion(introspectorFactoryType, version);
        dbEngine = new JanusGraphDBEngine(
                queryStyle,
                type,
                loader);
        rules = EdgeRules.getInstance();

        Vertex servSub1 = g.addV().property("aai-node-type", "service-subscription")
                .property("service-type", "DHV")
                .next();
        Vertex servinst1 = g.addV().property("aai-node-type", "service-instance")
                .property("service-type", "DHV")
                .next();
        Vertex allotedRsrc1 = g.addV().property("aai-node-type", "allotted-resource")
                .property("id", "rsrc1")
                .next();
        Vertex servinst2 = g.addV().property("aai-node-type", "service-instance")
                .property("service-type", "VVIG")
                .next();
        Vertex servSub2 = g.addV().property("aai-node-type", "service-subscription")
                .property("service-type", "VVIG")
                .next();

        Vertex genericvnf1 = g.addV().property("aai-node-type", "generic-vnf")
                .property("vnf-id", "vnfId1")
                .next();
        Vertex vServer1 = g.addV().property("aai-node-type", "vserver")
                .next();
        Vertex pServer1 = g.addV().property("aai-node-type", "pserver")
                .next();
        Vertex pInterfaceWan1 = g.addV().property("aai-node-type", "p-interface")
                .property("interface-name", "ge-0/0/10")
                .next();
        Vertex tunnelXConnectAll_Wan1 = g.addV().property("aai-node-type", "tunnel-xconnect")
                .property("id", "txc1")
                .property("bandwidth-up-wan1", "300 Mbps")
                .property("bandwidth-down-wan1", "400 Mbps")
                .property("bandwidth-up-wan2", "500 Mbps")
                .property("bandwidth-down-wan2", "600 Mbps")
                .next();

        pLinkWan1 = g.addV().property("aai-node-type", "physical-link")
                .property("link-name", "pLinkWan1")
                .property("service-provider-bandwidth-up-value", "empty")
                .property("service-provider-bandwidth-up-units", "empty")
                .property("service-provider-bandwidth-down-value", "empty")
                .property("service-provider-bandwidth-down-units", "empty")
                .next();
        Vertex servSub3 = g.addV().property("aai-node-type", "service-subscription")
                .property("service-type", "DHV")
                .next();
        Vertex servinst3 = g.addV().property("aai-node-type", "service-instance")
                .property("service-type", "DHV")
                .next();
        Vertex allotedRsrc3 = g.addV().property("aai-node-type", "allotted-resource")
                .property("id", "rsrc1")
                .next();
        Vertex servinst4 = g.addV().property("aai-node-type", "service-instance")
                .property("service-type", "VVIG")
                .next();
        Vertex servSub4 = g.addV().property("aai-node-type", "service-subscription")
                .property("service-type", "VVIG")
                .next();

        Vertex genericvnf3 = g.addV().property("aai-node-type", "generic-vnf")
                .property("vnf-id", "vnfId1")
                .next();
        Vertex vServer3 = g.addV().property("aai-node-type", "vserver")
                .next();
        Vertex pServer3 = g.addV().property("aai-node-type", "pserver")
                .next();
        Vertex pInterfaceWan3 = g.addV().property("aai-node-type", "p-interface")
                .property("interface-name", "ge-0/0/11")
                .next();
        Vertex tunnelXConnectAll_Wan3 = g.addV().property("aai-node-type", "tunnel-xconnect")
                .property("id", "txc3")
                .property("bandwidth-up-wan1", "300 Mbps")
                .property("bandwidth-down-wan1", "400 Mbps")
                .property("bandwidth-up-wan2", "500 Mbps")
                .property("bandwidth-down-wan2", "600 Mbps")
                .next();

        pLinkWan3 = g.addV().property("aai-node-type", "physical-link")
                .property("link-name", "pLinkWan3")
                .property("service-provider-bandwidth-up-value", "empty")
                .property("service-provider-bandwidth-up-units", "empty")
                .property("service-provider-bandwidth-down-value", "empty")
                .property("service-provider-bandwidth-down-units", "empty")
                .next();


        Vertex servSub5 = g.addV().property("aai-node-type", "service-subscription")
                .property("service-type", "DHV")
                .next();
        Vertex servinst5 = g.addV().property("aai-node-type", "service-instance")
                .property("service-type", "DHV")
                .next();
        Vertex allotedRsrc5 = g.addV().property("aai-node-type", "allotted-resource")
                .property("id", "rsrc1")
                .next();
        Vertex servinst6 = g.addV().property("aai-node-type", "service-instance")
                .property("service-type", "VVIG")
                .next();
        Vertex servSub6 = g.addV().property("aai-node-type", "service-subscription")
                .property("service-type", "VVIG")
                .next();

        Vertex genericvnf5 = g.addV().property("aai-node-type", "generic-vnf")
                .property("vnf-id", "vnfId1")
                .next();
        Vertex vServer5 = g.addV().property("aai-node-type", "vserver")
                .next();
        Vertex pServer5 = g.addV().property("aai-node-type", "pserver")
                .next();
        Vertex pInterfaceWan5 = g.addV().property("aai-node-type", "p-interface")
                .property("interface-name", "ge-0/0/10")
                .next();
        Vertex tunnelXConnectAll_Wan5 = g.addV().property("aai-node-type", "tunnel-xconnect")
                .property("id", "txc5")
                .property("bandwidth-up-wan1", "")
                .property("bandwidth-down-wan1", "")
                .property("bandwidth-up-wan2", "500 Mbps")
                .property("bandwidth-down-wan2", "600 Mbps")
                .next();

        pLinkWan5 = g.addV().property("aai-node-type", "physical-link")
                .property("link-name", "pLinkWan5")
                .property("service-provider-bandwidth-up-value", "")
                .property("service-provider-bandwidth-up-units", "")
                .property("service-provider-bandwidth-down-value", "")
                .property("service-provider-bandwidth-down-units", "")
                .next();


        Vertex servSub7 = g.addV().property("aai-node-type", "service-subscription")
                .property("service-type", "DHV")
                .next();
        Vertex servinst7 = g.addV().property("aai-node-type", "service-instance")
                .property("service-type", "DHV")
                .next();
        Vertex allotedRsrc7 = g.addV().property("aai-node-type", "allotted-resource")
                .property("id", "rsrc1")
                .next();
        Vertex servinst9 = g.addV().property("aai-node-type", "service-instance")
                .property("service-type", "VVIG")
                .next();
        Vertex servSub9 = g.addV().property("aai-node-type", "service-subscription")
                .property("service-type", "VVIG")
                .next();

        Vertex genericvnf7 = g.addV().property("aai-node-type", "generic-vnf")
                .property("vnf-id", "vnfId1")
                .next();
        Vertex vServer7 = g.addV().property("aai-node-type", "vserver")
                .next();
        Vertex pServer7 = g.addV().property("aai-node-type", "pserver")
                .next();
        Vertex pInterfaceWan7 = g.addV().property("aai-node-type", "p-interface")
                .property("interface-name", "ge-0/0/11")
                .next();
        Vertex tunnelXConnectAll_Wan7 = g.addV().property("aai-node-type", "tunnel-xconnect")
                .property("id", "txc7")
                .property("bandwidth-up-wan1", "300 Mbps")
                .property("bandwidth-down-wan1", "400 Mbps")
                .property("bandwidth-up-wan2", "")
                .property("bandwidth-down-wan2", "")
                .next();

        pLinkWan7 = g.addV().property("aai-node-type", "physical-link")
                .property("link-name", "pLinkWan5")
                .property("service-provider-bandwidth-up-value", "")
                .property("service-provider-bandwidth-up-units", "")
                .property("service-provider-bandwidth-down-value", "")
                .property("service-provider-bandwidth-down-units", "")
                .next();



        rules.addTreeEdge(g, servSub1, servinst1);
        rules.addEdge(g, servinst1, allotedRsrc1);
        rules.addTreeEdge(g, servinst2, servSub2);
        rules.addTreeEdge(g, allotedRsrc1, servinst2);

        rules.addTreeEdge(g, allotedRsrc1, tunnelXConnectAll_Wan1);


        rules.addEdge(g, servinst1, genericvnf1);
        rules.addEdge(g, genericvnf1, vServer1);
        rules.addEdge(g, vServer1, pServer1);
        rules.addTreeEdge(g, pServer1, pInterfaceWan1);
        rules.addEdge(g, pInterfaceWan1, pLinkWan1);

        rules.addTreeEdge(g, servSub3, servinst3);
        rules.addEdge(g, servinst3, allotedRsrc3);
        rules.addTreeEdge(g, servinst4, servSub4);
        rules.addTreeEdge(g, allotedRsrc3, servinst4);

        rules.addTreeEdge(g, allotedRsrc3, tunnelXConnectAll_Wan3);


        rules.addEdge(g, servinst3, genericvnf3);
        rules.addEdge(g, genericvnf3, vServer3);
        rules.addEdge(g, vServer3, pServer3);
        rules.addTreeEdge(g, pServer3, pInterfaceWan3);
        rules.addEdge(g, pInterfaceWan3, pLinkWan3);


        rules.addTreeEdge(g, servSub5, servinst5);
        rules.addEdge(g, servinst5, allotedRsrc5);
        rules.addTreeEdge(g, servinst6, servSub6);
        rules.addTreeEdge(g, allotedRsrc5, servinst6);

        rules.addTreeEdge(g, allotedRsrc5, tunnelXConnectAll_Wan5);


        rules.addEdge(g, servinst5, genericvnf5);
        rules.addEdge(g, genericvnf5, vServer5);
        rules.addEdge(g, vServer5, pServer5);
        rules.addTreeEdge(g, pServer5, pInterfaceWan5);
        rules.addEdge(g, pInterfaceWan5, pLinkWan5);

        rules.addTreeEdge(g, servSub7, servinst7);
        rules.addEdge(g, servinst7, allotedRsrc7);
        rules.addTreeEdge(g, servinst9, servSub9);
        rules.addTreeEdge(g, allotedRsrc7, servinst9);

        rules.addTreeEdge(g, allotedRsrc7, tunnelXConnectAll_Wan7);


        rules.addEdge(g, servinst7, genericvnf7);
        rules.addEdge(g, genericvnf7, vServer7);
        rules.addEdge(g, vServer7, pServer7);
        rules.addTreeEdge(g, pServer7, pInterfaceWan7);
        rules.addEdge(g, pInterfaceWan7, pLinkWan7);


        TransactionalGraphEngine spy = spy(dbEngine);
        TransactionalGraphEngine.Admin adminSpy = spy(dbEngine.asAdmin());
        GraphTraversalSource traversal = g;
        when(spy.asAdmin()).thenReturn(adminSpy);
        when(adminSpy.getTraversalSource()).thenReturn(traversal);
        migration = new SDWANSpeedChangeMigration(spy);
        migration.run();
    }


    @After
    public void cleanUp() {
        tx.rollback();
        graph.close();
    }


    /***
     * Checks to see if the Wan1 properties were updated in the physical link
     */

    @Test
    public void ConfirmWan1Changes() {

        assertEquals("300", pLinkWan1.property("service-provider-bandwidth-up-value").value().toString());
        assertEquals("Mbps", pLinkWan1.property("service-provider-bandwidth-up-units").value().toString());
        assertEquals("400", pLinkWan1.property("service-provider-bandwidth-down-value").value().toString());
        assertEquals("Mbps", pLinkWan1.property("service-provider-bandwidth-down-units").value().toString());

    }

    /***
     * Checks to see if the Wan2 properties were updated in the physical link
     */
    @Test
    public void ConfirmWan2Changes() {

        assertEquals("500", pLinkWan3.property("service-provider-bandwidth-up-value").value().toString());
        assertEquals("Mbps", pLinkWan3.property("service-provider-bandwidth-up-units").value().toString());
        assertEquals("600", pLinkWan3.property("service-provider-bandwidth-down-value").value().toString());
        assertEquals("Mbps", pLinkWan3.property("service-provider-bandwidth-down-units").value().toString());

    }

    /***
     * if tunnel xconncets missing bandwidth up 1 value the plink should not be updated
     */

    @Test
    public void Wan1EmptyNoChanges() {

        assertEquals("", pLinkWan5.property("service-provider-bandwidth-up-value").value().toString());
        assertEquals("", pLinkWan5.property("service-provider-bandwidth-up-units").value().toString());
        assertEquals("", pLinkWan5.property("service-provider-bandwidth-down-value").value().toString());
        assertEquals("", pLinkWan5.property("service-provider-bandwidth-down-units").value().toString());

    }

    /***
     * if tunnel xconncets missing bandwidth up 2 value the plink should not be updated
     */

    @Test
    public void Wan2EmptyNoChanges() {

        assertEquals("", pLinkWan7.property("service-provider-bandwidth-up-value").value().toString());
        assertEquals("", pLinkWan7.property("service-provider-bandwidth-up-units").value().toString());
        assertEquals("", pLinkWan7.property("service-provider-bandwidth-down-value").value().toString());
        assertEquals("", pLinkWan7.property("service-provider-bandwidth-down-units").value().toString());

    }


}

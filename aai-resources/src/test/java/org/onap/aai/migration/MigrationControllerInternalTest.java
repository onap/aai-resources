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
package org.onap.aai.migration;

import com.att.eelf.configuration.EELFLogger;
import com.att.eelf.configuration.EELFManager;
import org.janusgraph.core.JanusGraphTransaction;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onap.aai.AAISetup;
import org.onap.aai.dbmap.AAIGraph;
import org.onap.aai.exceptions.AAIException;
import org.onap.aai.serialization.db.EdgeRules;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.StringContains.containsString;

public class MigrationControllerInternalTest extends AAISetup {

    private static final EELFLogger logger = EELFManager.getInstance().getLogger(MigrationControllerInternalTest.class);

    private MigrationControllerInternal migrationControllerInternal;

    @Before
    public void setup() throws AAIException {
        migrationControllerInternal = new MigrationControllerInternal();
        clearGraph();
        createGraph();
    }

    private void createGraph(){

        JanusGraphTransaction transaction = AAIGraph.getInstance().getGraph().newTransaction();
        boolean success = true;

        try {
            GraphTraversalSource g = transaction.traversal();

            Vertex servSub1 = g.addV().property("aai-node-type", "service-subscription")
                    .property("service-type", "DHV")
                    .property("source-of-truth", "JUNIT")
                    .next();
            Vertex servinst1 =  g.addV().property( "aai-node-type", "service-instance")
                    .property("service-type", "DHV")
                    .property("source-of-truth", "JUNIT")
                    .next();
            Vertex allotedRsrc1 =  g.addV().property( "aai-node-type", "allotted-resource")
                    .property("id","rsrc1")
                    .property("source-of-truth", "JUNIT")
                    .next();
            Vertex servinst2 =  g.addV().property( "aai-node-type", "service-instance")
                    .property("service-type", "VVIG")
                    .property("source-of-truth", "JUNIT")
                    .next();
            Vertex servSub2 = g.addV().property("aai-node-type", "service-subscription")
                    .property("service-type", "VVIG")
                    .property("source-of-truth", "JUNIT")
                    .next();

            Vertex genericvnf1 = g.addV().property("aai-node-type", "generic-vnf")
                    .property("vnf-id", "vnfId1")
                    .property("source-of-truth", "JUNIT")
                    .next();
            Vertex vServer1 = g.addV().property("aai-node-type", "vserver")
                    .property("source-of-truth", "JUNIT")
                    .next();
            Vertex pServer1 = g.addV().property("aai-node-type", "pserver")
                    .property("source-of-truth", "JUNIT")
                    .next();
            Vertex pInterfaceWan1 = g.addV().property("aai-node-type", "p-interface")
                    .property("interface-name","ge-0/0/10")
                    .property("source-of-truth", "JUNIT")
                    .next();
            Vertex tunnelXConnectAll_Wan1 =  g.addV().property( "aai-node-type", "tunnel-xconnect")
                    .property("id", "tunnelXConnectWan1")
                    .property("bandwidth-up-wan1", "300")
                    .property("bandwidth-down-wan1", "400")
                    .property("bandwidth-up-wan2", "500")
                    .property("bandwidth-down-wan2", "600")
                    .property("source-of-truth", "JUNIT")
                    .next();

            Vertex pLinkWan1 = g.addV().property("aai-node-type", "physical-link")
                    .property("link-name", "pLinkWan1")
                    .property("service-provider-bandwidth-up-units", "empty")
                    .property("service-provider-bandwidth-down-units", "empty")
                    .property("source-of-truth", "JUNIT")
                    .next();
            Vertex servSub3 = g.addV().property("aai-node-type", "service-subscription")
                    .property("service-type", "DHV")
                    .property("source-of-truth", "JUNIT")
                    .next();
            Vertex servinst3 =  g.addV().property( "aai-node-type", "service-instance")
                    .property("service-type", "DHV")
                    .property("source-of-truth", "JUNIT")
                    .next();
            Vertex allotedRsrc3 =  g.addV().property( "aai-node-type", "allotted-resource")
                    .property("id","rsrc3")
                    .property("source-of-truth", "JUNIT")
                    .next();
            Vertex servinst4 =  g.addV().property( "aai-node-type", "service-instance")
                    .property("service-type", "VVIG")
                    .property("source-of-truth", "JUNIT")
                    .next();
            Vertex servSub4 = g.addV().property("aai-node-type", "service-subscription")
                    .property("service-type", "VVIG")
                    .property("source-of-truth", "JUNIT")
                    .next();

            Vertex genericvnf3 = g.addV().property("aai-node-type", "generic-vnf")
                    .property("vnf-id", "vnfId3")
                    .property("source-of-truth", "JUNIT")
                    .next();
            Vertex vServer3 = g.addV().property("aai-node-type", "vserver")
                    .property("source-of-truth", "JUNIT")
                    .next();
            Vertex pServer3 = g.addV().property("aai-node-type", "pserver")
                    .property("source-of-truth", "JUNIT")
                    .next();
            Vertex pInterfaceWan3 = g.addV().property("aai-node-type", "p-interface")
                    .property("interface-name","ge-0/0/11")
                    .property("source-of-truth", "JUNIT")
                    .next();
            Vertex tunnelXConnectAll_Wan3 =  g.addV().property( "aai-node-type", "tunnel-xconnect")
                    .property("id", "tunnelXConnectWan3")
                    .property("bandwidth-up-wan1", "300")
                    .property("bandwidth-down-wan1", "400")
                    .property("bandwidth-up-wan2", "500")
                    .property("bandwidth-down-wan2", "600")
                    .property("source-of-truth", "JUNIT")
                    .next();

            Vertex pLinkWan3 = g.addV().property("aai-node-type", "physical-link")
                    .property("link-name", "pLinkWan3")
                    .property("service-provider-bandwidth-up-units", "empty")
                    .property("service-provider-bandwidth-down-units", "empty")
                    .property("source-of-truth", "JUNIT")
                    .next();

            EdgeRules rules = EdgeRules.getInstance();

            rules.addTreeEdge(g,servSub1,servinst1);
            rules.addEdge(g,servinst1,allotedRsrc1);
            rules.addTreeEdge(g,servinst2,servSub2);
            rules.addTreeEdge(g,allotedRsrc1,servinst2);

            rules.addTreeEdge(g,allotedRsrc1,tunnelXConnectAll_Wan1);


            rules.addEdge(g,servinst1,genericvnf1);
            rules.addEdge(g,genericvnf1,vServer1);
            rules.addEdge(g,vServer1,pServer1);
            rules.addTreeEdge(g,pServer1,pInterfaceWan1);
            rules.addEdge(g,pInterfaceWan1,pLinkWan1);

            rules.addTreeEdge(g,servSub3,servinst3);
            rules.addEdge(g,servinst3,allotedRsrc3);
            rules.addTreeEdge(g,servinst4,servSub4);
            rules.addTreeEdge(g,allotedRsrc3,servinst4);

            rules.addTreeEdge(g,allotedRsrc3,tunnelXConnectAll_Wan3);


            rules.addEdge(g,servinst3,genericvnf3);
            rules.addEdge(g,genericvnf3,vServer3);
            rules.addEdge(g,vServer3,pServer3);
            rules.addTreeEdge(g,pServer3,pInterfaceWan3);
            rules.addEdge(g,pInterfaceWan3,pLinkWan3);

        } catch(Exception ex){
            success = false;
            logger.error("Unable to create the graph {}", ex);
        } finally {
            if(success){
                transaction.commit();
            } else {
                transaction.rollback();
            }

        }
    }

    @Test
    public void testListAllOfMigrations() throws Exception {
        PrintStream oldOutputStream = System.out;
        final ByteArrayOutputStream myOut = new ByteArrayOutputStream();
        System.setOut(new PrintStream(myOut));

        String [] args = {
            "-c", "./bundleconfig-local/etc/appprops/janusgraph-realtime.properties",
            "-l"
        };

        migrationControllerInternal.run(args);

        String content = myOut.toString();
        assertThat(content, containsString("List of all migrations"));
        System.setOut(oldOutputStream);
    }

    @Test
    public void testRunSpecificMigration() throws Exception {
        String [] args = "-c ./bundleconfig-local/etc/appprops/janusgraph-realtime.properties -m SDWANSpeedChangeMigration".split(" ");
        migrationControllerInternal.run(args);
    }

    @Test
    public void testRunSpecificMigrationAndCommit() throws Exception {
        String [] args = {
                "-c", "./bundleconfig-local/etc/appprops/janusgraph-realtime.properties",
                "-m", "SDWANSpeedChangeMigration",
                "--commit"
        };
        migrationControllerInternal.run(args);
    }

    @Test
    public void testRunSpecificMigrationFromLoadingSnapshotAndCommit() throws Exception{
        clearGraph();
        String [] args = {
                "-d", "./snapshots/sdwan_test_migration.graphson",
                "-c", "./bundleconfig-local/etc/appprops/janusgraph-realtime.properties",
                "-m", "SDWANSpeedChangeMigration"
        };
        migrationControllerInternal.run(args);
    }

    @After
    public void tearDown(){
        clearGraph();
    }

    public void clearGraph(){

        JanusGraphTransaction janusgraphTransaction = AAIGraph.getInstance().getGraph().newTransaction();

        boolean success = true;

        try {
            GraphTraversalSource g = janusgraphTransaction.traversal();

            g.V().has("source-of-truth", "JUNIT")
                 .toList()
                 .forEach((v) -> v.remove());

        } catch(Exception ex) {
            success = false;
            logger.error("Unable to remove all of the vertexes", ex);
        } finally {
            if(success){
                janusgraphTransaction.commit();
            } else {
                janusgraphTransaction.rollback();
            }
        }

    }
}
package org.onap.aai.dbgen;

import com.att.eelf.configuration.EELFLogger;
import com.att.eelf.configuration.EELFManager;
import com.thinkaurelius.titan.core.TitanTransaction;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onap.aai.AAISetup;
import org.onap.aai.dbmap.AAIGraph;
import org.onap.aai.serialization.db.EdgeRules;

import static org.junit.Assert.*;


public class DupeToolTest extends AAISetup {

    private static final EELFLogger logger = EELFManager.getInstance().getLogger(DupeToolTest.class);

    private DupeTool dupeTool;

    @Before
    public void setup(){
        dupeTool = new DupeTool();
        dupeTool.SHOULD_EXIT_VM = false;
        createGraph();
    }

    private void createGraph() {
        TitanTransaction transaction = AAIGraph.getInstance().getGraph().newTransaction();

        EdgeRules edgeRules = EdgeRules.getInstance();

        boolean success = true;

        try {

            GraphTraversalSource g = transaction.traversal();

            Vertex cloudRegionVertex = g.addV()
                    .property("aai-node-type", "cloud-region")
                    .property("cloud-owner", "test-owner")
                    .property("cloud-region-id", "test-region")
                    .property("source-of-truth", "JUNIT")
                    .next();

            Vertex tenantVertex = g.addV()
                    .property("aai-node-type", "tenant")
                    .property("tenant-id", "test-tenant")
                    .property("source-of-truth", "JUNIT")
                    .next();

            Vertex pserverVertex = g.addV()
                    .property("aai-node-type", "pserver")
                    .property("hostname", "test-pserver")
                    .property("in-maint", false)
                    .property("source-of-truth", "JUNIT")
                    .next();

            for(int i = 0; i < 100; ++i){
                g.addV()
                        .property("aai-node-type", "pserver")
                        .property("hostname", "test-pserver")
                        .property("in-maint", false)
                        .property("source-of-truth", "JUNIT")
                        .next();
            }

            edgeRules.addTreeEdge(g, cloudRegionVertex, tenantVertex);
            edgeRules.addEdge(g, cloudRegionVertex, pserverVertex);

        } catch(Exception ex){
            success = false;
            logger.error("Unable to create the vertexes", ex);
        } finally {
            if(success){
                transaction.commit();
            } else {
                transaction.rollback();
                fail("Unable to setup the graph");
            }
        }
    }

    @Test
    public void testDupeTool(){

        String[] args = {
                "-userId", "testuser",
                "-nodeType", "pserver",
                "-timeWindowMinutes", "30",
                "-autoFix",
                "-maxFix", "30",
                "-sleepMinutes", "0"
        };

        dupeTool.main(args);
    }

    @After
    public void tearDown(){

        TitanTransaction transaction = AAIGraph.getInstance().getGraph().newTransaction();
        boolean success = true;

        try {

            GraphTraversalSource g = transaction.traversal();

            g.V().has("source-of-truth", "JUNIT")
                    .toList()
                    .forEach(v -> v.remove());

        } catch(Exception ex){
            success = false;
            logger.error("Unable to remove the vertexes", ex);
        } finally {
            if(success){
                transaction.commit();
            } else {
                transaction.rollback();
                fail("Unable to teardown the graph");
            }
        }

    }
}
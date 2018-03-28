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
import org.janusgraph.core.Cardinality;
import org.janusgraph.core.JanusGraphTransaction;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.junit.Before;
import org.junit.Test;
import org.onap.aai.AAISetup;
import org.onap.aai.dbmap.AAIGraph;
import org.onap.aai.dbmap.DBConnectionType;
import org.onap.aai.introspection.Loader;
import org.onap.aai.introspection.LoaderFactory;
import org.onap.aai.introspection.ModelType;
import org.onap.aai.introspection.Version;
import org.onap.aai.serialization.engines.QueryStyle;
import org.onap.aai.serialization.engines.JanusGraphDBEngine;
import org.onap.aai.serialization.engines.TransactionalGraphEngine;

import java.util.List;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class PropertyMigratorTest extends AAISetup {

    private static final EELFLogger logger = EELFManager.getInstance().getLogger(PropertyMigratorTest.class);

    public static class PserverPropMigrator extends PropertyMigrator {

        public PserverPropMigrator(TransactionalGraphEngine engine, String oldName, String newName, Class<?> type, Cardinality cardinality) {
            super(engine, oldName, newName, type, cardinality);
        }

        @Override
        public boolean isIndexed() {
            return true;
        }

        @Override
        public Optional<String[]> getAffectedNodeTypes() {
            return Optional.of(new String[]{ "pserver" });
        }

        @Override
        public String getMigrationName() {
            return "PserverPropMigrator";
        }
    }

    @Before
    public void setup(){
        AAIGraph.getInstance();
        JanusGraphTransaction janusgraphTransaction = AAIGraph.getInstance().getGraph().newTransaction();
        boolean success = true;

        try {
            GraphTraversalSource g = janusgraphTransaction.traversal();
            g.addV()
                 .property("aai-node-type", "pserver")
                 .property("hostname", "fake-hostname")
                 .property("inv-status", "some status")
                 .property("source-of-truth", "JUNIT")
                 .next();
        } catch(Exception ex){
           success = false;
           logger.error("Unable to commit the transaction {}", ex);

        } finally {
            if(success){
                janusgraphTransaction.commit();
            } else {
                janusgraphTransaction.rollback();
            }

        }
    }

    @Test
    public void testAfterPropertyMigration(){

        String oldPropName = "inv-status";
        String newPropName = "inventory-status";

        Loader loader = LoaderFactory.createLoaderForVersion(ModelType.MOXY, Version.v11);
        JanusGraphDBEngine dbEngine = new JanusGraphDBEngine(QueryStyle.TRAVERSAL, DBConnectionType.REALTIME, loader);
        dbEngine.startTransaction();

        PropertyMigrator propertyMigrator = new PserverPropMigrator(dbEngine, oldPropName, newPropName, String.class, Cardinality.SINGLE);
        propertyMigrator.run();
        assertEquals("Expecting the property to be success", Status.SUCCESS, propertyMigrator.getStatus());
        dbEngine.commit();

        JanusGraphTransaction janusgraphTransaction = AAIGraph.getInstance().getGraph().newTransaction();
        GraphTraversalSource g = janusgraphTransaction.traversal();

        List<Vertex> oldVList = g.V().has("aai-node-type", "pserver").has(oldPropName).toList();
        List<Vertex> newVList = g.V().has("aai-node-type", "pserver").has(newPropName).toList();

        assertEquals("Expecting the vertex list with old property to be zero", 0, oldVList.size());
        assertEquals("Expecting the vertex list with new property to be 1", 1, newVList.size());
        assertEquals("Expecting the equipment type to be some equipment", "some status", newVList.get(0).property(newPropName).value());
    }
}
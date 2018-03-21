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
import org.janusgraph.core.schema.JanusGraphManagement;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.onap.aai.AAISetup;
import org.onap.aai.db.props.AAIProperties;
import org.onap.aai.dbmap.DBConnectionType;
import org.onap.aai.introspection.Loader;
import org.onap.aai.introspection.LoaderFactory;
import org.onap.aai.introspection.ModelType;
import org.onap.aai.introspection.Version;
import org.onap.aai.serialization.db.AAIDirection;
import org.onap.aai.serialization.db.EdgeProperty;
import org.onap.aai.serialization.engines.QueryStyle;
import org.onap.aai.serialization.engines.JanusGraphDBEngine;
import org.onap.aai.serialization.engines.TransactionalGraphEngine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

public class ToscaMigrationTest extends AAISetup {

	private final static Version version = Version.v12;
	private final static ModelType introspectorFactoryType = ModelType.MOXY;
	private final static QueryStyle queryStyle = QueryStyle.TRAVERSAL;
	private final static DBConnectionType type = DBConnectionType.REALTIME;
	private Loader loader;
	private TransactionalGraphEngine dbEngine;
	private JanusGraph graph;
	private ToscaMigration migration;
	private GraphTraversalSource g;
	private Graph tx;

	@Before
	public void setUp() throws Exception {
		graph = JanusGraphFactory.build().set("storage.backend","inmemory").open();
		JanusGraphManagement janusgraphManagement = graph.openManagement();
		tx = graph.newTransaction();
		g = tx.traversal();
		loader = LoaderFactory.createLoaderForVersion(introspectorFactoryType, version);
		dbEngine = new JanusGraphDBEngine(
				queryStyle,
				type,
				loader);

		Vertex gvnf = g.addV().property(AAIProperties.NODE_TYPE, "generic-vnf")
							.property("vnf-id", "toscaMigration-test-vnf")
							.next();

		Vertex lInterface = g.addV().property(AAIProperties.NODE_TYPE, "l-interface")
							.property("interface-name", "toscaMigration-test-lint")
							.next();

		Vertex logicalLink = g.addV().property(AAIProperties.NODE_TYPE, "logical-link")
				.property("link-name", "toscaMigration-logical-link")
				.next();



		gvnf.addEdge("hasLInterface", lInterface, EdgeProperty.CONTAINS.toString(), AAIDirection.OUT.toString(),
													EdgeProperty.DELETE_OTHER_V.toString(), AAIDirection.NONE.toString());

		lInterface.addEdge("usesLogicalLink", logicalLink, EdgeProperty.CONTAINS.toString(), AAIDirection.NONE.toString(),
				EdgeProperty.DELETE_OTHER_V.toString(), AAIDirection.NONE.toString());
		lInterface.addEdge("sourceLInterface", logicalLink, EdgeProperty.CONTAINS.toString(), AAIDirection.NONE.toString(),
				EdgeProperty.DELETE_OTHER_V.toString(), AAIDirection.NONE.toString());

		Vertex pserver = g.addV("aai-node-type","pserver","hostname","a-name").next();
		Vertex vnfc = g.addV("aai-node-type","vnfc","vnfc-name","a-name").next();
		pserver.addEdge("blah", vnfc, EdgeProperty.CONTAINS.toString(), AAIDirection.NONE.toString(),
				EdgeProperty.DELETE_OTHER_V.toString(), AAIDirection.NONE.toString());

		TransactionalGraphEngine spy = spy(dbEngine);
		TransactionalGraphEngine.Admin adminSpy = spy(dbEngine.asAdmin());
		GraphTraversalSource traversal = g;
		when(spy.asAdmin()).thenReturn(adminSpy);
		when(adminSpy.getTraversalSource()).thenReturn(traversal);
		Mockito.doReturn(janusgraphManagement).when(adminSpy).getManagementSystem();
		migration = new ToscaMigration(spy);
		migration.run();


	}
	
	@After
	public void cleanUp() {
		tx.tx().rollback();
		graph.close();
	}
	
	@Test
	public void verifyVnfHasOnlyNewEdgeTest() {
		
		// We want to check that this edge has the expected new label and reversed direction from
		// what it was created with.   NOTE - if the csv file changes what the new label is supposed
		// to be, then this test will need to be updated.
		
		assertEquals("edge direction and label were migrated", true,
				g.V().has(AAIProperties.NODE_TYPE, "generic-vnf").has("vnf-id", "toscaMigration-test-vnf").inE()
						.hasLabel("org.onap.relationships.inventory.BelongsTo").hasNext());
						
		
		assertEquals("if we look for old edge, it should be gone", false,
				g.V().has(AAIProperties.NODE_TYPE, "generic-vnf").has("vnf-id", "toscaMigration-test-vnf").outE()
						.hasLabel("hasLInterface").hasNext());
	}

	@Test
	public void verifyGraphHasNoOldEdgeLabelsTest() {
		assertEquals("Graph should have none of the old edge label"
				, Long.valueOf(0)
				, g.E().hasLabel("hasLInterface","usesLogicalLink").count().next());
		assertEquals("Graph should have none of the old edge label"
				, Long.valueOf(2)
				, g.E().hasLabel("org.onap.relationships.inventory.BelongsTo","tosca.relationships.network.LinksTo")
						.count().next());
	}

	@Test
	public void verifyGenericVnfHas1EdgeTest() {
		assertEquals("Generic vnf should have 1 edge"
				, Long.valueOf(1)
				, g.V().has(AAIProperties.NODE_TYPE, "generic-vnf")
						.both()
						.count().next());

	}

	@Test
	public void verifyLogicalLinkHas2EdgesTest() {
		assertEquals("Logical Link should have 2 edges"
				, Long.valueOf(2)
				, g.V().has(AAIProperties.NODE_TYPE, "logical-link")
						.both()
						.count().next());

		assertTrue("Logical Link has source edge"
				, g.V().has(AAIProperties.NODE_TYPE, "logical-link").bothE("org.onap.relationships.inventory.Source").hasNext());

		assertTrue("Logical Link has default edge"
				, g.V().has(AAIProperties.NODE_TYPE, "logical-link").bothE("tosca.relationships.network.LinksTo").hasNext());

	}

	@Test
	public void checkThatEdgeWithNoRulesDoesNotGetMigratedTest() {
		assertTrue("Edge with no rule did not get migrated ", g.E().hasLabel("blah").hasNext());
	}

}

 
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
package org.onap.aai.dbgen.tags;

import org.janusgraph.core.JanusGraphFactory;
import org.janusgraph.core.JanusGraph;
import org.janusgraph.core.schema.JanusGraphManagement;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Direction;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;
import org.onap.aai.AAISetup;
import org.onap.aai.db.props.AAIProperties;
import org.onap.aai.dbmap.AAIGraph;
import org.onap.aai.dbmap.DBConnectionType;
import org.onap.aai.introspection.Loader;
import org.onap.aai.introspection.LoaderFactory;
import org.onap.aai.introspection.ModelType;
import org.onap.aai.introspection.Version;
import org.onap.aai.serialization.db.AAIDirection;
import org.onap.aai.serialization.db.EdgeProperty;
import org.onap.aai.serialization.db.EdgeRules;
import org.onap.aai.serialization.engines.QueryStyle;
import org.onap.aai.serialization.engines.JanusGraphDBEngine;
import org.onap.aai.serialization.engines.TransactionalGraphEngine;
import org.onap.aai.exceptions.AAIException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.util.Iterator;

public class UpdateEdgeTagsTest extends AAISetup {

	private final static Version version = Version.v12;
	private final static ModelType introspectorFactoryType = ModelType.MOXY;
	private final static QueryStyle queryStyle = QueryStyle.TRAVERSAL;
	private final static DBConnectionType type = DBConnectionType.REALTIME;
	private Loader loader;
	private TransactionalGraphEngine dbEngine;
	private JanusGraph graph;
	private JanusGraph passedGraph;
	private UpdateEdgeTagsCmd edgeOp;
	private GraphTraversalSource g;
	private Graph tx;
    @Rule
    public ExpectedException thrown = ExpectedException.none();
	
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
							.property("vnf-id", "test-vnf")
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
		pserver.addEdge("tosca.relationships.HostedOn", vnfc, EdgeProperty.CONTAINS.toString(), AAIDirection.NONE.toString(),
				EdgeProperty.DELETE_OTHER_V.toString(), AAIDirection.NONE.toString());
		GraphTraversalSource traversal = g;
	
		passedGraph = spy(graph);
		when(passedGraph.traversal()).thenReturn(traversal);
		
   		Iterator<Edge> edgeItr = traversal.E();
   		while( edgeItr != null && edgeItr.hasNext() ){
   			Edge tEdge = edgeItr.next();
   			String edLab = tEdge.label().toString();
   			System.out.println("key = " + tEdge.inVertex().<String>property("aai-node-type").orElse(null)+"|"+ tEdge.outVertex().<String>property("aai-node-type").orElse(null)+ ", label = " + tEdge.label()
   				+ ", for id = " + tEdge.id().toString() + ", set: "+tEdge.keys()+":"+tEdge.values());
   			System.out.println("DEBUG - label = " + edLab);
   		}

	}
	
	@After
	public void cleanUp() {
		tx.tx().rollback();
		graph.close();
	}
	
	@Test
	public void verifyDirectionWasReversed_OriginalGraph() {
	
	 edgeOp = new UpdateEdgeTagsCmd("all", "/updateEdgeTestRules.json");
	 edgeOp.setGraph(passedGraph);
	 try {
		edgeOp.execute();
	 } catch (AAIException e) {
		e.printStackTrace();
	 }
   	 Iterator<Edge> edgeItr = g.E();
	 while( edgeItr != null && edgeItr.hasNext() ){
		 Edge tmpEd = edgeItr.next();
		 System.out.println("Edge = " + tmpEd.inVertex().property("aai-node-type") + ", label = " + tmpEd.label() 
				 + ", for id = " + tmpEd.id().toString() +","+tmpEd.keys()+tmpEd.value("contains-other-v"));
		 try {
		 System.out.println("Edge prevent-delete = " +tmpEd.value("prevent-delete")); 
		 System.out.println("Edge description = " +tmpEd.value("description")); 
		 } catch (Exception e) {;} finally {;}
	 }
		assertEquals("Graph should have four(4) Edges with contains-other-v=OUT"
				, Long.valueOf(4)
				, g.E().has("contains-other-v",AAIDirection.IN.toString()).count().next());
		assertEquals("Graph should have zero(0) Edges with contains-other-v=NONE"
				, Long.valueOf(0)
				, g.E().has("contains-other-v",AAIDirection.NONE.toString()).count().next());
	}

	@Test
	public void verifyDirectionWasReversed_withFilter() {
	
	 edgeOp = new UpdateEdgeTagsCmd("vnfc|pserver", "/updateEdgeTestRules.json");
	 edgeOp.setGraph(passedGraph);
	 try {
		edgeOp.execute();
	 } catch (AAIException e) {
		e.printStackTrace();
	 }	
   	 Iterator<Edge> edgeItr = g.E();
	 while( edgeItr != null && edgeItr.hasNext() ){
		 Edge tmpEd = edgeItr.next();
		 System.out.println("Edge = " + tmpEd.inVertex().property("aai-node-type") + ", label = " + tmpEd.label() 
				 + ", for id = " + tmpEd.id().toString() +","+tmpEd.keys()+tmpEd.value("contains-other-v"));
	 }
		assertEquals("Graph should have one(1) Edges with contains-other-v=OUT"
				, Long.valueOf(1)
				, g.E().has("contains-other-v",AAIDirection.OUT.toString()).count().next());
		assertEquals("Graph should have one(1) Edges with contains-other-v=IN"
				, Long.valueOf(1)
				, g.E().has("contains-other-v",AAIDirection.IN.toString()).count().next());
		assertEquals("Graph should have two(2) Edges with contains-other-v=NONE"
				, Long.valueOf(2)
				, g.E().has("contains-other-v",AAIDirection.NONE.toString()).count().next());
	}

	@Test
	public void verifyFaultyRuleFile_MissingEdgeSpec() throws AAIException {

		thrown.expect(AAIException.class);
		thrown.expectMessage("No EdgeRule found for nodeTypes: pserver|vnfc|blah");

		Vertex pserver = g.V().has(AAIProperties.NODE_TYPE, "pserver").has("hostname", "a-name").next();
		Vertex vnfc = g.V().has(AAIProperties.NODE_TYPE,"vnfc").has("vnfc-name","a-name").next();
		pserver.addEdge("blah", vnfc, EdgeProperty.CONTAINS.toString(), AAIDirection.NONE.toString(),
				EdgeProperty.DELETE_OTHER_V.toString(), AAIDirection.NONE.toString());
		//Edge "blah" is not found in updateEdgeTestRules.json
		edgeOp = new UpdateEdgeTagsCmd("all", "/updateEdgeTestRules.json");
		edgeOp.setGraph(passedGraph);
		edgeOp.execute();
	}

	@Test
	public void verifyNewPropertyWasNOTAdded() {
		//Edge rules in updateEdgeTestRules.json have "description" and "newProperty"
		// but they don't transfer
		 edgeOp = new UpdateEdgeTagsCmd("all", "/updateEdgeTestRules.json");
		 edgeOp.setGraph(passedGraph);
		 try {
			edgeOp.execute();
		 } catch (AAIException e) {
			e.printStackTrace();
		 }	
		assertEquals("Graph could have zero(4) Edges with newProperty=newValue"
				, Long.valueOf(0)
				, g.E().has("newProperty","newValue").count().next());
		assertEquals("Graph should have one(1) Edge with description=A l-interface/logical-link(1) edge description"
				, Long.valueOf(0)
				, g.E().has("description","A l-interface/logical-link(0) edge description").count().next());
	}
}

 
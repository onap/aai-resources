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

package org.openecomp.aai.migration;

import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import org.openecomp.aai.db.props.AAIProperties;
import org.openecomp.aai.dbmap.DBConnectionType;
import org.openecomp.aai.introspection.Loader;
import org.openecomp.aai.introspection.LoaderFactory;
import org.openecomp.aai.introspection.ModelType;
import org.openecomp.aai.introspection.Version;
import org.openecomp.aai.serialization.db.DBSerializer;
import org.openecomp.aai.serialization.db.EdgeRules;
import org.openecomp.aai.serialization.engines.QueryStyle;
import org.openecomp.aai.serialization.engines.TitanDBEngine;
import org.openecomp.aai.serialization.engines.TransactionalGraphEngine;
//import org.openecomp.aai.serialization.queryformats.QueryFormatTestHelper;
import org.openecomp.aai.util.AAIConstants;
import com.thinkaurelius.titan.core.Cardinality;
import com.thinkaurelius.titan.core.TitanFactory;
import com.thinkaurelius.titan.core.TitanGraph;
import com.thinkaurelius.titan.core.TitanTransaction;
import com.thinkaurelius.titan.core.schema.TitanManagement;

@Ignore
public class VertexMergeTest {
	
	
	private final static Version version = Version.v10;
	private final static ModelType introspectorFactoryType = ModelType.MOXY;
	private final static QueryStyle queryStyle = QueryStyle.TRAVERSAL;
	private final static DBConnectionType type = DBConnectionType.REALTIME;
	private static Loader loader;
	private static TransactionalGraphEngine dbEngine;
	private static TitanGraph graph;
	private static EdgeRules rules;
	private static GraphTraversalSource g;
	private static TitanTransaction tx;
	@BeforeClass
	public static void setUp() throws NoSuchFieldException, SecurityException, Exception {
		graph = TitanFactory.build().set("storage.backend","inmemory").open();
		tx = graph.newTransaction();
		g = tx.traversal();
		System.setProperty("AJSC_HOME", ".");
		System.setProperty("BUNDLECONFIG_DIR", "bundleconfig-local");
//		QueryFormatTestHelper.setFinalStatic(AAIConstants.class.getField("AAI_HOME_ETC_OXM"), "src/test/resources/org/openecomp/aai/introspection/");
		loader = LoaderFactory.createLoaderForVersion(introspectorFactoryType, version);
		dbEngine = new TitanDBEngine(
				queryStyle,
				type,
				loader);
		rules = EdgeRules.getInstance();
		
		TitanManagement mgmt = graph.openManagement();
		mgmt.makePropertyKey("test-list").dataType(String.class).cardinality(Cardinality.SET).make();
		mgmt.commit();
		Vertex pserverSkeleton = g.addV().property("aai-node-type", "pserver").property("hostname", "TEST1")
				.property("source-of-truth", "AAI-EXTENSIONS").property("fqdn", "test1.com").property("test-list", "value1").next();

		Vertex pInterface1 = g.addV().property("aai-node-type", "p-interface").property("interface-name", "p-interface1")
				.property(AAIProperties.AAI_URI, "/cloud-infrastructure/pservers/pserver/TEST1/p-interfaces/p-interface/p-interface1").next();
		
		Vertex pInterface2 = g.addV().property("aai-node-type", "p-interface").property("interface-name", "p-interface2")
				.property(AAIProperties.AAI_URI, "/cloud-infrastructure/pservers/pserver/TEST1/p-interfaces/p-interface/p-interface2").next();
		
		Vertex pInterface2Secondary = g.addV().property("aai-node-type", "p-interface").property("interface-name", "p-interface2").property("special-prop", "value")
				.property(AAIProperties.AAI_URI, "/cloud-infrastructure/pservers/pserver/TEST1/p-interfaces/p-interface/p-interface2").next();
		
		Vertex lInterface1 = g.addV().property("aai-node-type", "l-interface").property("interface-name", "l-interface1").property("special-prop", "value")
				.property(AAIProperties.AAI_URI, "/cloud-infrastructure/pservers/pserver/TEST1/p-interfaces/p-interface/p-interface2/l-interfaces/l-interface/l-interface1").next();
		
		Vertex lInterface1Canopi = g.addV().property("aai-node-type", "l-interface").property("interface-name", "l-interface1")
				.property(AAIProperties.AAI_URI, "/cloud-infrastructure/pservers/pserver/TEST1/p-interfaces/p-interface/p-interface2/l-interfaces/l-interface/l-interface1").next();
		
		Vertex logicalLink = g.addV().property("aai-node-type", "logical-link").property("link-name", "logical-link1")
				.property(AAIProperties.AAI_URI, "/network/logical-links/logical-link/logical-link1").next();
		Vertex pserverCanopi = g.addV().property("aai-node-type", "pserver").property("hostname",  "TEST1")
				.property("source-of-truth", "CANOPI-WS").property("fqdn", "test2.com").property("test-list", "value2").next();
		
		Vertex complex1 = g.addV().property("aai-node-type", "complex").property("physical-location-id", "complex1")
				.property("source-of-truth", "RO").next();
		
		Vertex complex2 = g.addV().property("aai-node-type", "complex").property("physical-location-id", "complex2")
				.property("source-of-truth", "RCT").next();
		
		Vertex vserver1 = g.addV().property("aai-node-type", "vserver").property("vserver-id", "vserver1")
				.property("source-of-truth", "RO").next();
		
		Vertex vserver2 = g.addV().property("aai-node-type", "vserver").property("vserver-id", "vserver2")
				.property("source-of-truth", "RCT").next();
		Vertex vserver3 = g.addV().property("aai-node-type", "vserver").property("vserver-id", "vserver3")
				.property("source-of-truth", "RCT").next();
		Vertex vserver4 = g.addV().property("aai-node-type", "vserver").property("vserver-id", "vserver4")
				.property("source-of-truth", "RCT").next();
		Vertex vserver5 = g.addV().property("aai-node-type", "vserver").property("vserver-id", "vserver5")
				.property("source-of-truth", "RCT").next();
		
		
		rules.addEdge(g, pserverSkeleton, complex1);
		rules.addEdge(g, pserverSkeleton, vserver1);
		rules.addEdge(g, pserverSkeleton, vserver2);
		rules.addTreeEdge(g, pserverSkeleton, pInterface1);
		rules.addTreeEdge(g, pserverSkeleton, pInterface2Secondary);
		rules.addTreeEdge(g, pInterface2Secondary, lInterface1);
		rules.addEdge(g, lInterface1, logicalLink);
		rules.addEdge(g, pserverCanopi, complex2);
		rules.addEdge(g, pserverCanopi, vserver3);
		rules.addEdge(g, pserverCanopi, vserver4);
		rules.addEdge(g, pserverCanopi, vserver5);
		rules.addTreeEdge(g, pserverCanopi, pInterface2);
		rules.addTreeEdge(g, pInterface2, lInterface1Canopi);

		Map<String, Set<String>> forceCopy = new HashMap<>();
		Set<String> forceSet = new HashSet<>();
		forceSet.add("fqdn");
		forceCopy.put("pserver", forceSet);
		
		TransactionalGraphEngine spy = spy(dbEngine);
		TransactionalGraphEngine.Admin adminSpy = spy(dbEngine.asAdmin());
		GraphTraversalSource traversal = g;
		GraphTraversalSource readOnly = g;
		when(spy.asAdmin()).thenReturn(adminSpy);
		when(adminSpy.getTraversalSource()).thenReturn(traversal);
		when(adminSpy.getReadOnlyTraversalSource()).thenReturn(readOnly);
		DBSerializer serializer = new DBSerializer(version, spy, introspectorFactoryType, "Merge test");

		VertexMerge merge = new VertexMerge.Builder(loader, spy, serializer).build();
		merge.performMerge(pserverCanopi, pserverSkeleton, forceCopy);
	}
	@AfterClass
	public static void cleanUp() {
		tx.rollback();
		graph.close();
	}

	@Test
	public void run() throws UnsupportedEncodingException {

		assertEquals("pserver merged", false, g.V().has("hostname", "TEST1").has("source-of-truth", "AAI-EXTENSIONS").hasNext());
		assertThat("pserver list merge", Arrays.asList("value1", "value2"), containsInAnyOrder(g.V().has("hostname", "TEST1").values("test-list").toList().toArray()));
		assertEquals("canopi pserver has one edge to vserver2", 1, g.V().has("hostname", "TEST1").both().has("vserver-id", "vserver2").toList().size());
		assertEquals("canopi pserver has one edge to vserver1", 1, g.V().has("hostname", "TEST1").both().has("vserver-id", "vserver1").toList().size());
		assertEquals("canopi pserver retained edge to complex2", true, g.V().has("hostname", "TEST1").both().has("physical-location-id", "complex2").hasNext());
		assertEquals("canopi pserver received forced prop", "test1.com", g.V().has("hostname", "TEST1").values("fqdn").next());
		assertEquals("pserver skeleton child copied", true, g.V().has("hostname", "TEST1").both().has("interface-name", "p-interface1").hasNext());
		assertEquals("pserver skeleton child merged", true, g.V().has("hostname", "TEST1").both().has("interface-name", "p-interface2").has("special-prop", "value").hasNext());
		assertEquals("l-interface child merged", true, g.V().has("hostname", "TEST1").both().has("interface-name", "p-interface2").both().has("interface-name", "l-interface1").has("special-prop", "value").hasNext());
		assertEquals("l-interface child cousin edge merged", true, g.V().has("hostname", "TEST1").both().has("interface-name", "p-interface2").both().has("interface-name", "l-interface1").both().has("link-name", "logical-link1").hasNext());
		assertEquals("one l-interface1 found", new Long(1), g.V().has("interface-name", "l-interface1").count().next());
		assertEquals("one p-interface2 found", new Long(1), g.V().has("interface-name", "p-interface2").count().next());

	}
}

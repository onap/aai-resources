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
package org.onap.aai;

import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.janusgraph.core.JanusGraph;
import org.janusgraph.core.JanusGraphTransaction;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.apache.tinkerpop.gremlin.structure.Vertex;

import static org.mockito.Mockito.*;

public class IncreaseNodesToolTest extends AAISetup {

    @Mock
    JanusGraphTransaction janusGraphtransaction;

    @Mock
    JanusGraph janusGraph;

    @Mock
    GraphTraversalSource graphTraversalSource;

    @Mock
    Vertex mockVertex;

    @Mock
    GraphTraversal<org.apache.tinkerpop.gremlin.structure.Vertex, org.apache.tinkerpop.gremlin.structure.Vertex> graphTraversalVertex;

    private IncreaseNodesTool increaseNodesTool;


    String [] args = {"-numberOfNodes", "5" ,"-nodeType", "pserver", "-uri", "/cloud-infrastructure/pservers/pserver/"};

    @Before
    public void setup(){
        increaseNodesTool = new IncreaseNodesTool(loaderFactory, schemaVersions);
    }

    @Test
    public void addVertex() throws Exception {

        when(janusGraph.newTransaction()).thenReturn(janusGraphtransaction);
        when(janusGraphtransaction.traversal()).thenReturn(graphTraversalSource);
        when(graphTraversalSource.addV()).thenReturn(graphTraversalVertex);
        when(graphTraversalVertex.next()).thenReturn(mockVertex);
        increaseNodesTool.run(janusGraph,args);

        Mockito.verify(janusGraph).newTransaction();

        Mockito.verify(graphTraversalSource,times(5)).addV();
    }

    @Test
    public void addVertexfFail() throws Exception {

        when(janusGraph.newTransaction()).thenReturn(janusGraphtransaction);
        when(janusGraphtransaction.traversal()).thenThrow(new RuntimeException());

        increaseNodesTool.run(janusGraph,args);

        Mockito.verify(janusGraphtransaction).rollback();
    }

}


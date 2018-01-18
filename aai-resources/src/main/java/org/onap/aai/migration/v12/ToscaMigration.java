package org.onap.aai.migration.v12;
/*-
 * ============LICENSE_START=======================================================
 * org.onap.aai
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


import java.util.*;

import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.onap.aai.db.props.AAIProperties;
import org.onap.aai.migration.Enabled;
import org.onap.aai.migration.MigrationDangerRating;
import org.onap.aai.migration.MigrationPriority;
import org.onap.aai.migration.Migrator;
import org.onap.aai.migration.Status;
import org.onap.aai.serialization.db.*;
import org.onap.aai.serialization.db.exceptions.EdgeMultiplicityException;
import org.onap.aai.serialization.engines.TransactionalGraphEngine;

@Enabled

@MigrationPriority(0)
@MigrationDangerRating(1000)
public class ToscaMigration extends Migrator {

    private boolean success = true;
    EdgeRules ers = EdgeRules.getInstance();

    public ToscaMigration(TransactionalGraphEngine graphEngine){
        super(graphEngine);
    }

    @Override
    public Status getStatus() {
    	if (success) {
    		return Status.SUCCESS;
    	} else {
    		return Status.FAILURE;
    	}
    }

    @Override
    public void run() {
		Vertex out = null;
		Vertex in = null;
		boolean isCousin = false;
		String oldEdgeString = null;
		Map<String,Integer> edgeMultiplicityExceptionCtr = new HashMap<>();
		List<String> edgeMissingParentProperty = new ArrayList<>();

		GraphTraversalSource g = engine.asAdmin().getTraversalSource();

    	try {
			Set<Edge> edges = g.E().toSet();
        	for (Edge edge : edges) {
        		// skip if this edge was migrated in a previous run
        		if (edge.label().contains("org.") || edge.label().contains("tosca.")) {
        			continue;
				}
				out = edge.outVertex();
				in = edge.inVertex();
				isCousin = false;

				if (out == null || in == null) {
					logger.error(edge.id() + " invalid because one vertex was null: out=" + edge.outVertex() + " in=" + edge.inVertex());
				} else {

					if (edge.property("contains-other-v").isPresent()) {
						isCousin = "NONE".equals(edge.property("contains-other-v").value());
					} else if (edge.property("isParent").isPresent()) {
						isCousin = !(Boolean)edge.property("isParent").value();
					} else {
						edgeMissingParentProperty.add(this.toStringForPrinting(edge, 1));
					}

					String inVertexNodeType = in.value(AAIProperties.NODE_TYPE);
					String outVertexNodeType = out.value(AAIProperties.NODE_TYPE);
					String label = null;


					Set<String> edgeLabels = ers.getEdgeRules(outVertexNodeType,inVertexNodeType).keySet();

					if (edgeLabels.isEmpty()) {
						logger.error(edge.id() + " did not migrate as no edge rule found for: out=" + outVertexNodeType + " in=" + inVertexNodeType);
						continue;
					} else if (edgeLabels.size() > 1) {
						if (edgeLabels.contains("org.onap.relationships.inventory.Source")) {
							if ("sourceLInterface".equals(edge.label())) {
								label = "org.onap.relationships.inventory.Source";
							} else if ("targetLInterface".equals(edge.label())) {
								label = "org.onap.relationships.inventory.Destination";
							} else {
								label = "tosca.relationships.network.LinksTo";
							}
						}
					}

					try {
						if (isCousin) {
							ers.addEdgeIfPossible(g, in, out, label);
						} else {
							ers.addTreeEdge(g, out, in);
						}
						edge.remove();
					} catch (EdgeMultiplicityException edgeMultiplicityException) {
						logger.warn("Edge Multiplicity Exception: "
								+ "\nInV:\n" + this.toStringForPrinting(in, 1)
								+ "Edge:\n" + this.toStringForPrinting(edge, 1)
								+ "OutV:\n" + this.toStringForPrinting(out, 1)
						);

						final String mapKey = "OUT:" + outVertexNodeType + " " + (isCousin ? EdgeType.COUSIN.toString():EdgeType.TREE.toString()) + " " + "IN:" + inVertexNodeType;
						if (edgeMultiplicityExceptionCtr.containsKey(mapKey)) {
							edgeMultiplicityExceptionCtr.put(mapKey, edgeMultiplicityExceptionCtr.get(mapKey)+1);
						} else {
							edgeMultiplicityExceptionCtr.put(mapKey, 1);
						}
					}
				}
			}
        } catch(Exception ex){
        	logger.error("exception occurred during migration, failing: out=" + out + " in=" + in + "edge=" + oldEdgeString, ex);
        	success = false;
        }

		logger.info("Edge Missing Parent Property Count: " + edgeMissingParentProperty.size());
		logger.info("Edge Multiplicity Exception Count : " + edgeMultiplicityExceptionCtr.values().stream().mapToInt(Number::intValue).sum());
		logger.info("Edge Multiplicity Exception Breakdown : " + edgeMultiplicityExceptionCtr);

	}

	@Override
	public Optional<String[]> getAffectedNodeTypes() {
		return Optional.empty();
	}

	@Override
	public String getMigrationName() {
		return "migrate-all-edges";
	}

}

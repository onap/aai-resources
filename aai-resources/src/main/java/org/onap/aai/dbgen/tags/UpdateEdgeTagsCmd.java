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

import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.janusgraph.core.JanusGraph;
import org.onap.aai.dbmap.AAIGraph;
import org.onap.aai.exceptions.AAIException;
import org.onap.aai.logging.ErrorLogHelper;
import org.onap.aai.serialization.db.EdgeRule;
import org.onap.aai.serialization.db.EdgeRules;
import org.onap.aai.util.AAIConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class UpdateEdgeTagsCmd implements Command {
	private JanusGraph graph;
	private String rulesFilename;
	private EdgeRules edgeRulesInstance = null;
	private String[] edgeRuleKeys = null;

	private String filter = "all";
	private static 	final  String    FROMAPPID = "AAI-DB";
	private static 	final  String    TRANSID   = UUID.randomUUID().toString();
	

	static final Logger logger = LoggerFactory.getLogger(UpdateEdgeTagsCmd.class);

	/**
	 * @param filter
	 */
	public UpdateEdgeTagsCmd(String filter) throws AAIException {
		super();
		this.filter = filter;
		createEdgeRuleSet();
	}

	/**
	 * @param filter
	 * @param rulesFilename
	 */
	public UpdateEdgeTagsCmd(String filter, String rulesFilename) {
		super();
		this.rulesFilename = rulesFilename;
		this.filter = filter;
		this.createEdgeRuleSet();
	}

	@Override
	public void execute() throws AAIException {
    	try {
    		ErrorLogHelper.loadProperties();
    		if(graph == null) {
    			AAIConfig.init();
    			logger.info("    ---- NOTE --- about to open graph (takes a little while)--------\n");    			
    			graph = AAIGraph.getInstance().getGraph();
    		}
    		if( graph == null ){
    			String emsg = "null graph object in updateEdgeTags() \n";
    			logger.info(emsg);
    			return;
    		}
    	}
	    catch (AAIException e1) {
			String msg =  e1.getErrorObject().toString();
			System.out.println(msg);
			return;
        }
        catch (Exception e2) {
	 		String msg =  e2.toString();
	 		System.out.println(msg);
	 		e2.printStackTrace();
	 		return;
        }

    	Graph g = graph.newTransaction();
		try {  
        	 Iterator<Edge> edgeItr = graph.traversal().E();
/*
    		 if("all".equalsIgnoreCase(filter)) {
    			 edgeItr = graph.traversal().E();
    		 } else {
    			 edgeItr = graph.traversal().E()..inV().property("aai-node-type",this.edgeRuleKeys[0]);    			 
    			 edgeItr = graph.traversal().E().inV().has("aai-node-type").values(this.edgeRuleKeys[0]).outV().has("aai-node-type").values(this.edgeRuleKeys[1]);
    		 }
*/
        	 //Iterate over all the edges of the in memory graph
        	 while( edgeItr != null && edgeItr.hasNext() ){

        		 Edge thisEdge = edgeItr.next();
        		 //The filter can limit the application of changes to edges between vertices of one pair of node-types
        		 //Other node type pairs found in in-memory graph are skipped 
        		 if(! passesFilter(thisEdge) ) {
        			 continue;
        		 }
 
        		 //Find the rules in the file between the node-type pair for the current in-memory edge
        		 if( edgeRulesInstance.hasEdgeRule(thisEdge.inVertex().<String>property("aai-node-type").orElse(null),thisEdge.outVertex().<String>property("aai-node-type").orElse(null))) {
    	        		 logger.info("key = " + thisEdge.inVertex().<String>property("aai-node-type").orElse(null)+"|"+ thisEdge.outVertex().<String>property("aai-node-type").orElse(null)+ ", label = " + thisEdge.label()
    	        				 + ", for id = " + thisEdge.id().toString() + ", set: "+thisEdge.keys()+"\n");
    	        		 //Get the rule map from the FILE  for the node-type pair, filtered by label found on the in-memory Edge; expecting one rule
    	        		 //Note: the filter label does not work -- adding equals/contains(thisEdge.label() tests below
    	        		 Map<String, EdgeRule> edgeRules =edgeRulesInstance.getEdgeRules(thisEdge.inVertex().<String>property("aai-node-type").orElse(null),thisEdge.outVertex().<String>property("aai-node-type").orElse(null), thisEdge.label());
//    			         Collection<EdgeRule> edgeRules = edgeRuleMultimap.get(derivedEdgeKey);
    	        		 //Apply the Edge properties from the FILE rule to the in-memory Edge
    			         for(EdgeRule e : edgeRules.values()) {
    			        	 if(e.getLabel().equals(thisEdge.label())) {
        			        	 logger.info("EdgeRule e: " + String.join("|",thisEdge.outVertex().<String>property("aai-node-type").orElse(null),thisEdge.inVertex().<String>property("aai-node-type").orElse(null),e.getLabel()));
    			        		 edgeRulesInstance.addProperties(thisEdge, e); 
    			        	 }
    			         }
    			         //The FILE ruleset is broken? -- run away; discard all changes!
    			         if(! edgeRules.containsKey(thisEdge.label())) {
    						// Couldn't find a rule for this edge
    			        	logger.error("Broken EdgeSet in edgeRuleFile: " + thisEdge.bothVertices());
    						throw new AAIException("AAI_6120", "No EdgeRule found for nodeTypes: " + String.join("|",thisEdge.outVertex().<String>property("aai-node-type").orElse(null),thisEdge.inVertex().<String>property("aai-node-type").orElse(null),thisEdge.label()));
    			         }
    	        } else {
    	        	//The expected FILE ruleset could not be found -- run away; discard all changes!
    	        	logger.error("Missing EdgeSet in edgeRuleFile: " + thisEdge.bothVertices());
					throw new AAIException("AAI_6120", "No EdgeRule found for nodeTypes: " + String.join("|",thisEdge.outVertex().<String>property("aai-node-type").orElse(null),thisEdge.inVertex().<String>property("aai-node-type").orElse(null),thisEdge.label()));
    	        }
	        	 
			 } // End of looping over all in-memory edges
			 graph.tx().commit();
			 logger.info("- committed updates for listed edges " );
		}
		catch (Exception e2) {
			String msg = e2.toString();
			logger.error(msg);
			e2.printStackTrace();
			if( g != null ){
				graph.tx().rollback();
			}
			if(e2 instanceof AAIException) {
				throw e2;
			}
			return;
		}
	}


	/**
	 * @return the rulesFilename
	 */
	public String getRulesFilename() {
		return this.rulesFilename;
	}

	/**
	 * @return the graph
	 */
	public JanusGraph getGraph() {
		return this.graph;
	}

	/**
	 * @param graph the graph to set
	 */
	public void setGraph(JanusGraph graph) {
		this.graph = graph;
	}
	
	private void createEdgeRuleSet() {
		if(this.filter != null) this.edgeRuleKeys = filter.split("\\|");
		edgeRulesInstance = (this.rulesFilename == null) ? EdgeRules.getInstance() : EdgeRules.getInstance(rulesFilename);
		return;
	}
	
	private boolean passesFilter(Edge tEdge) {
		if("all".equalsIgnoreCase(filter) ) {
			logger.debug("EdgeRule PROCESSALL: " + String.join("|",tEdge.outVertex().<String>property("aai-node-type").orElse(null),tEdge.inVertex().<String>property("aai-node-type").orElse(null),tEdge.label()));
			return true;
		}
		Iterator<Vertex> vItr = tEdge.bothVertices();

		ArrayList<String> l = new ArrayList<String>(Arrays.asList(edgeRuleKeys));
		while( vItr != null && vItr.hasNext() ) {
			Vertex v = vItr.next();
			int i = l.indexOf(v.property("aai-node-type").value());
			if (i >= 0) l.remove(i);
		}
		if(l.isEmpty()) {
			logger.debug("EdgeRule filterPROCESS: " + String.join("|",tEdge.outVertex().<String>property("aai-node-type").orElse(null),tEdge.inVertex().<String>property("aai-node-type").orElse(null),tEdge.label()));
		}
		else {
			logger.debug("EdgeRule filterSKIP: " + String.join("|",tEdge.outVertex().<String>property("aai-node-type").orElse(null),tEdge.inVertex().<String>property("aai-node-type").orElse(null),tEdge.label()));
		}
		return l.isEmpty();
	}
}

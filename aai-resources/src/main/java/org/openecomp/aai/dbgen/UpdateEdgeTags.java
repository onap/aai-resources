package org.openecomp.aai.dbgen;

import com.google.common.collect.Multimap;
import com.thinkaurelius.titan.core.TitanGraph;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.openecomp.aai.dbmap.AAIGraph;
import org.openecomp.aai.exceptions.AAIException;
import org.openecomp.aai.logging.ErrorLogHelper;
import org.openecomp.aai.serialization.db.EdgeRule;
import org.openecomp.aai.serialization.db.EdgeRules;
import org.openecomp.aai.util.AAIConfig;

import java.util.*;


public class UpdateEdgeTags {

	private static 	final  String    FROMAPPID = "AAI-DB";
	private static 	final  String    TRANSID   = UUID.randomUUID().toString();
	
	/**
	 * The main method.
	 *
	 * @param args the arguments
	 */
	public static void main(String[] args) {
	
	  	if( args == null || args.length != 1 ){
				String msg = "usage:  UpdateEdgeTags edgeRuleKey  (edgeRuleKey can be either, all, or a rule key like 'nodeTypeA|nodeTypeB') \n";
				System.out.println(msg);
				System.exit(1);
		}
	  	String edgeRuleKeyVal = args[0];

		TitanGraph graph = null;
		EdgeRules edgeRulesInstance = EdgeRules.getInstance();
		Multimap<String, EdgeRule> edgeRuleMultimap = edgeRulesInstance.getAllRules();

		HashMap <String,Object> edgeRuleHash = new HashMap <String,Object>();
		HashMap <String,Object> edgeRulesFullHash = new HashMap <String,Object>();
		HashMap <String,Object> edgeRuleLabelToKeyHash = new HashMap <String,Object>();
		ArrayList <String> labelMapsToMultipleKeys = new <String> ArrayList ();
		
		// Loop through all the edge-rules make sure they look right and
    	//    collect info about which labels support duplicate ruleKeys.
    	Iterator<String> edgeRulesIterator = edgeRuleMultimap.keySet().iterator();

    	while( edgeRulesIterator.hasNext() ){
        	String ruleKey = edgeRulesIterator.next();
        	Collection <EdgeRule> edRuleColl = edgeRuleMultimap.get(ruleKey);
        	Iterator <EdgeRule> ruleItr = edRuleColl.iterator();
    		if( ruleItr.hasNext() ){
    			// For now, we only look for one type of edge between two nodes.
    			EdgeRule edgeRule = ruleItr.next();
    			String edgeRuleString = String.format("%s,%s,%s,%s,%s,%s",
					    edgeRule.getLabel(),
					    edgeRule.getDirection(),
					    edgeRule.getMultiplicityRule(),
					    edgeRule.getContains(),
					    edgeRule.getDeleteOtherV(),
					    edgeRule.getServiceInfrastructure());
    			edgeRulesFullHash.put(ruleKey,edgeRuleString);
  			  	String edgeLabel = edgeRule.getLabel();
  			  	if( edgeRuleLabelToKeyHash.containsKey(edgeLabel) ){
  			  		// This label maps to more than one edge rule - we'll have to figure out
  			  		// which rule applies when we look at each edge that uses this label.
  			  		// So we take it out of mapping hash and add it to the list of ones that
  			  		// we'll need to look up later.
  			  		edgeRuleLabelToKeyHash.remove(edgeLabel);
  			  		labelMapsToMultipleKeys.add(edgeLabel);
  			  	}
  			  	else {
  			  		edgeRuleLabelToKeyHash.put(edgeLabel, ruleKey);
  			  	}
    		}
    	}
    	
		if( ! edgeRuleKeyVal.equals( "all" ) ){
			// If they passed in a (non-"all") argument, that is the single edgeRule that they want to update.
			// Note - the key looks like "nodeA|nodeB" as it appears in DbEdgeRules.EdgeRules
			Collection <EdgeRule> edRuleColl = edgeRuleMultimap.get(edgeRuleKeyVal);
        	Iterator <EdgeRule> ruleItr = edRuleColl.iterator();
    		if( ruleItr.hasNext() ){
    			// For now, we only look for one type of edge between two nodes (Ie. for one key).
    			EdgeRule edRule = ruleItr.next();
    			edgeRuleHash.put(edgeRuleKeyVal, edRule);
    			System.out.println("Adding this rule to list of rules to do: key = " + edgeRuleKeyVal + ", rule = [" + edRule + "]");
    		}
    		else {
    			String msg = " Error - Unrecognized edgeRuleKey: [" + edgeRuleKeyVal + "]. ";
    			System.out.println(msg);
    			System.exit(0);
    		}
		}
		else {
			// They didn't pass a target ruleKey in, so we'll work on all types of edges
			edgeRuleHash.putAll(edgeRulesFullHash);
		}
		
    	try {   
    		AAIConfig.init();
    		System.out.println("    ---- NOTE --- about to open graph (takes a little while)--------\n");
    		ErrorLogHelper.loadProperties();

    		graph = AAIGraph.getInstance().getGraph();
    	
    		if( graph == null ){
    			String emsg = "null graph object in updateEdgeTags() \n";
    			System.out.println(emsg);
    	 		System.exit(0);
    		}
    	}
	    catch (AAIException e1) {
			String msg =  e1.getErrorObject().toString();
			System.out.println(msg);
			System.exit(0);
        }
        catch (Exception e2) {
	 		String msg =  e2.toString();
	 		System.out.println(msg);
	 		e2.printStackTrace();
	 		System.exit(0);
        }

    	Graph g = graph.newTransaction();
		try {  
        	 Iterator<Edge> edgeItr = graph.traversal().E();
        	 
     		 // Loop through all edges and update their tags if they are a type we are interested in.
        	 //    Sorry about looping over everything, but for now, I can't find a way to just select one type of edge at a time...!?
			 StringBuffer sb;
			 boolean missingEdge = false;
        	 while( edgeItr != null && edgeItr.hasNext() ){
        		 Edge tmpEd = edgeItr.next();
	        	 String edLab = tmpEd.label().toString(); 
	        	 
	     		 // Since we have edgeLabels that can be used for different pairs of node-types, we have to
	        	 //    look to see what nodeTypes this edge is connecting (if it is a label that could do this).
	        	 String derivedEdgeKey = "";
	        	 if( labelMapsToMultipleKeys.contains(edLab) ){
	        		 // need to figure out which key is right for this edge
	        		 derivedEdgeKey = deriveEdgeRuleKeyForThisEdge( TRANSID, FROMAPPID, g, tmpEd );
	        	 }
	        	 else {
	        		 // This kind of label only maps to one key -- so we can just look it up.
	        		 if ( edgeRuleLabelToKeyHash.get(edLab) == null ) {
	        			 if ( !missingEdge ) {
	        				 System.out.print("DEBUG - missing edge(s) in edgeRuleLabelToKeyHash " + edgeRuleLabelToKeyHash.toString());
	        				 missingEdge = true;
	        			 }
		        		 sb = new StringBuffer();
	        			 Vertex vIn = null;
	        			 Vertex vOut = null;
	        			 Object obj = null;
	        			 vIn = tmpEd.inVertex();
	        			 if ( vIn != null ){
	        				 obj = vIn.<String>property("aai-node-type").orElse(null);
	        				 if ( obj != null ) {
	        					 sb.append("from node-type " + obj.toString());
	        				 
	        					 obj = vIn.id();
	        					 sb.append(" id " + obj.toString());
	        				 } else {
	        					 sb.append(" missing from node-type ");
	        				 }
	        			 } else {
	        				 sb.append(" missing inbound vertex ");
	        			 }
	        			 vOut = tmpEd.outVertex();
	        			 if ( vOut != null ) {
	        				 obj = vOut.<String>property("aai-node-type").orElse(null);
	        				 if ( obj != null ) {
		        				 sb.append(" to node-type " + obj.toString());
		        				 obj = vOut.id();
		        				 sb.append(" id " + obj.toString());
	        				 } else {
	        					 sb.append(" missing to node-type ");
	        				 }
	        			 } else {
	        				 sb.append(" missing to vertex ");
	        			 }
	        			 System.out.println("DEBUG - null entry for [" + edLab + "] between " + sb.toString());
	        			 continue;
	        		 }
	        		 derivedEdgeKey = edgeRuleLabelToKeyHash.get(edLab).toString();
	        	 }
	        	 
	        	 if( edgeRuleHash.containsKey(derivedEdgeKey) ){
	        		 // this is an edge that we want to update
	        		 System.out.print("DEBUG - key = " + derivedEdgeKey + ", label = " + edLab 
	        				 + ", for id = " + tmpEd.id().toString() + ", set: ");
			         Map<String,EdgeRule> edgeRules = getEdgeTagPropPutHash(TRANSID, FROMAPPID, derivedEdgeKey);
			         for (String key : edgeRules.keySet()) {
			        	if (tmpEd.label().equals(key)) {
			        		EdgeRules.getInstance().addProperties(tmpEd, edgeRules.get(key));
			        	}
			         }
			         System.out.print("\n");
	        	 }
			 } // End of looping over all edges
			 graph.tx().commit();
			 System.out.println("DEBUG - committed updates for listed edges " );
		}
		catch (Exception e2) {
			String msg = e2.toString();
			System.out.println(msg);
			e2.printStackTrace();
			if( graph != null ){
				graph.tx().rollback();
			}
			System.exit(0);
		}

	    System.exit(0);
    
	}// end of main()

	
	/**
	 * Derive edge rule key for this edge.
	 *
	 * @param transId the trans id
	 * @param fromAppId the from app id
	 * @param graph the graph
	 * @param tEdge the t edge
	 * @return String - key to look up edgeRule (fromNodeType|toNodeType)
	 * @throws AAIException the AAI exception
	 */
	public static String deriveEdgeRuleKeyForThisEdge( String transId, String fromAppId, Graph graph,  
			Edge tEdge ) throws AAIException {

		Vertex fromVtx = tEdge.outVertex();
		Vertex toVtx = tEdge.inVertex();
		String startNodeType = fromVtx.<String>property("aai-node-type").orElse(null);
		String targetNodeType = toVtx.<String>property("aai-node-type").orElse(null);
		String key = startNodeType + "|" + targetNodeType;
		if( EdgeRules.getInstance().hasEdgeRule(startNodeType, targetNodeType) ){
			// We can use the node info in the order they were given
			return( key );
		}
		else {
			key = targetNodeType + "|" + startNodeType;
			if( EdgeRules.getInstance().hasEdgeRule(targetNodeType, startNodeType) ){
				return( key );
			}
			else {
				// Couldn't find a rule for this edge
				throw new AAIException("AAI_6120", "No EdgeRule found for passed nodeTypes: " + startNodeType + ", "
						+ targetNodeType); 
			}
		}
	}// end of deriveEdgeRuleKeyForThisEdge()
	

	/**
	 * Gets the edge tag prop put hash.
	 *
	 * @param transId the trans id
	 * @param fromAppId the from app id
	 * @param edgeRuleKey the edge rule key
	 * @return the edge tag prop put hash
	 * @throws AAIException the AAI exception
	 */
	public static Map<String, EdgeRule> getEdgeTagPropPutHash(String transId, String fromAppId, String edgeRuleKey )
			throws AAIException {
		// For a given edgeRuleKey (nodeTypeA|nodeTypeB), look up the rule that goes with it in
		// DbEdgeRules.EdgeRules and parse out the "tags" that need to be set on each edge.  
		// These are the Boolean properties like, "isParent", "usesResource" etc.  
		// Note - this code is also used by the updateEdgeTags.java code

		String[] edgeRuleKeys = edgeRuleKey.split("\\|");
		
		if (edgeRuleKeys.length < 2 || ! EdgeRules.getInstance().hasEdgeRule(edgeRuleKeys[0], edgeRuleKeys[1])) {
			throw new AAIException("AAI_6120", "Could not find an DbEdgeRule entry for passed edgeRuleKey (nodeTypeA|nodeTypeB): " + edgeRuleKey + ".");
		}
		
		Map<String, EdgeRule> edgeRules = EdgeRules.getInstance().getEdgeRules(edgeRuleKeys[0], edgeRuleKeys[1]);
		
		return edgeRules;
		
	} // End of getEdgeTagPropPutHash()


	
}




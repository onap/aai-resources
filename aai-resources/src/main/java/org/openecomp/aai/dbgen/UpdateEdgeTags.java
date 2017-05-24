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

package org.openecomp.aai.dbgen;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import org.apache.tinkerpop.gremlin.structure.Direction;
import org.apache.tinkerpop.gremlin.structure.Vertex;

import org.openecomp.aai.dbmap.AAIGraph;
import org.openecomp.aai.dbmodel.DbEdgeRules;
import org.openecomp.aai.exceptions.AAIException;
import org.openecomp.aai.logging.ErrorLogHelper;
import org.openecomp.aai.serialization.db.EdgeRule;
import org.openecomp.aai.serialization.db.EdgeRules;
import org.openecomp.aai.util.AAIConfig;
import com.thinkaurelius.titan.core.TitanEdge;
import com.thinkaurelius.titan.core.TitanGraph;
import com.thinkaurelius.titan.core.TitanTransaction;
import com.thinkaurelius.titan.core.TitanVertex;



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

		HashMap <String,Object> edgeRuleHash = new HashMap <String,Object>();
		HashMap <String,Object> edgeRulesFullHash = new HashMap <String,Object>();
		HashMap <String,Object> edgeRuleLabelToKeyHash = new HashMap <String,Object>();
		ArrayList <String> labelMapsToMultipleKeys = new <String> ArrayList ();
		
    	int tagCount = DbEdgeRules.EdgeInfoMap.size();
		// Loop through all the edge-rules make sure they look right and
    	//    collect info about which labels support duplicate ruleKeys.
    	Iterator<String> edgeRulesIterator = DbEdgeRules.EdgeRules.keySet().iterator();
    	while( edgeRulesIterator.hasNext() ){
        	String ruleKey = edgeRulesIterator.next();
        	Collection <String> edRuleColl = DbEdgeRules.EdgeRules.get(ruleKey);
        	Iterator <String> ruleItr = edRuleColl.iterator();
    		if( ruleItr.hasNext() ){
    			// For now, we only look for one type of edge between two nodes.
    			String fullRuleString = ruleItr.next();
    			edgeRulesFullHash.put(ruleKey,fullRuleString);
    			// An EdgeRule is comma-separated and the first item is the edgeLabel
  			  	String [] rules = fullRuleString.split(",");
  			  	//System.out.println( "rules.length = " + rules.length + ", tagCount = " + tagCount );
  			  	if( rules.length != tagCount ){
  			  		String detail = "Bad EdgeRule data (itemCount=" + rules.length + ") for key = [" + ruleKey + "].";
  			  		System.out.println(detail);
  			  		System.exit(0);
  			  	}
  			  	String edgeLabel = rules[0];
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
			Collection <String> edRuleColl = DbEdgeRules.EdgeRules.get(edgeRuleKeyVal);
        	Iterator <String> ruleItr = edRuleColl.iterator();
    		if( ruleItr.hasNext() ){
    			// For now, we only look for one type of edge between two nodes (Ie. for one key).
    			String edRule = ruleItr.next();
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

    	TitanTransaction g = graph.newTransaction();
		try {  
        	 Iterable <?> edges = graph.query().edges();
        	 Iterator <?> edgeItr = edges.iterator();
        	 
     		 // Loop through all edges and update their tags if they are a type we are interested in.
        	 //    Sorry about looping over everything, but for now, I can't find a way to just select one type of edge at a time...!?
			 StringBuffer sb;
			 boolean missingEdge = false;
        	 while( edgeItr != null && edgeItr.hasNext() ){
				 TitanEdge tmpEd = (TitanEdge) edgeItr.next();
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
	        			 vIn = tmpEd.vertex(Direction.IN);
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
	        			 vOut = tmpEd.vertex(Direction.OUT);
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
	public static String deriveEdgeRuleKeyForThisEdge( String transId, String fromAppId, TitanTransaction graph,  
			TitanEdge tEdge ) throws AAIException{

		TitanVertex fromVtx = tEdge.outVertex();
		TitanVertex toVtx = tEdge.inVertex();
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
	 * Gets the edge tag prop put hash 4 rule.
	 *
	 * @param transId the trans id
	 * @param fromAppId the from app id
	 * @param edRule the ed rule
	 * @return the edge tag prop put hash 4 rule
	 * @throws AAIException the AAI exception
	 */
	public static HashMap <String,Object> getEdgeTagPropPutHash4Rule( String transId, String fromAppId, String edRule ) 
			throws AAIException{ 
		// For a given edgeRule - already pulled out of DbEdgeRules.EdgeRules --  parse out the "tags" that 
		//     need to be set for this kind of edge.  
		// These are the Boolean properties like, "isParent", "usesResource" etc.  
		HashMap <String,Object> retEdgePropPutMap = new HashMap <String,Object>();
		
		if( (edRule == null) || edRule.equals("") ){
			// No edge rule found for this
			throw new AAIException("AAI_6120", "blank edRule passed to getEdgeTagPropPutHash4Rule()"); 
		}
			
		int tagCount = DbEdgeRules.EdgeInfoMap.size();
		String [] rules = edRule.split(",");
		if( rules.length != tagCount ){
			throw new AAIException("AAI_6121", "Bad EdgeRule data (itemCount =" + rules.length + ") for rule = [" + edRule  + "]."); 
		}

		// In DbEdgeRules.EdgeRules -- What we have as "edRule" is a comma-delimited set of strings.
		// The first item is the edgeLabel.
		// The second in the list is always "direction" which is always OUT for the way we've implemented it.
		// Items starting at "firstTagIndex" and up are all assumed to be booleans that map according to 
		// tags as defined in EdgeInfoMap.
		// Note - if they are tagged as 'reverse', that means they get the tag name with "-REV" on it
		for( int i = DbEdgeRules.firstTagIndex; i < tagCount; i++ ){
			String booleanStr = rules[i];
			Integer mapKey = new Integer(i);
			String propName = DbEdgeRules.EdgeInfoMap.get(mapKey);
			String revPropName = propName + "-REV";
			
			if( booleanStr.equals("true") ){
				retEdgePropPutMap.put(propName, true);
				retEdgePropPutMap.put(revPropName,false);
			}
			else if( booleanStr.equals("false") ){
				retEdgePropPutMap.put(propName, false);
				retEdgePropPutMap.put(revPropName,false);
			}
			else if( booleanStr.equals("reverse") ){
				retEdgePropPutMap.put(propName, false);
				retEdgePropPutMap.put(revPropName,true);
			}
			else {
				throw new AAIException("AAI_6121", "Bad EdgeRule data for rule = [" + edRule + "], val = [" + booleanStr + "]."); 
			}
			
		}

		return retEdgePropPutMap;
		
	} // End of getEdgeTagPropPutHash()
	
	
	/**
	 * Gets the edge tag prop put hash.
	 *
	 * @param transId the trans id
	 * @param fromAppId the from app id
	 * @param edgeRuleKey the edge rule key
	 * @return the edge tag prop put hash
	 * @throws AAIException the AAI exception
	 */
	public static Map<String, EdgeRule> getEdgeTagPropPutHash( String transId, String fromAppId, String edgeRuleKey ) 
			throws AAIException{ 
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




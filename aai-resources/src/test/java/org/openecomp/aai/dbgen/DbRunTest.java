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

import java.nio.file.attribute.FileTime;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;

import org.openecomp.aai.dbmap.AAIGraph;
import org.openecomp.aai.exceptions.AAIException;
import org.openecomp.aai.logging.ErrorLogHelper;
import org.openecomp.aai.util.AAIConfig;
import org.openecomp.aai.util.DbTestGetFileTime;
import com.thinkaurelius.titan.core.TitanTransaction;
import com.thinkaurelius.titan.core.TitanVertex;

public class DbRunTest {

	/**
	 * The main method.
	 *
	 * @param args the arguments
	 */
	public static void main(String[] args) {

		String testid = "NA";
		TitanTransaction graph = null;

		boolean runIt = false;
		try {
			AAIConfig.init();
			ErrorLogHelper.loadProperties();
			if  (args.length > 0) {
				System.out.println( "DbRunTest called with " + args.length + " arguments");
                
                
			} else {
				System.out.print("usage: DbRunTest <testid> <processDelay milliseconds> <processName> <loopcnt>\n");
				return;
			}
		} catch (AAIException e) {
			System.out.print("Threw a AAIException -- \n");
			System.out.println(e.getMessage());
		} catch (Exception ex) {
			System.out.print("Threw a regular ole Exception \n");
			System.out.println(ex.getMessage());
		}
			

		
		 //System.out.println("skipping Titan graph for now...");
		 //TitanGraph graph = null;
		 

        String processName = "NA";
        if ( args.length > 1 )
        	processName = args[2].trim();
		
		try {
			System.out.println(processName + "    ---- NOTE --- about to open graph (takes a little while)--------\n");

			graph = AAIGraph.getInstance().getGraph().newTransaction();
	    	
	       if( graph == null ){
	           System.out.println(processName + "Error creating Titan graph. \n");
	           return;
	       }
		} catch( Exception ex ){
		        System.out.println( processName + " ERROR: caught this exception: " + ex );
		        return;
		} 
		
		testid = args[0].trim();  
        runIt = true;
        Integer processDelay = new Integer(args[1].trim() ); // delay of write
        Integer loopCnt = new Integer(args[3].trim() ); // delay of write

        String path = "NA";
		try {

			path = AAIConfig.get("dbruntest.path" ); // uses file create-timestamp
			Integer writeDelay =  new Integer(AAIConfig.get("dbruntest.delay" )); // delay seconds from create-timestamp
			Integer loopDelay =  new Integer(AAIConfig.get("dbruntest.loopdelay" )); // delay seconds from create-timestamp for next loop
			
			DbTestGetFileTime getFileTime = new DbTestGetFileTime();
			FileTime fileTime = getFileTime.createFileReturnTime( path );
			long createMillis = fileTime.toMillis();
			System.out.println(processName + " delays to use " + writeDelay + " processDelay " + processDelay);
			System.out.println(processName + " test control file " + path + " creation time :"
		        + new SimpleDateFormat("dd/MM/yyyy HH:mm:ss:SSS")
		                   .format(fileTime.toMillis()));
			
			long delayMillis = writeDelay * 60 * 1000;
			long loopMillis = loopDelay * 60 * 1000;
			
			int loops = loopCnt.intValue();
			
			long sleepTime;
			for ( int i = 0; i < loops; ++i ) {
				
				if ( i > 0 )
					delayMillis = loopMillis * i; // to find the time the write will be done
				delayMillis += processDelay.intValue();
			
				System.out.println(processName + " test control file " + path + " write time :"
			        + new SimpleDateFormat("dd/MM/yyyy HH:mm:ss:SSS")
			                   .format(createMillis + delayMillis));

				Calendar now = Calendar.getInstance();
				long myMillis = now.getTimeInMillis();
				System.out.println(processName + " test control file " + path + " current time :"
    		        + new SimpleDateFormat("dd/MM/yyyy HH:mm:ss:SSS")
    		                   .format(myMillis));
				// total sleep based on current time, time file was created, and any delay per process
			
				if ( i == 0 )
					sleepTime = createMillis + delayMillis - myMillis; // + processDelay.intValue()
				else 
					sleepTime = createMillis + ( loopMillis * i ) - myMillis; 
				sleepTime += processDelay.intValue();
				System.out.println(processName + " sleep " + sleepTime + " current time :"
    		        + new SimpleDateFormat("dd/MM/yyyy HH:mm:ss:SSS")
    		                   .format(myMillis));
				Thread.sleep(sleepTime);
				
				System.out.println(processName + " out of sleep for loop count " + i);
			
				if (runIt) {
					// include loop in unique-id string
					runMultiLevelTest(graph, testid + i + "-", processName);
				}

			} // end loop
	

		} catch ( Exception io ) {
			System.out.println(  processName + " Exception getting creation time " + path + " message " + io.getMessage());
			io.printStackTrace();
		}



	
		
		if( graph != null && graph.isOpen() ){
			System.out.println(processName + "About to call graph.shutdown()");
			graph.tx().close();
			System.out.println(processName + " -- AFTER call graph.shutdown()");
		}

		System.out.println(processName + " -- Boom!");
		System.exit(0);
		
	}// end of main()
	
		
		
	/**
	 * Run multi level test.
	 *
	 * @param graph the graph
	 * @param testIdentifier the test identifier
	 * @param processName the process name
	 */
	private static void runMultiLevelTest(TitanTransaction graph, String testIdentifier, String processName) {

		try {
			String useRollback = AAIConfig.get("dbruntest.rollback");  

			//     l3-interface-ipv6-address-list -> vlan -> l-interface -> vnic -> vserver -> tenant
			
			HashMap<String, Object> propHash7 = new HashMap<String, Object>();
			propHash7.put("tenant-name", testIdentifier + "tenantName");
			propHash7.put("tenant-id", testIdentifier + "tenantId");
			TitanVertex tenantVtx = DbMeth.persistAaiNode("junkTransId",
					"testApp", graph, "tenant", propHash7, true, null);

			
			HashMap<String, Object> propHash8 = new HashMap<String, Object>();
			propHash8.put("vserver-selflink", testIdentifier + "vserverSelfThing");
			propHash8.put("vserver-id", testIdentifier + "vserverID");
			propHash8.put("vserver-name", testIdentifier + "vserverName");
			propHash8.put("vserver-name2", testIdentifier + "vserverName2");
			TitanVertex vserverVtx = DbMeth.persistAaiNode("junkTransId",
					"testApp", graph, "vserver", propHash8, true, tenantVtx);
			
			HashMap<String, Object> propHash10 = new HashMap<String, Object>();
			propHash10.put("interface-name", testIdentifier + "logIntfOnVpe");
			TitanVertex logIntfVtx = DbMeth.persistAaiNode("junkTransId",
					"testApp", graph, "l-interface", propHash10, true, vserverVtx);
			
			HashMap<String, Object> propHash11 = new HashMap<String, Object>();
			propHash11.put("vlan-interface", testIdentifier + "vlanIntf");
			TitanVertex vlanVtx = DbMeth.persistAaiNode("junkTransId",
					"testApp", graph, "vlan", propHash11, true, logIntfVtx);
			
			HashMap<String, Object> propHash12 = new HashMap<String, Object>();
			propHash12.put("l3-interface-ipv6-address", testIdentifier + "v6 pool");
			TitanVertex l3PoolVtx = DbMeth.persistAaiNode("junkTransId",
					"testApp", graph, "l3-interface-ipv6-address-list", propHash12, true, vlanVtx);
			
			System.out.println(processName + " ---- did the persist on all the nodes -- ");
			
			DbMeth.persistAaiEdge("junkTransId", "junkApp", graph, vserverVtx, tenantVtx);
			DbMeth.persistAaiEdge("junkTransId", "junkApp", graph, logIntfVtx, vserverVtx);
			DbMeth.persistAaiEdge("junkTransId", "junkApp", graph, vlanVtx, logIntfVtx);
			DbMeth.persistAaiEdge("junkTransId", "junkApp", graph, l3PoolVtx, vlanVtx);
			
			System.out.println(processName + " ---- persisted all the edges -- ");
			
			if ( useRollback.toUpperCase().equals("Y")) {
				System.out.print(processName + " using rollback for unittesting\n");
				if (graph != null) {
					graph.tx().rollback();
				}
			} else {
				graph.tx().commit();
				System.out.println(processName + " ---- Ran graph.commit() -- ");
			}
			HashMap<String, Object> propHash0 = new HashMap<String, Object>();
			propHash0.put("l3-interface-ipv6-address-list.l3-interface-ipv6-address", testIdentifier + "v6 pool");
			propHash0.put("tenant.tenant-name", testIdentifier + "tenantName");
			propHash0.put("tenant.tenant-id", testIdentifier + "tenantId");
			propHash0.put("vserver.vserver-selflink", testIdentifier + "vserverSelfThing");
			propHash0.put("vserver.vserver-id", testIdentifier + "vserverID");
			propHash0.put("vserver.vserver-name", testIdentifier + "vserverName");
			propHash0.put("vserver.vserver-name2", testIdentifier + "vserverName2");
			propHash0.put("l-interface.interface-name", testIdentifier + "logIntfOnVpe");
			propHash0.put("vlan.vlan-interface", testIdentifier + "vlanIntf");
			

			System.out.println(processName + " ---- First try the getUniqueNodeWithDepParams trail: ");

			TitanVertex poolV = DbMeth.getUniqueNodeWithDepParams("junkTransId", "junkAppId", graph, "l3-interface-ipv6-address-list", propHash0, null); 
		
			ArrayList <String> retArr = DbMeth.showPropertiesForNode("junkTransId", "junkFromAppId", poolV);
			for( String info : retArr ){ System.out.println(info); }

/*	
 *  RelationshipUtils replaced with RelationshipGraph
 * 		System.out.println(processName + " ---- Next try the figureRelData trail: ");
			List <RelationshipData> relDatList = RelationshipUtils.figureRelData("junkTransId", "junkAppId", graph, poolV, ""); 
			System.out.println(processName + " ---- Looks like it worked ----------");
			Iterator<RelationshipData> iterDat = relDatList.iterator();
			while (iterDat.hasNext()) {
				RelationshipData relDat = iterDat.next();
				System.out.println(processName + " relData: key = ["
						+ relDat.getRelationshipKey() + "], val = ["
						+ relDat.getRelationshipValue() + "]");
			}*/
			
			
			HashMap<String, Object> propHash = new HashMap<String, Object>();
			ArrayList<TitanVertex> vertList;
		
			propHash.put("tenant-id", testIdentifier + "tenantId");
			vertList = DbMeth.getNodes("junkTransId",
					"testApp", graph, "tenant", propHash, false, null, true);
			
			Iterator<TitanVertex> iter = vertList.iterator();

			int vertexCnt = 0;
			while( iter.hasNext() ) { 
				++vertexCnt;
				TitanVertex tvx = iter.next();
				System.out.println(processName + " Found tenant " + vertexCnt );
				DbMeth.showPropertiesForNode( "junkId", "junkApp", tvx ); 
			}
			if ( vertexCnt == 0 )
				System.out.println(processName + " no tenant found" );
			
			propHash = new HashMap<String, Object>();
			
			propHash.put("vserver-id", testIdentifier + "vserverID");
			vertList = DbMeth.getNodes("junkTransId",
					"testApp", graph, "vserver", propHash, false, null, true);
			
			iter = vertList.iterator();

			vertexCnt = 0;
			while( iter.hasNext() ) { 
				++vertexCnt;
				TitanVertex tvx = iter.next();
				System.out.println(processName + " Found vserver " + vertexCnt );
				DbMeth.showPropertiesForNode( "junkId", "junkApp", tvx ); 
			}
			if ( vertexCnt == 0 )
				System.out.println(processName + " no vserver found" );
			
			if ( useRollback.toUpperCase().equals("Y")) {
				System.out.print(processName + " using rollback for unittesting\n");
				if (graph != null) {
					graph.tx().rollback();
				}
			}
				
		} 
		catch (AAIException e) {
			System.out.print(processName + " Threw an AAIException -- calling graph.rollback()\n");
			System.out.println("exMsg = [" + e.getMessage() + "]");
			if (graph != null) {
				graph.tx().rollback();
			}
		} 
		catch (Exception ex) {
			System.out.print(processName + " Threw a regular ole Exception calling graph.rollback()\n");
			System.out.println(ex.getMessage());
			System.out.println(ex.toString());
			if (graph != null) {
				graph.tx().rollback();
			}
		}



		return;
	}
		

}

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

package org.openecomp.aai.util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.security.KeyManagementException;
import java.util.Properties;
import java.util.UUID;

import org.openecomp.aai.domain.yang.Relationship;
import org.openecomp.aai.exceptions.AAIException;
import com.att.eelf.configuration.Configuration;
import com.att.eelf.configuration.EELFLogger;
import com.att.eelf.configuration.EELFManager;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;


public class RelationshipPutDel {
	
	private static final String FROMAPPID = "AAI-TOOLS";
	private static final String TRANSID = UUID.randomUUID().toString();
	private static EELFLogger LOGGER;
	private static final String USAGE_STRING = "Usage: rshipTool.sh <PUT|DELETE> <resource-path> <filename>\n" +
			"resource-path for a particular resource starting after the aai/<version>, relationship-list/relationship gets added by script\n" +
			"filename is the path to a file which contains the json input for the relationship payload" +
			"for example: relTool.sh PUT cloud-infrastructure/oam-networks/oam-network/test-100-oam /tmp/putrship.json\n";
	
	/**
	 * The main method.
	 *
	 * @param args the arguments
	 */
	public static void main(String[] args) {

		// Set the logging file properties to be used by EELFManager
		Properties props = System.getProperties();
		props.setProperty(Configuration.PROPERTY_LOGGING_FILE_NAME, AAIConstants.AAI_RSHIPTOOL_LOGBACK_PROPS);
		props.setProperty(Configuration.PROPERTY_LOGGING_FILE_PATH, AAIConstants.AAI_HOME_ETC_APP_PROPERTIES);		
		LOGGER = EELFManager.getInstance().getLogger(RelationshipPutDel.class);
		String dMsg = "Start processing...";
		System.out.println(dMsg);
		LOGGER.debug(dMsg);
		
		String rshipURL, resURL = null;
		Relationship rship = null;
		
		try {
			if ((args.length < 3) || (!args[0].equalsIgnoreCase("PUT") && !args[0].equalsIgnoreCase("DELETE"))) {
				System.out.println("Insufficient or Invalid arguments");
				System.out.println(USAGE_STRING);
				System.exit(1);
			} 
			
			rship = readJsonFile(args[2]);
			
			// Assume the config AAI_SERVER_URL has a last slash so remove if  
			//  resource-path has it as the first char
			resURL = args[1].replaceFirst("^/", "");
			resURL = AAIConfig.get(AAIConstants.AAI_SERVER_URL) + resURL;
			rshipURL = resURL.concat("/relationship-list/relationship");

			dMsg = "Resource URL=" + rshipURL;
			System.out.println(dMsg);
			LOGGER.debug(dMsg);
			
			PutDelRelationship(rshipURL, rship, args[0]);
			
			String infMsg = args[0] + " Relationship succeeded to: " + rship.getRelatedTo() + "\n";
			System.out.println(infMsg);
			LOGGER.info(infMsg);
			
			GetResource.getNode(resURL);
			infMsg = "Done!!";
			System.out.println(infMsg);
			LOGGER.info(infMsg);
			
			System.exit(0);

		} catch (AAIException e) {
			LOGGER.error(args[0] + " Relationship PUT/DELETE failed", e);
			System.out.println(args[0] + " Relationship PUT/DELETE failed");
			System.exit(1);	
		} catch (Exception e) {
			LOGGER.error(args[0] + " Relationship PUT/DELETE failed", e);
			System.out.println(args[0] + " Relationship PUT/DELETE failed");
			System.exit(1);
		}
	}
	
	   /**
   	 * Read json file.
   	 *
   	 * @param fName the f name
   	 * @return the relationship
   	 * @throws AAIException the AAI exception
   	 */
   	public static Relationship readJsonFile( String fName ) throws AAIException { 
	        String jsonData = "";
	        BufferedReader br = null;
	        Relationship rship = new Relationship();
	        
	        try {
	            String line;
	            br = new BufferedReader(new FileReader(fName));
	            while ((line = br.readLine()) != null) {
	                jsonData += line + "\n";
	            }
	        } catch (IOException e) {
                throw new AAIException("AAI_7403", e, "Error opening json file");
	        } finally {
	            try {
	                if (br != null)
	                    br.close();
	            } catch (IOException ex) {
	                throw new AAIException("AAI_7403", ex, "Error closing json file");
	            }
	        }

	        try {	        	
	        	rship = MapperUtil.readWithDashesAsObjectOf(Relationship.class, jsonData);
	        }
	        catch (Exception je){
	            throw new AAIException("AAI_7403", je, "Error parsing json file"); 
	        }

	        return rship;

	    }//End readJsonFile()

	
	/**
	 * Put del relationship.
	 *
	 * @param aaiLogger the aai logger
	 * @param logline the logline
	 * @param rshipURL the rship URL
	 * @param rship the rship
	 * @param action the action
	 * @throws AAIException the AAI exception
	 */
	public static void PutDelRelationship(String rshipURL, 
											Relationship rship, 
											String action) throws AAIException{		
		try {
			Client client = HttpsAuthClient.getClient();			
			ClientResponse cres = null;
		
			if (action.equalsIgnoreCase("PUT"))
				 cres = client.resource(rshipURL)
										.header("X-TransactionId", TRANSID)
										.header("X-FromAppId",  FROMAPPID)
										.accept("application/json")
										.entity(rship)
										.put(ClientResponse.class);
			else
				 cres = client.resource(rshipURL)
					.header("X-TransactionId", TRANSID)
					.header("X-FromAppId",  FROMAPPID)
					.accept("application/json")
					.entity(rship)
					.delete(ClientResponse.class);
			
			if (cres.getStatus() == 404) { // resource not found				
				String infMsg = "Resource does not exist...: " + cres.getStatus()
									+ ":\n" + cres.getEntity(String.class);
				System.out.println(infMsg);
				LOGGER.info(infMsg);
	            throw new AAIException("AAI_7404", "Resource does not exist");
			} else if ((action.equalsIgnoreCase("PUT") && cres.getStatus() == 200) ||
					   (action.equalsIgnoreCase("DELETE") && cres.getStatus() == 204)) {
				String infMsg = action + " Resource status: " + cres.getStatus();
				System.out.println(infMsg);
				LOGGER.info(infMsg);
			} else {
				String eMsg = action + " Resource failed: " + cres.getStatus()
						+ ":\n" + cres.getEntity(String.class);
				System.out.println(eMsg);
				LOGGER.error(eMsg);
	            throw new AAIException("AAI_7402", "Error during PutDel");
			}
		} catch (AAIException e) {
            throw e;
		} catch (KeyManagementException e) {
            throw new AAIException("AAI_7401", "Error during PutDel");
		}  catch (Exception e) {
            throw new AAIException("AAI_7402", "Error during PutDel");
		}
	}	

}

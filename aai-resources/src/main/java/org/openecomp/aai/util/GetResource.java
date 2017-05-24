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

import java.security.KeyManagementException;
import java.util.Properties;
import java.util.UUID;

import org.openecomp.aai.exceptions.AAIException;
import org.openecomp.aai.logging.ErrorLogHelper;
import com.att.eelf.configuration.Configuration;
import com.att.eelf.configuration.EELFLogger;
import com.att.eelf.configuration.EELFManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;


public class GetResource {
	
	private static EELFLogger LOGGER;
	private static final String FROMAPPID = "AAI-TOOLS";
	private static final String TRANSID   = UUID.randomUUID().toString();
	private static final String USAGE_STRING = "Usage: getTool.sh <resource-path> \n + "
			+ "for example: resource-path for a particular customer is business/customers/customer/global-customer-id-1 \n";
	
	/**
	 * The main method.
	 *
	 * @param args the arguments
	 */
	public static void main(String[] args) {

		// Set the logging file properties to be used by EELFManager
		Properties props = System.getProperties();
		props.setProperty(Configuration.PROPERTY_LOGGING_FILE_NAME, AAIConstants.AAI_GETRES_LOGBACK_PROPS);
		props.setProperty(Configuration.PROPERTY_LOGGING_FILE_PATH, AAIConstants.AAI_HOME_ETC_APP_PROPERTIES);
		LOGGER = EELFManager.getInstance().getLogger(GetResource.class);
		String url = null;
		try {
			if (args.length < 1) {
				System.out.println("Nothing to get or Insufficient arguments");
				System.out.println(USAGE_STRING);
				System.exit(1);
			} else { 
				// Assume the config AAI_SERVER_URL has a last slash so remove if  
				//  resource-path has it as the first char
				url = args[0].replaceFirst("^/", "");
				url = AAIConfig.get(AAIConstants.AAI_SERVER_URL) + url;

				String dmsg = "url=" + url;
				LOGGER.debug( dmsg );
				System.out.println( dmsg );
				
				getNode(url);
				System.exit(0);
			}
		} catch (AAIException e) {
			String emsg = "GET failed: " + e.getMessage();
			System.out.println(emsg);
			LOGGER.error(emsg);
			ErrorLogHelper.logException(e);
			System.exit(1);	
		} catch (Exception e) {
			String emsg = "GET failed: " + e.getMessage();
			System.out.println(emsg);
			LOGGER.error(emsg);
			ErrorLogHelper.logError("AAI_7402", e.getMessage());
			System.exit(1);
		}
		
	}
	
	/**
	 * Gets the node.
	 *
	 * @param aaiLogger the aai logger
	 * @param logline the logline
	 * @param url the url
	 * @return the node
	 * @throws AAIException the AAI exception
	 */
	public static void getNode(String url) throws AAIException {		
		try {
			String useBasicAuth = AAIConfig.get("aai.tools.enableBasicAuth");
			Client client = null;

			if("true".equals(useBasicAuth)){
			    client = HttpsAuthClient.getBasicAuthClient();
			} else {
			    client = HttpsAuthClient.getTwoWaySSLClient();
			}
			
			System.out.println("Getting the resource...: " + url);
		
			ClientResponse cres = client.resource(url)
									.header("X-TransactionId", TRANSID)
									.header("X-FromAppId",  FROMAPPID)
									.header("Authorization", HttpsAuthClient.getBasicAuthHeaderValue()) 

									.accept("application/json")
									.get(ClientResponse.class);
						
			if (cres.getStatus() == 404) { // resource not found
				String infmsg = "\nResource does not exist: " + cres.getStatus()
						+ ":" + cres.getEntity(String.class);
				System.out.println(infmsg);
				LOGGER.info(infmsg);
	            throw new AAIException("AAI_7404", "Resource does not exist");
			} else if (cres.getStatus() == 200){
				String msg = cres.getEntity(String.class);
				ObjectMapper mapper = new ObjectMapper();
				Object json = mapper.readValue(msg, Object.class);
				String indented = mapper.writerWithDefaultPrettyPrinter()
				                               .writeValueAsString(json);
				System.out.println(indented);
				LOGGER.info(indented);
			} else {
				String emsg = "Getting the Resource failed: " + cres.getStatus()
												+ ":\n" + cres.getEntity(String.class);
				System.out.println(emsg);
				LOGGER.error(emsg);
	            throw new AAIException("AAI_7402", "Error during GET");
			}
		} catch (AAIException e) {
            throw e;
		} catch (KeyManagementException e) {
            throw new AAIException("AAI_7401", e, "Error during GET");
		}  catch (Exception e) {
            throw new AAIException("AAI_7402", e, "Error during GET");
		}
	}	

}

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

import java.util.Properties;
import java.util.UUID;

import org.openecomp.aai.domain.yang.Action;
import org.openecomp.aai.domain.yang.ActionData;
import org.openecomp.aai.domain.yang.Update;
import org.openecomp.aai.exceptions.AAIException;
import org.openecomp.aai.logging.ErrorLogHelper;
import com.att.eelf.configuration.Configuration;
import com.att.eelf.configuration.EELFLogger;
import com.att.eelf.configuration.EELFManager;


public class UpdateResource {
	
	private static EELFLogger LOGGER;
	private static final String FROMAPPID = "AAIUPDT";
	private static final String TRANSID = UUID.randomUUID().toString();
	private static final String UPDATE_URL = "actions/update";	
	private static final String USAGE_STRING = "Usage: updateTool.sh \n" + 
			"<node type> <update node URI> <property name>:<property value>[,<property name>:<property value]* | \n" + 
			"where update node uri is the URI path for that node \n" +
			" for ex. ./updateTool.sh pserver cloud-infrastructure/pservers/pserver/XXX prov-status:NEWSTATUS";
	
	/**
	 * The main method.
	 *
	 * @param args the arguments
	 */
	public static void main(String[] args) {
		
		String nodeType = null;
		String nodeURI = null;
		String updValueList = null;
		
		// Set the logging file properties to be used by EELFManager
		Properties props = System.getProperties();
		props.setProperty(Configuration.PROPERTY_LOGGING_FILE_NAME, AAIConstants.AAI_UPDTOOL_LOGBACK_PROPS);
		props.setProperty(Configuration.PROPERTY_LOGGING_FILE_PATH, AAIConstants.AAI_HOME_ETC_APP_PROPERTIES);
		LOGGER = EELFManager.getInstance().getLogger(UpdateResource.class.getName());
		try {		
			if (args.length < 3) {
				System.out.println("Nothing to update or Insufficient arguments");
				System.out.println(USAGE_STRING);
				System.exit(1);
			} else { 
					nodeType = args[0];
					nodeURI = args[1];
					updValueList = args[2];
					
					update(nodeType, nodeURI, updValueList);
			}
			
			System.exit(0);
		
		} catch (AAIException e) {
			System.out.println("Error - Update Failed.");
			ErrorLogHelper.logException(e);
			System.exit(1);
		} catch (Exception e) {
			System.out.println("Error - Update Failed." + e.getMessage());
			ErrorLogHelper.logError("AAI_7402", "Update failed: " + e.getMessage());
			System.exit(1);
		}
	}
	
	/**
	 * Update.
	 *
	 * @param <T> the generic type
	 * @param aaiLogger the aai logger
	 * @param logline the logline
	 * @param nodeType the node type
	 * @param nodeURI the node URI
	 * @param updValueList the upd value list
	 * @throws AAIException the AAI exception
	 */
	public static <T> void update(String nodeType, String nodeURI, String updValueList) throws AAIException {		
		try {
				Update update = new Update();
				update.setUpdateNodeType(nodeType);
				update.setUpdateNodeUri(nodeURI);
				
				Action action = new Action();
				action.setActionType("replace");
				
				for (String updValue: updValueList.split(",")) {
					ActionData data = new ActionData();
					data.setPropertyName(updValue.substring(0, updValue.indexOf(':')));
					data.setPropertyValue(updValue.substring(updValue.indexOf(':') + 1));
					action.getActionData().add(data);
				}		

				update.getAction().add(action);	
				
				String infMsg = "updating the resource... ";
				System.out.println(infMsg);
				LOGGER.info(infMsg);

				RestController.<Update>Put(update, FROMAPPID, TRANSID, UPDATE_URL);

				infMsg = "Update Successful";
				System.out.println(infMsg);
				LOGGER.info(infMsg);

		} catch (AAIException e) {
			throw e;
		}  catch (Exception e) {
            throw new AAIException("AAI_7402", e, "Error during UPDATE");
		}
	}
}

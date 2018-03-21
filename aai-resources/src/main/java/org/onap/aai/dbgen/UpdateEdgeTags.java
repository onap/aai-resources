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
package org.onap.aai.dbgen;

import org.onap.aai.dbgen.tags.UpdateEdgeTagsCmd;
import org.onap.aai.exceptions.AAIException;
import org.onap.aai.logging.LoggingContext;
import org.onap.aai.logging.LoggingContext.StatusCode;
import org.onap.aai.util.AAIConstants;
import org.onap.aai.util.AAISystemExitUtil;

import java.util.UUID;


public class UpdateEdgeTags {

	private static 	final  String    FROMAPPID = "AAI-DB";
	private static 	final  String    TRANSID   = UUID.randomUUID().toString();
	
	/**
	 * The main method.
	 *
	 * @param args the arguments
	 */
	public static void main(String[] args) {

		System.setProperty("aai.service.name", UpdateEdgeTags.class.getSimpleName());

	  	if( args == null || args.length != 1 ){
				String msg = "usage:  UpdateEdgeTags edgeRuleKey  (edgeRuleKey can be either, all, or a rule key like 'nodeTypeA|nodeTypeB') \n";
				System.out.println(msg);
				System.exit(1);
		}
	  	LoggingContext.init();
		LoggingContext.partnerName(FROMAPPID);
		LoggingContext.serviceName(AAIConstants.AAI_RESOURCES_MS);
		LoggingContext.component("updateEdgeTags");
		LoggingContext.targetEntity(AAIConstants.AAI_RESOURCES_MS);
		LoggingContext.targetServiceName("main");
		LoggingContext.requestId(TRANSID);
		LoggingContext.statusCode(StatusCode.COMPLETE);
		LoggingContext.responseCode("0");
		
	  	String edgeRuleKeyVal = args[0];
	  	
		try {
		  	UpdateEdgeTagsCmd edgeOp = new UpdateEdgeTagsCmd(edgeRuleKeyVal);
			edgeOp.execute();
		} catch (AAIException e) {
			e.printStackTrace();
		} finally {
			AAISystemExitUtil.systemExitCloseAAIGraph(0);
		}
    
	}// end of main()
}




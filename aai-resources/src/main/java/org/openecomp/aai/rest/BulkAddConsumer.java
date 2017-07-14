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

package org.openecomp.aai.rest;

import javax.ws.rs.Path;

import org.openecomp.aai.restcore.HttpMethod;

@Path("{version: v[8-9]|v1[01]}/bulkadd")
public class BulkAddConsumer extends BulkConsumer {

	@Override
	protected boolean functionAllowed(HttpMethod method) {
		
		return method.equals(HttpMethod.PUT);

	}
	
	@Override
	protected boolean enableResourceVersion() {
		return true;
	}

}

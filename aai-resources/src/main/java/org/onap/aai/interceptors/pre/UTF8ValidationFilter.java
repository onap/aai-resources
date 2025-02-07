/**
 * ============LICENSE_START=======================================================
 * org.onap.aai
 * ================================================================================
 * Copyright © 2025 Deutsche Telekom. All rights reserved.
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
package org.onap.aai.interceptors.pre;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import javax.annotation.Priority;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

import org.apache.commons.io.IOUtils;
import org.onap.aai.util.UTF8Validator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Provider
@PreMatching
@Priority(AAIRequestFilterPriority.UTFValidator)
@Component
public class UTF8ValidationFilter implements ContainerRequestFilter {

	
	@Value("${aai.request.encodingvalidation.enabled}")
	public boolean encodingvalidation_enabled;

	@Override
	public void filter(ContainerRequestContext requestContext) throws IOException {

		// Skip validation if not enabled
		
		 if (!encodingvalidation_enabled) { 
			 return; 
		 }
		
		 ByteBuffer buffer = ByteBuffer.wrap(
					(IOUtils.toString(requestContext.getEntityStream(), StandardCharsets.UTF_8))
					.getBytes(StandardCharsets.ISO_8859_1));
			// Validate UTF-8
			if (!UTF8Validator.isValidUTF8(buffer)) {
				// If validation fails, abort the request
				requestContext.abortWith(
						Response.status(Response.Status.BAD_REQUEST)
						.entity("Invalid UTF-8 encoding in request")
						.build()
						);
				return;
			}

		// Reset the entity stream for further processing
		requestContext.setEntityStream(new ByteArrayInputStream((IOUtils.toString(requestContext.getEntityStream(), StandardCharsets.UTF_8)).getBytes(StandardCharsets.UTF_8)));
	}
}
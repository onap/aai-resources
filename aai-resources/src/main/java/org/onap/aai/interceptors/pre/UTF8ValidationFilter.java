package org.onap.aai.interceptors.pre;

import java.io.ByteArrayInputStream;
import java.io.IOException;
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
		 
		
		String content = IOUtils.toString(requestContext.getEntityStream(), StandardCharsets.UTF_8);

		// Validate UTF-8
		if (!UTF8Validator.isValidUTF8(content)) {
			// If validation fails, abort the request
			requestContext.abortWith(
					Response.status(Response.Status.BAD_REQUEST)
					.entity("Invalid UTF-8 encoding in request")
					.build()
					);
			return;
		}

		// Reset the entity stream for further processing
		requestContext.setEntityStream(new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));
	}
}
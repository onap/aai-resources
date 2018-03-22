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
package org.onap.aai.rest.bulk;

import java.net.URI;
import java.util.List;
import java.util.Map.Entry;

import javax.ws.rs.core.UriInfo;

import org.onap.aai.introspection.Introspector;
import org.onap.aai.restcore.HttpMethod;
import org.springframework.util.MultiValueMap;

public class BulkOperation {

	private URI uri = null;
	private Introspector introspector = null;
	private HttpMethod httpMethod = null;
	private String rawReq = "";
	private UriInfo uriInfo = new BulkUriInfo();

	public UriInfo getUriInfo() {
		return uriInfo;
	}

	public String getRawReq() {
		return rawReq;
	}

	public void setRawReq(String rawReq) {
		this.rawReq = rawReq;
	}

	public URI getUri() {
		return uri;
	}

	public void setUri(URI uri) {
		this.uri = uri;
	}

	public Introspector getIntrospector() {
		return introspector;
	}

	public void setIntrospector(Introspector introspector) {
		this.introspector = introspector;
	}

	public HttpMethod getHttpMethod() {
		return httpMethod;
	}

	public void setHttpMethod(HttpMethod httpMethod) {
		this.httpMethod = httpMethod;
	}

	public void addUriInfoQueryParams(MultiValueMap<String, String> queryParams) {
		
		BulkUriInfo bui = new BulkUriInfo();
		
		for (Entry<String, List<String>> entry : queryParams.entrySet()) {
			bui.addParams(entry.getKey(), entry.getValue());
		}
		
		this.uriInfo = bui;
	}

}

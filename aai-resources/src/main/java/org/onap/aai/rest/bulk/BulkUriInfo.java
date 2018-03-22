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

import javax.ws.rs.core.*;


public class BulkUriInfo implements UriInfo {
	
	private MultivaluedMap<String, String> queryParams= new MultivaluedHashMap<>();
		
	@Override
	public String getPath() {
		return null;
	}

	@Override
	public String getPath(boolean decode) {
		return null;
	}

	@Override
	public List<PathSegment> getPathSegments() {
		return null;
	}

	@Override
	public List<PathSegment> getPathSegments(boolean decode) {
		return null;
	}

	@Override
	public URI getRequestUri() {
		return null;
	}

	@Override
	public UriBuilder getRequestUriBuilder() {
		return null;
	}

	@Override
	public URI getAbsolutePath() {
		return null;
	}

	@Override
	public UriBuilder getAbsolutePathBuilder() {
		return null;
	}

	@Override
	public URI getBaseUri() {
		return null;
	}

	@Override
	public UriBuilder getBaseUriBuilder() {
		return null;
	}

	@Override
	public MultivaluedMap<String, String> getPathParameters() {
		return null;
	}

	@Override
	public MultivaluedMap<String, String> getPathParameters(boolean decode) {
		return null;
	}

	@Override
	public MultivaluedMap<String, String> getQueryParameters() {
		return this.queryParams;
	}

	@Override
	public MultivaluedMap<String, String> getQueryParameters(boolean decode) {
		return this.queryParams;
	}

	@Override
	public List<String> getMatchedURIs() {
		return null;
	}

	@Override
	public List<String> getMatchedURIs(boolean decode) {
		return null;
	}

	@Override
	public List<Object> getMatchedResources() {
		return null;
	}

	@Override
	public URI resolve(URI uri) {
		return null;
	}

	@Override
	public URI relativize(URI uri) {
		return null;
	}

	public void addParams(String key, List<String> list) {
		this.queryParams.put(key, list);
	}
}

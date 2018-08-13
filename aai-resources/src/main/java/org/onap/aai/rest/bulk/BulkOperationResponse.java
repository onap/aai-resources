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
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.Response;

import org.javatuples.Pair;
import org.onap.aai.restcore.HttpMethod;

public class BulkOperationResponse {
	
	private URI uri = null;
	private Response response = null;
	private HttpMethod httpMethod = null;
	
	private BulkOperationResponse() {
		
	}
	
	private BulkOperationResponse(HttpMethod httpMethod, Pair<URI, Response> pair) {
		this.httpMethod = httpMethod;
		this.response = pair.getValue1();
		this.uri = pair.getValue0();
	}

	public BulkOperationResponse(HttpMethod httpMethod, URI uri, Response response) {
		this.httpMethod = httpMethod;
		this.response = response;
		this.uri = uri;
	}

	public URI getUri() {
		return uri;
	}

	public void setUri(URI uri) {
		this.uri = uri;
	}

	public Response getResponse() {
		return response;
	}

	public void setResponse(Response response) {
		this.response = response;
	}

	public HttpMethod getHttpMethod() {
		return httpMethod;
	}

	public void setHttpMethod(HttpMethod httpMethod) {
		this.httpMethod = httpMethod;
	}
	
	public static List<BulkOperationResponse> processPairList(HttpMethod httpMethod, List<Pair<URI, Response>> pairList) {
		
		List<BulkOperationResponse> borList = new ArrayList<>();
		BulkOperationResponse bor;
		
		for (Pair<URI, Response> pair: pairList) {
			bor = new BulkOperationResponse(httpMethod, pair);
			borList.add(bor);
		}
		
		return borList;
	}
}

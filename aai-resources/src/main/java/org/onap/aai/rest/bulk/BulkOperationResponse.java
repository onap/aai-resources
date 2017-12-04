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

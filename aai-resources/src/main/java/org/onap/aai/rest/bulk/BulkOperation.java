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

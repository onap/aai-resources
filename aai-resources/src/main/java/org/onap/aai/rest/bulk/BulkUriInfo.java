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

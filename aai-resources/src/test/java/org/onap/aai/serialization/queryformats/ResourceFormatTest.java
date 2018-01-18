/*-
 * ============LICENSE_START=======================================================
 * org.onap.aai
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

package org.onap.aai.serialization.queryformats;

import com.google.gson.JsonObject;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.onap.aai.introspection.Loader;
import org.onap.aai.serialization.db.DBSerializer;
import org.onap.aai.serialization.queryformats.exceptions.AAIFormatVertexException;
import org.onap.aai.serialization.queryformats.utils.UrlBuilder;

import java.io.IOException;

import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

@Ignore
public class ResourceFormatTest {

	@Mock
	private UrlBuilder urlBuilder;
	@Mock
	private DBSerializer serializer;
	@Mock
	private Loader loader;

	private ResourceFormatSpec spec;

	@Before
	public void setup() throws Exception {
		spec = new ResourceFormatSpec();
		MockitoAnnotations.initMocks(this);
	}

	@Test
	public void resourceAndUrl() throws Exception {
		QueryFormatTestHelper.mockPathed(urlBuilder);
		Resource resource = new Resource.Builder(loader, serializer, urlBuilder).includeUrl().build();
		Resource spy = spy(resource);
		doReturn(new JsonObject()).when(spy).vertexToJsonObject(isA(Vertex.class));
		Formatter formatter = new Formatter(spy);
		spec.verifyFormat(formatter, "resource_and_url-format.json");
	}
	
	@Test
	public void resource() throws IOException, AAIFormatVertexException {
		QueryFormatTestHelper.mockPathed(urlBuilder);
		Resource resource = new Resource.Builder(loader, serializer, urlBuilder).build();
		Resource spy = spy(resource);
		doReturn(new JsonObject()).when(spy).vertexToJsonObject(isA(Vertex.class));
		Formatter formatter = new Formatter(spy);
		spec.verifyFormat(formatter, "resource-format.json");
	}
}

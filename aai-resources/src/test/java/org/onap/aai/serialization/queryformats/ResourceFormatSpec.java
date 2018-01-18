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

import com.bazaarvoice.jolt.JsonUtils;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.onap.aai.transforms.JoltTestUtil;

import java.io.IOException;
import java.util.List;

public class ResourceFormatSpec {


	private final Graph graph;
	public ResourceFormatSpec() throws IOException {

		this.graph = QueryFormatTestHelper.loadGraphson("resource.graphson");
	}
	
	
	public void verifyFormat(Formatter formatter, String fileName) throws IOException {
		List<Object> vertices = graph.traversal().V().not(__.has("aai-node-type", "cloud-region")).map(x -> (Object)x.get()).toList();

		JsonObject obj = formatter.output(vertices); 
		String jsonStr = new Gson().toJson(obj); 
		Object actual = JsonUtils.jsonToObject(jsonStr);
		
		Object expected = JsonUtils.filepathToObject(QueryFormatTestHelper.testResources + fileName);

		JoltTestUtil.runArrayOrderObliviousDiffy("Failed case ", expected, actual);
	}
	
	public void mockToJson(String result) {
		
		
		
	}
}

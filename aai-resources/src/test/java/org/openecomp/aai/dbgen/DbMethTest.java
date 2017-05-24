/*-
 * ============LICENSE_START=======================================================
 * org.openecomp.aai
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

package org.openecomp.aai.dbgen;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.openecomp.aai.exceptions.AAIException;

public class DbMethTest {

	@Rule
	public ExpectedException expectedEx = ExpectedException.none();
	
	@Test
	public void testGetEdgeTagPropPutHash() throws AAIException {
		Map<String, String> expectedMap = new HashMap<>();
		expectedMap.put("usesResource", "false");
		expectedMap.put("isParent", "false");
		expectedMap.put("SVC-INFRA", "true");
		expectedMap.put("hasDelTarget", "false");
		
		assertEquals(expectedMap, DbMeth.getEdgeTagPropPutHash("", "", "pserver|complex").get("locatedIn").getEdgeProperties());
	}
	
	@Test
	public void testGetEdgeTagPropPutHash2() throws AAIException {
		Map<String, String> expectedMap = new HashMap<>();
		expectedMap.put("usesResource", "false");
		expectedMap.put("isParent", "false");
		expectedMap.put("SVC-INFRA", "false");
		expectedMap.put("hasDelTarget", "false");
		
		assertEquals(expectedMap, DbMeth.getEdgeTagPropPutHash("", "", "service-instance|allotted-resource").get("uses").getEdgeProperties());
	}
	
	@Test
	public void testGetEdgeTagPropPutHash3() throws AAIException {
		Map<String, String> expectedMap = new HashMap<>();
		expectedMap.put("usesResource", "false");
		expectedMap.put("isParent", "true");
		expectedMap.put("SVC-INFRA", "false");
		expectedMap.put("hasDelTarget", "false");
		
		assertEquals(expectedMap, DbMeth.getEdgeTagPropPutHash("", "", "service-instance|allotted-resource").get("has").getEdgeProperties());
	}

	@Ignore
	@Test
	public void getGetEdgeTagPropPutHashThrowsExceptionWhenNoRuleExists() throws Exception {
		expectedEx.expect(AAIException.class);
	    expectedEx.expectMessage("AAI_6120");
	    DbMeth.getEdgeTagPropPutHash("", "", "complex|pserver");
	}

	@Ignore
	@Test
	public void getGetEdgeTagPropPutHashThrowsExceptionWhenNoRuleExists1() throws Exception {
		expectedEx.expect(AAIException.class);
	    expectedEx.expectMessage("AAI_6120");
	    DbMeth.getEdgeTagPropPutHash("", "", "complex");
	}
	

}

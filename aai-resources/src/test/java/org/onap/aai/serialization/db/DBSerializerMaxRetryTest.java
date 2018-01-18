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

package org.onap.aai.serialization.db;


import com.att.eelf.configuration.EELFLogger;
import com.thinkaurelius.titan.core.TitanException;
import com.thinkaurelius.titan.core.TitanGraph;
import com.thinkaurelius.titan.core.TitanTransaction;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.onap.aai.db.props.AAIProperties;
import org.onap.aai.exceptions.AAIException;
import org.onap.aai.introspection.LoaderFactory;
import org.onap.aai.parsers.query.QueryParser;
import org.onap.aai.rest.db.HttpEntry;
import org.onap.aai.serialization.engines.TransactionalGraphEngine;
import org.onap.aai.serialization.engines.query.QueryEngine;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
@Ignore
@RunWith(PowerMockRunner.class)
@PrepareForTest({QueryParser.class, TransactionalGraphEngine.class, TitanGraph.class, TitanTransaction.class, LoaderFactory.class, DBSerializer.class})
public class DBSerializerMaxRetryTest {

	@Rule
	public ExpectedException thrown = ExpectedException.none();
	
	/**
	 * Test.
	 *
	 * @throws Exception the exception
	 */
	@Test
	public void test() throws Exception {
		
		HttpEntry mockEntry = PowerMockito.mock(HttpEntry.class);
		QueryParser mockQuery = PowerMockito.mock(QueryParser.class);
		PowerMockito.when(mockQuery.isDependent()).thenReturn(true);
		TransactionalGraphEngine mockEngine = PowerMockito.mock(TransactionalGraphEngine.class);
		TitanGraph mockGraph = PowerMockito.mock(TitanGraph.class);
		//PowerMockito.when(mockEngine.getGraph()).thenReturn(mockGraph);
		TitanTransaction mockGraphTrans = PowerMockito.mock(TitanTransaction.class);
		PowerMockito.when(mockGraph.newTransaction()).thenReturn(mockGraphTrans);
		QueryEngine mockQueryEngine = PowerMockito.mock(QueryEngine.class);
		PowerMockito.when(mockEngine.getQueryEngine()).thenReturn(mockQueryEngine);
		PowerMockito.when(mockQuery.getQueryBuilder().getParentQuery().toList()).thenThrow(new TitanException("mock error"));
		EELFLogger mockLogger = PowerMockito.mock(EELFLogger.class);
		
		PowerMockito.whenNew(EELFLogger.class).withAnyArguments().thenReturn(mockLogger);
		
		PowerMockito.mockStatic(LoaderFactory.class);
		
		DBSerializer dbs = new DBSerializer(AAIProperties.LATEST, mockEngine, null, null);
		
		thrown.expect(AAIException.class);
		thrown.expectMessage("AAI_6134");
		dbs.serializeToDb(null, null, mockQuery, null, null);
	}

}

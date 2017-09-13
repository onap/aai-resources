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

package org.openecomp.aai.util;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.powermock.modules.agent.PowerMockAgent;
import org.powermock.modules.junit4.rule.PowerMockRule;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class AAICSVWriterTest {
	
	@Rule
	public PowerMockRule rule = new PowerMockRule();
	
	static {
	     PowerMockAgent.initializeIfNeeded();
	 }
	

	Writer writer;
	File f; 
	AAICSVWriter testObj;
	String fileName = "test_csvWriter.csv";
	String lineEnd = "\n";
	char quoteChar = '\"';
	String separator = ",";
	String str1[], str2[];
	
	/**
	 * Initialize.
	 */
	@Before
	public void initialize(){
		str1 = new String[]{"s0", "s1"};
		
		str2 = new String[]{"t0", "t1"}; 
		
		try {
			f = new File(fileName);
			f.createNewFile();
			writer = new PrintWriter(f);
			testObj = new AAICSVWriter(new FileWriter(fileName), separator, quoteChar, lineEnd);
		} catch (FileNotFoundException e) {
			fail("Input csv file not found.");
			e.printStackTrace();
		} catch (IOException e) {
			fail("Can't create csv file.");
			e.printStackTrace();
		}
	}
	
	/**
	 * Test writeNextLine with String arrays.
	 */
	@Test
	public void testWriteNextLine(){
		populateAndGetFileSize(false, str1, str2);
		String wholeText = str1[0] + separator + str1[1] + lineEnd + str2[0] + separator + str2[1] + lineEnd;   
		assertTrue("CSV file missing text", allLinesFound(wholeText));
	}
	
	/**
	 * Test writeNextLine with null.
	 */
	@Test
	public void testWriteNextLine_withNull(){
		populateAndGetFileSize(false, null, null);
		assertTrue("CSV file should not contain any text", f.length() == 0);
	}
	

	/**
	 * Test writeColumn with String arrays.
	 */
	@Test
	public void testWriteColumn(){
		populateAndGetFileSize(true, str1, str2);
		String wholeText = str1[0] + str1[1] + lineEnd + str2[0] + str2[1] + lineEnd;   
		assertTrue("CSV file missing text", allLinesFound(wholeText));
	}

	
	/**
	 * Test writeColumn with null.
	 */
	@Test
	public void testWriteColumn_withNull(){
		populateAndGetFileSize(true, null, null);
		assertTrue("CSV file should not contain any text", f.length() == 0);
	}
	
	/**
	 * Helper method to create file with given data.
	 *
	 * @param isColumnWise True if csv file is to be written in column wise, false otherwise
	 * @param c1 First set of data
	 * @param c2 Second set of data
	 */
	private void populateAndGetFileSize(boolean isColumnWise, String c1[], String c2[]){
		if ( isColumnWise ){
			testObj.writeColumn(c1);
			testObj.writeColumn(c2);
		} else{
			testObj.writeNext(c1, false);
			testObj.writeNext(c2, false);
		}
		try {
			testObj.close();
		} catch (IOException e) {
			fail("Can't close stream");
			e.printStackTrace();
		}
	}
	

	/**
	 * Helper method to check if a file contains required data.
	 *
	 * @param all Data to look for
	 * @return True if data is found, false otherwise
	 */
	private boolean allLinesFound(String all){
		String fileContents = "";
		try {
			fileContents = new String(Files.readAllBytes(Paths.get(fileName)));
		} catch (IOException e1) {
			fail("csv file not found");
			e1.printStackTrace();
		}

		return all.equals(fileContents);
	}
	
	
	/**
	 * Cleanup.
	 */
	@After
	public void cleanup(){
		if ( f.exists() ){
			f.delete();
		}
	}
	
}

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

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.UUID;

import org.openecomp.aai.exceptions.AAIException;
import org.openecomp.aai.logging.ErrorLogHelper;
import com.att.eelf.configuration.Configuration;
import com.att.eelf.configuration.EELFLogger;
import com.att.eelf.configuration.EELFManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.CaseFormat;

/*
 * Allows to call POST REST API that AAI supports - currently for edge-tag-query
 */
public class PostResource {

	private static final EELFLogger LOGGER = EELFManager.getInstance().getLogger(PostResource.class);
	private static final String FROMAPPID = "AAI-TOOLS";
	private static final String TRANSID = UUID.randomUUID().toString();
	private static final String USAGE_STRING = "Usage: postTool.sh <resource-path> <filename>\n" +
			"resource-path for a particular resource or query starting after the aai/<version>\n" +
			"filename is the path to a file which contains the json input for the payload\n" +
			"for example: postTool.sh search/edge-tag-query /tmp/query-input.json\n";
	
	/**
	 * The main method.
	 *
	 * @param <T> the generic type
	 * @param args the arguments
	 */
	public static <T> void main(String[] args) {

		// Set the logging file properties to be used by EELFManager
		Properties props = System.getProperties();
		props.setProperty(Configuration.PROPERTY_LOGGING_FILE_NAME, AAIConstants.AAI_POSTTOOL_LOGBACK_PROPS);
		props.setProperty(Configuration.PROPERTY_LOGGING_FILE_PATH, AAIConstants.AAI_HOME_ETC_APP_PROPERTIES);
		
		try {
			if (args.length < 2) {
				System.out.println("Insufficient arguments");
				System.out.println(USAGE_STRING);
				System.exit(1);
			}
			
			// Assume the config AAI_SERVER_URL has a last slash so remove if  
			//  resource-path has it as the first char
			String path = args[0].replaceFirst("^/", "");		
			Path p = Paths.get(path);
			
			// currently , it is for edge-taq-query only
			String query = p.getName(p.getNameCount() - 1).toString();
			String resourceClass = null;
			if (query.equals("edge-tag-query"))
				resourceClass = "org.openecomp.aai.domain.search." + 
						CaseFormat.LOWER_HYPHEN.to(CaseFormat.UPPER_CAMEL, query) + "Request";
			else {
				ErrorLogHelper.logError("AAI_7403", "Incorrect resource or query");
				System.exit(1);
			}
			
			LOGGER.debug("class=" + resourceClass);
			LOGGER.debug("path=" + path);
			
			@SuppressWarnings("unchecked")
			T resJson1 = (T)readJsonFile(Class.forName(resourceClass), args[1]);
						
			String response = RestController.<T>Post(resJson1, FROMAPPID, TRANSID, path);
			ObjectMapper mapper = new ObjectMapper();
			Object json = mapper.readValue(response, Object.class);
			
			LOGGER.info(" POST succeeded\n");
			LOGGER.info("Response = " + mapper.writer().withDefaultPrettyPrinter().writeValueAsString(json));
			LOGGER.info("\nDone!!");
			
			System.exit(0); 

		} catch (AAIException e) {
			ErrorLogHelper.logException(e);
			System.exit(1);	
		} catch (Exception e) {
			ErrorLogHelper.logError("AAI_7402", e.getMessage());
			System.exit(1);
		}
	}
	
	/**
	 * Gets the single instance of PostResource.
	 *
	 * @param <T> the generic type
	 * @param clazz the clazz
	 * @return single instance of PostResource
	 * @throws IllegalAccessException the illegal access exception
	 * @throws InstantiationException the instantiation exception
	 */
	public static <T> T getInstance(Class<T> clazz) throws IllegalAccessException, InstantiationException
	{
		return clazz.newInstance();
	} 
	
	/**
	 * Read json file.
	 *
	 * @param <T> the generic type
	 * @param clazz the clazz
	 * @param fName the f name
	 * @return the t
	 * @throws AAIException the AAI exception
	 */
	public static <T> T  readJsonFile( Class<T> clazz, String fName ) throws AAIException 
	{    	
        String jsonData = "";
        BufferedReader br = null;
        T t;
        
        try {
            String line;
            br = new BufferedReader(new FileReader(fName));
            while ((line = br.readLine()) != null) {
                jsonData += line + "\n";
            }
        } catch (IOException e) {
            throw new AAIException("AAI_7403", e, "Error opening json file");
        } finally {
            try {
                if (br != null)
                    br.close();
            } catch (IOException ex) {
                ex.printStackTrace();
                throw new AAIException("AAI_7403", ex, "Error closing json file");
            }
        }

        try {	        	
        	t = MapperUtil.readWithDashesAsObjectOf(clazz, jsonData);
        }
        catch (Exception je){
            throw new AAIException("AAI_7403", je, "Error parsing json file"); 
        }

        return t;

    }//End readJsonFile()	 
}

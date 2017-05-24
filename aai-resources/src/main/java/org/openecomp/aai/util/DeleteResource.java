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

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Properties;
import java.util.Scanner;
import java.util.UUID;

import org.openecomp.aai.exceptions.AAIException;
import org.openecomp.aai.ingestModel.DbMaps;
import org.openecomp.aai.ingestModel.IngestModelMoxyOxm;
import org.openecomp.aai.logging.ErrorLogHelper;
import com.att.eelf.configuration.Configuration;
import com.att.eelf.configuration.EELFLogger;
import com.att.eelf.configuration.EELFManager;
import com.google.common.base.CaseFormat;


public class DeleteResource {
	
	private static EELFLogger LOGGER;
	private static final String FROMAPPID = "AAI-TOOLS";
	private static final String TRANSID   = UUID.randomUUID().toString();
	// the logic below to parse the url is dependent on the node types
	// code may need to be adjusted if different nodetypes are added here
	private static  final  String[]  nodeTypeList = {"complex", "availability-zone", "oam-network", 
            											"dvs-switch", "vserver", "vpe", "vpls-pe"};
	
	private static final String USAGE_STRING = "Usage: deleteTool.sh <resource-path>\n + "
			+ "for example: resource-path for a particular customer is business/customers/customer/global-customer-id-1 \n";
	
	/**
	 * The main method.
	 *
	 * @param <T> the generic type
	 * @param args the arguments
	 */
	public static <T> void main(String[] args) {

		// Set the logging file properties to be used by EELFManager
		Properties props = System.getProperties();
		props.setProperty(Configuration.PROPERTY_LOGGING_FILE_NAME, AAIConstants.AAI_DELTOOL_LOGBACK_PROPS);
		props.setProperty(Configuration.PROPERTY_LOGGING_FILE_PATH, AAIConstants.AAI_HOME_ETC_APP_PROPERTIES);
		LOGGER = EELFManager.getInstance().getLogger(DeleteResource.class);
		String url = null;
		try {
			if ((args.length < 1) || ((args.length > 1) )) {
				System.out.println("Insufficient or Invalid arguments");
				System.out.println(USAGE_STRING);
				System.exit(1);
			} 
			
			Boolean bResVersionEnabled = false;
			
			try
			{
			    String strEnableResVersion = AAIConfig.get(AAIConstants.AAI_RESVERSION_ENABLEFLAG);
			    if (strEnableResVersion != null && !strEnableResVersion.isEmpty())
			       bResVersionEnabled = Boolean.valueOf(strEnableResVersion);
			}
			catch (Exception e) {
			
			}
			
			// Assume the config AAI_SERVER_URL has a last slash so remove if  
			//  resource-path has it as the first char
			String path = args[0].replaceFirst("^/", "");

			Path p = Paths.get(path);
			
			// if the node type has one key
			String resource = p.getName(p.getNameCount() - 2).toString();
			
			IngestModelMoxyOxm moxyMod = new IngestModelMoxyOxm();
			DbMaps dbMaps = null;
			try {
				ArrayList <String> defaultVerLst = new ArrayList <String> ();
				defaultVerLst.add( AAIConfig.get(AAIConstants.AAI_DEFAULT_API_VERSION_PROP) );
				moxyMod.init( defaultVerLst, false);
				// Just make sure we can get DbMaps - don't actually need it until later.
				dbMaps = IngestModelMoxyOxm.dbMapsContainer.get(AAIConfig.get(AAIConstants.AAI_DEFAULT_API_VERSION_PROP));
				}
			catch (Exception ex){
				ErrorLogHelper.logError("AAI_7402", " ERROR - Could not get the DbMaps object: " + ex.getMessage());
				System.exit(1);
			}
			// if the node type has two keys - this assumes max 2 keys
			if (!dbMaps.NodeKeyProps.containsKey(resource))
				resource = p.getName(p.getNameCount() - 3).toString();
			String resourceClass = CaseFormat.LOWER_HYPHEN.to(CaseFormat.UPPER_CAMEL, resource);
			resourceClass = "org.openecomp.aai.domain.yang." + resourceClass;
			
			String dMsg = "class=" + resourceClass;
			System.out.println(dMsg);
			LOGGER.debug(dMsg);
			dMsg = "path=" + path;
			System.out.println(dMsg);
			LOGGER.debug(dMsg);
			
			if (bResVersionEnabled)
			{
				RestObject<T> restObj = new RestObject<T>();
				@SuppressWarnings("unchecked")
				T t = (T)getInstance(Class.forName(resourceClass));
				restObj.set(t);
			
				try
				{
				    RestController.<T>Get(t, FROMAPPID, TRANSID, path, restObj, false);
				    t = restObj.get();
				    String infMsg = " GET resoruceversion succeeded\n";
					System.out.println(infMsg);
					LOGGER.info(infMsg);
				    String resourceUpdateVersion = GetResourceVersion(t); 
				    path += "?resource-version=" + resourceUpdateVersion;
				} catch (AAIException e) {	
					if (e.getMessage().contains("with status=404") && resource.equals("named-query")) {
						System.out.println("Delete succeeded, the resource doesn't exist in the DB. " + p);
						System.exit(0);
					} else {
						System.out.println("Delete failed. Resource Not found in the DB.");
						System.exit(1);
					}
				} catch (Exception e1){
					System.out.println("Delete failed. Resource Not found in the DB.");
					System.exit(1);
				}
			}
			
			System.out.print("\nAre you sure you would like to delete the resource \n" + url + "? (y/n): ");
			Scanner s = new Scanner(System.in);
			s.useDelimiter("");
			String confirm = s.next();
			
			if (!confirm.equalsIgnoreCase("y")) {
				String infMsg = "User chose to exit before deleting";
				System.out.println(infMsg);
				LOGGER.info(infMsg);
				System.exit(1);
			}
			
			RestController.Delete(FROMAPPID, TRANSID, path);

			s.close();
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
	 * Gets the single instance of DeleteResource.
	 *
	 * @param <T> the generic type
	 * @param clazz the clazz
	 * @return single instance of DeleteResource
	 * @throws IllegalAccessException the illegal access exception
	 * @throws InstantiationException the instantiation exception
	 */
	public static <T> T getInstance(Class<T> clazz) throws IllegalAccessException, InstantiationException
	{
		return clazz.newInstance();
	} 
	
	/**
	 * Gets the resource version.
	 *
	 * @param <T> the generic type
	 * @param resource the resource
	 * @return the string
	 */
	public static <T> String GetResourceVersion(T resource)
	{
		Field[] fields = resource.getClass().getDeclaredFields();
		if (fields != null)
		{
		    for (Field field : fields)
		    {
		    	try
		    	{
			    	field.setAccessible(true);
			    	if ( field.getName().equalsIgnoreCase("resourceVersion") )
			    	{
			    		Object obj = field.get(resource);
			    		return (String)obj;
			    	}
			    		
			  
		    	}
		    	catch (Exception e)
	    		{
	    		
	    		}
		    	
		    	
		    }
		}
		return null;
	}
}

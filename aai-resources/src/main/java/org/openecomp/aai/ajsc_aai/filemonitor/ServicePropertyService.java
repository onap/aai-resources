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

package org.openecomp.aai.ajsc_aai.filemonitor;

//import java.io.File;
//import java.io.FileInputStream;
//import java.io.IOException;
//import java.lang.reflect.Method;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Properties;

//import javax.annotation.PostConstruct;

//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;

//import com.att.ssf.filemonitor.FileChangedListener;
//import com.att.ssf.filemonitor.FileMonitor;

//public class ServicePropertyService {
	//private boolean loadOnStartup;
	//private ServicePropertiesListener fileChangedListener;
	//private ServicePropertiesMap filePropertiesMap;
	//private String ssfFileMonitorPollingInterval;
	//private String ssfFileMonitorThreadpoolSize;
	//private List<File> fileList;
	//private static final String FILE_CHANGE_LISTENER_LOC = System
			//.getProperty("AJSC_CONF_HOME") + "/etc";
	//private static final String USER_CONFIG_FILE = "service-file-monitor.properties";
	//static final Logger logger = LoggerFactory
			//.getLogger(ServicePropertyService.class);

	//// do not remove the postConstruct annotation, init method will not be
	//// called after constructor
	/**
	 * Inits the.
	 *
	 * @throws Exception the exception
	 */	
	//@PostConstruct
	//public void init() throws Exception {

		//try {
			//getFileList(FILE_CHANGE_LISTENER_LOC);

			//for (File file : fileList) {
				//try {
					//FileChangedListener fileChangedListener = this.fileChangedListener;
					//Object filePropertiesMap = this.filePropertiesMap;
					//Method m = filePropertiesMap.getClass().getMethod(
							//"refresh", File.class);
					//m.invoke(filePropertiesMap, file);
					//FileMonitor fm = FileMonitor.getInstance();
					//fm.addFileChangedListener(file, fileChangedListener,
							//loadOnStartup);
				//} catch (Exception ioe) {
					//logger.error("Error in the file monitor block", ioe);
				//}
			//}
		//} catch (Exception ex) {
			//logger.error("Error creating property map ", ex);
		//}

	//}

	/**
	 * Gets the file list.
	 *
	 * @param dirName the dir name
	 * @return the file list
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	//private void getFileList(String dirName) throws IOException {
		//File directory = new File(dirName);
		//FileInputStream fis = null;

		//if (fileList == null)
			//fileList = new ArrayList<File>();

		//// get all the files that are ".json" or ".properties", from a directory
		//// & it's sub-directories
		//File[] fList = directory.listFiles();

		//for (File file : fList) {
			//// read service property files from the configuration file
			//if (file.isFile() && file.getPath().endsWith(USER_CONFIG_FILE)) {
				//try {
					//fis = new FileInputStream(file);
					//Properties prop = new Properties();
					//prop.load(fis);

					//for (String filePath : prop.stringPropertyNames()) {
						//fileList.add(new File(prop.getProperty(filePath)));
					//}
				//} catch (Exception ioe) {
					//logger.error("Error reading the file stream ", ioe);
				//} finally {
					//fis.close();
				//}
			//} else if (file.isDirectory()) {
				//getFileList(file.getPath());
			//}
		//}

	//}

	/**
	 * Sets the load on startup.
	 *
	 * @param loadOnStartup the new load on startup
	 */
	//public void setLoadOnStartup(boolean loadOnStartup) {
		//this.loadOnStartup = loadOnStartup;
	//}

	/**
	 * Sets the ssf file monitor polling interval.
	 *
	 * @param ssfFileMonitorPollingInterval the new ssf file monitor polling interval
	 */
	//public void setSsfFileMonitorPollingInterval(
			//String ssfFileMonitorPollingInterval) {
		//this.ssfFileMonitorPollingInterval = ssfFileMonitorPollingInterval;
	//}

	/**
	 * Sets the ssf file monitor threadpool size.
	 *
	 * @param ssfFileMonitorThreadpoolSize the new ssf file monitor threadpool size
	 */
	//public void setSsfFileMonitorThreadpoolSize(
			//String ssfFileMonitorThreadpoolSize) {
		//this.ssfFileMonitorThreadpoolSize = ssfFileMonitorThreadpoolSize;
	//}

	/**
	 * Gets the load on startup.
	 *
	 * @return the load on startup
	 */
	//public boolean getLoadOnStartup() {
		//return loadOnStartup;
	//}

	/**
	 * Gets the ssf file monitor polling interval.
	 *
	 * @return the ssf file monitor polling interval
	 */
	//public String getSsfFileMonitorPollingInterval() {
		//return ssfFileMonitorPollingInterval;
	//}

	/**
	 * Gets the ssf file monitor threadpool size.
	 *
	 * @return the ssf file monitor threadpool size
	 */
	//public String getSsfFileMonitorThreadpoolSize() {
		//return ssfFileMonitorThreadpoolSize;
	//}

	/**
	 * Gets the file changed listener.
	 *
	 * @return the file changed listener
	 */
	//public ServicePropertiesListener getFileChangedListener() {
		//return fileChangedListener;
	//}

	/**
	 * Sets the file changed listener.
	 *
	 * @param fileChangedListener the new file changed listener
	 */
	//public void setFileChangedListener(
			//ServicePropertiesListener fileChangedListener) {
		//this.fileChangedListener = fileChangedListener;
	//}

	/**
	 * Gets the file properties map.
	 *
	 * @return the file properties map
	 */
	//public ServicePropertiesMap getFilePropertiesMap() {
		//return filePropertiesMap;
	//}

	/**
	 * Sets the file properties map.
	 *
	 * @param filePropertiesMap the new file properties map
	 */
	//public void setFilePropertiesMap(ServicePropertiesMap filePropertiesMap) {
		//this.filePropertiesMap = filePropertiesMap;
	//}
//}

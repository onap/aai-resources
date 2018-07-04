/**
 * ============LICENSE_START=======================================================
 * org.onap.aai
 * ================================================================================
 * Copyright © 2017-2018 AT&T Intellectual Property. All rights reserved.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ============LICENSE_END=========================================================
 */
package org.onap.aai.dbgen;

import java.util.Properties;

import org.onap.aai.dbmap.DBConnectionType;
import org.onap.aai.introspection.Loader;
import org.onap.aai.introspection.LoaderFactory;
import org.onap.aai.introspection.ModelType;
import org.onap.aai.introspection.Version;
import org.onap.aai.logging.ErrorLogHelper;
import org.onap.aai.serialization.engines.QueryStyle;
import org.onap.aai.serialization.engines.JanusGraphDBEngine;
import org.onap.aai.serialization.engines.TransactionalGraphEngine;
import org.onap.aai.util.AAIConfig;
import org.onap.aai.util.AAIConstants;
import org.onap.aai.util.UniquePropertyCheck;
import org.slf4j.MDC;

import com.att.eelf.configuration.Configuration;
import com.att.eelf.configuration.EELFLogger;
import com.att.eelf.configuration.EELFManager;

public class SchemaMod {



	/**
	 * The main method.
	 *
	 * @param args the arguments
	 */
	public static void main(String[] args) {

		SchemaMod.execute(args);
		System.exit(0);

	}// End of main()

	/**
	 * Execute.
	 *
	 * @param args the args
	 */
	public static void execute(String[] args) {

		// Set the logging file properties to be used by EELFManager
		Properties props = System.getProperties();
		props.setProperty(Configuration.PROPERTY_LOGGING_FILE_NAME, AAIConstants.AAI_SCHEMA_MOD_LOGBACK_PROPS);
		props.setProperty(Configuration.PROPERTY_LOGGING_FILE_PATH, AAIConstants.AAI_HOME_ETC_APP_PROPERTIES);

		EELFLogger logger = EELFManager.getInstance().getLogger(UniquePropertyCheck.class.getSimpleName());
		MDC.put("logFilenameAppender", SchemaMod.class.getSimpleName());

		// NOTE -- We're just working with properties that are used for NODES
		// for now.
		String propName = "";
		String targetDataType = "";
		String targetIndexInfo = "";
		String preserveDataFlag = "";

		String usageString = "Usage: SchemaMod propertyName targetDataType targetIndexInfo preserveDataFlag \n";
		if (args.length != 4) {
			String emsg = "Four Parameters are required.  \n" + usageString;
			logAndPrint(logger, emsg);
			System.exit(1);
		} else {
			propName = args[0];
			targetDataType = args[1];
			targetIndexInfo = args[2];
			preserveDataFlag = args[3];
		}

		if (propName.equals("")) {
			String emsg = "Bad parameter - propertyName cannot be empty.  \n" + usageString;
			logAndPrint(logger, emsg);
			System.exit(1);
		} else if (!targetDataType.equals("String") && !targetDataType.equals("Set<String>")
				&& !targetDataType.equals("Integer") && !targetDataType.equals("Long")
				&& !targetDataType.equals("Boolean")) {
			String emsg = "Unsupported targetDataType.  We only support String, Set<String>, Integer, Long or Boolean for now.\n"
					+ usageString;
			logAndPrint(logger, emsg);
			System.exit(1);
		} else if (!targetIndexInfo.equals("uniqueIndex") && !targetIndexInfo.equals("index")
				&& !targetIndexInfo.equals("noIndex")) {
			String emsg = "Unsupported IndexInfo.  We only support: 'uniqueIndex', 'index' or 'noIndex'.\n"
					+ usageString;
			logAndPrint(logger, emsg);
			System.exit(1);
		}

		try {
			AAIConfig.init();
			ErrorLogHelper.loadProperties();
		} catch (Exception ae) {
			String emsg = "Problem with either AAIConfig.init() or ErrorLogHelper.LoadProperties(). ";
			logAndPrint(logger, emsg + "[" + ae.getMessage() + "]");
			System.exit(1);
		}

		
		// Give a big warning if the DbMaps.PropertyDataTypeMap value does not
		// agree with what we're doing
		String warningMsg = "";
		/*if (!dbMaps.PropertyDataTypeMap.containsKey(propName)) {
			String emsg = "Property Name = [" + propName + "] not found in PropertyDataTypeMap. ";
			logAndPrint(logger, emsg);
			System.exit(1);
		} else {
			String currentDataType = dbMaps.PropertyDataTypeMap.get(propName);
			if (!currentDataType.equals(targetDataType)) {
				warningMsg = "TargetDataType [" + targetDataType + "] does not match what is in DbRules.java ("
						+ currentDataType + ").";
			}
		}*/

		if (!warningMsg.equals("")) {
			logAndPrint(logger, "\n>>> WARNING <<<< ");
			logAndPrint(logger, ">>> " + warningMsg + " <<<");
		}

		logAndPrint(logger, ">>> Processing will begin in 5 seconds (unless interrupted). <<<");
		try {
			// Give them a chance to back out of this
			Thread.sleep(5000);
		} catch (java.lang.InterruptedException ie) {
			logAndPrint(logger, " DB Schema Update has been aborted. ");
			Thread.currentThread().interrupt();
			System.exit(1);
		}

		logAndPrint(logger, "    ---- NOTE --- about to open graph (takes a little while)\n");

		Version version = Version.getLatest();
		QueryStyle queryStyle = QueryStyle.TRAVERSAL;
		ModelType introspectorFactoryType = ModelType.MOXY;
		Loader loader = LoaderFactory.createLoaderForVersion(introspectorFactoryType, version);
		TransactionalGraphEngine engine = null;
		try {
			engine = new JanusGraphDBEngine(queryStyle, DBConnectionType.REALTIME, loader);
			SchemaModInternal internal = new SchemaModInternal(engine, logger, propName, targetDataType, targetIndexInfo, new Boolean(preserveDataFlag));
			internal.execute();
			engine.startTransaction();
			engine.tx().close();
		} catch (Exception e) {
			String emsg = "Not able to get a graph object in SchemaMod.java\n";
			logAndPrint(logger, e.getMessage());
			logAndPrint(logger, emsg);
			System.exit(1);
		}
	}
	/**
	 * Log and print.
	 *
	 * @param logger the logger
	 * @param msg the msg
	 */
	protected static void logAndPrint(EELFLogger logger, String msg) {
		System.out.println(msg);
		logger.info(msg);
	}
	

}

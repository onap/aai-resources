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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.tinkerpop.gremlin.structure.io.IoCore;
import org.apache.tinkerpop.gremlin.structure.io.graphson.LegacyGraphSONReader;
import org.openecomp.aai.dbmap.AAIGraph;
import org.openecomp.aai.exceptions.AAIException;
import org.openecomp.aai.logging.ErrorLogHelper;
import org.openecomp.aai.util.AAIConfig;
import org.openecomp.aai.util.AAIConstants;
import org.openecomp.aai.util.FormatDate;

import com.att.eelf.configuration.Configuration;
import com.att.eelf.configuration.EELFLogger;
import com.att.eelf.configuration.EELFManager;
import com.thinkaurelius.titan.core.TitanGraph;
import com.thinkaurelius.titan.core.util.TitanCleanup;

public class DataSnapshot {

	private static EELFLogger LOGGER;
	/**
	 * The main method.
	 *
	 * @param args
	 *            the arguments
	 */
	public static void main(String[] args) {
		// Set the logging file properties to be used by EELFManager
		Properties props = System.getProperties();
		props.setProperty(Configuration.PROPERTY_LOGGING_FILE_NAME, AAIConstants.AAI_DATA_SNAPSHOT_LOGBACK_PROPS);
		props.setProperty(Configuration.PROPERTY_LOGGING_FILE_PATH, AAIConstants.AAI_HOME_ETC_APP_PROPERTIES);
		LOGGER = EELFManager.getInstance().getLogger(DataSnapshot.class);
		Boolean dbClearFlag = false;
		TitanGraph graph = null;
		String command = "JUST_TAKE_SNAPSHOT"; // This is the default
		String oldSnapshotFileName = "";
		if (args.length == 1) {
			command = args[0];
		}
		if (args.length == 2) {
			// If they pass in a RELOAD_ENTIRE_DB argument, then we will be
			// reloading the database
			// from the filename passed in -which will be expected to be found
			// in our snapshot directory.
			command = args[0];
			oldSnapshotFileName = args[1];
		}
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			AAIConfig.init();
			ErrorLogHelper.loadProperties();
			System.out.println("Command = " + command + ", oldSnapshotFileName = " + oldSnapshotFileName);
			String targetDir = AAIConstants.AAI_HOME + AAIConstants.AAI_FILESEP + "logs" + AAIConstants.AAI_FILESEP + "data" + AAIConstants.AAI_FILESEP + "dataSnapshots";

			// Make sure the dataSnapshots directory is there
			new File(targetDir).mkdirs();

			System.out.println("    ---- NOTE --- about to open graph (takes a little while)\n");

			graph = AAIGraph.getInstance().getGraph();

			if (graph == null) {
				String emsg = "Not able to get a graph object in DataSnapshot.java\n";
				System.out.println(emsg);
				System.exit(1);
			}

			if (command.equals("JUST_TAKE_SNAPSHOT")) {
				// ------------------------------------------
				// They just want to take a snapshot.
				// ------------------------------------------
				FormatDate fd = new FormatDate("yyyyMMddHHmm", "GMT");
				String dteStr = fd.getDateTime();
				String newSnapshotOutFname = targetDir + AAIConstants.AAI_FILESEP + "dataSnapshot.graphSON." + dteStr;

				graph.io(IoCore.graphson()).writeGraph(newSnapshotOutFname);

				System.out.println("Snapshot written to " + newSnapshotOutFname);
				/****
				 * Don't really want to do this every hour ************** int
				 * vCount = 0; Iterator vIt =
				 * graph.query().vertices().iterator(); while( vIt.hasNext() ){
				 * vCount++; vIt.next(); } System.out.println(
				 * "A little after taking the snapshot, we see: " + vCount +
				 * " vertices in the db.");
				 ************/
			} else if (command.equals("CLEAR_ENTIRE_DATABASE")) {
				// ------------------------------------------------------------------
				// They are calling this to clear the db before re-loading it
				// later
				// ------------------------------------------------------------------

				// First - make sure the backup file they will be using can be
				// found and has data
				if (oldSnapshotFileName.equals("")) {
					String emsg = "No oldSnapshotFileName passed to DataSnapshot.";
					System.out.println(emsg);
					System.exit(1);
				}
				String oldSnapshotFullFname = targetDir + AAIConstants.AAI_FILESEP + oldSnapshotFileName;
				File f = new File(oldSnapshotFullFname);
				if (!f.exists()) {
					String emsg = "oldSnapshotFile " + oldSnapshotFullFname + " could not be found.";
					System.out.println(emsg);
					System.exit(1);
				} else if (!f.canRead()) {
					String emsg = "oldSnapshotFile " + oldSnapshotFullFname + " could not be read.";
					System.out.println(emsg);
					System.exit(1);
				} else if (f.length() == 0) {
					String emsg = "oldSnapshotFile " + oldSnapshotFullFname + " had no data.";
					System.out.println(emsg);
					System.exit(1);
				}

				System.out.println("\n>>> WARNING <<<< ");
				System.out.println(">>> All data and schema in this database will be removed at this point. <<<");
				System.out.println(">>> Processing will begin in 5 seconds. <<<");
				System.out.println(">>> WARNING <<<< ");

				try {
					// Give them a chance to back out of this
					Thread.sleep(5000);
				} catch (java.lang.InterruptedException ie) {
					System.out.println(" DB Clearing has been aborted. ");
					System.exit(1);
				}

				System.out.println(" Begin clearing out old data. ");
				graph.close();
				TitanCleanup.clear(graph);
				System.out.println(" Done clearing data. ");
				System.out.println(">>> IMPORTANT - NOTE >>> you need to run the SchemaGenerator (use GenTester) before ");
				System.out.println("     reloading data or the data will be put in without indexes. ");
				dbClearFlag = true;
				
			} else if (command.equals("RELOAD_LEGACY_DATA")) {
				// -------------------------------------------------------------------
				// They want to restore the database from an old snapshot file
				// -------------------------------------------------------------------
				if (oldSnapshotFileName.equals("")) {
					String emsg = "No oldSnapshotFileName passed to DataSnapshot when RELOAD_LEGACY_DATA used.";
					System.out.println(emsg);
					System.exit(1);
				}
				String oldSnapshotFullFname = targetDir + AAIConstants.AAI_FILESEP + oldSnapshotFileName;
				File f = new File(oldSnapshotFullFname);
				if (!f.exists()) {
					String emsg = "oldSnapshotFile " + oldSnapshotFullFname + " could not be found.";
					System.out.println(emsg);
					System.exit(1);
				} else if (!f.canRead()) {
					String emsg = "oldSnapshotFile " + oldSnapshotFullFname + " could not be read.";
					System.out.println(emsg);
					System.exit(1);
				} else if (f.length() == 0) {
					String emsg = "oldSnapshotFile " + oldSnapshotFullFname + " had no data.";
					System.out.println(emsg);
					System.exit(1);
				}

				System.out.println("We will load data IN from the file = " + oldSnapshotFullFname);
				System.out.println(" Begin reloading Titan 0.5 data. ");
				
				LegacyGraphSONReader lgr = LegacyGraphSONReader.build().create();
				InputStream is = new FileInputStream(oldSnapshotFullFname);
				lgr.readGraph(is, graph);
				
				System.out.println("Completed the inputGraph command, now try to commit()... ");
				graph.tx().commit();
				System.out.println("Completed reloading Titan 0.5 data.");

				long vCount = graph.traversal().V().count().next();
				System.out.println("A little after repopulating from an old snapshot, we see: " + vCount + " vertices in the db.");
			} else if (command.equals("RELOAD_DATA")) {
				// -------------------------------------------------------------------
				// They want to restore the database from an old snapshot file
				// -------------------------------------------------------------------
				if (oldSnapshotFileName.equals("")) {
					String emsg = "No oldSnapshotFileName passed to DataSnapshot when RELOAD_DATA used.";
					System.out.println(emsg);
					System.exit(1);
				}
				String oldSnapshotFullFname = targetDir + AAIConstants.AAI_FILESEP + oldSnapshotFileName;
				File f = new File(oldSnapshotFullFname);
				if (!f.exists()) {
					String emsg = "oldSnapshotFile " + oldSnapshotFullFname + " could not be found.";
					System.out.println(emsg);
					System.exit(1);
				} else if (!f.canRead()) {
					String emsg = "oldSnapshotFile " + oldSnapshotFullFname + " could not be read.";
					System.out.println(emsg);
					System.exit(1);
				} else if (f.length() == 0) {
					String emsg = "oldSnapshotFile " + oldSnapshotFullFname + " had no data.";
					System.out.println(emsg);
					System.exit(1);
				}

				System.out.println("We will load data IN from the file = " + oldSnapshotFullFname);
				System.out.println(" Begin reloading data. ");
				graph.io(IoCore.graphson()).readGraph(oldSnapshotFullFname);
				System.out.println("Completed the inputGraph command, now try to commit()... ");
				graph.tx().commit();
				System.out.println("Completed reloading data.");

				long vCount = graph.traversal().V().count().next();
				
				System.out.println("A little after repopulating from an old snapshot, we see: " + vCount + " vertices in the db.");
			} else {
				String emsg = "Bad command passed to DataSnapshot: [" + command + "]";
				System.out.println(emsg);
				System.exit(1);
			}

		} catch (AAIException e) {
			ErrorLogHelper.logError("AAI_6128", e.getMessage());
		} catch (Exception ex) {
			ErrorLogHelper.logError("AAI_6128", ex.getMessage());
		} finally {
			if (!dbClearFlag && graph != null) {
				// Any changes that worked correctly should have already done
				// thier commits.
				if (graph.isOpen()) {
					graph.tx().rollback();
					graph.close();
				}
			}
			try {
				baos.close();
			} catch (IOException iox) {
			}
		}

		System.exit(0);

	}// End of main()

}

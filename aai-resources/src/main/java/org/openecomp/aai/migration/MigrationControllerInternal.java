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

package org.openecomp.aai.migration;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.apache.activemq.broker.BrokerService;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.io.IoCore;
import org.openecomp.aai.db.props.AAIProperties;
import org.openecomp.aai.dbmap.AAIGraph;
import org.openecomp.aai.dbmap.DBConnectionType;
import org.openecomp.aai.exceptions.AAIException;
import org.openecomp.aai.introspection.Loader;
import org.openecomp.aai.introspection.LoaderFactory;
import org.openecomp.aai.introspection.ModelType;
import org.openecomp.aai.introspection.Version;
import org.openecomp.aai.serialization.engines.QueryStyle;
import org.openecomp.aai.serialization.engines.TitanDBEngine;
import org.openecomp.aai.serialization.engines.TransactionalGraphEngine;
import org.openecomp.aai.util.AAIConstants;
import org.openecomp.aai.util.FormatDate;
import org.reflections.Reflections;
import org.slf4j.MDC;

import com.att.eelf.configuration.Configuration;
import com.att.eelf.configuration.EELFLogger;
import com.att.eelf.configuration.EELFManager;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;

/**
 * Runs a series of migrations from a defined directory based on the presence of 
 * the {@link com.openecomp.aai.migration.Enabled Enabled} annotation
 * 
 * It will also write a record of the migrations run to the database.
 */
public class MigrationControllerInternal {

	private EELFLogger logger;
	private final int DANGER_ZONE = 10;
	private final String vertexType = "migration-list-1707";
	private final List<String> resultsSummary = new ArrayList<>();
	private BrokerService broker;
	private final List<NotificationHelper> notifications = new ArrayList<>();
	private final String snapshotLocation = AAIConstants.AAI_HOME + AAIConstants.AAI_FILESEP + "logs" + AAIConstants.AAI_FILESEP + "data" + AAIConstants.AAI_FILESEP + "migrationSnapshots";
	/**
	 * The main method.
	 *
	 * @param args
	 *            the arguments
	 */
	public void run(String[] args) {
		// Set the logging file properties to be used by EELFManager
		Properties props = System.getProperties();
		props.setProperty(Configuration.PROPERTY_LOGGING_FILE_NAME, "migration-logback.xml");
		props.setProperty(Configuration.PROPERTY_LOGGING_FILE_PATH, AAIConstants.AAI_HOME_ETC_APP_PROPERTIES);

		logger = EELFManager.getInstance().getLogger(MigrationControllerInternal.class.getSimpleName());
		MDC.put("logFilenameAppender", MigrationController.class.getSimpleName());

		boolean loadSnapshot = false;

		CommandLineArgs cArgs = new CommandLineArgs();

		JCommander jCommander = new JCommander(cArgs, args);
		jCommander.setProgramName(MigrationController.class.getSimpleName());
		// Set flag to load from snapshot based on the presence of snapshot and
		// graph storage backend of inmemory
		if (cArgs.dataSnapshot != null && !cArgs.dataSnapshot.isEmpty()) {
			try {
				PropertiesConfiguration config = new PropertiesConfiguration(cArgs.config);
				if (config.getString("storage.backend").equals("inmemory")) {
					loadSnapshot = true;
					System.setProperty("load.snapshot.file", "true");
					System.setProperty("snapshot.location", cArgs.dataSnapshot);
				}
			} catch (ConfigurationException e) {
				logAndPrint("ERROR: Could not load titan configuration.\n" + ExceptionUtils.getFullStackTrace(e));
				return;
			}
		}
		System.setProperty("realtime.db.config", cArgs.config);
		logAndPrint("\n\n---------- Connecting to Graph ----------");
		AAIGraph.getInstance();

		logAndPrint("---------- Connection Established ----------");
		Version version = AAIProperties.LATEST;
		QueryStyle queryStyle = QueryStyle.TRAVERSAL;
		ModelType introspectorFactoryType = ModelType.MOXY;
		Loader loader = LoaderFactory.createLoaderForVersion(introspectorFactoryType, version);
		TransactionalGraphEngine engine = new TitanDBEngine(queryStyle, DBConnectionType.REALTIME, loader);
		
		if (cArgs.help) {
			jCommander.usage();
			engine.rollback();
			return;
		} else if (cArgs.list) {
			Reflections reflections = new Reflections("org.openecomp.aai.migration");
			Set<Class<? extends Migrator>> migratorClasses = findClasses(reflections);
			List<Migrator> migratorList = createMigratorList(cArgs, migratorClasses);

			sortList(migratorList);
			engine.startTransaction();
			System.out.println("---------- List of all migrations ----------");
			migratorList.forEach(migrator -> {
				boolean enabledAnnotation = migrator.getClass().isAnnotationPresent(Enabled.class);
				String enabled = enabledAnnotation ? "Enabled" : "Disabled";
				StringBuilder sb = new StringBuilder();
				sb.append(migrator.getClass().getSimpleName() + " " + enabled);
				sb.append(" ");
				sb.append("[" + getDbStatus(migrator.getClass().getSimpleName(), engine) + "]");
				System.out.println(sb.toString());
			});
			engine.rollback();
			System.out.println("---------- Done ----------");
			return;
		}

		
		Reflections reflections = new Reflections("org.openecomp.aai.migration");

		logAndPrint("---------- Looking for migration scripts to be executed. ----------");
		Set<Class<? extends Migrator>> migratorClasses = findClasses(reflections);
		List<Migrator> migratorList = createMigratorList(cArgs, migratorClasses);

		sortList(migratorList);

		if (!cArgs.scripts.isEmpty() && migratorList.size() == 0) {
			logAndPrint("\tERROR: Failed to find migrations " + cArgs.scripts + ".");
			logAndPrint("---------- Done ----------");
		}

		logAndPrint("\tFound " + migratorList.size() + " migration scripts.");
		logAndPrint("---------- Executing Migration Scripts ----------");

		
		
		takeSnapshotIfRequired(engine, cArgs, migratorList);

		for (Migrator migratorClass : migratorList) {
			String name = migratorClass.getClass().getSimpleName();
			Migrator migrator;
			if (migratorClass.getClass().isAnnotationPresent(Enabled.class)) {
				
				try {
					engine.startTransaction();
					if (!cArgs.forced && hasAlreadyRun(name, engine)) {
						logAndPrint("Migration " + name + " has already been run on this database and will not be executed again. Use -f to force execution");
						continue;
					}
					migrator = migratorClass.getClass().getConstructor(TransactionalGraphEngine.class).newInstance(engine);
				} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
					logAndPrint("EXCEPTION caught initalizing migration class " + migratorClass.getClass().getSimpleName() + ".\n" + ExceptionUtils.getFullStackTrace(e));
					engine.rollback();
					continue;
				}
				logAndPrint("\tRunning " + migratorClass.getClass().getSimpleName() + " migration script.");
				logAndPrint("\t\t See " + System.getProperty("AJSC_HOME") + "/logs/migration/" + migratorClass.getClass().getSimpleName() + "/* for logs.");
				MDC.put("logFilenameAppender", migratorClass.getClass().getSimpleName() + "/" + migratorClass.getClass().getSimpleName());
	
				migrator.run();
		
				commitChanges(engine, migrator, cArgs);
			} else {
				logAndPrint("\tSkipping " + migratorClass.getClass().getSimpleName() + " migration script because it has been disabled.");
			}
		}
		MDC.put("logFilenameAppender", MigrationController.class.getSimpleName());
		for (NotificationHelper notificationHelper : notifications) {
			try {
				notificationHelper.triggerEvents();
			} catch (AAIException e) {
				logAndPrint("\tcould not event");
				logger.error("could not event", e);
			}
		}
		logAndPrint("---------- Done ----------");

		// Save post migration snapshot if snapshot was loaded
		generateSnapshot(engine, "post");
		
		outputResultsSummary();
	}

	private String getDbStatus(String name, TransactionalGraphEngine engine) {
		if (hasAlreadyRun(name, engine)) {
			return "Already executed in this env";
		}
		return "Will be run on next execution";
	}

	private boolean hasAlreadyRun(String name, TransactionalGraphEngine engine) {
		return engine.asAdmin().getReadOnlyTraversalSource().V().has(AAIProperties.NODE_TYPE, vertexType).has(name, true).hasNext();
	}
	private Set<Class<? extends Migrator>> findClasses(Reflections reflections) {
		Set<Class<? extends Migrator>> migratorClasses = reflections.getSubTypesOf(Migrator.class);
		migratorClasses.remove(PropertyMigrator.class);
		return migratorClasses;
	}


	private void takeSnapshotIfRequired(TransactionalGraphEngine engine, CommandLineArgs cArgs, List<Migrator> migratorList) {

		/*int sum = 0;
		for (Migrator migrator : migratorList) {
			if (migrator.getClass().isAnnotationPresent(Enabled.class)) {
				sum += migrator.getDangerRating();
			}
		}
		
		if (sum >= DANGER_ZONE) {
			
			logAndPrint("Entered Danger Zone. Taking snapshot.");
		}*/
		
		//always take snapshot for now
		generateSnapshot(engine, "pre");

	}


	private List<Migrator> createMigratorList(CommandLineArgs cArgs,
			Set<Class<? extends Migrator>> migratorClasses) {
		List<Migrator> migratorList = new ArrayList<>();

		for (Class<? extends Migrator> migratorClass : migratorClasses) {
			if (!cArgs.scripts.isEmpty() && !cArgs.scripts.contains(migratorClass.getSimpleName())) {
				continue;
			} else {
				Migrator migrator;
				try {

					migrator = migratorClass.getConstructor().newInstance();
				} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
					logAndPrint("EXCEPTION caught initalizing migration class " + migratorClass.getSimpleName() + ".\n" + ExceptionUtils.getFullStackTrace(e));
					continue;
				}
				migratorList.add(migrator);
			}
		}
		return migratorList;
	}


	private void sortList(List<Migrator> migratorList) {
		Collections.sort(migratorList, new Comparator<Migrator>() {
			public int compare(Migrator m1, Migrator m2) {
				try {
					
					if (m1.getPriority() > m2.getPriority()) {
						return 1;
					} else if (m1.getPriority() < m2.getPriority()) {
						return -1;
					} else {
						return m1.getClass().getSimpleName().compareTo(m2.getClass().getSimpleName());
					}
				} catch (Exception e) {
					return 0;
				}
			}
		});
	}

	
	private void generateSnapshot(TransactionalGraphEngine engine, String phase) {
		
		FormatDate fd = new FormatDate("yyyyMMddHHmm", "GMT");
		String dateStr= fd.getDateTime();
		String fileName = snapshotLocation + File.separator + phase + "Migration." + dateStr + ".graphson";
		logAndPrint("Saving snapshot of inmemory graph " + phase + " migration to " + fileName);
		Graph transaction = null;
		try {
			
			Path pathToFile = Paths.get(fileName);
			if (!pathToFile.toFile().exists()) {
				Files.createDirectories(pathToFile.getParent());
			}
			transaction = engine.startTransaction();
			transaction.io(IoCore.graphson()).writeGraph(fileName);
			engine.rollback();
		} catch (IOException e) {
			logAndPrint("ERROR: Could not write in memory graph to " + phase + "Migration file. \n" + ExceptionUtils.getFullStackTrace(e));
			engine.rollback();
		} 

		logAndPrint( phase + " migration snapshot saved to " + fileName);
	}
	/**
	 * Log and print.
	 *
	 * @param logger
	 *            the logger
	 * @param msg
	 *            the msg
	 */
	protected void logAndPrint(String msg) {
		System.out.println(msg);
		logger.info(msg);
	}

	/**
	 * Commit changes.
	 *
	 * @param g
	 *            the g
	 * @param migrator
	 *            the migrator
	 * @param logger
	 *            the logger
	 */
	protected void commitChanges(TransactionalGraphEngine engine, Migrator migrator, CommandLineArgs cArgs) {

		String simpleName = migrator.getClass().getSimpleName();
		String message;
		if (migrator.getStatus().equals(Status.FAILURE)) {
			message = "Migration " + simpleName + " Failed. Rolling back.";
			logAndPrint("\t" + message);
			migrator.rollback();
		} else if (migrator.getStatus().equals(Status.CHECK_LOGS)) {
			message = "Migration " + simpleName + " encountered an anomily, check logs. Rolling back.";
			logAndPrint("\t" + message);
			migrator.rollback();
		} else {
			MDC.put("logFilenameAppender", simpleName + "/" + migrator.getClass().getSimpleName());

			if (cArgs.commit) {
				if (!engine.asAdmin().getTraversalSource().V().has(AAIProperties.NODE_TYPE, vertexType).hasNext()) {
					engine.asAdmin().getTraversalSource().addV(AAIProperties.NODE_TYPE, vertexType).iterate();
				}
				engine.asAdmin().getTraversalSource().V().has(AAIProperties.NODE_TYPE, vertexType)
				.property(simpleName, true).iterate();
				MDC.put("logFilenameAppender", MigrationController.class.getSimpleName());
				notifications.add(migrator.getNotificationHelper());
				migrator.commit();
				message = "Migration " + simpleName + " Succeeded. Changes Committed.";
				logAndPrint("\t"+ message +"\t");
			} else {
				message = "--commit not specified. Not committing changes for " + simpleName + " to database.";
				logAndPrint("\t" + message);
				migrator.rollback();
			}

		}
		
		resultsSummary.add(message);

	}
	
	private void outputResultsSummary() {
		logAndPrint("---------------------------------");
		logAndPrint("-------------Summary-------------");
		for (String result : resultsSummary) {
			logAndPrint(result);
		}
		logAndPrint("---------------------------------");
		logAndPrint("---------------------------------");
	}
	
}

class CommandLineArgs {

	@Parameter(names = "--help", help = true)
	public boolean help;

	@Parameter(names = "-c", description = "location of configuration file")
	public String config;

	@Parameter(names = "-m", description = "names of migration scripts")
	public List<String> scripts = new ArrayList<>();

	@Parameter(names = "-l", description = "list the status of migrations")
	public boolean list = false;
	
	@Parameter(names = "-d", description = "location of data snapshot", hidden = true)
	public String dataSnapshot;
	
	@Parameter(names = "-f", description = "force migrations to be rerun")
	public boolean forced = false;
	
	@Parameter(names = "--commit", description = "commit changes to graph")
	public boolean commit = false;

}

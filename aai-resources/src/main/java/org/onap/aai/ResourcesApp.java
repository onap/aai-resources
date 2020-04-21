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
package org.onap.aai;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.onap.aai.aailog.logs.AaiDebugLog;
import org.onap.aai.config.SpringContextAware;
import org.onap.aai.dbmap.AAIGraph;
import org.onap.aai.exceptions.AAIException;
import org.onap.aai.logging.ErrorLogHelper;
import org.onap.aai.nodes.NodeIngestor;
import org.onap.aai.restclient.PropertyPasswordConfiguration;
import org.onap.aai.util.AAIConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.env.Environment;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

@SpringBootApplication(
	exclude = {
		DataSourceAutoConfiguration.class,
		DataSourceTransactionManagerAutoConfiguration.class,
		HibernateJpaAutoConfiguration.class
	}
)
// Component Scan provides a way to look for spring beans
// It only searches beans in the following packages
// Any method annotated with @Bean annotation or any class
// with @Component, @Configuration, @Service will be picked up
@ComponentScan(basePackages = {
     "org.onap.aai.config",
     "org.onap.aai.web",
     "org.onap.aai.setup",
     "org.onap.aai.tasks",
     "org.onap.aai.service",
     "org.onap.aai.rest",
     "org.onap.aai.aaf",
     "org.onap.aai.TenantIsolation",
     "org.onap.aai.aailog",
     "org.onap.aai.prevalidation"
})
public class ResourcesApp {

	private static final Logger logger = LoggerFactory.getLogger(ResourcesApp.class.getName());

	private static final String APP_NAME = "aai-resources";
	public static final String BUNDLECONFIG_DIR = "BUNDLECONFIG_DIR";
	private static AaiDebugLog debugLog = new AaiDebugLog();
	static {
		debugLog.setupMDC();
	}

	@Autowired
	private Environment env;

	@Autowired
	private NodeIngestor nodeIngestor;
	
	@Autowired
	private SpringContextAware context;
	
	@Autowired
	private SpringContextAware loaderFactory;
	
	
	@PostConstruct
	private void init() {
		System.setProperty("org.onap.aai.serverStarted", "false");
		setDefaultProps();
		logger.info("AAI Server initialization started...");

		// Setting this property to allow for encoded slash (/) in the path parameter
		// This is only needed for tomcat keeping this as temporary
		System.setProperty("org.apache.tomcat.util.buf.UDecoder.ALLOW_ENCODED_SLASH", "true");

	    logger.info("Starting AAIGraph connections and the NodeInjestor");

	    if(env.acceptsProfiles(Profiles.TWO_WAY_SSL) && env.acceptsProfiles(Profiles.ONE_WAY_SSL)){
	        logger.warn("You have seriously misconfigured your application");
	    }

	}

	@PreDestroy
	public void cleanup(){
		logger.info("Shutting down both realtime and cached connections");
		AAIGraph.getInstance().graphShutdown();
	}

	public static void main(String[] args) throws AAIException {

	    setDefaultProps();
	    
		Environment env = null;
		AAIConfig.init();
		try {
			SpringApplication app = new SpringApplication(ResourcesApp.class);
			app.setLogStartupInfo(false);
			app.setRegisterShutdownHook(true);
			app.addInitializers(new PropertyPasswordConfiguration());
			env = app.run(args).getEnvironment();
		}
		catch(Exception ex){
		    AAIException aai = null;
			if(ex.getCause() instanceof AAIException){
				aai = (AAIException)ex.getCause();
			} else {
				aai = schemaServiceExceptionTranslator(ex);
			}
			logger.error("Problems starting the ResourcesApp due to {}", aai.getMessage());
			ErrorLogHelper.logException(aai);
			throw aai;
		}

		logger.info(
			"Application '{}' is running on {}!" ,
			env.getProperty("spring.application.name"),
			env.getProperty("server.port")
		);

		// The main reason this was moved from the constructor is due
		// to the SchemaGenerator needs the bean and during the constructor
		// the Spring Context is not yet initialized

		AAIConfig.init();
		AAIGraph.getInstance();

		logger.info("Resources MicroService Started");
		logger.debug("Resources MicroService Started");
	}

	public static void setDefaultProps(){

		if (System.getProperty("file.separator") == null) {
			System.setProperty("file.separator", "/");
		}

		String currentDirectory = System.getProperty("user.dir");
		System.setProperty("aai.service.name", ResourcesApp.class.getSimpleName());

		if (System.getProperty("AJSC_HOME") == null) {
			System.setProperty("AJSC_HOME", ".");
		}

		if(currentDirectory.contains(APP_NAME)){
			if (System.getProperty(BUNDLECONFIG_DIR) == null) {
				System.setProperty(BUNDLECONFIG_DIR, "src/main/resources");
			}
		} else {
			if (System.getProperty(BUNDLECONFIG_DIR) == null) {
				System.setProperty(BUNDLECONFIG_DIR, "aai-resources/src/main/resources");
			}
		}
	}
	public static AAIException schemaServiceExceptionTranslator(Exception ex) {
		AAIException aai = null;
		String message = ExceptionUtils.getRootCause(ex).getMessage();
		if(message.contains("NodeIngestor")){
			aai = new  AAIException("AAI_3026","Error reading OXM from SchemaService - Investigate");
		}
		else if(message.contains("EdgeIngestor")){
			aai = new  AAIException("AAI_3027","Error reading EdgeRules from SchemaService - Investigate");
		}
		else if(message.contains("Connection refused")){
			aai = new  AAIException("AAI_3025","Error connecting to SchemaService - Investigate");
		}
		else {
			aai = new  AAIException("AAI_3025","Unable to determine what the error is, please check external.log");
		}

		return aai;
	}
}

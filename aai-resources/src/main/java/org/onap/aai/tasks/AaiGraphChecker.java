/*
 * ============LICENSE_START=======================================================
 * Copyright (C) 2022 Bell Canada
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ============LICENSE_END=========================================================
 */

package org.onap.aai.tasks;

import com.google.common.collect.Iterators;

import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.Validate;
import org.janusgraph.core.JanusGraphException;
import org.janusgraph.core.JanusGraphTransaction;
import org.janusgraph.core.JanusGraphVertex;
import org.onap.aai.dbmap.AAIGraph;
import org.onap.aai.exceptions.AAIException;
import org.onap.aai.logging.ErrorLogHelper;
import org.onap.aai.util.AAIConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Singleton class responsible to check that AAI service is able to connect to its back-end database.
 * The check can run as a scheduled task or on demand.
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
public class AaiGraphChecker extends TimerTask {

    private static final Logger LOGGER = LoggerFactory.getLogger(AaiGraphChecker.class);

    // Default indicator to enable or disable scheduled task
    private static final String DEFAULT_SCHEDULE_ENABLED_VALUE = "false";
    // Default delay, in seconds, before the scheduled task is started, if enabled
    private static final String DEFAULT_SCHEDULE_DELAY_VALUE = "5";
    // Default period, in seconds, between two consecutive executions of the scheduled task, if enabled
    private static final String DEFAULT_SCHEDULE_PERIOD_VALUE = "60";

    // Database availability cached indicator
    private volatile Boolean isAaiGraphDbAvailableCache = null;

    private Timer timer = null;

    /**
     * Enumeration of check type that can be made.
     */
    public enum CheckerType {
        ACTUAL, CACHED
    }

    private AaiGraphChecker() {
    }

    @PostConstruct
    private void setupTimer() {

        boolean scheduleEnabled = Boolean.parseBoolean(
                getConfigurationValueOrDefault("aai.graph.checker.task.enabled", DEFAULT_SCHEDULE_ENABLED_VALUE));
        long scheduleDelay = Long.parseLong(
                getConfigurationValueOrDefault("aai.graph.checker.task.delay", DEFAULT_SCHEDULE_DELAY_VALUE));
        long schedulePeriod = Long.parseLong(
                getConfigurationValueOrDefault("aai.graph.checker.task.period", DEFAULT_SCHEDULE_PERIOD_VALUE));
        LOGGER.debug("Setting up AaiGraphChecker with scheduleEnabled={}, scheduleDelay={}, schedulePeriod={} ",
                scheduleEnabled, scheduleDelay, schedulePeriod);

        if (scheduleEnabled) {
            timer = new Timer();
            timer.schedule(this, scheduleDelay * 1000, schedulePeriod * 1000);
        }
    }

    @PreDestroy
    private void tearDownTimer() {
        LOGGER.debug("Tear down AaiGraphChecker");
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    @Override
    public void run() {
        isAaiGraphDbAvailable(CheckerType.ACTUAL);
    }

    /**
     * Clear database availability cached indicator.
     */
    public void clearDbAvailabilityCachedIndicator() {
        isAaiGraphDbAvailableCache = null;
    }

    /**
     * Indicate if AAI Graph database is available either from actual db connection or from cached property state.
     * 
     * @param checkerType the type of check to be made (actual or cached). Null is not supported.
     * @return
     *         <li>true, if database is available</li>
     *         <li>false, if database is NOT available</li>
     *         <li>null, if database availability can not be determined</li>
     */
    public Boolean isAaiGraphDbAvailable(CheckerType checkerType) {
        Validate.notNull(checkerType);
        if (CheckerType.ACTUAL.equals(checkerType)) {
            isAaiGraphDbAvailableCache = isAaiGraphDbAvailableActual();
        }
        logDbState(checkerType);
        return isAaiGraphDbAvailableCache;
    }

    private Boolean isAaiGraphDbAvailableActual() {
        Boolean dbAvailable;
        JanusGraphTransaction transaction = null;
        try {
            transaction = AAIGraph.getInstance().getGraph().newTransaction();
            final Iterator<JanusGraphVertex> vertexIterator = transaction.query().limit(1).vertices().iterator();
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Number of vertices retrieved while checking db: {}", Iterators.size(vertexIterator));
            }
            vertexIterator.hasNext();
            LOGGER.debug("Actual database availability is true");
            dbAvailable = Boolean.TRUE;
        } catch (JanusGraphException e) {
            String message = "Actual database availability is false (after JanusGraph exception)";
            ErrorLogHelper.logError("500", message + ": " + e.getMessage());
            LOGGER.error(message, e);
            dbAvailable = Boolean.FALSE;
        } catch (Error e) {
            // Following error occurs when aai resources is starting:
            // - UnsatisfiedLinkError (for org.onap.aai.dbmap.AAIGraph$Helper instantiation)
            // Following errors are raised when aai resources is starting and cassandra is not running:
            // - ExceptionInInitializerError
            // - NoClassDefFoundError (definition for org.onap.aai.dbmap.AAIGraph$Helper is not found)
            String message = "Actual database availability is false (after error)";
            ErrorLogHelper.logError("500", message + ": " + e.getMessage());
            LOGGER.error(message, e);
            dbAvailable = Boolean.FALSE;
        } catch (Exception e) {
            String message = "Actual database availability can not be determined";
            ErrorLogHelper.logError("500", message + ": " + e.getMessage());
            LOGGER.error(message, e);
            dbAvailable = null;
        } finally {
            if (transaction != null && !transaction.isClosed()) {
                // check if transaction is open then close instead of flag
                try {
                    transaction.rollback();
                } catch (Exception e) {
                    String message = "Exception occurred while closing transaction";
                    LOGGER.error(message, e);
                    ErrorLogHelper.logError("500", message + ": " + e.getMessage());
                }
            }
        }
        return dbAvailable;
    }

    private void logDbState(CheckerType type) {
        if (BooleanUtils.isTrue(isAaiGraphDbAvailableCache)) {
            LOGGER.debug("Database is available from {} check.", type);
        } else if (BooleanUtils.isFalse(isAaiGraphDbAvailableCache)) {
            LOGGER.error("Database is NOT available from {} check.", type);
        } else {
            LOGGER.error("Database availability is UNKNOWN from {} check.", type);
        }
    }

    private String getConfigurationValueOrDefault(String property, String defaultValue) {
        String result;
        try {
            result = AAIConfig.get(property);
        } catch (AAIException e) {
            LOGGER.error("Unable to get defined configuration value for '{}' property, then default '{}' value is used",
                    property, defaultValue);
            result = defaultValue;
        }
        return result;
    }

}

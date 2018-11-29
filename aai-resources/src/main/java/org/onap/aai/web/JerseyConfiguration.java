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
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ============LICENSE_END=========================================================
 */
package org.onap.aai.web;

import static java.lang.Boolean.parseBoolean;
import static java.util.Comparator.comparingInt;

import java.lang.reflect.AnnotatedElement;
import java.util.Collection;
import java.util.Comparator;
import java.util.Set;
import java.util.logging.Logger;
import javax.annotation.Priority;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseFilter;
import org.glassfish.jersey.filter.LoggingFilter;
import org.glassfish.jersey.server.ResourceConfig;
import org.onap.aai.rest.BulkAddConsumer;
import org.onap.aai.rest.BulkProcessConsumer;
import org.onap.aai.rest.ExampleConsumer;
import org.onap.aai.rest.LegacyMoxyConsumer;
import org.onap.aai.rest.URLFromVertexIdConsumer;
import org.onap.aai.rest.VertexIdConsumer;
import org.onap.aai.rest.bulk.BulkSingleTransactionConsumer;
import org.onap.aai.rest.util.EchoResponse;
import org.reflections.Reflections;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;

@Configuration
public class JerseyConfiguration {

    private static final Logger log = Logger.getLogger(JerseyConfiguration.class.getName());

    private static final String LOGGING_ENABLED_PROPERTY = "aai.request.logging.enabled";
    private static final String INTERCEPTOR_PACKAGE = "org.onap.aai.interceptors";
    private static final boolean ENABLE_RESPONSE_LOGGING = false;

    private final Environment environment;
    private final ResourceConfig resourceConfig = new ResourceConfig();

    @Autowired
    JerseyConfiguration(Environment environment) {
        this.environment = environment;
    }

    @Bean
    public ResourceConfig resourceConfig() {
        resourceConfig.register(EchoResponse.class);
        resourceConfig.register(VertexIdConsumer.class);
        resourceConfig.register(ExampleConsumer.class);
        resourceConfig.register(BulkAddConsumer.class);
        resourceConfig.register(BulkProcessConsumer.class);
        resourceConfig.register(BulkSingleTransactionConsumer.class);
        resourceConfig.register(LegacyMoxyConsumer.class);
        resourceConfig.register(URLFromVertexIdConsumer.class);

        registerFiltersForClasses(ContainerRequestFilter.class, ContainerResponseFilter.class);

        if (loggingEnabled()) {
            logRequests();
        }

        return resourceConfig;
    }

    private void registerFiltersForClasses(Class<?>... classes) {
        Reflections reflections = new Reflections(INTERCEPTOR_PACKAGE);
        for (Class<?> clazz : classes) {
            registerFiltersFor(clazz, reflections);
        }
    }

    private <T> void registerFiltersFor(Class<T> clazz, Reflections reflections) {
        Set<Class<? extends T>> filters = reflections.getSubTypesOf(clazz);

        throwIfPriorityAnnotationAbsent(filters);

        filters.stream()
            .filter(this::isEnabledByActiveProfiles)
            .sorted(priorityComparator())
            .forEach(resourceConfig::register);
    }

    private <T> void throwIfPriorityAnnotationAbsent(Collection<Class<? extends T>> classes) {
        for (Class clazz : classes) {
            if (!clazz.isAnnotationPresent(Priority.class)) {
                throw new RuntimeException(
                    "Container filter " + clazz.getName() + " does not have @Priority annotation");
            }
        }
    }

    private <T> Comparator<Class<? extends T>> priorityComparator() {
        return comparingInt(clazz -> clazz.getAnnotation(Priority.class).value());
    }

    private void logRequests() {
        resourceConfig.register(new LoggingFilter(log, ENABLE_RESPONSE_LOGGING));
    }

    private boolean loggingEnabled() {
        return parseBoolean(environment.getProperty(LOGGING_ENABLED_PROPERTY));
    }

    private boolean isEnabledByActiveProfiles(AnnotatedElement annotatedElement) {
        return !annotatedElement.isAnnotationPresent(Profile.class) ||
            environment.acceptsProfiles(annotatedElement.getAnnotation(Profile.class).value());
    }
}
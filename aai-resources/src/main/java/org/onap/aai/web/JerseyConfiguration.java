/**
 * ============LICENSE_START=======================================================
 * org.onap.aai
 * ================================================================================
 * Copyright © 2017-2018 AT&T Intellectual Property. All rights reserved.
 * Modifications Copyright (C) 2026 Deutsche Telekom AG.
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

package org.onap.aai.web;

import static java.lang.Boolean.parseBoolean;
import static java.util.Comparator.comparingInt;

import com.google.common.collect.Sets;

import java.lang.reflect.AnnotatedElement;
import java.util.Collection;
import java.util.Comparator;
import java.util.Set;
import java.util.logging.Logger;

import jakarta.annotation.Priority;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.ReaderInterceptor;

import org.glassfish.jersey.logging.LoggingFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.onap.aai.rest.BulkAddConsumer;
import org.onap.aai.rest.BulkProcessConsumer;
import org.onap.aai.rest.ExampleConsumer;
import org.onap.aai.rest.ResourcesController;
import org.onap.aai.rest.URLFromVertexIdConsumer;
import org.onap.aai.rest.VertexIdConsumer;
import org.onap.aai.rest.bulk.BulkSingleTransactionConsumer;
import org.onap.aai.rest.util.EchoResponse;
import org.onap.logging.filter.base.AuditLogContainerFilter;
import org.reflections.Reflections;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

@Configuration
public class JerseyConfiguration {

    private static final Logger log = Logger.getLogger(JerseyConfiguration.class.getName());

    private static final String LOGGING_ENABLED_PROPERTY = "aai.request.logging.enabled";
    private static final String INTERCEPTOR_PACKAGE = "org.onap.aai.interceptors";
    private static final String LOGGING_INTERCEPTOR_PACKAGE = "org.onap.aai.aailog.filter";
    private static final boolean ENABLE_RESPONSE_LOGGING = false;

    private final Reflections reflections = new Reflections(INTERCEPTOR_PACKAGE);
    private final Reflections loggingReflections = new Reflections(LOGGING_INTERCEPTOR_PACKAGE);
    private final Environment environment;
    private final ListableBeanFactory beanFactory;

    public JerseyConfiguration(Environment environment, ListableBeanFactory beanFactory) {
        this.environment = environment;
        this.beanFactory = beanFactory;
    }

    @Bean
    public ResourceConfig resourceConfig() {
        ResourceConfig resourceConfig = new ResourceConfig();

        Set<Class<?>> classes = Sets.newHashSet(EchoResponse.class, VertexIdConsumer.class, ExampleConsumer.class,
                BulkAddConsumer.class, BulkProcessConsumer.class, BulkSingleTransactionConsumer.class,
                ResourcesController.class, URLFromVertexIdConsumer.class);
        resourceConfig.registerClasses(classes);
        registerFiltersForClasses(resourceConfig, ContainerRequestFilter.class, ContainerResponseFilter.class,
                ReaderInterceptor.class,
                AuditLogContainerFilter.class);

        if (isLoggingEnabled()) {
            logRequests(resourceConfig);
        }

        return resourceConfig;
    }

    private void registerFiltersForClasses(ResourceConfig resourceConfig, Class<?>... classes) {
        for (Class<?> clazz : classes) {
            registerFiltersFor(clazz, resourceConfig);
        }
    }

    private <T> void registerFiltersFor(Class<T> clazz, ResourceConfig resourceConfig) {
        Set<Class<? extends T>> filters = loggingReflections.getSubTypesOf(clazz);
        filters.addAll(reflections.getSubTypesOf(clazz));
        throwIfPriorityAnnotationAbsent(filters);

        filters.stream().filter(this::isEnabledByActiveProfiles).filter(this::isSpringBeanPresent)
                .sorted(priorityComparator())
                .forEach(resourceConfig::register);
    }

    private <T> void throwIfPriorityAnnotationAbsent(Collection<Class<? extends T>> classes) {
        for (Class<? extends T> clazz : classes) {
            if (!clazz.isAnnotationPresent(Priority.class)) {
                throw new MissingFilterPriorityException(clazz);
            }
        }
    }

    private <T> Comparator<Class<? extends T>> priorityComparator() {
        return comparingInt(clazz -> clazz.getAnnotation(Priority.class).value());
    }

    private void logRequests(ResourceConfig resourceConfig) {
        if(ENABLE_RESPONSE_LOGGING) {
            resourceConfig.register(new LoggingFeature(log));
        }
    }

    private boolean isLoggingEnabled() {
        return parseBoolean(environment.getProperty(LOGGING_ENABLED_PROPERTY));
    }

    private boolean isEnabledByActiveProfiles(AnnotatedElement annotatedElement) {
        return !annotatedElement.isAnnotationPresent(Profile.class)
                || environment.acceptsProfiles(Profiles.of(annotatedElement.getAnnotation(Profile.class).value()));
    }

    // Filters are registered by class scan, so a @Component filter whose bean a Spring
    // condition (e.g. @ConditionalOnProperty) switched off would otherwise still be handed
    // to Jersey, which cannot construct it outside the Spring context.
    private boolean isSpringBeanPresent(Class<?> clazz) {
        return !clazz.isAnnotationPresent(Component.class) || beanFactory.getBeanNamesForType(clazz).length > 0;
    }

    private class MissingFilterPriorityException extends RuntimeException {
        private MissingFilterPriorityException(Class<?> clazz) {
            super("Container filter " + clazz.getName() + " does not have @Priority annotation");
        }
    }
}

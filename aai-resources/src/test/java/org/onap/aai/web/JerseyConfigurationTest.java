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
package org.onap.aai.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.glassfish.jersey.filter.LoggingFilter;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.Test;
import org.onap.aai.interceptors.post.InvalidResponseStatus;
import org.onap.aai.interceptors.post.ResetLoggingContext;
import org.onap.aai.interceptors.post.ResponseHeaderManipulation;
import org.onap.aai.interceptors.post.ResponseTransactionLogging;
import org.onap.aai.interceptors.pre.HeaderValidation;
import org.onap.aai.interceptors.pre.HttpHeaderInterceptor;
import org.onap.aai.interceptors.pre.RequestHeaderManipulation;
import org.onap.aai.interceptors.pre.RequestModification;
import org.onap.aai.interceptors.pre.RequestTransactionLogging;
import org.onap.aai.interceptors.pre.RetiredInterceptor;
import org.onap.aai.interceptors.pre.SetLoggingContext;
import org.onap.aai.interceptors.pre.VersionInterceptor;
import org.onap.aai.interceptors.pre.VersionLatestInterceptor;
import org.onap.aai.rest.BulkAddConsumer;
import org.onap.aai.rest.BulkProcessConsumer;
import org.onap.aai.rest.ExampleConsumer;
import org.onap.aai.rest.LegacyMoxyConsumer;
import org.onap.aai.rest.URLFromVertexIdConsumer;
import org.onap.aai.rest.VertexIdConsumer;
import org.onap.aai.rest.bulk.BulkSingleTransactionConsumer;
import org.onap.aai.rest.util.EchoResponse;
import org.springframework.core.env.Environment;

public class JerseyConfigurationTest {

    @Test
    public void verify_all_classes_are_registered() {
        // given
        Environment environment = mock(Environment.class);

        // when class is constructed
        List<Class<?>> classCaptor = new ArrayList<>();
        List<Object> objectCaptor = new ArrayList<>();
        JerseyConfiguration jerseyConfiguration = JerseyConfigurationCaptor(environment, classCaptor, objectCaptor);

        // then
        List<Class<?>> expectedClasses = Arrays.asList(
            EchoResponse.class, VertexIdConsumer.class, ExampleConsumer.class, BulkAddConsumer.class,
            BulkProcessConsumer.class, BulkSingleTransactionConsumer.class, LegacyMoxyConsumer.class,
            URLFromVertexIdConsumer.class, RequestTransactionLogging.class, HeaderValidation.class,
            SetLoggingContext.class, HttpHeaderInterceptor.class, VersionLatestInterceptor.class,
            RetiredInterceptor.class, VersionInterceptor.class, RequestHeaderManipulation.class,
            RequestModification.class, InvalidResponseStatus.class, ResetLoggingContext.class,
            ResponseTransactionLogging.class, ResponseHeaderManipulation.class
        );

        assertEquals(expectedClasses.size(), classCaptor.size());
        assertTrue(classCaptor.containsAll(expectedClasses));
        assertTrue(expectedClasses.stream().allMatch(jerseyConfiguration::isRegistered));
    }

    @Test
    public void when_logging_enabled_should_register_logging_filter() {
        // given
        Environment environment = mock(Environment.class);
        when(environment.getProperty("aai.request.logging.enabled")).thenReturn("true");

        // when class is constructed
        List<Class<?>> classCaptor = new ArrayList<>();
        List<Object> objectCaptor = new ArrayList<>();
        JerseyConfiguration jerseyConfiguration = JerseyConfigurationCaptor(environment, classCaptor, objectCaptor);

        assertEquals(1, objectCaptor.size());
        assertTrue(objectCaptor.get(0) instanceof LoggingFilter);
    }

    @Test
    public void when_logging_disabled_should_register_nothing() {
        // given
        Environment environment = mock(Environment.class);
        when(environment.getProperty("aai.request.logging.enabled")).thenReturn("false");

        // when class is constructed
        List<Class<?>> classCaptor = new ArrayList<>();
        List<Object> objectCaptor = new ArrayList<>();
        JerseyConfiguration jerseyConfiguration = JerseyConfigurationCaptor(environment, classCaptor, objectCaptor);

        assertTrue(objectCaptor.isEmpty());
    }

    // FIXME
    // as tested class calls super method there is no way to test it with usage of regular Mockito constructs
    // this is a quick workaround to test behaviour of class before refactoring
    // after all problematic inheritance should be dropped in favor of class composition or even better @Bean creation
    private JerseyConfiguration JerseyConfigurationCaptor(Environment environment, List<Class<?>> capturedClasses,
        List<Object> capturedObjects) {
        return new JerseyConfiguration(environment) {
            @Override
            public ResourceConfig register(Class<?> componentClass) {
                capturedClasses.add(componentClass);
                return super.register(componentClass);
            }

            @Override
            public ResourceConfig register(Object component) {
                capturedObjects.add(component);
                return super.register(component);
            }
        };
    }
}
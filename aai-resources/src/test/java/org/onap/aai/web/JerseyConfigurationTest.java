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

import static org.mockito.Mockito.mock;

import java.util.Arrays;
import java.util.List;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.Assert;
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

        // when
        ResourceConfig resourceConfig = new JerseyConfiguration(environment).resourceConfig();

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

        expectedClasses
            .stream()
            .filter(clazz -> !resourceConfig.isRegistered(clazz))
            .findFirst()
            .ifPresent(clazz -> Assert.fail("Not registered: " + clazz));
    }
}
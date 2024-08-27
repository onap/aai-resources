/**
 * ============LICENSE_START=======================================================
 * org.onap.aai
 * ================================================================================
 * Copyright © 2024 Deutsche Telekom. All rights reserved.
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

package org.onap.aai.rest.interceptors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.nio.charset.Charset;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.MediaType;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.onap.aai.interceptors.pre.NamespaceInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.PropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(
  webEnvironment = WebEnvironment.RANDOM_PORT,
  properties = { "aai.remove-xmlns.enabled=true" }
)
public class NamespaceInterceptorTest {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private NamespaceInterceptor namespaceInterceptor;

    @Test
    public void thatNamespaceInterceptorIsActive() {
        webTestClient.put().uri("/test").exchange().expectStatus().isCreated();
    }

    @Test
    void testNamespaceInterceptor() throws Exception {
      String input = "<pserver xmlns=\"http://org.onap.aai.inventory/v11\">\r\n    <hostname>someHostname</hostname>\r\n    <ptnii-equip-name>example-ptnii-equip-name-val-35940</ptnii-equip-name>\r\n    <number-of-cpus>100</number-of-cpus>\r\n</pserver>";
      String expected = "<pserver>\r\n    <hostname>someHostname</hostname>\r\n    <ptnii-equip-name>example-ptnii-equip-name-val-35940</ptnii-equip-name>\r\n    <number-of-cpus>100</number-of-cpus>\r\n</pserver>";

      ByteArrayInputStream inputStream = namespaceInterceptor.removeNameSpace(new InputStreamReader(new ByteArrayInputStream(input.getBytes())));

      StringWriter writer = new StringWriter();
      IOUtils.copy(inputStream, writer, Charset.defaultCharset());
      String actual = writer.toString();
      assertEquals(expected, actual);
    }
}

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

import static org.junit.Assert.assertThat;
import static org.xmlunit.matchers.CompareMatcher.isIdenticalTo;

import org.xmlunit.builder.Input;
import org.xmlunit.input.WhitespaceStrippedSource;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.nio.charset.Charset;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.onap.aai.interceptors.pre.NamespaceInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
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

    // @Test
    // public void thatNamespaceInterceptorIsActive() {
    //     webTestClient.put().uri("/test").exchange().expectStatus().isCreated();
    // }

    @Test
    void testNamespaceInterceptor() throws Exception {
      String input = "<pserver xmlns=\"http://org.onap.aai.inventory/v11\">\r\n    <hostname>someHostname</hostname>\r\n    <ptnii-equip-name>example-ptnii-equip-name-val-35940</ptnii-equip-name>\r\n    <number-of-cpus>100</number-of-cpus>\r\n</pserver>";
      String expected = "<pserver>\r\n    <hostname>someHostname</hostname>\r\n    <ptnii-equip-name>example-ptnii-equip-name-val-35940</ptnii-equip-name>\r\n    <number-of-cpus>100</number-of-cpus>\r\n</pserver>";

      InputStreamReader xmlReader = new InputStreamReader(new ByteArrayInputStream(input.getBytes()));
      ByteArrayInputStream inputStream = namespaceInterceptor.removeNameSpace(xmlReader);
      StringWriter writer = new StringWriter();
      IOUtils.copy(inputStream, writer, Charset.defaultCharset());
      String actual = writer.toString();

      // Document expectedDoc = parseXml(expected);
      // Document actualDoc = parseXml(actual);


      assertThat(actual, isIdenticalTo(new WhitespaceStrippedSource(Input.from(expected).build())));
      // assertThat(actual, isIdenticalTo(new WhitespaceStrippedSource(Input.from(expected).build())));

      // assertTrue(actualDoc.isEqualNode(expectedDoc));
    }

    // private Document parseXml(String xml) throws ParserConfigurationException, SAXException, IOException {
    //   DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    //   dbf.setNamespaceAware(false);
    //   dbf.setCoalescing(true);
    //   dbf.setIgnoringElementContentWhitespace(true);
    //   dbf.setIgnoringComments(true);
    //   DocumentBuilder db = dbf.newDocumentBuilder();

    //   Document doc = db.parse(new InputSource(new StringReader(xml)));
    //   doc.normalizeDocument();
    //   return doc;
    // }
}

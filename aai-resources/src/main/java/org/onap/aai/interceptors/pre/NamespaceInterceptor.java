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
package org.onap.aai.interceptors.pre;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;

import javax.annotation.Priority;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.ReaderInterceptor;
import javax.ws.rs.ext.ReaderInterceptorContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.onap.aai.IncreaseNodesTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

/**
 * With newer versions of EclipseLink Moxy, the xmlns attribute cannot be included
 * in the xml structure anymore.
 * Changes to the model-loader and babel would be required to 'properly' remove
 * it for the model-distribution.
 * Since the impact of such a change is hard to judge, this workaround is taken
 * that is less invasive and with a lower risk of breaking model distribution.
 *
 * @deprecated This is only meant as a temporary compatibility layer and will be removed in the future
 * once all clients have been updated to not include the xmlns attribute.
 *
 */
@Deprecated
@Component
@Priority(AAIRequestFilterPriority.REQUEST_MODIFICATION)
@ConditionalOnProperty(value = "aai.remove-xmlns.enabled", havingValue = "true", matchIfMissing = true)
public class NamespaceInterceptor implements ReaderInterceptor {

    private static final Logger log = LoggerFactory.getLogger(IncreaseNodesTool.class);
    private static final String xslStr = String.join("\n",
        "<xsl:transform xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\" version=\"1.0\">",
        "<xsl:output version=\"1.0\" encoding=\"UTF-8\" indent=\"no\"/>",
        "<xsl:strip-space elements=\"*\"/>",
        "  <xsl:template match=\"@*|node()\">",
        "   <xsl:element name=\"{local-name()}\">",
        "     <xsl:apply-templates select=\"@*|node()\"/>",
        "  </xsl:element>",
        "  </xsl:template>",
        "  <xsl:template match=\"text()\">",
        "    <xsl:copy/>",
        "  </xsl:template>",
        "</xsl:transform>");

    @Override
    public Object aroundReadFrom(ReaderInterceptorContext context) throws IOException, WebApplicationException {
        if(MediaType.APPLICATION_XML.equalsIgnoreCase(context.getMediaType().toString())) {
            Reader xmlReader = new InputStreamReader(context.getInputStream());
            try {
                ByteArrayInputStream inputStream = removeNameSpace(xmlReader);
                context.setInputStream(inputStream);
                return context.proceed();
            } catch (Exception e) {
                log.error("Could not remove namespace from model payload: " + e.getMessage());
                return context.proceed();
            }
        }
        return context.proceed();
    }

    /**
     * Temporary solution to removing the xmlns attribute from the model payload.
     * The payload is coming from babel and removing it there would be some larger effort.
     * As such, this workaround is applied.
     * Taken from: https://stackoverflow.com/questions/37354605/how-to-remove-xmlns-attribute-from-the-root-element-in-xml-and-java#answer-37357777
     * @throws Exception
     */
    public ByteArrayInputStream removeNameSpace(Reader xmlReader) throws Exception {
        // Parse XML and Build Document
        DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        InputSource is = new InputSource();
        is.setCharacterStream(xmlReader);
        Document doc = db.parse(is);

        // Parse XSLT and Configure Transformer
        Source xslt = new StreamSource(new StringReader(xslStr));
        Transformer tf = TransformerFactory.newInstance().newTransformer(xslt);
        tf.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");

        // Output Result to String
        DOMSource source = new DOMSource(doc);
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        StreamResult strresult = new StreamResult(outStream);
        tf.transform(source, strresult);

        return new ByteArrayInputStream(outStream.toByteArray());
    }
}

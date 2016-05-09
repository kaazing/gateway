/**
 * Copyright 2007-2016, Kaazing Corporation. All rights reserved.
 *
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
 */
package org.kaazing.gateway.server;

import org.junit.Test;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import static org.junit.Assert.assertEquals;

/**
 * Created by cochiriac on 5/9/16.
 */
public class SettingsTest {

    /**
     * Checks in the projects's pom.xml that the manifest entries will be generated as expected by others (update.check)
     */
    @Test
    public void shouldHaveCommunityProductEditionAndTitle() {

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse("../server/pom.xml");
            XPathFactory xPathfactory = XPathFactory.newInstance();
            XPath xpath = xPathfactory.newXPath();
            XPathExpression expr = xpath.compile("/project/build/pluginManagement/plugins/plugin[2]/configuration/archive/manifestEntries/Implementation-Title/text()");
            assertEquals("Kaazing Gateway", expr.evaluate(doc, XPathConstants.STRING));
            expr = xpath.compile("/project/build/pluginManagement/plugins/plugin[2]/configuration/archive/manifestEntries/Kaazing-Product/text()");
            assertEquals("Community.Gateway", expr.evaluate(doc, XPathConstants.STRING));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

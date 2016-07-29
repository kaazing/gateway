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

package org.kaazing.gateway.server.config.parse.translate;

import static org.junit.Assert.*;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.junit.Test;
import org.kaazing.gateway.server.config.parse.GatewayConfigNamespace;

public class RemoveRequireUserTest {
    
    private final String TRANSLATED_CONFIG_FILE_EXT = ".new";
    
    private File createTempFileFromResource(String resourceName) throws IOException {
        File file = File.createTempFile("gateway-config", "xml");
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        InputStream is = classLoader.getResource(resourceName).openStream();
        FileOutputStream fos = new FileOutputStream(file);
        int datum;
        while ((datum = is.read()) != -1) {
            fos.write(datum);
        }
        fos.flush();
        fos.close();
        return file;
    }
    
    private Document getDocument(File configFile) throws Exception {
        SAXBuilder xmlReader = new SAXBuilder();
        Document dom = xmlReader.build(configFile);
        return dom;
    }
    
    private GatewayConfigTranslator getSpecificTranslator(Document dom) throws Exception {
        GatewayConfigTranslator translator; 
        GatewayConfigNamespace namespace =  GatewayConfigNamespace.fromURI(dom.getRootElement().getNamespace().getURI());
        translator = GatewayConfigTranslatorFactory.newInstance().getTranslator(namespace);
        return translator;
    }
    
    private void writeTranslationToFile(File configFile, Document dom) throws IOException {
        File translatedConfigFile = new File(configFile.getParent(), configFile.getName() + TRANSLATED_CONFIG_FILE_EXT);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        BufferedOutputStream bos = new BufferedOutputStream(baos);
        Format outputFormat = Format.getPrettyFormat();
        outputFormat.setLineSeparator(System.getProperty("line.separator"));
        XMLOutputter xmlWriter = new XMLOutputter(outputFormat);
        xmlWriter.output(dom, bos);
        bos.close();
        final String xml = baos.toString();
        FileWriter fw = new FileWriter(translatedConfigFile);
        BufferedWriter bw = new BufferedWriter(fw);
        bw.write(xml);
        bw.close();
    }
    
    private boolean ensureNoAuth(Element element, Namespace namespace) {
        Element typeElement = element.getChild("realm-name", namespace);
        if (typeElement != null) {
            boolean check = element.removeChildren("authorization-constraint", namespace);
            if (check) {
                return true;
            }
        }
        return false;
    }
    
    @Test
    public void testNoRemoveRealmNov2015() throws Exception {
        // Create a temporary file to test on
        File configFile = null;
        configFile = createTempFileFromResource("org/kaazing/gateway/server/config/parse/data/gateway-config-no-remove-realm-test.xml");
        
        // Converting it into a document
        Document dom = getDocument(configFile);
        GatewayConfigTranslator translator = getSpecificTranslator(dom);
        translator.translate(dom);
        
        // Writing to a new file to read
        writeTranslationToFile(configFile, dom);

        File translatedFile = new File(configFile.getAbsolutePath() + TRANSLATED_CONFIG_FILE_EXT);
        Document translatedDom = getDocument(translatedFile);
        Element root = translatedDom.getRootElement();
        Namespace namespace = root.getNamespace();
        List<Element> children = dom.getRootElement().getChildren("service", namespace);
        for (Element child : children) {
            assertTrue(ensureNoAuth(child, namespace));
        }
    }
    
    @Test
    public void testRemoveRealmNov2015() throws Exception {
        // Create a temporary file to test on
        File configFile = null;
        configFile = createTempFileFromResource("org/kaazing/gateway/server/config/parse/data/gateway-config-yes-remove-realm-test.xml");
        
        // Converting it into a document
        Document dom = getDocument(configFile);
        GatewayConfigTranslator translator = getSpecificTranslator(dom);
        translator.translate(dom);
        
        // Writing to a new file to read
        writeTranslationToFile(configFile, dom);

        File translatedFile = new File(configFile.getAbsolutePath() + TRANSLATED_CONFIG_FILE_EXT);
        Document translatedDom = getDocument(translatedFile);
        Element root = translatedDom.getRootElement();
        Namespace namespace = root.getNamespace();
        List<Element> children = dom.getRootElement().getChildren("service", namespace);
        for (Element child : children) {
            boolean check = child.removeChildren("realm-name", namespace);
            assertTrue(!check);
        }
    }
}

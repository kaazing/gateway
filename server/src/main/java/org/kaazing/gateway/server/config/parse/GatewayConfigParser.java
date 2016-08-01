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
package org.kaazing.gateway.server.config.parse;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.ErrorListener;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.xmlbeans.XmlError;
import org.apache.xmlbeans.XmlOptions;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.kaazing.gateway.server.Launcher;
import org.kaazing.gateway.server.config.parse.translate.GatewayConfigTranslator;
import org.kaazing.gateway.server.config.parse.translate.GatewayConfigTranslatorFactory;
import org.kaazing.gateway.server.config.june2016.ClusterType;
import org.kaazing.gateway.server.config.june2016.GatewayConfigDocument;
import org.kaazing.gateway.server.config.june2016.PropertiesType;
import org.kaazing.gateway.server.config.june2016.PropertyType;
import org.kaazing.gateway.server.config.june2016.SecurityType;
import org.kaazing.gateway.server.config.june2016.ServiceDefaultsType;
import org.kaazing.gateway.server.config.june2016.ServiceType;
import org.kaazing.gateway.util.parse.ConfigParameter;
import org.slf4j.Logger;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.ext.DefaultHandler2;
import org.xml.sax.ext.Locator2;
import org.xml.sax.helpers.DefaultHandler;

public class GatewayConfigParser {

    /**
     * XSL stylesheet to be used before parsing. Adds xsi:type to login-module and service elements.
     */
    private static final String GATEWAY_CONFIG_ANNOTATE_TYPES_XSL = "META-INF/gateway-config-annotate-types.xsl";

    /**
     * Charset string for XML prologue, must match {@link #CHARSET_OUTPUT}
     */
    private static final String CHARSET_OUTPUT_XML = "UTF-16";

    /**
     * Charset string for output stream, must match {@link #CHARSET_OUTPUT_XML}
     */
    private static final String CHARSET_OUTPUT = "UTF16";

    /**
     * Extension to add to translated/updated config files
     */
    private static final String TRANSLATED_CONFIG_FILE_EXT = ".new";

    private static final Logger LOGGER = Launcher.getGatewayStartupLogger();

    private final Properties configuration;

    public GatewayConfigParser() {
        this(System.getProperties());
    }

    public GatewayConfigParser(Properties configuration) {
        this.configuration = configuration;
    }


    private void translate(final GatewayConfigNamespace ns,
                           final Document dom,
                           final File translatedConfigFile,
                           boolean writeTranslatedFile)
            throws Exception {

        GatewayConfigTranslator translator = GatewayConfigTranslatorFactory.newInstance().getTranslator(ns);
        translator.translate(dom);

        if (writeTranslatedFile) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            BufferedOutputStream bos = new BufferedOutputStream(baos);

            Format outputFormat = Format.getPrettyFormat();
            outputFormat.setLineSeparator(System.getProperty("line.separator"));

            XMLOutputter xmlWriter = new XMLOutputter(outputFormat);
            xmlWriter.output(dom, bos);
            bos.close();

            final String xml = baos.toString();

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(String.format("Translated gateway config XML:\n%s", xml));
            }

            // Write the translated DOM out to the given file
            FileWriter fw = new FileWriter(translatedConfigFile);
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write(xml);
            bw.close();
        }
    }

    private File getTranslatedConfigFile(final File configFile)
            throws Exception {

        // Build a DOM of the config file, so that we can easily sniff the
        // namespace used.  We then key off the namespace and attempt to
        // Do The Right Thing(tm).

        SAXBuilder xmlReader = new SAXBuilder();
        Document dom = xmlReader.build(configFile);
        Element root = dom.getRootElement();
        GatewayConfigNamespace namespace =  GatewayConfigNamespace.fromURI(root.getNamespace().getURI());
        checkForNoLongerSupported(root); 
        boolean writeTranslatedFile = !namespace.equals(GatewayConfigNamespace.CURRENT_NS);
        File translatedConfigFile = writeTranslatedFile ?
                new File(configFile.getParent(), configFile.getName()
                + TRANSLATED_CONFIG_FILE_EXT) : configFile;

        translate(namespace, dom, translatedConfigFile, writeTranslatedFile);
        return translatedConfigFile;
    }
    
    private void checkForNoLongerSupported(Element root) throws Exception {
        Namespace namespace = root.getNamespace();
        List<Element> children = root.getChildren("service", namespace);
        for (Element child : children) {
            Element typeChild = child.getChild("type", namespace);
            String type = typeChild.getText();
            if (type.equals("management.snmp")) {
                throw new Exception("snmp management type is no longer supported."); 
            } else if (type.equals("session")) {
                throw new Exception("session service type is no longer supported.");
            }
        }
        
    }

    /**
     * Parse and validate a gateway configuration file.
     *
     * @param configFile the configuration file
     * @return GatewayConfig the parsed gateway configuration
     * @throws Exception when a problem occurs
     */
    public GatewayConfigDocument parse(final File configFile) throws Exception {
        long time = 0;
        if (LOGGER.isDebugEnabled()) {
            time = System.currentTimeMillis();
        }

        // For errors and logging (KG-1379) we need to report the real config file name,
        // which is not always 'gateway-config.xml'.
        String configFileName = configFile.getName();

        // Validate the gateway-config
        GatewayConfigDocument config = null;
        XmlOptions parseOptions = new XmlOptions();
        parseOptions.setLoadLineNumbers();
        parseOptions.setLoadLineNumbers(XmlOptions.LOAD_LINE_NUMBERS_END_ELEMENT);
        parseOptions.setLoadStripWhitespace();
        parseOptions.setLoadStripComments();

        File translatedConfigFile;
        try {
            translatedConfigFile = getTranslatedConfigFile(configFile);
        } catch (Exception e) {
            Throwable rootCause = getRootCause(e);
            if (rootCause == null) {
                rootCause = e;
            }

            if (LOGGER.isDebugEnabled()) {
                LOGGER.error("Error upgrading XML: " + rootCause, rootCause);

            } else {
                LOGGER.error("Error upgrading XML: " + rootCause);
            }

            // If it's not an IllegalArgumentException, wrap it in a
            // GatewayConfigParserException
            if (e instanceof IllegalArgumentException) {
                throw e;

            } else {
                throw new GatewayConfigParserException(e.getMessage());
            }
        }

        List<String> xmlParseErrors = new ArrayList<>();
        try {
            config = GatewayConfigDocument.Factory.parse(new FileInputStream(translatedConfigFile), parseOptions);

        } catch (Exception e) {
            // track the parse error so that we don't make the 2nd pass through the file
            xmlParseErrors.add("Invalid XML: " + getRootCause(e).getMessage());
        }

        if (xmlParseErrors.isEmpty()) {
            // The properties used in parameter substitution are now proper XMLBeans
            // and should be injected after an initial parse
            GatewayConfigDocument.GatewayConfig gatewayConfig = config.getGatewayConfig();
            PropertiesType properties = gatewayConfig.getProperties();
            Map<String, String> propertiesMap = new HashMap<>();
            if (properties != null) {
                for (PropertyType propertyType : properties.getPropertyArray()) {
                    propertiesMap.put(propertyType.getName(), propertyType.getValue());
                }
            }

            // make a second pass through the file now, injecting the properties and performing XSL translations
            InputStream xmlInjectedIn = new PipedInputStream();
            OutputStream xmlInjectedOut = new PipedOutputStream((PipedInputStream) xmlInjectedIn);
            ExecutorService xmlInjectedExecutor = Executors.newSingleThreadExecutor();
            Future<Boolean> xmlInjectedFuture = xmlInjectedExecutor.submit(new XMLParameterInjector(new FileInputStream(
                    translatedConfigFile), xmlInjectedOut, propertiesMap, configuration, xmlParseErrors));

            // trace injected xml
            if (LOGGER.isTraceEnabled()) {
                xmlInjectedIn = bufferToTraceLog(xmlInjectedIn,
                        "Gateway config file '" + configFileName + "' post parameter injection", LOGGER);
            }

            // Pass gateway-config through the pre-parse transformer
            InputStream xmlTransformedIn = new PipedInputStream();
            OutputStream xmlTransformedOut = new PipedOutputStream((PipedInputStream) xmlTransformedIn);
            ExecutorService xmlTransformedExecutor = Executors.newSingleThreadExecutor();
            Future<Boolean> xmlTransformedFuture = xmlTransformedExecutor.submit(
                    new XSLTransformer(xmlInjectedIn, xmlTransformedOut, GATEWAY_CONFIG_ANNOTATE_TYPES_XSL));

            // trace transformed xml
            if (LOGGER.isTraceEnabled()) {
                xmlTransformedIn = bufferToTraceLog(xmlTransformedIn,
                        "Gateway config file '" + configFileName + "' post XSL transformation", LOGGER);
            }

            try {
                config = GatewayConfigDocument.Factory.parse(xmlTransformedIn, parseOptions);
            } catch (Exception e) {
                // If parsing with previous namespace was also unsuccessful,
                // process errors top down, failing fast, for user level errors
               try {
                    if (xmlInjectedFuture.get()) {
                        if (xmlTransformedFuture.get()) {
                            throw e;
                        }
                    }
                } catch (Exception n) {
                    xmlParseErrors.add("Invalid XML: " + getRootCause(n).getMessage());
                }
            } finally {
                xmlInjectedFuture.cancel(true);
                xmlInjectedExecutor.shutdownNow();
                xmlTransformedFuture.cancel(true);
                xmlTransformedExecutor.shutdownNow();
            }
        }

        validateGatewayConfig(config, xmlParseErrors);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("parsed " + " gateway config file '" + configFileName + "' in [" + (System.currentTimeMillis() - time) +
                    " ms]");
        }

        return config;
    }

    /**
     * Validate the parsed gateway configuration file.
     *
     * @param configDoc the XmlObject representing the gateway-config document
     */
    private void validateGatewayConfig(GatewayConfigDocument configDoc, List<String> preProcessErrors) {
        List<XmlError> errorList = new ArrayList<>();
        for (String preProcessError : preProcessErrors) {
            errorList.add(XmlError.forMessage(preProcessError, XmlError.SEVERITY_ERROR));
        }
        if (errorList.isEmpty()) {
            XmlOptions validationOptions = new XmlOptions();
            validationOptions.setLoadLineNumbers();
            validationOptions.setLoadLineNumbers(XmlOptions.LOAD_LINE_NUMBERS_END_ELEMENT);
            validationOptions.setErrorListener(errorList);
            boolean valid = configDoc.validate(validationOptions);
            if (valid) {
                // Perform custom validations that aren't expressed in the XSD
                GatewayConfigDocument.GatewayConfig config = configDoc.getGatewayConfig();

                ServiceType[] services = config.getServiceArray();
                if (services != null && services.length > 0) {
                    List<String> serviceNames = new ArrayList<>();
                    for (ServiceType service : services) {
                        String name = service.getName();
                        if (name == null || name.length() == 0) {
                            errorList.add(XmlError.forMessage("All services must have unique non-empty names",
                                    XmlError.SEVERITY_ERROR));
                        } else if (serviceNames.indexOf(name) >= 0) {
                            errorList.add(XmlError
                                    .forMessage("Service name must be unique. More than one service named '" + name + "'",
                                            XmlError.SEVERITY_ERROR));
                        } else {
                            serviceNames.add(name);
                        }
                    }
                }

                SecurityType[] security = config.getSecurityArray();
                if (security != null && security.length > 1) {
                    errorList.add(XmlError.forMessage("Multiple <security> elements found; only one allowed",
                            XmlError.SEVERITY_ERROR));
                }
                ServiceDefaultsType[] serviceDefaults = config.getServiceDefaultsArray();
                if (serviceDefaults != null && serviceDefaults.length > 1) {
                    errorList.add(XmlError.forMessage("Multiple <service-defaults> elements found; only one allowed",
                            XmlError.SEVERITY_ERROR));
                }
                ClusterType[] clusterConfigs = config.getClusterArray();
                if (clusterConfigs != null && clusterConfigs.length > 1) {
                    errorList.add(XmlError.forMessage("Multiple <cluster> elements found; only one allowed",
                            XmlError.SEVERITY_ERROR));
                }
            }
        }

        // Report all validation errors
        if (errorList.size() > 0) {
            String validationError = "Validation errors in gateway configuration file";
            LOGGER.error(validationError);
            for (XmlError error : errorList) {
                int line = error.getLine();
                if (line != -1) {
                    int column = error.getColumn();
                    if (column == -1) {
                        LOGGER.error("  Line: " + line);
                    } else {
                        LOGGER.error("  Line: " + line + " Column: " + column);
                    }
                }
                LOGGER.error("  " + error.getMessage().replaceAll("@" + GatewayConfigNamespace.CURRENT_NS, ""));
                if (error.getMessage().contains("notify-options") || error.getMessage().contains("notify")) {
                    validationError = "Could not start because of references to APNs in the configuration."
                     + " APNs is not supported in this version of the gateway, but will be added in a future release.";
                    LOGGER.error(validationError);

                }
                if (error.getMessage().contains("DataRateString")) {
                    // Yeah, it's crude, but customers are going to keep tripping over cases like 100KB/s being invalid otherwise
                    // Example output:
                    // ERROR - Validation errors in gateway configuration file
                    // ERROR -   Line: 12 Column: 36
                    // ERROR -   string value '1m' does not match pattern for DataRateString in namespace http://xmlns.kaazing
                    // .com/2012/08/gateway
                    // ERROR -   (permitted data rate units are B/s, kB/s, KiB/s, kB/s, MB/s, and MiB/s)
                    // ERROR -   <xml-fragment xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                    LOGGER.error("  " + "(permitted data rate units are B/s, kB/s, KiB/s, kB/s, MB/s, and MiB/s)");
                }
                if (error.getCursorLocation() != null) {
                    LOGGER.error("  " + error.getCursorLocation().xmlText());
                }
            }
            throw new GatewayConfigParserException(validationError);
        }
    }

    /**
     * Get the root cause from a <code>Throwable</code> stack
     *
     * @param throwable
     * @return
     */
    private static Throwable getRootCause(Throwable throwable) {
        List<Throwable> list = new ArrayList<>();
        while (throwable != null && !list.contains(throwable)) {
            list.add(throwable);
            throwable = throwable.getCause();
        }
        return list.get(list.size() - 1);
    }

    /**
     * Buffer a stream, flushing it to <code>log</code> and returning it as input
     *
     * @param input
     * @param message
     * @param log
     * @return
     */
    private static InputStream bufferToTraceLog(InputStream input, String message, Logger log) {
        InputStream output;
        try {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            int read;
            byte[] data = new byte[16384];
            while ((read = input.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, read);
            }
            buffer.flush();
            log.trace(message + "\n\n\n" + new String(buffer.toByteArray(), CHARSET_OUTPUT) + "\n\n\n");
            output = new ByteArrayInputStream(buffer.toByteArray());
        } catch (Exception e) {
            throw new RuntimeException("could not buffer stream", e);
        }
        return output;
    }

    /**
     * Count the number of new lines
     *
     * @param ch
     * @param start
     * @param length
     */
    private static int countNewLines(char[] ch, int start, int length) {
        int newLineCount = 0;
        // quite reliable, since only Commodore 8-bit machines, TRS-80, Apple II family, Mac OS up to version 9 and OS-9
        // use only '\r'
        for (int i = start; i < length; i++) {
            newLineCount = newLineCount +  ((ch[i] == '\n') ? 1 : 0);
        }
        return newLineCount;
    }

    /**
     * Inject resolved parameter values into XML stream
     */
    private static final class XMLParameterInjector implements Callable<Boolean> {

        private InputStream souceInput;
        private OutputStreamWriter injectedOutput;
        private Map<String, String> properties;
        private Properties configuration;
        private List<String> errors;
        private int currentFlushedLine = 1;

        public XMLParameterInjector(InputStream souceInput, OutputStream injectedOutput, Map<String, String> properties,
                                    Properties configuration, List<String> errors)
                throws UnsupportedEncodingException {
            this.souceInput = souceInput;
            this.injectedOutput = new OutputStreamWriter(injectedOutput, CHARSET_OUTPUT_XML);
            this.properties = properties;
            this.configuration = configuration;
            this.errors = errors;
        }

        private void write(char[] ch, int start, int length) {
            try {
                currentFlushedLine += countNewLines(ch, start, length);
                injectedOutput.write(ch, start, length);
                injectedOutput.flush();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        private void write(char[] ch) {
            write(ch, 0, ch.length);
        }

        private void write(String s) {
            write(s.toCharArray(), 0, s.length());
        }

        private void close() {
            try {
                souceInput.close();
                injectedOutput.flush();
                injectedOutput.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        /**
         * Parse the config file, resolving and injecting parameters encountered
         *
         * @return <code>true</code> if processed without errors, <code>false</code> otherwise
         */
        @Override
        public Boolean call() throws Exception {
            try {
                SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
                DefaultHandler handler = new DefaultHandler2() {

                    private Locator2 locator;

                    private void realignElement() {
                        String realignment = "";
                        for (int i = 0; i < locator.getLineNumber() - currentFlushedLine; i++) {
                            realignment += System.getProperty("line.separator");
                        }
                        write(realignment);
                    }

                    @Override
                    public void setDocumentLocator(Locator locator) {
                        this.locator = (Locator2) locator;
                    }

                    @Override
                    public void startDocument() throws SAXException {
                        write("<?xml version=\"1.0\" encoding=\"" + CHARSET_OUTPUT_XML + "\" ?>" +
                                System.getProperty("line.separator"));
                    }

                    @Override
                    public void startElement(String uri, String localName, String qName, Attributes attributes)
                            throws SAXException {
                        realignElement();
                        String elementName = (localName == null || localName.equals("")) ? qName : localName;
                        write("<" + elementName);
                        if (attributes != null) {
                            for (int i = 0; i < attributes.getLength(); i++) {
                                String attributeName = (attributes.getLocalName(i) == null || attributes
                                        .getLocalName(i).equals("")) ? attributes.getQName(i) : attributes
                                                               .getLocalName(i);
                                write(" " + attributeName + "=\"");
                                char[] attributeValue = attributes.getValue(i).toCharArray();
                                write(ConfigParameter.resolveAndReplace(attributeValue, 0,
                                        attributeValue.length, properties, configuration, errors) + "\"");
                            }
                        }
                        write(new char[]{'>'});
                    }

                    @Override
                    public void comment(char[] ch, int start, int length) throws SAXException {
                        write("<!--");
                        write(ch, start, length);
                        write("-->");
                    }

                    @Override
                    public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
                        write(ch, start, length);
                    }

                    @Override
                    public void characters(char[] ch, int start, int length) throws SAXException {
                        write(ConfigParameter.resolveAndReplace(ch, start, length, properties, configuration, errors));
                    }

                    @Override
                    public void endElement(String uri, String localName, String qName) throws SAXException {
                        realignElement();
                        String elementName = (localName == null || localName.equals("")) ? qName : localName;
                        write("</" + elementName + ">");
                    }

                };
                parser.getXMLReader().setProperty("http://xml.org/sax/properties/lexical-handler", handler);
                parser.getXMLReader().setProperty("http://apache.org/xml/properties/input-buffer-size",
                        souceInput.available());
                parser.parse(souceInput, handler);
            } finally {
                close();
            }
            return errors.size() == 0;
        }
    }

    /**
     * XSL Transformer.
     */
    private static final class XSLTransformer implements Callable<Boolean> {

        private InputStream streamToTransform;
        private OutputStream transformerOutput;
        private String stylesheet;

        /**
         * Constructor.
         *
         * @param streamToTransform the gateway configuration file to transform
         * @param transformerOutput the output stream to be used for transformed output
         */
        public XSLTransformer(InputStream streamToTransform, OutputStream transformerOutput, String stylesheet) {
            this.streamToTransform = streamToTransform;
            this.transformerOutput = transformerOutput;
            this.stylesheet = stylesheet;
        }

        /**
         * Transform the gateway configuration file using the stylesheet.
         *
         * @return <code>true</code> if processed without errors, <code>false</code> otherwise
         */
        @Override
        public Boolean call() throws Exception {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            URL resource = classLoader.getResource(stylesheet);
            InputStream xslIn = resource.openStream();
            try {
                Source xmlSource = new StreamSource(streamToTransform);
                Source xslSource = new StreamSource(xslIn);
                Result xmlResult = new StreamResult(transformerOutput);
                Transformer transformer = TransformerFactory.newInstance().newTransformer(xslSource);
                transformer.setOutputProperty(OutputKeys.ENCODING, CHARSET_OUTPUT_XML);
                transformer.setErrorListener(new ErrorListener() {

                    @Override
                    public void warning(TransformerException exception) throws TransformerException {
                        throw exception;
                    }

                    @Override
                    public void fatalError(TransformerException exception) throws TransformerException {
                        throw exception;
                    }

                    @Override
                    public void error(TransformerException exception) throws TransformerException {
                        throw exception;
                    }

                });
                transformer.transform(xmlSource, xmlResult);
            } finally {
                transformerOutput.flush();
                transformerOutput.close();
                xslIn.close();
            }
            return Boolean.TRUE;
        }
    }
}

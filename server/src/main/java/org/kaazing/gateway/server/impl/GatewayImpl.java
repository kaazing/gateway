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
package org.kaazing.gateway.server.impl;

import static java.lang.String.format;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.Properties;

import javax.management.MBeanServer;

import org.apache.log4j.LogManager;
import org.apache.log4j.config.PropertySetter;
import org.apache.log4j.helpers.FileWatchdog;
import org.apache.log4j.helpers.OptionConverter;
import org.apache.log4j.xml.DOMConfigurator;
import org.kaazing.gateway.server.Gateway;
import org.kaazing.gateway.server.GatewayObserver;
import org.kaazing.gateway.server.Launcher;
import org.kaazing.gateway.server.api.GatewayAlreadyRunningException;
import org.kaazing.gateway.server.config.june2016.GatewayConfigDocument;
import org.kaazing.gateway.server.config.parse.GatewayConfigParser;
import org.kaazing.gateway.server.context.GatewayContext;
import org.kaazing.gateway.server.context.resolve.GatewayContextResolver;
import org.kaazing.gateway.server.util.version.DuplicateJarFinder;
import org.slf4j.Logger;
import org.w3c.dom.Element;

/**
 * <p> Use this class to start and stop a Gateway from Java. </p>
 * <p/>
 * <p> Before creating the Gateway, ensure that the system property <strong>GATEWAY_HOME</strong> has been set and points to the
 * home directory of a Gateway installation. This can be done by specifying a -D parameter when starting on the command line, or
 * dynamically in Java with system properties. For example: </p>
 * <p/>
 * <p> Example as a command line parameter: </p>
 * <p/>
 * <blockquote>-DGATEWAY_HOME=/home/user/gateway</blockquote>
 * <p/>
 * <p> Example using system properties in Java: </p>
 * <p/>
 * <blockquote>System.setProperty("GATEWAY_HOME", "/home/user/gateway");</blockquote>
 */
final class GatewayImpl implements Gateway {
    private static final String DEFAULT_CONFIG_DIRECTORY = "conf/";
    private static final String DEFAULT_TEMP_DIRECTORY = "temp/";
    private static final String DEFAULT_WEB_DIRECTORY = "web/";
    private static final String DEFAULT_GATEWAY_CONFIG_XML = "gateway-config.xml";
    private static final String DEFAULT_GATEWAY_CONFIG_MINIMAL_XML = "gateway-config-minimal.xml";
    private static final String DEFAULT_LOG4J_CONFIG_XML = "log4j-config.xml";
    private static final String DEFAULT_LOG_DIRECTORY = "log/";
    private static final long DEFAULT_LOG_REFRESH_INTERVAL_MILLIS = 60 * 1000;

    private static final Logger LOGGER = Launcher.getGatewayStartupLogger();

    private Properties env;
    private MBeanServer jmxMBeanServer;
    private Launcher gateway;
    private Gateway baseGateway;
    private KaazingFileWatchdog watchDog;
    private final DuplicateJarFinder duplicateJarFinder;

    /**
     * <p> Create a new in-process Gateway instance.
     * <p/>
     * <p> After calling this constructor the Gateway instance will be created but not yet started. Use {@link #launch()} to
     * launch the Gateway. </p>
     *
     * @throws Exception
     */
    public GatewayImpl(Gateway baseGateway) {
        this.baseGateway = baseGateway;
        this.duplicateJarFinder = new DuplicateJarFinder(LOGGER);
    }

    // Configure a new Gateway instance with the given environment properties.
    @Override
    public void setProperties(Properties properties) {
        if (this.env == null) {
            this.env = properties;
        } else {
            this.env.putAll(properties);
        }

        if (baseGateway != null) {
            baseGateway.setProperties(properties);
        }
    }

    @Override
    public Properties getProperties() {
        Properties configuration = new Properties();
        configuration.putAll(env);

        // Since this is a basic implementation, overlay other gateways' config in order to
        // go from generic (System properties) to specific
        if (baseGateway != null) {
            configuration.putAll(baseGateway.getProperties());
        }

        return configuration;
    }

    /**
     * Stop an in-process Gateway that was started using {@link #launch()}.
     *
     * @throws Exception
     */
    @Override
    public void destroy() throws Exception {
        if (baseGateway != null) {
            baseGateway.destroy();
        }

        if (watchDog != null) {
            watchDog.stop();
            watchDog = null;
        }

        if (gateway != null) {
            gateway.destroy();
            gateway = null;
        }
    }

    /**
     * <p> Launch the in-process Gateway. </p>
     *
     * @throws Exception
     */
    @Override
    public void launch() throws Exception {

        if (baseGateway != null) {
            baseGateway.launch();
        }

        if (gateway != null) {
            throw new GatewayAlreadyRunningException("An instance of the Gateway is already running");
        }

        Properties configuration = getProperties();
        if (configuration == null) {
            // Change to a public exception once all calls to System.getProperty() throughout the entire
            // codebase have been eliminated.
            //
            throw new Exception("No environment has been specified");
        }

        if (!supportedJavaVersion(1, 8, "0")) {
            throw new RuntimeException("Unsupported JDK version, Please install Java SE 8.0 or later and relaunch " +
                    "Kaazing WebSocket Gateway");
        }

        String gatewayHomeProperty = configuration.getProperty(GATEWAY_HOME_PROPERTY);
        if (gatewayHomeProperty == null) {
            throw new IllegalArgumentException(GATEWAY_HOME_PROPERTY + " directory was not specified");
        }

        File homeDir = new File(gatewayHomeProperty);
        if (!homeDir.isDirectory()) {
            throw new IllegalArgumentException(GATEWAY_HOME_PROPERTY + " is not a valid directory: "
                    + homeDir.getAbsolutePath());
        }

        String gatewayConfigDirectoryProperty = configuration.getProperty(GATEWAY_CONFIG_DIRECTORY_PROPERTY);
        File configDir = (gatewayConfigDirectoryProperty != null)
                         ? new File(gatewayConfigDirectoryProperty)
                         : new File(homeDir, DEFAULT_CONFIG_DIRECTORY);
        if (!configDir.isDirectory()) {
            throw new IllegalArgumentException(GATEWAY_CONFIG_DIRECTORY_PROPERTY + " is not a valid directory: "
                    + configDir.getAbsolutePath());
        }

        // Login modules needs the CONFIG directory, put it back into the configuration properties.
        configuration.setProperty(GATEWAY_CONFIG_DIRECTORY_PROPERTY, configDir.toString());

        String gatewayTempDirectoryProperty = configuration.getProperty(GATEWAY_TEMP_DIRECTORY_PROPERTY);
        File tempDir = (gatewayTempDirectoryProperty != null)
                       ? new File(gatewayTempDirectoryProperty)
                       : new File(homeDir, DEFAULT_TEMP_DIRECTORY);
        if (!tempDir.isDirectory()) {
            throw new IllegalArgumentException(GATEWAY_TEMP_DIRECTORY_PROPERTY + " is not a valid directory: "
                    + tempDir.getAbsolutePath());
        }

        String gatewayLogDirectoryProperty = configuration.getProperty(GATEWAY_LOG_DIRECTORY_PROPERTY);
        File logDir = (gatewayLogDirectoryProperty != null)
                      ? new File(gatewayLogDirectoryProperty)
                      : new File(homeDir, DEFAULT_LOG_DIRECTORY);
        if (!logDir.exists()) {
            logDir.mkdir();
        }
        if (!logDir.isDirectory()) {
            throw new IllegalArgumentException(GATEWAY_LOG_DIRECTORY_PROPERTY
                    + " is not a valid directory or could not be created: " + logDir.getAbsolutePath());
        }

        // Because we use Log4J and it contains a reference to ${GATEWAY_LOG_DIRECTORY},
        // we need to make sure to put the log directory back into the configuration properties.
        configuration.setProperty(GATEWAY_LOG_DIRECTORY_PROPERTY, logDir.toString());

        // As of 3.2 we will normally have two config files in the config directory (if you
        // have the 'full' installation), one for the 'minimal' configuration (that you would
        // get if you just have a base installation) and one for the demos and docs.  To decide
        // on the config file to use we'll have the following rules:
        //  * If there is a config specified as a system property, we'll use that.
        //  * If there is no config specified, we'll look for the default "full" gateway config.
        //    (of course, the user may have modified that file and just be using the same name.)
        //  * If that doesn't exist, we'll look for the default "minimal" gateway config.
        //  * If none of those exists (and is a readable file), that's an error.
        File gatewayConfigFile;
        String gatewayConfigProperty = configuration.getProperty(GATEWAY_CONFIG_PROPERTY);

        // If config property is a url then we download it and reset the property
        try {
            // if config is a URL download it to config directory
            URL configURL = new URL(gatewayConfigProperty);
            String path = configURL.getPath();
            ReadableByteChannel rbc = Channels.newChannel(configURL.openStream());
            final File configFile = new File(configDir, path.substring(path.lastIndexOf('/') + 1));
            try (FileOutputStream fos = new FileOutputStream(configFile)) {
                fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
            }
            configuration.setProperty(Gateway.GATEWAY_CONFIG_PROPERTY, configFile.getPath());
            gatewayConfigProperty = configuration.getProperty(GATEWAY_CONFIG_PROPERTY);
        } catch (MalformedURLException e1) {
            // expected exception if config is not a url
        } catch (IOException e) {
            throw new RuntimeException("Could not fetch config from url: " + gatewayConfigProperty, e);
        }

        if (gatewayConfigProperty != null) {
            gatewayConfigFile = new File(gatewayConfigProperty);
            if (!gatewayConfigFile.isFile() || !gatewayConfigFile.canRead()) {
                throw new IllegalArgumentException(GATEWAY_CONFIG_PROPERTY
                        + " was specified but is not a valid, readable file: "
                        + gatewayConfigFile.getAbsolutePath());
            }
        } else {
            gatewayConfigFile = new File(configDir, DEFAULT_GATEWAY_CONFIG_XML);
            if (!gatewayConfigFile.exists()) {
                gatewayConfigFile = new File(configDir, DEFAULT_GATEWAY_CONFIG_MINIMAL_XML);
            }
            if (!gatewayConfigFile.isFile() || !gatewayConfigFile.canRead()) {
                throw new IllegalArgumentException(GATEWAY_CONFIG_PROPERTY
                        + " was not specified, and no default readable config file"
                        + " could be found in the conf/ directory");
            }
        }

        String gatewayWebDirectoryProperty = configuration.getProperty(GATEWAY_WEB_DIRECTORY_PROPERTY);
        File webRootDir = (gatewayWebDirectoryProperty != null)
                          ? new File(gatewayWebDirectoryProperty)
                          : new File(homeDir, DEFAULT_WEB_DIRECTORY);
        if (!webRootDir.exists()) {
            webRootDir.mkdir();
        }
        if (!webRootDir.isDirectory()) {
            throw new IllegalArgumentException(GATEWAY_WEB_DIRECTORY_PROPERTY
                    + " is not a valid directory or could not be created: " + webRootDir.getAbsolutePath());
        }

        String overrideLogging = configuration.getProperty(OVERRIDE_LOGGING);
        if ((overrideLogging == null) || !Boolean.parseBoolean(overrideLogging)) {
            configureLogging(configDir, configuration);
        }

        duplicateJarFinder.findDuplicateJars();

        displayVersionInfo();

        LOGGER.info("Configuration file: " + gatewayConfigFile.getCanonicalPath());

        GatewayObserver gatewayObserver = GatewayObserver.newInstance();
        GatewayConfigParser parser = new GatewayConfigParser(configuration);
        GatewayConfigDocument config = parser.parse(gatewayConfigFile);
        GatewayContextResolver resolver = new GatewayContextResolver(configDir, webRootDir, tempDir, jmxMBeanServer);
        gatewayObserver.initingGateway(configuration, resolver.getInjectables());
        resolver.setObserver(gatewayObserver);
        GatewayContext context = resolver.resolve(config, configuration);

        gateway = new Launcher(gatewayObserver);

        try {
            gateway.init(context);
        } catch (Exception e) {
            LOGGER.error(String.format("Error starting Gateway: caught exception %s", e));
            throw e;
        }
    }

    @Override
    public void setMBeanServer(MBeanServer server) {
        this.jmxMBeanServer = server;
        if (baseGateway != null) {
            baseGateway.setMBeanServer(server);
        }
    }

    private void configureLogging(File configDir, Properties configuration) throws Exception {
        // Allow control over whether or not the Gateway logging external to the Gateway so that customers can configure their
        // own logging when embedding the Gateway
        String log4jConfigProperty = configuration.getProperty(GatewayImpl.LOG4J_CONFIG_PROPERTY);
        File log4jConfigFile = (log4jConfigProperty != null)
                               ? new File(log4jConfigProperty)
                               : new File(configDir, GatewayImpl.DEFAULT_LOG4J_CONFIG_XML);
        if (!log4jConfigFile.isFile() || !log4jConfigFile.canRead()) {
            throw new IllegalArgumentException(GatewayImpl.LOG4J_CONFIG_PROPERTY + " is not a valid, readable file: "
                    + log4jConfigFile.getAbsolutePath());
        }

        // configure log4j
        String log4jConfigRefreshInterval = configuration.getProperty(LOG4J_CONFIG_REFRESH_INTERVAL);
        long log4jConfigRefreshIntervalMillis = DEFAULT_LOG_REFRESH_INTERVAL_MILLIS;
        if (log4jConfigRefreshInterval != null) {
            try {
                long newInterval = Long.parseLong(log4jConfigRefreshInterval);
                log4jConfigRefreshIntervalMillis = newInterval * 1000; // convert to millis
            } catch (NumberFormatException nfe) {
                throw new IllegalArgumentException(format("The %s value of %s is not a valid integer value.",
                        LOG4J_CONFIG_REFRESH_INTERVAL, log4jConfigRefreshInterval));
            }
        }

        if (log4jConfigRefreshIntervalMillis <= 0) {
            KaazingDOMConfigurator configurator = new KaazingDOMConfigurator(configuration);
            configurator.doConfigure(log4jConfigFile.toURI().toURL(), LogManager.getLoggerRepository());
        } else {
            watchDog = new KaazingFileWatchdog(log4jConfigFile, configuration);
            watchDog.setDelay(log4jConfigRefreshIntervalMillis);
            watchDog.start();
        }
    }

    private void displayVersionInfo() {
        String gatewayProductTitle = VersionUtils.getGatewayProductTitle();
        String gatewayProductVersion = VersionUtils.getGatewayProductVersion();

        if (gatewayProductVersion == null) {
            // The only case I know of where this happens is the development one.
            // In this case just return the title.
            LOGGER.info(gatewayProductTitle);
        } else {
            LOGGER.info(gatewayProductTitle + " (" + gatewayProductVersion + ")");
        }
    }

    private static boolean supportedJavaVersion(int major, int minor, String point) {
        return supportedJavaVersion(System.getProperty("java.version"), System.getProperty("java.vendor"), major,
                minor, point);
    }

    // package access for unit test
    static boolean supportedJavaVersion(String javaVersion, String javaVendor, int major, int minor, String point) {
        String[] versionParts = javaVersion.split("\\.");
        int currentMajor = Integer.parseInt(versionParts[0]);
        int currentMinor = Integer.parseInt(versionParts[1]);
        String currentPoint = versionParts[2];

        if (currentMajor > major) {
            return true;
        } else if (currentMajor == major) {
            if (currentMinor > minor) {
                return true;
            } else if (currentMinor == minor) {
                // java.version point version is not a version number and for Azul (zing) it does not follow the
                // the number_number... format used by Oracle and Open JDK (e.g. 1.7.0-zing_5.10.1.0). So just
                // allow any zing version that starts with major.minor.
                return javaVendor.startsWith("Azul") || currentPoint.compareTo(point) >= 0;
            }
        }

        return false;
    }

    // Log4J typically replaces ${variable} with the value of System.getProperty("variable").
    // Since the Gateway does not set System properties, we need to do our own variable
    // substitution by looking for these variable strings and replacing them with values
    // from the Gateway's configured properties.
    private static class KaazingDOMConfigurator extends DOMConfigurator {
        private Properties properties;

        KaazingDOMConfigurator(Properties properties) {
            this.properties = properties;
        }

        @Override
        protected void setParameter(Element elem, PropertySetter propSetter) {
            String paramName = OptionConverter.substVars(elem.getAttribute("name"), properties);
            String value = elem.getAttribute("value");
            value = OptionConverter.substVars(OptionConverter.convertSpecialChars(value), properties);
            propSetter.setProperty(paramName, value);
        }
    }

    private static class KaazingFileWatchdog extends FileWatchdog {
        private File log4jConfigFile;
        private Properties properties;

        KaazingFileWatchdog(File log4jConfigFile, Properties properties) {
            super(log4jConfigFile.getAbsolutePath());
            this.log4jConfigFile = log4jConfigFile;
            this.properties = properties;

            // calling super() invokes doOnChange, but without the log4jConfigFile and properties set, so mimic that
            // behavior here to bootstrap the configuring of log4j
            doOnChange();
        }

        @Override
        public void doOnChange() {
            // Calling super() in the constructor ends up calling doOnChange(), which happens before
            // the pointer to the log4jConfigFile is set. Check for null, and skip configuring as
            // the constructor will take care of the first configure.
            if (log4jConfigFile != null) {
                KaazingDOMConfigurator configurator = new KaazingDOMConfigurator(properties);

                try {
                    configurator.doConfigure(log4jConfigFile.toURI().toURL(), LogManager.getLoggerRepository());
                } catch (MalformedURLException ex) {
                    System.err.println("Error configuring LOG4J, unable to load file: "
                            + log4jConfigFile.getAbsolutePath());
                }
            }
        }
    }
}

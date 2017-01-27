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

import java.util.Properties;

import javax.management.MBeanServer;

/**
 * Gateway for configuring, launching, and destroying a Launcher instance.  The GatewayFactory is used
 * to create Gateways, for example:
 *
 * <pre>
 * {@code
 *     Gateway embeddedGateway = GatewayFactory.createGateway();
 * }
 * </pre>
 *
 * <p>
 * Once a Gateway has been created, it can be immediately launched with the default environment.  The
 * default environment is to have <i>GATEWAY_HOME</i> set to the parent directory of the location of the JAR
 * file containing the Gateway class file.  Any configuration files, content files, etc are searched for under
 * this directory.
 * </p>
 *
 * <p>
 * If another environment is desired, or properties in the Gateway's configuration file (e.g. gateway-config.xml)
 * are to be overridden, then invoke the configure(java.util.Properties) method first with the desired properties.
 * </p>
 * @see GatewayFactory
 */
@SuppressWarnings("UnusedDeclaration")
public interface Gateway {
    /**
     * The root directory of the Gateway.  Startup scripts are located in <i>GATEWAY_HOME</i>/bin and Gateway libraries
     * are located in <i>GATEWAY_HOME</i>/lib.  This property can be set to something like /usr/share/kaazing/html5/3.3
     */
    String GATEWAY_HOME_PROPERTY = "GATEWAY_HOME";

    /**
     * The fully qualified path to the configuration file of the Gateway.  For example, /etc/kaazing/conf/gateway-config.xml.
     * This file contains the set of properties and services that will be exposed by the running Gateway.  See "Configuring
     * Kaazing WebSocket Gateway" in the admin guide for more information.
     */
    String GATEWAY_CONFIG_PROPERTY = "GATEWAY_CONFIG";

    /**
     * The location of configuration files used by the Gateway.  For example, /etc/kaazing/conf/.  This directory will
     * be searched for gateway-config.xml if the GATEWAY_CONFIG_PROPERTY is not set.  Any configuration files (e.g. for
     * login modules) are located in this directory.
     */
    String GATEWAY_CONFIG_DIRECTORY_PROPERTY = "GATEWAY_CONFIG_DIRECTORY";

    /**
     * The location of web content served by the Gateway.  For example, /var/lib/kaazing/web/.  This directory serves
     * as the root of any directory service configured in the Gateway.  The directory property of a directory service
     * is treated as a child of the <i>GATEWAY_WEB_DIRECTORY</i>.
     */
    String GATEWAY_WEB_DIRECTORY_PROPERTY = "GATEWAY_WEB_DIRECTORY";

    /**
     * The location of temp files used by the Gateway.  During execution the Gateway may need to create temporary
     * files, and those files are created in this directory.  For example, /var/lib/kaazing/temp/.
     */
    String GATEWAY_TEMP_DIRECTORY_PROPERTY = "GATEWAY_TEMP_DIRECTORY";

    /**
     * The location of the Gateway's log files.  For example, /var/log/kaazing/.
     */
    String GATEWAY_LOG_DIRECTORY_PROPERTY = "GATEWAY_LOG_DIRECTORY";

    /**
     * The location custom code used by the Gateway.  Any JAR files in this directory will be added to the Gateway's
     * classpath.  Custom login-modules or libraries used to access a custom server (e.g. a message broker) would be
     * located in this directory.  For example, /home/kaazing/lib/.
     */
    String GATEWAY_USER_LIB_DIRECTORY_PROPERTY = "GATEWAY_USER_LIB_DIRECTORY";

    /**
     * The fully qualified path to a Log4J configuration file.  See http://logging.apache.org/log4j/ for more information
     * about configuring Log4J.  By default, the Gateway will look for log4j-config.xml in this directory.
     */
    String LOG4J_CONFIG_PROPERTY = "LOG4J_CONFIG";

    /**
     * In order to override the Gateway's default logging configuration this property can be set to <code>true</code>.  By
     * default the Gateway configures Log4J as the logging system, but any logging system that supports SLF4J (see
     * http://www.slf4j.org/ for more information) can be substituted.
     */
    String OVERRIDE_LOGGING = "OVERRIDE_LOGGING";

    /**
     * By default, the Gateway will check for changes in the log4j config file every minute.  When a change is
     * detected, the log levels are refreshed as for the new configuration values.
     *
     * By specifying the LOG4J_CONFIG_REFRESH_INTERVAL the default value can be changed.  The value is an integer
     * that specifies the interval in seconds.  The default is 60 seconds.  By setting the value to 0 the feature
     * is disabled and the Gateway will never check for changes in the log4j config file.
     */
    String LOG4J_CONFIG_REFRESH_INTERVAL = "LOG4J_CONFIG_REFRESH_INTERVAL";

    /**
     * Set up the Gateway with the given properties.  Example of properties used by the Gateway are <code>GATEWAY_HOME</code> and
     * <code>GATEWAY_CONFIG</code>.  The gateway-config.xml configuration file also specifies property names and values that can
     * be overridden by the configured properties.
     * @param properties the name/value pairs used to configure the Gateway by setting the location of the GATEWAY_HOME,
     *        GATEWAY_CONFIG, etc.  and/or the names of properties in the gateway-config.xml and the values to which those
     *        properties should be set.
     */
    void setProperties(Properties properties);

    /**
     * Get the current set of properties used to configure the Gateway.
     * @return The name/value pairs used to configure the Gateway
     */
    Properties getProperties();

    /**
     * Set an MBeanServer on the Gateway.  This will be the MBeanServer the Gateway populates with its management information,
     * eg. information about services configured in the Gateway and current sessions connected to the Gateway.  If an MBeanServer
     * is set on the Gateway then management should not be configured in the Gateway's configuration file.  The presence of
     * management in the configuration file causes the platform MBeanServer to be used for the Gateway's management information.
     * @param server the MBeanServer the Gateway should use to populate with its management information.
     */
    void setMBeanServer(MBeanServer server);

    /**
     * Starts the Gateway, which in turn initializes and starts up all the configured services, including binding to host and
     * port specified in the service's accept tag.  If launch() is called on an already launched Gateway then an exception is
     * thrown.
     *
     * After calling destroy, the Gateway can be launched again.
     *
     * @throws Exception examples of exceptions thrown by the Gateway at launch time:  unable to read the configuration file,
     *                   unable to bind to a network port specified in a service configuration, or a directory service's
     *                   directory property is not a valid directory.
     */
    void launch() throws Exception;

    /**
     * Stops the running Gateway.  If the Gateway has already been destroyed this has no effect.
     * @throws Exception an example of exceptions thrown by the Gateway at destroy time is unable to clean up management
     *                   information from the MBeanServer, which could happen if the MBeanServer is destroyed out from
     *                   underneath the Gateway unexpectedly.
     */
    void destroy() throws Exception;
}

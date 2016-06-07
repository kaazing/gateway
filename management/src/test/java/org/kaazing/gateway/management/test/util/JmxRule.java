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
package org.kaazing.gateway.management.test.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import javax.management.remote.rmi.RMIConnectorServer;
import javax.rmi.ssl.SslRMIClientSocketFactory;
import javax.rmi.ssl.SslRMIServerSocketFactory;

import org.junit.Assert;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

public class JmxRule implements TestRule {

    private final String jmxURI;
    private MBeanServerConnection connection;

    public JmxRule(String jmxURI) {
        this.jmxURI = jmxURI;
    }

    public MBeanServerConnection getConnection() {
        return connection;
    }

    @Override
    public Statement apply(Statement base, Description description) {
        return new JmxStatement(base);
    }

    public class JmxStatement extends Statement {

        private final Statement base;

        public JmxStatement(Statement base) {
            this.base = base;
        }

        @Override
        public void evaluate() throws Throwable {
            /*
             * Confirm system properties are properly set to use JMX
             * 
             * -Dcom.sun.management.jmxremote.authenticate=false
             * -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.local.only=false
             * -Djava.rmi.server.hostname=localhost -Djavax.net.ssl.trustStore=$JAVA_HOME/lib/security/cacerts
             */
            Properties properties = System.getProperties();
            Assert.assertEquals("false", properties.get("com.sun.management.jmxremote.authenticate"));
            Assert.assertEquals("false", properties.get("com.sun.management.jmxremote.ssl"));
            Assert.assertEquals("false", properties.get("com.sun.management.jmxremote.local.only"));
            Assert.assertEquals("localhost", properties.get("java.rmi.server.hostname"));

            Map<String, Object> env = new HashMap<>();
            SslRMIClientSocketFactory csf = new SslRMIClientSocketFactory();
            SslRMIServerSocketFactory ssf = new SslRMIServerSocketFactory();
            env.put(RMIConnectorServer.RMI_CLIENT_SOCKET_FACTORY_ATTRIBUTE, csf);
            env.put(RMIConnectorServer.RMI_SERVER_SOCKET_FACTORY_ATTRIBUTE, ssf);
            env.put(JMXConnector.CREDENTIALS, new String[]{"admin", "admin"});
            JMXConnector jmxConnector = JMXConnectorFactory.connect(new JMXServiceURL(jmxURI), env);
            connection = jmxConnector.getMBeanServerConnection();
            base.evaluate();
        }

    }

}

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
package org.kaazing.gateway.management.jmx;

import java.lang.management.ManagementFactory;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URI;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.RMIServerSocketFactory;
import java.rmi.server.UnicastRemoteObject;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.KeyStore;
import java.security.Principal;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import javax.annotation.Resource;
import javax.management.MBeanServer;
import javax.management.Notification;
import javax.management.NotificationFilterSupport;
import javax.management.NotificationListener;
import javax.management.remote.JMXAuthenticator;
import javax.management.remote.JMXConnectionNotification;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXServiceURL;
import javax.management.remote.MBeanServerForwarder;
import javax.management.remote.rmi.RMIConnectorServer;
import javax.rmi.ssl.SslRMIClientSocketFactory;
import javax.rmi.ssl.SslRMIServerSocketFactory;
import javax.security.auth.Subject;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import org.kaazing.gateway.management.ManagementService;
import org.kaazing.gateway.management.context.ManagementContext;
import org.kaazing.gateway.security.RealmContext;
import org.kaazing.gateway.server.Launcher;
import org.kaazing.gateway.server.context.resolve.DefaultSecurityContext;
import org.kaazing.gateway.service.ServiceContext;
import org.kaazing.gateway.service.ServiceProperties;
import org.kaazing.gateway.util.InternalSystemProperty;
import org.slf4j.Logger;
import static java.lang.String.format;
import static java.util.Arrays.asList;

/**
 * Service for handling JMX management of the gateway (cluster?)
 */
public class JmxManagementService implements ManagementService, NotificationListener {

    private DefaultSecurityContext securityContext;
    private ManagementContext managementContext;
    private JMXConnectorServer connectorServer;
    private MBeanServer mbeanServer;
    private Properties configuration;

    private static Registry sRMIRegistry;


// -- before

    private JmxManagementServiceHandler handler;
    private ServiceContext serviceContext;

    @Override
    public void destroy() throws Exception {
        // FIXME:  implement
    }

    @Override
    public void init() {
    }

    @Override
    public String getType() {
        return "management.jmx";
    }

    @Resource(name = "configuration")
    public void setConfiguration(Properties configuration) {
        this.configuration = configuration;
    }

    @Resource(name = "securityContext")
    public void setSecurityContext(DefaultSecurityContext securityContext) {
        this.securityContext = securityContext;
    }

    @Resource(name = "mbeanServer")
    public void setMBeanServer(MBeanServer mbeanServer) {
        this.mbeanServer = mbeanServer;
    }

    @Resource(name = "managementContext")
    public void setManagementContext(ManagementContext managementContext) {
        this.managementContext = managementContext;
    }

    private MBeanServer getMBeanServer() {
        if (mbeanServer == null) {
            mbeanServer = ManagementFactory.getPlatformMBeanServer();
        }
        return mbeanServer;
    }

    @Override
    public void init(ServiceContext serviceContext) throws Exception {
        this.serviceContext = serviceContext;
        handler = new JmxManagementServiceHandler(serviceContext, managementContext, getMBeanServer());
        managementContext.setManagementSessionThreshold(InternalSystemProperty.MANAGEMENT_SESSION_THRESHOLD
                .getIntProperty(configuration));
        managementContext.addManagementServiceHandler(handler);
        managementContext.setActive(true);
    }

    @Override
    public void start() throws Exception {
        // update the management context with service, license, security, cluster, network,
        // and realm config info before starting the service
        managementContext.updateManagementContext(securityContext);

        if (connectorServer != null) {
            throw new IllegalStateException("Already started");
        }

        URI uri;
        ServiceProperties properties = serviceContext.getProperties();
        String connectorServerAddress = properties.get("connector.server.address");
        if (connectorServerAddress != null) {
            uri = new URI(connectorServerAddress);
        } else {
            uri = new URI("jmx://localhost:2020");
        }

        int port = uri.getPort();
        if (port == -1) {
            port = 2020;
        }

        // To allow Gateway to be destroyed and a new one created and started in the same process,
        // make sure we only call createRegistry once:
        if (sRMIRegistry == null) {
            sRMIRegistry = LocateRegistry.createRegistry(port);
        }

        RealmContext serviceRealmContext = serviceContext.getServiceRealm();

        KeyStore keyStore = securityContext.getKeyStore();
        if (keyStore != null) {

            System.setProperty("javax.net.ssl.keyStore", securityContext.getKeyStoreFilePath());
            System.setProperty("javax.net.ssl.keyStoreType", keyStore.getType());
            char[] keyStorePassword = securityContext.getKeyStorePassword();
            if (keyStorePassword != null) {
                System.setProperty("javax.net.ssl.keyStorePassword", new String(keyStorePassword));
            }
        }

        Map<String, Object> env = new HashMap<>();

        RMIClientSocketFactory csf = new SslRMIClientSocketFactory();
        RMIServerSocketFactory ssf = new SslRMIServerSocketFactory();

        env.put(RMIConnectorServer.RMI_CLIENT_SOCKET_FACTORY_ATTRIBUTE, csf);
        env.put(RMIConnectorServer.RMI_SERVER_SOCKET_FACTORY_ATTRIBUTE, ssf);
        env.put(JMXConnectorServer.AUTHENTICATOR, new RealmJMXAuthenticator(serviceRealmContext));

        JMXServiceURL serviceURL = new JMXServiceURL("service:jmx:rmi:///jndi/rmi://" + uri.getHost() + ":" + port + "/jmxrmi");

        MBeanServer mbeanServer = getMBeanServer();

        connectorServer = JMXConnectorServerFactory.newJMXConnectorServer(serviceURL, env, mbeanServer);

        List<String> requiredRoles = asList(serviceContext.getRequireRoles());
        connectorServer.setMBeanServerForwarder(MBSFInvocationHandler.newProxyInstance(requiredRoles));

        // listen for connection notifications on the connectorServer so we know when somebody is
        // connected and can control generating notifications.
        NotificationFilterSupport filter = new NotificationFilterSupport();
        filter.enableType(JMXConnectionNotification.OPENED);
        filter.enableType(JMXConnectionNotification.CLOSED);
        connectorServer.addNotificationListener(this, filter, null);

        // register the connectorServer officially as an MBean, so we can attach
        // connection listeners to it. Passing it in when calling newJMXConnectorServer
        // does not do that, weirdly enough.
        //ObjectName connectorServerName = new ObjectName("connectors:name=rmi");
        //mbeanServer.registerMBean(connectorServer, connectorServerName);

        connectorServer.start();

        // From before.  Should this come first, or last?
//        serviceContext.bind(serviceContext.getAccepts(), handler);

        // KG-10156:  Add some logging to indicate that JMX has started
        Logger startupLogger = Launcher.getGatewayStartupLogger();
        startupLogger.info(format("JMX Management service started with URI %s with service URI %s", uri, serviceURL.toString()));
    }

    @Override
    public void quiesce() throws Exception {
//        serviceContext.unbind(serviceContext.getAccepts(), handler);
    }

    /**
     * NotificationListener support for connection open and closed notifications
     */
    @Override
    public void handleNotification(Notification notification, Object handback) {
        String notificationType = notification.getType();
        if (notificationType.equals(JMXConnectionNotification.OPENED)) {
            managementContext.incrementManagementSessionCount();
        } else if (notificationType.equals(JMXConnectionNotification.CLOSED)) {
            managementContext.decrementManagementSessionCount();
        }
    }

    @Override
    public void stop() throws Exception {
        quiesce();

        // KG-10156:  Add some logging to indicate that JMX has stopped
        ServiceProperties properties = serviceContext.getProperties();
        String connectorServerAddress = properties.get("connector.server.address");
        if (connectorServerAddress == null) {
            connectorServerAddress = "jmx://localhost:2020";
        }

        Logger startupLogger = Launcher.getGatewayStartupLogger();
        startupLogger.info(format("Stopping JMX Management service with URI %s", connectorServerAddress));

        // cleanup the MBeans that were added...
        handler.cleanupRegisteredBeans();

        if (connectorServer != null) {
            connectorServer.stop();
            connectorServer = null;
        }

        if (sRMIRegistry != null) {
            UnicastRemoteObject.unexportObject(sRMIRegistry, true);
            sRMIRegistry = null;
        }
    }


    private static class RealmJMXAuthenticator implements JMXAuthenticator {

        private final RealmContext realm;

        public RealmJMXAuthenticator(RealmContext realm) {
            this.realm = realm;
        }

        @Override
        public Subject authenticate(Object credentialsAsObject) {

            // verify that credentials is of type String[].
            if (!(credentialsAsObject instanceof String[])) {
                // Special case for null so we get a more informative message
                if (credentialsAsObject == null) {
                    throw new SecurityException("Credentials required");
                }
                throw new SecurityException("Credentials should be String[]");
            }

            // verify that the array contains two elements (username/password).
            String[] credentials = (String[]) credentialsAsObject;
            if (credentials.length != 2) {
                throw new SecurityException("Credentials should have 2 elements");
            }

            // perform authentication
            String username = credentials[0];
            String password = credentials[1];

            try {
                Subject subject = new Subject();
                LoginContext loginContext =
                        realm.getLoginContextFactory().createLoginContext(subject, username, password.toCharArray());
                loginContext.login();
                return subject;
            } catch (LoginException e) {
                throw new SecurityException("Invalid credentials");
            }
        }
    }

    private static class MBSFInvocationHandler implements InvocationHandler {

        private MBeanServer mbs;

        private final Collection<String> requiredRoles;

        public MBSFInvocationHandler(Collection<String> requiredRoles) {
            this.requiredRoles = requiredRoles;
        }

        public static MBeanServerForwarder newProxyInstance(Collection<String> requiredRoles) {

            final InvocationHandler handler = new MBSFInvocationHandler(requiredRoles);

            final Class<?>[] interfaces = new Class[]{MBeanServerForwarder.class};

            Object proxy = Proxy.newProxyInstance(MBeanServerForwarder.class.getClassLoader(), interfaces, handler);

            return MBeanServerForwarder.class.cast(proxy);
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

            final String methodName = method.getName();

            if (methodName.equals("getMBeanServer")) {
                return mbs;
            }

            if (methodName.equals("setMBeanServer")) {
                if (args[0] == null) {
                    throw new IllegalArgumentException("Null MBeanServer");
                }
                if (mbs != null) {
                    throw new IllegalArgumentException("MBeanServer object " + "already initialized");
                }
                mbs = (MBeanServer) args[0];
                return null;


            }

            // Retrieve Subject from current AccessControlContext
            AccessControlContext acc = AccessController.getContext();
            Subject subject = Subject.getSubject(acc);

            // Allow operations performed locally on behalf of the connector server itself
            if (subject == null) {
                return method.invoke(mbs, args);
            }

            // Restrict access to "createMBean" and "unregisterMBean" to any user
            if (methodName.equals("createMBean") || methodName.equals("unregisterMBean")) {
                throw new SecurityException("Access denied");
            }

            if (requiredRoles.contains("*")) {
                return method.invoke(mbs, args);
            } else {
                // Retrieve JMXPrincipal from Subject
                Set<Principal> principals = subject.getPrincipals();
                if (principals == null || principals.isEmpty()) {
                    throw new SecurityException("Access denied");
                }

                Set<String> authorizedRoles = new HashSet<>();
                for (Principal principal : principals) {
                    authorizedRoles.add(principal.getName());
                }

                if (authorizedRoles.containsAll(requiredRoles)) {
                    return method.invoke(mbs, args);
                }
            }

            throw new SecurityException("Access denied");
        }
    }
}

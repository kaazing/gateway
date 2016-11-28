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
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import javax.management.JMException;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.kaazing.gateway.management.ManagementServiceHandler;
import org.kaazing.gateway.management.config.ClusterConfigurationBean;
import org.kaazing.gateway.management.config.NetworkConfigurationBean;
import org.kaazing.gateway.management.config.RealmConfigurationBean;
import org.kaazing.gateway.management.config.SecurityConfigurationBean;
import org.kaazing.gateway.management.config.ServiceConfigurationBean;
import org.kaazing.gateway.management.config.ServiceDefaultsConfigurationBean;
import org.kaazing.gateway.management.context.ManagementContext;
import org.kaazing.gateway.management.gateway.GatewayManagementBean;
import org.kaazing.gateway.management.service.ServiceManagementBean;
import org.kaazing.gateway.management.session.SessionManagementBean;
import org.kaazing.gateway.server.Gateway;
import org.kaazing.gateway.service.ServiceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Kaazing service that implements JMX management support.
 */
class JmxManagementServiceHandler implements ManagementServiceHandler {

    private static final String LOGGER_NAME = "management.jmx";
    private static final Logger LOGGER = LoggerFactory.getLogger(LOGGER_NAME);

    public static final String JMX_OBJECT_NAME = Gateway.class.getPackage().getName() + ".management";
    private static final String GATEWAY_MBEAN_FORMAT_STR = "%s:root=gateways,gatewayId=%s,name=summary";
    private static final String SERVICE_MBEAN_FORMAT_STR =
            "%s:root=gateways,gatewayId=%s,subtype=services,serviceType=%s,serviceId=\"%s\",name=summary";
    private static final String SESSION_MBEAN_FORMAT_STR =
            "%s:root=gateways,gatewayId=%s,subtype=services,serviceType=%s,serviceId=\"%s\",name=sessions,sessionId=id-%d";
    private static final String CLUSTER_CONFIG_MBEAN_FORMAT_STR =
            "%s:root=gateways,gatewayId=%s,subtype=configuration,name=cluster,clusterName=%s";
    private static final String NETWORK_MAPPING_MBEAN_FORMAT_STR =
            "%s:root=gateways,gatewayId=%s,subtype=configuration,name=network-mappings,id=%d";
    private static final String SECURITY_MBEAN_FORMAT_STR = "%s:root=gateways,gatewayId=%s,subtype=configuration,name=security";
    private static final String REALM_MBEAN_FORMAT_STR =
            "%s:root=gateways,gatewayId=%s,subtype=configuration,name=realms,realm=%s";
    private static final String SERVICE_CONFIG_MBEAN_FORMAT_STR =
            "%s:root=gateways,gatewayId=%s,subtype=configuration,name=services,serviceType=%s,id=%d";
    private static final String SERVICE_DEFAULTS_CONFIG_MBEAN_FORMAT_STR =
            "%s:root=gateways,gatewayId=%s,subtype=configuration,name=service-defaults";
    private static final String VERSION_INFO_MBEAN_FORMAT_STR =
            "%s:root=gateways,gatewayId=%s,subtype=configuration,name=version-info";

    private final AtomicLong notificationSequenceNumber = new AtomicLong(0);
    // For performance, I need to pass this to the agent
    protected final ServiceContext serviceContext;
    private final MBeanServer mbeanServer;

    private final Map<Integer, ServiceMXBean> serviceBeanMap;
    private final Map<Long, SessionMXBean> sessionBeanMap;

    public JmxManagementServiceHandler(ServiceContext serviceContext, ManagementContext managementContext, MBeanServer
            mbeanServer) {
        this.serviceContext = serviceContext;
        this.mbeanServer = mbeanServer;
        serviceBeanMap = new ConcurrentHashMap<>();
        sessionBeanMap = new ConcurrentHashMap<>();

        managementContext.addGatewayManagementListener(new JmxGatewayManagementListener(this));
        managementContext.addServiceManagementListener(new JmxServiceManagementListener(this));
        managementContext.addSessionManagementListener(new JmxSessionManagementListener(this));
    }

    protected long nextNotificationSequenceNumber() {
        return notificationSequenceNumber.getAndIncrement();
    }

    @Override
    public ServiceContext getServiceContext() {
        return serviceContext;
    }

    public ServiceMXBean getServiceMXBean(int serviceId) {
        return serviceBeanMap.get(serviceId);
    }

    public SessionMXBean getSessionMXBean(long sessionId) {
        return sessionBeanMap.get(sessionId);
    }

    @Override
    public void addGatewayManagementBean(GatewayManagementBean gatewayManagementBean) {
        try {
            String hostAndPid = gatewayManagementBean.getHostAndPid();

            ObjectName name =
                    new ObjectName(String.format(GATEWAY_MBEAN_FORMAT_STR,
                            JMX_OBJECT_NAME,
                            hostAndPid));
            if (mbeanServer.isRegistered(name)) {
                LOGGER.warn(String.format("Gateway MBean name %s already registered", name));
            } else {
                GatewayMXBeanImpl gatewayMXBean = new GatewayMXBeanImpl(name, gatewayManagementBean);

                mbeanServer.registerMBean(gatewayMXBean, name);
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void addServiceManagementBean(ServiceManagementBean serviceManagementBean) {
        try {
            GatewayManagementBean gatewayManagementBean = serviceManagementBean.getGatewayManagementBean();

            ObjectName name =
                    new ObjectName(String.format(SERVICE_MBEAN_FORMAT_STR,
                            JMX_OBJECT_NAME,
                            gatewayManagementBean.getHostAndPid(),
                            replaceCharactersDisallowedInObjectName(serviceManagementBean.getServiceType()),
                            serviceManagementBean.getServiceName()
                    ));
            if (mbeanServer.isRegistered(name)) {
                LOGGER.warn(String.format("Service MBean name %s already registered", name));

            } else {
                ServiceMXBeanImpl serviceMXBean = new ServiceMXBeanImpl(this, name, serviceManagementBean);
                mbeanServer.registerMBean(serviceMXBean, name);
                serviceBeanMap.put(serviceManagementBean.getId(), serviceMXBean);
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void addSessionManagementBean(SessionManagementBean sessionManagementBean) {
        try {
            ServiceManagementBean serviceManagementBean = sessionManagementBean.getServiceManagementBean();
            GatewayManagementBean gatewayManagementBean = serviceManagementBean.getGatewayManagementBean();

            ObjectName name =
                    new ObjectName(String.format(SESSION_MBEAN_FORMAT_STR,
                            JMX_OBJECT_NAME,
                            gatewayManagementBean.getHostAndPid(),
                            replaceCharactersDisallowedInObjectName(serviceManagementBean.getServiceType()),
                            serviceManagementBean.getServiceName(),
                            sessionManagementBean.getId()));
            if (mbeanServer.isRegistered(name)) {
                LOGGER.warn(String.format("Service MBean name %s already registered", name));
            } else {
                SessionMXBeanImpl sessionMXBean = new SessionMXBeanImpl(name, sessionManagementBean);
                mbeanServer.registerMBean(sessionMXBean, name);
                sessionBeanMap.put(sessionManagementBean.getId(), sessionMXBean);
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public SessionMXBean removeSessionMXBean(SessionManagementBean sessionManagementBean) {
        try {
            SessionMXBean sessionMXBean = sessionBeanMap.remove(sessionManagementBean.getId());
            if (sessionMXBean != null) {
                ObjectName name = sessionMXBean.getObjectName();
                if (mbeanServer.isRegistered(name)) {
                    mbeanServer.unregisterMBean(name);
                }
            }
            return sessionMXBean;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void addClusterConfigurationBean(ClusterConfigurationBean clusterConfigBean) {
        try {
            GatewayManagementBean gatewayManagementBean = clusterConfigBean.getGatewayManagementBean();
            ObjectName name =
                    new ObjectName(String.format(CLUSTER_CONFIG_MBEAN_FORMAT_STR,
                            JMX_OBJECT_NAME,
                            gatewayManagementBean.getHostAndPid(),
                            ObjectName.quote(clusterConfigBean.getName())));
            if (mbeanServer.isRegistered(name)) {
                LOGGER.warn(String.format("Cluster config MBean name %s already registered", name));

            } else {
                ClusterConfigurationMXBean clusterConfigMXBean = new ClusterConfigurationMXBeanImpl(name, clusterConfigBean);
                mbeanServer.registerMBean(clusterConfigMXBean, name);
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void addNetworkConfigurationBean(NetworkConfigurationBean networkMappingBean) {
        try {
            GatewayManagementBean gatewayManagementBean = networkMappingBean.getGatewayManagementBean();
            ObjectName name =
                    new ObjectName(String.format(NETWORK_MAPPING_MBEAN_FORMAT_STR,
                            JMX_OBJECT_NAME,
                            gatewayManagementBean.getHostAndPid(),
                            networkMappingBean.getId()));
            if (mbeanServer.isRegistered(name)) {
                LOGGER.warn(String.format("Network mapping MBean name %s already registered", name));

            } else {
                NetworkMappingMXBean networkMappingMXBean = new NetworkMappingMXBeanImpl(name, networkMappingBean);
                mbeanServer.registerMBean(networkMappingMXBean, name);
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void addSecurityConfigurationBean(SecurityConfigurationBean securityBean) {
        try {
            GatewayManagementBean gatewayManagementBean = securityBean.getGatewayManagementBean();
            ObjectName name =
                    new ObjectName(String.format(SECURITY_MBEAN_FORMAT_STR,
                            JMX_OBJECT_NAME,
                            gatewayManagementBean.getHostAndPid()));
            if (mbeanServer.isRegistered(name)) {
                LOGGER.warn(String.format("Realm MBean name %s already registered", name));

            } else {
                SecurityMXBean securityMXBean = new SecurityMXBeanImpl(name, securityBean);
                mbeanServer.registerMBean(securityMXBean, name);
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void addRealmConfigurationBean(RealmConfigurationBean realmBean) {
        try {
            GatewayManagementBean gatewayManagementBean = realmBean.getGatewayManagementBean();
            ObjectName name =
                    new ObjectName(String.format(REALM_MBEAN_FORMAT_STR,
                            JMX_OBJECT_NAME,
                            gatewayManagementBean.getHostAndPid(),
                            realmBean.getName()));
            if (mbeanServer.isRegistered(name)) {
                LOGGER.warn(String.format("Realm MBean name %s already registered", name));

            } else {
                RealmMXBean realmMXBean = new RealmMXBeanImpl(name, realmBean);
                mbeanServer.registerMBean(realmMXBean, name);
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void addServiceConfigurationBean(ServiceConfigurationBean serviceConfigurationBean) {
        try {
            GatewayManagementBean gatewayManagementBean = serviceConfigurationBean.getGatewayManagementBean();
            ObjectName name =
                    new ObjectName(String.format(SERVICE_CONFIG_MBEAN_FORMAT_STR,
                            JMX_OBJECT_NAME,
                            gatewayManagementBean.getHostAndPid(),
                            replaceCharactersDisallowedInObjectName(serviceConfigurationBean.getType()),
                            serviceConfigurationBean.getId()));
            if (mbeanServer.isRegistered(name)) {
                LOGGER.warn(String.format("Service config MBean name %s already registered", name));

            } else {
                ServiceConfigurationMXBean serviceConfigurationMXBean =
                        new ServiceConfigurationMXBeanImpl(name, serviceConfigurationBean);
                mbeanServer.registerMBean(serviceConfigurationMXBean, name);
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void addServiceDefaultsConfigurationBean(ServiceDefaultsConfigurationBean serviceDefaultsConfigurationBean) {
        try {
            GatewayManagementBean gatewayManagementBean = serviceDefaultsConfigurationBean.getGatewayManagementBean();
            ObjectName name =
                    new ObjectName(String.format(SERVICE_DEFAULTS_CONFIG_MBEAN_FORMAT_STR,
                            JMX_OBJECT_NAME,
                            gatewayManagementBean.getHostAndPid()));
            if (mbeanServer.isRegistered(name)) {
                LOGGER.warn(String.format("Service defaults config MBean name %s already registered", name));

            } else {
                ServiceDefaultsConfigurationMXBean serviceDefaultsConfigurationMXBean =
                        new ServiceDefaultsConfigurationMXBeanImpl(name, serviceDefaultsConfigurationBean);
                mbeanServer.registerMBean(serviceDefaultsConfigurationMXBean, name);
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }

    }

    @Override
    public void addVersionInfo(GatewayManagementBean gatewayManagementBean) {
        try {
            ObjectName name =
                    new ObjectName(String.format(VERSION_INFO_MBEAN_FORMAT_STR,
                            JMX_OBJECT_NAME,
                            gatewayManagementBean.getHostAndPid()));
            if (mbeanServer.isRegistered(name)) {
                LOGGER.warn(String.format("Version info MBean name %s already registered", name));

            } else {
                VersionInfoMXBean versionInfoMXBean =
                        new VersionInfoMXBeanImpl(name, gatewayManagementBean);
                mbeanServer.registerMBean(versionInfoMXBean, name);
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public void cleanupRegisteredBeans() {
        String gatewayId = ManagementFactory.getRuntimeMXBean().getName();

        // These query strings are tied to the constants at the top of the file and are sensitive to changes in those format
        // strings
        cleanupRegisteredBeans("%s:root=gateways,gatewayId=%s,name=summary", gatewayId);
        cleanupRegisteredBeans("%s:root=gateways,gatewayId=%s,subtype=services,*,name=summary", gatewayId);
        cleanupRegisteredBeans("%s:root=gateways,gatewayId=%s,subtype=configuration,*", gatewayId);
        cleanupRegisteredBeans("%s:root=gateways,gatewayId=%s,subtype=system,*", gatewayId);
        cleanupRegisteredBeans("%s:root=gateways,gatewayId=%s,subtype=jvm,name=summary", gatewayId);
    }

    private void cleanupRegisteredBeans(String formatString, String gatewayId) {
        try {
            ObjectName query = new ObjectName(String.format(formatString, JMX_OBJECT_NAME, gatewayId));

            Set<ObjectName> beanNames = mbeanServer.queryNames(null, query);
            for (ObjectName beanName : beanNames) {
                if (mbeanServer.isRegistered(beanName)) {
                    mbeanServer.unregisterMBean(beanName);
                }
            }
        } catch (JMException ex) {
            // this is cleanup, so just silently ignore
        }
    }

    // We allow service type in form $class:my.package.MyClass$ for internal testing purposes
    // We must replace the colon, otherwise new ObjectName(...) throws an exception
    private String replaceCharactersDisallowedInObjectName(String name) {
        return name.replace(':', '_');
    }

}

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
package org.kaazing.gateway.management.snmp;

import java.util.Collection;
import java.util.Map;
import java.util.Properties;

import javax.annotation.Resource;

import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.Uptime;
import org.kaazing.gateway.management.ManagementService;
import org.kaazing.gateway.management.context.ManagementContext;
import org.kaazing.gateway.server.context.resolve.DefaultSecurityContext;
import org.kaazing.gateway.service.ServiceContext;
import org.kaazing.gateway.service.cluster.ClusterContext;
import org.kaazing.gateway.service.cluster.MemberId;
import org.kaazing.gateway.service.messaging.collections.CollectionsFactory;
import org.kaazing.gateway.util.InternalSystemProperty;
import org.kaazing.mina.core.session.IoSessionEx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.VariableBinding;

/**
 * The service for managing SNMP connections.
 * <p/>
 * Kaazing's SNMP support is based on the SNMP4J open-source library under the Apache 2.0 license. To see the full text of the
 * license, please see the Kaazing third-party licenses file.
 */
public class SnmpManagementService implements ManagementService {

    private static final Logger logger = LoggerFactory.getLogger(SnmpManagementService.class);

    private ManagementContext managementContext;
    private SnmpManagementServiceHandler handler;
    private ServiceContext serviceContext;
    private DefaultSecurityContext securityContext;
    private Properties configuration;
//    private boolean systemStatsSupported = true;

    @Override
    public void destroy() throws Exception {
        // FIXME:  implement
    }

    @Override
    public void init() {

    }

    @Override
    public String getType() {
        return "management.snmp";
    }

    @Resource(name = "configuration")
    public void setConfiguration(Properties configuration) {
        this.configuration = configuration;
    }

    @Resource(name = "securityContext")
    public void setSecurityContext(DefaultSecurityContext securityContext) {
        this.securityContext = securityContext;
    }

    @Resource(name = "managementContext")
    public void setManagementContext(ManagementContext managementContext) {
        this.managementContext = managementContext;
    }

    @Override
    public void init(ServiceContext serviceContext) throws Exception {
        try {
//            Sigar sigar = new Sigar();
//            Uptime uptime = sigar.getUptime();
        } catch (Throwable t) {
            logger.info("SNMP management service: Unable to access system-level management statistics", t);
            logger.info("  (CPU, NIC, System data). Management will continue without them.", t);
//            systemStatsSupported = false;
        }

        this.serviceContext = serviceContext;
        handler = new SnmpManagementServiceHandler(serviceContext, managementContext);
        managementContext.setManagementSessionThreshold(InternalSystemProperty.MANAGEMENT_SESSION_THRESHOLD
                .getIntProperty(configuration));
        managementContext.addManagementServiceHandler(handler);
        managementContext.setActive(true);
    }

    @Override
    public void quiesce() throws Exception {
        serviceContext.unbind(serviceContext.getAccepts(), handler);
    }

    @Override
    public void start() throws Exception {
        // update the management context with service, license, security, cluster, network,
        // and realm config info before starting the service
        managementContext.updateManagementContext(securityContext);

        serviceContext.bind(serviceContext.getAccepts(), handler);

        // create a map of management services that can then be navigated in the cluster
        ClusterContext clusterContext = managementContext.getCluster();
        CollectionsFactory factory = clusterContext.getCollectionsFactory();
        Map<MemberId, Collection<String>> managementServiceUriMap = factory.getMap(MANAGEMENT_SERVICE_MAP_NAME);

        managementServiceUriMap.put(clusterContext.getLocalMember(), serviceContext.getAccepts());
    }

    @Override
    public void stop() throws Exception {
        quiesce();
        for (IoSessionEx session : serviceContext.getActiveSessions()) {
            session.close(true);
        }
    }

    public void removeGatewayBean(OID oid) {
        handler.removeGatewayBean(oid);
    }

    public void removeServiceBean(OID oid) {
        handler.removeServiceBean(oid);
    }

    public void removeSessionBean(OID oid) {
        handler.removeSessionBean(oid);
    }

    public void sendNotification(OID oid, VariableBinding[] variableBindings) {
        handler.sendNotification(oid, variableBindings);
    }
}

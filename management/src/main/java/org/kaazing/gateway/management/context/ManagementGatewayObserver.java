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
package org.kaazing.gateway.management.context;

import java.util.Properties;

import javax.annotation.Resource;

import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.session.IoSessionInitializer;
import org.kaazing.gateway.management.ManagementService;
import org.kaazing.gateway.management.filter.ManagementFilter;
import org.kaazing.gateway.management.monitoring.configuration.MonitoringDataManager;
import org.kaazing.gateway.management.monitoring.configuration.MonitoringDataManagerInjector;
import org.kaazing.gateway.management.monitoring.configuration.impl.MonitoringDataManagerInjectorImpl;
import org.kaazing.gateway.management.monitoring.service.impl.MonitoredServiceImpl;
import org.kaazing.gateway.server.GatewayObserverFactorySpiPrototype;
import org.kaazing.gateway.server.context.GatewayContext;
import org.kaazing.gateway.service.MonitoringEntityFactory;
import org.kaazing.gateway.service.Service;
import org.kaazing.gateway.service.ServiceContext;
import org.kaazing.mina.core.session.IoSessionEx;

public class ManagementGatewayObserver extends GatewayObserverFactorySpiPrototype {

    private ManagementContext managementContext;
    private Properties configuration;
    private MonitoringDataManager monitoringDataManager;

    @Resource(name = "managementContext")
    public void setManagementContext(ManagementContext managementContext) {
        this.managementContext = managementContext;
    }

    @Resource(name = "configuration")
    public void setConfiguration(Properties configuration) {
        this.configuration = configuration;
    }

    @Override
    public void startingGateway(GatewayContext gatewayContext) {
        managementContext.createGatewayManagementBean();
        MonitoringDataManagerInjector injector = new MonitoringDataManagerInjectorImpl(configuration);
        monitoringDataManager = injector.makeMonitoringDataManager();
    }

    @Override
    public void initingService(ServiceContext serviceContext) {
        MonitoringEntityFactory monitoringEntityFactory =
                monitoringDataManager.addService(new MonitoredServiceImpl(serviceContext));
        serviceContext.setMonitoringFactory(monitoringEntityFactory);
    }

    /**
     * Respond to a service starting by adding a management bean for it. Note that we CANNOT do this during initedService because
     * a given service may come before the SNMP Management service, which does create the SNMP agent until its own init(). Note:
     * it is still slightly possible that someone could attempt to log in through Command Center in the second or so during
     * service startup. If some service hasn't started yet, it won't show up in the service list. The chance of this is very
     * slight, though, so we should deal with it later, if at all.
     */
    @Override
    public void startingService(ServiceContext serviceContext) {
        // NOTE: the session initializer ultimately will create a management filter
        // to add to the session filter chain. Creating that filter requires that
        // the service management bean actually exist, which means that the first
        // line in the block to add the bean MUST come first, and the two lines
        // must exist together. Adding the bean before startingService doesn't work
        // as explained above.
        if (!(serviceContext.getService() instanceof ManagementService)) {
            managementContext.addServiceManagementBean(serviceContext);
            addSessionInitializer(serviceContext.getService(), serviceContext);
        }
    }

    @Override
    public void destroyedService(ServiceContext serviceContext) {
        serviceContext.getMonitoringFactory().close();
    }

    @Override
    public void stoppedGateway(GatewayContext gatewayContext) {
        monitoringDataManager.close();
    }

    private void addSessionInitializer(Service service, ServiceContext serviceContext) {
        // if (not a management service then add a Management filter to the chain
        if (!(service instanceof ManagementService)) {
            final IoSessionInitializer<ConnectFuture> parentInitializer = serviceContext.getSessionInitializor();
            final ManagementFilter managementFilter = managementContext.getManagementFilter(serviceContext);

            serviceContext.setSessionInitializor(new IoSessionInitializer<ConnectFuture>() {

                public static final String MANAGEMENT_FILTER_NAME = "management";

                @Override
                public void initializeSession(IoSession session, ConnectFuture future) {
                    if (!session.getFilterChain().contains(MANAGEMENT_FILTER_NAME)) {
                        try {
                            // let the management layer know the bean exists
                            managementFilter.newManagementSession((IoSessionEx) session);
                        } catch (Exception ex) {
                            throw new RuntimeException("Exception adding new management session for session: "
                                    + session, ex);
                        }

                        session.getFilterChain().addLast(MANAGEMENT_FILTER_NAME, managementFilter);

                        // For Management we invoke the parent initializer after doing our work (FIXME: should the
                        // parent go first?)
                        if (parentInitializer != null) {
                            parentInitializer.initializeSession(session, future);
                        }
                    }
                }
            });
        }
    }
}

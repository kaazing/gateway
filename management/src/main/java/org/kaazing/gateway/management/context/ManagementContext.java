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

import java.util.List;

import org.kaazing.gateway.management.ManagementServiceHandler;
import org.kaazing.gateway.management.ManagementStrategyChangeListener;
import org.kaazing.gateway.management.SummaryManagementInterval;
import org.kaazing.gateway.management.filter.ManagementFilter;
import org.kaazing.gateway.management.filter.ManagementFilterStrategy;
import org.kaazing.gateway.management.gateway.GatewayManagementListener;
import org.kaazing.gateway.management.gateway.ManagementGatewayStrategy;
import org.kaazing.gateway.management.service.ManagementServiceStrategy;
import org.kaazing.gateway.management.service.ServiceManagementBean;
import org.kaazing.gateway.management.service.ServiceManagementListener;
import org.kaazing.gateway.management.session.ManagementSessionStrategy;
import org.kaazing.gateway.management.session.SessionManagementBean;
import org.kaazing.gateway.management.session.SessionManagementListener;
import org.kaazing.gateway.security.SecurityContext;
import org.kaazing.gateway.service.ServiceContext;
import org.kaazing.gateway.service.cluster.ClusterContext;
import org.kaazing.gateway.util.scheduler.SchedulerProvider;
import org.kaazing.mina.core.session.IoSessionEx;

public interface ManagementContext {

    int getManagementSessionCount();

    void incrementManagementSessionCount();

    void decrementManagementSessionCount();

    void setManagementSessionThreshold(int managementSessionThreshold);

    int getSessionManagementThreshold();

    int getOverallSessionCount();

    void incrementOverallSessionCount();

    void decrementOverallSessionCount();

    SchedulerProvider getSchedulerProvider();

    List<GatewayManagementListener> getGatewayManagementListeners();

    void addGatewayManagementListener(GatewayManagementListener listener);

    void removeGatewayManagementListener(GatewayManagementListener managementListener);

    List<ServiceManagementListener> getServiceManagementListeners();

    void addServiceManagementListener(ServiceManagementListener listener);

    void removeServiceManagementListener(ServiceManagementListener managementListener);

    List<SessionManagementListener> getSessionManagementListeners();

    void addSessionManagementListener(SessionManagementListener listener);

    void removeSessionManagementListener(SessionManagementListener listener);

    List<ManagementStrategyChangeListener> getManagementStrategyChangeListeners();

    void addManagementStrategyChangeListener(ManagementStrategyChangeListener listener);

    void removeManagementStrategyListener(ManagementStrategyChangeListener listener);

    List<ManagementServiceHandler> getManagementServiceHandlers();

    void addManagementServiceHandler(ManagementServiceHandler managementServiceHandler);

    void removeManagementServiceHandler(ManagementServiceHandler managementServiceHandler);


    void runManagementTask(Runnable r);

    ManagementFilterStrategy getManagementFilterStrategy();

    ManagementGatewayStrategy getManagementGatewayStrategy();

    ManagementServiceStrategy getManagementServiceStrategy();

    ManagementSessionStrategy getManagementSessionStrategy();

    /**
     * Create and register a session management bean. The list of active session management beans is kept within the management
     * context. We also keep the resource address within the management bean itself, as we can use it later for
     * management-protocol-specific tasks like reconstructing a JMX MBean name.
     * <p/>
     * We need this (though not the service or gateway ones) because we call it from one of the filter strategies, while the
     * others are only called w/in DefaultManagementContext.
     *
     * @param address
     * @param serviceContext
     * @return the new SessionManagementBean
     */
    SessionManagementBean addSessionManagementBean(ServiceManagementBean serviceManagementBean, IoSessionEx session);

    void updateManagementContext(SecurityContext securityContext);

    ClusterContext getCluster();

    SummaryManagementInterval getGatewaySummaryDataNotificationInterval();

    SummaryManagementInterval getServiceSummaryDataNotificationInterval();

    SummaryManagementInterval getSessionSummaryDataNotificationInterval();

    /**
     * When a management service is initialized, the management context will be flagged as active.
     *
     * @return whether the  management context is active due to management services present in the Gateway
     */
    boolean isActive();

    /**
     * When a management service is initialized, the management context will be flagged as active.
     *
     * @param active - whether the  management context is active due to management services present in the Gateway
     */
    void setActive(boolean active);

    void addServiceManagementBean(ServiceContext serviceContext);

    ManagementFilter getManagementFilter(ServiceContext serviceContext);

    void createGatewayManagementBean();

}

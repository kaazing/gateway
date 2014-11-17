/**
 * Copyright (c) 2007-2014 Kaazing Corporation. All rights reserved.
 * 
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
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
import org.kaazing.gateway.management.system.ManagementSystemStrategy;
import org.kaazing.gateway.management.system.SystemDataProvider;
import org.kaazing.gateway.security.SecurityContext;
import org.kaazing.gateway.service.ServiceContext;
import org.kaazing.gateway.service.cluster.ClusterContext;
import org.kaazing.gateway.util.scheduler.SchedulerProvider;
import org.kaazing.mina.core.session.IoSessionEx;

public interface ManagementContext {
    
    public int getManagementSessionCount();
    public void incrementManagementSessionCount();
    public void decrementManagementSessionCount();

    public void setManagementSessionThreshold(int managementSessionThreshold);
    public int getSessionManagementThreshold();
    
    public int getOverallSessionCount();
    public void incrementOverallSessionCount();
    public void decrementOverallSessionCount();
        
    public SchedulerProvider getSchedulerProvider();
    
    public SystemDataProvider getSystemDataProvider();

    public List<GatewayManagementListener> getGatewayManagementListeners();
    public void addGatewayManagementListener(GatewayManagementListener listener);
    public void removeGatewayManagementListener(GatewayManagementListener managementListener);
    
    public List<ServiceManagementListener> getServiceManagementListeners();
    public void addServiceManagementListener(ServiceManagementListener listener);
    public void removeServiceManagementListener(ServiceManagementListener managementListener);
    
    public List<SessionManagementListener> getSessionManagementListeners();
    public void addSessionManagementListener(SessionManagementListener listener);
    public void removeSessionManagementListener(SessionManagementListener listener);
    
    public List<ManagementStrategyChangeListener> getManagementStrategyChangeListeners();
    public void addManagementStrategyChangeListener(ManagementStrategyChangeListener listener);
    public void removeManagementStrategyListener(ManagementStrategyChangeListener listener);

    public List<ManagementServiceHandler> getManagementServiceHandlers();
    
    public void addManagementServiceHandler(ManagementServiceHandler managementServiceHandler);

    public void removeManagementServiceHandler(ManagementServiceHandler managementServiceHandler);
    
    
    public void runManagementTask(Runnable r);
            
    public ManagementFilterStrategy getManagementFilterStrategy();
    
    public ManagementGatewayStrategy getManagementGatewayStrategy();
    
    public ManagementServiceStrategy getManagementServiceStrategy();
    
    public ManagementSessionStrategy getManagementSessionStrategy();
    
    public ManagementSystemStrategy getManagementSystemStrategy();
    
    /**
     * Create and register a session management bean. The list of active session management
     * beans is kept within the management context. We also keep the resource address within
     * the management bean itself, as we can use it later for management-protocol-specific
     * tasks like reconstructing a JMX MBean name.
     * 
     * We need this (though not the service or gateway ones) because we call it from one
     * of the filter strategies, while the others are only called w/in DefaultManagementContext.
     * 
     * @param address
     * @param serviceContext
     * 
     * @return the new SessionManagementBean
     */
    public SessionManagementBean addSessionManagementBean(ServiceManagementBean serviceManagementBean, IoSessionEx session);

    public void removeSessionManagementBean(SessionManagementBean sessionBean);

    public void updateManagementContext(SecurityContext securityContext);
    public ClusterContext getCluster();

    public SummaryManagementInterval getGatewaySummaryDataNotificationInterval();
    public SummaryManagementInterval getServiceSummaryDataNotificationInterval();
    public SummaryManagementInterval getSessionSummaryDataNotificationInterval();

    public SummaryManagementInterval getSystemSummaryDataGatherInterval();
    public SummaryManagementInterval getSystemSummaryDataNotificationInterval();

    public SummaryManagementInterval getCpuListSummaryDataGatherInterval();
    public SummaryManagementInterval getCpuListSummaryDataNotificationInterval();

    public SummaryManagementInterval getNicListSummaryDataGatherInterval();
    public SummaryManagementInterval getNicListSummaryDataNotificationInterval();

    public SummaryManagementInterval getJvmSummaryDataGatherInterval();
    public SummaryManagementInterval getJvmSummaryDataNotificationInterval();

    /**
     * When a management service is initialized, the management context will be flagged as active.
     * @return whether the  management context is active due to management services present in the Gateway
     */
    public boolean isActive();

    /**
     * When a management service is initialized, the management context will be flagged as active.
     * @param active - whether the  management context is active due to management services present in the Gateway
     */
    public void setActive(boolean active);

    public void addServiceManagementBean(ServiceContext serviceContext);
    public ManagementFilter getManagementFilter(ServiceContext serviceContext);
    public void createGatewayManagementBean();
}

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

import static org.kaazing.gateway.management.service.ServiceManagementBeanFactory.newServiceManagementBeanFactory;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Resource;

import org.kaazing.gateway.management.ManagementServiceHandler;
import org.kaazing.gateway.management.ManagementStrategy;
import org.kaazing.gateway.management.ManagementStrategyChangeListener;
import org.kaazing.gateway.management.SummaryManagementInterval;
import org.kaazing.gateway.management.config.ClusterConfigurationBean;
import org.kaazing.gateway.management.config.ClusterConfigurationBeanImpl;
import org.kaazing.gateway.management.config.RealmConfigurationBean;
import org.kaazing.gateway.management.config.RealmConfigurationBeanImpl;
import org.kaazing.gateway.management.config.SecurityConfigurationBean;
import org.kaazing.gateway.management.config.SecurityConfigurationBeanImpl;
import org.kaazing.gateway.management.config.ServiceConfigurationBean;
import org.kaazing.gateway.management.config.ServiceConfigurationBeanImpl;
import org.kaazing.gateway.management.config.ServiceDefaultsConfigurationBean;
import org.kaazing.gateway.management.config.ServiceDefaultsConfigurationBeanImpl;
import org.kaazing.gateway.management.filter.FullManagementFilterStrategy;
import org.kaazing.gateway.management.filter.ManagementFilter;
import org.kaazing.gateway.management.filter.ManagementFilterStrategy;
import org.kaazing.gateway.management.filter.PassThruManagementFilterStrategy;
import org.kaazing.gateway.management.filter.ServiceOnlyManagementFilterStrategy;
import org.kaazing.gateway.management.gateway.CollectOnlyManagementGatewayStrategy;
import org.kaazing.gateway.management.gateway.FullManagementGatewayStrategy;
import org.kaazing.gateway.management.gateway.GatewayManagementBean;
import org.kaazing.gateway.management.gateway.GatewayManagementBeanImpl;
import org.kaazing.gateway.management.gateway.GatewayManagementListener;
import org.kaazing.gateway.management.gateway.ManagementGatewayStrategy;
import org.kaazing.gateway.management.service.CollectOnlyManagementServiceStrategy;
import org.kaazing.gateway.management.service.FullManagementServiceStrategy;
import org.kaazing.gateway.management.service.ManagementServiceStrategy;
import org.kaazing.gateway.management.service.ServiceManagementBean;
import org.kaazing.gateway.management.service.ServiceManagementBeanFactory;
import org.kaazing.gateway.management.service.ServiceManagementListener;
import org.kaazing.gateway.management.session.CollectOnlyManagementSessionStrategy;
import org.kaazing.gateway.management.session.FullManagementSessionStrategy;
import org.kaazing.gateway.management.session.ManagementSessionStrategy;
import org.kaazing.gateway.management.session.SessionManagementBean;
import org.kaazing.gateway.management.session.SessionManagementBeanImpl;
import org.kaazing.gateway.management.session.SessionManagementListener;
import org.kaazing.gateway.security.RealmContext;
import org.kaazing.gateway.security.SecurityContext;
import org.kaazing.gateway.server.context.DependencyContext;
import org.kaazing.gateway.server.context.GatewayContext;
import org.kaazing.gateway.server.context.ServiceDefaultsContext;
import org.kaazing.gateway.service.ServiceContext;
import org.kaazing.gateway.service.cluster.ClusterContext;
import org.kaazing.gateway.util.scheduler.SchedulerProvider;
import org.kaazing.mina.core.session.IoSessionEx;

public class DefaultManagementContext implements ManagementContext, DependencyContext {
    public static final int DEFAULT_SUMMARY_DATA_NOTIFICATION_INTERVAL = 5000; // 5 seconds by default
    public static final int DEFAULT_SYSTEM_SUMMARY_DATA_NOTIFICATION_INTERVAL = 2000; // 5 seconds by default
    public static final int DEFAULT_SUMMARY_DATA_GATHER_INTERVAL = 500;  // 500 ms

    public static final String NAME = "managementContext";

    public static final String SESSION_MANAGEMENT_THRESHOLD_PROP = "session.management.threshold";
    public static final String MAX_MANAGEMENT_SESSIONS_PROP = "max.management.sessions";

    public static final ManagementFilterStrategy PASS_THRU_FILTER_STRATEGY = new PassThruManagementFilterStrategy();
    public static final ManagementFilterStrategy SERVICE_ONLY_FILTER_STRATEGY = new ServiceOnlyManagementFilterStrategy();
    public static final ManagementFilterStrategy FULL_FILTER_STRATEGY = new FullManagementFilterStrategy();

    public static final ManagementGatewayStrategy COLLECT_ONLY_GATEWAY_STRATEGY = new CollectOnlyManagementGatewayStrategy();
    public static final ManagementGatewayStrategy FULL_GATEWAY_STRATEGY = new FullManagementGatewayStrategy();

    public static final ManagementServiceStrategy COLLECT_ONLY_SERVICE_STRATEGY = new CollectOnlyManagementServiceStrategy();
    public static final ManagementServiceStrategy FULL_SERVICE_STRATEGY = new FullManagementServiceStrategy();

    public static final ManagementSessionStrategy COLLECT_ONLY_SESSION_STRATEGY = new CollectOnlyManagementSessionStrategy();
    public static final ManagementSessionStrategy FULL_SESSION_STRATEGY = new FullManagementSessionStrategy();

    // in the following lists, entries are: filter-level, gateway-level, service-level, session-level.
    private static final ManagementStrategy[] COLLECT_ONLY_STRATEGY = {FULL_FILTER_STRATEGY, // to allow creating session beans
            COLLECT_ONLY_GATEWAY_STRATEGY,
            COLLECT_ONLY_SERVICE_STRATEGY,
            COLLECT_ONLY_SESSION_STRATEGY};
    private static final ManagementStrategy[] FULL_STRATEGY = {FULL_FILTER_STRATEGY,
            FULL_GATEWAY_STRATEGY,
            FULL_SERVICE_STRATEGY,
            FULL_SESSION_STRATEGY};
    private final int FILTER_INDEX = 0;
    private final int GATEWAY_INDEX = 1;
    private final int SERVICE_INDEX = 2;
    private final int SESSION_INDEX = 3;

    private static Map<ServiceContext, Integer> serviceIndexMap = new HashMap<>();
    private static Integer serviceIndexCount = 1;

    private final String localGatewayHostAndPid;  // for the Gateway instance this is part of
    private final AtomicBoolean managementConfigured;

    private final SummaryManagementInterval gatewaySummaryDataNotificationInterval;
    private final SummaryManagementInterval serviceSummaryDataNotificationInterval;
    private final SummaryManagementInterval sessionSummaryDataNotificationInterval;
    // The set of management strategies currently in force.
    private ManagementStrategy[] managementStrategy = COLLECT_ONLY_STRATEGY;

    // The total number of current sessions on either management service. We use this
    // along with sessionManagementThreshold to modify the level of management.
    // Currently needs to be atomic because the update may be coming from any session IO thread.
    private AtomicInteger managementSessionCount = new AtomicInteger(0);

    // The total number of current sessions, NOT including management. This is the
    // value that we check against sessionManagementThreshold;
    // Currently needs to be atomic because the update may be coming from any session IO thread.
    private AtomicInteger overallSessionCount = new AtomicInteger(0);

    // The threshold for 'max number of sessions across all services that we
    // will tolerate. ABOVE this number management will be cut back. EQUAL to this number
    // we will still do 'full' management stats gathering and notification.
    // This is set once at DMC initialization, though we'll make it possible to change it
    // later, in case reading it through a system property isn't sufficient.
    // NOTE: this includes management sessions as well!
    //
    // This is set by the management services from properties
    private int managementSessionThreshold;

    // The list of handlers that have been configured for management for this gateway.
    // These are added during management-service init().
    private final List<ManagementServiceHandler> managementServiceHandlers;

    private final ConcurrentHashMap<ServiceContext, ManagementFilter> managementFilters =
            new ConcurrentHashMap<>();

    // The list of GatewayManagementListeners, one per management service type, for gateway
    // events. The particular listeners for each management service type are currently fixed,
    // so the list doesn't change for that, but we do want it to change for full/collect-only management.
    private final List<GatewayManagementListener> gatewayManagementListeners =
            new CopyOnWriteArrayList<>();

    // Similar set of listeners, but for service events.
    private final List<ServiceManagementListener> serviceManagementListeners =
            new CopyOnWriteArrayList<>();

    // Similar set of listeners, but for session events.
    private final List<SessionManagementListener> sessionManagementListeners =
            new CopyOnWriteArrayList<>();

    // List of objects to be notified when the managemment strategy changes. Generally this
    // is only used by AbstractSummaryDataProviders, as they need to be notified specifically
    // to start or stop gathering. But it can be used elsewhere, too
    private List<ManagementStrategyChangeListener> managementStrategyChangeListeners =
            new ArrayList<>();

    // injected at startup
    private SchedulerProvider schedulerProvider;
    private GatewayContext gatewayContext;

    private ScheduledExecutorService managementExecutorService;

    // when a management service is initialized it will flag the management context as active
    private boolean active;

    // The gateway management beans, keyed by a hostname:processId pair.
    // NOTE: we do not want to expose this to the ManagementProcessors--they should
    // be processing based on the gateway management bean, not the address.
    private final ConcurrentHashMap<String, GatewayManagementBean> gatewayManagementBeans =
            new ConcurrentHashMap<>();

    // The service management beans, keyed by the address of the service's transport
    // that they contain.
    // NOTE: we do not want to expose this to the ManagementProcessors--they should
    // be processing based on the service management bean, not the address.
    private final ConcurrentHashMap<ServiceContext, ServiceManagementBean> serviceManagementBeans =
            new ConcurrentHashMap<>();

    private final ConcurrentHashMap<ServiceContext, ServiceConfigurationBean> serviceConfigBeans =
            new ConcurrentHashMap<>();

    private final ServiceManagementBeanFactory serviceManagmentBeanFactory = newServiceManagementBeanFactory();

    public DefaultManagementContext() {
        this.managementServiceHandlers = new ArrayList<>();

        // The map of GatewayManagementBeans is keyed on the hostname and process ID of the
        // gateway process.  Compute that here so we can create a GatewayManagementBean for
        // the local gateway.  Session and service-level beans will then use the gateway's
        // index in their data.  Turns out (at least for linux and the Sun JDK) that the

        // The following is claimed to work on most JDKs, but is not guaranteed.
        localGatewayHostAndPid = ManagementFactory.getRuntimeMXBean().getName();
        managementConfigured = new AtomicBoolean(false);

        gatewaySummaryDataNotificationInterval = new SummaryManagementIntervalImpl(DEFAULT_SUMMARY_DATA_NOTIFICATION_INTERVAL);
        serviceSummaryDataNotificationInterval = new SummaryManagementIntervalImpl(DEFAULT_SUMMARY_DATA_NOTIFICATION_INTERVAL);
        sessionSummaryDataNotificationInterval = new SummaryManagementIntervalImpl(DEFAULT_SUMMARY_DATA_NOTIFICATION_INTERVAL);
    }

    @Resource(name = "schedulerProvider")
    public void setSchedulerProvider(SchedulerProvider schedulerProvider) {
        this.schedulerProvider = schedulerProvider;
        this.managementExecutorService = schedulerProvider.getScheduler("management", true);
    }

    @Override
    public SchedulerProvider getSchedulerProvider() {
        return this.schedulerProvider;
    }

    @Resource(name = "gatewayContext")
    public void setGatewayContext(GatewayContext gatewayContext) {
        this.gatewayContext = gatewayContext;
    }

    @Override
    public ClusterContext getCluster() {
        if (gatewayContext != null) {
            return gatewayContext.getCluster();
        }
        return null;
    }

    @Override
    public boolean isActive() {
        return active;
    }

    @Override
    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public int getManagementSessionCount() {
        return managementSessionCount.get();
    }

    @Override
    public void incrementManagementSessionCount() {
        adjustStrategies(managementSessionCount.incrementAndGet(), overallSessionCount.incrementAndGet());
    }

    @Override
    public void decrementManagementSessionCount() {
        adjustStrategies(managementSessionCount.decrementAndGet(), overallSessionCount.decrementAndGet());
    }

    @Override
    public void setManagementSessionThreshold(int managementSessionThreshold) {
        this.managementSessionThreshold = managementSessionThreshold;
    }

    @Override
    public int getSessionManagementThreshold() {
        return managementSessionThreshold;
    }

    @Override
    public int getOverallSessionCount() {
        return overallSessionCount.get();
    }

    @Override
    public void incrementOverallSessionCount() {
        adjustStrategies(managementSessionCount.get(), overallSessionCount.incrementAndGet());
    }

    @Override
    public void decrementOverallSessionCount() {
        adjustStrategies(managementSessionCount.get(), overallSessionCount.decrementAndGet());
    }

    @Override
    public List<GatewayManagementListener> getGatewayManagementListeners() {
        return gatewayManagementListeners;
    }

    @Override
    public void addGatewayManagementListener(GatewayManagementListener listener) {
        gatewayManagementListeners.add(listener);
    }

    @Override
    public void removeGatewayManagementListener(GatewayManagementListener listener) {
        gatewayManagementListeners.remove(listener);
    }

    @Override
    public List<ServiceManagementListener> getServiceManagementListeners() {
        return serviceManagementListeners;
    }

    @Override
    public void addServiceManagementListener(ServiceManagementListener listener) {
        serviceManagementListeners.add(listener);
    }

    @Override
    public void removeServiceManagementListener(ServiceManagementListener listener) {
        serviceManagementListeners.remove(listener);
    }

    @Override
    public List<SessionManagementListener> getSessionManagementListeners() {
        return sessionManagementListeners;
    }

    @Override
    public void addSessionManagementListener(SessionManagementListener listener) {
        sessionManagementListeners.add(listener);
    }

    @Override
    public void removeSessionManagementListener(SessionManagementListener listener) {
        sessionManagementListeners.remove(listener);
    }

    @Override
    public List<ManagementStrategyChangeListener> getManagementStrategyChangeListeners() {
        return managementStrategyChangeListeners;
    }

    @Override
    public void addManagementStrategyChangeListener(ManagementStrategyChangeListener listener) {
        managementStrategyChangeListeners.add(listener);
    }

    @Override
    public void removeManagementStrategyListener(ManagementStrategyChangeListener listener) {
        managementStrategyChangeListeners.remove(listener);
    }

    private void adjustStrategies(long managementSessionCount, long overallSessionCount) {
        ManagementStrategy[] newManagementStrategy;

        if (overallSessionCount > managementSessionThreshold ||
                managementSessionCount == 0) {
            // too many sessions, or "no management logged in"
            newManagementStrategy = COLLECT_ONLY_STRATEGY;
        } else {
            newManagementStrategy = FULL_STRATEGY;
        }

        if (newManagementStrategy != managementStrategy) {
            managementStrategy = newManagementStrategy;
            for (ManagementStrategyChangeListener listener : managementStrategyChangeListeners) {
                listener.managementStrategyChanged();
            }
        }
    }

    @Override
    public List<ManagementServiceHandler> getManagementServiceHandlers() {
        return managementServiceHandlers;
    }

    @Override
    public void addManagementServiceHandler(ManagementServiceHandler managementServiceHandler) {
        if (!managementServiceHandlers.contains(managementServiceHandler)) {
            managementServiceHandlers.add(managementServiceHandler);
            managementServiceHandler.addGatewayManagementBean(getLocalGatewayManagementBean());
        }
    }

    @Override
    public void removeManagementServiceHandler(ManagementServiceHandler managementServiceHandler) {
        managementServiceHandlers.remove(managementServiceHandler);
    }

    @Override
    public void runManagementTask(Runnable r) {
        managementExecutorService.execute(r);
    }

    public static synchronized int getNextServiceIndex(ServiceContext serviceContext) {
        Integer index = serviceIndexMap.get(serviceContext);
        if (index == null) {
            serviceIndexMap.put(serviceContext, serviceIndexCount);
            index = serviceIndexCount;
            serviceIndexCount++;
        }
        return index;
    }

    private GatewayManagementBean getLocalGatewayManagementBean() {
        GatewayManagementBean gatewayManagementBean = getGatewayManagementBean(localGatewayHostAndPid);
        if (gatewayManagementBean == null) {
            throw new RuntimeException("GatewayManagementBean has not been created, dependency injection failed.");
        }
        return gatewayManagementBean;
    }

    private GatewayManagementBean getGatewayManagementBean(String hostAndPid) {
        return gatewayManagementBeans.get(hostAndPid);
    }

    private ClusterConfigurationBean addClusterConfigurationBean(ClusterContext clusterContext, GatewayManagementBean
            gatewayBean) {
        // Note: per current 4.0 implementation, clusterContext will never be null
        ClusterConfigurationBean clusterConfigBean = new ClusterConfigurationBeanImpl(clusterContext, gatewayBean);
        gatewayBean.setClusterContext(clusterContext);

        for (ManagementServiceHandler handler : managementServiceHandlers) {
            handler.addClusterConfigurationBean(clusterConfigBean);
        }
        return clusterConfigBean;
    }

    private SecurityConfigurationBean addSecurityConfigurationBean(SecurityContext securityContext, GatewayManagementBean
            gatewayBean) {
        SecurityConfigurationBean securityBean = new SecurityConfigurationBeanImpl(securityContext, gatewayBean);

        for (ManagementServiceHandler handler : managementServiceHandlers) {
            handler.addSecurityConfigurationBean(securityBean);
        }
        return securityBean;
    }

    private RealmConfigurationBean addRealmConfigurationBean(RealmContext realm, GatewayManagementBean gatewayBean) {
        RealmConfigurationBean realmBean = new RealmConfigurationBeanImpl(realm, gatewayBean);
        for (ManagementServiceHandler handler : managementServiceHandlers) {
            handler.addRealmConfigurationBean(realmBean);
        }
        return realmBean;
    }

    private ServiceConfigurationBean addServiceConfigurationBean(ServiceContext service, GatewayManagementBean gatewayBean) {
        ServiceConfigurationBean serviceConfigurationBean = new ServiceConfigurationBeanImpl(service, gatewayBean);
        ServiceConfigurationBean tmpServiceConfigBean = serviceConfigBeans.putIfAbsent(service, serviceConfigurationBean);
        if (tmpServiceConfigBean != null) {
            return tmpServiceConfigBean;
        }

        for (ManagementServiceHandler handler : managementServiceHandlers) {
            handler.addServiceConfigurationBean(serviceConfigurationBean);
        }
        return serviceConfigurationBean;
    }

    private void addVersionInfo(GatewayManagementBean gatewayBean) {
        for (ManagementServiceHandler handler : managementServiceHandlers) {
            handler.addVersionInfo(gatewayBean);
        }
    }

    private ServiceDefaultsConfigurationBean addServiceDefaultsConfigurationBean(ServiceDefaultsContext serviceDefaultsContext,
                                                                                 GatewayManagementBean gatewayBean) {

        ServiceDefaultsConfigurationBean serviceDefaultsConfigurationBean =
                new ServiceDefaultsConfigurationBeanImpl(serviceDefaultsContext, gatewayBean);

        for (ManagementServiceHandler handler : managementServiceHandlers) {
            handler.addServiceDefaultsConfigurationBean(serviceDefaultsConfigurationBean);
        }

        return serviceDefaultsConfigurationBean;
    }

    private ManagementFilter addManagementFilter(ServiceContext serviceContext, ServiceManagementBean serviceBean) {
        ManagementFilter managementFilter = new ManagementFilter(serviceBean);
        managementFilters.put(serviceContext, managementFilter);
        return managementFilter;
    }

    @Override
    public ManagementFilter getManagementFilter(ServiceContext serviceContext) {
        ManagementFilter managementFilter = managementFilters.get(serviceContext);
        if (managementFilter == null) {
            // Service Management Beans are created in initing, getManagementFilter is done
            // on service start through session initializer
            ServiceManagementBean serviceBean = serviceManagementBeans.get(serviceContext);
            managementFilter = addManagementFilter(serviceContext, serviceBean);
        }

        return managementFilter;
    }

    @Override
    public ManagementFilterStrategy getManagementFilterStrategy() {
        return (ManagementFilterStrategy) managementStrategy[FILTER_INDEX];
    }

    @Override
    public ManagementGatewayStrategy getManagementGatewayStrategy() {
        return (ManagementGatewayStrategy) managementStrategy[GATEWAY_INDEX];
    }

    @Override
    public ManagementServiceStrategy getManagementServiceStrategy() {
        return (ManagementServiceStrategy) managementStrategy[SERVICE_INDEX];
    }

    @Override
    public ManagementSessionStrategy getManagementSessionStrategy() {
        return (ManagementSessionStrategy) managementStrategy[SESSION_INDEX];
    }

    /**
     * Create a ServiceManagementBean for a resource address on the local Gateway instance.
     * <p/>
     * XXX We need to do something more if we're going to support some idea of storing services from another Gateway instance in
     * the same repository, as we won't generally have ServiceContext to work with (since the other instance will be in a
     * different process, perhaps on a different machine.)
     */
    @Override
    public void addServiceManagementBean(ServiceContext serviceContext) {
        GatewayManagementBean gatewayManagementBean = getLocalGatewayManagementBean();

        ServiceManagementBean serviceManagementBean = serviceManagmentBeanFactory.newServiceManagementBean(
                serviceContext.getServiceType(), gatewayManagementBean, serviceContext);

        ServiceManagementBean tempBean = serviceManagementBeans.putIfAbsent(serviceContext, serviceManagementBean);
        if (tempBean == null) {
            // A bean was not already created for this service
            for (ManagementServiceHandler handler : managementServiceHandlers) {
                handler.addServiceManagementBean(serviceManagementBean);
            }
        }
    }

    /**
     * Create a SessionManagementBean for a resource address on the local Gateway instance.
     * <p/>
     * XXX We need to do something more if we're going to support some idea of storing sessions from another Gateway instance in
     * the same repository, as we won't generally have ServiceContext or IoSessions to work with (since the other instance will
     * be in a different process, perhaps on a different machine.)
     * <p/>
     * NOTE: we store the service resource address on the management bean because we need it later to do protocol-specific things
     * like reconstruct JMX session mbean names. NOTE: the managementProcessors are called during the constructor, so the bean
     * has not yet been loaded into the list of sessionManagement beans.
     */
    @Override
    public SessionManagementBean addSessionManagementBean(ServiceManagementBean serviceManagementBean,
                                                          IoSessionEx session) {
        SessionManagementBean sessionManagementBean =
                new SessionManagementBeanImpl(serviceManagementBean, session);

        for (ManagementServiceHandler handler : managementServiceHandlers) {
            handler.addSessionManagementBean(sessionManagementBean);
        }

        return sessionManagementBean;
    }

    @Override
    public void updateManagementContext(SecurityContext securityContext) {
        if (managementConfigured.compareAndSet(false, true)) {
            GatewayManagementBean gatewayBean = getLocalGatewayManagementBean();

            // services
            Collection<? extends ServiceContext> services = gatewayContext.getServices();
            if ((services != null) && !services.isEmpty()) {
                for (ServiceContext service : services) {
                    addServiceConfigurationBean(service, gatewayBean);
                }
            }

            addVersionInfo(gatewayBean);

            ClusterContext clusterContext = gatewayContext.getCluster();
            if (clusterContext != null) {
                addClusterConfigurationBean(clusterContext, gatewayBean);
            }

            addSecurityConfigurationBean(securityContext, gatewayBean);

            Collection<? extends RealmContext> realms = gatewayContext.getRealms();
            if ((realms != null) && !realms.isEmpty()) {
                for (RealmContext realm : realms) {
                    addRealmConfigurationBean(realm, gatewayBean);
                }
            }

            ServiceDefaultsContext serviceDefaults = gatewayContext.getServiceDefaults();
            if (serviceDefaults != null) {
                addServiceDefaultsConfigurationBean(serviceDefaults, gatewayBean);
            }
        }
    }

    @Override
    public SummaryManagementInterval getGatewaySummaryDataNotificationInterval() {
        return gatewaySummaryDataNotificationInterval;
    }

    @Override
    public SummaryManagementInterval getServiceSummaryDataNotificationInterval() {
        return serviceSummaryDataNotificationInterval;
    }

    @Override
    public SummaryManagementInterval getSessionSummaryDataNotificationInterval() {
        return sessionSummaryDataNotificationInterval;
    }

    private final class SummaryManagementIntervalImpl implements SummaryManagementInterval {
        private int interval;

        private SummaryManagementIntervalImpl(int interval) {
            this.interval = interval;
        }

        @Override
        public int getInterval() {
            return interval;
        }

        @Override
        public void setInterval(int interval) {
            this.interval = interval;
        }
    }

    @Override
    public String getName() {
        return "managementContext";
    }

    @Override
    public void createGatewayManagementBean() {
        GatewayManagementBean gatewayManagementBean = new GatewayManagementBeanImpl(this, this.gatewayContext,
                localGatewayHostAndPid);

        gatewayManagementBeans.put(localGatewayHostAndPid, gatewayManagementBean);
    }

}

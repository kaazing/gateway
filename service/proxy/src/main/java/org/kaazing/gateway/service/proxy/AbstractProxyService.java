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
package org.kaazing.gateway.service.proxy;

import static org.kaazing.gateway.service.util.ServiceUtils.getOptionalDataSizeProperty;
import static org.kaazing.gateway.service.util.ServiceUtils.getOptionalIntProperty;
import static org.kaazing.gateway.service.util.ServiceUtils.getOptionalProperty;

import java.util.concurrent.ScheduledExecutorService;
import java.util.function.IntFunction;

import javax.annotation.Resource;

import org.apache.mina.core.session.IoSession;
import org.kaazing.gateway.service.Service;
import org.kaazing.gateway.service.ServiceContext;
import org.kaazing.gateway.service.ServiceProperties;
import org.kaazing.gateway.util.scheduler.SchedulerProvider;


/**
 * Gateway service of type "proxy".
 */
public abstract class AbstractProxyService<HandlerType extends AbstractProxyHandler> implements Service {

    private static final String PROPERTY_MAXIMUM_PENDING_BYTES = "maximum.pending.bytes";
    private static final String PROPERTY_MAXIMUM_RECOVERY_INTERVAL = "maximum.recovery.interval";
    private static final String PROPERTY_PREPARED_CONNECTION_COUNT = "prepared.connection.count";
    private static final String PROPERTY_CONNECT_STRATEGY = "connect.strategy";
    private static final String PROPERTY_MAXIMUM_TRANSFERRED_BYTES = "internal.maximum.transferred.bytes";

    private static final int PROPERTY_MAXIMUM_PENDING_BYTES_DEFAULT = 64000;
    private static final int PROPERTY_MAXIMUM_RECOVERY_INTERVAL_DEFAULT = 0;
    private static final int PROPERTY_PREPARED_CONNECTION_COUNT_DEFAULT = 0;
    private static final int PROPERTY_MAXIMUM_TRANSFERRED_BYTES_DEFAULT = -1;
    private static final IntFunction<String> PROPERTY_CONNECT_STRATEGY_DEFAULT = count -> count > 0 ? "prepared" : "immediate";

    protected HandlerType handler;
    private ServiceContext serviceContext;

    protected ScheduledExecutorService scheduler;

    @Resource(name = "schedulerProvider")
    public void setSchedulerProvider(SchedulerProvider provider) {
        this.scheduler = provider.getScheduler("Scheduler-AbstractProxyService", false);
    }

    @Override
    public void init(ServiceContext serviceContext) throws Exception {
        this.serviceContext = serviceContext;

        // lookup service properties
        ServiceProperties properties = serviceContext.getProperties();
        int maximumPendingBytes = getOptionalDataSizeProperty(properties, PROPERTY_MAXIMUM_PENDING_BYTES, PROPERTY_MAXIMUM_PENDING_BYTES_DEFAULT);
        int maximumTransferredBytes = getOptionalDataSizeProperty(properties, PROPERTY_MAXIMUM_TRANSFERRED_BYTES, PROPERTY_MAXIMUM_TRANSFERRED_BYTES_DEFAULT);
        int maximumRecoveryInterval = getOptionalIntProperty(properties, PROPERTY_MAXIMUM_RECOVERY_INTERVAL, PROPERTY_MAXIMUM_RECOVERY_INTERVAL_DEFAULT);
        int preparedConnectionCount = getOptionalIntProperty(properties, PROPERTY_PREPARED_CONNECTION_COUNT, PROPERTY_PREPARED_CONNECTION_COUNT_DEFAULT);
        String connectStrategy = getOptionalProperty(properties, PROPERTY_CONNECT_STRATEGY, PROPERTY_CONNECT_STRATEGY_DEFAULT.apply(preparedConnectionCount));

        handler = createHandler();
        handler.setServiceContext(serviceContext);
        handler.setMaximumPendingBytes(maximumPendingBytes);
        handler.setMaximumTransferredBytes(maximumTransferredBytes);
        handler.setMaximumRecoveryInterval(maximumRecoveryInterval);
        handler.setPreparedConnectionCount(connectStrategy, preparedConnectionCount, serviceContext.getProcessorCount());
    }

    @Override
    public void start() throws Exception {
        serviceContext.bind(serviceContext.getAccepts(), handler);
        serviceContext.bindConnectsIfNecessary(serviceContext.getConnects());
    }

    @Override
    public void stop() throws Exception {
        quiesce();

        // defer until stop to allow connect to succeed and re-enable the service
        serviceContext.unbindConnectsIfNecessary(serviceContext.getConnects());

        if (serviceContext != null) {
            for (IoSession session : serviceContext.getActiveSessions()) {
                session.close(true);
            }
        }
    }

    @Override
    public void quiesce() throws Exception {
        if (serviceContext != null) {
            serviceContext.unbind(serviceContext.getAccepts(), handler);
        }
    }

    @Override
    public void destroy() throws Exception {
    }

    protected abstract HandlerType createHandler();

    protected HandlerType getHandler() {
        return handler;
    }
}

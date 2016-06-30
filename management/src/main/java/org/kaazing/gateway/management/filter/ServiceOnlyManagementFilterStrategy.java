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
package org.kaazing.gateway.management.filter;

import org.apache.mina.core.write.WriteRequest;
import org.kaazing.gateway.management.Utils.ManagementSessionType;
import org.kaazing.gateway.management.context.ManagementContext;
import org.kaazing.gateway.management.service.ServiceManagementBean;
import org.kaazing.mina.core.session.IoSessionEx;

/**
 * Do full management processing for both service and session. This is only done on non-management session requests.
 * <p/>
 * For this strategy, we just collect stats at the service level and do NOTHING at the session level, including creating
 * session-management beans.
 * <p/>
 * Note that for the filter strategy, Service processing is the same between service-collect-only and service-full. Those are
 * handled at the service level.
 * <p/>
 * ALL REQUESTS WILL BE ON ONE OR ANOTHER OF THE IO THREADS, SO MUST NOT BLOCK!
 */
public class ServiceOnlyManagementFilterStrategy extends PassThruManagementFilterStrategy {

    public ServiceOnlyManagementFilterStrategy() {
    }

    @Override
    // We must use sessionCreated because sessionOpened can fire after events like filterWrite if someone
    // (like proxy service) sets a connect future listener that does writes.
    public void doSessionCreated(ManagementContext managementContext,
                                 ServiceManagementBean serviceBean,
                                 IoSessionEx session,
                                 ManagementSessionType managementSessionType) throws Exception {
        long sessionId = session.getId();
        managementContext.getManagementServiceStrategy().doSessionCreated(serviceBean, sessionId, managementSessionType);
        managementContext.getManagementGatewayStrategy()
                .doSessionCreated(serviceBean.getGatewayManagementBean(), sessionId, managementSessionType);

        // We know the super does nothing, so comment it out here. If we change to include
        // something else in between, undo the comment.
        //super.doSessionCreated(managementContext, serviceBean, session, managementSessionType);
    }

    @Override
    public void doSessionClosed(ManagementContext managementContext,
                                ServiceManagementBean serviceBean,
                                long sessionId,
                                ManagementSessionType managementSessionType) throws Exception {
        managementContext.getManagementServiceStrategy().doSessionClosed(serviceBean, sessionId, managementSessionType);
        managementContext.getManagementGatewayStrategy()
                .doSessionClosed(serviceBean.getGatewayManagementBean(), sessionId, managementSessionType);

        // We know the super does nothing, so comment it out here. If we change to include
        // something else in between, undo the comment.
        //super.doSessionClosed(managementContext, serviceBean, sessionId, managementSessionType);
    }

    @Override
    public void doMessageReceived(ManagementContext managementContext,
                                  ServiceManagementBean serviceBean,
                                  long sessionId,
                                  long sessionReadBytes,
                                  Object message) throws Exception {
        managementContext.getManagementServiceStrategy().doMessageReceived(serviceBean, sessionId, sessionReadBytes, message);
        managementContext.getManagementGatewayStrategy()
                .doMessageReceived(serviceBean.getGatewayManagementBean(), sessionId, sessionReadBytes, message);

        // We know the super does nothing, so comment it out here. If we change to include
        // something else in between, undo the comment.
        //super.doMessageReceived(managementContext, serviceBean, sessionId, sessionReadBytes, message);
    }

    @Override
    public void doFilterWrite(ManagementContext managementContext,
                              ServiceManagementBean serviceBean,
                              long sessionId,
                              long sessionWrittenBytes,
                              WriteRequest writeRequest) throws Exception {
        managementContext.getManagementServiceStrategy()
                .doFilterWrite(serviceBean, sessionId, sessionWrittenBytes, writeRequest);
        managementContext.getManagementGatewayStrategy()
                .doFilterWrite(serviceBean.getGatewayManagementBean(), sessionId, sessionWrittenBytes, writeRequest);

        // We know the super does nothing, so comment it out here. If we change to include
        // something else in between, undo the comment.
        //super.doFilterWrite(managementContext, serviceBean, sessionId, sessionWrittenBytes, writeRequest);
    }

    @Override
    public void doExceptionCaught(ManagementContext managementContext,
                                  ServiceManagementBean serviceBean,
                                  long sessionId,
                                  Throwable cause) throws Exception {
        managementContext.getManagementServiceStrategy().doExceptionCaught(serviceBean, sessionId, cause);
        managementContext.getManagementGatewayStrategy()
                .doExceptionCaught(serviceBean.getGatewayManagementBean(), sessionId, cause);

        // We know the super does nothing, so comment it out here. If we change to include
        // something else in between, undo the comment.
        //super.doExceptionCaught(managementContext, serviceBean, sessionId, cause);
    }

    public String toString() {
        return "SERVICE_ONLY_FILTER_STRATEGY";
    }
}


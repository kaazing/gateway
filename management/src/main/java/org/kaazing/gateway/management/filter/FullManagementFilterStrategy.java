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

import org.apache.mina.core.session.AttributeKey;
import org.apache.mina.core.write.WriteRequest;
import org.kaazing.gateway.management.Utils.ManagementSessionType;
import org.kaazing.gateway.management.context.ManagementContext;
import org.kaazing.gateway.management.service.ServiceManagementBean;
import org.kaazing.gateway.management.session.SessionManagementBean;
import org.kaazing.mina.core.session.IoSessionEx;

/**
 * Do full management processing for both service and session. This is only done on non-management session requests.
 * <p/>
 * ALL REQUESTS WILL BE ON ONE OR ANOTHER OF THE IO THREADS, SO MUST NOT BLOCK!
 */
public class FullManagementFilterStrategy extends ServiceOnlyManagementFilterStrategy {

    public static final AttributeKey SESSION_BEAN_ATTRIBUTE_KEY =
            new AttributeKey(ManagementFilterStrategy.class, "managementSessionBean");

    // We must use sessionCreated because sessionOpened can fire after events like filterWrite if someone
    // (like proxy service) sets a connect future listener that does writes.
    @Override
    public void doSessionCreated(ManagementContext managementContext,
                                 ServiceManagementBean serviceBean,
                                 IoSessionEx session,
                                 ManagementSessionType managementSessionType) throws Exception {
        SessionManagementBean sessionBean = managementContext.addSessionManagementBean(serviceBean, session);
        serviceBean.storeSessionManagementBean(sessionBean);

        managementContext.getManagementSessionStrategy().doSessionCreated(sessionBean);

        super.doSessionCreated(managementContext, serviceBean, session, managementSessionType);
    }

    @Override
    public void doSessionClosed(ManagementContext managementContext,
                                ServiceManagementBean serviceBean,
                                long sessionId,
                                ManagementSessionType managementSessionType) throws Exception {
        // KG-2951: if sessionCreated calls session.close() because maximum licensed number of
        // connections has been exceeded, sessionBean will be null because sessionOpened has not fired,
        // XXX Now that we create the bean in doSessionCreated, is it still possible for sessionBean to be null here?
        SessionManagementBean sessionBean = serviceBean.removeSessionManagementBean(sessionId);

        if (sessionBean != null) {
            managementContext.getManagementSessionStrategy().doSessionClosed(sessionBean);
        }

        super.doSessionClosed(managementContext, serviceBean, sessionId, managementSessionType);
    }

    @Override
    public void doMessageReceived(ManagementContext managementContext,
                                  ServiceManagementBean serviceBean,
                                  long sessionId,
                                  long sessionBytesRead,
                                  Object message) throws Exception {
        // XXX Now that we create the bean in doSessionCreated, is it still possible for sessionBean to be null here?
        SessionManagementBean sessionBean = serviceBean.getSessionManagementBean(sessionId);

        if (sessionBean != null) {
            managementContext.getManagementSessionStrategy().doMessageReceived(sessionBean, message);
        }

        super.doMessageReceived(managementContext, serviceBean, sessionId, sessionBytesRead, message);
    }

    @Override
    public void doFilterWrite(ManagementContext managementContext,
                              ServiceManagementBean serviceBean,
                              long sessionId,
                              long sessionBytesWritten,
                              WriteRequest writeRequest) throws Exception {
        // XXX Now that we create the bean in doSessionCreated, is it still possible for sessionBean to be null here?
        SessionManagementBean sessionBean = serviceBean.getSessionManagementBean(sessionId);

        if (sessionBean != null) {
            managementContext.getManagementSessionStrategy().doFilterWrite(sessionBean, writeRequest);
        }

        super.doFilterWrite(managementContext, serviceBean, sessionId, sessionBytesWritten, writeRequest);
    }

    @Override
    public void doExceptionCaught(ManagementContext managementContext,
                                  ServiceManagementBean serviceBean,
                                  long sessionId,
                                  Throwable cause) throws Exception {
        // XXX Now that we create the bean in doSessionCreated, is it still possible for sessionBean to be null here?
        SessionManagementBean sessionBean = serviceBean.getSessionManagementBean(sessionId);

        if (sessionBean != null) {
            managementContext.getManagementSessionStrategy().doExceptionCaught(sessionBean, cause);
        }

        super.doExceptionCaught(managementContext, serviceBean, sessionId, cause);
    }

    public String toString() {
        return "FULL_FILTER_STRATEGY";
    }
}


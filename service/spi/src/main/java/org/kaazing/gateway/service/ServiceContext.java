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
package org.kaazing.gateway.service;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.session.IoSessionInitializer;
import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.security.CrossSiteConstraintContext;
import org.kaazing.gateway.security.RealmContext;
import org.kaazing.gateway.transport.BridgeSessionInitializer;
import org.kaazing.gateway.util.scheduler.SchedulerProvider;
import org.kaazing.mina.core.session.IoSessionEx;
import org.slf4j.Logger;

public interface ServiceContext {

    String SESSION_FILTER_NAME = "sessiontracker";

    RealmContext getServiceRealm();

    String getAuthorizationMode();

    String getSessionTimeout();

    String getServiceType();

    String getServiceName();

    String getServiceDescription();

    Collection<String> getAccepts();

    Collection<String> getBalances();

    Collection<String> getConnects();
    
    Map<String, String> getMimeMappings();

    ServiceProperties getProperties();

    Service getService();

    String[] getRequireRoles();
    
    /**
     * Return a MIME-type string for a given file extension, per the list of
     * <mime-mappings> defined in gateway-config for this service and
     * in <service-defaults>.  If the parameter is null or no MIME-type 
     * is configured for the given value, returns null.
     */
    String getContentType(String fileExtension);

    Map<String, ? extends Map<String, ? extends CrossSiteConstraintContext>> getCrossSiteConstraints();

    File getWebDirectory();

    File getTempDirectory();

    void init() throws Exception;

    void start() throws Exception;

    void bind(Collection<String> acceptURIs, IoHandler handler);

    void bind(Collection<String> acceptURIs, IoHandler handler,
              AcceptOptionsContext acceptOptionsContext);
    
    void bind(Collection<String> acceptURIs, IoHandler handler, AcceptOptionsContext acceptOptionsContext, BridgeSessionInitializer<ConnectFuture> bridgeSessionInitializer);
    
    void bind(Collection<String> acceptURIs, IoHandler handler, BridgeSessionInitializer<ConnectFuture> bridgeSessionInitializer);

    void bindConnectsIfNecessary(Collection<String> connectURIs);

    void unbind(Collection<String> acceptURIs, IoHandler handler);

    void unbindConnectsIfNecessary(Collection<String> connectURIs);

    void stop() throws Exception;

    void destroy() throws Exception;
    
    ConnectFuture connect(String connectURI, IoHandler connectHandler,
                          IoSessionInitializer<ConnectFuture> ioSessionInitializer);

    ConnectFuture connect(ResourceAddress address, IoHandler connectHandler,
                          IoSessionInitializer<ConnectFuture> connectSessionInitializer);

    Collection<IoSessionEx> getActiveSessions();
    
    IoSessionEx getActiveSession(Long sessionId);
    
    void addActiveSession(IoSessionEx session);
    
    void removeActiveSession(IoSessionEx session);

//    public ClusterContext getClusterContext();

    AcceptOptionsContext getAcceptOptionsContext();

    ConnectOptionsContext getConnectOptionsContext();

    Logger getLogger();

    SchedulerProvider getSchedulerProvider();

    String decrypt(String encrypted) throws Exception;

    String encrypt(String plaintext) throws Exception;

    /**
     * For help with the Console, indicate whether the service supports accepts
     * and accept options.
     */
    boolean supportsAccepts();

    /**
     * For help with the Console, indicate whether the service supports connects
     * and connect options.
     */
    boolean supportsConnects();

    /**
     * For help with the Console, indicate whether the service supports connects
     * and connect options.
     */
    boolean supportsMimeMappings();

    int getProcessorCount();

    void setListsOfAcceptConstraintsByURI(List<Map<String, Map<String, CrossSiteConstraintContext>>> authorityToSetOfAcceptConstraintsByURI);

    // Used by the update.check.service, could be used by other services
    Map<String, Object> getServiceSpecificObjects();

    IoSessionInitializer<ConnectFuture> getSessionInitializor();

    void setSessionInitializor(IoSessionInitializer<ConnectFuture> ioSessionInitializer);

    MonitoringEntityFactory getMonitoringFactory();

    void setMonitoringFactory(MonitoringEntityFactory monitoringFactory);
    
}

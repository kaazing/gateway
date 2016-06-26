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

    /**
     * TODO Add method documentation
     * 
     * @return
     */
    RealmContext getServiceRealm();

    /**
     * TODO Add method documentation
     * 
     * @return
     */
    String getAuthorizationMode();

    /**
     * TODO Add method documentation
     * 
     * @return
     */
    String getSessionTimeout();

    /**
     * TODO Add method documentation
     * 
     * @return
     */
    String getServiceType();

    /**
     * TODO Add method documentation
     * 
     * @return
     */
    String getServiceName();

    /**
     * TODO Add method documentation
     * 
     * @return
     */
    String getServiceDescription();

    /**
     * TODO Add method documentation
     * 
     * @return
     */
    Collection<String> getAccepts();

    /**
     * TODO Add method documentation
     * 
     * @return
     */
    Collection<String> getBalances();

    /**
     * TODO Add method documentation
     * 
     * @return
     */
    Collection<String> getConnects();
    
    /**
     * TODO Add method documentation
     * 
     * @return
     */
    Map<String, String> getMimeMappings();

    /**
     * TODO Add method documentation
     * 
     * @return
     */
    ServiceProperties getProperties();

    /**
     * TODO Add method documentation
     * 
     * @return
     */
    Service getService();

    /**
     * TODO Add method documentation
     * 
     * @return
     */
    String[] getRequireRoles();
    
    /**
     * Return a MIME-type string for a given file extension, per the list of
     * <mime-mappings> defined in gateway-config for this service and
     * in <service-defaults>.  If the parameter is null or no MIME-type 
     * is configured for the given value, returns null.
     * 
     * @param fileExtension
     * @return
     */
    String getContentType(String fileExtension);

    /**
     * TODO Add method documentation
     * 
     * @return
     */
    Map<String, ? extends Map<String, ? extends CrossSiteConstraintContext>> getCrossSiteConstraints();

    /**
     * TODO Add method documentation
     * 
     * @return
     */
    File getWebDirectory();

    /**
     * TODO Add method documentation
     * 
     * @return
     */
    File getTempDirectory();

    /**
     * TODO Add method documentation
     */
    void init() throws Exception;

    /**
     * TODO Add method documentation
     */
    void start() throws Exception;

    /**
     * TODO Add method documentation
     * 
     * @param acceptURIs
     * @param handler
     */
    void bind(Collection<String> acceptURIs, IoHandler handler);

    void bind(Collection<String> acceptURIs, IoHandler handler,
              AcceptOptionsContext acceptOptionsContext);
    
    void bind(Collection<String> acceptURIs, IoHandler handler, AcceptOptionsContext acceptOptionsContext, BridgeSessionInitializer<ConnectFuture> bridgeSessionInitializer);
    
    void bind(Collection<String> acceptURIs, IoHandler handler, BridgeSessionInitializer<ConnectFuture> bridgeSessionInitializer);

    /**
     * TODO Add method documentation
     * 
     * @param connectURIs
     */
    void bindConnectsIfNecessary(Collection<String> connectURIs);

    /**
     * TODO Add method documentation
     * 
     * @param acceptURIs
     * @param handler
     */
    void unbind(Collection<String> acceptURIs, IoHandler handler);

    /**
     * TODO Add method documentation
     * 
     * @param connectURIs
     */
    void unbindConnectsIfNecessary(Collection<String> connectURIs);

    /**
     * TODO Add method documentation
     */
    void stop() throws Exception;

    /**
     * TODO Add method documentation
     */
    void destroy() throws Exception;
    
    /**
     * TODO Add method documentation
     * 
     * @param connectURI
     * @param connectHandler
     * @param ioSessionInitializer
     * @return
     */
    ConnectFuture connect(String connectURI, IoHandler connectHandler,
                          IoSessionInitializer<ConnectFuture> ioSessionInitializer);

    /**
     * TODO Add method documentation
     * 
     * @param address
     * @param connectHandler
     * @param connectSessionInitializer
     * @return
     */
    ConnectFuture connect(ResourceAddress address, IoHandler connectHandler,
                          IoSessionInitializer<ConnectFuture> connectSessionInitializer);

    /**
     * TODO Add method documentation
     * 
     * @return
     */
    Collection<IoSessionEx> getActiveSessions();
    
    /**
     * TODO Add method documentation
     * 
     * @param sessionId
     * @return
     */
    IoSessionEx getActiveSession(Long sessionId);
    
    /**
     * TODO Add method documentation
     * 
     * @param session
     */
    void addActiveSession(IoSessionEx session);
    
    /**
     * TODO Add method documentation
     * 
     * @param session
     */
    void removeActiveSession(IoSessionEx session);

//    public ClusterContext getClusterContext();

    AcceptOptionsContext getAcceptOptionsContext();

    /**
     * TODO Add method documentation
     * 
     * @return
     */
    ConnectOptionsContext getConnectOptionsContext();

    /**
     * TODO Add method documentation
     * 
     * @return
     */
    Logger getLogger();

    /**
     * TODO Add method documentation
     * 
     * @return
     */
    SchedulerProvider getSchedulerProvider();

    /**
     * TODO Add method documentation
     * 
     * @param encrypted
     * @return
     */
    String decrypt(String encrypted) throws Exception;

    /**
     * TODO Add method documentation
     * 
     * @param plaintext
     * @return
     */
    String encrypt(String plaintext) throws Exception;

    /**
     * For help with the Console, indicate whether the service supports accepts
     * and accept options.
     * 
     * @return
     */
    boolean supportsAccepts();

    /**
     * For help with the Console, indicate whether the service supports connects
     * and connect options.
     * 
     * @return
     */
    boolean supportsConnects();

    /**
     * For help with the Console, indicate whether the service supports connects
     * and connect options.
     * 
     * @return
     */
    boolean supportsMimeMappings();

    /**
     * TODO Add method documentation
     * 
     * @return
     */
    int getProcessorCount();

    /**
     * TODO Add method documentation
     * 
     * @param authorityToSetOfAcceptConstraintsByURI
     */
    void setListsOfAcceptConstraintsByURI(List<Map<String, Map<String, CrossSiteConstraintContext>>> authorityToSetOfAcceptConstraintsByURI);

    /**
     * Used by the update.check.service, could be used by other services
     * 
     * @return
     */
    Map<String, Object> getServiceSpecificObjects();

    /**
     * TODO Add method documentation
     * 
     * @return
     */
    IoSessionInitializer<ConnectFuture> getSessionInitializor();

    /**
     * TODO Add method documentation
     * 
     * @param ioSessionInitializer
     */
    void setSessionInitializor(IoSessionInitializer<ConnectFuture> ioSessionInitializer);

    /**
     * TODO Add method documentation
     * 
     * @return
     */
    MonitoringEntityFactory getMonitoringFactory();

    /**
     * TODO Add method documentation
     * 
     * @param monitoringFactory
     */
    void setMonitoringFactory(MonitoringEntityFactory monitoringFactory);
    
}

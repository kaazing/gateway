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

package org.kaazing.gateway.service;

import java.io.File;
import java.net.URI;
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

    public static final String SESSION_FILTER_NAME = "sessiontracker";

    public RealmContext getServiceRealm();

    public String getAuthorizationMode();

    public String getSessionTimeout();

    public String getServiceType();

    public String getServiceName();

    public String getServiceDescription();

    public Collection<URI> getAccepts();

    public Collection<URI> getBalances();

    public Collection<URI> getConnects();
    
    public Map<String, String> getMimeMappings();

    public ServiceProperties getProperties();

    public Service getService();

    public String[] getRequireRoles();
    
    /**
     * Return a MIME-type string for a given file extension, per the list of
     * <mime-mappings> defined in gateway-config for this service and
     * in <service-defaults>.  If the parameter is null or no MIME-type 
     * is configured for the given value, returns null.
     */
    public String getContentType(String fileExtension);

    public Map<URI, ? extends Map<String, ? extends CrossSiteConstraintContext>> getCrossSiteConstraints();

    public File getWebDirectory();

    public File getTempDirectory();

    public void init() throws Exception;

    public void start() throws Exception;

    public void bind(Collection<URI> acceptURIs, IoHandler handler);

	public void bind(Collection<URI> acceptURIs, IoHandler handler,
			AcceptOptionsContext acceptOptionsContext);
	
	public void bind(Collection<URI> acceptURIs, IoHandler handler, AcceptOptionsContext acceptOptionsContext, BridgeSessionInitializer<ConnectFuture> bridgeSessionInitializer);
    
	public void bind(Collection<URI> acceptURIs, IoHandler handler, BridgeSessionInitializer<ConnectFuture> bridgeSessionInitializer);

    public void bindConnectsIfNecessary(Collection<URI> connectURIs);

    public void unbind(Collection<URI> acceptURIs, IoHandler handler);

    public void unbindConnectsIfNecessary(Collection<URI> connectURIs);

    public void stop() throws Exception;

    public void destroy() throws Exception;
    
    public ConnectFuture connect(URI connectURI, IoHandler connectHandler,
            IoSessionInitializer<ConnectFuture> ioSessionInitializer);

    public ConnectFuture connect(ResourceAddress address, IoHandler connectHandler,
            IoSessionInitializer<ConnectFuture> connectSessionInitializer);

    public Collection<IoSessionEx> getActiveSessions();
	
    public IoSessionEx getActiveSession(Long sessionId);
	
    public void addActiveSession(IoSessionEx session);
	
    public void removeActiveSession(IoSessionEx session);

//    public ClusterContext getClusterContext();

    public AcceptOptionsContext getAcceptOptionsContext();

    public ConnectOptionsContext getConnectOptionsContext();

    public Logger getLogger();

    public SchedulerProvider getSchedulerProvider();

    public String decrypt(String encrypted) throws Exception;

    public String encrypt(String plaintext) throws Exception;

    /**
     * For help with the Console, indicate whether the service supports accepts
     * and accept options.
     */
    public boolean supportsAccepts();

    /**
     * For help with the Console, indicate whether the service supports connects
     * and connect options.
     */
    public boolean supportsConnects();

    /**
     * For help with the Console, indicate whether the service supports connects
     * and connect options.
     */
    public boolean supportsMimeMappings();

    public int getProcessorCount();

    public void setListsOfAcceptConstraintsByURI(List<Map<URI, Map<String, CrossSiteConstraintContext>>> authorityToSetOfAcceptConstraintsByURI);

    // Used by the update.check.service, could be used by other services
    public Map<String, Object> getServiceSpecificObjects();

    public IoSessionInitializer<ConnectFuture> getSessionInitializor();

    public void setSessionInitializor(IoSessionInitializer<ConnectFuture> ioSessionInitializer);

}

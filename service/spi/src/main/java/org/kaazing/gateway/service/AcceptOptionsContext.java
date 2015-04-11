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

import java.net.URI;
import java.util.List;
import java.util.Map;

@Deprecated
public interface AcceptOptionsContext {

    Map<String, Object> asOptionsMap();

    boolean isSslEncryptionEnabled();

    URI getInternalURI(URI externalURI);

    List<String> getWsProtocols();

    List<String> getWsExtensions();

    String[] getSslCiphers();
    String[] getSslProtocols();

    boolean getSslWantClientAuth();
    boolean getSslNeedClientAuth();

    URI getTcpTransport();
    URI getSslTransport();
    URI getHttpTransport();

    String getUdpInterface();
    URI getPipeTransport();

    /**
     * Add a binding to the accept-options from the given scheme to the given authority.  If a service needs to
     * use a specific accept URI, e.g. wsn://<authority>/<path>, then it might need to add a binding for the
     * scheme of that URI in order to preserve the behavior of the configuration.  In the given example, having
     * <ws.bind>some_port</ws.bind> in the accept-options for the service will result in a failure to bind to
     * some_port due to the change in scheme in the URI.  By allowing the service to add the necessary binding,
     * the configured behavior will be preserved.
     *
     * An example of a service that needs to add bindings at runtime is the HttpBalancerService.
     *
     * @param scheme
     * @param authority
     */
    void addBind(String scheme, String authority);

    /**
     * @return the configured binds for a service
     */
    Map<String, String> getBinds();

    /**
     * @return The configured maximum WebSocket message size, or 0 meaning no limit if none is configured
     */
    int getWsMaxMessageSize();


    /**
     * @return The configured WebSocket inactivity timeout (for check alive feature), in milliseconds
     */
    long getWsInactivityTimeout();

    /**
     *
     * @return The configured maximum outbound rate in bytes per second. 0xFFFFFFFF (max unsigned int) means no limit.
     */
    long getTcpMaximumOutboundRate();

    /**
     * @return The configured idle timeout for the given protocol scheme, or null if there is none
     *         configured and there is no default for the scheme
     */
    Integer getSessionIdleTimeout(String scheme);
    
    Integer getHttpKeepaliveTimeout();

    class Wrapper implements AcceptOptionsContext {

        private final AcceptOptionsContext delegate;

        public Wrapper(AcceptOptionsContext delegate) {
            this.delegate = delegate;
        }

        @Override
        public List<String> getWsProtocols() {
            return delegate.getWsProtocols();
        }

        @Override
        public List<String> getWsExtensions() {
            return delegate.getWsExtensions();
        }

        @Override
        public void addBind(String scheme, String authority) {
            delegate.addBind(scheme, authority);
        }

        @Override
        public Map<String, String> getBinds() {
            return delegate.getBinds();
        }

        @Override
        public URI getInternalURI(URI externalURI) {
            return delegate.getInternalURI(externalURI);
        }

        @Override
        public Integer getSessionIdleTimeout(String scheme) {
            return delegate.getSessionIdleTimeout(scheme);
        }

        @Override
        public int getWsMaxMessageSize() {
            return delegate.getWsMaxMessageSize();
        }

        public long getWsInactivityTimeout() {
            return delegate.getWsInactivityTimeout();
        }

        @Override
        public long getTcpMaximumOutboundRate() {
            return delegate.getTcpMaximumOutboundRate();
        }

    	@Override
    	public String[] getSslCiphers() {
    		return delegate.getSslCiphers();
    	}

        @Override
        public String[] getSslProtocols() {
            return delegate.getSslProtocols();
        }

        @Override
        public Map<String, Object> asOptionsMap() {
            return delegate.asOptionsMap();
        }

        @Override
        public boolean isSslEncryptionEnabled() {
            return delegate.isSslEncryptionEnabled();
        }

        @Override
        public boolean getSslWantClientAuth() {
            return delegate.getSslWantClientAuth();
        }

        @Override
        public boolean getSslNeedClientAuth() {
            return delegate.getSslNeedClientAuth();
        }

        @Override
        public URI getTcpTransport() {
            return delegate.getTcpTransport();
        }

        @Override
        public URI getSslTransport() {
            return delegate.getSslTransport();
        }

        @Override
        public URI getHttpTransport() {
            return delegate.getHttpTransport();
        }

        @Override
        public Integer getHttpKeepaliveTimeout() {
            return delegate.getHttpKeepaliveTimeout();
        }

        @Override
        public String getUdpInterface() {
            return delegate.getUdpInterface();
        }

        @Override
        public URI getPipeTransport() {
            return delegate.getPipeTransport();
        }
    }
}

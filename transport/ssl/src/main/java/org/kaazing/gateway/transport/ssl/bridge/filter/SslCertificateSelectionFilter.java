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
package org.kaazing.gateway.transport.ssl.bridge.filter;

import static org.kaazing.gateway.resource.address.Comparators.compareResourceOriginAndProtocolStack;

import java.util.Collection;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentMap;

import org.kaazing.gateway.transport.TransportKeySelector;
import org.apache.mina.core.filterchain.IoFilterAdapter;
import org.apache.mina.core.session.IoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.transport.ssl.cert.DefaultKeySelector;
import org.kaazing.mina.netty.util.threadlocal.VicariousThreadLocal;

public class SslCertificateSelectionFilter
    extends IoFilterAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(SslCertificateSelectionFilter.class);

    // Convenience class used to reduce the number of ThreadLocals needed
    // for stashing the session/key/aliases for the client/server threads.
    private static class AvailableCertInfo {
        private IoSession session;
        private ResourceAddress certAliasesKey;
        private ConcurrentMap<ResourceAddress, Collection<String>> certAliases;

        public AvailableCertInfo() {
            certAliases = new ConcurrentSkipListMap<>(compareResourceOriginAndProtocolStack());
        }

        public IoSession getSession() {
            return session;
        }

        public void setSession(IoSession session) {
            this.session = session;
        }

        public ResourceAddress getCertAliasesKey() {
            return certAliasesKey;
        }

        public void setCertAliasesKey(ResourceAddress certAliasesKey) {
            this.certAliasesKey = certAliasesKey;
        }

        public ConcurrentMap<ResourceAddress, Collection<String>> getCertAliases() {
            return certAliases;
        }
    }

    private static final ThreadLocal<AvailableCertInfo> availClientCertInfo = new VicariousThreadLocal<AvailableCertInfo>() {
        @Override
        protected AvailableCertInfo initialValue() {
            return new AvailableCertInfo();
        }
    };

    private static final ThreadLocal<AvailableCertInfo> availServerCertInfo = new VicariousThreadLocal<AvailableCertInfo>() {
        @Override
        protected AvailableCertInfo initialValue() {
            return new AvailableCertInfo();
        }
    };

    private TransportKeySelector keySelector = new DefaultKeySelector();
    private boolean clientMode = false;

    public SslCertificateSelectionFilter(boolean clientMode) {
        this.clientMode = clientMode;
    }

    public static Collection<String> getAvailableClientCertAliases() {
        AvailableCertInfo clientInfo = availClientCertInfo.get();
        if (clientInfo == null) {
            return null;
        }

        ConcurrentMap<ResourceAddress, Collection<String>> certAliases = clientInfo.getCertAliases();
        if (certAliases == null ||
            certAliases.isEmpty()) {
            return null;
        }

        return certAliases.get(clientInfo.getCertAliasesKey());
    }

    public static Collection<String> getAvailableServerCertAliases() {
        AvailableCertInfo serverInfo = availServerCertInfo.get();
        if (serverInfo == null) {
            return null;
        }

        ConcurrentMap<ResourceAddress, Collection<String>> certAliases = serverInfo.getCertAliases();
        if (certAliases == null ||
            certAliases.isEmpty()) {
            return null;
        }

        return certAliases.get(serverInfo.getCertAliasesKey());
    }

    public static void setAvailableCertAliases(ResourceAddress key,
                                               Collection<String> aliases,
                                               boolean clientMode) {
        AvailableCertInfo certInfo;

        if (clientMode) {
            certInfo = availClientCertInfo.get();

        } else {
            certInfo = availServerCertInfo.get();
        }

        if (key != null &&
            aliases != null) {

            certInfo.setCertAliasesKey(key);
            certInfo.getCertAliases().put(key, aliases);
        }
    }

    public static IoSession getCurrentSession(boolean client) {
        AvailableCertInfo certInfo;

        if (client) {
            certInfo = availClientCertInfo.get();

        } else {
            certInfo = availServerCertInfo.get();
        }

        return certInfo.getSession();
    }

    public static void setCurrentSession(final IoSession session,
                                         boolean client) {
        AvailableCertInfo certInfo;

        certInfo = (client ? availClientCertInfo.get() : availServerCertInfo.get());
        certInfo.setSession(session);
    }

    public boolean getClientMode() {
        return clientMode;
    }

    public void setKeySelector(TransportKeySelector keySelector) {
        this.keySelector = keySelector;
    }

    @Override
    public void messageReceived(NextFilter nextFilter,
                                IoSession session,
                                Object message) throws Exception {
 
        SslCertificateSelectionFilter.setCurrentSession(session, clientMode);

        try {
            ResourceAddress availableAliasesKey = keySelector.getAvailableCertAliasesKey(clientMode);
            Collection<String> availableAliases = keySelector.getAvailableCertAliases(clientMode);

            if (clientMode) {
                SslCertificateSelectionFilter.setAvailableCertAliases(availableAliasesKey, availableAliases, clientMode);

            } else {
                // This else clause is here in order to keep the logging that
                // we do in the server case; client cases do not need this
                // logging of sanity checks.

                if (availableAliases == null) {
                    String msg = "Available certificate aliases is null for " + availableAliasesKey;
                    LOGGER.warn(msg);

                } else if (availableAliases.isEmpty()) {
                    String msg = "Certificate not available for SSL connection on " + availableAliasesKey;
                    LOGGER.warn(msg);
                }

                SslCertificateSelectionFilter.setAvailableCertAliases(availableAliasesKey, availableAliases, clientMode);
            }

            super.messageReceived(nextFilter, session, message);

        } catch (Exception e) {
            LOGGER.warn(String.format("Unable to determine certificate aliases: %s", e), e);
            throw e;

        } finally {
            SslCertificateSelectionFilter.setCurrentSession(null, clientMode);
        }
    }
}

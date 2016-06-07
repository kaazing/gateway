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
package org.kaazing.gateway.transport.ssl;

import static org.kaazing.gateway.resource.address.ssl.SslResourceAddress.ENCRYPTION_ENABLED;

import javax.net.ssl.SSLException;

import org.apache.mina.core.filterchain.IoFilterChain;
import org.apache.mina.core.filterchain.IoFilterChain.Entry;
import org.apache.mina.core.session.IoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.kaazing.gateway.transport.BridgeConnectProcessor;
import org.kaazing.gateway.transport.ssl.bridge.filter.SslFilter;

public class SslConnectProcessor extends BridgeConnectProcessor<SslSession> {

    private final Logger logger = LoggerFactory.getLogger("transport.ssl");

    @Override
    protected void removeInternal(SslSession session) {
        try {
            // For the SSL Connector, the remote address will contain the option of whether SSL Encryption is
            // enabled or not.  If it's not enabled, no SslFilter will have been added to the filterChain, so
            // skip trying to stopSsl() in that case.
            boolean isSslEncryptionEnabled = session.getRemoteAddress().getOption(ENCRYPTION_ENABLED);
            if (isSslEncryptionEnabled) {
                IoSession parent = session.getParent();
                IoFilterChain filterChain = parent.getFilterChain();
                Entry entry = filterChain.getEntry(SslFilter.class);
                if (entry != null) {
                    SslFilter sslFilter = (SslFilter) entry.getFilter();
                    if (sslFilter.isSslStarted(parent)) {
                        // unsecuring the session will trigger
                        //  parent close from SslConnector
                        sslFilter.stopSsl(parent);
                        return;
                    }
                }
            }
        }
        catch (SSLException e) {
            logger.debug("Ignoring message during SSL shutdown due to closing session", e);
        }
        
        super.removeInternal(session);
    }
}

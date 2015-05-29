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

package org.kaazing.gateway.transport.ssl.bridge.filter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.mina.core.filterchain.IoFilterAdapter;
import org.apache.mina.core.filterchain.IoFilterChain;
import org.apache.mina.core.session.IoSession;
import org.slf4j.Logger;

import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.transport.ssl.SslAcceptor;
import org.kaazing.gateway.transport.TypedAttributeKey;
import org.kaazing.gateway.util.ssl.SslCipherSuites;

import static org.kaazing.gateway.resource.address.ssl.SslResourceAddress.CIPHERS;

public class SslCipherSelectionFilter
    extends IoFilterAdapter {

    static final TypedAttributeKey<SslVersion> SSL_PROTOCOL_VERSION = new TypedAttributeKey<>(SslCipherSelectionFilter.class, "sslProtocolVersion");
    static final TypedAttributeKey<List<String>> SSL_APPLET_CIPHERS = new TypedAttributeKey<>(SslCipherSelectionFilter.class, "sslAppletCiphers");

    // The blessed/magic ciphersuites which are approved -- for purposes
    // of jar validation -- by the Java Plugin.  Determined experimentally.
    // Subject to change without notice.
    static final String RC4_SHA = "SSL_RSA_WITH_RC4_128_SHA";
    static final String RC4_MD5 = "SSL_RSA_WITH_RC4_128_MD5";

    private final String filterName;
    private final String parentFilterName;
    private final SslFilter parentFilter;
    private final Logger logger;

    public SslCipherSelectionFilter(String filterName,
                                    String parentFilterName,
                                    SslFilter parentFilter,
                                    Logger logger) {
        this.filterName = filterName;
        this.parentFilterName = parentFilterName;
        this.parentFilter = parentFilter;
        this.logger = logger;
    }

    private void selectCiphers(IoSession session)
        throws Exception {

        /* For the purpose of this filter, see KG-7083.
         *
         *  1.  Is our ResourceAddress configured to support either/both of
         *      SSL_RSA_WITH_RC4_128_SHA, SSL_RSA_WITH_RC4_128_MD5?
         *
         *  2.  Is the ClientHello for TLS 1.0 or lower?
         *
         *  3.  Does the ClientHello offer either/both of
         *      SSL_RSA_WITH_RC4_128_SHA, SSL_RSA_WITH_RC4_128_MD5?
         *
         * IFF the answer to ALL of the above is 'yes', THEN modify
         * the enabled cipher suites configured on the parent SslFilter,
         * so that the new ciphers (i.e. the "blessed" ciphers for verifying
         * applets) are chosen.
         */

        ResourceAddress address = SslAcceptor.SSL_RESOURCE_ADDRESS.get(session);
        if (address == null) {
            // Clear out session attributes
            SSL_PROTOCOL_VERSION.remove(session);
            SSL_APPLET_CIPHERS.remove(session);

            return;
        }

        String[] ciphers = address.getOption(CIPHERS);
        List<String> enabledCiphers = SslCipherSuites.resolve(toCipherList(ciphers));
        boolean enabledRC4SHA = enabledCiphers.contains(RC4_SHA);
        boolean enabledRC4MD5 = enabledCiphers.contains(RC4_MD5);

        if (!enabledRC4SHA && !enabledRC4MD5) {
            // Clear out session attributes
            SSL_PROTOCOL_VERSION.remove(session);
            SSL_APPLET_CIPHERS.remove(session);

            // We're not configured for the blessed ciphersuites anyway,
            // nothing to do.
            return;
        }

        SslVersion protocolVersion = SSL_PROTOCOL_VERSION.remove(session);
        if (logger.isTraceEnabled()) {
            logger.trace(String.format("ClientHello is requesting protocol version %s", protocolVersion));
        }

        if (protocolVersion == null) {
            // Clear out session attributes
            SSL_APPLET_CIPHERS.remove(session);

            // If the version attribute is not present, it means there was
            // a problem with the decoding of the ClientHello.  Avoid an
            // NPE here, and let the javax.net.ssl.SSLEngine deal with the
            // (malformed?) ClientHello.
            return;
        }

        if (!protocolVersion.equals(SslVersion.SSL_3_0) &&
            !protocolVersion.equals(SslVersion.TLS_1_0)) {

            // Clear out session attributes
            SSL_APPLET_CIPHERS.remove(session);

            // We only need to worry about SSLv3.0/TLSv1.0 requests;
            // TLSv1.1 and TLSv1.2 requests should Just Work.  Right??
            return;
        }

        List<String> appletCiphers = SSL_APPLET_CIPHERS.remove(session);
        if (appletCiphers == null) {
            // The blessed ciphers were not requested by the client;
            // nothing more to do.
            return;
        }

        if (logger.isTraceEnabled()) {
            logger.trace(String.format("ClientHello supported blessed applet ciphers: %s", appletCiphers));
        }

        String[] newCiphers = appletCiphers.toArray(new String[appletCiphers.size()]);
        parentFilter.setEnabledCipherSuites(newCiphers);
    }

    @Override
    public void messageReceived(NextFilter nextFilter,
                                IoSession session,
                                Object message)
        throws Exception {

        try {
            session.suspendRead();

            selectCiphers(session);

            IoFilterChain filterChain = session.getFilterChain();

            // Now that we have (possibly) customized the ciphers, we can
            // add the real SslFilter in the filter chain.
            filterChain.addAfter(filterName, parentFilterName, parentFilter);

            // Make sure to remove the ClientHello codec filter, too.
            filterChain.remove(SslAcceptor.CLIENT_HELLO_CODEC_FILTER);
            filterChain.remove(this);

            super.messageReceived(nextFilter, session, message);

        } finally {
            session.resumeRead(); 
        }
    }

    private List<String> toCipherList(String[] names) {
        if (names == null ||
            names.length == 0) {
            return null;
        }

        List<String> list = new ArrayList<>(names.length);
        Collections.addAll(list, names);

        return list;
    }
}

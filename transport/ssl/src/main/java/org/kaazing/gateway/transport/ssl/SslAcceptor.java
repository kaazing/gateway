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

import static java.lang.String.format;
import static org.kaazing.gateway.resource.address.ResourceAddress.NEXT_PROTOCOL;
import static org.kaazing.gateway.resource.address.ResourceAddress.TRANSPORT;
import static org.kaazing.gateway.resource.address.ssl.SslResourceAddress.CIPHERS;
import static org.kaazing.gateway.resource.address.ssl.SslResourceAddress.ENCRYPTION_ENABLED;
import static org.kaazing.gateway.resource.address.ssl.SslResourceAddress.KEY_SELECTOR;
import static org.kaazing.gateway.resource.address.ssl.SslResourceAddress.NEED_CLIENT_AUTH;
import static org.kaazing.gateway.resource.address.ssl.SslResourceAddress.PROTOCOLS;
import static org.kaazing.gateway.resource.address.ssl.SslResourceAddress.WANT_CLIENT_AUTH;
import static org.kaazing.gateway.transport.BridgeSession.LOCAL_ADDRESS;
import static org.kaazing.gateway.transport.BridgeSession.NEXT_PROTOCOL_KEY;
import static org.kaazing.gateway.transport.BridgeSession.REMOTE_ADDRESS;

import java.io.IOException;
import java.net.URI;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.SortedSet;
import java.util.concurrent.Callable;

import javax.annotation.Resource;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;

import org.apache.mina.core.filterchain.IoFilterAdapter;
import org.apache.mina.core.filterchain.IoFilterChain;
import org.apache.mina.core.future.IoFuture;
import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.service.TransportMetadata;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.ssl.SslContextFactory;
import org.kaazing.gateway.resource.address.Protocol;
import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.resource.address.ResourceAddressFactory;
import org.kaazing.gateway.resource.address.ResourceOptions;
import org.kaazing.gateway.resource.address.ssl.SslResourceAddress;
import org.kaazing.gateway.security.KeySelector;
import org.kaazing.gateway.security.SecurityContext;
import org.kaazing.gateway.transport.AbstractBridgeAcceptor;
import org.kaazing.gateway.transport.Bindings;
import org.kaazing.gateway.transport.Bindings.Binding;
import org.kaazing.gateway.transport.BridgeAcceptor;
import org.kaazing.gateway.transport.BridgeServiceFactory;
import org.kaazing.gateway.transport.BridgeSessionInitializer;
import org.kaazing.gateway.transport.BridgeSessionInitializerAdapter;
import org.kaazing.gateway.transport.DefaultIoSessionConfigEx;
import org.kaazing.gateway.transport.DefaultTransportMetadata;
import org.kaazing.gateway.transport.IoHandlerAdapter;
import org.kaazing.gateway.transport.NextProtocolBindings;
import org.kaazing.gateway.transport.NextProtocolBindings.NextProtocolBinding;
import org.kaazing.gateway.transport.NextProtocolFilter;
import org.kaazing.gateway.transport.NioBindException;
import org.kaazing.gateway.transport.TransportKeySelector;
import org.kaazing.gateway.transport.TypedAttributeKey;
import org.kaazing.gateway.transport.dispatch.ProtocolDispatcher;
import org.kaazing.gateway.transport.ssl.bridge.filter.SslCertificateSelectionFilter;
import org.kaazing.gateway.transport.ssl.bridge.filter.SslFilter;
import org.kaazing.gateway.transport.ssl.cert.VirtualHostKeySelector;
import org.kaazing.gateway.util.ssl.SslCipherSuites;
import org.kaazing.mina.core.buffer.IoBufferAllocatorEx;
import org.kaazing.mina.core.future.UnbindFuture;
import org.kaazing.mina.core.service.IoProcessorEx;
import org.kaazing.mina.core.session.IoSessionEx;

public class SslAcceptor extends AbstractBridgeAcceptor<SslSession, NextProtocolBinding> {

    private static final TypedAttributeKey<SslSession> SESSION_KEY = new TypedAttributeKey<>(SslAcceptor.class, "session");

    private static final String CODEC_FILTER = SslProtocol.NAME + "#codec";
    private static final String CERTIFICATE_SELECTION_FILTER = SslProtocol.NAME + "#certificate_selection";
    private static final String CIPHER_SELECTION_FILTER = SslProtocol.NAME + "#cipher_selection";

    public static final String CLIENT_HELLO_CODEC_FILTER = SslProtocol.NAME + "#client_hello_codec";

    private static final String NEXT_PROTOCOL_FILTER = SslProtocol.NAME + "#nextProtocol";

    private static final String ENCRYPTION_DISABLED_FILTER = SslProtocol.NAME + "#encryption_disabled";

    private SSLContext sslContext;
    private SslContextFactory sslContextFactory;
    private SslCertificateSelectionFilter certificateSelection;
    private ResourceAddressFactory resourceAddressFactory;
    private BridgeServiceFactory bridgeServiceFactory;
    private VirtualHostKeySelector vhostKeySelector;

    // TODO: SslBindings like HttpBindings
    
    public SslAcceptor() {
        super(new DefaultIoSessionConfigEx());
    }

    @Resource(name = "bridgeServiceFactory")
    public void setBridgeServiceFactory(BridgeServiceFactory bridgeServiceFactory) {
        this.bridgeServiceFactory = bridgeServiceFactory;
    }

    @Resource(name = "resourceAddressFactory")
    public void setResourceAddressFactory(ResourceAddressFactory factory) {
        this.resourceAddressFactory = factory;
    }

    @Resource(name = "securityContext")
    public void setSecurityContext(SecurityContext securityContext) {
        vhostKeySelector = new VirtualHostKeySelector();
        try {
            vhostKeySelector.init(securityContext.getKeyStore(), securityContext.getKeyStorePassword());
        } catch (KeyStoreException e) {
            throw new RuntimeException(e);
        }

        try {
            sslContextFactory = new SslContextFactory();
            sslContextFactory.setTrustManagerFactoryKeyStore(securityContext.getTrustStore());
            //sslContextFactory.setTrustManagerFactoryKeyStorePassword(trustStorePassword);
            char[] keyStorePassword = securityContext.getKeyStorePassword();
            sslContextFactory.setKeyManagerFactoryKeyStorePassword(keyStorePassword == null ? null : new String(keyStorePassword));
            sslContextFactory.setKeyManagerFactoryKeyStore(securityContext.getKeyStore());

            // Create a new SslProvider to select certificates based on
            // various key selection criteria
            KeyManagerFactory kmf = KeyManagerFactory.getInstance("SslTransport", new SslProvider());
            sslContextFactory.setKeyManagerFactory(kmf);

            // avoid caching SSLSession in shared SSLContextFactory instance
            // Note: SSLSessionContext.setSessionCacheSize(0) means unlimited,
            // so we use 1 instead
            sslContextFactory.setServerSessionCacheSize(1);
        } catch (NoSuchAlgorithmException ne) {
            throw new RuntimeException(ne);
        }
    }

    @Override
    public TransportMetadata getTransportMetadata() {
        return new DefaultTransportMetadata(SslProtocol.NAME);
    }

    @Override
    protected void init() {
        super.init();

        try {
            sslContext = sslContextFactory.newInstance();

        } catch (UnrecoverableKeyException uke) {
            // Catch these exceptions separately, so that we can throw
            // a RuntimeException and cause the Gateway to not start up.
            // The likely cause is that the keystore contains keys that
            // are encrypted using a passphrase which differs from the
            // passphrase protecting the keystore itself (see KG-6925).
           throw new RuntimeException("Unable to load necessary certificate keys from keystore; perhaps your keys are protected by passwords that are different from the keystore password?", uke);

        } catch (Exception e) {
            logger.error("Exception while creating SSL context: ", e);
        }

        certificateSelection = new SslCertificateSelectionFilter(false);
    }

    @Override
    protected Bindings<NextProtocolBinding> initBindings() {
        return new NextProtocolBindings();
    }

    @Override
    protected IoProcessorEx<SslSession> initProcessor() {
        return new SslAcceptProcessor();
    }

    @Override
    public void addBridgeFilters(IoFilterChain filterChain) {

        // These must be in front of the dispatch filter
        //
        // First, we want to select the appropriate server certificate to
        // use for this connection; that's the purpose of the certificate
        // selection filter.  Note that the selection criteria for the
        // server certificate do NOT depend in any way on any data from the
        // connecting client.
        //
        // Next, we may need to customize the list of ciphers from which to
        // choose for this TLS session.  That's the purpose of the cipher
        // selection filter.  In this case, the criteria DO depend on data
        // from the client (specifically the client's ClientHello message).
        // Thus we first have a cumulative protocol codec (to streamily handle
        // ClientHello bytes), and then the actual cipher selection filter.
        //
        // Last is the actual SSL filter, which handles the IO.  Note that
        // the actual SSL filter is added to the filter chain by the
        // cipher selection filter dynamically; we do not need to add it here.

        // Create our SslFilter instance, and configure it based on the
        // resource address.
        SslFilter sslFilter = new SslFilter(sslContext, false, logger);

        IoSession session = filterChain.getSession();

        // Note: Do NOT remove the SSL_RESOURCE_ADDRESS here; it will be
        // removed by the cipher selection filter as needed.
        ResourceAddress sslAddress = SSL_RESOURCE_ADDRESS.get(session);
        boolean encryption = sslAddress.getOption(ENCRYPTION_ENABLED);

        if (encryption) {
            boolean wantClientAuth = sslAddress.getOption(WANT_CLIENT_AUTH);
            boolean needClientAuth = sslAddress.getOption(NEED_CLIENT_AUTH);

            List<String> unresolvedCipherNames = toCipherList(sslAddress.getOption(CIPHERS));
            List<String> resolvedCipherNames = SslCipherSuites.resolve(unresolvedCipherNames);
            String[] enabledCipherSuites = resolvedCipherNames.toArray(new String[resolvedCipherNames.size()]);

            if (logger.isTraceEnabled()) {
                logger.trace(String.format("Configured SSL/TLS ciphersuites:\n  %s", toCipherString(toCipherList(enabledCipherSuites))));
            }

            sslFilter.setWantClientAuth(wantClientAuth);
            sslFilter.setNeedClientAuth(needClientAuth);
            sslFilter.setEnabledCipherSuites(enabledCipherSuites);
            // Enable the configured SSL protocols like TLSv1 etc
            sslFilter.setEnabledProtocols(sslAddress.getOption(PROTOCOLS));

            IoSessionEx sessionEx = (IoSessionEx) session;
            IoBufferAllocatorEx<?> allocator = sessionEx.getBufferAllocator();

            // Filter are armed and ready, now add them to the filter chain.
            filterChain.addFirst(CERTIFICATE_SELECTION_FILTER, certificateSelection);
            filterChain.addAfter(CERTIFICATE_SELECTION_FILTER, CODEC_FILTER, sslFilter);
        }

        // detect next-protocol
        NextProtocolBinding sslBinding = bindings.getBinding0(sslAddress);
        if (sslBinding == null) {
            // Not currently bound (A concurrent unbind may have removed the binding)
            session.close(true);
            return;
        }
        SortedSet<String> nextProtocolNames = sslBinding.getNextProtocolNames();
        if (nextProtocolNames.isEmpty()) {
            NEXT_PROTOCOL_KEY.set(session, null);
        }
        else if (nextProtocolNames.size() == 1) {
            NEXT_PROTOCOL_KEY.set(session, nextProtocolNames.first());
        }
        else {
            Collection<ProtocolDispatcher> dispatchers = bridgeServiceFactory.getTransportFactory().getProtocolDispatchers().values();
            filterChain.addLast(NEXT_PROTOCOL_FILTER, new NextProtocolFilter(dispatchers));
        }

        if (!encryption) {
            filterChain.addLast(ENCRYPTION_DISABLED_FILTER, new EncryptionDisabledFilter());
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

    private String toCipherString(List<String> names) {
        if (names == null ||
            names.size() == 0) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        for (String name : names) {
            sb.append("  ").append(name).append("\n");
        }

        String cipherString = sb.toString().trim();
        return cipherString;
    }

    @Override
    public void removeBridgeFilters(IoFilterChain filterChain) {
        removeFilter(filterChain, CODEC_FILTER);
        removeFilter(filterChain, CIPHER_SELECTION_FILTER);
        removeFilter(filterChain, CLIENT_HELLO_CODEC_FILTER);
        removeFilter(filterChain, CERTIFICATE_SELECTION_FILTER);
        removeFilter(filterChain, NEXT_PROTOCOL_FILTER);
    }

    @Override
    protected boolean canBind(String transportName) {
        return transportName.equals("ssl");
    }

    @Override
    protected <T extends IoFuture> void bindInternal(final ResourceAddress address, IoHandler handler,
                                                     final BridgeSessionInitializer<T> initializer) {

        boolean sslEncryptionEnabled = address.getOption(ENCRYPTION_ENABLED);
        if (sslEncryptionEnabled) {
            try {
                KeySelector keySelector = address.getOption(KEY_SELECTOR);
                if (keySelector == null) {
                    // For servers, if no specific KeySelector has been
                    // configured (e.g. by a higher layer), then use the
                    // default KeySelector for servers (i.e. the VirtualHost
                    // key selector).
                    keySelector = vhostKeySelector;
                }
                TransportKeySelector transportKeySelector = TransportKeySelector.class.cast(keySelector);
                certificateSelection.setKeySelector(transportKeySelector);
                transportKeySelector.bind(address);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        // JRF: this looks like we're breaking the transport abstraction by assuming that SSL is always over TCP
        // with SNI extension coming from client, we can bind the same handler at transport layer for all certificates
        // without SNI extension, we need to guess at SNI based on transport
        // SslNextAddressFilter determines ssl://[virtual host]:[virtual port] address over underlying transport address
        //   use ThreadLocal to set server name (drives certificate selection)
        
        ResourceAddress transport = address.getTransport();
        assert (transport != null);

        IoHandler bridgeHandler = sslEncryptionEnabled ? secureBridgeHandler : unsecureBridgeHandler;

        final ResourceAddress transportAddress = transport;
        BridgeAcceptor acceptor = bridgeServiceFactory.newBridgeAcceptor(transportAddress);
        try {
            final URI transportURI = transportAddress.getResource();
            final Protocol transportProtocol = bridgeServiceFactory.getTransportFactory().getProtocol(transportURI.getScheme());
            final BridgeSessionInitializer<T> parentInitializer = (initializer != null) ? initializer
                    .getParentInitializer(transportProtocol) : null;

            // We wrap the session initializer here with our own, so that
            // we can set the ResourceAddress on the session (for use later
            // in SslHandler, for setting per-address options).  See
            // KG-7059 for the gory, tragic details.

            BridgeSessionInitializer<T> wrapperInitializer = new BridgeSessionInitializerAdapter<T>() {
                @Override
                public void initializeSession(IoSession session, T future) {
                    SslAcceptor.SSL_RESOURCE_ADDRESS.set(session, address);
                    if (parentInitializer != null) {
                        parentInitializer.initializeSession(session, future);
                    }
                }
            };

            acceptor.bind(transportAddress, bridgeHandler, wrapperInitializer);
        }
        catch (NioBindException e) {
            Iterable<ResourceAddress> failedAddresses = e.getFailedAddresses();
            for (ResourceAddress failedAddress : failedAddresses) {
                IoHandler existingHandler = acceptor.getHandler(failedAddress);
                if (existingHandler == secureBridgeHandler) {
                    throw new RuntimeException(
                            failedAddress
                                    + " is configured as a secure port in a service already and cannot be bound as an unsecure port in service: "
                                    + address, e);
                }
            }
            throw new RuntimeException("Unable to bind address " + failedAddresses.iterator().next() + ": " + e.getMessage(), e);
        }
    }

    @Override
    protected UnbindFuture unbindInternal(ResourceAddress address, IoHandler handler, BridgeSessionInitializer<? extends IoFuture> initializer) {
        ResourceAddress transport = address.getTransport();
        assert (transport != null);
        ResourceAddress transportAddress = address.getTransport();

        boolean sslEncryptionEnabled = address.getOption(ENCRYPTION_ENABLED);
        if (sslEncryptionEnabled) {
            try {
                KeySelector keySelector = address.getOption(SslResourceAddress.KEY_SELECTOR);
                if (keySelector == null) {
                    // For servers, if no specific KeySelector has been
                    // configured (e.g. by a higher layer), then use the
                    // default KeySelector for servers (i.e. the VirtualHost
                    // key selector).
                    keySelector = vhostKeySelector;
                }
                TransportKeySelector transportKeySelector = TransportKeySelector.class.cast(keySelector);
                transportKeySelector.unbind(address);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        BridgeAcceptor acceptor  = bridgeServiceFactory.newBridgeAcceptor(transportAddress);
        return acceptor.unbind(transportAddress);
    }

    private IoHandler secureBridgeHandler = new BridgeHandler(true);

    private BridgeHandler unsecureBridgeHandler = new BridgeHandler(false);

    @Deprecated // HOWTO get the requested server name / port from the SSL handshake (even without SNI)?
    public static final TypedAttributeKey<ResourceAddress> SSL_RESOURCE_ADDRESS = new TypedAttributeKey<>(SslFilter.class, "sslResourceAddress");


    // When SSL encryption is disabled, this filter is used to create SslSession.
    // One needs to know the next protocol to create a SslSession, hence this
    // filter is run after NextProtocolFilter
    private final class EncryptionDisabledFilter extends IoFilterAdapter {
        @Override
        public void messageReceived(NextFilter nextFilter, IoSession session, Object message) throws Exception {
            // create SslSession
            SslSession sslSession = unsecureBridgeHandler.createSslSession((IoSessionEx) session);
            SESSION_KEY.set(session, sslSession);
            session.getFilterChain().remove(this);
            super.messageReceived(nextFilter, session, message);
        }
    }


    private final class BridgeHandler extends IoHandlerAdapter<IoSessionEx> {

        private final boolean sslEncryptionEnabled;

        public BridgeHandler(boolean sslEncryptionEnabled) {
            this.sslEncryptionEnabled = sslEncryptionEnabled;
        }

        @Override
        protected void doExceptionCaught(IoSessionEx session, Throwable cause) throws Exception {
            if (logger.isDebugEnabled()) {
                String message = format("Error on SSL connection, closing connection: %s", cause);
                if (logger.isTraceEnabled()) {
                    // note: still debug level, but with extra detail about the exception
                    logger.debug(message, cause);
                }
                else {
                    logger.debug(message);
                }
            }
            session.close(true);
        }

        @Override
        protected void doMessageReceived(final IoSessionEx session, Object message) throws Exception {
            if (message == SslFilter.SESSION_SECURED) {
                IoFilterChain filterChain = session.getFilterChain();
                removeFilter(filterChain, certificateSelection);

                // create session
                IoSession sslSession = SESSION_KEY.get(session);
                assert (sslSession == null);

                SslSession newSslSession = createSslSession(session);
                SESSION_KEY.set(session, newSslSession);
            }
            else if (message == SslFilter.SESSION_UNSECURED) {
                SslSession sslSession = SESSION_KEY.remove(session);
                
                if (sslSession != null && !sslSession.isClosing()) {
                    sslSession.getProcessor().remove(sslSession);
                }
            }
            else {
                IoSession sslSession = SESSION_KEY.get(session);
                assert (sslSession != null);
                IoFilterChain filterChain = sslSession.getFilterChain();
                filterChain.fireMessageReceived(message);
            }
        }

        private SslSession createSslSession(final IoSessionEx session) throws Exception {
            SslSession sslSession = newSession(null, new Callable<SslSession>() {
                @Override
                public SslSession call() {
                    //
                    // Construct the local and remote addresses as ResourceAddress
                    // objects now using the parent session's local address.
                    //
                    // TODO: avoid NPE when Binding not found
                    ResourceAddress localSslAddress = getSslSessionLocalAddress(session);
                    ResourceAddress remoteSslAddress = getSslSessionRemoteAddress(session, localSslAddress);

                    SslSession newSslSession = new SslSession(SslAcceptor.this, getProcessor(), localSslAddress, remoteSslAddress, session);
                    IoHandler handler = getHandler(newSslSession.getLocalAddress());
                    newSslSession.setHandler(handler);
                    return newSslSession;
                }

                private ResourceAddress getSslSessionLocalAddress(IoSession session) {
                    // note: bound address is unified in SSL options during bind to avoid conflicts like different cipher suites
                    ResourceAddress boundAddress = SslAcceptor.SSL_RESOURCE_ADDRESS.remove(session);
                    assert (boundAddress != null);

                    // construct the candidate address with observed transport and next protocol
                    String candidateURI = boundAddress.getExternalURI();
                    ResourceOptions candidateOptions = ResourceOptions.FACTORY.newResourceOptions(boundAddress);
                    candidateOptions.setOption(NEXT_PROTOCOL, NEXT_PROTOCOL_KEY.get(session));
                    candidateOptions.setOption(TRANSPORT, LOCAL_ADDRESS.get(session));
                    ResourceAddress candidateAddress = resourceAddressFactory.newResourceAddress(candidateURI, candidateOptions);

                    // lookup the binding for this candidate address
                    Binding binding = bindings.getBinding(candidateAddress);
                    return (binding != null) ? binding.bindAddress() : null;
                }

                private ResourceAddress getSslSessionRemoteAddress(IoSession session, ResourceAddress localSslAddress) {
                    return resourceAddressFactory.newResourceAddress(localSslAddress, REMOTE_ADDRESS.get(session));
                }

            });
            return sslSession;
        }

        @Override
        protected void doSessionClosed(IoSessionEx session) throws Exception {
            SslSession sslSession = SESSION_KEY.remove(session);
            if (sslSession != null) {
                if (sslSession.isClosing()) {
                    sslSession.getProcessor().remove(sslSession);
                } else {
                    // behave similarly to connection reset by peer at NIO layer
                    sslSession.reset(new IOException("Early termination of IO session").fillInStackTrace());
                }
            }
        }

        @Override
        protected void doSessionIdle(IoSessionEx session, IdleStatus status) throws Exception {
            SslSession sslSession = SESSION_KEY.get(session);
            if (sslSession != null) {
                IoFilterChain filterChain = sslSession.getFilterChain();
                filterChain.fireSessionIdle(status);
            }
        }

        @Override
        protected void doSessionCreated(final IoSessionEx session) throws Exception {
            // note: *always* add bridge filters, even if SSL encryption not enabled
            //       so that we can determine the next-protocol in all scenarios
            session.setAttribute(SslFilter.USE_NOTIFICATION);
            addBridgeFilters(session.getFilterChain());
        }
    }
}

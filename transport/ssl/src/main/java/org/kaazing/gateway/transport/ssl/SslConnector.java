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

import static org.kaazing.gateway.resource.address.ssl.SslResourceAddress.CIPHERS;
import static org.kaazing.gateway.resource.address.ssl.SslResourceAddress.PROTOCOLS;
import static org.kaazing.gateway.resource.address.ssl.SslResourceAddress.ENCRYPTION_ENABLED;
import static org.kaazing.gateway.resource.address.ssl.SslResourceAddress.KEY_SELECTOR;
import static org.kaazing.gateway.resource.address.ssl.SslResourceAddress.NEED_CLIENT_AUTH;
import static org.kaazing.gateway.resource.address.ssl.SslResourceAddress.WANT_CLIENT_AUTH;
import static org.kaazing.gateway.transport.BridgeSession.LOCAL_ADDRESS;
import static java.lang.String.format;

import java.io.IOException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;

import javax.annotation.Resource;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;

import org.apache.mina.core.filterchain.IoFilterChain;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.future.DefaultConnectFuture;
import org.apache.mina.core.future.IoFutureListener;
import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.service.TransportMetadata;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.session.IoSessionInitializer;
import org.apache.mina.filter.ssl.SslContextFactory;

import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.resource.address.ResourceAddressFactory;
import org.kaazing.gateway.security.KeySelector;
import org.kaazing.gateway.security.SecurityContext;
import org.kaazing.gateway.transport.AbstractBridgeConnector;
import org.kaazing.gateway.transport.AbstractBridgeSession;
import org.kaazing.gateway.transport.BridgeConnector;
import org.kaazing.gateway.transport.BridgeServiceFactory;
import org.kaazing.gateway.transport.BridgeSessionInitializer;
import org.kaazing.gateway.transport.BridgeSessionInitializerAdapter;
import org.kaazing.gateway.transport.DefaultIoSessionConfigEx;
import org.kaazing.gateway.transport.DefaultTransportMetadata;
import org.kaazing.gateway.transport.IoHandlerAdapter;
import org.kaazing.gateway.transport.TransportKeySelector;
import org.kaazing.gateway.transport.TypedAttributeKey;
import org.kaazing.gateway.transport.ssl.bridge.filter.SslCertificateSelectionFilter;
import org.kaazing.gateway.transport.ssl.bridge.filter.SslFilter;
import org.kaazing.gateway.transport.ssl.cert.VirtualHostKeySelector;
import org.kaazing.gateway.util.ssl.SslCipherSuites;
import org.kaazing.mina.core.service.IoProcessorEx;
import org.kaazing.mina.core.session.IoSessionEx;

public class SslConnector extends AbstractBridgeConnector<SslSession> {

    private static final TypedAttributeKey<Callable<SslSession>> SSL_SESSION_FACTORY_KEY = new TypedAttributeKey<>(SslConnector.class, "sslSessionFactory");
    private static final TypedAttributeKey<ConnectFuture> SSL_CONNECT_FUTURE_KEY = new TypedAttributeKey<>(SslConnector.class, "sslConnectFuture");
    private static final TypedAttributeKey<SslSession> SSL_SESSION_KEY = new TypedAttributeKey<>(SslConnector.class, "sslSession");

    private static final String CODEC_FILTER = SslProtocol.NAME + "#codec";
    private static final String CERTIFICATE_SELECTION_FILTER = SslProtocol.NAME + "#certificate_selection";

    private BridgeServiceFactory bridgeServiceFactory;
    private SslContextFactory sslContextFactory;
    private SSLContext sslContext;
    private SslCertificateSelectionFilter certificateSelection;
    private ResourceAddressFactory resourceAddressFactory;
    private VirtualHostKeySelector vhostKeySelector;

    public SslConnector() {
        super(new DefaultIoSessionConfigEx());
    }

    @Resource(name = "resourceAddressFactory")
    public void setResourceAddressFactory(ResourceAddressFactory resourceAddressFactory) {
        this.resourceAddressFactory = resourceAddressFactory;
    }

    @Resource(name = "bridgeServiceFactory")
    public void setBridgeServiceFactory(BridgeServiceFactory bridgeServiceFactory) {
        this.bridgeServiceFactory = bridgeServiceFactory;
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
            sslContextFactory.setClientSessionCacheSize(1);
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
        }
        catch (Exception e) {
            logger.error("Exception while creating SSL context: ", e);
        }

        certificateSelection = new SslCertificateSelectionFilter(true);
    }

    @Override
    protected IoProcessorEx<SslSession> initProcessor() {
        return new SslConnectProcessor();
    }

    @Override
    public void addBridgeFilters(IoFilterChain filterChain) {
        IoSession session = filterChain.getSession();
        ResourceAddress address = SslAcceptor.SSL_RESOURCE_ADDRESS.remove(session);
        if (address != null) {
            boolean encryption = address.getOption(ENCRYPTION_ENABLED);
            if (encryption) {
                // Create our SslFilter instance, and configure it based on the
                // resource address.
                SslFilter sslFilter = new SslFilter(sslContext, true, logger);
                sslFilter.setUseClientMode(true);

                boolean wantClientAuth = address.getOption(WANT_CLIENT_AUTH);
                boolean needClientAuth = address.getOption(NEED_CLIENT_AUTH);

                List<String> unresolvedCipherNames = toCipherList(address.getOption(CIPHERS));
                List<String> resolvedCipherNames = SslCipherSuites.resolve(unresolvedCipherNames);
                String[] enabledCipherSuites = resolvedCipherNames.toArray(new String[resolvedCipherNames.size()]);

                if (logger.isTraceEnabled()) {
                    logger.trace(String.format("Configured SSL/TLS ciphersuites:\n  %s", toCipherString(toCipherList(enabledCipherSuites))));
                }

                sslFilter.setWantClientAuth(wantClientAuth);
                sslFilter.setNeedClientAuth(needClientAuth);
                sslFilter.setEnabledCipherSuites(enabledCipherSuites);
                // Enable the configured SSL protocols like TLSv1 etc
                sslFilter.setEnabledProtocols(address.getOption(PROTOCOLS));

                filterChain.addFirst(CERTIFICATE_SELECTION_FILTER, certificateSelection);
                filterChain.addAfter(CERTIFICATE_SELECTION_FILTER, CODEC_FILTER, sslFilter);
            } else {
                try {
                    Callable<SslSession> sessionFactory = SSL_SESSION_FACTORY_KEY.get(session);
                    SslSession sslSession = sessionFactory.call();

                    session.setAttribute(SSL_SESSION_KEY, sslSession);
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }
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
        removeFilter(filterChain, CERTIFICATE_SELECTION_FILTER);
    }

    @Override
    protected boolean canConnect(String transportName) {
        return transportName.equals("ssl");
    }

    @Override
    protected <T extends ConnectFuture> ConnectFuture connectInternal(final ResourceAddress connectAddress, IoHandler handler,
                                                                      final IoSessionInitializer<T> initializer) {

        boolean isSSLEncryptionEnabled = connectAddress.getOption(ENCRYPTION_ENABLED);
        if (isSSLEncryptionEnabled) {
            try {
                KeySelector keySelector = connectAddress.getOption(KEY_SELECTOR);
                if (keySelector == null) {
                    // For servers, if no specific KeySelector has been configured
                    // (e.g. by a higher layer), then use the default KeySelector
                    // for servers (i.e. the VirtualHost key selector).
                    keySelector = vhostKeySelector;
                }

                TransportKeySelector transportKeySelector = TransportKeySelector.class.cast(keySelector);
                certificateSelection.setKeySelector(transportKeySelector);
                transportKeySelector.connect(connectAddress);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        final DefaultConnectFuture sslConnectFuture = new DefaultConnectFuture();

        // propagate connection failure, if necessary
        IoFutureListener<ConnectFuture> parentConnectListener = new IoFutureListener<ConnectFuture>() {
            @Override
            public void operationComplete(ConnectFuture future) {
                // fail bridge connect future if parent connect fails
                if (!future.isConnected()) {
                    sslConnectFuture.setException(future.getException());
                }
            }
        };

        final IoSessionInitializer<ConnectFuture> parentInitializer = createParentInitializer(connectAddress, handler, initializer, sslConnectFuture);

        // We wrap the session initializer here with our own, so that
        // we can set the ResourceAddress on the session (for use later
        // in SslHandler, for setting per-address options).  See
        // KG-7059 for the gory, tragic details.

        BridgeSessionInitializer<T> wrapperInitializer = new BridgeSessionInitializerAdapter<T>() {
            @Override
            public void initializeSession(IoSession session, T future) {
                SslAcceptor.SSL_RESOURCE_ADDRESS.set(session, connectAddress);
                if (parentInitializer != null) {
                    parentInitializer.initializeSession(session, future);
                }
            }
        };

        BridgeConnector connector = bridgeServiceFactory.newBridgeConnector(connectAddress.getTransport());
        connector.connect(connectAddress.getTransport(), bridgeHandler, wrapperInitializer).addListener(parentConnectListener);

        return sslConnectFuture;
    }

    private <T extends ConnectFuture> IoSessionInitializer<ConnectFuture> createParentInitializer(final ResourceAddress connectAddress,
            final IoHandler handler, final IoSessionInitializer<T> initializer, final DefaultConnectFuture sslConnectFuture) {
        // initialize parent session before connection attempt
        return new IoSessionInitializer<ConnectFuture>() {
            @Override
            public void initializeSession(final IoSession parent, ConnectFuture future) {
                // initializer for bridge session to specify bridge handler,
                // and call user-defined bridge session initializer if present
                final IoSessionInitializer<T> bridgeSessionInitializer = new IoSessionInitializer<T>() {
                    @Override
                    public void initializeSession(IoSession bridgeSession, T future) {
                        ((AbstractBridgeSession<?, ?>) bridgeSession).setHandler(handler);

                        if (initializer != null) {
                            initializer.initializeSession(bridgeSession, future);
                        }
                    }
                };

                // factory to create a new bridge session
                Callable<SslSession> sslSessionFactory = new Callable<SslSession>() {
                    @Override
                    public SslSession call() throws Exception {

                        Callable<SslSession> bridgeSessionFactory = new Callable<SslSession>() {
                            @Override
                            public SslSession call() throws Exception {
                                ResourceAddress localAddress =
                                        resourceAddressFactory.newResourceAddress(connectAddress,
                                                                                  LOCAL_ADDRESS.get(parent));
                                IoSessionEx parentEx = (IoSessionEx) parent;

                                return new SslSession(SslConnector.this, getProcessor(), localAddress, connectAddress, parentEx);
                            }
                        };

                        return newSession(bridgeSessionInitializer, sslConnectFuture, bridgeSessionFactory);
                    }
                };

                SSL_SESSION_FACTORY_KEY.set(parent, sslSessionFactory);
                SSL_CONNECT_FUTURE_KEY.set(parent, sslConnectFuture);
            }
        };
    }

    private IoHandler bridgeHandler = new IoHandlerAdapter<IoSessionEx>() {

        @Override
        protected void doSessionOpened(IoSessionEx session) throws Exception {
            // want to receive SslFilter.SESSION_SECURED when handshake
            // completed successfully
            session.setAttribute(SslFilter.USE_NOTIFICATION);

            IoFilterChain filterChain = session.getFilterChain();
            addBridgeFilters(filterChain);
        }

        @Override
        protected void doSessionClosed(IoSessionEx session) throws Exception {

            // needed by HTTPS upgrade to websocket
            if (!session.isClosing()) {
                IoFilterChain filterChain = session.getFilterChain();
                removeBridgeFilters(filterChain);
            }

            SslSession sslSession = SSL_SESSION_KEY.get(session);
        	if (sslSession != null) {
        	    if (!sslSession.isClosing()) {
        	        // behave similarly to connection reset by peer at NIO layer
        	        sslSession.reset(new IOException("Early termination of IO session").fillInStackTrace());
        	    }
        	}
        	else {
        	    // SSL handshake incomplete, fail the SslSession connect future
        	    ConnectFuture sslConnectFuture = SSL_CONNECT_FUTURE_KEY.remove(session);
        	    if (sslConnectFuture != null) {
        	        sslConnectFuture.setException(new Exception("SSL connection failed"));
        	    }
            }
        }

        @Override
        protected void doMessageReceived(final IoSessionEx session, Object message) throws Exception {
            if (message == SslFilter.SESSION_SECURED) {
                // no need for certificate selection filter after SSL handshake complete
                IoFilterChain filterChain = session.getFilterChain();
                removeFilter(filterChain, certificateSelection);

                Callable<SslSession> sessionFactory = SSL_SESSION_FACTORY_KEY.get(session);
                SslSession sslSession = sessionFactory.call();

                session.setAttribute(SSL_SESSION_KEY, sslSession);
            } else if (message == SslFilter.SESSION_UNSECURED) {
                SslSession sslSession = SSL_SESSION_KEY.get(session);
                sslSession.close(false);
            } else {
                SslSession sslSession = SSL_SESSION_KEY.get(session);
                IoFilterChain filterChain = sslSession.getFilterChain();
                filterChain.fireMessageReceived(message);
            }
        }

        @Override
        protected void doExceptionCaught(IoSessionEx session, Throwable cause) throws Exception {
            if (logger.isDebugEnabled()) {
                String message = format("Error on SSL connection attempt: %s", cause);
                if (logger.isTraceEnabled()) {
                    // note: still debug level, but with extra detail about the exception
                    logger.debug(message, cause);
                }
                else {
                    logger.debug(message);
                }
            }

            session.close(true);

            // exception may be triggered by SSL handshake
            ConnectFuture sslConnectFuture = SSL_CONNECT_FUTURE_KEY.remove(session);
            if (sslConnectFuture != null) {
                sslConnectFuture.setException(cause);
            }
        }

        @Override
        protected void doSessionIdle(IoSessionEx session, IdleStatus status) throws Exception {
            IoSession bridgeSession = (IoSession) session.getAttribute(SSL_SESSION_KEY);
            bridgeSession.getFilterChain().fireSessionIdle(status);
        }
    };
}

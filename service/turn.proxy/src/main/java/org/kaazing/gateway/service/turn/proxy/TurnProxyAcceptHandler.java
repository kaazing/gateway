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
package org.kaazing.gateway.service.turn.proxy;

import java.security.Key;
import java.util.HashMap;
import java.util.Map;

import org.apache.mina.core.filterchain.IoFilterChain;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.future.IoFutureListener;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.session.IoSessionInitializer;
import org.kaazing.gateway.security.SecurityContext;
import org.kaazing.gateway.service.ServiceContext;
import org.kaazing.gateway.service.ServiceProperties;
import org.kaazing.gateway.service.proxy.AbstractProxyAcceptHandler;
import org.kaazing.gateway.service.proxy.AbstractProxyHandler;
import org.kaazing.gateway.service.turn.proxy.filters.StunCodecFilter;
import org.kaazing.gateway.service.turn.proxy.filters.StunMaskAddressFilter;
import org.kaazing.gateway.service.turn.proxy.filters.StunUserameFilter;
import org.kaazing.gateway.service.turn.proxy.filters.TurnFrameDecoderException;
import org.kaazing.gateway.transport.TypedAttributeKey;
import org.kaazing.gateway.util.turn.TurnUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TurnProxyAcceptHandler extends AbstractProxyAcceptHandler {

    static final Logger LOGGER = LoggerFactory.getLogger(TurnProxyAcceptHandler.class);
    public static final TypedAttributeKey<Map<String, String>> TURN_CURRENT_TRANSACTION_ATTRIBUTE =
            new TypedAttributeKey<>(Map.class, "turn-session-transactions-map");

    // Configuration properties
    public static final String PROPERTY_MAPPED_ADDRESS = "mapped.address";
    public static final String PROPERTY_AUTO_MAPPED_ADDRESS = "AUTO";
    public static final String PROPERTY_MASK_ADDRESS = "masking.key";
    public static final String PROPERTY_KEY_ALIAS = "key.alias";
    public static final String PROPERTY_KEY_ALGORITHM = "key.algorithm";

    private String connectURI;
    private Key sharedSecret;
    private String keyAlgorithm;
    private Long mask;

    public TurnProxyAcceptHandler() {
        super();
    }

    /**
     * Applies the configuration settings to the handler and the composed objects.
     *
     * @param serviceContext Configuration holder
     * @param securityContext Security configuration holder
     */
    public void init(ServiceContext serviceContext, SecurityContext securityContext) {
        connectURI = serviceContext.getConnects().iterator().next();

        boolean keyAliasRequired = false;
        ServiceProperties properties = serviceContext.getProperties();
        String mappedAddress = properties.get(PROPERTY_MAPPED_ADDRESS);
        if (mappedAddress != null) {
            ((TurnProxyConnectHandler) getConnectHandler()).setFixedMappedAddress(mappedAddress);
            keyAliasRequired = true;
        }

        String maskProperty;
        if ((maskProperty = properties.get(PROPERTY_MASK_ADDRESS)) != null) {
            mask = Long.decode(maskProperty);
            keyAliasRequired = true;
        }

        if (properties.get(PROPERTY_KEY_ALIAS) != null) {
            sharedSecret = TurnUtils.getSharedSecret(securityContext.getKeyStore(), properties.get(PROPERTY_KEY_ALIAS),
                    securityContext.getKeyStorePassword());
            if ((keyAlgorithm = properties.get(PROPERTY_KEY_ALGORITHM)) == null) {
                keyAlgorithm = TurnUtils.HMAC_SHA_1;
            }
        } else if (keyAliasRequired) {
            throw new TurnProxyException(
                    "Missing configuration property 'key.alias' required by 'mapped.address' or 'masking.key'.");
        }
    }

    @Override
    protected AbstractProxyHandler createConnectHandler() {
        return new TurnProxyConnectHandler();
    }

    @Override
    public void sessionCreated(IoSession acceptSession) {
        final IoFilterChain filterChain = acceptSession.getFilterChain();

        filterChain.addLast("STUN_CODEC", new StunCodecFilter(sharedSecret, keyAlgorithm));

        Map<String, String> currentTransactions = new HashMap<>();
        filterChain.addLast("STUN_USERNAME", new StunUserameFilter(currentTransactions));
        acceptSession.setAttribute(TURN_CURRENT_TRANSACTION_ATTRIBUTE, currentTransactions);
        

        if (mask != null) {
            filterChain.addLast("STUN_MASK", new StunMaskAddressFilter(mask, StunMaskAddressFilter.Orientation.INCOMING));
        }
        super.sessionCreated(acceptSession);
    }

    @Override
    public void sessionOpened(IoSession acceptSession) {
        ConnectSessionInitializer sessionInitializer = new ConnectSessionInitializer(acceptSession);
        ConnectFuture connectFuture = getServiceContext().connect(connectURI, getConnectHandler(), sessionInitializer);
        connectFuture.addListener(new ConnectListener(acceptSession));
        super.sessionOpened(acceptSession);
    }

    @Override
    public void messageReceived(IoSession session, Object message) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(String.format("Received message [%s] from [%s]", message, session));
        }
        super.messageReceived(session, message);
    }

    @Override
    public void exceptionCaught(IoSession session, Throwable cause) throws Exception {
        if (cause instanceof TurnFrameDecoderException) {
            LOGGER.warn("Protocol Decoder Exception", cause);
            session.write(((TurnFrameDecoderException) cause).getStunMessage());
        } else {
            super.exceptionCaught(session, cause);
        }
    }
    /*
     * Initializer for connect session. It adds the processed accept session headers on the connect session
     */
    class ConnectSessionInitializer implements IoSessionInitializer<ConnectFuture> {

        private final IoSession acceptSession;

        public ConnectSessionInitializer(IoSession acceptSession) {
            this.acceptSession = acceptSession;
        }

        @Override
        public void initializeSession(IoSession session, ConnectFuture future) {
            final IoFilterChain filterChain = session.getFilterChain();

            filterChain.addLast("STUN_CODEC", new StunCodecFilter(sharedSecret, keyAlgorithm));

            Map<String, String> currentTransactions = TURN_CURRENT_TRANSACTION_ATTRIBUTE.get(acceptSession);
            filterChain.addLast("STUN_USERNAME", new StunUserameFilter(currentTransactions));

            if (mask != null) {
                filterChain.addLast("STUN_MASK", new StunMaskAddressFilter(mask, StunMaskAddressFilter.Orientation.OUTGOING));
            }
        }
    }

    private class ConnectListener implements IoFutureListener<ConnectFuture> {

        private final IoSession acceptSession;

        ConnectListener(IoSession acceptSession) {
            this.acceptSession = acceptSession;
        }

        @Override
        public void operationComplete(ConnectFuture future) {
            if (future.isConnected()) {
                IoSession connectSession = future.getSession();

                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace("Connected to " + connectURI + " [" + acceptSession + "->" + connectSession + "]");
                }
                if (acceptSession == null || acceptSession.isClosing()) {
                    connectSession.close(true);
                } else {
                    AttachedSessionManager attachedSessionManager = attachSessions(acceptSession, connectSession);
                    flushQueuedMessages(acceptSession, attachedSessionManager);
                }
            } else {
                LOGGER.warn("Connection to " + connectURI + " failed [" + acceptSession + "->]");
                acceptSession.close(true);
            }
        }
    }
}

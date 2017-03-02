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
package org.kaazing.gateway.transport.wsn;

import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.security.auth.context.ResultAwareLoginContext;
import org.kaazing.gateway.transport.Direction;
import org.kaazing.gateway.transport.TypedAttributeKey;
import org.kaazing.gateway.transport.bridge.CachingMessageEncoder;
import org.kaazing.gateway.transport.bridge.Message;
import org.kaazing.gateway.transport.bridge.MessageEncoder;
import org.kaazing.gateway.transport.http.HttpAcceptor;
import org.kaazing.gateway.transport.ws.AbstractWsBridgeSession;
import org.kaazing.gateway.transport.ws.bridge.filter.WsBuffer;
import org.kaazing.gateway.transport.ws.extension.WebSocketExtension;
import org.kaazing.gateway.util.ws.WebSocketWireProtocol;
import org.kaazing.mina.core.buffer.IoBufferAllocatorEx;
import org.kaazing.mina.core.buffer.IoBufferEx;
import org.kaazing.mina.core.service.IoProcessorEx;
import org.kaazing.mina.core.service.IoServiceEx;
import org.kaazing.mina.core.session.IoSessionEx;

public class WsnSession extends AbstractWsBridgeSession<WsnSession, WsBuffer> {
    public static final TypedAttributeKey<WsnSession> SESSION_KEY = new TypedAttributeKey<>(WsnSession.class, "session");

    private static final CachingMessageEncoder WS_RFC6455_MESSAGE_ENCODER = new CachingMessageEncoder() {

        @Override
        public <T extends Message> IoBufferEx encode(MessageEncoder<T> encoder, T message, IoBufferAllocatorEx<?> allocator, int flags) {
            return encode("ws/rcf6455", encoder, message, allocator, flags);
        }

    };

    private static final CachingMessageEncoder WS_DRAFT7X_MESSAGE_ENCODER = new CachingMessageEncoder() {

        @Override
        public <T extends Message> IoBufferEx encode(MessageEncoder<T> encoder, T message, IoBufferAllocatorEx<?> allocator, int flags) {
            return encode("ws/draft-7x", encoder, message, allocator, flags);
        }

    };

    private URI httpRequestURI; // the URI of the pre-upgrade HTTP request
    private Collection<String> balanceeURIs;
    private WebSocketWireProtocol version;
    AtomicBoolean sendCloseFrame;

    public WsnSession(IoServiceEx service, IoProcessorEx<WsnSession> processor, ResourceAddress localAddress,
                      ResourceAddress remoteAddress, IoSessionEx parent, IoBufferAllocatorEx<WsBuffer> allocator,
                      URI httpRequestURI, ResultAwareLoginContext loginContext,
                      WebSocketWireProtocol version, List<WebSocketExtension> extensions) {
        super(service, processor, localAddress, remoteAddress, parent, allocator, Direction.BOTH, loginContext, extensions);
        this.httpRequestURI = httpRequestURI;
        this.version = version;
        this.sendCloseFrame = new AtomicBoolean();
        this.sendCloseFrame.set(WebSocketWireProtocol.RFC_6455 == version || WebSocketWireProtocol.HYBI_13 == version); //send close frame if protocol is RFC-6455
    }

    @Override
    public CachingMessageEncoder getMessageEncoder() {
        switch (version) {
        case HIXIE_75:
        case HIXIE_76:
            return WS_DRAFT7X_MESSAGE_ENCODER;
        default:
            return WS_RFC6455_MESSAGE_ENCODER;
        }
    }

    Collection<String> getBalanceeURIs() {
        return balanceeURIs;
    }

    public WebSocketWireProtocol getVersion() {
        return version;
    }

    public boolean isBalanceSupported() {
        String query = httpRequestURI.getQuery();
        return ((query != null) && query.contains(".kl=Y")) || this.getParent().getAttribute(HttpAcceptor.BALANCEES_KEY) != null;
    }

    public void setBalanceeURIs(Collection<String> balanceeURIs) {
        this.balanceeURIs = balanceeURIs;
    }

    public boolean isSecure() {
        return httpRequestURI.toString().startsWith("https");
    }

    public URI getParentHttpRequestURI() {
        return httpRequestURI;
        //comes from getParent().getAttribute(HTTP_REQUEST_URI_KEY)
    }

    @Override
    public ResourceAddress getLocalAddress() {
        return super.getLocalAddress();
    }

    @Override
    public ResourceAddress getRemoteAddress() {
        return super.getRemoteAddress();
    }

    @Override
    public void reset(final Throwable cause) {
        super.reset(cause);
    }
}

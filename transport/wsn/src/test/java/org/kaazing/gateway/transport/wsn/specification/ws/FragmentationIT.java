/*
 * Copyright 2014, Kaazing Corporation. All rights reserved.
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

package org.kaazing.gateway.transport.wsn.specification.ws;

import org.apache.mina.core.filterchain.IoFilterChain;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.DisableOnDebug;
import org.junit.rules.TestRule;
import org.junit.rules.Timeout;
import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.resource.address.ResourceAddressFactory;
import org.kaazing.gateway.transport.BridgeServiceFactory;
import org.kaazing.gateway.transport.IoHandlerAdapter;
import org.kaazing.gateway.transport.TransportFactory;
import org.kaazing.gateway.transport.http.HttpAcceptor;
import org.kaazing.gateway.transport.http.HttpConnector;
import org.kaazing.gateway.transport.nio.internal.NioSocketAcceptor;
import org.kaazing.gateway.transport.nio.internal.NioSocketConnector;
import org.kaazing.gateway.transport.ws.WsAcceptor;
import org.kaazing.gateway.transport.ws.bridge.filter.WsBuffer;
import org.kaazing.gateway.transport.ws.extension.WebSocketExtensionFactory;
import org.kaazing.gateway.transport.wsn.WsnAcceptor;
import org.kaazing.gateway.transport.wsn.WsnSession;
import org.kaazing.gateway.util.scheduler.SchedulerProvider;
import org.kaazing.k3po.junit.annotation.Specification;
import org.kaazing.k3po.junit.rules.K3poRule;
import org.kaazing.mina.core.buffer.IoBufferAllocatorEx;
import org.kaazing.mina.core.buffer.IoBufferEx;
import org.kaazing.mina.core.session.IoSessionEx;
import org.kaazing.test.util.MethodExecutionTrace;

import java.net.URI;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.rules.RuleChain.outerRule;
import static org.kaazing.gateway.resource.address.ws.WsResourceAddress.LIGHTWEIGHT;

/**
 * RFC-6455, section 5.4 "Fragmentation"
 */
public class FragmentationIT {

    private final K3poRule k3po = new K3poRule().setScriptRoot("org/kaazing/specification/ws/fragmentation");
    
    
    public TestRule testExecutionTrace = new MethodExecutionTrace();
    
    public final TestRule timeout = new DisableOnDebug(new Timeout(5, SECONDS));

    @Rule
    public final TestRule chain = outerRule(k3po).around(timeout).around(testExecutionTrace);

    private SchedulerProvider schedulerProvider;

    private static int NETWORK_OPERATION_WAIT_SECS = 10; // was 3, increasing for loaded environments


    private ResourceAddressFactory addressFactory;
    private BridgeServiceFactory serviceFactory;

    private NioSocketConnector tcpConnector;
    private HttpConnector httpConnector;
    
    private NioSocketAcceptor tcpAcceptor;
    private HttpAcceptor httpAcceptor;
    private WsnAcceptor wsnAcceptor;
    private WsAcceptor wsAcceptor;

    private static final boolean DEBUG = false;

    
    @Before
    public void init() {
        schedulerProvider = new SchedulerProvider();
        
        addressFactory = ResourceAddressFactory.newResourceAddressFactory();
        Map<String, Object> config = Collections.emptyMap();
        TransportFactory transportFactory = TransportFactory.newTransportFactory(config);
        serviceFactory = new BridgeServiceFactory(transportFactory);

        tcpAcceptor = (NioSocketAcceptor)transportFactory.getTransport("tcp").getAcceptor();
        tcpAcceptor.setResourceAddressFactory(addressFactory);
        tcpAcceptor.setBridgeServiceFactory(serviceFactory);
        tcpAcceptor.setSchedulerProvider(schedulerProvider);
        
        tcpConnector = (NioSocketConnector)transportFactory.getTransport("tcp").getConnector();
        tcpConnector.setResourceAddressFactory(addressFactory);
        tcpConnector.setBridgeServiceFactory(serviceFactory);
        
        httpAcceptor = (HttpAcceptor)transportFactory.getTransport("http").getAcceptor();
        httpAcceptor.setBridgeServiceFactory(serviceFactory);
        httpAcceptor.setResourceAddressFactory(addressFactory);
        httpAcceptor.setSchedulerProvider(schedulerProvider);
        
        httpConnector = (HttpConnector)transportFactory.getTransport("http").getConnector();
        httpConnector.setBridgeServiceFactory(serviceFactory);
        httpConnector.setResourceAddressFactory(addressFactory);

        wsnAcceptor = (WsnAcceptor)transportFactory.getTransport("wsn").getAcceptor();
        wsnAcceptor.setBridgeServiceFactory(serviceFactory);
        wsnAcceptor.setResourceAddressFactory(addressFactory);
        wsnAcceptor.setSchedulerProvider(schedulerProvider);
        wsAcceptor = new WsAcceptor(WebSocketExtensionFactory.newInstance());
        wsnAcceptor.setWsAcceptor(wsAcceptor);

    }

    @After
    public void disposeConnector() {
        if (tcpAcceptor != null) {
            tcpAcceptor.dispose();
        }
        if (httpAcceptor != null) {
            httpAcceptor.dispose();
        }
        if (wsnAcceptor != null) {
            wsnAcceptor.dispose();
        }
        if (tcpConnector != null) {
            tcpConnector.dispose();
        }
        if (httpConnector != null) {
            httpConnector.dispose();
        }
    }
    
    private void bindTo8080(IoHandlerAdapter adapter) {
        final URI location = URI.create("ws://localhost:8080/echo");
        Map<String, Object> addressOptions = Collections.emptyMap();
        ResourceAddress address = addressFactory.newResourceAddress(location, addressOptions);
        
        wsnAcceptor.bind(address, adapter, null);
    }
    

    @Test
    @Specification({
        "client.send.continuation.payload.length.125.not.fragmented/handshake.request.and.frame"
        })
    public void shouldFailWebSocketConnectionWhenClientSendContinuationFrameWithPayloadNotFragmented() throws Exception {
        IoHandlerAdapter<IoSessionEx> acceptHandler = textFragmentIoHandlerAdapter(WsBuffer.Kind.CONTINUATION);
        
        bindTo8080(acceptHandler);
        k3po.finish();
    }

    @Test
    @Specification({
        "client.send.continuation.payload.length.125.fragmented/handshake.request.and.frames"
        })
    public void shouldFailWebSocketConnectionWhenClientSendContinuationFrameWithPayloadFragmented() throws Exception {
        IoHandlerAdapter<IoSessionEx> acceptHandler = textFragmentIoHandlerAdapter(WsBuffer.Kind.CONTINUATION);
        
        bindTo8080(acceptHandler);
        k3po.finish();
    }

    @Test
    @Specification({
        "client.echo.text.payload.length.125.not.fragmented/handshake.request.and.frame"
        })
    public void shouldEchoClientSendTextFrameWithPayloadNotFragmented() throws Exception {
        IoHandlerAdapter<IoSessionEx> acceptHandler = textFragmentIoHandlerAdapter(WsBuffer.Kind.TEXT);
        
        bindTo8080(acceptHandler);
        k3po.finish();
    }

    @Test
    @Ignore("Gateway throws error when trying to send empty payload")
    @Specification({
        "client.echo.text.payload.length.0.fragmented/handshake.request.and.frames"
        })
    public void shouldEchoClientSendTextFrameWithEmptyPayloadFragmented() throws Exception {
        IoHandlerAdapter<IoSessionEx> acceptHandler = textFragmentIoHandlerAdapter(WsBuffer.Kind.TEXT);
        
        bindTo8080(acceptHandler);
        
        k3po.finish();
    }

    @Test
    @Ignore("Gateway throws error when trying to send empty payload")
    @Specification({
        "client.echo.text.payload.length.0.fragmented.with.injected.ping.pong/handshake.request.and.frames"
        })
    public void shouldEchoClientSendTextFrameWithEmptyPayloadFragmentedAndInjectedPingPong() throws Exception {
        IoHandlerAdapter<IoSessionEx> acceptHandler = textFragmentIoHandlerAdapter(WsBuffer.Kind.TEXT);
        
        bindTo8080(acceptHandler);
        k3po.finish();
    }

    @Test
    @Specification({
        "client.echo.text.payload.length.125.fragmented/handshake.request.and.frames"
        })
    public void shouldEchoClientSendTextFrameWithPayloadFragmented() throws Exception {
        IoHandlerAdapter<IoSessionEx> acceptHandler = textFragmentIoHandlerAdapter(WsBuffer.Kind.TEXT);
        
        bindTo8080(acceptHandler);
        k3po.finish();
    }

    @Test
    @Specification({
        "client.echo.text.payload.length.125.fragmented.with.some.empty.fragments/handshake.request.and.frames"
        })
    public void shouldEchoClientSendTextFrameWithPayloadFragmentedWithSomeEmptyFragments() throws Exception {
        IoHandlerAdapter<IoSessionEx> acceptHandler = textFragmentIoHandlerAdapter(WsBuffer.Kind.TEXT);
        
        bindTo8080(acceptHandler);
        k3po.finish();
    }

    @Test
    @Ignore("TODO: investigate")
    @Specification({
        "client.echo.text.payload.length.125.fragmented.but.not.utf8.aligned/handshake.request.and.frames"
        })
    public void shouldEchoClientSendTextFrameWithPayloadFragmentedEvenWhenNotUTF8Aligned() throws Exception {
        IoHandlerAdapter<IoSessionEx> acceptHandler = textFragmentIoHandlerAdapter(WsBuffer.Kind.TEXT);
        
        bindTo8080(acceptHandler);
        k3po.finish();
    }

    @Test
    @Specification({
        "client.echo.text.payload.length.125.fragmented.with.injected.ping.pong/handshake.request.and.frames"
        })
    public void shouldEchoClientSendTextFrameWithPayloadFragmentedAndInjectedPingPong() throws Exception {
        IoHandlerAdapter<IoSessionEx> acceptHandler = textFragmentIoHandlerAdapter(WsBuffer.Kind.TEXT);
        
        bindTo8080(acceptHandler);
        k3po.finish();
    }

    @Test
    @Specification({
        "client.send.text.payload.length.125.fragmented.but.not.continued/handshake.request.and.frames"
        })
    public void shouldFailWebSocketConnectionWhenClientSendTextFrameWithPayloadFragmentedButNotContinued() throws Exception {
        IoHandlerAdapter<IoSessionEx> acceptHandler = textFragmentIoHandlerAdapter(WsBuffer.Kind.TEXT);
        
        bindTo8080(acceptHandler);
        k3po.finish();
    }

    @Test
    @Specification({
        "client.echo.binary.payload.length.125.not.fragmented/handshake.request.and.frame"
        })
    public void shouldEchoClientSendBinaryFrameWithPayloadNotFragmented() throws Exception {
        IoHandlerAdapter<IoSessionEx> acceptHandler = textFragmentIoHandlerAdapter(WsBuffer.Kind.BINARY);
        
        bindTo8080(acceptHandler);
        k3po.finish();
    }

    @Test
    @Ignore("Gateway throws error when trying to send empty payload")
    @Specification({
        "client.echo.binary.payload.length.0.fragmented/handshake.request.and.frames"
        })
    public void shouldEchoClientSendBinaryFrameWithEmptyPayloadFragmented() throws Exception {
        IoHandlerAdapter<IoSessionEx> acceptHandler = textFragmentIoHandlerAdapter(WsBuffer.Kind.BINARY);
        
        bindTo8080(acceptHandler);
        k3po.finish();
    }

    @Test
    @Ignore("Gateway throws error when trying to send empty payload")
    @Specification({
        "client.echo.binary.payload.length.0.fragmented.with.injected.ping.pong/handshake.request.and.frames"
        })
    public void shouldEchoClientSendBinaryFrameWithEmptyPayloadFragmentedAndInjectedPingPong() throws Exception {
        IoHandlerAdapter<IoSessionEx> acceptHandler = textFragmentIoHandlerAdapter(WsBuffer.Kind.BINARY);
        
        bindTo8080(acceptHandler);
        k3po.finish();
    }

    @Test
    @Specification({
        "client.echo.binary.payload.length.125.fragmented/handshake.request.and.frames"
        })
    public void shouldEchoClientSendBinaryFrameWithPayloadFragmented() throws Exception {
        IoHandlerAdapter<IoSessionEx> acceptHandler = textFragmentIoHandlerAdapter(WsBuffer.Kind.BINARY);
        
        bindTo8080(acceptHandler);
        k3po.finish();
    }

    @Test
    @Specification({
        "client.echo.binary.payload.length.125.fragmented.with.some.empty.fragments/handshake.request.and.frames"
        })
    public void shouldEchoClientSendBinaryFrameWithPayloadFragmentedWithSomeEmptyFragments() throws Exception {
        IoHandlerAdapter<IoSessionEx> acceptHandler = textFragmentIoHandlerAdapter(WsBuffer.Kind.BINARY);
        
        bindTo8080(acceptHandler);
        k3po.finish();
    }

    @Test
    @Specification({
        "client.echo.binary.payload.length.125.fragmented.with.injected.ping.pong/handshake.request.and.frames"
        })
    public void shouldEchoClientSendBinaryFrameWithPayloadFragmentedAndInjectedPingPong() throws Exception {
        IoHandlerAdapter<IoSessionEx> acceptHandler = textFragmentIoHandlerAdapter(WsBuffer.Kind.BINARY);
        
        bindTo8080(acceptHandler);
        k3po.finish();
    }

    @Test
    @Specification({
        "client.send.binary.payload.length.125.fragmented.but.not.continued/handshake.request.and.frames"
        })
    public void shouldFailWebSocketConnectionWhenClientSendBinaryFrameWithPayloadFragmentedButNotContinued() throws Exception {
        bindTo8080(new IoHandlerAdapter());
        k3po.finish();
    }

    @Test
    @Specification({
        "client.send.close.payload.length.2.fragmented/handshake.request.and.frames"
        })
    public void shouldFailWebSocketConnectionWhenClientSendCloseFrameWithPayloadFragmented() throws Exception {
        bindTo8080(new IoHandlerAdapter());
        k3po.finish();
    }

    @Test
    @Specification({
        "client.send.ping.payload.length.0.fragmented/handshake.request.and.frames"
        })
    public void shouldFailWebSocketConnectionWhenClientSendPingFrameWithPayloadFragmented() throws Exception {
        bindTo8080(new IoHandlerAdapter());
        k3po.finish();
    }

    @Test
    @Specification({
        "client.send.pong.payload.length.0.fragmented/handshake.request.and.frames"
        })
    public void shouldFailWebSocketConnectionWhenClientSendPongFrameWithPayloadFragmented() throws Exception {
        bindTo8080(new IoHandlerAdapter());
        k3po.finish();
    }
    
    /** special IoHandlerAdapter for consolidating message fragments
     * @return
     */
    private IoHandlerAdapter<IoSessionEx> textFragmentIoHandlerAdapter(WsBuffer.Kind kind) {
        IoHandlerAdapter<IoSessionEx> acceptHandler = new IoHandlerAdapter<IoSessionEx>() {
            List<ByteBuffer> bufferList = new ArrayList<ByteBuffer>();
            @Override
            protected void doSessionOpened(IoSessionEx session) throws Exception {
                // custom logic
            }
            
            @Override
            protected void doMessageReceived(IoSessionEx session, Object message) throws Exception {
                WsnSession wsnSession = (WsnSession) session;

                if (wsnSession != null) {
                    IoFilterChain filterChain = wsnSession.getFilterChain();
                    IoBufferAllocatorEx<? extends WsBuffer> allocator = wsnSession.getBufferAllocator();

                    final boolean hasPostUpgradeChildWsnSession = wsnSession.getHandler() == this;
                    final ResourceAddress wsnSessionLocalAddress = wsnSession.getLocalAddress();
                    final boolean isLightweightWsnSession = wsnSessionLocalAddress.getOption(LIGHTWEIGHT);
                    boolean sendMessagesDirect = isLightweightWsnSession
                                                 && hasPostUpgradeChildWsnSession; // post-upgrade
                    if ( sendMessagesDirect ) {
                        filterChain.fireMessageReceived(message);
                        return;
                    }
                    
                    WsBuffer wsMessage = (WsBuffer) message;
                    
                    
                    bufferList.add(wsMessage.buf());
                     
                    if (wsMessage.isFin()) { 
                        ByteBuffer buffer = ByteBuffer.allocate(4096);
                        
                        for (ByteBuffer bb : bufferList) {
                            buffer.put(bb);
                        }
                        buffer.flip();
                        WsBuffer wsBuffer = allocator.wrap(buffer, IoBufferEx.FLAG_SHARED);
                        wsBuffer.setKind(kind);
                        session.write(wsBuffer);
                    }
                }
            }

        };
        return acceptHandler;
    }
}
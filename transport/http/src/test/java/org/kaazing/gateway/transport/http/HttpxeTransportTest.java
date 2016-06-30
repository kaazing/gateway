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
package org.kaazing.gateway.transport.http;

import static java.util.EnumSet.noneOf;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertSame;
import static org.jboss.netty.util.CharsetUtil.UTF_8;
import static org.kaazing.gateway.resource.address.ResourceAddress.TRANSPORT;
import static org.kaazing.gateway.resource.address.ResourceAddressFactory.newResourceAddressFactory;
import static org.kaazing.gateway.transport.http.HttpStatus.CLIENT_NOT_FOUND;
import static org.kaazing.gateway.transport.http.HttpStatus.REDIRECT_FOUND;
import static org.kaazing.gateway.transport.http.HttpStatus.SUCCESS_OK;
import static org.kaazing.gateway.transport.http.HttpVersion.HTTP_1_1;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.filterchain.IoFilter;
import org.apache.mina.core.filterchain.IoFilterChain;
import org.apache.mina.core.future.CloseFuture;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.future.IoFutureListener;
import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.lib.concurrent.Synchroniser;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.resource.address.ResourceAddressFactory;
import org.kaazing.gateway.resource.address.http.HttpInjectableHeader;
import org.kaazing.gateway.transport.AbstractBridgeAcceptor;
import org.kaazing.gateway.transport.BridgeConnector;
import org.kaazing.gateway.transport.BridgeServiceFactory;
import org.kaazing.gateway.transport.BridgeSession;
import org.kaazing.gateway.transport.IoFilterAdapter;
import org.kaazing.gateway.transport.TransportFactory;
import org.kaazing.gateway.transport.pipe.NamedPipeAcceptor;
import org.kaazing.gateway.transport.pipe.NamedPipeConnector;
import org.kaazing.gateway.util.scheduler.SchedulerProvider;
import org.kaazing.mina.core.buffer.IoBufferAllocatorEx;
import org.kaazing.mina.core.buffer.IoBufferEx;
import org.kaazing.mina.core.buffer.SimpleBufferAllocator;
import org.kaazing.mina.core.session.IoSessionEx;

/**
 * README: If you see failures because the bytes coming back are not what is expected,
 * go to the bottom of the file and print out the merged buffers.  This vastly helps with debugging.
 */
public class HttpxeTransportTest {

    private ResourceAddressFactory addressFactory = newResourceAddressFactory();

    private Mockery context = newMockeryWithClassImposterizer();
    private IoHandler handler = context.mock(IoHandler.class);

    private ResourceAddress httpxeAddress;
    private ResourceAddress pipeAddress;
    private AbstractBridgeAcceptor httpxeAcceptor;


    private static IoBuffer wrap(byte[] array) {
        return SimpleBufferAllocator.BUFFER_ALLOCATOR.wrap(ByteBuffer.wrap(array));
    }

    @Before
    public void setupServiceFactory() {
        Map<String, ?> config = Collections.emptyMap();
        TransportFactory transportFactory = TransportFactory.newTransportFactory(config);
        BridgeServiceFactory serviceFactory = new BridgeServiceFactory(transportFactory);

        HttpAcceptor httpAcceptor = (HttpAcceptor)transportFactory.getTransport("http").getAcceptor();
        httpAcceptor.setResourceAddressFactory(addressFactory);
        httpAcceptor.setBridgeServiceFactory(serviceFactory);
        httpAcceptor.setSchedulerProvider(new SchedulerProvider());

        NamedPipeAcceptor pipeAcceptor = (NamedPipeAcceptor)transportFactory.getTransport("pipe").getAcceptor();
        pipeAcceptor.setResourceAddressFactory(addressFactory);
        pipeAcceptor.setBridgeServiceFactory(serviceFactory);

        NamedPipeConnector pipeConnector = (NamedPipeConnector)transportFactory.getTransport("pipe").getConnector();
        pipeConnector.setBridgeServiceFactory(serviceFactory);
        pipeConnector.setResourceAddressFactory(addressFactory);
        pipeConnector.setNamedPipeAcceptor(pipeAcceptor);

        String transportURI = "pipe://integrate";
        String location = "httpxe://localhost:8000/";
        Map<String, Object> options = new HashMap<>();
        options.put("http[http/1.1].injectableHeaders", noneOf(HttpInjectableHeader.class));
        options.put("http[http/1.1].transport", transportURI);

        httpxeAddress = addressFactory.newResourceAddress(location, options);
        pipeAddress = addressFactory.newResourceAddress(transportURI);

        httpxeAcceptor = (AbstractBridgeAcceptor) serviceFactory.newBridgeAcceptor(httpxeAddress);
        this.pipeConnector = serviceFactory.newBridgeConnector(pipeAddress);
    }

    @Before
    public void setupDefaultExpectations() throws Exception {
        context.checking(new Expectations() { {
            allowing(handler).messageSent(with(any(IoSession.class)), with(any(Object.class)));
            allowing(handler).sessionClosed(with(any(IoSession.class)));
        } });
        context.setThreadingPolicy(new Synchroniser());
    }


    @Test
    public void shouldBindAndUnbindLeavingEmptyBindingsMaps() throws Exception {

        Map<String, Object> acceptOptions = new HashMap<>();
        acceptOptions.put(TRANSPORT.name(), "pipe://transport");

        final String connectURIString = "httpxe://localhost:8000/path";
        final ResourceAddress bindAddress =
                addressFactory.newResourceAddress(
                        connectURIString,
                        acceptOptions);

        final IoHandler ioHandler = new org.kaazing.gateway.transport.IoHandlerAdapter();

        int[] rounds = new int[]{1,2,10};
        for ( int iterationCount: rounds ) {
            for ( int i = 0; i < iterationCount; i++) {
                httpxeAcceptor.bind(bindAddress, ioHandler, null);
            }
            for (int j = 0; j < iterationCount; j++) {
                httpxeAcceptor.unbind(bindAddress);
            }
            Assert.assertTrue(httpxeAcceptor.emptyBindings());
        }
    }


    @Test
    public void encodeEmulatedHttpResponse() throws Exception {

        final byte[] array = ("HTTP/1.1 200 OK\r\n" +
                              "Cache-Control: no-cache\r\n" +
                              "Content-Length: 69\r\n" +
                              "Content-Type: text/plain;charset=UTF-8\r\n" +
                              "Set-Cookie: KSSOID=12345;\r\n" +
                              "\r\n" +
                              "HTTP/1.1 302 Cross-Origin Redirect\r\n" +
                              "Location: https://www.w3.org/\r\n" +
                              "\r\n").getBytes(UTF_8);

        context.checking(new Expectations() { {
            allowing(handler).sessionCreated(with(any(IoSession.class)));
            allowing(handler).sessionOpened(with(any(IoSession.class)));

            oneOf(handler).messageReceived(with(any(IoSession.class)), with(equal(wrap(array))));
        } });

        httpxeAcceptor.bind(httpxeAddress, new IoHandlerAdapter() {

            @Override
            public void sessionOpened(IoSession session) throws Exception {
                HttpAcceptSession httpSession = (HttpAcceptSession) session;
                httpSession.setVersion(HTTP_1_1);
                httpSession.setStatus(REDIRECT_FOUND);
                httpSession.setReason("Cross-Origin Redirect");
                httpSession.setWriteHeader("Location", "https://www.w3.org/");
                httpSession.setWriteHeader("Set-Cookie", "KSSOID=12345;");
                httpSession.close(false).addListener(CLOSE_TRANSPORT_LISTENER);
            }

        }, null);

        ConnectFuture future = pipeConnector.connect(pipeAddress, handler, null);
        IoSession session = future.await().getSession();
        IoFilterChain filterChain = session.getFilterChain();
        filterChain.addLast("merge", new IoBufferMergeFilter());
        session.write(wrap((
                "POST / HTTP/1.1\r\n" +
                "Host: localhost:8000\r\n" +
                "Content-Type: application/x-message-http\r\n" +
                "Content-Length: 18\r\n" +
                "X-Next-Protocol: httpxe/1.1\r\n" +
                "\r\n" +
                "GET / HTTP/1.1\r\n" +
                "\r\n").getBytes(UTF_8))).await();

        context.assertIsSatisfied();
    }

    @Test
    public void encodeEmulatedTextPlainHttpResponse() throws Exception {

        final byte[] array = ("HTTP/1.1 200 OK\r\n" +
                              "Content-Length: 45\r\n" +
                              "Content-Type: text/plain\r\n" +
                              "\r\n" +
                              "HTTP/1.1 200 OK\r\n" +
                              "Content-Type: text/plain\r\n" +
                              "\r\n").getBytes(UTF_8);

        context.checking(new Expectations() { {
            allowing(handler).sessionCreated(with(any(IoSession.class)));
            allowing(handler).sessionOpened(with(any(IoSession.class)));

            oneOf(handler).messageReceived(with(any(IoSession.class)), with(equal(wrap(array))));
        } });

        httpxeAcceptor.bind(httpxeAddress, new IoHandlerAdapter() {

            @Override
            public void sessionOpened(IoSession session) throws Exception {
                HttpAcceptSession httpSession = (HttpAcceptSession) session;
                httpSession.setVersion(HTTP_1_1);
                httpSession.setStatus(SUCCESS_OK);
                httpSession.setWriteHeader("Content-Type", "text/plain");
                httpSession.close(false).addListener(CLOSE_TRANSPORT_LISTENER);
            }

        }, null);

        ConnectFuture future = pipeConnector.connect(pipeAddress, handler, null);
        IoSession session = future.await().getSession();
        IoFilterChain filterChain = session.getFilterChain();
        filterChain.addLast("merge", new IoBufferMergeFilter());
        session.write(wrap((
                "POST / HTTP/1.1\r\n" +
                "Host: localhost:8000\r\n" +
                "Content-Type: application/x-message-http\r\n" +
                "Content-Length: 18\r\n" +
                "X-Next-Protocol: httpxe/1.1\r\n" +
                "\r\n" +
                "GET / HTTP/1.1\r\n" +
                "\r\n").getBytes(UTF_8))).await();

        context.assertIsSatisfied();
    }

    @Test
    public void encodeEmulatedTextHttpResponse() throws Exception {

        final byte[] array = ("HTTP/1.1 200 OK\r\n" +
                              "Content-Length: 64\r\n" +
                              "Content-Type: text/plain;charset=windows-1252\r\n" +
                              "\r\n" +
                              "HTTP/1.1 200 OK\r\n" +
                              "Content-Type: text/xyz;charset=windows-1252\r\n" +
                              "\r\n").getBytes(UTF_8);

        context.checking(new Expectations() { {
            allowing(handler).sessionCreated(with(any(IoSession.class)));
            allowing(handler).sessionOpened(with(any(IoSession.class)));

            oneOf(handler).messageReceived(with(any(IoSession.class)), with(equal(wrap(array))));
        } });

        httpxeAcceptor.bind(httpxeAddress, new IoHandlerAdapter() {

            @Override
            public void sessionOpened(IoSession session) throws Exception {
                HttpAcceptSession httpSession = (HttpAcceptSession) session;
                httpSession.setVersion(HTTP_1_1);
                httpSession.setStatus(SUCCESS_OK);
                httpSession.setWriteHeader("Content-Type", "text/xyz;charset=windows-1252");
                httpSession.close(false).addListener(CLOSE_TRANSPORT_LISTENER);
            }

        }, null);

        ConnectFuture future = pipeConnector.connect(pipeAddress, handler, null);
        IoSession session = future.await().getSession();
        IoFilterChain filterChain = session.getFilterChain();
        filterChain.addLast("merge", new IoBufferMergeFilter());
        session.write(wrap((
                "POST / HTTP/1.1\r\n" +
                "Host: localhost:8000\r\n" +
                "Content-Type: application/x-message-http\r\n" +
                "Content-Length: 18\r\n" +
                "X-Next-Protocol: httpxe/1.1\r\n" +
                "\r\n" +
                "GET / HTTP/1.1\r\n" +
                "\r\n").getBytes(UTF_8))).await();

        context.assertIsSatisfied();
    }

    @Test
    public void encodeEmulatedCORSResponse() throws Exception {

        final byte[] array = ("HTTP/1.1 200 OK\r\n" +
                              "Access-Control-Allow-Headers: x-websocket-extensions\r\n" +
                              "Content-Length: 64\r\n" +
                              "Content-Type: text/plain;charset=windows-1252\r\n" +
                              "\r\n" +
                              "HTTP/1.1 200 OK\r\n" +
                              "Content-Type: text/xyz;charset=windows-1252\r\n" +
                              "\r\n").getBytes(UTF_8);

        context.checking(new Expectations() { {
            allowing(handler).sessionCreated(with(any(IoSession.class)));
            allowing(handler).sessionOpened(with(any(IoSession.class)));

            oneOf(handler).messageReceived(with(any(IoSession.class)), with(equal(wrap(array))));
        } });

        httpxeAcceptor.bind(httpxeAddress, new IoHandlerAdapter() {

            @Override
            public void sessionOpened(IoSession session) throws Exception {
                HttpAcceptSession httpSession = (HttpAcceptSession) session;
                httpSession.setVersion(HTTP_1_1);
                httpSession.setStatus(SUCCESS_OK);
                httpSession.setWriteHeader("Access-Control-Allow-Headers", "x-websocket-extensions");
                httpSession.setWriteHeader("Content-Type", "text/xyz;charset=windows-1252");
                httpSession.close(false).addListener(CLOSE_TRANSPORT_LISTENER);
            }

        }, null);

        ConnectFuture future = pipeConnector.connect(pipeAddress, handler, null);
        IoSession session = future.await().getSession();
        IoFilterChain filterChain = session.getFilterChain();
        filterChain.addLast("merge", new IoBufferMergeFilter());
        session.write(wrap((
                "POST / HTTP/1.1\r\n" +
                "Host: localhost:8000\r\n" +
                "Content-Type: application/x-message-http\r\n" +
                "Content-Length: 18\r\n" +
                "X-Next-Protocol: httpxe/1.1\r\n" +
                "\r\n" +
                "GET / HTTP/1.1\r\n" +
                "\r\n").getBytes(UTF_8))).await();

        context.assertIsSatisfied();
    }

    @Test
    public void encodeEmulatedGzippedTextResponse() throws Exception {

        final SimpleBufferAllocator allocator = SimpleBufferAllocator.BUFFER_ALLOCATOR;
        final IoBuffer expected = allocator.wrap(allocator.allocate(227));
        expected.put(("HTTP/1.1 200 OK\r\n" +
                      "Connection: close\r\n" +
                      "Content-Encoding: gzip\r\n" +
                      "Content-Type: text/plain;charset=windows-1252\r\n" +
                      "\r\n").getBytes(UTF_8));
        expected.put(new byte[] { 31, (byte)139, 8, 0, (byte)240, 106, 64, 78, 2, (byte)255 });
        expected.put(new byte[] { 0, (byte)0x40, 0x0, (byte)0xBF, (byte)0xFF });
        expected.put(("HTTP/1.1 200 OK\r\n" +
                      "Content-Type: text/xyz;charset=windows-1252\r\n" +
                      "\r\n").getBytes());
        expected.flip();

        context.checking(new Expectations() { {
            allowing(handler).sessionCreated(with(any(IoSession.class)));
            allowing(handler).sessionOpened(with(any(IoSession.class)));

            oneOf(handler).messageReceived(with(any(IoSession.class)), with(equal(expected)));
        } });

        httpxeAcceptor.bind(httpxeAddress, new IoHandlerAdapter() {

            @Override
            public void sessionOpened(IoSession session) throws Exception {
                HttpAcceptSession httpSession = (HttpAcceptSession) session;
                httpSession.setVersion(HTTP_1_1);
                httpSession.setStatus(SUCCESS_OK);
                httpSession.setWriteHeader("Connection", "close");
                httpSession.setWriteHeader("Content-Type", "text/xyz;charset=windows-1252");
                httpSession.setWriteHeader("Content-Encoding", "gzip");
                httpSession.close(false).addListener(CLOSE_TRANSPORT_LISTENER);
            }

        }, null);

        ConnectFuture future = pipeConnector.connect(pipeAddress, handler, null);
        IoSession session = future.await().getSession();
        IoFilterChain filterChain = session.getFilterChain();
        filterChain.addLast("merge", new IoBufferMergeFilter());
        session.write(wrap((
                "POST /foo?.kbp HTTP/1.1\r\n" +
                "Host: localhost:8000\r\n" +
                "Content-Type: application/x-message-http\r\n" +
                "Content-Length: 26\r\n" +
                "X-Next-Protocol: httpxe/1.1\r\n" +
                "\r\n" +
                "GET /foo?.kbp HTTP/1.1\r\n" +
                "\r\n").getBytes(UTF_8))).await();

        context.assertIsSatisfied();
    }

    @Test
    public void encodeEmulatedHttpResponseWithCacheControl() throws Exception {

        final byte[] array = ("HTTP/1.1 200 OK\r\n" +
                              "Cache-Control: private\r\n" +
                              "Content-Length: 69\r\n" +
                              "Content-Type: text/plain;charset=UTF-8\r\n" +
                              "\r\n" +
                              "HTTP/1.1 302 Cross-Origin Redirect\r\n" +
                              "Location: https://www.w3.org/\r\n" +
                              "\r\n").getBytes(UTF_8);

        context.checking(new Expectations() { {
            allowing(handler).sessionCreated(with(any(IoSession.class)));
            allowing(handler).sessionOpened(with(any(IoSession.class)));

            oneOf(handler).messageReceived(with(any(IoSession.class)), with(equal(wrap(array))));
        } });

        httpxeAcceptor.bind(httpxeAddress, new IoHandlerAdapter() {

            @Override
            public void sessionOpened(IoSession session) throws Exception {
                HttpAcceptSession httpSession = (HttpAcceptSession) session;
                httpSession.setVersion(HTTP_1_1);
                httpSession.setStatus(REDIRECT_FOUND);
                httpSession.setReason("Cross-Origin Redirect");
                httpSession.setWriteHeader("Cache-Control", "private");
                httpSession.setWriteHeader("Location", "https://www.w3.org/");
                httpSession.close(false).addListener(CLOSE_TRANSPORT_LISTENER);
            }

        }, null);

        ConnectFuture future = pipeConnector.connect(pipeAddress, handler, null);
        IoSession session = future.await().getSession();
        IoFilterChain filterChain = session.getFilterChain();
        filterChain.addLast("merge", new IoBufferMergeFilter());
        session.write(wrap((
                "POST / HTTP/1.1\r\n" +
                "Host: localhost:8000\r\n" +
                "Content-Type: application/x-message-http\r\n" +
                "Content-Length: 18\r\n" +
                "X-Next-Protocol: httpxe/1.1\r\n" +
                "\r\n" +
                "GET / HTTP/1.1\r\n" +
                "\r\n").getBytes(UTF_8))).await();

        context.assertIsSatisfied();
    }

    @Test
    public void encodeEmulatedBinaryHttpResponse() throws Exception {

        final byte[] array = ("HTTP/1.1 200 OK\r\n" +
                              "Content-Length: 59\r\n" +
                              "Content-Type: application/octet-stream\r\n" +
                              "\r\n" +
                              "HTTP/1.1 200 OK\r\n" +
                              "Content-Type: application/octet-stream\r\n" +
                              "\r\n").getBytes(UTF_8);

        context.checking(new Expectations() { {
            allowing(handler).sessionCreated(with(any(IoSession.class)));
            allowing(handler).sessionOpened(with(any(IoSession.class)));

            oneOf(handler).messageReceived(with(any(IoSession.class)), with(equal(wrap(array))));
        } });

        httpxeAcceptor.bind(httpxeAddress, new IoHandlerAdapter() {

            @Override
            public void sessionOpened(IoSession session) throws Exception {
                HttpAcceptSession httpSession = (HttpAcceptSession) session;
                httpSession.setVersion(HTTP_1_1);
                httpSession.setStatus(SUCCESS_OK);
                httpSession.setWriteHeader("Content-Type", "application/octet-stream");
                httpSession.close(false).addListener(CLOSE_TRANSPORT_LISTENER);
            }

        }, null);

        ConnectFuture future = pipeConnector.connect(pipeAddress, handler, null);
        IoSession session = future.await().getSession();
        IoFilterChain filterChain = session.getFilterChain();
        filterChain.addLast("merge", new IoBufferMergeFilter());
        session.write(wrap((
                "POST / HTTP/1.1\r\n" +
                "Host: localhost:8000\r\n" +
                "Content-Type: application/x-message-http\r\n" +
                "Content-Length: 18\r\n" +
                "X-Next-Protocol: httpxe/1.1\r\n" +
                "\r\n" +
                "GET / HTTP/1.1\r\n" +
                "\r\n").getBytes(UTF_8))).await();

        context.assertIsSatisfied();
    }

    @Test
    public void encodeEmulatedHttpResponseIncomplete() throws Exception {

        final byte[] array = ("HTTP/1.1 200 OK\r\n" +
                              "Connection: close\r\n" +
                              "Content-Type: text/plain;charset=UTF-8\r\n" +
                              "\r\n" +
                              "HTTP/1.1 200 OK\r\n" +
                              "\r\n" +
                              "Hello, world").getBytes(UTF_8);

        context.checking(new Expectations() { {
            allowing(handler).sessionCreated(with(any(IoSession.class)));
            allowing(handler).sessionOpened(with(any(IoSession.class)));

            oneOf(handler).messageReceived(with(any(IoSession.class)), with(equal(wrap(array))));
        } });

        httpxeAcceptor.bind(httpxeAddress, new IoHandlerAdapter() {

            @Override
            public void sessionOpened(IoSession session) throws Exception {
                HttpAcceptSession httpSession = (HttpAcceptSession) session;
                httpSession.setVersion(HTTP_1_1);
                httpSession.setStatus(SUCCESS_OK);
                httpSession.setWriteHeader("Connection", "close");
                httpSession.write(wrap("Hello, world".getBytes(UTF_8)));
                httpSession.close(false).addListener(CLOSE_TRANSPORT_LISTENER);
            }

        }, null);

        ConnectFuture future = pipeConnector.connect(pipeAddress, handler, null);
        IoSession session = future.await().getSession();
        IoFilterChain filterChain = session.getFilterChain();
        filterChain.addLast("merge", new IoBufferMergeFilter());
        session.write(wrap((
                "POST / HTTP/1.1\r\n" +
                "Host: localhost:8000\r\n" +
                "Content-Type: application/x-message-http\r\n" +
                "Content-Length: 18\r\n" +
                "X-Next-Protocol: httpxe/1.1\r\n" +
                "\r\n" +
                "GET / HTTP/1.1\r\n" +
                "\r\n").getBytes(UTF_8))).await();

        context.assertIsSatisfied();
    }

    @Test
    public void encodeEmulatedHttpResponseChunked() throws Exception {

        // note: httpxe layer needs to behave as if Content-Length is implicitly present (implied by outer http layer, even if chunked or connection:close)

        final byte[] array = ("HTTP/1.1 200 OK\r\n" +
                              "Content-Type: text/plain;charset=UTF-8\r\n" +
                              "Transfer-Encoding: chunked\r\n" +
                              "\r\n" +
                              "13\r\n" +
                              "HTTP/1.1 200 OK\r\n" +
                              "\r\n" +
                              "\r\n" +
                              "c\r\n" +
                              "Hello, world" +
                              "\r\n" +
                              "0\r\n" +
                              "\r\n").getBytes(UTF_8);

        context.checking(new Expectations() { {
            allowing(handler).sessionCreated(with(any(IoSession.class)));
            allowing(handler).sessionOpened(with(any(IoSession.class)));

            oneOf(handler).messageReceived(with(any(IoSession.class)), with(equal(wrap(array))));
        } });

        httpxeAcceptor.bind(httpxeAddress, new IoHandlerAdapter() {

            @Override
            public void sessionOpened(IoSession session) throws Exception {
                HttpAcceptSession httpSession = (HttpAcceptSession) session;
                httpSession.setVersion(HTTP_1_1);
                httpSession.setStatus(SUCCESS_OK);
                httpSession.write(wrap("Hello, world".getBytes(UTF_8)));
                httpSession.close(false).addListener(CLOSE_TRANSPORT_LISTENER);
            }

        }, null);

        ConnectFuture future = pipeConnector.connect(pipeAddress, handler, null);
        IoSession session = future.await().getSession();
        IoFilterChain filterChain = session.getFilterChain();
        filterChain.addLast("merge", new IoBufferMergeFilter());
        session.write(wrap((
                "POST / HTTP/1.1\r\n" +
                "Host: localhost:8000\r\n" +
                "Content-Type: application/x-message-http\r\n" +
                "Content-Length: 18\r\n" +
                "X-Next-Protocol: httpxe/1.1\r\n" +
                "\r\n" +
                "GET / HTTP/1.1\r\n" +
                "\r\n").getBytes(UTF_8))).await();

        context.assertIsSatisfied();
    }

    @Test
    public void encodeEmulatedHttpResponseComplete() throws Exception {

        final byte[] array = ("HTTP/1.1 200 OK\r\n" +
                              "Content-Length: 31\r\n" +
                              "Content-Type: text/plain;charset=UTF-8\r\n" +
                              "\r\n" +
                              "HTTP/1.1 200 OK\r\n" +
                              "\r\n" +
                              "Hello, world").getBytes(UTF_8);

        context.checking(new Expectations() { {
            allowing(handler).sessionCreated(with(any(IoSession.class)));
            allowing(handler).sessionOpened(with(any(IoSession.class)));

            oneOf(handler).messageReceived(with(any(IoSession.class)), with(equal(wrap(array))));
        } });

        httpxeAcceptor.bind(httpxeAddress, new IoHandlerAdapter() {

            @Override
            public void sessionOpened(IoSession session) throws Exception {
                HttpAcceptSession httpSession = (HttpAcceptSession) session;
                httpSession.setVersion(HTTP_1_1);
                httpSession.setStatus(SUCCESS_OK);
                //TODO: should not be necessary to suspend reads here but AbstractIoProcessor.updateTrafficControl0
                //TODO: resumes reads during the next suspendWrite call below.
                httpSession.suspendRead();
                httpSession.suspendWrite();
                httpSession.write(wrap("Hello, world".getBytes(UTF_8)));
                // note: shutdown write to indicate no more writes will follow before close
                //       so we can safely calculate the Content-Length header
                httpSession.shutdownWrite();
                httpSession.close(false).addListener(CLOSE_TRANSPORT_LISTENER);
                httpSession.resumeWrite();
            }

        }, null);

        ConnectFuture future = pipeConnector.connect(pipeAddress, handler, null);
        IoSession session = future.await().getSession();
        IoFilterChain filterChain = session.getFilterChain();
        filterChain.addLast("merge", new IoBufferMergeFilter());
        session.write(wrap((
                "POST / HTTP/1.1\r\n" +
                "Host: localhost:8000\r\n" +
                "Content-Type: application/x-message-http\r\n" +
                "Content-Length: 18\r\n" +
                "X-Next-Protocol: httpxe/1.1\r\n" +
                "\r\n" +
                "GET / HTTP/1.1\r\n" +
                "\r\n").getBytes(UTF_8))).await();

        context.assertIsSatisfied();
    }

    @Test
    public void encodeEmulatedHttpResponseWithContentLength() throws Exception {

        final byte[] array = ("HTTP/1.1 200 OK\r\n" +
                              "Content-Length: 31\r\n" +
                              "Content-Type: text/plain;charset=UTF-8\r\n" +
                              "\r\n" +
                              "HTTP/1.1 200 OK\r\n" +
                              "\r\n" +
                              "Hello, world").getBytes(UTF_8);

        context.checking(new Expectations() { {
            allowing(handler).sessionCreated(with(any(IoSession.class)));
            allowing(handler).sessionOpened(with(any(IoSession.class)));

            oneOf(handler).messageReceived(with(any(IoSession.class)), with(equal(wrap(array))));
        } });

        httpxeAcceptor.bind(httpxeAddress, new IoHandlerAdapter() {

            @Override
            public void sessionOpened(IoSession session) throws Exception {
                HttpAcceptSession httpSession = (HttpAcceptSession) session;
                httpSession.setVersion(HTTP_1_1);
                httpSession.setStatus(SUCCESS_OK);
                httpSession.setWriteHeader("Content-Length", "12");
                httpSession.write(wrap("Hello, world".getBytes(UTF_8)));
                httpSession.close(false).addListener(CLOSE_TRANSPORT_LISTENER);
            }

        }, null);

        ConnectFuture future = pipeConnector.connect(pipeAddress, handler, null);
        IoSession session = future.await().getSession();
        IoFilterChain filterChain = session.getFilterChain();
        filterChain.addLast("merge", new IoBufferMergeFilter());
        session.write(wrap((
                "POST / HTTP/1.1\r\n" +
                "Host: localhost:8000\r\n" +
                "Content-Type: application/x-message-http\r\n" +
                "Content-Length: 18\r\n" +
                "X-Next-Protocol: httpxe/1.1\r\n" +
                "\r\n" +
                "GET / HTTP/1.1\r\n" +
                "\r\n").getBytes(UTF_8))).await();

        context.assertIsSatisfied();
    }

    @Test
    public void encodeEmulatedNotFoundHttpResponse() throws Exception {

        final byte[] array = ("HTTP/1.1 404 Not Found\r\n" +
                              "Content-Length: 112\r\n" +
                              "Content-Type: text/plain;charset=UTF-8\r\n" +
                              "\r\n" +
                              "HTTP/1.1 404 Not Found\r\n" +
                              "Content-Type: text/html\r\n" +
                              "\r\n" +
                              "<html><head></head><body><h1>404 Not Found</h1></body></html>").getBytes(UTF_8);

        context.checking(new Expectations() { {
            allowing(handler).sessionCreated(with(any(IoSession.class)));
            allowing(handler).sessionOpened(with(any(IoSession.class)));

            oneOf(handler).messageReceived(with(any(IoSession.class)), with(equal(wrap(array))));
        } });

        httpxeAcceptor.bind(httpxeAddress, new IoHandlerAdapter() {

            @Override
            public void sessionOpened(IoSession session) throws Exception {
                HttpAcceptSession httpSession = (HttpAcceptSession) session;
                httpSession.setVersion(HTTP_1_1);
                httpSession.setStatus(CLIENT_NOT_FOUND);
                httpSession.close(false).addListener(CLOSE_TRANSPORT_LISTENER);
            }

        }, null);

        ConnectFuture future = pipeConnector.connect(pipeAddress, handler, null);
        IoSession session = future.await().getSession();
        IoFilterChain filterChain = session.getFilterChain();
        filterChain.addLast("merge", new IoBufferMergeFilter());
        session.write(wrap((
                "POST / HTTP/1.1\r\n" +
                "Host: localhost:8000\r\n" +
                "Content-Type: application/x-message-http\r\n" +
                "Content-Length: 18\r\n" +
                "X-Next-Protocol: httpxe/1.1\r\n" +
                "\r\n" +
                "GET / HTTP/1.1\r\n" +
                "\r\n").getBytes(UTF_8))).await();

        context.assertIsSatisfied();
    }

    @Test
    public void decodeEnvelopedHttpRequest() throws Exception {

        final byte[] array = ("retry:2500\r\n" +
                              "event:TCPSend\r\n" +
                              "id:24\r\n" +
                              "data:Hello, world\r\n" +
                              "\r\n").getBytes(UTF_8);

        context.checking(new Expectations() { {
            allowing(handler).sessionCreated(with(any(IoSession.class)));
            allowing(handler).sessionOpened(with(any(IoSession.class)));

            oneOf(handler).messageReceived(with(any(IoSession.class)), with(equal(wrap(array))));
        } });

        httpxeAcceptor.bind(httpxeAddress, handler, null);

        ConnectFuture future = pipeConnector.connect(pipeAddress, new IoHandlerAdapter(), null);
        IoSession session = future.await().getSession();
        session.write(wrap((
                "POST / HTTP/1.1\r\n" +
                "Host: localhost:8000\r\n" +
                "Content-Type: application/x-message-http\r\n" +
                "Content-Length: 160\r\n" +
                "\r\n" +
                "POST / HTTP/1.1\r\n" +
                "Authorization: restricted-usage\r\n" +
                "Content-Type: text/event-stream\r\n" +
                "Content-Length: 55\r\n" +
                "\r\n" +
                "retry:2500\r\n" +
                "event:TCPSend\r\n" +
                "id:24\r\n" +
                "data:Hello, world\r\n" +
                "\r\n").getBytes(UTF_8))).await();

        context.assertIsSatisfied();
    }

    @Test
    public void decodeEnvelopedHttpPostRequest() throws Exception {

        final byte[] array = (">|<").getBytes(UTF_8);

        context.checking(new Expectations() { {
            allowing(handler).sessionCreated(with(any(IoSession.class)));
            allowing(handler).sessionOpened(with(any(IoSession.class)));
        } });

        httpxeAcceptor.bind(httpxeAddress, new IoHandlerAdapter() {

            @Override
            public void messageReceived(IoSession session, Object message) throws Exception {
                assertEquals(wrap(array), message);
            }

        }, null);

        ConnectFuture future = pipeConnector.connect(pipeAddress, handler, null);
        IoSession session = future.await().getSession();
        session.write(wrap((
                "POST /path HTTP/1.1\r\n" +
                "Host: localhost:8000\r\n" +
                "Accept: */*\r\n" +
                "Referer: http://gateway.kzng.net:8003/?.kr=xsa\r\n" +
                "Accept-Language: en-us\r\n" +
                "User-Agent: Shockwave Flash\r\n" +
                "x-flash-version: 9,0,124,0\r\n" +
                "Content-Type: application/x-message-http\r\n" +
                "X-Origin: http://localhost:8000\r\n" +
                "Content-Length: 71\r\n" +
                "\r\n" +
                "POST /path HTTP/1.1\r\n" +
                "Content-length: 3\r\n" +
                "Content-type: text/plain\r\n" +
                "\r\n" +
                ">|<").getBytes(UTF_8))).await();

        context.assertIsSatisfied();
    }

    @Test
    public void decodeEnvelopedGetHttpRequest() throws Exception {

        context.checking(new Expectations() { {
            allowing(handler).sessionCreated(with(any(IoSession.class)));
            allowing(handler).sessionOpened(with(any(IoSession.class)));
        } });

        httpxeAcceptor.bind(httpxeAddress, new IoHandlerAdapter() {

            @Override
            public void sessionCreated(IoSession session) throws Exception {
                HttpSession httpSession = (HttpSession) session;
                assertSame(HttpMethod.GET, httpSession.getMethod());
                assertNull(httpSession.getReadHeader("Content-Length"));
                assertNull(httpSession.getReadHeader("Content-Type"));
                assertEquals("value", httpSession.getReadHeader("X-Header"));
            }

        }, null);

        ConnectFuture future = pipeConnector.connect(pipeAddress, handler, null);
        IoSession session = future.await().getSession();
        session.write(wrap((
                "POST / HTTP/1.1\r\n" +
                "Content-Type: application/x-message-http\r\n" +
                "Content-Length: 102\r\n" +
                "X-Header: value\r\n" +
                "\r\n" +
                "GET / HTTP/1.1\r\n" +
                "Authorization: restricted-usage\r\n" +
                "\r\n").getBytes(UTF_8))).await();

        context.assertIsSatisfied();
    }

    @Test
    public void decodeHttpRequestInvalidEnvelopedPath() throws Exception {

        context.checking(new Expectations() { {
            never(handler).sessionCreated(with(any(IoSession.class)));
            never(handler).sessionOpened(with(any(IoSession.class)));
        } });

        httpxeAcceptor.bind(httpxeAddress, handler, null);

        ConnectFuture future = pipeConnector.connect(pipeAddress, new IoHandlerAdapter(), null);
        IoSession session = future.await().getSession();
        session.write(wrap((
                "POST / HTTP/1.1\r\n" +
                "Host: localhost:8000\r\n" +
                "Content-Type: application/x-message-http\r\n" +
                "Content-Length: 65\r\n" +
                "X-Header: value\r\n" +
                "\r\n" +
                "GET /different/path HTTP/1.1\r\n" +
                "Authorization: restricted-usage\r\n" +
                "\r\n").getBytes(UTF_8))).await();

        context.assertIsSatisfied();
    }

    @Test
    public void decodeHttpRequestInvalidEnvelopedVersion() throws Exception {

        context.checking(new Expectations() { {
            never(handler).sessionCreated(with(any(IoSession.class)));
            never(handler).sessionOpened(with(any(IoSession.class)));
        } });

        httpxeAcceptor.bind(httpxeAddress, handler, null);

        ConnectFuture future = pipeConnector.connect(pipeAddress, new IoHandlerAdapter(), null);
        IoSession session = future.await().getSession();
        session.write(wrap((
                "POST / HTTP/1.1\r\n" +
                "Host: localhost:8000\r\n" +
                "Content-Type: application/x-message-http\r\n" +
                "Content-Length: 51\r\n" +
                "X-Header: value\r\n" +
                "\r\n" +
                "GET / HTTP/1.0\r\n" +
                "Authorization: restricted-usage\r\n" +
                "\r\n").getBytes(UTF_8))).await();

        context.assertIsSatisfied();
    }

    @Test
    public void decodeHttpRequestInvalidEnvelopedHeader() throws Exception {

        context.checking(new Expectations() { {
            never(handler).sessionCreated(with(any(IoSession.class)));
            never(handler).sessionOpened(with(any(IoSession.class)));
        } });

        httpxeAcceptor.bind(httpxeAddress, handler, null);

        ConnectFuture future = pipeConnector.connect(pipeAddress, new IoHandlerAdapter(), null);
        IoSession session = future.await().getSession();
        session.write(wrap((
                "POST / HTTP/1.1\r\n" +
                "Host: localhost:8000\r\n" +
                "Content-Type: application/x-message-http\r\n" +
                "Content-Length: 68\r\n" +
                "\r\n" +
                "GET / HTTP/1.1\r\n" +
                "Accept-Charset: value\r\n" +
                "Authorization: restricted-usage\r\n" +
                "\r\n").getBytes(UTF_8))).await();

        context.assertIsSatisfied();
    }

    private static Mockery newMockeryWithClassImposterizer() {
        Mockery context = new Mockery();
        context.setThreadingPolicy(new Synchroniser());
        context.setImposteriser(ClassImposteriser.INSTANCE);
        return context;
    }

    private static final IoFutureListener<CloseFuture> CLOSE_TRANSPORT_LISTENER = new IoFutureListener<CloseFuture>() {
        @Override
        public void operationComplete(CloseFuture future) {
            IoSession session = future.getSession();
            if (session instanceof BridgeSession) {
                BridgeSession bridgeSession = (BridgeSession) session;
                IoSession transportSession = bridgeSession.getParent();
                transportSession.close(false);
                CloseFuture closeFuture = transportSession.getCloseFuture();
                closeFuture.addListener(CLOSE_TRANSPORT_LISTENER);
            }
        }
    };
    private BridgeConnector pipeConnector;

    private final class IoBufferMergeFilter extends IoFilterAdapter<IoSessionEx> {

        private volatile IoBufferEx mergedBuffers;

        @Override
        public void doMessageReceived(IoFilter.NextFilter nextFilter, IoSessionEx session, Object message) throws Exception {

            IoBufferEx buffer = (IoBufferEx) message;
            logger.messageReceived(session, message);
            if (mergedBuffers == null) {
                final IoBufferAllocatorEx<?> allocator = session.getBufferAllocator();
                mergedBuffers = allocator.wrap(allocator.allocate(buffer.remaining()));
                mergedBuffers.setAutoExpander(allocator);
            }
            mergedBuffers.put(buffer);
        }

        @Override
        public void doSessionClosed(IoFilter.NextFilter nextFilter, IoSessionEx session) throws Exception {

            if (mergedBuffers != null) {
                IoBufferEx message = mergedBuffers.flip();
                mergedBuffers = null;

                nextFilter.messageReceived(session, message);
            }
            super.doSessionClosed(nextFilter, session);
        }
    }

    IoHandler logger = new IoHandlerAdapter() {

        @Override
        public void messageReceived(IoSession session, Object message) throws Exception {
            IoBuffer buf = (IoBuffer) message;
            buf.duplicate();
//            buf = buf.duplicate();
//            System.out.print(buf.getString(UTF_8.newDecoder()));

//            System.out.print(buf.getString(109, UTF_8.newDecoder()));
//            System.out.print(buf.limit(buf.position() + 15).slice().getHexDump());
//            System.out.print(buf.skip(15).limit(buf.capacity()).getString(UTF_8.newDecoder()));
        }

    };
}

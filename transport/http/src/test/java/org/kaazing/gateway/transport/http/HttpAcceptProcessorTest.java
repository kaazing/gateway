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

import static java.lang.Thread.currentThread;
import static org.jmock.Expectations.returnValue;
import static org.kaazing.gateway.resource.address.ResourceAddress.TRANSPORT_URI;
import static org.kaazing.mina.core.session.IoSessionEx.IMMEDIATE_EXECUTOR;

import java.nio.ByteBuffer;
import java.util.Properties;
import java.util.concurrent.Executor;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.filterchain.IoFilter;
import org.apache.mina.core.filterchain.IoFilterChain;
import org.apache.mina.core.filterchain.IoFilterChain.Entry;
import org.apache.mina.core.future.DefaultWriteFuture;
import org.apache.mina.core.future.WriteFuture;
import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.write.WriteRequest;
import org.apache.mina.core.write.WriteRequestQueue;
import org.jmock.lib.concurrent.Synchroniser;
import org.junit.Test;
import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.resource.address.ResourceAddressFactory;
import org.kaazing.gateway.resource.address.ResourceOptions;
import org.kaazing.gateway.transport.BridgeSession;
import org.kaazing.gateway.transport.DefaultIoSessionConfigEx;
import org.kaazing.gateway.transport.DefaultTransportMetadata;
import org.kaazing.gateway.transport.ObjectLoggingFilter;
import org.kaazing.gateway.transport.http.bridge.HttpContentMessage;
import org.kaazing.gateway.transport.http.bridge.HttpResponseMessage;
import org.kaazing.gateway.transport.http.bridge.filter.HttpBuffer;
import org.kaazing.gateway.transport.http.bridge.filter.HttpBufferAllocator;
import org.kaazing.gateway.transport.http.bridge.filter.HttpCodecFilter;
import org.kaazing.gateway.transport.test.Expectations;
import org.kaazing.mina.core.buffer.IoBufferEx;
import org.kaazing.mina.core.buffer.SimpleBufferAllocator;
import org.kaazing.mina.core.service.IoServiceEx;
import org.kaazing.mina.core.session.IoSessionEx;
import org.kaazing.mina.core.write.DefaultWriteRequestEx.ShareableWriteRequest;
import org.kaazing.test.util.Mockery;

public class HttpAcceptProcessorTest {

    private HttpBufferAllocator httpAllocator = new HttpBufferAllocator(SimpleBufferAllocator.BUFFER_ALLOCATOR);

    @Test
    public void testWriteMessageCompleted() {

        Mockery context = new Mockery();
        context.setThreadingPolicy(new Synchroniser());
        final IoServiceEx httpService = context.mock(IoServiceEx.class);
        final IoHandler httpHandler = context.mock(IoHandler.class, "httpHandler");
        final IoSessionEx session = context.mock(IoSessionEx.class);
        final IoFilterChain filterChain = context.mock(IoFilterChain.class);
        final IoHandler handler = context.mock(IoHandler.class);
        final WriteRequestQueue writeRequestQueue = context.mock(WriteRequestQueue.class);
        final WriteRequest writeRequest = context.mock(WriteRequest.class);
        final WriteFuture writeFuture = context.mock(WriteFuture.class);
        ByteBuffer buf = ByteBuffer.wrap(new byte[] { 0x00 });
        final IoBufferEx bufEx = httpAllocator.wrap(buf).mark();

        context.checking(new Expectations() {
            {
                try {
                    allowing(httpService).getTransportMetadata(); will(returnValue(new DefaultTransportMetadata(HttpProtocol.NAME)));
                    allowing(httpService).getHandler(); will(returnValue(httpHandler));
                    allowing(httpService).getSessionConfig(); will(returnValue(new DefaultIoSessionConfigEx()));
                    allowing(httpService).getThreadLocalWriteRequest(with(any(int.class))); will(returnValue(new ShareableWriteRequest()));
                    allowing(session).getIoLayer(); will(returnValue(0));
                    allowing(session).getIoThread(); will(returnValue(currentThread()));
                    allowing(session).getIoExecutor(); will(returnValue(IMMEDIATE_EXECUTOR));
                    allowing(session).getConfig(); will(returnValue(new DefaultIoSessionConfigEx()));
                    allowing(session).isClosing(); will(returnValue(false));
                    allowing(session).getFilterChain(); will(returnValue(filterChain));
                    allowing(writeRequest).getMessage(); will(returnValue(bufEx));
                    allowing(writeRequest).getFuture(); will(returnValue(writeFuture));
                    allowing(session).getWrittenBytes(); will(returnValue(0L));

                    allowing(session).resumeRead(); // TODO: eliminate?

                    oneOfSslUtilsIsSecure(this,session);

                    allowing(writeRequestQueue).isEmpty(with(aNonNull(IoSession.class))); will(returnValue(false));
                    oneOf(writeRequestQueue).poll(with(aNonNull(IoSession.class))); will(returnValue(writeRequest));
                    allowing(writeRequestQueue).poll(with(aNonNull(IoSession.class))); will(returnValue(null));
                    oneOf(session).write(with(aNonNull(HttpResponseMessage.class))); will(returnValue(DefaultWriteFuture.newWrittenFuture(session)));
                    oneOf(session).write(with(aNonNull(HttpContentMessage.class))); will(returnValue(DefaultWriteFuture.newWrittenFuture(session)));

                    // the following only occurs when parent write is completed
                    oneOf(writeFuture).setWritten();
                    // we don't send messageSent any more for performance reasons
                    never(handler).messageSent(with(aNonNull(IoSession.class)), with(aNonNull(IoBuffer.class)));
                }
                catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        });

        HttpAcceptProcessor processor = new HttpAcceptProcessor();
        ResourceAddressFactory addressFactory = ResourceAddressFactory.newResourceAddressFactory();
        ResourceOptions options = ResourceOptions.FACTORY.newResourceOptions();
        options.setOption(TRANSPORT_URI, "pipe://test");
        ResourceAddress address = addressFactory.newResourceAddress("http://localhost:8000/");
        ResourceAddress remoteAddress = addressFactory.newResourceAddress("http://localhost:42342/");

        DefaultHttpSession httpSession = new DefaultHttpSession(httpService, processor, address, remoteAddress, session, httpAllocator, new Properties());
        httpSession.setHandler(handler);
        httpSession.setWriteRequestQueue(writeRequestQueue);
        httpSession.commit();
        httpSession.getProcessor().flush(httpSession);

        context.assertIsSatisfied();
    }

    @Test
    public void testWriteMessageNotCompleted() {

        Mockery context = new Mockery();
        context.setThreadingPolicy(new Synchroniser());
        final IoServiceEx httpService = context.mock(IoServiceEx.class);
        final IoHandler httpHandler = context.mock(IoHandler.class, "httpHandler");
        final IoSessionEx session = context.mock(IoSessionEx.class);
        final IoHandler handler = context.mock(IoHandler.class);
        final IoFilterChain filterChain = context.mock(IoFilterChain.class);
        final WriteRequestQueue writeRequestQueue = context.mock(WriteRequestQueue.class);
        final WriteRequest writeRequest = context.mock(WriteRequest.class);
        final WriteFuture writeFuture = context.mock(WriteFuture.class);
        ByteBuffer buf = ByteBuffer.wrap(new byte[] { 0x00 });
        final IoBufferEx bufEx = httpAllocator.wrap(buf).mark();

        context.checking(new Expectations() {
            {
                try {
                    allowing(httpService).getTransportMetadata(); will(returnValue(new DefaultTransportMetadata(HttpProtocol.NAME)));
                    allowing(httpService).getHandler(); will(returnValue(httpHandler));
                    allowing(httpService).getSessionConfig(); will(returnValue(new DefaultIoSessionConfigEx()));
                    allowing(httpService).getThreadLocalWriteRequest(with(any(int.class))); will(returnValue(new ShareableWriteRequest()));
                    allowing(session).getIoLayer(); will(returnValue(0));
                    allowing(session).getIoThread(); will(returnValue(currentThread()));
                    allowing(session).getIoExecutor(); will(returnValue(IMMEDIATE_EXECUTOR));
                    allowing(session).getConfig(); will(returnValue(new DefaultIoSessionConfigEx()));
                    allowing(session).isClosing(); will(returnValue(false));
                    allowing(session).getFilterChain(); will(returnValue(filterChain));
                    allowing(writeRequest).getMessage(); will(returnValue(bufEx));
                    allowing(writeRequest).getFuture(); will(returnValue(writeFuture));
                    allowing(session).getWrittenBytes(); will(returnValue(0L));

                    allowing(session).resumeRead(); // TODO: eliminate?

                    oneOfSslUtilsIsSecure(this,session);

                    allowing(writeRequestQueue).isEmpty(with(aNonNull(IoSession.class))); will(returnValue(false));
                    oneOf(writeRequestQueue).poll(with(aNonNull(IoSession.class))); will(returnValue(writeRequest));
                    allowing(writeRequestQueue).poll(with(aNonNull(IoSession.class))); will(returnValue(null));
                    oneOf(session).write(with(aNonNull(HttpResponseMessage.class))); will(returnValue(DefaultWriteFuture.newWrittenFuture(session)));
                    oneOf(session).write(with(aNonNull(HttpContentMessage.class))); will(returnValue(new DefaultWriteFuture(session)));
                }
                catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        });

        HttpAcceptProcessor processor = new HttpAcceptProcessor();
        ResourceAddressFactory addressFactory = ResourceAddressFactory.newResourceAddressFactory();
        ResourceOptions options = ResourceOptions.FACTORY.newResourceOptions();
        options.setOption(TRANSPORT_URI, "pipe://test");
        ResourceAddress address = addressFactory.newResourceAddress("http://localhost:8000/");
        ResourceAddress remoteAddress = addressFactory.newResourceAddress("http://localhost:42342/");

        DefaultHttpSession httpSession = new DefaultHttpSession(httpService, processor, address, remoteAddress, session, httpAllocator, new Properties());
        httpSession.setHandler(handler);
        httpSession.setWriteRequestQueue(writeRequestQueue);
        httpSession.commit();
        httpSession.getProcessor().flush(httpSession);

        context.assertIsSatisfied();
    }

    @Test
    public void testWriteBufferCompleted() {

        Mockery context = new Mockery();
        context.setThreadingPolicy(new Synchroniser());
        final IoServiceEx httpService = context.mock(IoServiceEx.class);
        final IoHandler httpHandler = context.mock(IoHandler.class, "httpHandler");
        final IoSessionEx session = context.mock(IoSessionEx.class);
        final IoFilterChain filterChain = context.mock(IoFilterChain.class);
        final Entry codecEntry = context.mock(Entry.class);
        final IoHandler handler = context.mock(IoHandler.class);
        final WriteRequestQueue writeRequestQueue = context.mock(WriteRequestQueue.class);
        final WriteRequest writeRequest = context.mock(WriteRequest.class);
        final WriteFuture writeFuture = context.mock(WriteFuture.class);
        final ByteBuffer buf = ByteBuffer.wrap(new byte[] { 0x00 });
        final IoBufferEx bufEx = httpAllocator.wrap(buf).mark();
        final Executor executor = new Executor() {
            @Override
            public void execute(Runnable command) {
                command.run();
            }
        };

        context.checking(new Expectations() {
            {
                try {
                    allowing(httpService).getTransportMetadata(); will(returnValue(new DefaultTransportMetadata(HttpProtocol.NAME)));
                    allowing(httpService).getHandler(); will(returnValue(httpHandler));
                    allowing(httpService).getSessionConfig(); will(returnValue(new DefaultIoSessionConfigEx()));
                    allowing(httpService).getThreadLocalWriteRequest(with(any(int.class))); will(returnValue(new ShareableWriteRequest()));
                    allowing(session).getIoLayer(); will(returnValue(0));
                    allowing(session).getIoThread(); will(returnValue(currentThread()));
                    allowing(session).getIoExecutor(); will(returnValue(IMMEDIATE_EXECUTOR));
                    allowing(session).getConfig(); will(returnValue(new DefaultIoSessionConfigEx()));
                    allowing(session).isClosing(); will(returnValue(false));
                    allowing(session).getFilterChain(); will(returnValue(filterChain));
                    allowing(session).getIoThread(); will(returnValue(Thread.currentThread()));
                    allowing(session).getIoExecutor(); will(returnValue(executor));
                    allowing(filterChain).getEntry(HttpCodecFilter.class); will(returnValue(codecEntry));
                    allowing(filterChain).getEntry(ObjectLoggingFilter.class); will(returnValue(null));
                    allowing(filterChain).clear();
                    allowing(codecEntry).getName();
                    allowing(codecEntry).getFilter();
                    allowing(writeRequest).getMessage(); will(returnValue(bufEx));
                    allowing(writeRequest).getFuture(); will(returnValue(writeFuture));
                    allowing(session).getWrittenBytes(); will(returnValue(0L));
                    allowing(session).getBufferAllocator(); will(returnValue(SimpleBufferAllocator.BUFFER_ALLOCATOR));

                    allowing(session).resumeRead(); // TODO: eliminate?

                    oneOfSslUtilsIsSecure(this,session);

                    allowing(writeRequestQueue).isEmpty(with(aNonNull(IoSession.class))); will(returnValue(false));
                    oneOf(writeRequestQueue).poll(with(aNonNull(IoSession.class))); will(returnValue(writeRequest));
                    allowing(writeRequestQueue).poll(with(aNonNull(IoSession.class))); will(returnValue(null));
                    oneOf(session).write(with(aNonNull(HttpResponseMessage.class))); will(returnValue(DefaultWriteFuture.newWrittenFuture(session)));
                    oneOf(session).write(with(aNonNull(HttpContentMessage.class))); will(returnValue(DefaultWriteFuture.newWrittenFuture(session)));
                    // we don't send messageSent any more for performance reasons
                    never(session).write(buf); will(returnValue(DefaultWriteFuture.newWrittenFuture(session)));

                    // the following only occurs when parent write is completed
                    oneOf(writeFuture).setWritten();
                    // we don't send messageSent any more for performance reasons
                    never(handler).messageSent(with(aNonNull(IoSession.class)), with(aNonNull(IoBuffer.class)));
                }
                catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        });

        HttpAcceptProcessor processor = new HttpAcceptProcessor();
        ResourceAddressFactory addressFactory = ResourceAddressFactory.newResourceAddressFactory();
        ResourceOptions options = ResourceOptions.FACTORY.newResourceOptions();
        options.setOption(TRANSPORT_URI, "pipe://test");
        ResourceAddress address = addressFactory.newResourceAddress("http://localhost:8000/");
        ResourceAddress remoteAddress = addressFactory.newResourceAddress("http://localhost:42342/");
        DefaultHttpSession httpSession = new DefaultHttpSession(httpService, processor, address, remoteAddress, session, httpAllocator, new Properties());
        httpSession.setConnectionClose();
        httpSession.setWriteHeader("Connection", "close");
        httpSession.setHandler(handler);
        httpSession.setWriteRequestQueue(writeRequestQueue);
        httpSession.commit();
        httpSession.getProcessor().flush(httpSession);

        context.assertIsSatisfied();
    }

    @Test
    public void testWriteBufferNotCompleted() {

        Mockery context = new Mockery();
        context.setThreadingPolicy(new Synchroniser());
        final IoServiceEx httpService = context.mock(IoServiceEx.class);
        final IoHandler httpHandler = context.mock(IoHandler.class, "httpHandler");
        final IoSessionEx session = context.mock(IoSessionEx.class);
        final IoHandler handler = context.mock(IoHandler.class);
        final IoFilterChain filterChain = context.mock(IoFilterChain.class);
        final Entry codecEntry = context.mock(Entry.class);
        final IoFilter codec = context.mock(IoFilter.class);
        final WriteRequestQueue writeRequestQueue = context.mock(WriteRequestQueue.class);
        final WriteRequest writeRequest = context.mock(WriteRequest.class);
        final WriteFuture writeFuture = context.mock(WriteFuture.class);
        ByteBuffer buf = ByteBuffer.wrap(new byte[] { 0x00 });
        final IoBufferEx bufEx = httpAllocator.wrap(buf).mark();

        context.checking(new Expectations() {
            {
                try {
                    allowing(httpService).getTransportMetadata(); will(returnValue(new DefaultTransportMetadata(HttpProtocol.NAME)));
                    allowing(httpService).getHandler(); will(returnValue(httpHandler));
                    allowing(httpService).getSessionConfig(); will(returnValue(new DefaultIoSessionConfigEx()));
                    allowing(httpService).getThreadLocalWriteRequest(with(any(int.class))); will(returnValue(new ShareableWriteRequest()));
                    allowing(session).getIoLayer(); will(returnValue(0));
                    allowing(session).getIoThread(); will(returnValue(currentThread()));
                    allowing(session).getIoExecutor(); will(returnValue(IMMEDIATE_EXECUTOR));
                    allowing(session).getConfig(); will(returnValue(new DefaultIoSessionConfigEx()));
                    allowing(session).isClosing(); will(returnValue(false));
                    allowing(session).getFilterChain(); will(returnValue(filterChain));
                    allowing(session).getBufferAllocator(); will(returnValue(SimpleBufferAllocator.BUFFER_ALLOCATOR));
                    allowing(filterChain).getEntry(HttpCodecFilter.class); will(returnValue(codecEntry));
                    allowing(filterChain).getEntry(ObjectLoggingFilter.class); will(returnValue(null));
                    allowing(filterChain).clear();
                    allowing(writeRequest).getMessage(); will(returnValue(bufEx));
                    allowing(writeRequest).getFuture(); will(returnValue(writeFuture));
                    allowing(session).getWrittenBytes(); will(returnValue(0L));
                    allowing(filterChain).getEntry(HttpCodecFilter.class); will(returnValue(codec));
                    allowing(filterChain).getEntry(HttpCodecFilter.class); will(returnValue(null));

                    allowing(session).resumeRead(); // TODO: eliminate?

                    oneOfSslUtilsIsSecure(this,session);

                    allowing(writeRequestQueue).isEmpty(with(aNonNull(IoSession.class))); will(returnValue(false));
                    oneOf(writeRequestQueue).poll(with(aNonNull(IoSession.class))); will(returnValue(writeRequest));
                    allowing(writeRequestQueue).poll(with(aNonNull(IoSession.class))); will(returnValue(null));
                    oneOf(session).write(with(aNonNull(HttpResponseMessage.class))); will(returnValue(DefaultWriteFuture.newWrittenFuture(session)));
                    oneOf(session).write(with(aNonNull(HttpContentMessage.class))); will(returnValue(new DefaultWriteFuture(session)));
//                    oneOf(session).write(with(buf)); will(returnValue(new DefaultWriteFuture(session)));
                }
                catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        });

        HttpAcceptProcessor processor = new HttpAcceptProcessor();
        ResourceAddressFactory addressFactory = ResourceAddressFactory.newResourceAddressFactory();
        ResourceOptions options = ResourceOptions.FACTORY.newResourceOptions();
        options.setOption(TRANSPORT_URI, "pipe://test");
        ResourceAddress address = addressFactory.newResourceAddress("http://localhost:8000/");
        ResourceAddress remoteAddress = addressFactory.newResourceAddress("http://localhost:42342/");
        DefaultHttpSession httpSession = new DefaultHttpSession(httpService, processor, address, remoteAddress, session, httpAllocator, new Properties());
        httpSession.setConnectionClose();
        httpSession.setWriteHeader("Connection", "close");
        httpSession.setHandler(handler);
        httpSession.setWriteRequestQueue(writeRequestQueue);
        httpSession.commit();
        httpSession.getProcessor().flush(httpSession);

        context.assertIsSatisfied();
    }

    @Test
    public void testWriteSharedMessageCompleted() {

        Mockery context = new Mockery();
        context.setThreadingPolicy(new Synchroniser());
        final IoServiceEx httpService = context.mock(IoServiceEx.class);
        final IoHandler httpHandler = context.mock(IoHandler.class, "httpHandler");
        final IoSessionEx session = context.mock(IoSessionEx.class);
        final IoFilterChain filterChain = context.mock(IoFilterChain.class);
        final IoHandler handler = context.mock(IoHandler.class);
        final WriteRequestQueue writeRequestQueue = context.mock(WriteRequestQueue.class);
        final WriteRequest writeRequest = context.mock(WriteRequest.class);
        final WriteFuture writeFuture = context.mock(WriteFuture.class);
        final HttpBuffer buf = httpAllocator.wrap(ByteBuffer.wrap(new byte[] { 0x00 }));
        final HttpContentMessage message = new HttpContentMessage(buf, false);
        buf.mark();
        buf.putMessage("KEY", message);
        final Executor executor = new Executor() {
            @Override
            public void execute(Runnable command) {
                command.run();
            }
        };

        context.checking(new Expectations() {
            {
                try {
                    allowing(httpService).getTransportMetadata(); will(returnValue(new DefaultTransportMetadata(HttpProtocol.NAME)));
                    allowing(httpService).getHandler(); will(returnValue(httpHandler));
                    allowing(httpService).getSessionConfig(); will(returnValue(new DefaultIoSessionConfigEx()));
                    allowing(httpService).getThreadLocalWriteRequest(with(any(int.class))); will(returnValue(new ShareableWriteRequest()));
                    allowing(session).getIoLayer(); will(returnValue(0));
                    allowing(session).getIoThread(); will(returnValue(currentThread()));
                    allowing(session).getIoExecutor(); will(returnValue(IMMEDIATE_EXECUTOR));
                    allowing(session).getConfig(); will(returnValue(new DefaultIoSessionConfigEx()));
                    allowing(session).isClosing(); will(returnValue(false));
                    allowing(session).getFilterChain(); will(returnValue(filterChain));
                    allowing(session).getIoThread(); will(returnValue(Thread.currentThread()));
                    allowing(session).getIoExecutor(); will(returnValue(executor));
                    allowing(writeRequest).getMessage(); will(returnValue(buf));
                    allowing(writeRequest).getFuture(); will(returnValue(writeFuture));
                    allowing(session).getWrittenBytes(); will(returnValue(0L));

                    allowing(session).resumeRead(); // TODO: eliminate?

                    oneOfSslUtilsIsSecure(this,session);

                    allowing(writeRequestQueue).isEmpty(with(aNonNull(IoSession.class))); will(returnValue(false));
                    oneOf(writeRequestQueue).poll(with(aNonNull(IoSession.class))); will(returnValue(writeRequest));
                    allowing(writeRequestQueue).poll(with(aNonNull(IoSession.class))); will(returnValue(null));
                    oneOf(session).write(with(aNonNull(HttpResponseMessage.class))); will(returnValue(DefaultWriteFuture.newWrittenFuture(session)));
                    oneOf(session).write(with(aNonNull(HttpContentMessage.class))); will(returnValue(DefaultWriteFuture.newWrittenFuture(session)));
//                    oneOf(session).write(with(message)); will(returnValue(DefaultWriteFuture.newWrittenFuture(session)));

                    // the following only occurs when parent write is completed
                    oneOf(writeFuture).setWritten();
                    never(handler).messageSent(with(aNonNull(IoSession.class)), with(aNonNull(IoBuffer.class)));
                }
                catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        });

        HttpAcceptProcessor processor = new HttpAcceptProcessor();
        ResourceAddressFactory addressFactory = ResourceAddressFactory.newResourceAddressFactory();
        ResourceOptions options = ResourceOptions.FACTORY.newResourceOptions();
        options.setOption(TRANSPORT_URI, "pipe://test");
        ResourceAddress address = addressFactory.newResourceAddress("http://localhost:8000/");
        ResourceAddress remoteAddress = addressFactory.newResourceAddress("http://localhost:42342/");

        DefaultHttpSession httpSession = new DefaultHttpSession(httpService, processor, address, remoteAddress, session, httpAllocator, new Properties());
        httpSession.setHandler(handler);
        httpSession.setWriteRequestQueue(writeRequestQueue);
        httpSession.commit();
        httpSession.getProcessor().flush(httpSession);

        context.assertIsSatisfied();
    }

    @Test
    public void testWriteSharedMessageNotCompleted() {

        Mockery context = new Mockery();
        context.setThreadingPolicy(new Synchroniser());
        final IoServiceEx httpService = context.mock(IoServiceEx.class);
        final IoHandler httpHandler = context.mock(IoHandler.class, "httpHandler");
        final IoSessionEx session = context.mock(IoSessionEx.class);
        final IoHandler handler = context.mock(IoHandler.class);
        final IoFilterChain filterChain = context.mock(IoFilterChain.class);
        final WriteRequestQueue writeRequestQueue = context.mock(WriteRequestQueue.class);
        final WriteRequest writeRequest = context.mock(WriteRequest.class);
        final WriteFuture writeFuture = context.mock(WriteFuture.class);
        final HttpBuffer buf = httpAllocator.wrap(ByteBuffer.wrap(new byte[] { 0x00 }));
        final HttpContentMessage message = new HttpContentMessage(buf, false);
        buf.mark();
        buf.putMessage("KEY", message);
        final Executor executor = new Executor() {
            @Override
            public void execute(Runnable command) {
                command.run();
            }
        };

        context.checking(new Expectations() {
            {
                try {
                    allowing(httpService).getTransportMetadata(); will(returnValue(new DefaultTransportMetadata(HttpProtocol.NAME)));
                    allowing(httpService).getHandler(); will(returnValue(httpHandler));
                    allowing(httpService).getSessionConfig(); will(returnValue(new DefaultIoSessionConfigEx()));
                    allowing(httpService).getThreadLocalWriteRequest(with(any(int.class))); will(returnValue(new ShareableWriteRequest()));
                    allowing(session).getIoLayer(); will(returnValue(0));
                    allowing(session).getIoThread(); will(returnValue(currentThread()));
                    allowing(session).getIoExecutor(); will(returnValue(IMMEDIATE_EXECUTOR));
                    allowing(session).getConfig(); will(returnValue(new DefaultIoSessionConfigEx()));
                    allowing(session).isClosing(); will(returnValue(false));
                    allowing(session).getFilterChain(); will(returnValue(filterChain));
                    allowing(session).getIoThread(); will(returnValue(Thread.currentThread()));
                    allowing(session).getIoExecutor(); will(returnValue(executor));
                    allowing(writeRequest).getMessage(); will(returnValue(buf));
                    allowing(writeRequest).getFuture(); will(returnValue(writeFuture));
                    allowing(session).getWrittenBytes(); will(returnValue(0L));

                    allowing(session).resumeRead(); // TODO: eliminate?

                    oneOfSslUtilsIsSecure(this,session);

                    allowing(writeRequestQueue).isEmpty(with(aNonNull(IoSession.class))); will(returnValue(false));
                    oneOf(writeRequestQueue).poll(with(aNonNull(IoSession.class))); will(returnValue(writeRequest));
                    allowing(writeRequestQueue).poll(with(aNonNull(IoSession.class))); will(returnValue(null));
                    oneOf(session).write(with(aNonNull(HttpResponseMessage.class))); will(returnValue(DefaultWriteFuture.newWrittenFuture(session)));
                    oneOf(session).write(with(aNonNull(HttpContentMessage.class))); will(returnValue(new DefaultWriteFuture(session)));
//                    oneOf(session).write(with(message)); will(returnValue(new DefaultWriteFuture(session)));
                }
                catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        });

        HttpAcceptProcessor processor = new HttpAcceptProcessor();
        ResourceAddressFactory addressFactory = ResourceAddressFactory.newResourceAddressFactory();
        ResourceOptions options = ResourceOptions.FACTORY.newResourceOptions();
        options.setOption(TRANSPORT_URI, "pipe://test");
        ResourceAddress address = addressFactory.newResourceAddress("http://localhost:8000/");
        ResourceAddress remoteAddress = addressFactory.newResourceAddress("http://localhost:42342/");

        DefaultHttpSession httpSession = new DefaultHttpSession(httpService, processor, address, remoteAddress, session, httpAllocator, new Properties());
        httpSession.setHandler(handler);
        httpSession.setWriteRequestQueue(writeRequestQueue);
        httpSession.commit();
        httpSession.getProcessor().flush(httpSession);

        context.assertIsSatisfied();
    }


    private void oneOfSslUtilsIsSecure(Expectations exp, IoSession session) {
        exp.allowing(session).getAttribute(BridgeSession.LOCAL_ADDRESS);
        exp.will(returnValue(null));

        exp.allowing(session).getLocalAddress();
        exp.will(returnValue(null));
    }

    // TODO: Add more flushNow unit tests
    // Test single session across connection close/gzipped/chunked permutations
    // Test multiple sessions with different encodings for shared zero copy
}

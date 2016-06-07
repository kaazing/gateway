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

import static org.junit.Assert.assertEquals;

import java.nio.channels.SocketChannel;

import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.service.IoProcessor;
import org.apache.mina.core.service.TransportMetadata;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.transport.socket.nio.NioSessionEx;
import org.apache.mina.transport.socket.nio.NioSocketSessionEx;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.Test;

import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.transport.DefaultIoSessionConfigEx;
import org.kaazing.mina.core.service.IoProcessorEx;
import org.kaazing.mina.core.service.IoServiceEx;
import org.kaazing.mina.core.session.IoSessionConfigEx;
import org.kaazing.mina.core.write.DefaultWriteRequestEx.ShareableWriteRequest;

public class SslSessionTest {

    @Test
    @SuppressWarnings("unchecked")
    public void getIdleTimeInMillis_shouldReturnSetValue() {
        Mockery context = new Mockery(){{
            setImposteriser(ClassImposteriser.INSTANCE);
        }};
        final IoServiceEx service = context.mock(IoServiceEx.class);
        final IoHandler handler = context.mock(IoHandler.class);
        final TransportMetadata transportMetadata = context.mock(TransportMetadata.class);
        IoProcessor<NioSessionEx> nioProcessor = context.mock(IoProcessor.class, "nioProcessor");
        IoProcessorEx<SslSession> sslProcessor = context.mock(IoProcessorEx.class, "sslProcessor");
        SocketChannel channel = context.mock(SocketChannel.class);
        ResourceAddress local = context.mock(ResourceAddress.class, "local");
        ResourceAddress remote = context.mock(ResourceAddress.class, "remote");
        
        context.checking(new Expectations() {{
            allowing(service).getHandler(); will(returnValue(handler));
            allowing(service).getThreadLocalWriteRequest(with(any(int.class))); will(returnValue(new ShareableWriteRequest()));
            oneOf(service).getTransportMetadata(); will(returnValue(transportMetadata));
            oneOf(service).getSessionConfig(); will(returnValue(new DefaultIoSessionConfigEx()));
        }});
        
        NioSocketSessionEx parent = new NioSocketSessionEx(service, nioProcessor, channel);
        SslSession session = new SslSession(service, sslProcessor, local, remote, parent);
        IoSessionConfigEx msConfig = session.getConfig();
        msConfig.setIdleTimeInMillis(IdleStatus.READER_IDLE, 123);
        assertEquals(123, msConfig.getReaderIdleTimeInMillis());
        assertEquals(123, msConfig.getIdleTimeInMillis(IdleStatus.READER_IDLE));
        assertEquals(0, msConfig.getReaderIdleTime());
        assertEquals(0, msConfig.getIdleTime(IdleStatus.READER_IDLE));
    }
    
    @Test
    @SuppressWarnings("unchecked")
    public void setIdleTimeInMillis_shouldSetOnParent_if_parent_implements_it() {
        Mockery context = new Mockery(){{
            setImposteriser(ClassImposteriser.INSTANCE);
        }};
        final IoServiceEx service = context.mock(IoServiceEx.class);
        final IoHandler handler = context.mock(IoHandler.class);
        final TransportMetadata transportMetadata = context.mock(TransportMetadata.class);
        IoProcessor<NioSessionEx> nioProcessor = context.mock(IoProcessor.class, "nioProcessor");
        IoProcessorEx<SslSession> sslProcessor = context.mock(IoProcessorEx.class, "sslProcessor");
        SocketChannel channel = context.mock(SocketChannel.class);
        ResourceAddress local = context.mock(ResourceAddress.class, "local");
        ResourceAddress remote = context.mock(ResourceAddress.class, "remote");
        
        context.checking(new Expectations() {{
            allowing(service).getHandler(); will(returnValue(handler));
            allowing(service).getThreadLocalWriteRequest(with(any(int.class))); will(returnValue(new ShareableWriteRequest()));
            oneOf(service).getTransportMetadata(); will(returnValue(transportMetadata));
            oneOf(service).getSessionConfig(); will(returnValue(new DefaultIoSessionConfigEx()));
        }});
        
        NioSocketSessionEx parent = new NioSocketSessionEx(service, nioProcessor, channel);
        SslSession session = new SslSession(service, sslProcessor, local, remote, parent);
        IoSessionConfigEx msConfig = session.getConfig();
        msConfig.setIdleTimeInMillis(IdleStatus.READER_IDLE, 1230);
        
        IoSessionConfigEx msParentConfig = parent.getConfig();
        assertEquals(1230, msParentConfig.getReaderIdleTimeInMillis());
        assertEquals(1230, msParentConfig.getIdleTimeInMillis(IdleStatus.READER_IDLE));
        assertEquals(1, msParentConfig.getReaderIdleTime());
        assertEquals(1, msParentConfig.getIdleTime(IdleStatus.READER_IDLE));
    }
    
}

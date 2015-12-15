/**
 * Copyright 2007-2015, Kaazing Corporation. All rights reserved.
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

package org.kaazing.gateway.transport;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;

import org.apache.mina.core.filterchain.IoFilter.NextFilter;
import org.apache.mina.core.filterchain.IoFilterChain;
import org.apache.mina.core.service.IoAcceptor;
import org.apache.mina.core.service.IoConnector;
import org.apache.mina.core.service.TransportMetadata;
import org.apache.mina.core.session.IoSession;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;

import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.resource.address.ResourceAddressFactories;
import org.kaazing.gateway.resource.address.ResourceAddressFactory;
import org.kaazing.gateway.resource.address.ws.WsResourceAddress;
import org.kaazing.gateway.transport.test.Expectations;
import org.kaazing.gateway.transport.AbstractBridgeAcceptor;
import org.kaazing.gateway.transport.LoggingFilter;
import org.kaazing.gateway.transport.ExceptionLoggingFilter;

public class LoggingFilterTest {
    
    @Rule
    public JUnitRuleMockery context = new  JUnitRuleMockery();
    
    final IoSession session = context.mock(IoSession.class, "session");
    final NextFilter nextFilter = context.mock(NextFilter.class);
    final Logger logger = context.mock(Logger.class);
    
    @Test
    public void shouldLogExceptionAsErrorWithStack() throws Exception {
        LoggingFilter filter = new ExceptionLoggingFilter(logger, "tcp%s");
        final Exception exception = new NullPointerException();
        exception.fillInStackTrace();
        
        context.checking(new Expectations() {
            {
                oneOf(logger).isInfoEnabled(); will(returnValue(true));
                oneOf(session).getId(); will(returnValue(123L));
                oneOf(logger).info(with(stringMatching(".*NullPointerException.*")), with(exception));
                oneOf(nextFilter).exceptionCaught(session, exception);
            }
        });
        
        filter.exceptionCaught(nextFilter, session, exception);
    }
    
    @Test
    public void shouldLogIOExceptionAsInfoWithoutStack() throws Exception {
        LoggingFilter filter = new ExceptionLoggingFilter(logger, "tcp%s");
        final Exception exception = new IOException();
        exception.fillInStackTrace();
        
        context.checking(new Expectations() {
            {
                oneOf(logger).isInfoEnabled(); will(returnValue(true));
                oneOf(session).getId(); will(returnValue(123L));
                oneOf(logger).info(with(stringMatching(".*IOException.*")));
                oneOf(nextFilter).exceptionCaught(session, exception);
            }
        });
        
        filter.exceptionCaught(nextFilter, session, exception);
    }
    
    @Test
    public void getUserIdentifierShouldReturnLocalAddress() throws Exception {
        final TransportMetadata transportMetadata = context.mock(TransportMetadata.class);
        final IoConnector connector = context.mock(IoConnector.class, "connector");
        
        final InetSocketAddress address = new InetSocketAddress(InetAddress.getByName("localhost"), 2121);
        
        context.checking(new Expectations() {
            {
                oneOf(session).getService(); will(returnValue(connector));
                oneOf(session).getLocalAddress(); will(returnValue(address));
                oneOf(session).getTransportMetadata(); will(returnValue(transportMetadata));
                oneOf(transportMetadata).getAddressType(); will(returnValue(Object.class));
                
            }
        });
        assertEquals(address.toString(), LoggingFilter.getUserIdentifier(session));
    }
    
    @Test
    public void getUserIdentifierShouldReturnRemoteIpv4AddressForIoAcceptor() throws Exception {
        final TransportMetadata transportMetadata = context.mock(TransportMetadata.class);
        final IoAcceptor service = context.mock(IoAcceptor.class, "service");

        final InetSocketAddress address = new InetSocketAddress(InetAddress.getByAddress(new byte[]{127,0,0,1}), 2121);
        
        context.checking(new Expectations() {
            {
                oneOf(session).getService(); will(returnValue(service));
                oneOf(session).getRemoteAddress(); will(returnValue(address));
                oneOf(session).getTransportMetadata(); will(returnValue(transportMetadata));
                oneOf(transportMetadata).getAddressType(); will(returnValue(InetSocketAddress.class));
            }
        });
        assertEquals("127.0.0.1:2121", LoggingFilter.getUserIdentifier(session));
    }
    
    @Test
    public void getUserIdentifierShouldReturnIpv6AddressForIoAcceptor() throws Exception {
        final TransportMetadata transportMetadata = context.mock(TransportMetadata.class);
        final IoAcceptor service = context.mock(IoAcceptor.class, "service");

        final InetSocketAddress address = new InetSocketAddress(InetAddress.getByAddress(
                new byte[]{(byte) 0xfe, (byte) 0x80, 0, 0, 0, 0, 0, 0, (byte) 0x90,
                        (byte) 0xea, 0x3e, (byte) 0xe4, 0x77, (byte) 0xad, 0x77, (byte) 0xec}), 2121);

        context.checking(new Expectations() {
            {

                oneOf(session).getService(); will(returnValue(service));
                oneOf(session).getRemoteAddress(); will(returnValue(address));
                oneOf(session).getTransportMetadata(); will(returnValue(transportMetadata));
                oneOf(transportMetadata).getAddressType(); will(returnValue(InetSocketAddress.class));
            }
        });
        assertEquals("fe80:0:0:0:90ea:3ee4:77ad:77ec:2121", LoggingFilter.getUserIdentifier(session));
    }
    
    @Test
    public void getUserIdentifierShouldReturnScopedIpv6AddressForIoAcceptor() throws Exception {
        final TransportMetadata transportMetadata = context.mock(TransportMetadata.class);
        final IoAcceptor service = context.mock(IoAcceptor.class, "service");

        final InetSocketAddress address = new InetSocketAddress(Inet6Address.getByAddress(
                null,
                new byte[]{(byte) 0xfe, (byte) 0x80, 0, 0, 0, 0, 0, 0, (byte) 0x90, (byte) 0xea, 0x3e,
                           (byte) 0xe4, 0x77, (byte) 0xad, 0x77, (byte) 0xec},
                15),
                2121);
        
        context.checking(new Expectations() {
            {
                oneOf(session).getService(); will(returnValue(service));
                oneOf(session).getRemoteAddress(); will(returnValue(address));
                oneOf(session).getTransportMetadata(); will(returnValue(transportMetadata));
                oneOf(transportMetadata).getAddressType(); will(returnValue(InetSocketAddress.class));
            }
        });
        assertEquals("fe80:0:0:0:90ea:3ee4:77ad:77ec%15:2121", LoggingFilter.getUserIdentifier(session));
    }
    
    @Test
    public void getUserIdentifierShouldReturnRemoteAddressForBridgeAcceptor() throws Exception {
        context.setImposteriser(ClassImposteriser.INSTANCE);
        final TransportMetadata transportMetadata = context.mock(TransportMetadata.class);
        final AbstractBridgeAcceptor<?, ?> service = context.mock(AbstractBridgeAcceptor.class, "service");

        final InetSocketAddress address = new InetSocketAddress(InetAddress.getByAddress(new byte[]{127,0,0,1}), 2121);
        
        context.checking(new Expectations() {
            {
                oneOf(session).getService(); will(returnValue(service));
                oneOf(session).getRemoteAddress(); will(returnValue(address));
                oneOf(session).getTransportMetadata(); will(returnValue(transportMetadata));
                oneOf(transportMetadata).getAddressType(); will(returnValue(InetSocketAddress.class));
            }
        });
        assertEquals("127.0.0.1:2121", LoggingFilter.getUserIdentifier(session));
    }
    
    @Test
    public void getUserIdentifierShouldReturnTcpEndpointFromTransport() throws Exception {
        context.setImposteriser(ClassImposteriser.INSTANCE);
        final ResourceAddress address = context.mock(ResourceAddress.class, "address");

        ResourceAddressFactory addressFactory = ResourceAddressFactories.newResourceAddressFactory();
        URI transportURI = URI.create("tcp://localhost:2121");
        final ResourceAddress transport = addressFactory.newResourceAddress(transportURI);

        final TransportMetadata transportMetadata = context.mock(TransportMetadata.class);
        final IoAcceptor service = context.mock(IoAcceptor.class, "service");

        context.checking(new Expectations() {
            {
                oneOf(session).getService(); will(returnValue(service));
                oneOf(session).getRemoteAddress(); will(returnValue(address));
                oneOf(address).getTransport(); will(returnValue(transport));
                oneOf(session).getTransportMetadata(); will(returnValue(transportMetadata));
                oneOf(transportMetadata).getAddressType(); will(returnValue(WsResourceAddress.class));
            }
        });
        assertEquals("localhost:2121", LoggingFilter.getUserIdentifier(session));
    }

    @Test
    public void getUserIdentifierShouldReturnTcpEndpointFromHttpAddress() throws Exception {
        ResourceAddressFactory addressFactory = ResourceAddressFactories.newResourceAddressFactory();
        URI addressURI = URI.create("http://localhost:2121/jms");
        final ResourceAddress address = addressFactory.newResourceAddress(addressURI);

        assertEquals("localhost:2121", LoggingFilter.getUserIdentifier(address));
    }

    @Test
    public void getUserIdentifierShouldReturnTcpEndpointFromWsAddress() throws Exception {
        ResourceAddressFactory addressFactory = ResourceAddressFactories.newResourceAddressFactory();
        URI addressURI = URI.create("ws://localhost:2121/jms");
        final ResourceAddress address = addressFactory.newResourceAddress(addressURI);

        assertEquals("localhost:2121", LoggingFilter.getUserIdentifier(address));
    }

    @Test
    public void shouldAddLoggingFilterWhenUserIdIsScopedIpv6Address() throws Exception {
        final TransportMetadata transportMetadata = context.mock(TransportMetadata.class);
        final IoConnector connector = context.mock(IoConnector.class, "connector");
        final IoFilterChain filterChain = context.mock(IoFilterChain.class, "filterChain");
        
        final InetSocketAddress address = new InetSocketAddress(Inet6Address.getByAddress(
                null,
                new byte[]{(byte) 0xfe, (byte) 0x80, 0, 0, 0, 0, 0, 0, (byte) 0x90, (byte) 0xea, 0x3e,
                           (byte) 0xe4, 0x77, (byte) 0xad, 0x77, (byte) 0xec},
                15),
                2121);

        context.checking(new Expectations() {
            {
                oneOf(logger).isInfoEnabled(); will(returnValue(true));
                oneOf(logger).isTraceEnabled(); will(returnValue(false));
                oneOf(session).getService(); will(returnValue(connector));
                oneOf(session).getLocalAddress(); will(returnValue(address));
                oneOf(session).getTransportMetadata(); will(returnValue(transportMetadata));
                oneOf(transportMetadata).getAddressType(); will(returnValue(Object.class));
                oneOf(session).getFilterChain(); will(returnValue(filterChain));
                oneOf(filterChain).addLast(with("tcp#logging"), with(any(ExceptionLoggingFilter.class)));
            }
        });
        LoggingFilter.addIfNeeded(logger, session, "tcp");
    }
    
}
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
import static org.junit.Assert.assertNull;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;

import org.apache.mina.core.filterchain.IoFilter.NextFilter;
import org.apache.mina.core.service.IoAcceptor;
import org.apache.mina.core.service.TransportMetadata;
import org.apache.mina.core.session.IoSession;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;

import org.kaazing.gateway.transport.test.Expectations;
import org.kaazing.gateway.transport.LoggingFilter;
import org.kaazing.gateway.transport.BridgeSession;
import org.kaazing.gateway.transport.ExceptionLoggingFilter;
import org.kaazing.mina.core.service.IoServiceEx;
import org.kaazing.mina.core.session.IoSessionEx;

public class LoggingFilterTest {

    @Rule
    public JUnitRuleMockery context = new  JUnitRuleMockery();

    final IoSession session = context.mock(IoSession.class, "session");
    final NextFilter nextFilter = context.mock(NextFilter.class);
    final Logger logger = context.mock(Logger.class);

    @Test
    public void shouldLogExceptionWithStack() throws Exception {
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
    public void shouldLogIOExceptionWithoutStack() throws Exception {
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
    public void shouldLogIOExceptionWithCauseMessageWithStack() throws Exception {
        LoggingFilter filter = new ExceptionLoggingFilter(logger, "tcp%s");
        final Exception exception = new IOException("Oops", new Exception("Cause exception"));
        exception.fillInStackTrace();

        context.checking(new Expectations() {
            {
                oneOf(logger).isInfoEnabled(); will(returnValue(true));
                oneOf(session).getId(); will(returnValue(123L));
                oneOf(logger).info(with(stringMatching(".*IOException.*")), with(exception));
                oneOf(nextFilter).exceptionCaught(session, exception);
            }
        });

        filter.exceptionCaught(nextFilter, session, exception);
    }

    @Test
    public void getUserIdentifierShouldReturnNull() throws Exception {
        final TransportMetadata transportMetadata = context.mock(TransportMetadata.class);
        final BridgeSession bridgeSession = context.mock(BridgeSession.class, "bridgeSession");

        context.checking(new Expectations() {
            {
                oneOf(bridgeSession).getTransportMetadata(); will(returnValue(transportMetadata));
                oneOf(transportMetadata).getAddressType(); will(returnValue(Object.class));
                oneOf(bridgeSession).getParent();
            }
        });
        assertNull(LoggingFilter.getUserIdentifier(bridgeSession));
    }

    @Test
    public void getUserIdentifierShouldReturnTcpEndpoint() throws Exception {
        final TransportMetadata transportMetadata = context.mock(TransportMetadata.class);
        final IoAcceptor service = context.mock(IoAcceptor.class, "service");

        final InetSocketAddress remoteAddress = new InetSocketAddress(InetAddress.getByName("localhost"), 2121);

        context.checking(new Expectations() {
            {
                oneOf(session).getService(); will(returnValue(service));
                oneOf(session).getTransportMetadata(); will(returnValue(transportMetadata));
                oneOf(transportMetadata).getAddressType(); will(returnValue(InetSocketAddress.class));
                oneOf(session).getRemoteAddress(); will(returnValue(remoteAddress));
            }
        });
        assertEquals(remoteAddress.toString(), LoggingFilter.getUserIdentifier(session));
    }

    @Test
    public void getUserIdentifierShouldReturnTcpEndpointFromParent() throws Exception {
        final TransportMetadata transportMetadata = context.mock(TransportMetadata.class);
        final BridgeSession bridgeSession = context.mock(BridgeSession.class, "bridgeSession");
        final IoSessionEx parent = context.mock(IoSessionEx.class, "parent");
        final IoServiceEx service = context.mock(IoServiceEx.class, "service");

        final InetSocketAddress localAddress = new InetSocketAddress(InetAddress.getByName("localhost"), 2121);

        context.checking(new Expectations() {
            {
                oneOf(bridgeSession).getTransportMetadata(); will(returnValue(transportMetadata));
                oneOf(transportMetadata).getAddressType(); will(returnValue(Object.class));
                oneOf(bridgeSession).getParent(); will(returnValue(parent));
                oneOf(parent).getService(); will(returnValue(service));
                oneOf(parent).getTransportMetadata(); will(returnValue(transportMetadata));
                oneOf(transportMetadata).getAddressType(); will(returnValue(InetSocketAddress.class));
                oneOf(parent).getLocalAddress(); will(returnValue(localAddress));
            }
        });
        assertEquals(localAddress.toString(), LoggingFilter.getUserIdentifier(bridgeSession));
    }

}
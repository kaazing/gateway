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
package org.kaazing.gateway.transport;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.Inet6Address;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.atomic.AtomicReference;

import javax.security.auth.Subject;

import org.apache.mina.core.filterchain.IoFilter.NextFilter;
import org.apache.mina.core.filterchain.IoFilterChain;
import org.apache.mina.core.session.IoSession;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Rule;
import org.junit.Test;
import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.resource.address.ResourceAddressFactory;
import org.kaazing.gateway.transport.test.Expectations;
import org.kaazing.mina.core.service.IoAcceptorEx;
import org.kaazing.mina.core.service.IoConnectorEx;
import org.kaazing.mina.core.session.IoSessionEx;
import org.slf4j.Logger;

public class LoggingFilterTest {

    @Rule
    public JUnitRuleMockery context = new  JUnitRuleMockery();

    final IoSession session = context.mock(IoSession.class, "session");
    final IoSessionEx sessionEx = context.mock(IoSessionEx.class, "sessionEx");

    final NextFilter nextFilter = context.mock(NextFilter.class);
    final Logger logger = context.mock(Logger.class);

    @Test
    public void addIfNeeded_shouldNotAddIfInfoNotEnabled() throws Exception {
        context.checking(new Expectations() {
            {
                oneOf(logger).isInfoEnabled(); will(returnValue(false));
            }
        });

        assertFalse(LoggingFilter.addIfNeeded(logger, session, "wsn"));
    }

    @Test
    public void addIfNeeded_shouldAddExceptionLoggingFilter() throws Exception {
        final IoFilterChain filterChain = context.mock(IoFilterChain.class, "filterChain");
        final IoAcceptorEx service = context.mock(IoAcceptorEx.class, "service");
        ResourceAddressFactory addressFactory = ResourceAddressFactory.newResourceAddressFactory();
        final ResourceAddress localAddress = addressFactory.newResourceAddress("http://111.122.133.144:2121/jms");
        final ResourceAddress  remoteAddress = addressFactory.newResourceAddress("http://111.122.133.144:41234/jms");
        final Subject subject = new Subject();

        AtomicReference<LoggingFilter> filter = new AtomicReference<LoggingFilter>();

        context.checking(new Expectations() {
            {
                oneOf(logger).isInfoEnabled(); will(returnValue(true));
                oneOf(logger).isTraceEnabled(); will(returnValue(false));
                oneOf(sessionEx).getService(); will(returnValue(service));
                oneOf(sessionEx).getRemoteAddress(); will(returnValue(remoteAddress));
                oneOf(sessionEx).getSubject(); will(returnValue(subject));
                oneOf(sessionEx).getLocalAddress(); will(returnValue(localAddress));
                oneOf(sessionEx).getFilterChain(); will(returnValue(filterChain));
                oneOf(filterChain).addLast(with("http#logging"), with(any(ExceptionLoggingFilter.class)));
                will(saveParameter(filter, 1));
            }
        });

        assertTrue(LoggingFilter.addIfNeeded(logger, sessionEx, "http"));
        assertEquals("http#%s 111.122.133.144:41234", filter.get().getFormat());
    }

    @Test
    public void addIfNeeded_shouldAddObjectLoggingFilter() throws Exception {
        final IoFilterChain filterChain = context.mock(IoFilterChain.class, "filterChain");
        final IoAcceptorEx service = context.mock(IoAcceptorEx.class, "service");
        ResourceAddressFactory addressFactory = ResourceAddressFactory.newResourceAddressFactory();
        final ResourceAddress localAddress = addressFactory.newResourceAddress("http://111.122.133.144:2121/jms");
        final ResourceAddress  remoteAddress = addressFactory.newResourceAddress("http://111.122.133.144:41234/jms");
        final Subject subject = new Subject();

        AtomicReference<LoggingFilter> filter = new AtomicReference<LoggingFilter>();

        context.checking(new Expectations() {
            {
                oneOf(logger).isInfoEnabled(); will(returnValue(true));
                oneOf(logger).isTraceEnabled(); will(returnValue(true));
                oneOf(sessionEx).getService(); will(returnValue(service));
                oneOf(sessionEx).getRemoteAddress(); will(returnValue(remoteAddress));
                oneOf(sessionEx).getSubject(); will(returnValue(subject));
                oneOf(sessionEx).getLocalAddress(); will(returnValue(localAddress));
                oneOf(sessionEx).getFilterChain(); will(returnValue(filterChain));
                oneOf(filterChain).addLast(with("http#logging"), with(any(ObjectLoggingFilter.class)));
                will(saveParameter(filter, 1));
            }
        });

        assertTrue(LoggingFilter.addIfNeeded(logger, sessionEx, "http"));
        assertEquals("http#%s 111.122.133.144:41234", filter.get().getFormat());
    }

    @Test
    public void shouldIncludeIdentityInSessionIdFormat() throws Exception {
        final IoAcceptorEx acceptor = context.mock(IoAcceptorEx.class, "acceptor");
        SocketAddress remoteAddress = new InetSocketAddress("host", 12244);
        SocketAddress localAddress = new InetSocketAddress("host", 8001);

        context.checking(new Expectations() {
            {
                allowing(sessionEx).getService(); will(returnValue(acceptor));
                oneOf(sessionEx).getRemoteAddress(); will(returnValue(remoteAddress));
                oneOf(sessionEx).getLocalAddress(); will(returnValue(localAddress));
            }
        });

        LoggingFilter filter = new ObjectLoggingFilter(logger, sessionEx, "service");
        String format = filter.getFormat();
        assertTrue("Format did not match expected pattern: " + format, format.matches("service#%s host:12244"));
    }

    @Test
    public void shouldLogExceptionWithStack() throws Exception {
        LoggingFilter filter = new ExceptionLoggingFilter(logger, "tcp%s");
        final Exception exception = new NullPointerException();
        exception.fillInStackTrace();

        context.checking(new Expectations() {
            {
                oneOf(logger).isWarnEnabled(); will(returnValue(true));
                oneOf(session).getId(); will(returnValue(123L));
                oneOf(logger).warn(with(stringMatching("\\[tcp123].*NullPointerException.*")), with(exception));
                oneOf(nextFilter).exceptionCaught(session, exception);
            }
        });

        filter.exceptionCaught(nextFilter, session, exception);
    }

    @Test
    public void shouldNotLogIOExceptionWhenLoggerLevelIsCoarserThanInfo() throws Exception {
        LoggingFilter filter = new ExceptionLoggingFilter(logger, "tcp%s");
        final Exception exception = new IOException();
        exception.fillInStackTrace();

        context.checking(new Expectations() {
            {
                oneOf(logger).isInfoEnabled(); will(returnValue(false));
                oneOf(session).getId(); will(returnValue(123L));
                oneOf(nextFilter).exceptionCaught(session, exception);
            }
        });

        filter.exceptionCaught(nextFilter, session, exception);
    }

    @Test
    public void shouldLogIOExceptionWithoutStackWhenThereIsNoCause() throws Exception {
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
    public void shouldLogIOExceptionWithMessageIncludingCauseAndCauseExceptionStack() throws Exception {
        LoggingFilter filter = new ExceptionLoggingFilter(logger, "tcp%s");
        Exception cause = new Exception("Cause exception");
        final Exception exception = new IOException("Oops", cause);
        exception.fillInStackTrace();

        context.checking(new Expectations() {
            {
                oneOf(logger).isInfoEnabled(); will(returnValue(true));
                oneOf(session).getId(); will(returnValue(123L));
                oneOf(logger).info(with(stringMatching(".*IOException.*Oops.*Cause exception")), with(cause));
                oneOf(nextFilter).exceptionCaught(session, exception);
            }
        });

        filter.exceptionCaught(nextFilter, session, exception);
    }

    @Test
    public void shouldAddLoggingFilterWhenUserIdIsScopedIpv6Address() throws Exception {

        final IoConnectorEx  connector = context.mock(IoConnectorEx .class, "connector");
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
                oneOf(sessionEx).getService(); will(returnValue(connector));
                oneOf(sessionEx).getLocalAddress(); will(returnValue(address));
                oneOf(sessionEx).getRemoteAddress(); will(returnValue(address));
                oneOf(sessionEx).getFilterChain(); will(returnValue(filterChain));

                oneOf(filterChain).addLast(with("tcp#logging"), with(any(ExceptionLoggingFilter.class)));
            }
        });
        LoggingFilter.addIfNeeded(logger, sessionEx, "tcp");
    }

}

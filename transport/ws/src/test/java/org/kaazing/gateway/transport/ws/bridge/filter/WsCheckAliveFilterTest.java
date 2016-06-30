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
package org.kaazing.gateway.transport.ws.bridge.filter;

import static org.junit.Assert.assertNotNull;

import java.nio.ByteBuffer;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.mina.core.filterchain.IoFilter;
import org.apache.mina.core.filterchain.IoFilter.NextFilter;
import org.apache.mina.core.filterchain.IoFilterChain;
import org.apache.mina.core.session.IdleStatus;
import org.jmock.Mockery;
import org.junit.Ignore;
import org.junit.Test;
import org.kaazing.gateway.transport.test.Expectations;
import org.kaazing.gateway.transport.ws.WsAcceptor;
import org.kaazing.gateway.transport.ws.WsPingMessage;
import org.kaazing.gateway.transport.ws.WsPongMessage;
import org.kaazing.gateway.util.Utils;
import org.kaazing.mina.core.buffer.SimpleBufferAllocator;
import org.kaazing.mina.core.session.IoSessionConfigEx;
import org.kaazing.mina.core.session.IoSessionEx;
import org.slf4j.Logger;

public class WsCheckAliveFilterTest {
    private WsPingMessage PING = new WsPingMessage();
    private static final String FILTER_NAME = "wsn#checkalive";

    private static final long STANDARD_INACTIVITY_TIMEOUT_MILLIS = Utils.parseTimeInterval("30sec", TimeUnit.MILLISECONDS);

    @Test
    public void validateSystemPropertiesShouldThrowErrorIfObsoleteSystemPropertyIsSet() throws Exception {
        Mockery context = new Mockery();
        final Logger logger = context.mock(Logger.class);

        context.checking(new Expectations() {
            {
                oneOf(logger).error(with(stringMatching(String.format(
                        "System property %s is no longer supported, please use accept-option %s instead in the gateway configuration file",
                        "org.kaazing.gateway.transport.ws.INACTIVITY_TIMEOUT", "ws.inactivity.timeout"))));
            }
        });

        Properties configuration = new Properties();
        configuration.setProperty("org.kaazing.gateway.transport.ws.INACTIVITY_TIMEOUT", "15s");
        Exception caught = null;
        try {
            WsCheckAliveFilter.validateSystemProperties(configuration, logger);
        }
        catch(Exception e) {
            caught = e;
        }
        context.assertIsSatisfied();
        assertNotNull(caught);
    }

    @Test
    public void validateSystemPropertiesShouldNotThrowErrorIfObsoleteSystemPropertyNotSet() throws Exception {
        Mockery context = new Mockery();
        final Logger logger = context.mock(Logger.class);

        context.checking(new Expectations() {
            {
            }
        });

        Properties configuration = new Properties();

        WsCheckAliveFilter.validateSystemProperties(configuration, logger);
        context.assertIsSatisfied();
    }

    @Test
    public void addIfFeatureEnabledShouldDisableWebsocketInactivityTimeoutByDefault() throws Exception {
        Mockery context = new Mockery();
        final Logger logger = context.mock(Logger.class);
        final IoFilterChain filterChain = context.mock(IoFilterChain.class);

        context.checking(new Expectations() {
            {
                oneOf(logger).isDebugEnabled(); will(returnValue(true));
                oneOf(logger).debug(with(stringMatching("WebSocket inactivity timeout is disabled.*")));
            }
        });

        WsCheckAliveFilter.addIfFeatureEnabled(filterChain, FILTER_NAME, 0L, logger);
        context.assertIsSatisfied();
    }

    @Test
    public void addIfFeatureEnabledShouldAcceptZeroTimeoutOptionToDisableFeature() throws Exception {
        Mockery context = new Mockery();
        final Logger logger = context.mock(Logger.class);
        final IoFilterChain filterChain = context.mock(IoFilterChain.class);

        context.checking(new Expectations() {
            {
                oneOf(logger).isDebugEnabled(); will(returnValue(true));
                oneOf(logger).debug(with(stringMatching("WebSocket inactivity timeout is disabled.*")));
            }
        });


        WsCheckAliveFilter.addIfFeatureEnabled(filterChain, FILTER_NAME, 0L, logger);
        context.assertIsSatisfied();
    }

    @Test
    public void moveIfFeatureEnabled() throws Exception {
        Mockery context = new Mockery();
        final Logger logger = context.mock(Logger.class);
        final IoFilterChain filterChain = context.mock(IoFilterChain.class, "filterChain");
        final IoFilterChain toFilterChain = context.mock(IoFilterChain.class, "toFilterChain");

        final WsCheckAliveFilter filter = new WsCheckAliveFilter(STANDARD_INACTIVITY_TIMEOUT_MILLIS, logger);

        context.checking(new Expectations() {
            {
                oneOf(filterChain).remove(FILTER_NAME); will(returnValue(filter));
                oneOf(logger).isDebugEnabled(); will(returnValue(true));
                oneOf(logger).debug(with(any(String.class)));
                oneOf(toFilterChain).addLast(FILTER_NAME, filter);
            }
        });
        WsCheckAliveFilter.moveIfFeatureEnabled(filterChain, toFilterChain, FILTER_NAME, STANDARD_INACTIVITY_TIMEOUT_MILLIS, logger);
    }

    @Test
    public void postAddShouldSchedulePingWithTimeoutEqualsHalfWsIntactivityTimeout() throws Exception {
        Mockery context = new Mockery();
        final Logger logger = context.mock(Logger.class);
        final NextFilter nextFilter = context.mock(NextFilter.class);
        final IoFilterChain filterChain = context.mock(IoFilterChain.class);
        final IoSessionEx session = context.mock(IoSessionEx.class);
        final IoSessionConfigEx sessionConfig = context.mock(IoSessionConfigEx.class);
        final AtomicReference<WsCheckAliveFilter> filterHolder = new AtomicReference<>();

        context.checking(new Expectations() {
            {
                oneOf(logger).isDebugEnabled(); will(returnValue(false));
                allowing(session).getBufferAllocator(); will(returnValue(SimpleBufferAllocator.BUFFER_ALLOCATOR));
                oneOf(logger).isTraceEnabled(); will(returnValue(false));
                allowing(filterChain).getSession(); will(returnValue(session));
                oneOf(filterChain).addLast(with(FILTER_NAME), with(any(IoFilter.class)));
                will(saveParameter(filterHolder, 1));
                allowing(session).getConfig(); will(returnValue(sessionConfig));
                oneOf(sessionConfig).setIdleTimeInMillis(IdleStatus.READER_IDLE, STANDARD_INACTIVITY_TIMEOUT_MILLIS / 2);
            }
        });

        WsCheckAliveFilter.addIfFeatureEnabled(filterChain, FILTER_NAME, STANDARD_INACTIVITY_TIMEOUT_MILLIS, logger);
        WsCheckAliveFilter filter = filterHolder.get();
        filter.onPostAdd(filterChain, FILTER_NAME, nextFilter);
        context.assertIsSatisfied();
    }

    @Test
    public void postRemoveShouldUnsetReadIdleTimeout() throws Exception {
        Mockery context = new Mockery();
        final Logger logger = context.mock(Logger.class);
        final NextFilter nextFilter = context.mock(NextFilter.class);
        final IoFilterChain filterChain = context.mock(IoFilterChain.class);
        final IoSessionEx session = context.mock(IoSessionEx.class);
        final IoSessionConfigEx sessionConfig = context.mock(IoSessionConfigEx.class);

        final WsCheckAliveFilter filter = new WsCheckAliveFilter(STANDARD_INACTIVITY_TIMEOUT_MILLIS, logger);

        context.checking(new Expectations() {
            {
                allowing(session).getConfig(); will(returnValue(sessionConfig));
                oneOf(sessionConfig).setIdleTimeInMillis(IdleStatus.READER_IDLE, 0);
            }
        });
        filter.onPostRemove(filterChain, FILTER_NAME, nextFilter);
    }

    @Test
    @Ignore( "No longer relevant, since compile-time checking now proves we support millisecond idle timeout")
    public void postAddShouldLogErrorIfIoSessionDoesNotSupportMillisecondIdleTimeout() throws Exception {
        Mockery context = new Mockery();
        final Logger logger = context.mock(Logger.class);
        final IoFilterChain filterChain = context.mock(IoFilterChain.class);
        final IoSessionEx session = context.mock(IoSessionEx.class);

        context.checking(new Expectations() {
            {
                oneOf(filterChain).getSession(); will(returnValue(session));
                oneOf(logger).error(with(any(String.class)));
            }
        });

        WsCheckAliveFilter.addIfFeatureEnabled(filterChain, FILTER_NAME, STANDARD_INACTIVITY_TIMEOUT_MILLIS, logger);
        context.assertIsSatisfied();
    }

    @Test
    public void receivePongShouldNotPingImmediatelyIfConfiguredTimeoutIs2sec() throws Exception {
        Mockery context = new Mockery();
        final Logger logger = context.mock(Logger.class);
        final NextFilter nextFilter = context.mock(NextFilter.class);
        final IoSessionEx session = context.mock(IoSessionEx.class);
        final IoSessionConfigEx config = context.mock(IoSessionConfigEx.class);
        final long inactivityTimeout = 2000L;

        context.checking(new Expectations() {
            {
                allowing(logger).isDebugEnabled();
                allowing(logger).isTraceEnabled();
                allowing(session).getBufferAllocator(); will(returnValue(SimpleBufferAllocator.BUFFER_ALLOCATOR));
                oneOf(session).getConfig(); will(returnValue(config));
                oneOf(config).setIdleTimeInMillis(IdleStatus.READER_IDLE, inactivityTimeout / 2);
                allowing(session).setAttribute(with(any(Object.class)), with(any(Long.class)));
            }
        });

        WsCheckAliveFilter filter = new WsCheckAliveFilter(inactivityTimeout, logger);
        filter.flipNextAction();
        filter.pingWritten(System.currentTimeMillis());
        filter.messageReceived(nextFilter, session, new WsPongMessage(SimpleBufferAllocator.BUFFER_ALLOCATOR.wrap(ByteBuffer.allocate(0))));
        context.assertIsSatisfied();
    }

    @Test
    public void receivePongShouldNotPingImmediatelyEvenWithTimeout1ms() throws Exception {
        Mockery context = new Mockery();
        final Logger logger = context.mock(Logger.class);
        final NextFilter nextFilter = context.mock(NextFilter.class);
        final IoSessionEx session = context.mock(IoSessionEx.class);
        final IoSessionConfigEx config = context.mock(IoSessionConfigEx.class);

        context.checking(new Expectations() {
            {
                allowing(logger).isDebugEnabled();
                allowing(logger).isTraceEnabled();
                oneOf(session).getConfig(); will(returnValue(config));
                oneOf(config).setIdleTimeInMillis(IdleStatus.READER_IDLE, 1L);
                allowing(session).setAttribute(with(any(Object.class)), with(any(Long.class)));
            }
        });

        WsCheckAliveFilter filter = new WsCheckAliveFilter(1L, logger);
        filter.flipNextAction();
        long now = System.currentTimeMillis();
        filter.pingWritten(now);
        filter.messageReceived(nextFilter, session, new WsPongMessage(SimpleBufferAllocator.BUFFER_ALLOCATOR.wrap(ByteBuffer.allocate(0))));
        context.assertIsSatisfied();
    }

    @Test
    // If some rogue client sends a PONG randomly without having received a PING
    public void unexpectedPongShouldBeIgnored() throws Exception {
        Mockery context = new Mockery();
        final Logger logger = context.mock(Logger.class);
        final NextFilter nextFilter = context.mock(NextFilter.class);
        final IoSessionEx session = context.mock(IoSessionEx.class);
        final IoSessionConfigEx config = context.mock(IoSessionConfigEx.class);
        final long rtt = 10;
        final long inactivityTimeout = 3000L;

        context.checking(new Expectations() {
            {
                allowing(logger).isDebugEnabled();
                allowing(logger).isTraceEnabled();
                allowing(session).getConfig(); will(returnValue(config));
                oneOf(config).setIdleTimeInMillis(IdleStatus.READER_IDLE, inactivityTimeout / 2);
                allowing(session).setAttribute(with(any(Object.class)), with(any(Long.class)));
            }
        });

        WsCheckAliveFilter filter = new WsCheckAliveFilter(inactivityTimeout, logger);
        filter.flipNextAction();
        filter.pingWritten(System.currentTimeMillis());
        Thread.sleep(rtt);
        filter.messageReceived(nextFilter, session, new WsPongMessage(SimpleBufferAllocator.BUFFER_ALLOCATOR.wrap(ByteBuffer.allocate(0))));
        filter.messageReceived(nextFilter, session, new WsPongMessage(SimpleBufferAllocator.BUFFER_ALLOCATOR.wrap(ByteBuffer.allocate(0))));
        context.assertIsSatisfied();
    }

    @Test
    public void sessionIdleShouldCloseConnectionIfAwaitingPong() throws Exception {
        Mockery context = new Mockery();
        final Logger logger = context.mock(Logger.class);
        final NextFilter nextFilter = context.mock(NextFilter.class);
        final IoSessionEx session = context.mock(IoSessionEx.class);
        final IoFilterChain filterChain = context.mock(IoFilterChain.class);

        context.checking(new Expectations() {
            {
                allowing(logger).info(with(any(String.class)), with(any(IoSessionEx.class)));
                oneOf(logger).isTraceEnabled(); will(returnValue(false));
                oneOf(session).getConfig();
                oneOf(session).getFilterChain(); will(returnValue(filterChain));
                oneOf(filterChain).contains(WsAcceptor.CLOSE_FILTER); will(returnValue(true));
                oneOf(filterChain).remove(WsAcceptor.CLOSE_FILTER);
                oneOf(session).close(true);
                oneOf(nextFilter).sessionIdle(session, IdleStatus.READER_IDLE);
            }
        });

        WsCheckAliveFilter filter = new WsCheckAliveFilter(STANDARD_INACTIVITY_TIMEOUT_MILLIS, logger);
        filter.flipNextAction();
        filter.pingWritten(System.currentTimeMillis());
        filter.sessionIdle(nextFilter, session, IdleStatus.READER_IDLE);
        context.assertIsSatisfied();
    }

    @Test
    public void sessionIdleShouldResetIdleTimeAndSendPingIfNotAwaitingPong() throws Exception {
        Mockery context = new Mockery();
        final Logger logger = context.mock(Logger.class);
        final NextFilter nextFilter = context.mock(NextFilter.class);
        final IoSessionEx session = context.mock(IoSessionEx.class);
        final IoSessionConfigEx config = context.mock(IoSessionConfigEx.class);

        context.checking(new Expectations() {
            {
                allowing(logger).isTraceEnabled(); will(returnValue(false));
                oneOf(session).getConfig(); will(returnValue(config));
                oneOf(config).setIdleTimeInMillis(IdleStatus.READER_IDLE, STANDARD_INACTIVITY_TIMEOUT_MILLIS / 2);
                oneOf(session).write(with(PING));
                oneOf(nextFilter).sessionIdle(session, IdleStatus.READER_IDLE);
            }
        });

        WsCheckAliveFilter filter = new WsCheckAliveFilter(STANDARD_INACTIVITY_TIMEOUT_MILLIS, logger);
        filter.doSessionIdle(nextFilter, session, IdleStatus.READER_IDLE);
        context.assertIsSatisfied();
    }

    @Test
    public void sessionWriterIdleShouldCorrectlyPassItToNextFilter() throws Exception {
        Mockery context = new Mockery();
        final Logger logger = context.mock(Logger.class);
        final NextFilter nextFilter = context.mock(NextFilter.class);
        final IoSessionEx session = context.mock(IoSessionEx.class);

        context.checking(new Expectations() {
            {
                oneOf(nextFilter).sessionIdle(session, IdleStatus.WRITER_IDLE);
            }
        });

        WsCheckAliveFilter filter = new WsCheckAliveFilter(STANDARD_INACTIVITY_TIMEOUT_MILLIS, logger);
        filter.sessionIdle(nextFilter, session, IdleStatus.WRITER_IDLE);
        context.assertIsSatisfied();
    }

}

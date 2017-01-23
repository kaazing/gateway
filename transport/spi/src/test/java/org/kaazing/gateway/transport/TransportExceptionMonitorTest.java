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


import java.net.SocketAddress;
import java.util.Arrays;
import java.util.List;

import javax.security.auth.Subject;

import org.apache.mina.core.service.TransportMetadata;
import org.apache.mina.core.session.IoSession;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.junit.runners.model.Statement;
import org.kaazing.gateway.resource.address.ResourceAddressFactory;
import org.kaazing.gateway.transport.test.Expectations;
import org.kaazing.mina.core.service.IoServiceEx;
import org.kaazing.mina.core.session.IoSessionEx;
import org.kaazing.test.util.MemoryAppender;
import org.kaazing.test.util.MethodExecutionTrace;


public class TransportExceptionMonitorTest
{
    public static long TEST_SESSION_NUMBER = 23;

    @Rule
    public JUnitRuleMockery context = new JUnitRuleMockery();

    private static final String ERROR_MESSAGE = "ERROR";
    private static final String EXCEPTION_MESSAGE = "EXCEPTION";
    private List<String> expectedPatterns;

    public TestRule trace = new MethodExecutionTrace();

    private TestRule checkLogMessageRule = (base, description) -> new Statement() {
        @Override
        public void evaluate() throws Throwable {
            base.evaluate();
            MemoryAppender.assertMessagesLogged(expectedPatterns, null, null, true);
        }
    };

    @Rule
    public TestRule chain = RuleChain.outerRule(trace).around(checkLogMessageRule);

    @Test(expected=Error.class)
    public void exceptionCaughtShouldRethrowError() throws Exception {
        IoSession session = context.mock(IoSession.class);
        new TransportExceptionMonitor().exceptionCaught(new Error(ERROR_MESSAGE), session);
    }

    @Test
    public void shouldLogMessageIncludingSession() throws Exception {
        IoSessionEx session = context.mock(IoSessionEx.class, "session");
        IoServiceEx service = context.mock(IoServiceEx.class, "service");
        TransportMetadata metadata = context.mock(TransportMetadata.class, "metadata");
        ResourceAddressFactory addressFactory = ResourceAddressFactory.newResourceAddressFactory();
        String addressURI = "ws://localhost:2121/jms";
        SocketAddress address = addressFactory.newResourceAddress(addressURI);
        Subject subject = new Subject();

        context.checking(new Expectations() {
            {
                allowing(session).getService(); will(returnValue(service));
                oneOf(session).getId(); will(returnValue(TEST_SESSION_NUMBER));
                oneOf(service).getTransportMetadata(); will(returnValue(metadata));
                oneOf(session).getTransportMetadata(); will(returnValue(metadata));
                allowing(metadata).getName(); will(returnValue("wsn"));
                oneOf(session).getAttribute(with(any(Object.class))); will(returnValue(null));
                oneOf(session).getLocalAddress(); will(returnValue(address));
                oneOf(session).getRemoteAddress(); will(returnValue(address));
                oneOf(session).getSubject(); will(returnValue(subject));
                oneOf(session).setAttribute(with(any(Object.class)), with(any(String.class)));
            }
        });
        new TransportExceptionMonitor().exceptionCaught(new NullPointerException(EXCEPTION_MESSAGE), session);
        expectedPatterns = Arrays.asList("\\[wsn#23 127.0.0.1:2121\\] java.lang.NullPointerException: EXCEPTION");
    }
}

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

import java.util.List;
import java.util.Properties;
import java.util.concurrent.ScheduledExecutorService;

import org.apache.mina.core.filterchain.IoFilter.NextFilter;
import org.apache.mina.core.session.DummySession;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.write.WriteRequest;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.api.Action;
import org.jmock.api.Invocation;
import org.jmock.lib.concurrent.Synchroniser;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.junit.runners.model.Statement;
import org.kaazing.gateway.transport.ws.WsCloseMessage;
import org.kaazing.gateway.transport.ws.WsMessage;
import org.kaazing.gateway.util.ws.WebSocketWireProtocol;
import org.kaazing.mina.core.write.WriteRequestEx;
import org.kaazing.test.util.MemoryAppender;
import org.kaazing.test.util.MethodExecutionTrace;
import org.slf4j.Logger;

public class ExceptionMonitorTest {
    private List<String> expectedPatterns;
    private TestRule checkLogMessageRule = (base, description) -> new Statement() {
        @Override
        public void evaluate() throws Throwable {
            base.evaluate();
            MemoryAppender.assertMessagesLogged(expectedPatterns, null, null, true);
        }
    };
    
    
    @Rule
    public TestRule testExecutionTrace = new MethodExecutionTrace();
    @Rule
    public final TestRule chain = RuleChain.outerRule(testExecutionTrace).around(checkLogMessageRule);


    
    @Test
    public void testExceptionCaughtFormat() throws Exception {
        Mockery context = new Mockery() {{
            setImposteriser(ClassImposteriser.INSTANCE);
        }};
        context.setThreadingPolicy(new Synchroniser());
        final NextFilter nextFilter = context.mock(NextFilter.class);
        final IoSession session = new DummySession();
        final Logger logger = context.mock(Logger.class);
        final ScheduledExecutorService scheduler = context.mock(ScheduledExecutorService.class);
        final WsMessage close = new WsCloseMessage();

        context.checking(new Expectations() {
            {
                allowing(logger).isTraceEnabled(); will(returnValue(false));
                oneOf(nextFilter).filterWrite(with(session), with(writeRequestWithMessage(close)));
                will(new Action() {

                    @Override
                    public void describeTo(Description description) {
                        description.appendText("Invokes setWritten on future which should trigger an Exception.");
                    }

                    @Override
                    public Object invoke(Invocation invocation) throws Throwable {
                        WriteRequestEx writeRequestEx = (WriteRequestEx)invocation.getParameter(1);
                        writeRequestEx.getFuture().setWritten();
                        return null;
                    }
                });
            }

            public Matcher<WriteRequest> writeRequestWithMessage(final Object message) {
                return new BaseMatcher<WriteRequest>() {
                    @Override
                    public boolean matches(Object arg0) {
                        WriteRequest request = (WriteRequest)arg0;
                        return message.equals(request.getMessage());
                    }
                    @Override
                    public void describeTo(Description arg0) {
                        arg0.appendText("write request containing a message equal to " + message);
                    }
                };
            }
        });
        
        Properties configuration = new Properties();
        WsCloseFilter filter = new WsCloseFilter(WebSocketWireProtocol.RFC_6455, configuration, logger, scheduler);
        filter.messageReceived(nextFilter, session, close);
        context.assertIsSatisfied();
        
    }


}

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
package org.kaazing.mina.netty;



import java.util.Arrays;

import org.apache.mina.core.session.IoSession;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.kaazing.mina.util.DefaultExceptionMonitor;
import org.kaazing.test.util.LoggingRule;
import org.kaazing.test.util.MethodExecutionTrace;


public class DefaultExceptionMonitorTest
{
    @Rule
    public JUnitRuleMockery context = new JUnitRuleMockery();

    private static final String ERROR_MESSAGE = "ERROR";
    private static final String EXCEPTION_MESSAGE = "EXCEPTION";

    public TestRule trace = new MethodExecutionTrace();

    private LoggingRule checkLogMessageRule = new LoggingRule();

    @Rule
    public TestRule chain = RuleChain.outerRule(trace).around(checkLogMessageRule);

    @Test(expected=Error.class)
    public void exceptionCaughtShouldRethrowError() throws Exception {
        IoSession session = context.mock(IoSession.class);
        new DefaultExceptionMonitor().exceptionCaught(new Error(ERROR_MESSAGE), session);
    }

    @Test
    public void shouldLogMessageIncludingSession() throws Exception {
        IoSession session = context.mock(IoSession.class, "session");

        new DefaultExceptionMonitor().exceptionCaught(new NullPointerException(EXCEPTION_MESSAGE), session);
        checkLogMessageRule.expectPatterns(Arrays.asList("Unexpected exception in session session"));
    }
}

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
package org.kaazing.test.util;

import static java.util.concurrent.TimeUnit.SECONDS;

import java.lang.reflect.Field;
import java.util.concurrent.TimeUnit;

import org.junit.internal.runners.statements.InvokeMethod;
import org.junit.rules.DisableOnDebug;
import org.junit.rules.MethodRule;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.junit.rules.Timeout;
import org.junit.runner.Description;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;
import org.kaazing.k3po.junit.rules.K3poRule;

public final class ITUtil {

    /**
     * Creates a rule (chain) out of a k3po rule and gateway rule, adding extra rules as follows:<ol>
     * <li> a timeout rule to have tests time out if they run for more than 10 seconds
     * <li> a rule to print console messages at the start and end of each test method and print trace level
     * log messages on test failure.
     * </ol>
     * @param gateway  Rule to start up and shut down the gateway
     * @param robot    Rule to startup and stop k3po
     * @return         A TestRule which should be the only public @Rule in our robot tests
     */
    public static RuleChain createRuleChain(TestRule gateway, K3poRule robot) {
        return createRuleChain(gateway, robot, 10, SECONDS);
    }

    /**
     * Creates a rule (chain) out of a k3po rule and gateway rule, adding extra rules as follows:<ol>
     * <li> a timeout rule
     * <li> a rule to print console messages at the start and end of each test method and print trace level
     * log messages on test failure.
     * </ol>
     * @param gateway  Rule to start up and shut down the gateway (or acceptor or etc)
     * @param robot    Rule to startup and stop k3po
     * @param timeout  The maximum allowed time duration of each test
     * @param timeUnit The unit for the timeout
     * @return         A TestRule which should be the only public @Rule in our robot tests
     */
    public static RuleChain createRuleChain(TestRule gateway, K3poRule robot, long timeout, TimeUnit timeUnit) {
                TestRule trace = new MethodExecutionTrace();
                TestRule timeoutRule = new DisableOnDebug(Timeout.builder().withTimeout(timeout, timeUnit)
                        .withLookingForStuckThread(true).build());
                return RuleChain.outerRule(trace).around(gateway).around(robot).around(timeoutRule);
    }

    /**
     * Creates a rule (chain) out of a gateway or other rule, adding extra rules as follows:<ol>
     * <li> a timeout rule
     * <li> a rule to print console messages at the start and end of each test method and print trace level
     * log messages on test failure.
     * </ol>
     * @param rule    Rule to startup and stop gateway
     * @param timeout  The maximum allowed time duration of each test (including the gateway rule)
     * @param timeUnit The unit for the timeout
     * @return         A TestRule which should be the only public @Rule in our robot tests
     */
    public static RuleChain createRuleChain(TestRule gateway, long timeout, TimeUnit timeUnit) {
        TestRule trace = new MethodExecutionTrace();
        TestRule timeoutRule = new DisableOnDebug(Timeout.builder().withTimeout(timeout, timeUnit)
                .withLookingForStuckThread(true).build());
        return RuleChain.outerRule(trace).around(timeoutRule).around(gateway);
    }

    /**
     * Creates a rule chain containing the following rules:<ol>
     * <li> a timeout rule
     * <li> a rule to print console messages at the start and end of each test method and print trace level
     * log messages on test failure.
     * </ol>
     * @param timeout  The maximum allowed time duration of the test
     * @param timeUnit The unit for the timeout
     * @return
     */
    public static RuleChain createRuleChain(long timeout, TimeUnit timeUnit) {
        TestRule timeoutRule = timeoutRule(timeout, timeUnit);
        TestRule trace = new MethodExecutionTrace();
        return RuleChain.outerRule(trace).around(timeoutRule);
    }

    public static TestRule timeoutRule(long timeout, TimeUnit timeUnit) {
        return new DisableOnDebug(Timeout.builder().withTimeout(timeout, timeUnit)
            .withLookingForStuckThread(true).build());
    }

    public static TestRule toTestRule(MethodRule in) {
        return new TestRule() {

            @Override
            public Statement apply(Statement base, Description description) {
                if (base instanceof InvokeMethod) {
                    return doApplyInvokeMethod(in, base, (InvokeMethod) base);
                }

                return in.apply(base, null, description);
            }

            private Statement doApplyInvokeMethod(
                MethodRule in,
                Statement base,
                InvokeMethod invokeMethod) {
                try {
                    FrameworkMethod frameworkMethod = (FrameworkMethod) FIELD_FRAMEWORK_METHOD.get(invokeMethod);
                    Object target = FIELD_TARGET.get(invokeMethod);

                    return in.apply(base, frameworkMethod, target);
                }
                catch (IllegalArgumentException | IllegalAccessException ex) {
                    throw new RuntimeException(ex);
                }
            }

        };
    }

    private ITUtil() {

    }

    private static final Field FIELD_TARGET;
    private static final Field FIELD_FRAMEWORK_METHOD;

    static {
        try {
            final Field target = InvokeMethod.class.getDeclaredField("target");
            final Field frameworkMethod = InvokeMethod.class.getDeclaredField("testMethod");

            target.setAccessible(true);
            frameworkMethod.setAccessible(true);

            FIELD_TARGET = target;
            FIELD_FRAMEWORK_METHOD = frameworkMethod;
        }
        catch (NoSuchFieldException | SecurityException ex) {
            throw new RuntimeException(ex);
        }
    }
}

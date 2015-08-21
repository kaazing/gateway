/**
 * Copyright (c) 2007-2014, Kaazing Corporation. All rights reserved.
 */

package org.kaazing.test.util;

import static java.util.concurrent.TimeUnit.SECONDS;

import java.util.concurrent.TimeUnit;

import org.junit.rules.DisableOnDebug;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.junit.rules.Timeout;
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
    public static TestRule createRuleChain(TestRule gateway, K3poRule robot) {
        return createRuleChain(robot, 10, SECONDS).around(gateway);
    }

    /**
     * Creates a rule (chain) out of a k3po rule and gateway rule, adding extra rules as follows:<ol>
     * <li> a timeout rule
     * <li> a rule to print console messages at the start and end of each test method and print trace level
     * log messages on test failure.
     * </ol>
     * @param gateway  Rule to start up and shut down the gateway
     * @param robot    Rule to startup and stop k3po
     * @param timeout  The maximum allowed time duration of each test (including Gateway and robot startup and shutdown)
     * @param timeUnit The unit for the timeout
     * @return         A TestRule which should be the only public @Rule in our robot tests
     */
    public static TestRule createRuleChain(TestRule gateway, K3poRule robot, long timeout, TimeUnit timeUnit) {
        return createRuleChain(robot, timeout, timeUnit).around(gateway);
    }

    /**
     * Creates a rule (chain) out of a k3po rule, adding extra rules as follows:<ol>
     * <li> a timeout rule
     * <li> a rule to print console messages at the start and end of each test method and print trace level
     * log messages on test failure.
     * </ol>
     * @param robot    Rule to startup and stop k3po
     * @param timeout  The maximum allowed time duration of each test (including Gateway and robot startup and shutdown)
     * @param timeUnit The unit for the timeout
     * @return         A TestRule which should be the only public @Rule in our robot tests
     */
    public static RuleChain createRuleChain(K3poRule robot, long timeout, TimeUnit timeUnit) {
        TestRule timeoutRule = new DisableOnDebug(new Timeout(timeout, timeUnit));
        TestRule trace = new MethodExecutionTrace();
        return RuleChain.outerRule(trace).around(timeoutRule).around(robot);
    }


    private ITUtil() {

    }

}

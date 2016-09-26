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
package org.kaazing.gateway.service.update.check;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.jmock.lib.legacy.ClassImposteriser.INSTANCE;
import static org.kaazing.test.util.ITUtil.timeoutRule;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.log4j.BasicConfigurator;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.jmock.lib.concurrent.Synchroniser;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.junit.runners.model.Statement;
import org.kaazing.k3po.junit.annotation.Specification;
import org.kaazing.k3po.junit.rules.K3poRule;
import org.kaazing.test.util.ITUtil;
import org.kaazing.test.util.MemoryAppender;
import org.kaazing.test.util.MethodExecutionTrace;

public class UpdateCheckTaskIT {

    private UpdateCheckService updateCheckService;
    private UpdateCheckTask task;

    private K3poRule k3po = new K3poRule();

    private List<String> expectedPatterns = new ArrayList();
    private List<String> forbiddenPatterns = new ArrayList();

    private TestRule checkLogMessageRule = (base, description) -> new Statement() {
        @Override
        public void evaluate() throws Throwable {
            base.evaluate();
            MemoryAppender.assertMessagesLogged(expectedPatterns, forbiddenPatterns, null, true);
        }
    };


    private JUnitRuleMockery context = new JUnitRuleMockery() {
        {
            setImposteriser(INSTANCE);
            setThreadingPolicy(new Synchroniser());
        }
    };

    private TestRule contextRule = ITUtil.toTestRule(context);

    @Rule
    public final TestRule chain = RuleChain.outerRule(new MethodExecutionTrace()).around(checkLogMessageRule).around(contextRule).around(k3po)
            .around(timeoutRule(5, SECONDS));

    @Before
    public void init() {
        BasicConfigurator.configure();
        updateCheckService = context.mock(UpdateCheckService.class);
        task = new UpdateCheckTask(updateCheckService, "http://localhost:8080", "notARealProduct");
    }

    @Specification("testUpdateCheckTask")
    @Test
    public void testRequestInCorrectFormat() throws Exception {
        context.checking(new Expectations() {
            {
                oneOf(updateCheckService).setLatestGatewayVersion(with(equal(new GatewayVersion(6, 6, 6))));
            }
        });
        task.run();
        k3po.finish();
    }

    @Specification("testUpdateCheckTaskWithFailedRequests")
    @Test
    public void testTaskFunctioningEvenAfterFailedRequests() throws Exception {
        Thread t = new Thread(task, "task");
        t.start();
        k3po.finish();
    }

    @Specification("testUpdateCheckTaskWithFailedRequestsResponseCode")
    @Test
    public void testUpdateCheckTaskWithFailedRequestsResponseCode() throws Exception {
        task.run();
        k3po.finish();
    }

    @Specification("testUpdateCheckTaskRCWithCorrectFormat")
    @Test
    public void testRequestInCorrectFormatWithRC() throws Exception {
        context.checking(new Expectations() {
            {
                oneOf(updateCheckService).setLatestGatewayVersion(with(equal(new GatewayVersion(1, 2, 3, "RC999"))));
            }
        });
        task.run();
        k3po.finish();
    }

    @Specification("testUpdateCheckTaskRCWithFailingFormat")
    @Test
    public void testRequestWithRCWithFailingFormat() throws Exception {
        task.run();
        k3po.finish();
        expectedPatterns = new ArrayList<>(Arrays.asList(new String[]{
                "java.lang.IllegalArgumentException: version String is not of form",
        }));
    }

}

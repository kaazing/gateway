/**
 * Copyright (c) 2007-2014 Kaazing Corporation. All rights reserved.
 * 
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.kaazing.gateway.service.update.check;

import static org.jmock.lib.legacy.ClassImposteriser.INSTANCE;

import org.apache.log4j.BasicConfigurator;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.lib.concurrent.Synchroniser;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.kaazing.k3po.junit.annotation.Specification;
import org.kaazing.k3po.junit.rules.K3poRule;

public class UpdateCheckTaskIT {

    private Mockery context;
    private UpdateCheckService updateCheckService;
    private UpdateCheckTask task;

    @Rule
    public K3poRule robot = new K3poRule();

    @Before
    public void init() {
        BasicConfigurator.configure();
        context = new Mockery() {
            {
                setImposteriser(INSTANCE);
                setThreadingPolicy(new Synchroniser());
            }
        };
        updateCheckService = context.mock(UpdateCheckService.class);
        task = new UpdateCheckTask(updateCheckService, "http://localhost:8080", "notARealProduct");
    }

    @Specification("testUpdateCheckTask")
    @Test(timeout = 3000)
    public void testRequestInCorrectFormat() throws Exception {
        context.checking(new Expectations() {
            {
                oneOf(updateCheckService).setLatestGatewayVersion(with(equal(new GatewayVersion(6, 6, 6))));
            }
        });
        task.run();
        robot.finish();
        context.assertIsSatisfied();
    }

    @Specification("testUpdateCheckTaskWithFailedRequests")
    @Test(timeout = 3000)
    public void testTaskFunctioningEvenAfterFailedRequests() throws Exception {
        task.run();
        robot.finish();
        context.assertIsSatisfied();
    }

    @Specification("testUpdateCheckTaskWithFailedRequestsResponseCode")
    @Test(timeout = 3000)
    public void testUpdateCheckTaskWithFailedRequestsResponseCode() throws Exception {
        task.run();
        robot.finish();
        context.assertIsSatisfied();
    }
}

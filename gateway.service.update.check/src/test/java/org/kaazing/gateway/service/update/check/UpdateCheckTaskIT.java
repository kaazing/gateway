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
import org.kaazing.robot.junit.annotation.Robotic;
import org.kaazing.robot.junit.rules.RobotRule;

public class UpdateCheckTaskIT {

    private Mockery context;
    private UpdateCheckService updateCheckService;
    private UpdateCheckTask task;

    @Rule
    public RobotRule robot = new RobotRule();

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

    @Robotic(script = "testUpdateCheckTask")
    @Test(timeout = 3000)
    public void testRequestInCorrectFormat() throws Exception {
        context.checking(new Expectations() {
            {
                oneOf(updateCheckService).setLatestGatewayVersion(with(equal(new GatewayVersion(6, 6, 6))));
            }
        });
        task.run();
        robot.join();
        context.assertIsSatisfied();
    }

    @Robotic(script = "testUpdateCheckTaskWithFailedRequests")
    @Test(timeout = 3000)
    public void testTaskFunctioningEvenAfterFailedRequests() throws Exception {
        task.run();
        robot.join();
        context.assertIsSatisfied();
    }

    @Robotic(script = "testUpdateCheckTaskWithFailedRequestsResponseCode")
    @Test(timeout = 3000)
    public void testUpdateCheckTaskWithFailedRequestsResponseCode() throws Exception {
        task.run();
        robot.join();
        context.assertIsSatisfied();
    }
}

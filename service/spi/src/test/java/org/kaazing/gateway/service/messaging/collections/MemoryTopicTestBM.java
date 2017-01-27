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
package org.kaazing.gateway.service.messaging.collections;


import java.util.concurrent.TimeUnit;

import org.jboss.byteman.contrib.bmunit.BMRule;
import org.jboss.byteman.contrib.bmunit.BMScript;
import org.jboss.byteman.contrib.bmunit.BMUnitConfig;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.kaazing.gateway.service.collections.MemoryCollectionsException;
import org.kaazing.gateway.service.collections.MemoryCollectionsFactory;
import org.kaazing.test.util.ITUtil;

import com.hazelcast.core.ITopic;

/**
 * To run this test:
 *  - in the IDE add ${java.home}/../lib/tools.jar in the JVM's jars.
 *  - with Maven: mvn test -Dtest.byteman.only=true
 *
 * TODO create one script per method and remove the @BMRule definitions
 */
@RunWith(org.jboss.byteman.contrib.bmunit.BMUnitRunner.class)
@BMUnitConfig(loadDirectory = "target/test-classes", debug = true)
@BMScript(value = "memory.topic.test.btm")
public class MemoryTopicTestBM {

    private MemoryCollectionsFactory factory;

    @Rule
    public RuleChain chain = ITUtil.createRuleChain(10, TimeUnit.SECONDS);

    @Before
    public void setUp() throws Exception {
        factory = new MemoryCollectionsFactory();
    }

    @Test(expected = MemoryCollectionsException.class)
    @BMRule(name = "handle interrupted exception",
        targetClass = "java.util.concurrent.FutureTask",
        targetMethod = "get",
        action =
            "traceln(\"Triggering failure\"); " +
            "throw new java.lang.InterruptedException( \"Future interrupted!\" )"
    )
    public void shouldHandleInterruptedException() {
        ITopic<String> topic = factory.getTopic("topic");
        topic.addMessageListener(message -> {});
    }

    @Test(expected = MemoryCollectionsException.class)
    @BMRule(name = "handle execution exception",
        targetClass = "java.util.concurrent.FutureTask",
        targetMethod = "get",
        action =
            "traceln(\"Triggering failure\"); " +
            "throw new java.util.concurrent.ExecutionException(new Exception())"
    )
    public void shouldHandleExecutionException() {
        ITopic<String> topic = factory.getTopic("topic");
        topic.addMessageListener(message -> {});
    }

}

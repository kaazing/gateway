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

import static org.kaazing.test.util.MemoryAppender.printAllMessages;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MemoryAppenderTest {
    @Rule
    public TestRule testExecutionTrace = new MethodExecutionTrace("log4j-trace.properties");

    @Test
    public void testMemoryAppender() {
        Logger logger = LoggerFactory.getLogger(MemoryAppenderTest.class);
        method1(logger);
        method2(logger);
        method3(logger);

        printAllMessages();
    }

    private void method1(Logger logger) {
        logger.trace("Entering method1");
        logger.trace("Exiting method1");
    }

    private void method2(Logger logger) {
        logger.trace("Entering method2");
        logger.trace("Exiting method2");
    }

    private void method3(Logger logger) {
        logger.trace("Entering method3");
        logger.trace("Exiting method3");
    }
}
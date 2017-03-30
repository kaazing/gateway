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

import java.util.List;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

public class LoggingTestRule implements TestRule {
    private List<String> expectedPatterns;
    private List<String> forbiddenPatterns;
    private String filterPattern;
    private long timeOutMilis = 2000; // default value

    private static final long SLEEP_STEP = 50; // how much to sleep between retries

    @Override
    public Statement apply(Statement base, Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                base.evaluate();
                long initialTime = System.currentTimeMillis();
                System.out.println("Stefan - starting");
                do {
                    try {
                        MemoryAppender.assertMessagesLogged(expectedPatterns, forbiddenPatterns, filterPattern, true);
                        return;
                    } catch (AssertionError e) {
                        if (initialTime + timeOutMilis < System.currentTimeMillis()) {
                            throw e;
                        }
                        System.out.println("Stefan - waiting some more");
                        Thread.sleep(SLEEP_STEP);
                    }
                } while (true);
            }
        };
    }

    public void setExpectedPatterns(List<String> expectedPatterns) {
        this.expectedPatterns = expectedPatterns;
    }

    public void setForbiddenPatterns(List<String> forbiddenPatterns) {
        this.forbiddenPatterns = forbiddenPatterns;
    }

    public void setFilterPattern(String filterPattern) {
        this.filterPattern = filterPattern;
    }

}

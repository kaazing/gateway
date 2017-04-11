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
import java.util.concurrent.TimeUnit;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 * LoggingRule should be used to check that certain information is present in the logs. It receives as parameters
 * a filter patters, the patterns that should occur and the patterns that should not occur in the filtered logs.
 *
 * In case a GatewayRule is used, make sure that the GatewayRule is around the LoggingRule, to allow the LoggingRule to wait
 * for the expected log lines before stopping the gateway.
 *
 */
public class LoggingRule implements TestRule {
    private List<String> expectedPatterns;
    private List<String> forbiddenPatterns;
    private String filterPattern;
    private long timeOutMillis = 2000; // default value

    private static final long SLEEP_TIME_MILLIS = 50; // how much to sleep between retries

    @Override
    public Statement apply(Statement base, Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                base.evaluate();
                long initialTime = System.currentTimeMillis();
                do {
                    try {
                        MemoryAppender.assertMessagesLogged(expectedPatterns, forbiddenPatterns, filterPattern, true);
                        return;
                    } catch (AssertionError e) {
                        if (initialTime + timeOutMillis < System.currentTimeMillis()) {
                            throw e;
                        }
                        Thread.sleep(SLEEP_TIME_MILLIS);
                    }
                } while (true);
            }
        };
    }

    public LoggingRule expectPatterns(List<String> expectedPatterns) {
        this.expectedPatterns = expectedPatterns;
        return this;
    }

    public LoggingRule forbidPatterns(List<String> forbiddenPatterns) {
        this.forbiddenPatterns = forbiddenPatterns;
        return this;
    }

    public LoggingRule filterPattern(String filterPattern) {
        this.filterPattern = filterPattern;
        return this;
    }

    public LoggingRule timeOut(long timeOutMillis, TimeUnit timeUnit) {
        this.timeOutMillis = timeUnit.toMillis(timeOutMillis);
        return this;
    }
}

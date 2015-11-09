/**
 * Copyright 2007-2015, Kaazing Corporation. All rights reserved.
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

package org.kaazing.gateway.service.proxy;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.PropertyConfigurator;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.DisableOnDebug;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.junit.rules.Timeout;
import org.kaazing.gateway.resource.address.pipe.NamedPipePathException;
import org.kaazing.gateway.server.test.Gateway;
import org.kaazing.test.util.MemoryAppender;
import org.kaazing.test.util.MethodExecutionTrace;
import org.kaazing.gateway.server.test.config.GatewayConfiguration;
import org.kaazing.gateway.server.test.config.builder.GatewayConfigurationBuilder;

public class NamedPipePathExceptionMessageTest {

    private static final String PIPE_AUTHORITY = "customera";
    private static final String PIPE_PATH = "/app1";
    private static final String SERVICE_NAME_PIPEACCEPTOR = "ServiceWithPipe";
    private static final String ERROR_MESSAGE = "Using pipe://"
            + PIPE_AUTHORITY + ".*pipe://" + PIPE_AUTHORITY + PIPE_PATH + ".*" + SERVICE_NAME_PIPEACCEPTOR;
    private GatewayConfiguration configuration;

    private TestRule timeoutRule = new DisableOnDebug(new Timeout(10, SECONDS));

    @Before
    public void init() {
        configuration =
                new GatewayConfigurationBuilder()
                    .service()
                        .name(SERVICE_NAME_PIPEACCEPTOR)
                        .accept(URI.create("pipe://" + PIPE_AUTHORITY + PIPE_PATH))
                        .connect(URI.create("http://localhost:8080/"))
                        .type("proxy")
                    .done()
                .done();
        // @formatter:on
        Properties log4j = new Properties();
        log4j.setProperty("log4j.rootLogger", "WARN, A1");
        log4j.setProperty("log4j.appender.A1", "org.kaazing.test.util.MemoryAppender");
        log4j.setProperty("log4j.appender.A1.layout", "org.apache.log4j.PatternLayout");
        log4j.setProperty("log4j.appender.A1.layout.ConversionPattern", "%-5p %m%n");
        PropertyConfigurator.configure(log4j);

    }

    @Rule
    public TestRule chain = RuleChain.outerRule(new MethodExecutionTrace()).around(timeoutRule);

    @Ignore(" The test Gateway does not catch the Exceptions thrown by Launcher - init(), no logging is done for this during the test Gateway startup")
    @Test
    public void shouldLogErrorIfPipeUriWithPathIsUsed() throws Exception {
        
        Gateway gateway = new Gateway();
        try {
            gateway.start(configuration);
        } catch (NamedPipePathException ex) {
            List<String> expectedPatterns = new ArrayList<String>(Arrays.asList(new String[]{ ERROR_MESSAGE }));
            List<String> forbiddenPatterns = null;
            MemoryAppender.assertMessagesLogged(expectedPatterns, forbiddenPatterns, null, true);
        } catch (Exception ex) {
            assertEquals("Expected message is different from actual message", ERROR_MESSAGE, ex.getMessage());
        } finally {
            gateway.stop();
        }
        assertTrue("Exception should be thrown, but no exception was thrown", false);
    }

}

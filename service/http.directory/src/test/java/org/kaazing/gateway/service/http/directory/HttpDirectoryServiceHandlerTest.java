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
package org.kaazing.gateway.service.http.directory;

import static org.junit.Assert.fail;

import java.io.File;
import java.net.URI;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.kaazing.gateway.transport.http.HttpAcceptSession;
import org.kaazing.gateway.transport.http.HttpMethod;
import org.kaazing.gateway.transport.http.HttpStatus;
import org.kaazing.test.util.MethodExecutionTrace;

public class HttpDirectoryServiceHandlerTest {
    @Rule
    public TestRule testExecutionTrace = new MethodExecutionTrace();

    private Mockery mockery;
    private HttpDirectoryServiceHandler handler;

    @Before
    public void setup() {
        mockery = new Mockery() {
            {
                setImposteriser(ClassImposteriser.INSTANCE);
            }
        };
        handler = new HttpDirectoryServiceHandler();
        handler.setBaseDir(new File("." + File.pathSeparator)); // current directory
    }

    @Test
    public void test404NoCustomErrorPagesDir() {
        final HttpAcceptSession session = mockery.mock(HttpAcceptSession.class);
        mockery.checking(new Expectations() {
            {
                allowing(session).getMethod();
                will(returnValue(HttpMethod.GET));
                allowing(session).getPathInfo();
                will(returnValue(URI.create("/some/noneexistent/path")));
                oneOf(session).setStatus(HttpStatus.CLIENT_NOT_FOUND);
                oneOf(session).close(false);
            }
        });
        try {
            handler.doSessionOpened(session);
        } catch (Exception e) {
            fail(e.toString());
        }
        mockery.assertIsSatisfied();
    }

    @Test
    public void test404WithCustomErrorPagesDirButNo404File() {
        final File errorPagesDir = mockery.mock(File.class);
        handler.setErrorPagesDir(errorPagesDir);
        final HttpAcceptSession session = mockery.mock(HttpAcceptSession.class);
        mockery.checking(new Expectations() {
            {
                oneOf(errorPagesDir).exists();
                will(returnValue(false));
                allowing(session).getMethod();
                will(returnValue(HttpMethod.GET));
                allowing(session).getPathInfo();
                will(returnValue(URI.create("/some/noneexistent/path")));
                oneOf(session).setStatus(HttpStatus.CLIENT_NOT_FOUND);
                oneOf(session).close(false);
            }
        });
        try {
            handler.doSessionOpened(session);
        } catch (Exception e) {
            fail(e.toString());
        }
        mockery.assertIsSatisfied();
    }
}

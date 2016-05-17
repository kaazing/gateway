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
package org.kaazing.gateway.security.auth;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class DispatchCallbackHandlerTest {

    DispatchCallbackHandler handler;

    @Before
    public void setUp() throws Exception {
        handler = new DispatchCallbackHandler();
    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void testRegister() throws Exception {
        Assert.assertEquals("Expected empty dispatch map.", 0, handler.getDispatchMap().size());
        NameCallback nameCallback = new NameCallback(">|<");
        NameCallbackHandler nameCallbackHandler = new NameCallbackHandler("joe");
        handler.register(NameCallback.class, nameCallbackHandler);
        Assert.assertEquals("Expected a single registered callback handler.", 1, handler.getDispatchMap().size());
    }

    @Test
    public void testUnregister() throws Exception {
        Assert.assertEquals("Expected empty dispatch map.", 0, handler.getDispatchMap().size());
        NameCallback nameCallback = new NameCallback(">|<");
        NameCallbackHandler nameCallbackHandler = new NameCallbackHandler("joe");
        handler.register(NameCallback.class, nameCallbackHandler);
        Assert.assertEquals("Expected a single registered callback handler.", 1, handler.getDispatchMap().size());

        handler.unregister(NameCallback.class);
        Assert.assertEquals("Expected empty dispatch map.", 0, handler.getDispatchMap().size());
    }

    @Test
    public void testHandle() throws Exception {
        Assert.assertEquals("Expected empty dispatch map.", 0, handler.getDispatchMap().size());
        NameCallback nameCallback = new NameCallback(">|<");
        NameCallbackHandler nameCallbackHandler = new NameCallbackHandler("joe");
        handler.register(NameCallback.class, nameCallbackHandler);
        Assert.assertEquals("Expected a single registered callback handler.", 1, handler.getDispatchMap().size());

        handler.handle(new Callback[]{nameCallback});

        Assert.assertEquals("Expected a single registered callback handler.", 1, handler.getDispatchMap().size());
        Assert.assertEquals("Expecting callback to be populated.", "joe", nameCallback.getName());
    }

    @Test(expected = NullPointerException.class)
    public void testNullClassArgumentToRegister() throws Exception {
        Assert.assertEquals("Expected empty dispatch map.", 0, handler.getDispatchMap().size());
        NameCallbackHandler nameCallbackHandler = new NameCallbackHandler("joe");
        handler.register(null, nameCallbackHandler);
    }

    @Test(expected = NullPointerException.class)
    public void testNullHandlerArgumentToRegister() throws Exception {
        Assert.assertEquals("Expected empty dispatch map.", 0, handler.getDispatchMap().size());
        handler.register(NameCallback.class, null);
    }

    @Test(expected = NullPointerException.class)
    public void testNullClassArgumentToUnRegister() throws Exception {
        handler.unregister(null);
    }

    @Test(expected = UnsupportedCallbackException.class)
    public void testShouldThrowExceptionWhenHandleCalledOnEmptyMap() throws Exception {
        Assert.assertEquals("Expected empty dispatch map.", 0, handler.getDispatchMap().size());
        NameCallback nameCallback = new NameCallback(">|<");
        handler.handle(new Callback[]{nameCallback});
    }

    @Test
    public void testShouldQuietlySucceedWhenHandleCalledWIthNullCallbackArray() throws Exception {
        Assert.assertEquals("Expected empty dispatch map.", 0, handler.getDispatchMap().size());
        NameCallback nameCallback = new NameCallback(">|<");
        NameCallbackHandler nameCallbackHandler = new NameCallbackHandler("joe");
        handler.register(NameCallback.class, nameCallbackHandler);
        Assert.assertEquals("Expected a single registered callback handler.", 1, handler.getDispatchMap().size());

        handler.handle(null);
    }

    @Test
    public void testShouldQuietlySucceedWhenHandleCalledWithEmptyCallbackArray() throws Exception {
        Assert.assertEquals("Expected empty dispatch map.", 0, handler.getDispatchMap().size());
        NameCallback nameCallback = new NameCallback(">|<");
        NameCallbackHandler nameCallbackHandler = new NameCallbackHandler("joe");
        handler.register(NameCallback.class, nameCallbackHandler);
        Assert.assertEquals("Expected a single registered callback handler.", 1, handler.getDispatchMap().size());

        handler.handle(new Callback[]{});
    }

    @Test(expected = UnsupportedCallbackException.class)
    public void testShouldThrowExceptionWhenHandleCalledWithNullCallbackInCallbackArray() throws Exception {
        Assert.assertEquals("Expected empty dispatch map.", 0, handler.getDispatchMap().size());
        NameCallback nameCallback = new NameCallback(">|<");
        NameCallbackHandler nameCallbackHandler = new NameCallbackHandler("joe");
        handler.register(NameCallback.class, nameCallbackHandler);
        Assert.assertEquals("Expected a single registered callback handler.", 1, handler.getDispatchMap().size());

        handler.handle(new Callback[]{nameCallback, null});
    }

    @Test(expected = UnsupportedCallbackException.class)
    public void testShouldThrowExceptionWhenHandleCalledWithUnhandledCallbackInCallbackArray() throws Exception {
        Assert.assertEquals("Expected empty dispatch map.", 0, handler.getDispatchMap().size());
        NameCallback nameCallback = new NameCallback(">|<");
        NameCallbackHandler nameCallbackHandler = new NameCallbackHandler("joe");
        handler.register(NameCallback.class, nameCallbackHandler);
        Assert.assertEquals("Expected a single registered callback handler.", 1, handler.getDispatchMap().size());

        PasswordCallback passwordCallback = new PasswordCallback(">|<", false);
        handler.handle(new Callback[]{nameCallback, passwordCallback});
    }

}

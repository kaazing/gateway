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
package org.kaazing.gateway.transport.http.security.auth.challenge;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.resource.address.http.DefaultHttpRealmInfo;
import org.kaazing.gateway.resource.address.http.HttpRealmInfo;
import org.kaazing.gateway.transport.http.bridge.HttpRequestMessage;
import org.kaazing.gateway.transport.http.bridge.HttpResponseMessage;

public class DispatchHttpChallengeFactoryTest {

    private DispatchHttpChallengeFactory factory;
    Mockery context;

    @Before
    public void setUp() throws Exception {
        clear();

        factory = new DispatchHttpChallengeFactory();
        context = new Mockery() {
            {
                setImposteriser(ClassImposteriser.INSTANCE);
            }
        };
    }

    @After
    public void clear() {
        DispatchHttpChallengeFactory.clear();
    }


    @Test
    public void testLookupWithNullAuthSchemeReturnsNull() throws Exception {
        assertNull(factory.lookup(null));
    }

    @Test
    public void testLookupWithApplicationStillFindsChallengeFactory() throws Exception {
        assertNull(factory.lookup("Application Basic"));
        factory.register("Basic", new BasicHttpChallengeFactory());
        assertNotNull(factory.lookup("Application Basic"));
    }


    @Test
    public void testRegister() throws Exception {
        try {
            factory.register(null, null);
            fail("Expected Null Pointer Exception");
        } catch (NullPointerException e) {
            //ok
        }

        try {
            factory.register("Basic", null);
            fail("Expected Null Pointer Exception");
        } catch (NullPointerException e) {
            //ok
        }

        assertEquals(0, DispatchHttpChallengeFactory.challengeFactoriesByAuthScheme.size());
        factory.register("Basic", new BasicHttpChallengeFactory());
        assertEquals(1, DispatchHttpChallengeFactory.challengeFactoriesByAuthScheme.size());
    }

    @Test
    public void testAttemptToCreateBasicChallengeWithNoRegisteredFactories() throws Exception {
        final HttpRequestMessage request = new HttpRequestMessage();
        final ResourceAddress address = context.mock(ResourceAddress.class);
        request.setLocalAddress(address);

        final HttpRealmInfo realm = new DefaultHttpRealmInfo("demo", "Basic", null, new String[]{"foo"},  new String[]{}, new String[]{}, null, null);
        try {
            factory.createChallenge(request, realm);
            fail("Expecting an Illegal State Exception because no factories are registered.");
        } catch (IllegalStateException e) {
            context.assertIsSatisfied();
        }

    }

    @Test
    public void testDispatchToBasicChallengeFactorySuccessfully() throws Exception {
        testDispatchSuccess("Basic");
    }

    @Test
    public void testDispatchToNegotiateChallengeFactorySuccessfully() throws Exception {
        testDispatchSuccess("Negotiate");

    }

    private void testDispatchSuccess(final String authScheme) {
        final HttpRequestMessage request = new HttpRequestMessage();
        final HttpResponseMessage response  = context.mock(HttpResponseMessage.class);
        final ResourceAddress address = context.mock(ResourceAddress.class);
        final NegotiateHttpChallengeFactory basicFactory = context.mock(NegotiateHttpChallengeFactory.class);

        request.setLocalAddress(address);
        factory.register(authScheme, basicFactory);
        final HttpRealmInfo realm = new DefaultHttpRealmInfo("demo", authScheme, null, new String[]{"foo"},  new String[]{}, new String[]{}, null, null);
        context.checking(new Expectations() {
            {
                oneOf(basicFactory).createChallenge(request, realm);
                will(returnValue(response));
            }
        });
        assertSame(response, factory.createChallenge(request, realm));
        context.assertIsSatisfied();
    }
}


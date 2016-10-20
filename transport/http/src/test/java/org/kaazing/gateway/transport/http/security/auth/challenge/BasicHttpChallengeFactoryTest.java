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
import static org.kaazing.gateway.resource.address.ResourceAddress.NEXT_PROTOCOL;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.Before;
import org.junit.Test;
import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.resource.address.http.DefaultHttpRealmInfo;
import org.kaazing.gateway.transport.http.HttpStatus;
import org.kaazing.gateway.transport.http.bridge.HttpRequestMessage;
import org.kaazing.gateway.transport.http.bridge.HttpResponseMessage;

public class BasicHttpChallengeFactoryTest {

    BasicHttpChallengeFactory factory;
    Mockery context;
    @Before
    public void setUp() throws Exception {
        factory = new BasicHttpChallengeFactory();
        context = new Mockery() {
            {
                setImposteriser(ClassImposteriser.INSTANCE);
            }
        };
    }

    @Test
    public void canBuildASimpleChallenge() throws Exception {
        final HttpRequestMessage request = new HttpRequestMessage();
        final ResourceAddress address = context.mock(ResourceAddress.class);
        request.setLocalAddress(address);
        HttpResponseMessage response = factory.createChallenge(request, new DefaultHttpRealmInfo("demo", "Basic", "Realm Description", null, new String[]{},
                null, null, null));
        context.assertIsSatisfied();
        assertEquals(HttpStatus.CLIENT_UNAUTHORIZED, response.getStatus());
        assertEquals("Basic realm=\"Realm Description\"", response.getHeader("WWW-Authenticate"));
    }

    @Test
    public void canBuildASimpleChallengeWithParams() throws Exception {
        final HttpRequestMessage request = new HttpRequestMessage();
        final ResourceAddress address = context.mock(ResourceAddress.class);
        request.setLocalAddress(address);

        Object[] params = new Object[] { "foo=\"bar\"", "baz=\"quxx\"" };
        HttpResponseMessage response = factory.createChallenge(request, new DefaultHttpRealmInfo("demo", "Basic", "Realm Description", null, new String[]{},
                null, null, null), params);
        context.assertIsSatisfied();
        assertEquals(HttpStatus.CLIENT_UNAUTHORIZED, response.getStatus());
        String expected = "Basic realm=\"Realm Description\" foo=\"bar\" baz=\"quxx\"";
        assertEquals(expected, response.getHeader("WWW-Authenticate"));
    }

    @Test
    public void canBuildASimpleChallengeWithNullParams() throws Exception {
        final HttpRequestMessage request = new HttpRequestMessage();
        final ResourceAddress address = context.mock(ResourceAddress.class);
        request.setLocalAddress(address);

        Object[] params = null;
        HttpResponseMessage response = factory.createChallenge(request, new DefaultHttpRealmInfo("demo", "Basic", "Realm Description", null, new String[]{},
                null, null, null), params);
        context.assertIsSatisfied();
        assertEquals(HttpStatus.CLIENT_UNAUTHORIZED, response.getStatus());
        String expected = "Basic realm=\"Realm Description\"";
        assertEquals(expected, response.getHeader("WWW-Authenticate"));
    }

    @Test
    public void canBuildAnApplicationChallenge() throws Exception {
        final HttpRequestMessage request = new HttpRequestMessage();
        final ResourceAddress address = context.mock(ResourceAddress.class);
        final ResourceAddress transportAddress = context.mock(ResourceAddress.class, "transportAddress");

        request.setLocalAddress(address);
        context.checking(new Expectations() {
            {

                allowing(address).getOption(NEXT_PROTOCOL);
                will(returnValue("ws/rfc7455"));

                allowing(address).getTransport();
                will(returnValue(transportAddress));

                allowing(transportAddress).getOption(NEXT_PROTOCOL);
                will(returnValue("http/1.1"));

            }
        });
        HttpResponseMessage response = factory.createChallenge(request, new DefaultHttpRealmInfo("demo", "Application Basic", "Realm Description", null, new String[]{},
                null, null, null));
        context.assertIsSatisfied();
        assertEquals(HttpStatus.CLIENT_UNAUTHORIZED, response.getStatus());
        assertEquals("Application Basic realm=\"Realm Description\"", response.getHeader("WWW-Authenticate"));
    }

    @Test
    public void canBuildAnApplicationChallengeWithParams() throws Exception {
        final HttpRequestMessage request = new HttpRequestMessage();
        final ResourceAddress address = context.mock(ResourceAddress.class);
        final ResourceAddress transportAddress = context.mock(ResourceAddress.class, "transportAddress");

        request.setLocalAddress(address);
        context.checking(new Expectations() {
            {

                allowing(address).getOption(NEXT_PROTOCOL);
                will(returnValue("ws/rfc7455"));

                allowing(address).getTransport();
                will(returnValue(transportAddress));

                allowing(transportAddress).getOption(NEXT_PROTOCOL);
                will(returnValue("http/1.1"));

            }
        });

        Object[] params = new Object[] { "foo=\"bar\"", "baz=\"quxx\"" };
        HttpResponseMessage response = factory.createChallenge(request, new DefaultHttpRealmInfo("demo", "Application Basic", "Realm Description", null, new String[]{},
                null, null, null), params);
        context.assertIsSatisfied();
        assertEquals(HttpStatus.CLIENT_UNAUTHORIZED, response.getStatus());
        String expected = "Application Basic realm=\"Realm Description\" foo=\"bar\" baz=\"quxx\"";
        assertEquals(expected, response.getHeader("WWW-Authenticate"));
    }

    @Test
    public void canBuildAnApplicationChallengeWithNullParams() throws Exception {
        final HttpRequestMessage request = new HttpRequestMessage();
        final ResourceAddress address = context.mock(ResourceAddress.class);
        final ResourceAddress transportAddress = context.mock(ResourceAddress.class, "transportAddress");

        request.setLocalAddress(address);
        context.checking(new Expectations() {
            {

                allowing(address).getOption(NEXT_PROTOCOL);
                will(returnValue("ws/rfc7455"));

                allowing(address).getTransport();
                will(returnValue(transportAddress));

                allowing(transportAddress).getOption(NEXT_PROTOCOL);
                will(returnValue("http/1.1"));

            }
        });

        Object[] params = null;
        HttpResponseMessage response = factory.createChallenge(request, new DefaultHttpRealmInfo("demo", "Application Basic", "Realm Description", null, new String[]{},
                null, null, null), params);
        context.assertIsSatisfied();
        assertEquals(HttpStatus.CLIENT_UNAUTHORIZED, response.getStatus());
        String expected = "Application Basic realm=\"Realm Description\"";
        assertEquals(expected, response.getHeader("WWW-Authenticate"));
    }

    @Test
    public void canBuildAChallengeWhenAuthTypeIsNull() throws Exception {
        final HttpRequestMessage request = new HttpRequestMessage();
        final ResourceAddress address = context.mock(ResourceAddress.class);
        request.setLocalAddress(address);

        HttpResponseMessage response = factory.createChallenge(request, new DefaultHttpRealmInfo("demo", null, "Realm Description", null, new String[]{},
                null, null, null));
        context.assertIsSatisfied();
        assertEquals(HttpStatus.CLIENT_UNAUTHORIZED, response.getStatus());
        assertEquals("Basic realm=\"Realm Description\"", response.getHeader("WWW-Authenticate"));
    }
}


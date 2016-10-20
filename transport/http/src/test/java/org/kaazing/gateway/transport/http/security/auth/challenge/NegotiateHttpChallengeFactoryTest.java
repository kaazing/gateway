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

import java.net.URI;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.Before;
import org.junit.Test;
import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.resource.address.http.DefaultHttpRealmInfo;
import org.kaazing.gateway.resource.address.http.HttpResourceAddress;
import org.kaazing.gateway.transport.http.HttpStatus;
import org.kaazing.gateway.transport.http.bridge.HttpRequestMessage;
import org.kaazing.gateway.transport.http.bridge.HttpResponseMessage;

@SuppressWarnings({"NullableProblems"})
public class NegotiateHttpChallengeFactoryTest {
    public static final String AUTH_CONNECT = "http://kerberos.kaazing.com";
    public static final String AUTH_IDENTIFIER = "ServiceName";
    NegotiateHttpChallengeFactory factory;
    Mockery context;
    @Before
    public void setUp() throws Exception {
        factory = new NegotiateHttpChallengeFactory();
        initMockContext();
    }

    private void initMockContext() {
        context = new Mockery() {
            {
                setImposteriser(ClassImposteriser.INSTANCE);
            }
        };
    }

    @Test
    public void canBuildASimpleChallenge() throws Exception {
        testNegotiateChallenge("Negotiate", null, null, "Negotiate");
        testNegotiateChallenge("Negotiate", AUTH_CONNECT, null, "Negotiate");
        testNegotiateChallenge("Negotiate", AUTH_CONNECT, AUTH_IDENTIFIER, "Negotiate");
    }

    @Test
    public void canBuildAnApplicationChallenge() throws Exception {
        testNegotiateChallenge("Application Negotiate", null, null, "Application Negotiate");
        testNegotiateChallenge("Application Negotiate", AUTH_CONNECT, null, "Application Negotiate "+AUTH_CONNECT);
        testNegotiateChallenge("Application Negotiate", AUTH_CONNECT, AUTH_IDENTIFIER, "Application Negotiate "+AUTH_CONNECT+" "+AUTH_IDENTIFIER);
    }

    private void testNegotiateChallenge(final String authenticationScheme, final String authConnect,
                                        final String authIdentifier, final String expected) {
        initMockContext();
        HttpRequestMessage request = new HttpRequestMessage();
        final ResourceAddress address = context.mock(ResourceAddress.class);
        request.setLocalAddress(address);

        context.checking(new Expectations() {
            {

                allowing(address).getOption(HttpResourceAddress.AUTHENTICATION_CONNECT);
                will(returnValue(authConnect == null ? null : URI.create(authConnect).toASCIIString()));
                allowing(address).getOption(HttpResourceAddress.AUTHENTICATION_IDENTIFIER);
                will(returnValue(authIdentifier));
            }
        });
        HttpResponseMessage response = factory.createChallenge(request, new DefaultHttpRealmInfo("demo", authenticationScheme, "Realm Description", null, new String[]{},
                null, null, null));
        context.assertIsSatisfied();
        
        assertEquals(HttpStatus.CLIENT_UNAUTHORIZED, response.getStatus());
        assertEquals(expected, response.getHeader("WWW-Authenticate"));
    }
}


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
package org.kaazing.gateway.transport.http.security.auth.token;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jmock.Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.Test;
import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.resource.address.http.DefaultHttpRealmInfo;
import org.kaazing.gateway.resource.address.http.HttpResourceAddress;
import org.kaazing.gateway.security.auth.token.DefaultAuthenticationToken;
import org.kaazing.gateway.transport.http.bridge.HttpRequestMessage;
import org.kaazing.gateway.transport.test.Expectations;

public class CustomAuthenticationTokenHeadersTest {


    AuthenticationTokenExtractor extractor = DefaultAuthenticationTokenExtractor.INSTANCE;

    @Test
    public void testAttemptToExtractHeaderButHeaderMissing() throws Exception {
        Mockery context = new Mockery();
        context.setImposteriser(ClassImposteriser.INSTANCE);
        final ResourceAddress address = context.mock(ResourceAddress.class);
        context.checking(getExpectations(address));

        HttpRequestMessage requestMessage = new HttpRequestMessage();
        requestMessage.setLocalAddress(address);
        DefaultAuthenticationToken token = (DefaultAuthenticationToken) extractor.extract(requestMessage,
                new DefaultHttpRealmInfo(null, "Basic", null, null, new String[]{}, new String[]{}, null, null));
        assertEquals("Expecting empty token", 0, token.size());

        context.assertIsSatisfied();
    }

    private Expectations getExpectations(final ResourceAddress address) {
        return new Expectations() {{
            allowing(address).getOption(HttpResourceAddress.REALMS);
            will(returnValue(null));
        }
        };
    }

    @Test
    public void testAttemptToExtractHeaderWithSingleValue() throws Exception {
        Mockery context = new Mockery();
        context.setImposteriser(ClassImposteriser.INSTANCE);
        final ResourceAddress address = context.mock(ResourceAddress.class);
        context.checking(getExpectations(address));


        HttpRequestMessage requestMessage = new HttpRequestMessage();
        requestMessage.setLocalAddress(address);

        requestMessage.setHeader("foo", "bar");
        DefaultAuthenticationToken token = (DefaultAuthenticationToken) extractor.extract(requestMessage,  new DefaultHttpRealmInfo(null, "Basic", null, new String[]{"foo"},  new String[]{}, new String[]{}, null, null));
        assertEquals("Expecting single sized token", 1, token.size());
        assertEquals("Expecting value 'bar'", "bar", token.get());

        context.assertIsSatisfied();
    }

    @Test
    public void testAttemptToExtractHeaderWithMultipleValues() throws Exception {
        Mockery context = new Mockery();
        context.setImposteriser(ClassImposteriser.INSTANCE);
        final ResourceAddress address = context.mock(ResourceAddress.class);
        context.checking(getExpectations(address));

        HttpRequestMessage requestMessage = new HttpRequestMessage();
        requestMessage.setLocalAddress(address);
        Map<String, List<String>> newHeaders = new HashMap<>();
        List<String> newHeaderValues = new ArrayList<>();
        newHeaderValues.add("bar");
        newHeaderValues.add("baz");
        newHeaders.put("foo", newHeaderValues);
        requestMessage.setHeaders(newHeaders);
        DefaultAuthenticationToken token = (DefaultAuthenticationToken) extractor.extract(requestMessage,  new DefaultHttpRealmInfo(null, "Basic", null, new String[]{"foo"},  new String[]{}, new String[]{}, null, null));
        assertEquals("Expecting single sized token", 1, token.size());
        assertEquals("Expecting value 'bar'", "bar", token.get());

        context.assertIsSatisfied();
    }


}

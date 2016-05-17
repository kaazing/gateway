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
package org.kaazing.gateway.transport.http.bridge.filter;

import static org.kaazing.gateway.util.Utils.asByteBuffer;

import java.net.URI;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.mina.core.filterchain.IoFilter.NextFilter;
import org.apache.mina.core.filterchain.IoFilterChain;
import org.jmock.lib.concurrent.Synchroniser;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.Test;
import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.resource.address.http.HttpResourceAddress;
import org.kaazing.gateway.service.ServiceProperties;
import org.kaazing.gateway.transport.http.DefaultHttpCookie;
import org.kaazing.gateway.transport.http.HttpStatus;
import org.kaazing.gateway.transport.http.HttpVersion;
import org.kaazing.gateway.transport.http.bridge.HttpContentMessage;
import org.kaazing.gateway.transport.http.bridge.HttpRequestMessage;
import org.kaazing.gateway.transport.http.bridge.HttpResponseMessage;
import org.kaazing.gateway.transport.http.bridge.filter.HttpOperationFilter.HttpGetCookiesOperation;
import org.kaazing.gateway.transport.http.bridge.filter.HttpOperationFilter.HttpSetCookiesOperation;
import org.kaazing.gateway.transport.test.Expectations;
import org.kaazing.gateway.util.Utils;
import org.kaazing.mina.core.buffer.IoBufferAllocatorEx;
import org.kaazing.mina.core.session.DummySessionEx;
import org.kaazing.mina.core.session.IoSessionEx;
import org.kaazing.test.util.Mockery;

public class HttpOperationFilterTest {

    @Test
    public void testInjectSetCookies() throws Exception {
        Mockery context = new Mockery();
        context.setThreadingPolicy(new Synchroniser());

        final NextFilter nextFilter = context.mock(NextFilter.class);
        final IoSessionEx session = context.mock(IoSessionEx.class);
        final IoFilterChain filterChain = context.mock(IoFilterChain.class);
        final IoFilterChain.Entry entry = context.mock(IoFilterChain.Entry.class);

        context.checking(new Expectations() {
            {

                allowing(session).getFilterChain(); will(returnValue(filterChain));
                allowing(filterChain).getEntry(with(aNonNull(HttpOperationFilter.class))); will(returnValue(entry));
                allowing(entry).getName(); will(returnValue("operation"));
                oneOf(entry).addAfter(with(aNonNull(String.class)), with(aNonNull(HttpSetCookiesOperation.class)));
                oneOf(nextFilter).messageReceived(with(session), with(aNonNull(HttpRequestMessage.class)));
            }
        });

        HttpOperationFilter filter = new HttpOperationFilter();
        HttpRequestMessage message = new HttpRequestMessage();
        message.setRequestURI(URI.create("/path/;api/set-cookies"));
        filter.messageReceived(nextFilter, session, message);
        context.assertIsSatisfied();
    }

    @Test
    public void testInjectGetCookies() throws Exception {
        Mockery context = new Mockery();
        context.setThreadingPolicy(new Synchroniser());

        final NextFilter nextFilter = context.mock(NextFilter.class);
        final IoSessionEx session = context.mock(IoSessionEx.class);
        final IoFilterChain filterChain = context.mock(IoFilterChain.class);
        final IoFilterChain.Entry entry = context.mock(IoFilterChain.Entry.class);

        context.checking(new Expectations() {
            {
                allowing(session).getFilterChain(); will(returnValue(filterChain));
                allowing(filterChain).getEntry(with(aNonNull(HttpOperationFilter.class))); will(returnValue(entry));
                allowing(entry).getName(); will(returnValue("operation"));
                oneOf(entry).addAfter(with(aNonNull(String.class)), with(aNonNull(HttpGetCookiesOperation.class)));
                oneOf(nextFilter).messageReceived(with(session), with(aNonNull(HttpRequestMessage.class)));
            }
        });

        HttpOperationFilter filter = new HttpOperationFilter();
        HttpRequestMessage message = new HttpRequestMessage();
        message.setRequestURI(URI.create("/path/;api/get-cookies"));
        filter.messageReceived(nextFilter, session, message);
        context.assertIsSatisfied();
    }

    @Test
    public void testSetCookies() throws Exception {
        Mockery context = new Mockery();
        context.setThreadingPolicy(new Synchroniser());

        final NextFilter nextFilter = context.mock(NextFilter.class);
        final IoSessionEx session = new DummySessionEx();
        IoBufferAllocatorEx<?> allocator = session.getBufferAllocator();

        final HttpResponseMessage response = new HttpResponseMessage();
        response.setVersion(HttpVersion.HTTP_1_1);
        response.setStatus(HttpStatus.SUCCESS_OK);
        response.setHeader("Set-Cookie", "COOKIE=value;");

        context.checking(new Expectations() {
            {
                oneOf(nextFilter).filterWrite(with(session), with(hasMessage(response)));
            }
        });

        final HttpRequestMessage message = new HttpRequestMessage();
        message.setHeader("Content-Type", "text/plain");
        message.setRequestURI(URI.create("/path/;api/set-cookies"));
        ByteBuffer buf = Utils.asByteBuffer("COOKIE=value;");
        message.setContent(new HttpContentMessage(allocator.wrap(buf), true));
        HttpSetCookiesOperation filter = new HttpSetCookiesOperation();
        session.getFilterChain().addLast("operation", filter);
        filter.messageReceived(nextFilter, session, message);
        context.assertIsSatisfied();
    }

    @Test
    public void testDeleteCookies() throws Exception {
        Mockery context = new Mockery() {{ setImposteriser(ClassImposteriser.INSTANCE);}};
        context.setThreadingPolicy(new Synchroniser());

        final NextFilter nextFilter = context.mock(NextFilter.class);
        final ResourceAddress address = context.mock(ResourceAddress.class);
        final IoSessionEx session = new DummySessionEx();
        IoBufferAllocatorEx<?> allocator = session.getBufferAllocator();

        final HttpRequestMessage message = new HttpRequestMessage();
        message.setHeader("Content-Type", "text/plain");
        message.setHeader("Host", "foo");
        message.setRequestURI(URI.create("http://foo/path/;api/set-cookies"));
        ByteBuffer buf = Utils.asByteBuffer("KSSOID=value");
        message.setContent(new HttpContentMessage(allocator.wrap(buf), true));
        message.setLocalAddress(address);

        HttpOperationFilter.HttpDeleteCookiesOperation filter = new HttpOperationFilter.HttpDeleteCookiesOperation();


        final HttpResponseMessage response = new HttpResponseMessage();
        response.setVersion(HttpVersion.HTTP_1_1);
        response.setStatus(HttpStatus.SUCCESS_OK);
        response.setHeader("Set-Cookie", "KSSOID=value; Domain=.cookie.domain");

        context.checking(new Expectations() {
            {
                allowing(address).getOption(HttpResourceAddress.SERVICE_DOMAIN);
                will(returnValue(".cookie.domain"));

                oneOf(nextFilter).filterWrite(with(session), with(hasMessage(response)));
            }
        });

        ServiceProperties properties = new ServiceProperties() {
            private Map<String, String> properties = new HashMap<>();

            @Override
            public String get(String key) {
                return properties.get(key);
            }

            @Override
            public List<ServiceProperties> getNested(String arg0) {
                return null;
            }

            @Override
            public List<ServiceProperties> getNested(String arg0, boolean arg1) {
                return null;
            }

            @Override
            public boolean isEmpty() {
                return properties.isEmpty();
            }

            @Override
            public Iterable<String> nestedPropertyNames() {
                return null;
            }

            @Override
            public void put(String key, String value) {
                properties.put(key, value);
            }

            @Override
            public Iterable<String> simplePropertyNames() {
                return properties.keySet();
            }

        };
        properties.put("service.domain", ".cookie.domain");

        session.getFilterChain().addLast("operation", filter);
        filter.messageReceived(nextFilter, session, message);
        context.assertIsSatisfied();
    }

    @Test
    public void testSetCookiesWithFragmentation() throws Exception {
        Mockery context = new Mockery();
        context.setThreadingPolicy(new Synchroniser());

        final NextFilter nextFilter = context.mock(NextFilter.class);
        final IoSessionEx session = new DummySessionEx();
        IoBufferAllocatorEx<?> allocator = session.getBufferAllocator();

        final HttpResponseMessage response = new HttpResponseMessage();
        response.setVersion(HttpVersion.HTTP_1_1);
        response.setStatus(HttpStatus.SUCCESS_OK);
        response.setHeader("Set-Cookie", "COOKIE=value;");

        context.checking(new Expectations() {
            {
                oneOf(nextFilter).filterWrite(with(session), with(hasMessage(response)));
            }
        });

        final HttpRequestMessage request = new HttpRequestMessage();
        request.setHeader("Content-Type", "text/plain");
        request.setRequestURI(URI.create("/path/;api/set-cookies"));
        ByteBuffer buf0 = asByteBuffer("COOKIE=");
        request.setContent(new HttpContentMessage(allocator.wrap(buf0), false));
        ByteBuffer buf1 = asByteBuffer("value;");
        HttpContentMessage content = new HttpContentMessage(allocator.wrap(buf1), true);
        HttpSetCookiesOperation filter = new HttpSetCookiesOperation();
        session.getFilterChain().addLast("operation", filter);
        filter.messageReceived(nextFilter, session, request);
        filter.messageReceived(nextFilter, session, content);
        context.assertIsSatisfied();
    }

    @Test
    public void testGetCookies() throws Exception {
        Mockery context = new Mockery();
        context.setThreadingPolicy(new Synchroniser());

        final NextFilter nextFilter = context.mock(NextFilter.class);
        final IoSessionEx session = new DummySessionEx();
        IoBufferAllocatorEx<?> allocator = session.getBufferAllocator();

        final HttpResponseMessage response = new HttpResponseMessage();
        response.setVersion(HttpVersion.HTTP_1_1);
        response.setStatus(HttpStatus.SUCCESS_OK);
        ByteBuffer buf = Utils.asByteBuffer("COOKIE=value\r\n");
        response.setHeader("Content-Type", "text/plain; charset=UTF-8");
        response.setContent(new HttpContentMessage(allocator.wrap(buf), true));

        context.checking(new Expectations() {
            {
                oneOf(nextFilter).filterWrite(with(session), with(hasMessage(response)));
            }
        });

        final HttpRequestMessage message = new HttpRequestMessage();
        message.setRequestURI(URI.create("/path/;api/get-cookies"));
        message.addCookie(new DefaultHttpCookie("COOKIE", "value"));
        HttpGetCookiesOperation filter = new HttpGetCookiesOperation();
        session.getFilterChain().addLast("operation", filter);
        filter.messageReceived(nextFilter, session, message);
        context.assertIsSatisfied();
    }

    @Test
    public void testGetCookiesWithNoCookies() throws Exception {
        Mockery context = new Mockery();
        context.setThreadingPolicy(new Synchroniser());

        final NextFilter nextFilter = context.mock(NextFilter.class);
        final IoSessionEx session = new DummySessionEx();
        IoBufferAllocatorEx<?> allocator = session.getBufferAllocator();

        final HttpResponseMessage response = new HttpResponseMessage();
        response.setVersion(HttpVersion.HTTP_1_1);
        response.setStatus(HttpStatus.SUCCESS_OK);
        response.setHeader("Content-Type", "text/plain; charset=UTF-8");

        context.checking(new Expectations() {
            {
                oneOf(nextFilter).filterWrite(with(session), with(hasMessage(response)));
            }
        });

        final HttpRequestMessage message = new HttpRequestMessage();
        message.setRequestURI(URI.create("/path/;api/get-cookies"));
        HttpGetCookiesOperation filter = new HttpGetCookiesOperation();
        session.getFilterChain().addLast("operation", filter);
        filter.messageReceived(nextFilter, session, message);
        context.assertIsSatisfied();
    }
}
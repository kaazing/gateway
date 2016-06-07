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
package org.kaazing.gateway.transport.http;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.kaazing.gateway.resource.address.ResourceAddressFactory.newResourceAddressFactory;
import static org.kaazing.test.util.Assert.assertEmpty;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.mina.core.future.IoFuture;
import org.apache.mina.core.service.IoHandler;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.kaazing.gateway.resource.address.NameResolver;
import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.resource.address.ResourceAddressFactory;
import org.kaazing.gateway.transport.Bindings;
import org.kaazing.gateway.transport.BridgeSessionInitializer;
import org.kaazing.gateway.transport.BridgeSessionInitializerAdapter;
import org.kaazing.gateway.transport.IoHandlerAdapter;

public class HttpBindingsTest {

    private HttpBindings httpBindings;
    private Set<Map.Entry<ResourceAddress,HttpBindings.HttpBinding>> entries;

    private ResourceAddressFactory addressFactory;
    private NameResolver nameResolver;

    private IoHandler handler = new IoHandlerAdapter();
    private BridgeSessionInitializer<? extends IoFuture> initializer;


    @Before
    public void setup() {
        httpBindings = new HttpBindings();
        entries = httpBindings.entrySet();
        assertEmpty(entries);

        addressFactory = newResourceAddressFactory();

        nameResolver = new NameResolver() {
            @Override
            public Collection<InetAddress> getAllByName(String host) throws UnknownHostException {
                if (Arrays.asList("localhost", "origin1.com", "origin2.com", "example.com").contains(host)) {
                    return Collections.singleton(InetAddress.getLocalHost());
                }
                throw new UnknownHostException(host);
            }
        };

        handler = new IoHandlerAdapter();
        initializer = new BridgeSessionInitializerAdapter<>();
    }


    @Test
    public void testAddHttpBinding() throws Exception {

        String fooURI = "http://example.com/foo";
        ResourceAddress address = addressFactory.newResourceAddress(fooURI, makeOpts());

        Bindings.Binding binding = new Bindings.Binding(address, handler, initializer);
        Bindings.Binding oldBinding = httpBindings.addBinding(binding);
        assertNull(oldBinding);

        assertEquals(1, entries.size());
        assertEquals(1, httpBindings.getBinding(address).referenceCount());

        System.out.println(httpBindings);

    }


    @Test
    public void testRemoveHttpBinding() throws Exception {

        String fooURI = "http://example.com/foo";
        ResourceAddress address = addressFactory.newResourceAddress(fooURI, makeOpts());

        Bindings.Binding binding = new Bindings.Binding(address, handler, initializer);
        Bindings.Binding oldBinding = httpBindings.addBinding(binding);
        assertNull(oldBinding);

        assertEquals(1, entries.size());
        Bindings.Binding foundBinding = httpBindings.getBinding(address);
        assertNotNull(foundBinding);
        assertEquals(1, foundBinding.referenceCount());

        ResourceAddress removeAddress = addressFactory.newResourceAddress(fooURI, makeOpts());
        boolean removed = httpBindings.removeBinding(removeAddress, binding);
        assertTrue(removed);
        assertEmpty(entries);

        System.out.println(httpBindings);

    }


    @Test
    public void testTwoAddsAndOneRemoveHttpBindingLeavesUsWithAOneReferenceCountBinding() throws Exception {

        String uri1 = "http://example.com/foo";
        String uri2 = "http://example.com/foo";
        ResourceAddress address1 = addressFactory.newResourceAddress(uri1, makeOpts());
        ResourceAddress address2 = addressFactory.newResourceAddress(uri2, makeOpts());

        Bindings.Binding binding1 = new Bindings.Binding(address1, handler, initializer);
        Bindings.Binding binding2 = new Bindings.Binding(address2, handler, initializer);

        Bindings.Binding oldBinding = httpBindings.addBinding(binding1);
        assertNull(oldBinding);
        oldBinding = httpBindings.addBinding(binding2);
        assertNull(oldBinding);

        assertEquals(1, entries.size());
        Bindings.Binding foundBinding1 = httpBindings.getBinding(address1);
        Bindings.Binding foundBinding2 = httpBindings.getBinding(address2);
        assertNotNull(foundBinding1);
        assertNotNull(foundBinding2);
        assertSame(foundBinding1,foundBinding2);

        assertEquals(2, foundBinding1.referenceCount());

        ResourceAddress removeAddress = addressFactory.newResourceAddress(uri1, makeOpts());
        boolean removed = httpBindings.removeBinding(removeAddress, foundBinding1);
        Assert.assertFalse(removed);
        assertEquals(1, entries.size());
        assertEquals(1, foundBinding1.referenceCount());

        System.out.println(httpBindings);
    }

    @Test
    public void shouldCorrectlyBindTwoAddressesWithTcpBind() throws Exception {
        String uri1 = "http://localhost:8001/";
        HashMap<String, Object> options1 = new HashMap<>();
        options1.put("tcp.bind", "7777");

        String uri2 = "http://localhost:8001/";
        HashMap<String, Object> options2 = new HashMap<>();


        ResourceAddress address1 = addressFactory.newResourceAddress(uri1, options1);
        ResourceAddress address2 = addressFactory.newResourceAddress(uri2, options2);

        Bindings.Binding binding1 = new Bindings.Binding(address1, handler, initializer);
        Bindings.Binding binding2 = new Bindings.Binding(address2, handler, initializer);

        Bindings.Binding oldBinding = httpBindings.addBinding(binding1);
        assertNull(oldBinding);
        oldBinding = httpBindings.addBinding(binding2);
        assertNull(oldBinding);
    }

    @Test
    public void shouldCorrectlyBindTwoSecureAddressesWithTcpBind() throws Exception {
        String uri1 = "https://localhost:8001/";
        HashMap<String, Object> options1 = new HashMap<>();
        options1.put("tcp.bind", "7777");

        String uri2 = "https://localhost:8001/";
        HashMap<String, Object> options2 = new HashMap<>();


        ResourceAddress address1 = addressFactory.newResourceAddress(uri1, options1);
        ResourceAddress address2 = addressFactory.newResourceAddress(uri2, options2);

        Bindings.Binding binding1 = new Bindings.Binding(address1, handler, initializer);
        Bindings.Binding binding2 = new Bindings.Binding(address2, handler, initializer);

        Bindings.Binding oldBinding = httpBindings.addBinding(binding1);
        assertNull(oldBinding);
        oldBinding = httpBindings.addBinding(binding2);
        assertNull(oldBinding);
    }

    @Test
    public void testAddHttpBindingTwiceShouldIncrementHttpBindingBindingReferenceCount() throws Exception {
        String fooURI = "http://example.com/foo";
        ResourceAddress address = addressFactory.newResourceAddress(fooURI, makeOpts());

        Bindings.Binding binding = new Bindings.Binding(address, handler, initializer);
        Bindings.Binding oldBinding = httpBindings.addBinding(binding);
        assertNull(oldBinding);

        assertEquals(1, entries.size());
        assertEquals(1, httpBindings.getBinding(address).referenceCount());

        // adding same binding twice updates reference count to 2, only one entry though
        oldBinding = httpBindings.addBinding(binding);
        assertNull(oldBinding);
        assertEquals(1, entries.size());
        assertEquals(2, httpBindings.getBinding(address).referenceCount());

        System.out.println(httpBindings);
    }

    @Test
    public void shouldCorrectlyBindTwoAddressesWithDistinctOrigins() throws Exception {
        Bindings.Binding oldBinding;

        String origin1URI = "http://origin1.com/foo";
        String origin2URI = "http://origin2.com/foo";

        ResourceAddress address1 = addressFactory.newResourceAddress(origin1URI, makeOpts());
        ResourceAddress address2 = addressFactory.newResourceAddress(origin2URI, makeOpts());

        Bindings.Binding binding1 = new Bindings.Binding(address1, handler, initializer);
        Bindings.Binding binding2 = new Bindings.Binding(address2, handler, initializer);

        oldBinding = httpBindings.addBinding(binding1);
        assertNull(oldBinding);
        oldBinding = httpBindings.addBinding(binding2);
        assertNull(oldBinding);

        assertEquals(2, entries.size());
        assertEquals(1, binding1.referenceCount());
        assertEquals(1, binding2.referenceCount());

        // Now look up with suffix addresses, should find original binding1 and binding2
        String origin1aURI = "http://origin1.com/foo/bar";
        String origin2aURI = "http://origin2.com/foo/bar";
        ResourceAddress address1a = addressFactory.newResourceAddress(origin1aURI, makeOpts());
        ResourceAddress address2a = addressFactory.newResourceAddress(origin2aURI, makeOpts());

        Bindings.Binding foundBinding1 = httpBindings.getBinding(address1a);
        Bindings.Binding foundBinding2 = httpBindings.getBinding(address2a);
        assertSame(binding1, foundBinding1);
        assertSame(binding2, foundBinding2);

        assertEquals(1, foundBinding1.referenceCount());
        assertEquals(1, foundBinding2.referenceCount());

        System.out.println(httpBindings);


    }

    @Test
    public void shouldCorrectlyBindTwoAddressesWithDistinctProtocolStacks() throws Exception {

        Bindings.Binding oldBinding;

        String stack1URI = "http://origin1.com/foo";
        String stack2URI = "httpx://origin1.com/foo";

        ResourceAddress address1 = addressFactory.newResourceAddress(stack1URI, makeOpts());
        ResourceAddress address2 = addressFactory.newResourceAddress(stack2URI, makeOpts());

        Bindings.Binding binding1 = new Bindings.Binding(address1, handler, initializer);
        Bindings.Binding binding2 = new Bindings.Binding(address2, handler, initializer);

        oldBinding = httpBindings.addBinding(binding1);
        assertNull(oldBinding);
        oldBinding = httpBindings.addBinding(binding2);
        assertNull(oldBinding);

        assertEquals(2, entries.size());
        assertEquals(1, binding1.referenceCount());
        assertEquals(1, binding2.referenceCount());

        // Now look up with suffix addresses, should find original binding1 and binding2
        String stack1aURI = "http://origin1.com/foo/bar";
        String stack2aURI = "httpx://origin1.com/foo/bar";
        ResourceAddress address1a = addressFactory.newResourceAddress(stack1aURI, makeOpts());
        ResourceAddress address2a = addressFactory.newResourceAddress(stack2aURI, makeOpts());

        Bindings.Binding foundBinding1 = httpBindings.getBinding(address1a);
        Bindings.Binding foundBinding2 = httpBindings.getBinding(address2a);
        assertSame(binding1, foundBinding1);
        assertSame(binding2, foundBinding2);

        assertEquals(1, foundBinding1.referenceCount());
        assertEquals(1, foundBinding2.referenceCount());

        System.out.println(httpBindings);

    }


    @Test
    public void shouldCorrectlyBindHttpBindingsForWsnWsxBindings() throws Exception {

        Bindings.Binding oldBinding;

        String stack1URI = "http://origin1.com/foo";
        String stack2URI = "httpx://origin1.com/foo";

        ResourceAddress address1 = addressFactory.newResourceAddress(stack1URI, makeOpts("ws/rfc6455"));
        ResourceAddress address2 = addressFactory.newResourceAddress(stack2URI, makeOpts("ws/rfc6455"));

        Bindings.Binding binding1 = new Bindings.Binding(address1, handler, initializer);
        Bindings.Binding binding2 = new Bindings.Binding(address2, handler, initializer);

        oldBinding = httpBindings.addBinding(binding1);
        assertNull(oldBinding);
        oldBinding = httpBindings.addBinding(binding2);
        assertNull(oldBinding);

        assertEquals(2, entries.size());
        assertEquals(1, binding1.referenceCount());
        assertEquals(1, binding2.referenceCount());

        Bindings.Binding foundBinding1 = httpBindings.getBinding(address1);
        Bindings.Binding foundBinding2 = httpBindings.getBinding(address2);
        assertSame(binding1, foundBinding1);
        assertSame(binding2, foundBinding2);

        assertEquals(1, foundBinding1.referenceCount());
        assertEquals(1, foundBinding2.referenceCount());

        boolean removed = httpBindings.removeBinding(address1, foundBinding1);
        assertTrue(removed);
        assertEquals(1, entries.size());
        assertEquals(0, foundBinding1.referenceCount());
        assertEquals(1, foundBinding2.referenceCount());

        removed = httpBindings.removeBinding(address2, foundBinding2);
        assertTrue(removed);
        assertEmpty(entries);
        assertEquals(0, foundBinding1.referenceCount());
        assertEquals(0, foundBinding2.referenceCount());

        System.out.println(httpBindings);

    }




    private Map<String, Object> makeOpts() {
        return makeOpts(null);
    }

    private Map<String, Object> makeOpts(String nextProtocol) {
        Map<String, Object> opts = new HashMap<>();
        opts.put("tcp.resolver", nameResolver);
        if ( nextProtocol != null ) {
            opts.put("nextProtocol", nextProtocol);
        }
        return opts;
    }


    @Test
    public void testGet() throws Exception {

        /*
        Test tree:
            /                            <no address>
            /foo                         <foo address>
            /foo/baz                     <baz address>
            /bar                         <no address>
            /bar/fiz                     <fiz address>
            /bar/buz                     <buz address>
         */
        String fooURI = "http://example.com/foo";
        String bazURI = "http://example.com/foo/baz";
        String fizURI = "http://example.com/bar/fiz";
        String buzURI = "http://example.com/bar/buz";

        ResourceAddress fooBindAddress = addressFactory.newResourceAddress(fooURI, makeOpts());
        ResourceAddress bazBindAddress = addressFactory.newResourceAddress(bazURI, makeOpts());
        ResourceAddress fizBindAddress = addressFactory.newResourceAddress(fizURI, makeOpts());
        ResourceAddress buzBindAddress = addressFactory.newResourceAddress(buzURI, makeOpts());

        HttpBindings.HttpBinding fooBinding = new HttpBindings.HttpBinding(fooBindAddress);
        HttpBindings.HttpBinding bazBinding = new HttpBindings.HttpBinding(bazBindAddress);
        HttpBindings.HttpBinding fizBinding = new HttpBindings.HttpBinding(fizBindAddress);
        HttpBindings.HttpBinding buzBinding = new HttpBindings.HttpBinding(buzBindAddress);

        assertNull(httpBindings.addBinding(fooBinding));
        assertNull(httpBindings.addBinding(bazBinding));
        assertNull(httpBindings.addBinding(fizBinding));
        assertNull(httpBindings.addBinding(buzBinding));

        // Do not expect to get addresses at these urls
        assertNull(httpBindings.getBinding(addressFactory.newResourceAddress("http://example.com/", makeOpts())));
        assertNull(httpBindings.getBinding(addressFactory.newResourceAddress("http://example.com/extra", makeOpts())));
        assertNull(httpBindings.getBinding(addressFactory.newResourceAddress("http://example.com/bar", makeOpts())));
        assertNull(httpBindings.getBinding(addressFactory.newResourceAddress("http://example.com/bar/extra", makeOpts())));

        // Expect to find specific addresses at these urls
        assertSame(fooBinding, httpBindings.getBinding(fooBindAddress));
        assertSame(bazBinding, httpBindings.getBinding(bazBindAddress));
        assertSame(fizBinding, httpBindings.getBinding(fizBindAddress));
        assertSame(buzBinding, httpBindings.getBinding(buzBindAddress));
        assertSame(fooBinding, httpBindings.getBinding(addressFactory.newResourceAddress("http://example.com/foo/bar", makeOpts())));
        assertSame(bazBinding, httpBindings.getBinding(addressFactory.newResourceAddress("http://example.com/foo/baz/extra", makeOpts())));
        assertSame(bazBinding, httpBindings.getBinding(addressFactory.newResourceAddress("http://example.com/foo/baz/extra/extra", makeOpts())));
        assertSame(fizBinding, httpBindings.getBinding(addressFactory.newResourceAddress("http://example.com/bar/fiz/extra/extra", makeOpts())));
        assertSame(fizBinding, httpBindings.getBinding(addressFactory.newResourceAddress("http://example.com/bar/fiz/extra", makeOpts())));
    }

    @Test
    public void testBindPutLogic() throws Exception {

        // Basic bind to start with
        String bindUri = "http://localhost/foo/bar";
        ResourceAddress bindAddress = addressFactory.newResourceAddress(bindUri, makeOpts());
        Bindings.Binding binding = new Bindings.Binding(bindAddress, new IoHandlerAdapter());
        Bindings.Binding oldBinding = httpBindings.addBinding(binding);
        assertNull(oldBinding);

        // Attempt to bind another equal address should yield null
        ResourceAddress bindAddress2 = addressFactory.newResourceAddress(bindUri, makeOpts());
        Bindings.Binding binding2 = new Bindings.Binding(bindAddress2, binding.handler());
        oldBinding = httpBindings.addBinding(binding2);
        assertNull(oldBinding);

        // Now bind a handler at /foo.
        String fooBindUri = "http://localhost/foo";
        ResourceAddress fooBindAddress = addressFactory.newResourceAddress(fooBindUri, makeOpts());
        Bindings.Binding fooBinding = new Bindings.Binding(fooBindAddress, new IoHandlerAdapter());
        oldBinding = httpBindings.addBinding(fooBinding);
        assertNull(oldBinding);

        // Attempt to bind another address should yield non-null
        Bindings.Binding fooBinding2 = new Bindings.Binding(fooBindAddress, new IoHandlerAdapter());
        oldBinding = httpBindings.addBinding(fooBinding2);
        assertNotNull(oldBinding);

        // Put a non-matching address, expect to get different binding out
        String wsFooBindUri = "ws://localhost/foo";
        ResourceAddress wsFooBindAddress = addressFactory.newResourceAddress(wsFooBindUri);
        Bindings.Binding wsFooBinding = new Bindings.Binding(wsFooBindAddress, new IoHandlerAdapter());
        oldBinding = httpBindings.addBinding(wsFooBinding);
        assertNotSame(oldBinding, fooBinding);
    }

}

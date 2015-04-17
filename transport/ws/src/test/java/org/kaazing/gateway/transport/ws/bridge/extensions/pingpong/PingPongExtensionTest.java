/**
 * Copyright (c) 2007-2014 Kaazing Corporation. All rights reserved.
 * 
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.kaazing.gateway.transport.ws.bridge.extensions.pingpong;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.kaazing.gateway.transport.ws.AbstractWsControlMessage.Style.CLIENT;
import static org.kaazing.gateway.transport.ws.WsMessage.Kind.BINARY;
import static org.kaazing.gateway.transport.ws.WsMessage.Kind.PING;
import static org.kaazing.gateway.transport.ws.WsMessage.Kind.PONG;
import static org.kaazing.gateway.transport.ws.WsMessage.Kind.TEXT;
import static org.kaazing.gateway.transport.ws.extension.Extension.EndpointKind.SERVER;
import static org.kaazing.mina.core.buffer.SimpleBufferAllocator.BUFFER_ALLOCATOR;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.mina.core.filterchain.IoFilter;
import org.apache.mina.core.filterchain.IoFilterChain;
import org.apache.mina.core.filterchain.IoFilterChain.Entry;
import org.apache.mina.filter.codec.ProtocolCodecFactory;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.lib.concurrent.Synchroniser;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.Before;
import org.junit.Test;
import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.resource.address.ResourceAddressFactory;
import org.kaazing.gateway.transport.ws.AbstractWsControlMessage;
import org.kaazing.gateway.transport.ws.WsMessage;
import org.kaazing.gateway.transport.ws.WsPingMessage;
import org.kaazing.gateway.transport.ws.WsPongMessage;
import org.kaazing.gateway.transport.ws.extension.Extension;
import org.kaazing.gateway.transport.ws.extension.ExtensionBuilder;
import org.kaazing.gateway.util.Utils;
import org.kaazing.mina.filter.codec.ProtocolCodecFilter;

/**
* NOTE: this class is a "classic" unit test for the WsCloseFilter. Overall testing of websocket close
* handling for the wsn transport layer is in test class WsCloseTransportTest.
*/
public class PingPongExtensionTest {

    ResourceAddress wsAddress;

    @Before
    public void setUp() {
        URI addressURI = URI.create("ws://localhost:2020/");
        Map<String, Object> options = new HashMap<>();
        options.put("ws.inactivityTimeout", 2500L);
        wsAddress = ResourceAddressFactory.newResourceAddressFactory().newResourceAddress(addressURI, options);
    }

    @Test
    public void shouldDecodeAndEncodeTextOnly() throws Exception {
//        Mockery context = new Mockery() {{
//            setImposteriser(ClassImposteriser.INSTANCE);
//        }};
//        final ServiceContext serviceContext = context.mock(ServiceContext.class);
//        
//        context.checking(new Expectations() {{
//
//        }});
        Extension extension = ExtensionBuilder.create(wsAddress, new ExtensionBuilder("x-kaazing-ping-pong"));
        assertFalse(extension.canDecode(SERVER, BINARY));
        assertTrue(extension.canDecode(SERVER, TEXT));
        assertFalse(extension.canEncode(SERVER, BINARY));
        assertTrue(extension.canEncode(SERVER, TEXT));
    }

    @Test
    public void shouldDecodePing() throws Exception {
//        Mockery context = new Mockery() {{
//            setImposteriser(ClassImposteriser.INSTANCE);
//        }};
//        final ServiceContext serviceContext = context.mock(ServiceContext.class);
//        
//        context.checking(new Expectations() {{
//
//        }});
        Extension extension = ExtensionBuilder.create(wsAddress, new ExtensionBuilder("x-kaazing-ping-pong"));
        WsMessage result = extension.decode(BUFFER_ALLOCATOR.wrap(Utils.asByteBuffer(new byte[]{0x09, 0x00})));
        assertEquals(PING, result.getKind());
        assertEquals(WsPingMessage.class, result.getClass());
    }

    @Test
    public void shouldDecodePong() throws Exception {
//        Mockery context = new Mockery() {{
//            setImposteriser(ClassImposteriser.INSTANCE);
//        }};
//        final ServiceContext serviceContext = context.mock(ServiceContext.class);
//        
//        context.checking(new Expectations() {{
//
//        }});
        Extension extension = ExtensionBuilder.create(wsAddress, new ExtensionBuilder("x-kaazing-ping-pong"));
        WsMessage result = extension.decode(BUFFER_ALLOCATOR.wrap(Utils.asByteBuffer(new byte[]{0x0a, 0x00})));
        assertEquals(PONG, result.getKind());
        assertEquals(WsPongMessage.class, result.getClass());
    }

    @Test
    public void shouldEncodePing() throws Exception {
//        Mockery context = new Mockery() {{
//            setImposteriser(ClassImposteriser.INSTANCE);
//        }};
//        final ServiceContext serviceContext = context.mock(ServiceContext.class);
//        
//        context.checking(new Expectations() {{
//
//        }});
        Extension extension = ExtensionBuilder.create(wsAddress, new ExtensionBuilder("x-kaazing-ping-pong"));
        AbstractWsControlMessage message = new WsPingMessage();
        message.setStyle(CLIENT);
        message.setExtension(extension);
        byte[] result = extension.encode(message);
        assertTrue(Arrays.equals(new byte[]{0x09, 0x00}, result));
    }

    @Test
    public void shouldEncodePong() throws Exception {
//        Mockery context = new Mockery() {{
//            setImposteriser(ClassImposteriser.INSTANCE);
//        }};
//        final ServiceContext serviceContext = context.mock(ServiceContext.class);
//        
//        context.checking(new Expectations() {{
//
//        }});
        Extension extension = ExtensionBuilder.create(wsAddress, new ExtensionBuilder("x-kaazing-ping-pong"));
        AbstractWsControlMessage message = new WsPongMessage();
        message.setStyle(CLIENT);
        message.setExtension(extension);
        byte[] result = extension.encode(message);
        assertTrue(Arrays.equals(new byte[]{0x0a, 0x00}, result));
    }

    @Test
    public void addBridgeFiltersShouldAddFilter() throws Exception {
        Mockery context = new Mockery() {{
            setImposteriser(ClassImposteriser.INSTANCE);
        }};
        context.setThreadingPolicy(new Synchroniser());
//        final ServiceContext serviceContext = context.mock(ServiceContext.class);
        final IoFilterChain filterChain = context.mock(IoFilterChain.class);
        final Entry filter1 = context.mock(Entry.class, "filter1");
        final Entry codec = context.mock(Entry.class, "codec");
        final Entry filter2 = context.mock(Entry.class, "filter2");
        final List<Entry> filters = new ArrayList<>(asList(filter1, codec, filter2));
        final IoFilter filter1Filter = context.mock(IoFilter.class);
        final ProtocolCodecFactory protocolCodecFactory = context.mock(ProtocolCodecFactory.class);  
        final IoFilter codecFilter = new ProtocolCodecFilter(protocolCodecFactory);
        
        context.checking(new Expectations() {{
            oneOf(filterChain).getAll(); will(returnValue(filters));
            oneOf(filter1).getFilter(); will(returnValue(filter1Filter));
            oneOf(codec).getName(); will(returnValue("codec"));
            oneOf(codec).getFilter(); will(returnValue(codecFilter));
            oneOf(filterChain).addAfter(with("codec"), with("x-kaazing-ping-pong"), with(any(PingPongFilter.class)));
        }});
        Extension extension = ExtensionBuilder.create(wsAddress, new ExtensionBuilder("x-kaazing-ping-pong"));
        extension.updateBridgeFilters(filterChain);
    }

    @Test
    public void removeBridgeFiltersShouldRemoveFilter() throws Exception {
        Mockery context = new Mockery() {{
            setImposteriser(ClassImposteriser.INSTANCE);
        }};
        context.setThreadingPolicy(new Synchroniser());
//        final ServiceContext serviceContext = context.mock(ServiceContext.class);
        final IoFilterChain filterChain = context.mock(IoFilterChain.class);
        
        context.checking(new Expectations() {{
            oneOf(filterChain).remove(with(PingPongFilter.class));
        }});
        Extension extension = ExtensionBuilder.create(wsAddress, new ExtensionBuilder("x-kaazing-ping=pong"));
        extension.removeBridgeFilters(filterChain);
    }

    @Test
    public void removeBridgeFiltersShouldIgnoreException() throws Exception {
        Mockery context = new Mockery() {{
            setImposteriser(ClassImposteriser.INSTANCE);
        }};
        context.setThreadingPolicy(new Synchroniser());
//        final ServiceContext serviceContext = context.mock(ServiceContext.class);
        final IoFilterChain filterChain = context.mock(IoFilterChain.class);
        
        context.checking(new Expectations() {{
            oneOf(filterChain).remove(with(PingPongFilter.class));
            will(throwException(new IllegalArgumentException("filter not present in chain")));
        }});
        Extension extension = ExtensionBuilder.create(wsAddress, new ExtensionBuilder("x-kaazing-ping-pong"));
        extension.removeBridgeFilters(filterChain);
    }
    
}

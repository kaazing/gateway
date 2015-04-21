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

package org.kaazing.gateway.transport.ws.bridge.extensions.idletimeout;

import static org.junit.Assert.assertFalse;
import static org.kaazing.gateway.transport.ws.WsMessage.Kind.BINARY;
import static org.kaazing.gateway.transport.ws.WsMessage.Kind.TEXT;
import static org.kaazing.gateway.transport.ws.extension.ExtensionHeader.EndpointKind.SERVER;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import org.apache.mina.core.filterchain.IoFilterChain;
import org.jmock.Mockery;
import org.jmock.lib.concurrent.Synchroniser;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.Before;
import org.junit.Test;
import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.resource.address.ResourceAddressFactory;
import org.kaazing.gateway.transport.ws.extension.ExtensionHeader;
import org.kaazing.gateway.transport.ws.extension.ExtensionHeaderBuilder;
import org.kaazing.gateway.transport.ws.util.Expectations;

/**
* NOTE: this class is a "classic" unit test for the WsCloseFilter. Overall testing of websocket close
* handling for the wsn transport layer is in test class WsCloseTransportTest.
*/
public class IdleTimeouExtensionTest {

    ResourceAddress wsAddress;

    @Before
    public void setUp() {
        URI addressURI = URI.create("ws://localhost:2020/");
        Map<String, Object> options = new HashMap<>();
        options.put("ws.inactivityTimeout", 2500L);
        wsAddress = ResourceAddressFactory.newResourceAddressFactory().newResourceAddress(addressURI, options);
    }

    @Test
    public void shouldNotDecodeOrEncode() throws Exception {
        ExtensionHeader extension = ExtensionHeaderBuilder.create(wsAddress, new ExtensionHeaderBuilder("x-kaazing-idle-timeout"));
        assertFalse(extension.canDecode(SERVER, BINARY));
        assertFalse(extension.canDecode(SERVER, TEXT));
        assertFalse(extension.canEncode(SERVER, BINARY));
        assertFalse(extension.canEncode(SERVER, TEXT));
    }

    @Test
    public void addBridgeFiltersShouldAddFilter() throws Exception {
        Mockery context = new Mockery() {{
            setImposteriser(ClassImposteriser.INSTANCE);
        }};
        context.setThreadingPolicy(new Synchroniser());
        final IoFilterChain filterChain = context.mock(IoFilterChain.class);
        
        context.checking(new Expectations() {{
            oneOf(filterChain).addLast(with("x-kaazing-idle-timeout"), with(any(IdleTimeoutFilter.class)));
        }});
        ExtensionHeader extension = ExtensionHeaderBuilder.create(wsAddress, new ExtensionHeaderBuilder("x-kaazing-idle-timeout"));
        extension.updateBridgeFilters(filterChain);
    }

    @Test
    public void removeBridgeFiltersShouldRemoveFilter() throws Exception {
        Mockery context = new Mockery() {{
            setImposteriser(ClassImposteriser.INSTANCE);
        }};
        context.setThreadingPolicy(new Synchroniser());
        final IoFilterChain filterChain = context.mock(IoFilterChain.class);
        
        context.checking(new Expectations() {{
            oneOf(filterChain).remove(with(IdleTimeoutFilter.class));
        }});
        ExtensionHeader extension = ExtensionHeaderBuilder.create(wsAddress, new ExtensionHeaderBuilder("x-kaazing-idle-timeout"));
        extension.removeBridgeFilters(filterChain);
    }

    @Test
    public void removeBridgeFiltersShouldIgnoreException() throws Exception {
        Mockery context = new Mockery() {{
            setImposteriser(ClassImposteriser.INSTANCE);
        }};
        context.setThreadingPolicy(new Synchroniser());
        final IoFilterChain filterChain = context.mock(IoFilterChain.class);
        
        context.checking(new Expectations() {{
            oneOf(filterChain).remove(with(IdleTimeoutFilter.class));
            will(throwException(new IllegalArgumentException("filter not present in chain")));
        }});
        ExtensionHeader extension = ExtensionHeaderBuilder.create(wsAddress, new ExtensionHeaderBuilder("x-kaazing-idle-timeout"));
        extension.removeBridgeFilters(filterChain);
    }
    
}

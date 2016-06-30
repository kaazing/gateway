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
package org.kaazing.gateway.resource.address.wse;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.util.Arrays.asList;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.kaazing.gateway.resource.address.ResourceAddress.TRANSPORT_URI;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.resource.address.ResourceAddressFactory;

//TODO: Make this test assert over the entire structure.  Develop other tests to do the same.
public class WseSslResourceAddressTest {

    private ResourceAddressFactory addressFactory;
    private String addressURI;

    @Before
    public void before() {
        addressFactory = ResourceAddressFactory.newResourceAddressFactory();

        addressURI = "wse+ssl://localhost:2020/";
        Map<String, Object> options = new HashMap<>();
        options.put("ws.nextProtocol", "custom");
        options.put("ws.qualifier", "random");
        options.put("ws.codecRequired", FALSE);
        options.put("ws.lightweight", TRUE);
        options.put("ws.extensions", asList("x-kaazing-alpha", "x-kaazing-beta"));
        options.put("ws.maxMessageSize", 1024);
        options.put("ws.inactivityTimeout", SECONDS.toMillis(5));
        options.put("ws.supportedProtocols", new String[]{"amqp/0.91", "amqp/1.0"});
        options.put("ws.requiredProtocols", new String[]{"amqp/0.91", "amqp/1.0"});
        options.put("ws.transport", "https://localhost:2121/");
    }

    @Test
    public void shouldCreateAddressWithDefaultTransport() throws Exception {
        ResourceAddress address = addressFactory.newResourceAddress(addressURI);
        assertNotNull(address.getOption(TRANSPORT_URI));
        System.out.println(address);
        //NOTE: the primary transport is still https(=http|ssl|tcp), the alternate is httpxe|http|ssl|tcp
        //      however in this unit test the alternate is not yet materialized.
        assertEquals("https://localhost:2020/", address.getOption(TRANSPORT_URI));
    }

    
}

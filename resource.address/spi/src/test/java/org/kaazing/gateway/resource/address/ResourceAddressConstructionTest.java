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
package org.kaazing.gateway.resource.address;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.net.URI;
import java.util.Map;

import org.junit.Before;
import org.kaazing.gateway.resource.address.uri.URIUtils;

public class ResourceAddressConstructionTest {

    private ResourceAddressFactory resourceAddressFactory;

    private URI addressURI;
    private Map<String, Object> options;
    
    @Before
    public void before() {

        resourceAddressFactory = ResourceAddressFactory.newResourceAddressFactory();


    }


/*
    Commenting out as it needs dependency on wsxdraft module

    @Test
    public void verifyConstructionOfWsxDraftSslAddress() throws Exception {

        addressURI = URI.create("wsx-draft+ssl://localhost:2020/");

        options = new HashMap<String, Object>();
        options.put("ws.nextProtocol", "custom");
        options.put("ws.qualifier", "random");
        options.put("ws.codecRequired", FALSE);
        options.put("ws.lightweight", TRUE);
        options.put("ws.extensions", asList("x-kaazing-alpha", "x-kaazing-beta"));
        options.put("ws.maxMessageSize", 1024);
        options.put("ws.inactivityTimeout", SECONDS.toMillis(5));
        options.put("ws.supportedProtocols", new String[] { "amqp/0.91", "amqp/1.0" });
        options.put("ws.requiredProtocols", new String[] { "amqp/0.91", "amqp/1.0" });

        verifyResourceAddressStructure(new String[][] {
                new String[] { "ws://localhost:2020/",   "wsx-draft+ssl",   "custom" },
                new String[] { "http://localhost:2020/", "httpx-draft+ssl", "ws/draft-7x" },
                new String[] { "ws://localhost:2020/",   "ws-draft+ssl",    "x-kaazing-handshake" },
                new String[] { "http://localhost:2020/", "https",           "ws/draft-7x" },
                new String[] { "ssl://localhost:2020",   "ssl",             "http/1.1" },
                new String[] { "tcp://127.0.0.1:2020",   "tcp",             "ssl" }
        }, resourceAddressFactory.newResourceAddress(addressURI, options));
    }
*/







    private void verifyResourceAddressStructure(String[][] expectedStructures, ResourceAddress address) {
        if ( address == null ) {
            fail("address to verify is null");
        }
        if ( expectedStructures == null ) {
            fail("expected structures to verify address are null");
        }

        verifyResourceAddressDepth(expectedStructures, address);

        ResourceAddress cursor = address;
        for ( String[] expectedStructure: expectedStructures) {
            String expectedResourceURI = expectedStructure[0];
            String expectedExternalScheme = expectedStructure[1];
            String expectedNextProtocol = expectedStructure[2];

            assertEquals("resource mismatch\n"+address, URI.create(expectedResourceURI), cursor.getResource());
            assertEquals("scheme mismatch\n"+address, expectedExternalScheme, URIUtils.getScheme(cursor.getExternalURI()));
            assertEquals("nextprotocol mismatch\n"+address, expectedNextProtocol, cursor.getOption(ResourceAddress.NEXT_PROTOCOL));

            cursor = cursor.getTransport();

        }
    }

    private void verifyResourceAddressDepth(String[][] expectedStructure, ResourceAddress address) {
        int expectedDepth = expectedStructure.length;
        int actualDepth = 1;
        ResourceAddress cursor = address;
        while ( cursor.getTransport() != null) {
            cursor = cursor.getTransport();
            actualDepth++;
        }
        assertEquals("depth mismatch\n"+address,expectedDepth, actualDepth);
    }


}

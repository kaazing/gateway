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
import static org.kaazing.gateway.resource.address.ResourceAddress.ALTERNATE;
import static org.kaazing.gateway.resource.address.ResourceAddress.NEXT_PROTOCOL;
import static org.kaazing.gateway.resource.address.ResourceAddress.QUALIFIER;
import static org.kaazing.gateway.resource.address.ResourceAddress.TRANSPORT;
import static org.kaazing.gateway.resource.address.ResourceAddress.TRANSPORT_URI;
import static org.kaazing.gateway.resource.address.ResourceAddressFactory.newResourceAddressFactory;

import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class ResolveResourceAddressTest {


    private static final String ALTERNATE_STRATEGY_PROPERTY_NAME = "TestResourceAddressFactorySpi.alternateStrategy";
    ResourceAddressFactory addressFactory;
    private ResourceAddress address;

    @Before
    public void setup() throws Exception {
        addressFactory= newResourceAddressFactory();
        address = createTestAddress();
    }


    private ResourceAddress createTestAddress() {
        String addressURI = "test://opaque/path";
        Map<String, Object> options = new HashMap<>();
        options.put(TRANSPORT.name(), "test://transport/path");
        return addressFactory.newResourceAddress(addressURI, options);
    }

    @After
    public void after() throws Exception {
        System.clearProperty("alternateStrategy");
    }



    @Test(expected = NullPointerException.class)
    public void shouldFailWhenResolvingToANullPath() throws Exception {
        address.resolve(null);
    }

    @Test
    public void shouldResolveSamePath() throws Exception {
        ResourceAddress result = address.resolve("/path");
        Assert.assertEquals(address, result);
        Assert.assertSame(address, result);
        System.out.println(result);
    }

    @Test
    public void shouldResolveNewPath() throws Exception {
        System.setProperty(ALTERNATE_STRATEGY_PROPERTY_NAME, "duplicateAlternates");
        address = createTestAddress();
        ResourceAddress result = address.resolve("/newpath");
        Assert.assertNotNull(result.equals(address));
        Assert.assertNotSame(address, result);
        Assert.assertFalse(result.equals(address));

        verifyResolvedAddress(address, result, "/path", "/newpath");
        System.out.println(result);
    }


    @Test
    public void shouldResolveNewPathButLeaveDifferentPathsUnaffected() throws Exception {
        System.setProperty(ALTERNATE_STRATEGY_PROPERTY_NAME, "differentPaths");
        address = createTestAddress();
        ResourceAddress result = address.resolve("/newpath");
        Assert.assertNotNull(result.equals(address));
        Assert.assertNotSame(address, result);
        Assert.assertFalse(result.equals(address));

        verifyResolvedAddress(address, result, "/path", "/newpath");
        System.out.println(result);

    }


    @Test
    public void shouldResolveNewPathButLeaveDifferentAuthoritiesUnaffected() throws Exception {
        System.setProperty(ALTERNATE_STRATEGY_PROPERTY_NAME, "differentAuthorities");
        address = createTestAddress();
        ResourceAddress result = address.resolve("/newpath");
        Assert.assertNotNull(result.equals(address));
        Assert.assertNotSame(address, result);
        Assert.assertFalse(result.equals(address));

        verifyResolvedAddress(address, result, "/path", "/newpath");
        System.out.println(result);

    }

    private void verifyResolvedAddress(ResourceAddress source,
                                       ResourceAddress dest,
                                       final String oldPath,
                                       final String newPath) {
        verifyResolvedAddress(source, dest, oldPath, newPath, true);
    }

    private void verifyResolvedAddress(ResourceAddress source,
                                       ResourceAddress dest,
                                       final String oldPath,
                                       final String newPath,
                                       boolean continueToVerify) {


        if ( source == null && dest == null ) {
            return;
        }


        assert (dest != null && source != null);

        continueToVerify = assertPathOnlyUpdated(source, dest,
                                         oldPath, newPath,
                                         continueToVerify);

        verifyResolvedAddress(source.getOption(ALTERNATE),
                              dest.getOption(ALTERNATE),
                              oldPath,
                              newPath,
                              continueToVerify);


        verifyResolvedAddress(source.getTransport(),
                              dest.getTransport(),
                              oldPath, newPath,
                              continueToVerify);

    }


    private boolean assertPathOnlyUpdated(ResourceAddress source, ResourceAddress dest,
                                          final String oldPath, final String newPath, boolean continueToVerify) {

        if ( continueToVerify ) {
            if ( source.getResource().getPath().equals(oldPath)) {
                assertEquals(newPath, dest.getResource().getPath());
            } else {
                assertEquals(source.getResource().getPath(), dest.getResource().getPath());
                continueToVerify = false;
            }

            assertEquals(source.getResource().getAuthority(), dest.getResource().getAuthority());
            assertEquals(source.getResource().getQuery(), dest.getResource().getQuery());
            assertEquals(source.getResource().getFragment(), dest.getResource().getFragment());
            assertEquals(source.getOption(NEXT_PROTOCOL), dest.getOption(NEXT_PROTOCOL));
            assertEquals(source.getOption(QUALIFIER), dest.getOption(QUALIFIER));
            assertEquals(source.getOption(TRANSPORT_URI), dest.getOption(TRANSPORT_URI));
        }

        return continueToVerify;
    }


}

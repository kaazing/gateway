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
package org.kaazing.gateway.transport;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.kaazing.gateway.resource.address.Comparators.compareResourceOriginAndProtocolStack;

import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;

import org.apache.mina.core.future.IoFuture;
import org.apache.mina.core.service.IoHandler;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.resource.address.ResourceAddressFactory;

public class BindingsTest {


    private Bindings<Bindings.Binding> bindings;
    private ResourceAddressFactory addressFactory = ResourceAddressFactory.newResourceAddressFactory();
    Comparator<ResourceAddress> BINDINGS_COMPARATOR = compareResourceOriginAndProtocolStack();
    private static final int BIND_COUNT = 10;

    @Before
    public void setUp() throws Exception {
        bindings = new Bindings.Default();
    }


    @Test
    public void canAddABindingAndSeeItIsAddedProperly() throws Exception {

        IoHandler handler = new IoHandlerAdapter();
        BridgeSessionInitializer<? extends IoFuture> initializer = new BridgeSessionInitializerAdapter<>();

        String location = "tcp://localhost:8000";
        ResourceAddress bindAddress = makeResourceAddress(location);
        Bindings.Binding newBinding = new Bindings.Binding(bindAddress, handler, initializer);
        bindings.addBinding(newBinding);

        Set<Map.Entry<ResourceAddress, Bindings.Binding>> entries = bindings.entrySet();

        assertEquals("Failed to add a binding.", 1, entries.size());

        for (Map.Entry<ResourceAddress, Bindings.Binding> entry : entries) {
            Bindings.Binding binding = entry.getValue();
            assertEquals(1, binding.referenceCount());
            assertSame(newBinding, binding);
            assertEquals("Binding found does not equal binding added.", newBinding, binding);
            assertEquals(0, BINDINGS_COMPARATOR.compare(bindAddress, binding.bindAddress()));
            final ResourceAddress addressTWO = makeResourceAddress("tcp://LOCALHOST:8000");
            assertEquals(0, BINDINGS_COMPARATOR.compare(addressTWO, binding.bindAddress()));
        }
    }

    @Test
    public void canAddAHttpBindingAndSeeItIsAddedProperly() throws Exception {

        IoHandler handler = new IoHandlerAdapter();
        BridgeSessionInitializer<? extends IoFuture> initializer = new BridgeSessionInitializerAdapter<>();

        String location = "http://localhost:8000/";
        ResourceAddress bindAddress = makeResourceAddress(location);
        Bindings.Binding newBinding = new Bindings.Binding(bindAddress, handler, initializer);
        bindings.addBinding(newBinding);

        Set<Map.Entry<ResourceAddress, Bindings.Binding>> entries = bindings.entrySet();

        assertEquals("Failed to add a binding.", 1, entries.size());

        for (Map.Entry<ResourceAddress, Bindings.Binding> entry : entries) {
            Bindings.Binding binding = entry.getValue();
            assertEquals(1, binding.referenceCount());
            assertSame(newBinding, binding);
            assertEquals("Binding found does not equal binding added.", newBinding, binding);
            assertEquals(0, BINDINGS_COMPARATOR.compare(bindAddress, binding.bindAddress()));
            final ResourceAddress addressTWO = makeResourceAddress("http://LOCALHOST:8000/");
            // TCP resource will match on ip address, where HTTP forces case-sensitive check on authority
            assertEquals(0, BINDINGS_COMPARATOR.compare(addressTWO, binding.bindAddress()));
        }
    }

    @Test
    public void addingAndRemovingABindingShouldRemoveBindingEntry() throws Exception {


        Set<Map.Entry<ResourceAddress, Bindings.Binding>> entries = bindings.entrySet();

        IoHandler handler = new IoHandlerAdapter();
        BridgeSessionInitializer<? extends IoFuture> initializer = new BridgeSessionInitializerAdapter<>();
        String location = "tcp://localhost:8000";

        // Add binding
        ResourceAddress bindAddress = makeResourceAddress(location);
        Bindings.Binding newBinding = new Bindings.Binding(bindAddress, handler, initializer);
        assertEquals(0, newBinding.referenceCount());
        bindings.addBinding(newBinding);

        assertEquals(1, newBinding.referenceCount());

        // Remove binding
        ResourceAddress unbindAddress = makeResourceAddress(location);
        boolean removed = bindings.removeBinding(unbindAddress, bindings.getBinding(bindAddress));

        assertTrue(removed);
        assertEmpty(entries);
        assertEquals(0, newBinding.referenceCount());
    }

    @Test
    public void testReferenceCountIncreasesThenDecreasesWithMultipleBindsAndUnbindsOfEqualAddresses() throws Exception {

        IoHandler handler = new IoHandlerAdapter();
        BridgeSessionInitializer<? extends IoFuture> initializer = new BridgeSessionInitializerAdapter<>();
        String location = "tcp://localhost:8000";
        Set<Map.Entry<ResourceAddress, Bindings.Binding>> entries = bindings.entrySet();

        for (int i = 0; i < BIND_COUNT; i++) {
            // Add binding
            ResourceAddress bindAddress = makeResourceAddress(location);
            Bindings.Binding newBinding = new Bindings.Binding(bindAddress, handler, initializer);
            bindings.addBinding(newBinding);
            assertEquals("Failed to add a binding.", 1, entries.size());
            assertEquals(i+1, bindings.getBinding(bindAddress).referenceCount());
        }

        assertEquals("Adding equivalent bindings should only add one Binding object.", 1, entries.size());

        assertEquals(BIND_COUNT, bindings.getBinding( makeResourceAddress(location) ).referenceCount());

        for (int i = 0; i < BIND_COUNT; i++) {
            // Remove binding
            ResourceAddress unbindAddress = makeResourceAddress(location);
            boolean removed =bindings.removeBinding(unbindAddress, bindings.getBinding(makeResourceAddress(location)));
            if ( 10-i-1 > 0) {
                assertFalse(removed);
                assertEquals(10-i-1, bindings.getBinding(unbindAddress).referenceCount());
            } else {
                assertTrue(removed);
                Assert.assertNull(bindings.getBinding(unbindAddress));
            }
        }
        assertEmpty(entries);
    }


    @Test
    public void shouldBeAbleToBindAndUnBindDistinctAddressesAndHaveReferenceCountsCorrect() throws Exception {

        Bindings.Binding[] testBindings = new Bindings.Binding[10];
        int bindCounts[] = new int[10];

        // Create bindings
        for (int i = 0; i < 10; i++) {
            String location = String.format("tcp://localhost:%d", i);
            testBindings[i] = new Bindings.Binding(makeResourceAddress(location),
                    new IoHandlerAdapter(), new BridgeSessionInitializerAdapter<>());
        }

        // choose random bind counts
        for (int i = 0; i < 10; i++) {
            bindCounts[i] = (int) (Math.random()*7+2);
        }

        // bind each address a random number of times.
        for (int i = 0; i < 10; i++) {
            for ( int j = 0; j < bindCounts[i]; j++) {
                bindings.addBinding(testBindings[i]);
                assertEquals(j+1,testBindings[i].referenceCount());
            }
        }

        // check reference counts
        for (int i = 0; i < 10; i++) {
            Bindings.Binding testBinding = bindings.getBinding(testBindings[i].bindAddress());
            assertEquals(bindCounts[i], testBinding.referenceCount());
        }

        // unbind addresses
        for (int i = 0; i < 10; i++) {
            for ( int j = 0; j < bindCounts[i]; j++) {
                ResourceAddress unbindAddress = testBindings[i].bindAddress();
                boolean removed = bindings.removeBinding(unbindAddress, testBindings[i]);

                if (bindCounts[i]-j-1 > 0) {
                    assertFalse(removed);
                    assertEquals(bindCounts[i] - j - 1, bindings.getBinding(unbindAddress).referenceCount());
                } else {
                    assertTrue(removed);
                    Assert.assertNull(bindings.getBinding(unbindAddress));
                }
            }
        }

        assertEmpty(bindings.entrySet());

    }

    private ResourceAddress makeResourceAddress(String location) {
        return addressFactory.newResourceAddress(location);
    }

    public static <T extends Collection<?>> void assertEmpty(T collection) {
        assertThat(collection, new IsCollectionEmpty<>());
    }

    private static final class IsCollectionEmpty<T extends Collection<?>> extends BaseMatcher<T> {
        @Override
        public void describeTo(Description description) {
            description.appendText("is empty");
        }

        @Override
        @SuppressWarnings("rawtypes")
        public boolean matches(Object that) {
            return (that != null) && ((Collection) that).isEmpty();
        }
    }
}

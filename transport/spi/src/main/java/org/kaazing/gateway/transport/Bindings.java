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

import static org.kaazing.gateway.resource.address.Comparators.compareResourceOriginPathAlternatesAndProtocolStack;
import static org.kaazing.gateway.resource.address.ResourceAddress.NEXT_PROTOCOL;
import static java.lang.String.format;
import static java.lang.System.identityHashCode;

import java.net.URI;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.mina.core.future.IoFuture;
import org.apache.mina.core.service.IoHandler;

import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.transport.Bindings.Binding;

public abstract class Bindings<B extends Binding> {

    private final ConcurrentNavigableMap<ResourceAddress, B> bindings;

    protected Bindings(Comparator<ResourceAddress> comparator) {
        bindings = new ConcurrentSkipListMap<>(comparator);
    }

    public boolean isEmpty() {
        return bindings.isEmpty();
    }

    /**
     * @return the existing (old) binding  when the newBinding clashes with the existing binding,
     *         null if the newBinding has been recorded successfully.
     */
    public abstract Binding addBinding(Binding newBinding);
    
    public abstract Binding getBinding(ResourceAddress address);

    public boolean hasAnyBinding(
        ResourceAddress address)
    {
        return bindings.containsKey(address);
    }

    /**
     * @return true when the last binding has been removed (reference count 0)
     */
    public abstract boolean removeBinding(ResourceAddress address, Binding binding);
    
    @Override
    public String toString() {
        Iterator<Map.Entry<ResourceAddress,B>> i = entrySet().iterator();
        if (! i.hasNext()) {
            return "{}";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        for (;;) {
            Map.Entry<ResourceAddress, B> e = i.next();
            ResourceAddress key = e.getKey();
            B value = e.getValue();
            URI location = key.getResource();
            String nextProtocol = key.getOption(ResourceAddress.NEXT_PROTOCOL);
            String nextProtocolStr = nextProtocol == null ? "" : " " + nextProtocol;
            sb.append("  ").append('[').append(location).append(nextProtocolStr).append(']');
            sb.append('=');
            sb.append(value);
            if (! i.hasNext())
                return sb.append("\n}").toString();
            sb.append(',').append('\n');
        }
    }
    
    protected Set<Map.Entry<ResourceAddress, B>> entrySet() {
        return bindings.entrySet();
    }
    
    protected final B addBinding0(B newBinding) {
        ResourceAddress bindAddress = newBinding.bindAddress();
        B oldBinding = bindings.get(bindAddress);
        if (oldBinding != null) {
            IoHandler newHandler = newBinding.handler();
            IoHandler oldHandler = oldBinding.handler();
            if (oldHandler != null && newHandler != oldHandler) {
                throw new RuntimeException("Tried to bind address "+bindAddress+" to a different handler (old,new) = ("+oldHandler+","+newHandler+")");
            }
            oldBinding.incrementReferenceCount();
            return null;
        }

        // bind address to handler
        B binding = bindings.putIfAbsent(bindAddress, newBinding);
        if (binding != null) {
            if (equivalent(newBinding, binding)) {
                binding.incrementReferenceCount();
                binding = null;
            }
        } else {
            newBinding.incrementReferenceCount();
        }
        return binding;
    }

    protected final boolean equivalent(Binding newBinding, Binding binding) {
        return binding.bindAddress().equals(newBinding.bindAddress()) && binding.handler() == newBinding.handler();
    }

    // note: needed for usage of NextProtocolBindings
    public final B getBinding0(ResourceAddress address) {
        return bindings.get(address);
    }

    protected final boolean removeBinding0(ResourceAddress address, B binding) {
        B binding0 = getBinding0(address);
        if ( binding0 != null && binding.equals(binding0)) {  // we are going to remove, so decrement is OK
            if ( binding.decrementReferenceCount() == 0) {
                return bindings.remove(address, binding);
            }
        }
        return false;
    }

    public static class Default extends Bindings<Binding> {

        private static final Comparator<ResourceAddress> BINDINGS_COMPARATOR =
                compareResourceOriginPathAlternatesAndProtocolStack();

        public Default() {
            super(BINDINGS_COMPARATOR);
        }
        
        @Override
        public Binding addBinding(Binding newBinding) {
            return addBinding0(newBinding);
        }

        @Override
        public Binding getBinding(ResourceAddress address) {
            return getBinding0(address);
        }

        @Override
        public boolean removeBinding(ResourceAddress address, Binding binding) {
            return removeBinding0(address, binding);
        }
        
    }

    // create binding object that contains initializer and handler
    public static class Binding {
        private final ResourceAddress bindAddress;
        private final IoHandler handler;
        private final BridgeSessionInitializer<? extends IoFuture> initializer;
        private final AtomicInteger referenceCount;

        public Binding(ResourceAddress bindAddress, IoHandler handler) {
            this(bindAddress, handler, null);
        }
        
        public Binding(ResourceAddress bindAddress, IoHandler handler, BridgeSessionInitializer<? extends IoFuture> initializer) {
            if (bindAddress == null) {
                throw new NullPointerException("bindAddress");
            }
            if (handler == null) {
                throw new NullPointerException("handler");
            }
            this.bindAddress = bindAddress;
            this.handler = handler;
            this.initializer = initializer;
            this.referenceCount = new AtomicInteger(0);
        }
        
        protected Binding(ResourceAddress bindAddress) {
            if (bindAddress == null) {
                throw new NullPointerException("bindAddress");
            }
            // no handler null check for subclasses
            this.bindAddress = bindAddress;
            this.handler = null;
            this.initializer = null;
            this.referenceCount = new AtomicInteger(0);
        }

        public final ResourceAddress bindAddress() {
            return bindAddress;
        }
        
        public final IoHandler handler() {
            return handler;
        }
        
        public final BridgeSessionInitializer<? extends IoFuture> initializer() {
            return initializer;
        }

        public final int referenceCount() {
            return referenceCount.get();
        }

        public final int incrementReferenceCount() {
            return referenceCount.incrementAndGet();
        }

        public final int decrementReferenceCount() {
            return referenceCount.decrementAndGet();
        }

        @Override
        public int hashCode() {
            int hashCode = bindAddress.hashCode();
            if (handler != null) {
                hashCode = hashCode * 31 + handler.hashCode();
            }
            if (initializer != null) {
                hashCode = hashCode * 31 + initializer.hashCode();
            }
            return hashCode;
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            }
            
            if (!(o instanceof Binding)) {
                return false;
            }
            
            Binding that = (Binding)o;
            return equals(that);
        }

        @Override
        public String toString() {
            String nextProtocol = bindAddress.getOption(NEXT_PROTOCOL);
            nextProtocol = (nextProtocol == null ? "" : " "+nextProtocol);
            return format("([%s%s],0x%x,0x%x,#%d)",
                    bindAddress.getResource(),
                    nextProtocol,
                    identityHashCode(handler),
                    identityHashCode(initializer),
                    referenceCount());
        }
        
        protected final boolean equals(Binding that) {
            return this == that ||
                    (this.bindAddress.equals(that.bindAddress) &&
                     this.handler == that.handler &&
                     this.initializer == that.initializer);
        }
    }
}

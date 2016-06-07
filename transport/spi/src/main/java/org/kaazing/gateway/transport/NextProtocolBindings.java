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

import static org.kaazing.gateway.resource.address.ResourceAddress.NEXT_PROTOCOL;

import java.net.URI;
import java.util.Comparator;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicReference;

import org.kaazing.gateway.resource.address.Comparators;
import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.transport.NextProtocolBindings.NextProtocolBinding;

public class NextProtocolBindings extends Bindings<NextProtocolBinding> {

    public NextProtocolBindings() {
        super(Comparators.compareResourceLocationAndTransportProtocolStack());
    }

    public NextProtocolBindings(Comparator<ResourceAddress> comparator) {
        super(comparator);
    }

    public NextProtocolBinding getProtocolBinding(ResourceAddress bindAddress) {
        return super.getBinding0(bindAddress);
    }

    @Override
    public final Binding addBinding(Binding newBinding) {
        ResourceAddress bindAddress = newBinding.bindAddress();
        NextProtocolBinding nextBinding = super.getBinding0(bindAddress);

        if (nextBinding == null) {
            NextProtocolBinding newNextBinding = new NextProtocolBinding(bindAddress);
            nextBinding = addBinding1(newNextBinding);
            if (nextBinding == null) {
                nextBinding = newNextBinding;
            }
        }

        return nextBinding.addBinding(newBinding);
    }

    protected NextProtocolBinding addBinding1(NextProtocolBinding newNextBinding) {
        return super.addBinding0(newNextBinding);
    }

    @Override
    public Binding getBinding(ResourceAddress address) {

        NextProtocolBinding nextBinding = super.getBinding0(address);
        if (nextBinding != null) {
            return nextBinding.getBinding(address);
        }

        return null;
    }

    @Override
    public final boolean removeBinding(ResourceAddress address, Binding oldBinding) {
        NextProtocolBinding nextBinding = super.getBinding0(address);
        if (nextBinding != null) {
            if (nextBinding.removeBinding(address, oldBinding) && nextBinding.getNextProtocolNames().size() == 0) {
                removeBinding1(address, nextBinding);
                return true;
            }
        }
        return false;
    }

    protected void removeBinding1(ResourceAddress address, NextProtocolBinding binding) {
        super.removeBinding0(address, binding);
    }

    public String toString() {
        Set<Entry<ResourceAddress, NextProtocolBinding>> entries = entrySet();
        if (entries.isEmpty()) {
            return "{}";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        if (!entries.isEmpty()) {
            for (Entry<ResourceAddress, NextProtocolBinding> entry : entries) {
                ResourceAddress key = entry.getKey();
                NextProtocolBinding value = entry.getValue();
                URI location = key.getResource();
                sb.append("  ").append('[').append(location.resolve("/")).append(']');
                sb.append(" = ");
                sb.append(value);
                sb.append(',').append('\n');
            }
            sb.setLength(sb.length() - 2);
        }
        return sb.append("\n}").toString();
    }

    public class NextProtocolBinding extends Bindings.Binding {

        private final ConcurrentNavigableMap<String, Binding> nextProtocols;
        private final AtomicReference<Binding> nullNextProtocol;

        NextProtocolBinding(ResourceAddress bindAddress) {
            super(bindAddress);
            nextProtocols = new ConcurrentSkipListMap<>();
            nullNextProtocol = new AtomicReference<>();
        }

        public Binding addBinding(Binding newBinding) {
            ResourceAddress bindAddress = newBinding.bindAddress();
            String nextProtocol = bindAddress.getOption(NEXT_PROTOCOL);

            if (nextProtocol == null) {
                if (nullNextProtocol.compareAndSet(null, newBinding)) {
                    newBinding.incrementReferenceCount();
                    return null;
                }
                Binding oldBinding = nullNextProtocol.get();
                if (equivalent(newBinding, oldBinding)) {
                    oldBinding.incrementReferenceCount();
                    return null;
                }
                return oldBinding;
            }

            Binding oldBinding = nextProtocols.putIfAbsent(nextProtocol, newBinding);
            if (oldBinding == null) {
                newBinding.incrementReferenceCount();
            }
            else if (equivalent(newBinding, oldBinding)) {
                oldBinding.incrementReferenceCount();
                oldBinding = null;
            }
            return oldBinding;
        }

        public Binding getBinding(ResourceAddress address) {
            String nextProtocol = address.getOption(NEXT_PROTOCOL);

            if (nextProtocol == null) {
                return nullNextProtocol.get();
            }
            
            return nextProtocols.get(nextProtocol);
        }

        public boolean removeBinding(ResourceAddress address, Binding binding) {
            ResourceAddress bindAddress = binding.bindAddress();
            String nextProtocol = bindAddress.getOption(NEXT_PROTOCOL);

            if (nextProtocol == null) {
                Binding oldBinding = nullNextProtocol.get();
                if (equivalent(oldBinding, binding)) {
                    binding = oldBinding;
                }
                if (binding.decrementReferenceCount() == 0) {
                    return nullNextProtocol.compareAndSet(binding, null);
                }
                return false;
            }

            Binding oldBinding = nextProtocols.get(nextProtocol);
            if (equivalent(oldBinding, binding)) {
                binding = oldBinding;
            }
            if (binding.decrementReferenceCount() == 0) {
                return nextProtocols.remove(nextProtocol, binding);
            }
            return false;
        }

        public boolean hasNullNextProtocol() {
            return (nullNextProtocol.get() != null);
        }

        public SortedSet<String> getNextProtocolNames() {
            return nextProtocols.keySet();
        }

        @Override
        public int hashCode() {
            int hashCode = super.hashCode();
            hashCode = hashCode * 31 + nextProtocols.hashCode();
            hashCode = hashCode * 31 + nullNextProtocol.hashCode();
            return hashCode;
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            }

            if (!(o instanceof NextProtocolBinding)) {
                return false;
            }

            NextProtocolBinding that = (NextProtocolBinding) o;
            return equals(that);
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("{");

            int mark = sb.length();
            Binding nullNextProtocolSnapshot = nullNextProtocol.get();
            if (nullNextProtocolSnapshot != null) {
                sb.append("null: ");
                ResourceAddress bindAddress = nullNextProtocolSnapshot.bindAddress();
                sb.append(bindAddress);
                sb.append(", ");
            }

            for (Binding binding : nextProtocols.values()) {
                ResourceAddress bindAddress = binding.bindAddress();
                sb.append(bindAddress);
                sb.append(", ");
            }

            if (sb.length() > mark) {
                // rewind last ", "
                sb.setLength(sb.length() - 2);
            }

            sb.append('}');

            return sb.toString();
        }

        protected final boolean equals(NextProtocolBinding that) {
            return this.nullNextProtocol.equals(that.nullNextProtocol) && 
                    this.nextProtocols.equals(that.nextProtocols) && 
                    super.equals(that);
        }
    }
}

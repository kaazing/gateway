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

import java.net.URI;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.NavigableSet;
import java.util.Set;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

import org.kaazing.gateway.resource.address.Comparators;
import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.resource.address.uri.URIUtils;
import org.kaazing.gateway.transport.Bindings;
import org.kaazing.gateway.transport.http.HttpBindings.HttpBinding;

/**
 *
 */
public class HttpBindings extends Bindings<HttpBinding> {

    private static final Comparator<ResourceAddress> BINDINGS_COMPARATOR = Comparators.compareResourceOriginAndProtocolStack();

    public HttpBindings() {
        super(BINDINGS_COMPARATOR);
    }

    @Override
    public Binding addBinding(Binding newBinding) {
        ResourceAddress bindAddress = newBinding.bindAddress();
        HttpBinding httpBinding = getBinding0(bindAddress);
        if (httpBinding == null) {
            HttpBinding newHttpBinding = new HttpBinding(bindAddress);
            httpBinding = bindAdditionalAddressesIfNecessary(newHttpBinding);
            if (httpBinding == null) {
                httpBinding = newHttpBinding;
            }
        }
        
        URI location = bindAddress.getResource();
        String path = location.getPath();
        Binding binding = httpBinding.put(path, newBinding);
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
    
    protected HttpBinding bindAdditionalAddressesIfNecessary(HttpBinding newHttpBinding) {
        return addBinding0(newHttpBinding);
    }


    protected boolean unbindAdditionalAddressesIfNecessary(ResourceAddress address, HttpBinding newHttpBinding) {
        return false;
    }

    @Override
    public Binding getBinding(ResourceAddress address) {
        
        HttpBinding httpBinding = super.getBinding0(address);
        if (httpBinding != null) {
            URI location = address.getResource();
            String path = location.getPath();
            return httpBinding.get(path);
        }
        
        return null;
    }

    @Override
    public boolean removeBinding(ResourceAddress address, Binding oldBinding) {
        HttpBinding httpBinding = super.getBinding0(address);
        if ( httpBinding != null ) {
            URI location = address.getResource();
            String path = location.getPath();
            if (httpBinding.remove(path, oldBinding)) {
                unbindAdditionalAddressesIfNecessary(address, httpBinding);
                if (httpBinding.isEmpty()) {
                    return super.removeBinding0(address, httpBinding);
                }
            }
        }
        return false;
    }

    public String toString() {
        Iterator<Entry<ResourceAddress,HttpBinding>> i = entrySet().iterator();
        if (! i.hasNext())
            return "{}";

        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        for (;;) {
            Entry<ResourceAddress, HttpBinding> e = i.next();
            ResourceAddress key = e.getKey();
            HttpBinding value = e.getValue();
            String location = key.getExternalURI();
            String nextProtocol = key.getOption(ResourceAddress.NEXT_PROTOCOL);
            String nextProtocolStr = nextProtocol == null ? "" : " " + nextProtocol;
            sb.append("  ").append('[').append(URIUtils.resolve(location, "/")).append(nextProtocolStr).append(']');
            sb.append('=');
            sb.append(value);
            if (! i.hasNext()) {
                return sb.append("\n}").toString();
            }
            sb.append(',').append('\n');
        }
    }

    @Override
    protected Set<Entry<ResourceAddress, HttpBinding>> entrySet() {
        return super.entrySet();
    }

    public static class HttpBinding extends org.kaazing.gateway.transport.Bindings.Binding {

        private static final Comparator<String> PATH_ASCENDING = new Comparator<String>() {

            @Override
            public int compare(String path1, String path2) {

                // TODO: optimize by inspecting string directly rather than splitting into arrays
                String[] segments1 = path1.split("/");
                String[] segments2 = path2.split("/");

                int nSegments = Math.min(segments1.length, segments2.length);
                int comparison = 0;
                for (int i=0; i < nSegments; i++) {
                    if (comparison != 0) {
                        break;
                    }
                    comparison = segments1[i].compareTo(segments2[i]);
                }

                if (comparison == 0) {
                    return (segments1.length - segments2.length);
                }
                
                return comparison;
            }

        };

        private final ConcurrentNavigableMap<String, Binding> bindingsByPath;

        @Override
        public int hashCode() {
            int hashCode = super.hashCode();
            hashCode = hashCode * 31 + bindingsByPath.hashCode();
            return hashCode;
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            }
            
            if (!(o instanceof HttpBinding)) {
                return false;
            }
            
            HttpBinding that = (HttpBinding)o;
            return equals(that);
        }
        
        @Override
        public String toString() {
            Iterator<Entry<String,Binding>> i = bindingsByPath.entrySet().iterator();
            if (! i.hasNext())
                return super.toString();

            StringBuilder sb = new StringBuilder();
            sb.append("(\n");
            for (;;) {
                Entry<String, Binding> e = i.next();
                String key = e.getKey();
                Binding value = e.getValue();
                sb.append("    ").append(key);
                sb.append('=');
                sb.append(value);
                if (! i.hasNext())
                    return sb.append("\n    #").append(referenceCount()).append(")").toString();
                sb.append(';').append('\n');
            }
        }
        
        protected final boolean equals(HttpBinding that) {
            return super.equals(that) &&
                    this.bindingsByPath.equals(that.bindingsByPath);
        }

        HttpBinding(ResourceAddress bindAddress) {
            super(bindAddress);
            bindingsByPath = new ConcurrentSkipListMap<>(PATH_ASCENDING);
        }
        
        boolean isEmpty() {
            return bindingsByPath.isEmpty();
        }

        int size() {
            return bindingsByPath.size();
        }

        Binding get(String path)  {
            NavigableSet<String> paths = bindingsByPath.navigableKeySet();
            NavigableSet<String> candidatePaths = paths.headSet(path, true);

            for (Iterator<String> $i = candidatePaths.descendingIterator(); $i.hasNext(); ) {
                String candidatePath = $i.next();
                if (path.startsWith(candidatePath)) {
                    return bindingsByPath.get(candidatePath);
                }
            }
            
            return null;
        }

        Binding put(String path, Binding binding)  {
            return bindingsByPath.putIfAbsent(path, binding);
        }

        boolean remove(String path, Binding binding)  {
            if ( binding == (bindingsByPath.get(path))) {
                if (binding != null && binding.decrementReferenceCount() == 0 ) {
                    return bindingsByPath.remove(path, binding);
                }
            }
            return false;
        }
    }
}

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
package org.kaazing.gateway.transport.wsn;

import java.util.Comparator;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.mina.core.future.IoFuture;
import org.apache.mina.core.service.IoHandler;
import org.kaazing.gateway.resource.address.Comparators;
import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.transport.Bindings;
import org.kaazing.gateway.transport.BridgeSessionInitializer;

public class WsnBindings extends Bindings<WsnBindings.WsnBinding> {

    private static final Comparator<ResourceAddress> BINDINGS_COMPARATOR =
            Comparators.compareResourceOriginPathAndProtocolStack();

    public WsnBindings() {
        super(BINDINGS_COMPARATOR);
    }

    @Override
    public Binding addBinding(Binding newBinding) {
        ResourceAddress bindAddress = newBinding.bindAddress();
        WsnBinding wsnBinding = getBinding0(bindAddress);
        if (wsnBinding == null) {
            wsnBinding = new WsnBinding(newBinding.bindAddress(),
                    newBinding.handler(),
                    newBinding.initializer());
        }
        return super.addBinding0(wsnBinding);
    }


    @Override
    public Binding getBinding(ResourceAddress address) {
        return super.getBinding0(address);
    }

    @Override
    public boolean removeBinding(ResourceAddress address, Binding binding) {
        WsnBinding wsnBinding = super.getBinding0(address);
        return wsnBinding != null && removeBinding0(address, wsnBinding);
    }

    public static class WsnBinding extends Bindings.Binding {
        public WsnBinding(ResourceAddress bindAddress, IoHandler handler) {
            super(bindAddress, handler);
        }

        public WsnBinding(ResourceAddress bindAddress,
                          IoHandler handler,
                          BridgeSessionInitializer<? extends IoFuture> initializer) {
            super(bindAddress, handler, initializer);
        }
    }

    @Override
    protected Set<Entry<ResourceAddress, WsnBinding>> entrySet() {
        return super.entrySet();
    }

}
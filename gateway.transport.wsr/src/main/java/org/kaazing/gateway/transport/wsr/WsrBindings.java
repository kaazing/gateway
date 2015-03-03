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

package org.kaazing.gateway.transport.wsr;

import java.util.Comparator;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.mina.core.future.IoFuture;
import org.apache.mina.core.service.IoHandler;
import org.kaazing.gateway.resource.address.Comparators;
import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.transport.Bindings;
import org.kaazing.gateway.transport.BridgeSessionInitializer;

public class WsrBindings extends Bindings<WsrBindings.WsrBinding> {

    private static final Comparator<ResourceAddress> BINDINGS_COMPARATOR =
            Comparators.compareResourceOriginPathAlternatesAndProtocolStack();

    public WsrBindings() {
        super(BINDINGS_COMPARATOR);
    }

    @Override
    public Binding addBinding(Binding newBinding) {
        ResourceAddress bindAddress = newBinding.bindAddress();
        WsrBinding wsrBinding = getBinding0(bindAddress);
        if (wsrBinding == null) {
            wsrBinding = new WsrBinding(newBinding.bindAddress(),
                    newBinding.handler(),
                    newBinding.initializer());
        }
        return super.addBinding0(wsrBinding);
    }


    @Override
    public Binding getBinding(ResourceAddress address) {
        return super.getBinding0(address);
    }

    @Override
    public boolean removeBinding(ResourceAddress address, Binding binding) {
        WsrBinding wsrBinding = super.getBinding0(address);
        return wsrBinding != null && removeBinding0(address, wsrBinding);
    }

    public static class WsrBinding extends Bindings.Binding {
        public WsrBinding(ResourceAddress bindAddress, IoHandler handler) {
            super(bindAddress, handler);
        }

        public WsrBinding(ResourceAddress bindAddress,
                          IoHandler handler,
                          BridgeSessionInitializer<? extends IoFuture> initializer) {
            super(bindAddress, handler, initializer);
        }
    }

    @Override
    protected Set<Entry<ResourceAddress, WsrBinding>> entrySet() {
        return super.entrySet();
    }

}
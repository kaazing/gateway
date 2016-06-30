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

import static org.kaazing.gateway.resource.address.ResourceAddress.ALTERNATE;
import static org.kaazing.gateway.resource.address.ResourceAddress.BIND_ALTERNATE;
import static org.kaazing.gateway.transport.BridgeSession.LOCAL_ADDRESS;
import static java.lang.String.format;

import java.net.URI;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.mina.core.future.IoFuture;
import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.session.IoSessionInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.transport.Bindings.Binding;
import org.kaazing.mina.core.future.DefaultUnbindFuture;
import org.kaazing.mina.core.future.UnbindFuture;
import org.kaazing.mina.core.service.IoProcessorEx;
import org.kaazing.mina.core.session.IoSessionConfigEx;

public abstract class AbstractBridgeAcceptor<T extends AbstractBridgeSession<?,?>, B extends Binding> extends AbstractBridgeService<T> implements
        BridgeAcceptor {

    protected final Bindings<B> bindings;
    protected final Logger logger;
    protected final AtomicBoolean started;

    public AbstractBridgeAcceptor(IoSessionConfigEx sessionConfig) {
        super(sessionConfig);

        this.bindings = initBindings();
        this.logger = LoggerFactory.getLogger(String.format("transport.%s.accept", getTransportMetadata().getName()));
        this.started = new AtomicBoolean(false);
    }


    public boolean emptyBindings() {
        return bindings.isEmpty();
    }

    protected abstract Bindings<B> initBindings();

    @Override
    public IoHandler getHandler(ResourceAddress address) {
        Binding binding = bindings.getBinding(address);
        if (binding != null) {
            return binding.handler();
        }
        System.out.println(String.format("ERROR in getHandler: bindings: %s\naddress: %s", bindings, address));
        return null;
    }

    protected IoSessionInitializer<?> getInitializer(ResourceAddress address) {
        Binding binding = bindings.getBinding(address);
        if (binding != null) {
            return binding.initializer();
        }
        return null;
    }

    @Override
    protected IoProcessorEx<T> initProcessor() {
        return new BridgeAcceptProcessor<>();
    }

    @Override
    protected IoHandler initHandler() {
        return new BridgeAcceptHandler(this);
    }

    protected abstract boolean canBind(String transportName);

    // note: relax final for WebSocket balancer initializer (temporary)
    @Override
    public /*final*/ void bind(ResourceAddress address, IoHandler handler, BridgeSessionInitializer<? extends IoFuture> initializer) {

        // bind only address with matching scheme
        URI location = address.getResource();
        String schemeName = location.getScheme();
        if (!canBind(schemeName)) {
            throw new IllegalArgumentException(format("Unexpected scheme \"%s\" for URI: %s", schemeName, location));
        }
        
        if (!started.get()) {
            synchronized (started) {
                if (!started.get()) {
                    init();
                    started.set(true);
                }
            }
        }

        boolean bindAlternate;
        do {
            bindAlternate = address.getOption(BIND_ALTERNATE);
            //
            // add binding, expecting no clashing (according to BINDINGS_COMPARATOR)
            //
            Binding newBinding = new Binding(address, handler, initializer);
            Binding oldBinding = bindings.addBinding(newBinding);

            //System.out.println(getClass().getSimpleName()+"@"+hashCode()+" binding: "+address.getExternalURI()+" -- "+address.getOption(NEXT_PROTOCOL));

            if (oldBinding != null) {
                throw new RuntimeException("Unable to bind address " + address
                        + " because it collides with an already bound address " + oldBinding.bindAddress());
            }

            bindInternal(address, handler, initializer);
            address = address.getOption(ALTERNATE);
            
        } while (address != null && bindAlternate);
    }

    protected abstract <F extends IoFuture>
    void bindInternal(ResourceAddress address, IoHandler handler, BridgeSessionInitializer<F> initializer);

    @Override
    public final UnbindFuture unbind(ResourceAddress address) {
        UnbindFuture future = null;

        boolean unbindAlternate;
        do {

            //System.out.println(getClass().getSimpleName()+"@"+hashCode()+" unbinding: "+address.getExternalURI()+" -- "+address.getOption(NEXT_PROTOCOL));
            unbindAlternate = address.getOption(BIND_ALTERNATE);

            Binding binding = bindings.getBinding(address);
            bindings.removeBinding(address, binding);
            // Using address (instead of binding.bindAddress()) as two different addresses may have the same binding
            // due to alternates. For example: sse (with http transport) has alternate sse(with httpxe transport)
            // Say, they are A and its alternate B. A and B share the same binding as alternates comparator consider
            // A and B are equal. Unbinding A happens fine. While unbinding B, the binding.bindAddress() would be A
            // with alternate B and this would cause problems(as A is already used for unbinding).
            UnbindFuture newFuture = unbindInternal(address,
                    binding.handler(),
                    binding.initializer());

            if (future != null) {
                future = DefaultUnbindFuture.combineFutures(future, newFuture);
            }
            else {
                future = newFuture;
            }

            address = address.getOption(ALTERNATE);
            
        } while (address != null && unbindAlternate);
        
        return future;
    }

    protected abstract UnbindFuture unbindInternal(ResourceAddress address, IoHandler handler, 
                                                   BridgeSessionInitializer<? extends IoFuture> initializer);

    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    protected void finishSessionInitialization0(IoSession session, IoFuture future) {
        ResourceAddress address = LOCAL_ADDRESS.get(session);
        IoSessionInitializer initializer = getInitializer(address);
        if (initializer != null) {
            initializer.initializeSession(session, future);
        }
    }

    @Override
    protected Logger getLogger() {
        return logger;
    }

}

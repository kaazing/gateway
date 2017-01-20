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

import java.util.concurrent.Callable;
import java.util.concurrent.Executor;

import org.apache.mina.core.filterchain.IoFilter;
import org.apache.mina.core.filterchain.IoFilterChain;
import org.apache.mina.core.future.IoFuture;
import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.service.TransportMetadata;
import org.apache.mina.core.session.DefaultIoSessionDataStructureFactory;
import org.apache.mina.core.session.IoSessionInitializer;
import org.kaazing.mina.core.service.AbstractIoServiceEx;
import org.kaazing.mina.core.service.IoProcessorEx;
import org.kaazing.mina.core.session.IoSessionConfigEx;
import org.kaazing.mina.util.ExceptionMonitor;
import org.slf4j.Logger;

public abstract class AbstractBridgeService<T extends AbstractBridgeSession<?, ?>> extends AbstractIoServiceEx implements BridgeService {

    private IoProcessorEx<T> processor;

    protected AbstractBridgeService(IoSessionConfigEx sessionConfig) {
        super(sessionConfig, new Executor() {
            @Override
            public void execute(Runnable command) {}
        });

        setSessionDataStructureFactory(new DefaultIoSessionDataStructureFactory());
    }

    protected abstract Logger getLogger();

    protected void init() {
        processor = initProcessor();
        setHandler(initHandler());
    }

    protected abstract IoProcessorEx<T> initProcessor();

    protected abstract IoHandler initHandler();

    @Override
    public abstract TransportMetadata getTransportMetadata();

    // TODO: change to return void for 2.0.0-RCx upgrade
    @Override
    protected IoFuture dispose0() throws Exception {
        // TODO: remove return for 2.0.0-RCx upgrade
        return null;
    }

    protected T newSession(Callable<T> sessionCreator) throws Exception {
        return newSession(null, sessionCreator);
    }

    protected T newSession(IoSessionInitializer<? extends IoFuture> initializer, Callable<T> sessionCreator) throws Exception {
        return newSession(initializer, null, sessionCreator);
    }

    // wtb closures
    protected T newSession(IoSessionInitializer<? extends IoFuture> initializer, IoFuture future, Callable<T> sessionCreator) throws Exception {
        T session;

        IoProcessorEx<T> processor = getProcessor();
        synchronized (processor) {
            session = sessionCreator.call();
            processor.add(session);
        }

        initSession(session, future, initializer);
        IoFilterChain filterChain = session.getFilterChain();

        try {
            this.getFilterChainBuilder().buildFilterChain(filterChain);
        } catch (Throwable t) {
            ExceptionMonitor.getInstance().exceptionCaught(t, session);
        }

        addLoggerFilter(session, getLogger());

        getListeners().fireSessionCreated(session);
        return session;
    }

    private void addLoggerFilter(T session, Logger logger) {
        if (logger != null) {
            // setup logging filters for the new session
            LoggingFilter.addIfNeeded(logger, session, getTransportMetadata().getName());
            if (session.getParent() != null) {
                LoggingFilter.moveAfterCodec(session.getParent());
            }
        }
    }

    protected IoProcessorEx<T> getProcessor() {
        return processor;
    }

    public void removeBridgeFilters(IoFilterChain filterChain) {
    }

    public void addBridgeFilters(IoFilterChain filterChain) {
    }

    protected final void removeFilter(IoFilterChain filterChain, String name) {
        if (filterChain.contains(name)) {
            filterChain.remove(name);
        }
    }

    protected final void removeFilter(IoFilterChain filterChain, IoFilter filter) {
        if (filterChain.contains(filter)) {
            filterChain.remove(filter);
        }
    }

}

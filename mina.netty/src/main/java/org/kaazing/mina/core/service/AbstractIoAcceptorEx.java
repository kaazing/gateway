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
package org.kaazing.mina.core.service;

import java.net.SocketAddress;
import java.util.List;
import java.util.concurrent.Executor;

import org.apache.mina.core.future.IoFutureListener;

import org.kaazing.mina.core.future.BindFuture;
import org.kaazing.mina.core.future.UnbindFuture;
import org.kaazing.mina.core.session.IoSessionConfigEx;
import org.kaazing.mina.core.write.WriteRequestEx;
import org.kaazing.mina.core.write.DefaultWriteRequestEx.ShareableWriteRequest;

public abstract class AbstractIoAcceptorEx extends AbstractIoAcceptor implements IoAcceptorEx  {

    private final List<ThreadLocal<WriteRequestEx>> sharedWriteRequests = ShareableWriteRequest.initWithLayers(16);

    protected AbstractIoAcceptorEx(IoSessionConfigEx sessionConfig,
            Executor executor) {
        super(sessionConfig, executor);
    }

    @Override
    public IoSessionConfigEx getSessionConfig() {
        return (IoSessionConfigEx) super.getSessionConfig();
    }

    @Override
    public final ThreadLocal<WriteRequestEx> getThreadLocalWriteRequest(int ioLayer) {
        return sharedWriteRequests.get(ioLayer);
    }

    @Override
    // This is basically just an asynchronous version of AbstractIoAcceptor.bind(Iterable... localAddresses)
    public BindFuture bindAsync(final SocketAddress localAddress) {
        if (isDisposing()) {
            throw new IllegalStateException("Already disposed.");
        }
        if (localAddress == null) {
            throw new NullPointerException("localAddress");
        }
        checkAddressType(localAddress);
        if (getHandler() == null) {
            throw new IllegalStateException("handler is not set.");
        }
        BindFuture bound = bindAsyncInternal(localAddress);
        bound.addListener(new IoFutureListener<BindFuture>() {
            @Override
            public void operationComplete(BindFuture future) {
                if (future.isBound()) {
                    boolean activate = false;
                    synchronized (bindLock) {
                        if (boundAddresses.isEmpty()) {
                            activate = true;
                        }
                        boundAddresses.add(localAddress);
                        if (activate) {
                            getListeners().fireServiceActivated();
                        }
                    }
                }
            }
        });
        return bound;
    }

    @Override
    // This is basically just an asynchronous version of AbstractNioAcceptor.unbind(Iterable... localAddresses)
    public UnbindFuture unbindAsync(final SocketAddress localAddress) {
        UnbindFuture unbound = unbindAsyncInternal(localAddress);
        unbound.addListener(new IoFutureListener<UnbindFuture>() {
            @Override
            public void operationComplete(UnbindFuture future) {
                if (future.isUnbound()) {
                    boolean deactivate = false;
                    synchronized (bindLock) {
                        if (boundAddresses.isEmpty()) {
                            return;
                        }
                        boundAddresses.remove(localAddress);
                        if (boundAddresses.isEmpty()) {
                            deactivate = true;
                        }
                        if (deactivate) {
                            getListeners().fireServiceDeactivated();
                        }
                    }

                }
            }
        });
        return unbound;
    }

    protected abstract BindFuture bindAsyncInternal(SocketAddress localAddress);

    protected abstract UnbindFuture unbindAsyncInternal(SocketAddress localAddress);

}

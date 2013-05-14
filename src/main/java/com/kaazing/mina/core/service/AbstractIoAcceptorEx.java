/**
 * Copyright (c) 2007-2012, Kaazing Corporation. All rights reserved.
 */

package com.kaazing.mina.core.service;

import java.net.SocketAddress;
import java.util.concurrent.Executor;

import org.apache.mina.core.future.IoFutureListener;

import com.kaazing.mina.core.future.BindFuture;
import com.kaazing.mina.core.future.UnbindFuture;
import com.kaazing.mina.core.session.IoSessionConfigEx;

public abstract class AbstractIoAcceptorEx extends AbstractIoAcceptor implements IoAcceptorEx  {

    protected AbstractIoAcceptorEx(IoSessionConfigEx sessionConfig,
            Executor executor) {
        super(sessionConfig, executor);
    }

    @Override
    public IoSessionConfigEx getSessionConfig() {
        return (IoSessionConfigEx) super.getSessionConfig();
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

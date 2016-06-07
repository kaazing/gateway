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
package org.apache.mina.core.service;

import java.io.IOException;
import java.net.SocketAddress;
import java.util.List;
import java.util.Set;

import org.apache.mina.core.session.IoSession;

/**
 * Accepts incoming connection, communicates with clients, and fires events to
 * {@link IoHandler}s.
 * <p>
 * Please refer to
 * <a href="../../../../../xref-examples/org/apache/mina/examples/echoserver/Main.html">EchoServer</a>
 * example.
 * <p>
 * You should bind to the desired socket address to accept incoming
 * connections, and then events for incoming connections will be sent to
 * the specified default {@link IoHandler}.
 * <p>
 * Threads accept incoming connections start automatically when
 * {@link #bind()} is invoked, and stop when {@link #unbind()} is invoked.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public interface IoAcceptor extends IoService {
    /**
     * Returns the local address which is bound currently.  If more than one
     * address are bound, only one of them will be returned, but it's not
     * necessarily the firstly bound address.
     */
    SocketAddress getLocalAddress();
    
    /**
     * Returns a {@link Set} of the local addresses which are bound currently.
     */
    Set<SocketAddress> getLocalAddresses();

    /**
     * Returns the default local address to bind when no argument is specified
     * in {@link #bind()} method.  Please note that the default will not be
     * used if any local address is specified.  If more than one address are
     * set, only one of them will be returned, but it's not necessarily the
     * firstly specified address in {@link #setDefaultLocalAddresses(List)}.
     * 
     */
    SocketAddress getDefaultLocalAddress();
    
    /**
     * Returns a {@link List} of the default local addresses to bind when no
     * argument is specified in {@link #bind()} method.  Please note that the
     * default will not be used if any local address is specified.
     */
    List<SocketAddress> getDefaultLocalAddresses();

    /**
     * Sets the default local address to bind when no argument is specified in
     * {@link #bind()} method.  Please note that the default will not be used
     * if any local address is specified.
     */
    void setDefaultLocalAddress(SocketAddress localAddress);
    
    /**
     * Sets the default local addresses to bind when no argument is specified
     * in {@link #bind()} method.  Please note that the default will not be
     * used if any local address is specified.
     */
    void setDefaultLocalAddresses(SocketAddress firstLocalAddress, SocketAddress... otherLocalAddresses);
    
    /**
     * Sets the default local addresses to bind when no argument is specified
     * in {@link #bind()} method.  Please note that the default will not be
     * used if any local address is specified.
     */
    void setDefaultLocalAddresses(Iterable<? extends SocketAddress> localAddresses);

    /**
     * Sets the default local addresses to bind when no argument is specified
     * in {@link #bind()} method.  Please note that the default will not be
     * used if any local address is specified.
     */
    void setDefaultLocalAddresses(List<? extends SocketAddress> localAddresses);

        /**
     * Returns <tt>true</tt> if and only if all clients are closed when this
     * acceptor unbinds from all the related local address (i.e. when the
     * service is deactivated).
     */
    boolean isCloseOnDeactivation();

    /**
     * Sets whether all client sessions are closed when this acceptor unbinds
     * from all the related local addresses (i.e. when the service is
     * deactivated).  The default value is <tt>true</tt>.
     */
    void setCloseOnDeactivation(boolean closeOnDeactivation);

    /**
     * Binds to the default local address(es) and start to accept incoming
     * connections.
     *
     * @throws IOException if failed to bind
     */
    void bind() throws IOException;
    
    /**
     * Binds to the specified local address and start to accept incoming
     * connections.
     *
     * @throws IOException if failed to bind
     */
    void bind(SocketAddress localAddress) throws IOException;
    
    /**
     * Binds to the specified local addresses and start to accept incoming
     * connections. If no address is given, bind on the default local address.
     *
     * @throws IOException if failed to bind
     */
    void bind(SocketAddress firstLocalAddress, SocketAddress... addresses) throws IOException;
    
    /**
     * Binds to the specified local addresses and start to accept incoming
     * connections.
     *
     * @throws IOException if failed to bind
     */
    void bind(Iterable<? extends SocketAddress> localAddresses) throws IOException;
    
    /**
     * Unbinds from all local addresses that this service is bound to and stops
     * to accept incoming connections.  All managed connections will be closed
     * if {@link #setCloseOnDeactivation(boolean) disconnectOnUnbind} property
     * is <tt>true</tt>.  This method returns silently if no local address is
     * bound yet.
     */
    void unbind();
    
    /**
     * Unbinds from the specified local address and stop to accept incoming
     * connections.  All managed connections will be closed if
     * {@link #setCloseOnDeactivation(boolean) disconnectOnUnbind} property is
     * <tt>true</tt>.  This method returns silently if the default local
     * address is not bound yet.
     */
    void unbind(SocketAddress localAddress);
    
    /**
     * Unbinds from the specified local addresses and stop to accept incoming
     * connections.  All managed connections will be closed if
     * {@link #setCloseOnDeactivation(boolean) disconnectOnUnbind} property is
     * <tt>true</tt>.  This method returns silently if the default local
     * addresses are not bound yet.
     */
    void unbind(SocketAddress firstLocalAddress, SocketAddress... otherLocalAddresses);
    
    /**
     * Unbinds from the specified local addresses and stop to accept incoming
     * connections.  All managed connections will be closed if
     * {@link #setCloseOnDeactivation(boolean) disconnectOnUnbind} property is
     * <tt>true</tt>.  This method returns silently if the default local
     * addresses are not bound yet.
     */
    void unbind(Iterable<? extends SocketAddress> localAddresses);
    
    /**
     * (Optional) Returns an {@link IoSession} that is bound to the specified
     * <tt>localAddress</tt> and the specified <tt>remoteAddress</tt> which
     * reuses the local address that is already bound by this service.
     * <p>
     * This operation is optional.  Please throw {@link UnsupportedOperationException}
     * if the transport type doesn't support this operation.  This operation is
     * usually implemented for connectionless transport types.
     *
     * @throws UnsupportedOperationException if this operation is not supported
     * @throws IllegalStateException if this service is not running.
     * @throws IllegalArgumentException if this service is not bound to the
     *                                  specified <tt>localAddress</tt>.
     */
    IoSession newSession(SocketAddress remoteAddress, SocketAddress localAddress);
}
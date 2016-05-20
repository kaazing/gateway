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
package org.apache.mina.transport.socket;

import java.net.InetSocketAddress;
import java.net.ServerSocket;

import org.apache.mina.core.service.IoAcceptor;

/**
 * {@link IoAcceptor} for socket transport (TCP/IP).  This class
 * handles incoming TCP/IP based socket connections.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public interface SocketAcceptor extends IoAcceptor {
    InetSocketAddress getLocalAddress();
    InetSocketAddress getDefaultLocalAddress();
    void setDefaultLocalAddress(InetSocketAddress localAddress);

    /**
     * @see ServerSocket#getReuseAddress()
     */
    boolean isReuseAddress();

    /**
     * @see ServerSocket#setReuseAddress(boolean)
     */
    void setReuseAddress(boolean reuseAddress);

    /**
     * Returns the size of the backlog.
     */
    int getBacklog();

    /**
     * Sets the size of the backlog.  This can only be done when this
     * class is not bound
     */
    void setBacklog(int backlog);
    
    /**
     * Returns the default configuration of the new SocketSessions created by 
     * this acceptor service.
     */
    SocketSessionConfig getSessionConfig();
}

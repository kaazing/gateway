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
/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */

/* This class has the following differences from NioDatagramSessionConfig in Mina 2.0.0-RC1:
 * 1. Use our XxxEx classes instead of Xxx in order to create sessions which implement IoSessionEx.
 * 3. Changes made in our Mina patch (2.0.0-RC1g):
 *    - do nothing in setSendBufferSize unless a special property is set
 */
package org.apache.mina.transport.socket.nio;

import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.channels.DatagramChannel;

import org.apache.mina.core.RuntimeIoException;
import org.apache.mina.transport.socket.AbstractDatagramSessionConfigEx;

/**
 * Define the configuration for a Datagram based session.
 */
class NioDatagramSessionConfigEx extends AbstractDatagramSessionConfigEx {
    /** The associated channel */
    private final DatagramChannel channel;

    /**
     * Creates a new instance of NioDatagramSessionConfig, associated
     * with the given DatagramChannel.
     *
     * @param channel The associated DatagramChannel
     */
    NioDatagramSessionConfigEx(DatagramChannel channel) {
        this.channel = channel;
    }

    /**
     * Get the Socket receive buffer size for this DatagramChannel.
     *
     * @return the DatagramChannel receive buffer size.
     * @throws RuntimeIoException if the socket is closed or if we
     * had a SocketException
     *
     * @see DatagramSocket#getReceiveBufferSize()
     */
    @Override
    public int getReceiveBufferSize() {
        try {
            return channel.socket().getReceiveBufferSize();
        } catch (SocketException e) {
            throw new RuntimeIoException(e);
        }
    }

    /**
     * Set the Socket receive buffer size for this DatagramChannel. <br>
     * <br>
     * Note : The underlying Socket may not accept the new buffer's size.
     * The user has to check that the new value has been set.
     *
     * @param receiveBufferSize the DatagramChannel receive buffer size.
     * @throws RuntimeIoException if the socket is closed or if we
     * had a SocketException
     *
     * @see DatagramSocket#setReceiveBufferSize()
     */
    @Override
    public void setReceiveBufferSize(int receiveBufferSize) {
        try {
            if (ENABLE_BUFFER_SIZE) {
                System.out.println("NioDatagramSessionConfig.setReceiveBufferSize(" + receiveBufferSize + ")");
                channel.socket().setReceiveBufferSize(receiveBufferSize);
            }
        } catch (SocketException e) {
            throw new RuntimeIoException(e);
        }
    }

    /**
     * Tells if SO_BROADCAST is enabled.
     *
     * @return <code>true</code> if SO_BROADCAST is enabled
     * @throws RuntimeIoException If the socket is closed or if we get an
     * {@link SocketException}
     */
    @Override
    public boolean isBroadcast() {
        try {
            return channel.socket().getBroadcast();
        } catch (SocketException e) {
            throw new RuntimeIoException(e);
        }
    }

    @Override
    public void setBroadcast(boolean broadcast) {
        try {
            channel.socket().setBroadcast(broadcast);
        } catch (SocketException e) {
            throw new RuntimeIoException(e);
        }
    }

    /**
     *
     * @throws RuntimeIoException If the socket is closed or if we get an
     * {@link SocketException}
     */
    @Override
    public int getSendBufferSize() {
        try {
            return channel.socket().getSendBufferSize();
        } catch (SocketException e) {
            throw new RuntimeIoException(e);
        }
    }

    /**
     *
     * @throws RuntimeIoException If the socket is closed or if we get an
     * {@link SocketException}
     */
    @Override
    public void setSendBufferSize(int sendBufferSize) {
        try {
            if (ENABLE_BUFFER_SIZE) {
                System.out.println("NioDatagramSessionConfig.setSendBufferSize(" + sendBufferSize + ")");
                channel.socket().setSendBufferSize(sendBufferSize);
            }
        } catch (SocketException e) {
            throw new RuntimeIoException(e);
        }
    }

    /**
     * Tells if SO_REUSEADDR is enabled.
     *
     * @return <code>true</code> if SO_REUSEADDR is enabled
     * @throws RuntimeIoException If the socket is closed or if we get an
     * {@link SocketException}
     */
    @Override
    public boolean isReuseAddress() {
        try {
            return channel.socket().getReuseAddress();
        } catch (SocketException e) {
            throw new RuntimeIoException(e);
        }
    }

    /**
     *
     * @throws RuntimeIoException If the socket is closed or if we get an
     * {@link SocketException}
     */
    @Override
    public void setReuseAddress(boolean reuseAddress) {
        try {
            channel.socket().setReuseAddress(reuseAddress);
        } catch (SocketException e) {
            throw new RuntimeIoException(e);
        }
    }

    /**
     * Get the current Traffic Class for this Socket, if any. As this is
     * not a mandatory feature, the returned value should be considered as
     * a hint.
     *
     * @return The Traffic Class supported by this Socket
     * @throws RuntimeIoException If the socket is closed or if we get an
     * {@link SocketException}
     */
    @Override
    public int getTrafficClass() {
        try {
            return channel.socket().getTrafficClass();
        } catch (SocketException e) {
            throw new RuntimeIoException(e);
        }
    }

    /**
     * {@inheritDoc}
     * @throws RuntimeIoException If the socket is closed or if we get an
     * {@link SocketException}
     */
    @Override
    public void setTrafficClass(int trafficClass) {
        try {
            channel.socket().setTrafficClass(trafficClass);
        } catch (SocketException e) {
            throw new RuntimeIoException(e);
        }
    }
}

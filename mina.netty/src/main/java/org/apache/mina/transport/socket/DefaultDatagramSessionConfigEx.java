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
/* This class has the following differences from DefaultDatagramSessionConfig in Mina 2.0.0-RC1:
 * 1. Extend AbstractDatagramSessionConfigEx instead of AbstractDatagramSessionConfig in order to implement IoSessionConfigEx.
 * 2. Make private static variables final for efficiency.
 */
package org.apache.mina.transport.socket;

import java.net.DatagramSocket;

/**
 * A default implementation of {@link DatagramSessionConfig}.
 */
public class DefaultDatagramSessionConfigEx extends AbstractDatagramSessionConfigEx {
    private static final boolean DEFAULT_BROADCAST = false;
    private static final boolean DEFAULT_REUSE_ADDRESS = false;
    private static final int DEFAULT_RECEIVE_BUFFER_SIZE = 1024;
    private static final int DEFAULT_SEND_BUFFER_SIZE = 1024;
    private static final int DEFAULT_TRAFFIC_CLASS = 0;

    private boolean broadcast = DEFAULT_BROADCAST;
    private boolean reuseAddress = DEFAULT_REUSE_ADDRESS;
    private int receiveBufferSize = DEFAULT_RECEIVE_BUFFER_SIZE;
    private int sendBufferSize = DEFAULT_SEND_BUFFER_SIZE;
    private int trafficClass = DEFAULT_TRAFFIC_CLASS;

    /**
     * Creates a new instance.
     */
    public DefaultDatagramSessionConfigEx() {
        // Do nothing
    }

    /**
     * @see DatagramSocket#getBroadcast()
     */
    @Override
    public boolean isBroadcast() {
        return broadcast;
    }

    /**
     * @see DatagramSocket#setBroadcast(boolean)
     */
    @Override
    public void setBroadcast(boolean broadcast) {
        this.broadcast = broadcast;
    }

    /**
     * @see DatagramSocket#getReuseAddress()
     */
    @Override
    public boolean isReuseAddress() {
        return reuseAddress;
    }

    /**
     * @see DatagramSocket#setReuseAddress(boolean)
     */
    @Override
    public void setReuseAddress(boolean reuseAddress) {
        this.reuseAddress = reuseAddress;
    }

    /**
     * @see DatagramSocket#getReceiveBufferSize()
     */
    @Override
    public int getReceiveBufferSize() {
        return receiveBufferSize;
    }

    /**
     * @see DatagramSocket#setReceiveBufferSize(int)
     */
    @Override
    public void setReceiveBufferSize(int receiveBufferSize) {
        this.receiveBufferSize = receiveBufferSize;
    }

    /**
     * @see DatagramSocket#getSendBufferSize()
     */
    @Override
    public int getSendBufferSize() {
        return sendBufferSize;
    }

    /**
     * @see DatagramSocket#setSendBufferSize(int)
     */
    @Override
    public void setSendBufferSize(int sendBufferSize) {
        this.sendBufferSize = sendBufferSize;
    }

    /**
     * @see DatagramSocket#getTrafficClass()
     */
    @Override
    public int getTrafficClass() {
        return trafficClass;
    }

    /**
     * @see DatagramSocket#setTrafficClass(int)
     */
    @Override
    public void setTrafficClass(int trafficClass) {
        this.trafficClass = trafficClass;
    }

    @Override
    protected boolean isBroadcastChanged() {
        return broadcast != DEFAULT_BROADCAST;
    }

    @Override
    protected boolean isReceiveBufferSizeChanged() {
        return receiveBufferSize != DEFAULT_RECEIVE_BUFFER_SIZE;
    }

    @Override
    protected boolean isReuseAddressChanged() {
        return reuseAddress != DEFAULT_REUSE_ADDRESS;
    }

    @Override
    protected boolean isSendBufferSizeChanged() {
        return sendBufferSize != DEFAULT_SEND_BUFFER_SIZE;
    }

    @Override
    protected boolean isTrafficClassChanged() {
        return trafficClass != DEFAULT_TRAFFIC_CLASS;
    }

}

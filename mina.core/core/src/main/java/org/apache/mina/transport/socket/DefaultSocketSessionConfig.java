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

import org.apache.mina.core.service.IoService;

/**
 * A default implementation of {@link SocketSessionConfig}.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class DefaultSocketSessionConfig extends AbstractSocketSessionConfig {
    private static boolean DEFAULT_REUSE_ADDRESS = false;
    private static int DEFAULT_RECEIVE_BUFFER_SIZE = 1024;
    private static int DEFAULT_SEND_BUFFER_SIZE = 1024;
    private static int DEFAULT_TRAFFIC_CLASS = 0;
    private static boolean DEFAULT_KEEP_ALIVE = false;
    private static boolean DEFAULT_OOB_INLINE = false;
    private static int DEFAULT_SO_LINGER = -1;
    private static boolean DEFAULT_TCP_NO_DELAY = false;

    private IoService parent;
    private boolean defaultReuseAddress;
    private int defaultReceiveBufferSize = DEFAULT_RECEIVE_BUFFER_SIZE;

    private boolean reuseAddress;
    private int receiveBufferSize = defaultReceiveBufferSize;
    private int sendBufferSize = DEFAULT_SEND_BUFFER_SIZE;
    private int trafficClass = DEFAULT_TRAFFIC_CLASS;
    private boolean keepAlive = DEFAULT_KEEP_ALIVE;
    private boolean oobInline = DEFAULT_OOB_INLINE;
    private int soLinger = DEFAULT_SO_LINGER;
    private boolean tcpNoDelay = DEFAULT_TCP_NO_DELAY;

    /**
     * Creates a new instance.
     */
    public DefaultSocketSessionConfig() {
        // Do nothing
    }

    public void init(IoService parent) {
        this.parent = parent;
        if (parent instanceof SocketAcceptor) {
            defaultReuseAddress = true;
        } else {
            defaultReuseAddress = DEFAULT_REUSE_ADDRESS;
        }
        reuseAddress = defaultReuseAddress;
    }

    public boolean isReuseAddress() {
        return reuseAddress;
    }

    public void setReuseAddress(boolean reuseAddress) {
        this.reuseAddress = reuseAddress;
    }

    public int getReceiveBufferSize() {
        return receiveBufferSize;
    }

    public void setReceiveBufferSize(int receiveBufferSize) {
        this.receiveBufferSize = receiveBufferSize;

        // The acceptor configures the SO_RCVBUF value of the
        // server socket when it is activated.  Consequently,
        // a newly accepted session doesn't need to update its
        // SO_RCVBUF parameter.  Therefore, we need to update
        // the default receive buffer size if the acceptor is
        // not bound yet to avoid a unnecessary system call
        // when the acceptor is activated and new sessions are
        // created.
        if (!parent.isActive() && parent instanceof SocketAcceptor) {
            defaultReceiveBufferSize = receiveBufferSize;
        }
    }

    public int getSendBufferSize() {
        return sendBufferSize;
    }

    public void setSendBufferSize(int sendBufferSize) {
        this.sendBufferSize = sendBufferSize;
    }

    public int getTrafficClass() {
        return trafficClass;
    }

    public void setTrafficClass(int trafficClass) {
        this.trafficClass = trafficClass;
    }

    public boolean isKeepAlive() {
        return keepAlive;
    }

    public void setKeepAlive(boolean keepAlive) {
        this.keepAlive = keepAlive;
    }

    public boolean isOobInline() {
        return oobInline;
    }

    public void setOobInline(boolean oobInline) {
        this.oobInline = oobInline;
    }

    public int getSoLinger() {
        return soLinger;
    }

    public void setSoLinger(int soLinger) {
        this.soLinger = soLinger;
    }

    public boolean isTcpNoDelay() {
        return tcpNoDelay;
    }

    public void setTcpNoDelay(boolean tcpNoDelay) {
        this.tcpNoDelay = tcpNoDelay;
    }

    @Override
    protected boolean isKeepAliveChanged() {
        return keepAlive != DEFAULT_KEEP_ALIVE;
    }

    @Override
    protected boolean isOobInlineChanged() {
        return oobInline != DEFAULT_OOB_INLINE;
    }

    @Override
    protected boolean isReceiveBufferSizeChanged() {
        return receiveBufferSize != defaultReceiveBufferSize;
    }

    @Override
    protected boolean isReuseAddressChanged() {
        return reuseAddress != defaultReuseAddress;
    }

    @Override
    protected boolean isSendBufferSizeChanged() {
        return sendBufferSize != DEFAULT_SEND_BUFFER_SIZE;
    }

    @Override
    protected boolean isSoLingerChanged() {
        return soLinger != DEFAULT_SO_LINGER;
    }

    @Override
    protected boolean isTcpNoDelayChanged() {
        return tcpNoDelay != DEFAULT_TCP_NO_DELAY;
    }

    @Override
    protected boolean isTrafficClassChanged() {
        return trafficClass != DEFAULT_TRAFFIC_CLASS;
    }
}

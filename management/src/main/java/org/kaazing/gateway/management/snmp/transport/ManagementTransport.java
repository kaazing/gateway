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
package org.kaazing.gateway.management.snmp.transport;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import org.apache.mina.core.future.IoFutureListener;
import org.apache.mina.core.future.WriteFuture;
import org.kaazing.gateway.management.context.ManagementContext;
import org.kaazing.mina.core.buffer.IoBufferEx;
import org.kaazing.mina.core.session.IoSessionEx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snmp4j.MessageDispatcher;
import org.snmp4j.TransportMapping;
import org.snmp4j.smi.Address;
import org.snmp4j.transport.TransportListener;

public class ManagementTransport implements TransportMapping {
    private static final Logger logger = LoggerFactory.getLogger(ManagementTransport.class);
    private final ManagementContext managementContext;
    private List<TransportListener> transportListeners = new ArrayList<>();

    // Pass in the ExecutorService used for management tasks, so we can get the management
    // requests OFF the normal IO threads before processing them.
    public ManagementTransport(ManagementContext managementContext) {
        super();
        this.managementContext = managementContext;
    }

    @Override
    public void addMessageDispatcher(MessageDispatcher dispatcher) {
        addTransportListener(dispatcher);
    }

    @Override
    public void addTransportListener(TransportListener transportListener) {
        transportListeners.add(transportListener);
    }

    @Override
    public void close() throws IOException {
    }

    @Override
    public Address getListenAddress() {
        return null;
    }

    @Override
    public int getMaxInboundMessageSize() {
        // FIXME:  implement
        return 0;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Class getSupportedAddressClass() {
        return ManagementAddress.class;
    }

    @Override
    public boolean isListening() {
        return true;
    }

    @Override
    public void listen() throws IOException {
    }

    @Override
    public void removeMessageDispatcher(MessageDispatcher dispatcher) {
        removeTransportListener(dispatcher);
    }

    @Override
    public void removeTransportListener(TransportListener transportListener) {
        transportListeners.remove(transportListener);
    }

    @Override
    public void sendMessage(Address address, final byte[] message) throws IOException {
        if (address instanceof ManagementAddress) {
            IoSessionEx session = ((ManagementAddress) address).getSession();

            // According to Chris B, the following will realign with the session's IO thread,
            // so we need not do it explicitly here. processMessage() was set up to get us OFF
            // the IO threads, and here is where we get back on.
            if (logger.isTraceEnabled()) {
                logger.trace("#### ManagementTransport sending message with " + message.length + " bytes");
            }

            ByteBuffer b = ByteBuffer.wrap(message);
            IoBufferEx buf = session.getBufferAllocator().wrap(b);
            WriteFuture future = session.write(buf); // FIXME:  is this sufficient?
            future.addListener(new IoFutureListener<WriteFuture>() {
                @Override
                public void operationComplete(WriteFuture future) {
                    if (future.isWritten() && logger.isTraceEnabled()) {
                        String msg = "SNMP write operation completed, bytes written: " + message.length;
                        logger.trace(msg);
                    }
                }
            });
        }
    }

    // This is always called on a session's IO thread from SnmpManagementServiceHandler.
    // Because requests involving service data need to avoid blocking IO threads but
    // also need to aggregate across IO threads, we need to use a new thread from the
    // management ExecutorService.
    public void processMessage(final ManagementAddress address, final ByteBuffer message) {
        for (final TransportListener listener : transportListeners) {
            managementContext.runManagementTask(new Runnable() {
                @Override
                public void run() {
                    listener.processMessage(ManagementTransport.this, address, message);
                }
            });
        }
    }
}

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

import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.future.IoFutureListener;
import org.apache.mina.core.future.WriteFuture;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.kaazing.gateway.management.context.ManagementContext;
import org.kaazing.gateway.service.ServiceContext;
import org.kaazing.mina.core.buffer.IoBufferEx;
import org.kaazing.mina.core.session.IoSessionEx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snmp4j.smi.Address;
import org.snmp4j.smi.UdpAddress;

public class ManagementUdpTransport extends ManagementTransport {
    private static final Logger LOGGER = LoggerFactory.getLogger(ManagementUdpTransport.class);
    private final ServiceContext serviceContext;
    private final UdpConnectHandler connectHandler;

    public ManagementUdpTransport(ServiceContext serviceContext, ManagementContext managementContext) {
        super(managementContext);
        this.serviceContext = serviceContext;
        connectHandler = new UdpConnectHandler();
    }

    @Override
    public void close() throws IOException {
    }

    @Override
    @SuppressWarnings("unchecked")
    public Class getSupportedAddressClass() {
        return UdpAddress.class;
    }

    @Override
    public void sendMessage(final Address address, final byte[] message) throws IOException {
        if (address instanceof UdpAddress) {
            UdpAddress udpAddress = (UdpAddress) address;
            String connectURI = "udp://" + udpAddress.getInetAddress().getHostAddress() + ":" + udpAddress.getPort();

            // FIXME:  IoSessionInitializer is null, but should set up security info, especially for SNMPv3...
            ConnectFuture future = serviceContext.connect(connectURI, connectHandler, null);
            future.addListener(new IoFutureListener<ConnectFuture>() {
                @Override
                public void operationComplete(ConnectFuture future) {
                    if (future.isConnected()) {
                        final IoSessionEx session = (IoSessionEx) future.getSession();
                        ByteBuffer b = ByteBuffer.wrap(message);
                        IoBufferEx buf = session.getBufferAllocator().wrap(b);
                        // FIXME:  is just calling session.write() sufficient?
                        WriteFuture writeFuture = session.write(buf);
                        writeFuture.addListener(new IoFutureListener<WriteFuture>() {
                            @Override
                            public void operationComplete(WriteFuture writeFuture) {
                                if (writeFuture.isWritten()) {
                                    if (LOGGER.isTraceEnabled()) {
                                        LOGGER.trace("SNMP write operation completed, bytes written: " + message.length);
                                    }
                                } else {
                                    if (LOGGER.isDebugEnabled()) {
                                        LOGGER.debug("SNMP write failure to address: " + address.toString());
                                    }
                                }
                                // FIXME:  close the session now?
                                session.close(false);
                            }
                        });
                    } else {
                        // FIXME:  log warning?  blacklist the offending address in the MIB?  It's UDP, so maybe we don't
                        // care
                    }
                }

            });
        }
    }

    private class UdpConnectHandler extends IoHandlerAdapter {
    }
}

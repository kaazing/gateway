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
import org.snmp4j.smi.Address;
import org.snmp4j.smi.TcpAddress;

public class ManagementTcpTransport extends ManagementTransport {

    private final ServiceContext serviceContext;
    private final TcpConnectHandler tcpConnectHandler;

    public ManagementTcpTransport(ServiceContext serviceContext, ManagementContext managementContext) {
        super(managementContext);
        this.serviceContext = serviceContext;
        tcpConnectHandler = new TcpConnectHandler();
    }

    @Override
    public void close() throws IOException {
    }

    @Override
    @SuppressWarnings("unchecked")
    public Class getSupportedAddressClass() {
        return TcpAddress.class;
    }

    @Override
    public void sendMessage(Address address, final byte[] message) throws IOException {
        if (address instanceof TcpAddress) {
            TcpAddress tcpAddress = (TcpAddress) address;
            String connectURI = "tcp://" + tcpAddress.getInetAddress().getHostAddress() + ":" + tcpAddress.getPort();

            // FIXME:  IoSessionInitializer is null, but should set up security info, especially for SNMPv3...
            ConnectFuture future = serviceContext.connect(connectURI, tcpConnectHandler, null);
            future.addListener(new IoFutureListener<ConnectFuture>() {
                @Override
                public void operationComplete(ConnectFuture future) {
                    if (future.isConnected()) {
                        final IoSessionEx session = (IoSessionEx) future.getSession();
                        ByteBuffer b = ByteBuffer.wrap(message);
                        IoBufferEx buf = session.getBufferAllocator().wrap(b);
                        WriteFuture writeFuture = session.write(buf); // FIXME:  is this sufficient?
                        writeFuture.addListener(new IoFutureListener<WriteFuture>() {
                            @Override
                            public void operationComplete(WriteFuture writeFuture) {
                                if (writeFuture.isWritten()) {
                                    // FIXME:  trace logging on success?  what about failure?  warning?
//                                        String msg = "SNMP write operation completed, bytes written: " + message.length;
//                                        logger.trace(msg);
                                }
                                // close the session now?
                                //session.close(false);
                            }
                        });
                    } else {
                        // FIXME:  log warning?  blacklist the offending address in the MIB?
                    }
                }

            });
        }
    }

    private class TcpConnectHandler extends IoHandlerAdapter {
    }
}

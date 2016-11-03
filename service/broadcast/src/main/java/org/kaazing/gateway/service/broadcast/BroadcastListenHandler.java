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
package org.kaazing.gateway.service.broadcast;

import java.util.Collection;
import java.util.Iterator;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;
import org.slf4j.Logger;

import org.kaazing.gateway.transport.BridgeSession;
import org.kaazing.gateway.transport.LoggingUtils;
import org.kaazing.gateway.transport.bridge.Message;
import org.kaazing.gateway.transport.io.filter.IoMessageCodecFilter;

public class BroadcastListenHandler extends IoHandlerAdapter {

    private final Collection<IoSession> clients;
    private final IoMessageCodecFilter codec;
    private final boolean disconnectClientsOnReconnect;
    private final long maximumScheduledWriteBytes;
    private final Logger logger;

	public BroadcastListenHandler(Collection<IoSession> clients, boolean disconnectClientsOnReconnect, long maximumScheduledWriteBytes, Logger logger) {
		this.clients = clients;
		this.codec = new IoMessageCodecFilter();
		this.disconnectClientsOnReconnect = disconnectClientsOnReconnect;
		this.logger = logger;
		this.maximumScheduledWriteBytes = maximumScheduledWriteBytes;
	}

	@Override
    public void sessionOpened(IoSession session) throws Exception {
	    session.getFilterChain().addLast("io", codec);
    }

    @Override
    public void sessionClosed(IoSession session) throws Exception {
        if (disconnectClientsOnReconnect) {
            Iterator<IoSession> clientsIterator = clients.iterator();
            while (clientsIterator.hasNext()) {
                clientsIterator.next().close(false);
                // BroadcastServiceHandler.sessionClosed(IoSession) will take care of removing client from clients
            }
        }
    }

    @Override
	public void messageReceived(IoSession session, Object message) throws Exception {
        if (message instanceof IoBuffer) {
			IoBuffer buf = (IoBuffer) message;
			for (IoSession client : clients) {
			    writeOrClose(client, buf);
			}
			buf.skip(buf.remaining());
		}
		else {
	        if (message instanceof Message) {
	            ((Message)message).initCache();
	        }

			for (IoSession client : clients) {
			    writeOrClose(client, message);
			}
		}
	}

    @Override
    public void exceptionCaught(IoSession session, Throwable cause) throws Exception {
        LoggingUtils.log(session, logger, cause);
    }

    private void writeOrClose(IoSession client, Object message) {
        long scheduledWriteBytes = getScheduledWriteBytes(client);
        if (logger.isDebugEnabled()) {
            logger.debug(String.format("BroadcastListenHandler: session %d: scheduledWriteBytes = %d", client.getId(), scheduledWriteBytes));
        }

        if (!client.isClosing()) {
            if (scheduledWriteBytes > maximumScheduledWriteBytes) {
                if (logger.isInfoEnabled()) {
                    String logMessage = String.format("Closing client session %s because scheduled write bytes %d exceeds the configured limit of %d",
                            client, scheduledWriteBytes, maximumScheduledWriteBytes);
                    logger.info(logMessage);
                }
                client.close(true);
                // BroadcastServiceHandler.sessionClosed(IoSession) will take care of removing client from clients
            }
            else {
                client.write(message);
            }
        }
    }

    private long getScheduledWriteBytes(IoSession client) {
        IoSession session = client;
        while (session instanceof BridgeSession) {
            IoSession parent = ((BridgeSession)session).getParent();
            if (parent == null) { // parent can occasionally be null (e.g. on a WsebSession from Flash client)
                if (logger.isDebugEnabled()) {
                    logger.debug(String.format("Null parent on session %s, ancestor of client session %s", session, client));
                }
                break;
            }
            session = parent;
        }
        return session.getScheduledWriteBytes();
    }

}

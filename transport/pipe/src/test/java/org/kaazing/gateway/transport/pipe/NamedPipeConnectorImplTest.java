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
package org.kaazing.gateway.transport.pipe;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.future.WriteFuture;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;
import org.junit.Test;
import org.kaazing.gateway.transport.NamedPipeAddress;
import org.kaazing.mina.core.buffer.IoBufferAllocatorEx;
import org.kaazing.mina.core.session.IoSessionEx;

public class NamedPipeConnectorImplTest {

	private static final byte[] ECHO_BYTES = "echo".getBytes(Charset.forName("UTF-8"));

	@Test(expected = NamedPipeException.class)
    public void connectorWithoutAcceptorShouldFailToConnect() throws Throwable {
        NamedPipeConnectorImpl connector = new NamedPipeConnectorImpl();
        connector.setHandler(new IoHandlerAdapter());

        NamedPipeAddress remoteAddress = new NamedPipeAddress("remote");
        ConnectFuture future = connector.connect(remoteAddress);
        future.awaitUninterruptibly();
        future.getSession();
    }

	@Test
    public void connectorWithAcceptorShouldConnectSuccessfully() throws Throwable {
        NamedPipeAcceptorImpl acceptor = new NamedPipeAcceptorImpl();
        acceptor.setHandler(new IoHandlerAdapter());

        NamedPipeAddress localAddress = new NamedPipeAddress("accept");
        acceptor.bind(localAddress);

        NamedPipeConnectorImpl connector = new NamedPipeConnectorImpl();
        connector.setNamedPipeAcceptor(acceptor);
        connector.setHandler(new IoHandlerAdapter());

        NamedPipeAddress remoteAddress = new NamedPipeAddress("accept");
        ConnectFuture future = connector.connect(remoteAddress);
        future.awaitUninterruptibly();
        NamedPipeSession connectSession = (NamedPipeSession)future.getSession();
        NamedPipeSession acceptSession = connectSession.getRemoteSession();
        
        assertNotNull(acceptSession);
        assertSame(connectSession, acceptSession.getRemoteSession());
    }

	@Test
    public void connectedSessionShouldTransferIoBuffers() throws Throwable {
		NamedPipeAcceptorImpl acceptor = new NamedPipeAcceptorImpl();
        acceptor.setHandler(new IoHandlerAdapter() {
			@Override
			public void messageReceived(IoSession session, Object message)
					throws Exception {
				// echo buffer
				IoBuffer buf = (IoBuffer)message;
				session.write(buf.duplicate());
			}
        });

        NamedPipeAddress localAddress = new NamedPipeAddress("accept");
        acceptor.bind(localAddress);

        final AtomicReference<IoBuffer> connectorMessageReceived = new AtomicReference<>();
        
        NamedPipeConnectorImpl connector = new NamedPipeConnectorImpl();
        connector.setNamedPipeAcceptor(acceptor);
        connector.setHandler(new IoHandlerAdapter() {
			@Override
			public void messageReceived(IoSession session, Object message)
					throws Exception {
				IoBuffer buf = (IoBuffer)message;
				connectorMessageReceived.set(buf.duplicate());
			}
        });

        NamedPipeAddress remoteAddress = new NamedPipeAddress("accept");
        ConnectFuture connectFuture = connector.connect(remoteAddress);
        connectFuture.awaitUninterruptibly();
        IoSessionEx session = (IoSessionEx) connectFuture.getSession();
        IoBufferAllocatorEx<?> allocator = session.getBufferAllocator();
        WriteFuture writeFuture = session.write(allocator.wrap(ByteBuffer.wrap(ECHO_BYTES)));
        writeFuture.awaitUninterruptibly();

        assertEquals(allocator.wrap(ByteBuffer.wrap(ECHO_BYTES)), connectorMessageReceived.get());
    }

	@Test
    public void closedConnectSessionShouldCloseAcceptSession() throws Throwable {
		NamedPipeAcceptorImpl acceptor = new NamedPipeAcceptorImpl();
        acceptor.setHandler(new IoHandlerAdapter());

        NamedPipeAddress localAddress = new NamedPipeAddress("accept");
        acceptor.bind(localAddress);

        NamedPipeConnectorImpl connector = new NamedPipeConnectorImpl();
        connector.setNamedPipeAcceptor(acceptor);
        connector.setHandler(new IoHandlerAdapter());

        NamedPipeAddress remoteAddress = new NamedPipeAddress("accept");
        ConnectFuture connectFuture = connector.connect(remoteAddress);
        connectFuture.awaitUninterruptibly();
        NamedPipeSession connectSession = (NamedPipeSession)connectFuture.getSession();
        NamedPipeSession acceptSession = connectSession.getRemoteSession();

        connectSession.close(false).await();
        assertTrue(acceptSession.getCloseFuture().isClosed());
    }

	@Test
    public void closedAcceptSessionShouldCloseConnectSession() throws Throwable {
		NamedPipeAcceptorImpl acceptor = new NamedPipeAcceptorImpl();
        acceptor.setHandler(new IoHandlerAdapter());

        NamedPipeAddress localAddress = new NamedPipeAddress("accept");
        acceptor.bind(localAddress);

        NamedPipeConnectorImpl connector = new NamedPipeConnectorImpl();
        connector.setNamedPipeAcceptor(acceptor);
        connector.setHandler(new IoHandlerAdapter());

        NamedPipeAddress remoteAddress = new NamedPipeAddress("accept");
        ConnectFuture connectFuture = connector.connect(remoteAddress);
        connectFuture.awaitUninterruptibly();
        NamedPipeSession connectSession = (NamedPipeSession)connectFuture.getSession();
        NamedPipeSession acceptSession = connectSession.getRemoteSession();

        acceptSession.close(true);
        assertTrue(connectSession.isClosing());
    }

	@Test
    public void resetConnectSessionShouldCloseSessions() throws Throwable {
        final AtomicBoolean acceptorSessionClosed = new AtomicBoolean();
        final AtomicBoolean connectorSessionClosed = new AtomicBoolean();

		NamedPipeAcceptorImpl acceptor = new NamedPipeAcceptorImpl();
        acceptor.setHandler(new IoHandlerAdapter() {
            @Override
            public void sessionClosed(IoSession session) throws Exception {
                acceptorSessionClosed.set(true);
            }
        });

        NamedPipeAddress localAddress = new NamedPipeAddress("accept");
        acceptor.bind(localAddress);

        NamedPipeConnectorImpl connector = new NamedPipeConnectorImpl();
        connector.setNamedPipeAcceptor(acceptor);
        connector.setHandler(new IoHandlerAdapter() {
            @Override
            public void sessionClosed(IoSession session) throws Exception {
                connectorSessionClosed.set(true);
            }
        });

        NamedPipeAddress remoteAddress = new NamedPipeAddress("accept");
        ConnectFuture connectFuture = connector.connect(remoteAddress);
        connectFuture.awaitUninterruptibly();
        NamedPipeSession connectSession = (NamedPipeSession)connectFuture.getSession();
        NamedPipeSession acceptSession = connectSession.getRemoteSession();

        assertFalse(acceptorSessionClosed.get());
        assertFalse(connectorSessionClosed.get());

        assertNotNull(connectSession.getRemoteSession());

        // break the pipe, similar to TCP network error
        connectSession.setRemoteSession(null);
        
        assertNull(acceptSession.getRemoteSession());
        assertNull(connectSession.getRemoteSession());

        assertTrue(acceptorSessionClosed.get());
        assertTrue(connectorSessionClosed.get());
    }

	@Test
    public void resetAcceptSessionShouldCloseSessions() throws Throwable {
        final AtomicBoolean acceptorSessionClosed = new AtomicBoolean();
        final AtomicBoolean connectorSessionClosed = new AtomicBoolean();
        NamedPipeAcceptorImpl acceptor = new NamedPipeAcceptorImpl();
        acceptor.setHandler(new IoHandlerAdapter() {
            @Override
            public void sessionClosed(IoSession session) throws Exception {
                acceptorSessionClosed.set(true);
            }
        });

        NamedPipeAddress localAddress = new NamedPipeAddress("accept");
        acceptor.bind(localAddress);

        NamedPipeConnectorImpl connector = new NamedPipeConnectorImpl();
        connector.setNamedPipeAcceptor(acceptor);
        connector.setHandler(new IoHandlerAdapter() {
            @Override
            public void sessionClosed(IoSession session) throws Exception {
                connectorSessionClosed.set(true);
            }
        });


        NamedPipeAddress remoteAddress = new NamedPipeAddress("accept");
        ConnectFuture connectFuture = connector.connect(remoteAddress);
        connectFuture.awaitUninterruptibly();
        NamedPipeSession connectSession = (NamedPipeSession)connectFuture.getSession();
        NamedPipeSession acceptSession = connectSession.getRemoteSession();

        assertFalse(acceptorSessionClosed.get());
        assertFalse(connectorSessionClosed.get());
        assertNotNull(acceptSession.getRemoteSession());

        // break the pipe, similar to TCP network error
        acceptSession.setRemoteSession(null);

        assertNull(connectSession.getRemoteSession());
        assertNull(acceptSession.getRemoteSession());
        assertTrue(acceptorSessionClosed.get());
        assertTrue(connectorSessionClosed.get());
    }

	@Test
    public void connectedSessionShouldTransferIoBuffersAfterResumeWrite() throws Throwable { 
		NamedPipeAcceptorImpl acceptor = new NamedPipeAcceptorImpl();
        acceptor.setHandler(new IoHandlerAdapter() {
			@Override
			public void messageReceived(IoSession session, Object message)
					throws Exception {
				// echo buffer
				IoBuffer buf = (IoBuffer)message;
				session.write(buf.duplicate());
			}
        });

        NamedPipeAddress localAddress = new NamedPipeAddress("accept");
        acceptor.bind(localAddress);

        final AtomicReference<IoBuffer> connectorMessageReceived = new AtomicReference<>();
        
        NamedPipeConnectorImpl connector = new NamedPipeConnectorImpl();
        connector.setNamedPipeAcceptor(acceptor);
        connector.setHandler(new IoHandlerAdapter() {
			@Override
			public void messageReceived(IoSession session, Object message)
					throws Exception {
				IoBuffer buf = (IoBuffer)message;
				connectorMessageReceived.set(buf.duplicate());
			}
        });

        NamedPipeAddress remoteAddress = new NamedPipeAddress("accept");
        ConnectFuture connectFuture = connector.connect(remoteAddress);
        connectFuture.awaitUninterruptibly();
        IoSessionEx session = (IoSessionEx) connectFuture.getSession();
        
        session.suspendWrite();
        IoBufferAllocatorEx<?> allocator = session.getBufferAllocator();
        WriteFuture writeFuture = session.write(allocator.wrap(ByteBuffer.wrap(ECHO_BYTES)));
        assertNull(connectorMessageReceived.get());

        session.resumeWrite();
        writeFuture.awaitUninterruptibly();
        assertEquals(allocator.wrap(ByteBuffer.wrap(ECHO_BYTES)), connectorMessageReceived.get());
    }

	@Test
    public void connectedSessionShouldTransferIoBuffersAfterResumeRead() throws Throwable { 
		NamedPipeAcceptorImpl acceptor = new NamedPipeAcceptorImpl();
        acceptor.setHandler(new IoHandlerAdapter() {
			@Override
			public void messageReceived(IoSession session, Object message)
					throws Exception {
				// echo buffer
				IoBuffer buf = (IoBuffer)message;
				session.write(buf.duplicate());
			}
        });

        NamedPipeAddress localAddress = new NamedPipeAddress("accept");
        acceptor.bind(localAddress);

        final AtomicReference<IoBuffer> connectorMessageReceived = new AtomicReference<>();
        
        NamedPipeConnectorImpl connector = new NamedPipeConnectorImpl();
        connector.setNamedPipeAcceptor(acceptor);
        connector.setHandler(new IoHandlerAdapter() {
			@Override
			public void messageReceived(IoSession session, Object message)
					throws Exception {
				IoBuffer buf = (IoBuffer)message;
				connectorMessageReceived.set(buf.duplicate());
			}
        });

        NamedPipeAddress remoteAddress = new NamedPipeAddress("accept");
        ConnectFuture connectFuture = connector.connect(remoteAddress);
        connectFuture.awaitUninterruptibly();
        IoSessionEx session = (IoSessionEx) connectFuture.getSession();
        IoBufferAllocatorEx<?> allocator = session.getBufferAllocator();
        session.suspendRead();
        WriteFuture writeFuture = session.write(allocator.wrap(ByteBuffer.wrap(ECHO_BYTES)));
        writeFuture.awaitUninterruptibly();

        assertNull(connectorMessageReceived.get());

        session.resumeRead();
        assertEquals(allocator.wrap(ByteBuffer.wrap(ECHO_BYTES)), connectorMessageReceived.get());
    }
	
    @Test
    public void readSuspendedConnectedSessionShouldNotReceiveIoBuffersFromRemote() throws Throwable { 
        NamedPipeAcceptorImpl acceptor = new NamedPipeAcceptorImpl();
        acceptor.setHandler(new IoHandlerAdapter() {
            @Override
            public void messageReceived(IoSession session, Object message)
                    throws Exception {
                // echo buffer
                IoBuffer buf = (IoBuffer)message;
                session.write(buf.duplicate());
            }
        });

        NamedPipeAddress localAddress = new NamedPipeAddress("accept");
        acceptor.bind(localAddress);

        final AtomicReference<IoBuffer> connectorMessageReceived = new AtomicReference<>();
        
        NamedPipeConnectorImpl connector = new NamedPipeConnectorImpl();
        connector.setNamedPipeAcceptor(acceptor);
        connector.setHandler(new IoHandlerAdapter() {
            @Override
            public void messageReceived(IoSession session, Object message)
                    throws Exception {
                IoBuffer buf = (IoBuffer)message;
                connectorMessageReceived.set(buf.duplicate());
            }
        });

        NamedPipeAddress remoteAddress = new NamedPipeAddress("accept");
        ConnectFuture connectFuture = connector.connect(remoteAddress);
        connectFuture.awaitUninterruptibly();
        IoSession session = connectFuture.getSession();
        session.suspendRead();
        
        IoSessionEx remoteSession = ((NamedPipeSession)session).getRemoteSession();
        IoBufferAllocatorEx<?> remoteAllocator = remoteSession.getBufferAllocator();
        WriteFuture writeFuture = remoteSession.write(remoteAllocator.wrap(ByteBuffer.wrap(ECHO_BYTES)));
        assertFalse(writeFuture.isWritten());
        assertNull(connectorMessageReceived.get());

        session.resumeRead();
        writeFuture.awaitUninterruptibly();
        assertEquals(remoteAllocator.wrap(ByteBuffer.wrap(ECHO_BYTES)), connectorMessageReceived.get());
    }
    
    @Test
    public void readSuspendedAcceptedSessionShouldNotReceiveIoBuffersFromRemote() throws Throwable { 
        NamedPipeAcceptorImpl acceptor = new NamedPipeAcceptorImpl();
        final AtomicReference<IoBuffer> acceptorMessageReceived = new AtomicReference<>();
        
        acceptor.setHandler(new IoHandlerAdapter() {
            @Override
            public void messageReceived(IoSession session, Object message)
                    throws Exception {
                IoBuffer buf = (IoBuffer)message;
                acceptorMessageReceived.set(buf.duplicate());
            }
        });

        NamedPipeAddress localAddress = new NamedPipeAddress("accept");
        acceptor.bind(localAddress);

        NamedPipeConnectorImpl connector = new NamedPipeConnectorImpl();
        connector.setNamedPipeAcceptor(acceptor);
        connector.setHandler(new IoHandlerAdapter() {
            @Override
            public void messageReceived(IoSession session, Object message)
                    throws Exception {
                // echo buffer
                IoBuffer buf = (IoBuffer)message;
                session.write(buf.duplicate());
            }
        });

        NamedPipeAddress remoteAddress = new NamedPipeAddress("accept");
        ConnectFuture connectFuture = connector.connect(remoteAddress);
        connectFuture.awaitUninterruptibly();
        IoSessionEx session = (IoSessionEx) connectFuture.getSession();
        IoSessionEx remoteSession = ((NamedPipeSession)session).getRemoteSession();
        remoteSession.suspendRead();
        
        IoBufferAllocatorEx<?> allocator = session.getBufferAllocator();
        WriteFuture writeFuture = session.write(allocator.wrap(ByteBuffer.wrap(ECHO_BYTES)));
        assertFalse(writeFuture.isWritten());
        assertNull(acceptorMessageReceived.get());

        remoteSession.resumeRead();
        writeFuture.awaitUninterruptibly();
        assertEquals(allocator.wrap(ByteBuffer.wrap(ECHO_BYTES)), acceptorMessageReceived.get());
    }

}

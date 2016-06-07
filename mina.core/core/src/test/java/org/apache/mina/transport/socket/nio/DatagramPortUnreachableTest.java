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
package org.apache.mina.transport.socket.nio;

import java.net.InetSocketAddress;
import java.net.PortUnreachableException;

import junit.framework.TestCase;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.service.IoConnector;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.transport.socket.DatagramSessionConfig;
import org.apache.mina.util.AvailablePortFinder;
import org.junit.Ignore;

/**
 * Tests {@link DatagramSessionConfig#setCloseOnPortUnreachable(boolean)}.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
@Ignore("Tests ignored out as per https://github.com/kaazing/tickets/issues/474")
public class DatagramPortUnreachableTest extends TestCase {

    Object mutex = new Object();
    
    private void runTest(boolean closeOnPortUnreachable) throws Exception {
        IoConnector connector = new NioDatagramConnector();
        connector.setHandler(new IoHandlerAdapter() {

            @Override
            public void exceptionCaught(IoSession session, Throwable cause)
                    throws Exception {
                if (cause instanceof PortUnreachableException) {
                    synchronized(mutex) {
                        mutex.notify();
                    }
                }
            }
            
        });
        ConnectFuture future = connector.connect(new InetSocketAddress("localhost", 
                AvailablePortFinder.getNextAvailable(20000)));
        future.awaitUninterruptibly();
        IoSession session = future.getSession();

        DatagramSessionConfig cfg = ((DatagramSessionConfig) session
                .getConfig());
        cfg.setUseReadOperation(true);
        cfg.setCloseOnPortUnreachable(closeOnPortUnreachable);
        
        synchronized(mutex) {
            session.write(IoBuffer.allocate(1)).awaitUninterruptibly().isWritten();
            session.read();
            mutex.wait();
        }
        
        Thread.sleep(500);
        
        assertEquals(closeOnPortUnreachable, session.isClosing());
        connector.dispose();
    }

    public void testPortUnreachableClosesSession() throws Exception {
        // session should be closing
        runTest(true);
    }

    public void testNormal() throws Exception {
        // test that session is not closed on port unreachable exception
        runTest(false);
    }
}

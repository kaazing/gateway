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
package org.apache.mina.transport;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import junit.framework.TestCase;

import org.apache.mina.core.service.IoAcceptor;
import org.apache.mina.core.service.IoConnector;

/**
 * TODO Add documentation
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public abstract class AbstractFileRegionTest extends TestCase {

    private static final int FILE_SIZE = 1 * 1024 * 1024; // 1MB file
    
    protected abstract IoAcceptor createAcceptor();
    protected abstract IoConnector createConnector();

    // Ignored for https://github.com/kaazing/gateway/issues/221
    // Not sure how to ignore in junit.framework with inheritance
    public void testSendLargeFile() throws Throwable {
        /*
        File file = createLargeFile();
        assertEquals("Test file not as big as specified", FILE_SIZE, file.length());
        
        final CountDownLatch latch = new CountDownLatch(1);
        final boolean[] success = {false};
        final Throwable[] exception = {null};
        
        int port = AvailablePortFinder.getNextAvailable(1025);
        IoAcceptor acceptor = createAcceptor();
        acceptor.setHandler(new IoHandlerAdapter() {
            private int index = 0;
            @Override
            public void exceptionCaught(IoSession session, Throwable cause)
                    throws Exception {
                exception[0] = cause;
                session.close(true);
            }
            @Override
            public void messageReceived(IoSession session, Object message) throws Exception {
                IoBuffer buffer = (IoBuffer) message;
                while (buffer.hasRemaining()) {
                    int x = buffer.getInt();
                    if (x != index) {
                        throw new Exception(String.format("Integer at %d was %d but should have been %d", index, x, index));
                    }
                    index++;
                }
                if (index > FILE_SIZE / 4) {
                    throw new Exception("Read too much data");
                }
                if (index == FILE_SIZE / 4) {
                    success[0] = true;
                    session.close(true);
                }
            }
        });
        acceptor.bind(new InetSocketAddress("localhost", port));
        
        IoConnector connector = createConnector();
        connector.setHandler(new IoHandlerAdapter() {
            @Override
            public void exceptionCaught(IoSession session, Throwable cause)
                    throws Exception {
                exception[0] = cause;
                session.close(true);
            }
            @Override
            public void sessionClosed(IoSession session) throws Exception {
                latch.countDown();
            }
        });
        ConnectFuture future = connector.connect(new InetSocketAddress("localhost", port));
        future.awaitUninterruptibly();
        
        IoSession session = future.getSession();
        session.write(file);
        
        latch.await();
        
        if (exception[0] != null) {
            throw exception[0];
        }
        assertTrue("Did not complete file transfer successfully", success[0]);
        
        assertEquals("Written messages should be 1 (we wrote one file)", 1, session.getWrittenMessages());
        assertEquals("Written bytes should match file size", FILE_SIZE, session.getWrittenBytes());
        
        connector.dispose();
        acceptor.dispose();
        */
    }
    
    private File createLargeFile() throws IOException {
        File largeFile = File.createTempFile("mina-test", "largefile");
        largeFile.deleteOnExit();
        FileChannel channel = new FileOutputStream(largeFile).getChannel();
        ByteBuffer buffer = createBuffer();
        channel.write(buffer);
        channel.close();
        return largeFile;
    }
    private ByteBuffer createBuffer() {
        ByteBuffer buffer = ByteBuffer.allocate(FILE_SIZE);
        for (int i = 0; i < FILE_SIZE / 4; i++) {
            buffer.putInt(i);
        }
        buffer.flip();
        return buffer;
    }
    
}

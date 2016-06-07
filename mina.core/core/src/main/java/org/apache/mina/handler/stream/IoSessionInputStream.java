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
package org.apache.mina.handler.stream;

import java.io.IOException;
import java.io.InputStream;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.service.IoHandler;

/**
 * An {@link InputStream} that buffers data read from
 * {@link IoHandler#messageReceived(IoSession, Object)} events.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
class IoSessionInputStream extends InputStream {
    private final Object mutex = new Object();

    private final IoBuffer buf;

    private volatile boolean closed;

    private volatile boolean released;

    private IOException exception;

    public IoSessionInputStream() {
        buf = IoBuffer.allocate(16);
        buf.setAutoExpand(true);
        buf.limit(0);
    }

    @Override
    public int available() {
        if (released) {
            return 0;
        }

        synchronized (mutex) {
            return buf.remaining();
        }
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }

        synchronized (mutex) {
            closed = true;
            releaseBuffer();

            mutex.notifyAll();
        }
    }

    @Override
    public int read() throws IOException {
        synchronized (mutex) {
            if (!waitForData()) {
                return -1;
            }

            return buf.get() & 0xff;
        }
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        synchronized (mutex) {
            if (!waitForData()) {
                return -1;
            }

            int readBytes;

            if (len > buf.remaining()) {
                readBytes = buf.remaining();
            } else {
                readBytes = len;
            }

            buf.get(b, off, readBytes);

            return readBytes;
        }
    }

    private boolean waitForData() throws IOException {
        if (released) {
            return false;
        }

        synchronized (mutex) {
            while (!released && buf.remaining() == 0 && exception == null) {
                try {
                    mutex.wait();
                } catch (InterruptedException e) {
                    IOException ioe = new IOException(
                            "Interrupted while waiting for more data");
                    ioe.initCause(e);
                    throw ioe;
                }
            }
        }

        if (exception != null) {
            releaseBuffer();
            throw exception;
        }

        if (closed && buf.remaining() == 0) {
            releaseBuffer();

            return false;
        }

        return true;
    }

    private void releaseBuffer() {
        if (released) {
            return;
        }

        released = true;
    }

    public void write(IoBuffer src) {
        synchronized (mutex) {
            if (closed) {
                return;
            }

            if (buf.hasRemaining()) {
                this.buf.compact();
                this.buf.put(src);
                this.buf.flip();
            } else {
                this.buf.clear();
                this.buf.put(src);
                this.buf.flip();
                mutex.notifyAll();
            }
        }
    }

    public void throwException(IOException e) {
        synchronized (mutex) {
            if (exception == null) {
                exception = e;

                mutex.notifyAll();
            }
        }
    }
}
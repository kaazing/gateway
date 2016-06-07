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
package org.kaazing.gateway.service.http.proxy;

import javax.net.ServerSocketFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

import static java.nio.charset.StandardCharsets.UTF_8;

class OriginServer implements Runnable {
    final int port;
    private final Handler handler;
    private volatile boolean stopped;
    ServerSocket socket;
    private volatile IOException ioe;        // handler's exception
    private Thread acceptThread;

    interface Handler {
        // Handler must close socket. Also the server calls handle
        // method in a serialized manner.
        void handle(Socket socket) throws IOException;
    }

    OriginServer(int port, Handler handler) {
        this.port = port;
        this.handler = handler;
    }

    void start() throws Exception {
        ServerSocketFactory serverSocketFactory = ServerSocketFactory.getDefault();
        socket = serverSocketFactory.createServerSocket(port);
        acceptThread = new Thread(this, "Origin Server");
        acceptThread.start();
    }

    @Override
    public void run() {
        while (!stopped) {
            try {
                Socket acceptSocket = socket.accept();
                try {
                    handler.handle(acceptSocket);
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                    if (this.ioe != null) {
                        this.ioe = ioe;
                    }
                }
            } catch (IOException ioe) {
                // no-op since socket.accept() may throw IOE
            }
        }
    }

    void stop() throws IOException {
        stopped = true;
        if (socket != null) {
            socket.close();
        }
        if (acceptThread != null) {
            try {
                acceptThread.join();
            } catch (InterruptedException ignore) {
            }
        }
        if (ioe != null) {
            throw ioe;
        }
    }

    /*
     * A simple http server
     */
    static class HttpHandler implements Handler {
        final byte[] resBytes;

        HttpHandler(String res) {
            resBytes = res.getBytes(UTF_8);
        }

        @Override
        public void handle(Socket acceptSocket) throws IOException {
            try (Socket socket = acceptSocket;
                 InputStream in = socket.getInputStream();
                 OutputStream out = socket.getOutputStream()) {

                parseHttpHeaders(in);
                out.write(resBytes);
            }
        }

    }

    enum State {
        START, SLASH_R, SLASH_RN, SLASH_RNR, END
    }

    /*
     * return true if HTTP request is successfully parsed
     *        false otherwise
     */
    static boolean parseHttpHeaders(InputStream in) throws IOException {
        State state = State.START;
        while (state != State.END) {
            int i = in.read();
            if (i == -1) {
                return false;
            }
            switch (state) {
                case START:
                    state = (i == '\r') ? State.SLASH_R : State.START;
                    break;
                case SLASH_R:
                    state = (i == '\n') ? State.SLASH_RN : State.START;
                    break;
                case SLASH_RN:
                    state = (i == '\r') ? State.SLASH_RNR : State.START;
                    break;
                case SLASH_RNR:
                    state = (i == '\n') ? State.END : State.START;
                    break;
            }
        }
        return true;
    }

}

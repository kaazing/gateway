/**
 * Copyright (c) 2007-2014 Kaazing Corporation. All rights reserved.
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.kaazing.gateway.service.http.proxy;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static java.nio.charset.StandardCharsets.UTF_8;

class SecureOriginServer implements Runnable {
    private final int port;
    private final Handler handler;
    private volatile boolean stopped;
    private SSLServerSocket socket;
    private volatile IOException ioe;        // handler's exception

    interface Handler {
        void handle(SSLSocket sslSocket) throws IOException;
    }

    SecureOriginServer(int port, Handler handler) {
        this.port = port;
        this.handler = handler;
    }

    void start() throws Exception {
        SSLServerSocketFactory serverSocketFactory = TlsTestUtil.serverSocketFactory();
        socket = (SSLServerSocket) serverSocketFactory.createServerSocket(port);
        new Thread(this, "SSL Origin Server").start();
    }

    @Override
    public void run() {
        while (!stopped) {
            try(SSLSocket acceptSocket = (SSLSocket) socket.accept()) {
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
        if (ioe != null) {
            throw ioe;
        }
    }



    /*
     * A simple http server
     */
    static class HttpHandler implements Handler {
        final byte[] resBytes;

        enum State {
            START, SLASH_R, SLASH_RN, SLASH_RNR, END
        }

        HttpHandler(String res) {
            resBytes = res.getBytes(UTF_8);
        }

        @Override
        public void handle(SSLSocket acceptSocket) throws IOException {
            try (SSLSocket socket = acceptSocket;
                 InputStream in = socket.getInputStream();
                 OutputStream out = socket.getOutputStream()) {

                parseHttpHeaders(in);
                out.write(resBytes);
            }
        }

        static void parseHttpHeaders(InputStream in) throws IOException {
            State state = State.START;
            while (state != State.END) {
                int i = in.read();
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
        }
    }


}

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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.DisableOnDebug;
import org.junit.rules.TestRule;
import org.junit.rules.Timeout;
import org.kaazing.gateway.server.test.Gateway;
import org.kaazing.gateway.server.test.config.GatewayConfiguration;
import org.kaazing.gateway.server.test.config.builder.GatewayConfigurationBuilder;

import javax.net.ssl.SSLSocketFactory;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.security.KeyStore;

import static java.util.concurrent.TimeUnit.SECONDS;

public class HttpProxyMultipleAuthHeaders {
    private final KeyStore keyStore = TlsTestUtil.keyStore();
    private final char[] password = TlsTestUtil.password();
    private final KeyStore trustStore = TlsTestUtil.trustStore();
    private final SSLSocketFactory clientSocketFactory = TlsTestUtil.clientSocketFactory();

    @Rule
    public TestRule timeout = new DisableOnDebug(new Timeout(10, SECONDS));

    @Test
    public void testExtremelyLargeHeader() throws Exception {
        Gateway gateway = new Gateway();
        // @formatter:off
        GatewayConfiguration configuration =
                new GatewayConfigurationBuilder()
                    .service()
                        .accept("https://localhost:8110")
                        .connect("https://localhost:8080")
                        .type("http.proxy")
                    .done()
                    .security()
                        .trustStore(trustStore)
                        .keyStore(keyStore)
                        .keyStorePassword(password)
                    .done()
                .done();
        // @formatter:on

        SecureOriginServer.Handler handler = new WebSocketServer();
        SecureOriginServer originServer = new SecureOriginServer(8080, handler);

        try {
            originServer.start();
            gateway.start(configuration);

            Socket socket = clientSocketFactory.createSocket("localhost", 8110);
            WebSocketClient client = new WebSocketClient();
            client.handle(socket);
        } finally {
            gateway.stop();
            originServer.stop();
        }

    }

    private static class WebSocketClient {

        // byte[] b = string.getBytes();

        public byte[] getHttpRequest (){
            StringBuilder request = new StringBuilder(9999999);

            request.append("GET /echo HTTP/1.1\r\n" +
                    "Upgrade: websocket\r\n" +
                    "Connection: Upgrade\r\n" +
                    "User-Agent: Mozilla/5.0 (Macintosh; Intel Mac OS X 10.6; rv:8.0) Gecko/20100101 Firefox/8.0\r\n" +
                    "Host: localhost:8110\r\n" +
                    "Origin: http://localhost:8110\r\n" +
                    "Sec-WebSocket-Key: nDaimG37f4nUqogPypithw==\r\n" +
                    "Sec-WebSocket-Version: 13\r\n");
            
            request.append("X-ExtraHeader: ");

            for (long i=0; i < 9999999; i++){
                request.append("1");
            }

            request.append("\r\n"+"\r\n");
            return request.toString().getBytes();
        }

        void handle(Socket clientSocket) throws IOException {
            try (Socket socket = clientSocket;
                 InputStream in = socket.getInputStream();
                 OutputStream out = socket.getOutputStream()) {

                // write and read HTTP request and response headers
                out.write(getHttpRequest());
                OriginServer.parseHttpHeaders(in);

                int eof = in.read();
                if (eof != -1) {
                    throw new IOException("Gateway <--> Server is closed. Expected closing of client connection");
                }
            }
        }

    }

    private static class WebSocketServer implements OriginServer.Handler {

        public byte[] getHttpResponse (){
            StringBuilder request = new StringBuilder();

            request.append("HTTP/1.1 101 Web Socket Protocol Handshake\r\n" +
                    "Connection: Upgrade\r\n" +
                    "Date: Wed, 04 Mar 2015 02:26:37 GMT\r\n" +
                    "Sec-WebSocket-Accept: Uq2pD+MOrXQIut+yUNUP6dvhWBw=\r\n" +
                    "Server: Kaazing Gateway\r\n" +
                    "Upgrade: websocket\r\n");

            request.append("X-ExtraHeader: .*/");
            request.append("\r\n"+"\r\n");

            return request.toString().getBytes();
        }

        @Override
        public void handle(Socket serverSocket) throws IOException {
            try(Socket socket = serverSocket;
                InputStream in = socket.getInputStream();
                OutputStream out = socket.getOutputStream()) {

                // read and write HTTP request and response headers
                OriginServer.parseHttpHeaders(in);
                out.write(getHttpResponse());
            }
        }
    }

    static void readFully(InputStream in, byte b[]) throws IOException {
        int n = 0;
        while (n < b.length) {
            int count = in.read(b, n, b.length - n);
            if (count < 0)
                throw new EOFException();
            n += count;
        }
    }

}

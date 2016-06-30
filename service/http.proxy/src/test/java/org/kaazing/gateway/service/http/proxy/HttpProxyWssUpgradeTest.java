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

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.concurrent.TimeUnit.SECONDS;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.security.KeyStore;

import javax.net.ssl.SSLSocketFactory;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.DisableOnDebug;
import org.junit.rules.TestRule;
import org.junit.rules.Timeout;
import org.kaazing.gateway.server.test.Gateway;
import org.kaazing.gateway.server.test.config.GatewayConfiguration;
import org.kaazing.gateway.server.test.config.builder.GatewayConfigurationBuilder;
import org.kaazing.gateway.util.feature.EarlyAccessFeatures;

public class HttpProxyWssUpgradeTest {
    private final KeyStore keyStore = TlsTestUtil.keyStore();
    private final char[] password = TlsTestUtil.password();
    private final KeyStore trustStore = TlsTestUtil.trustStore();
    private final SSLSocketFactory clientSocketFactory = TlsTestUtil.clientSocketFactory();

    @Rule
    public TestRule timeout = new DisableOnDebug(new Timeout(10, SECONDS));

    @Test
    public void upgradeSecureWebSocket() throws Exception {
        Gateway gateway = new Gateway();
        // @formatter:off
        GatewayConfiguration configuration =
                new GatewayConfigurationBuilder()
                    .property(EarlyAccessFeatures.HTTP_PROXY_SERVICE.getPropertyName(), "true")
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
        static final byte[] HTTP_REQUEST =
                ("GET /echo HTTP/1.1\r\n" +
                "Upgrade: websocket\r\n" +
                "Connection: Upgrade\r\n" +
                "User-Agent: Mozilla/5.0 (Macintosh; Intel Mac OS X 10.6; rv:8.0) Gecko/20100101 Firefox/8.0\r\n" +
                "Host: localhost:8110\r\n" +
                "Origin: http://localhost:8110\r\n" +
                "Sec-WebSocket-Key: nDaimG37f4nUqogPypithw==\r\n" +
                "Sec-WebSocket-Version: 13\r\n" +
                "\r\n").getBytes(UTF_8);

        static final byte[] TEXT_FRAME = new byte[] {
                (byte) 0x81, (byte) 0x89, (byte) 0x94, 0x4e, (byte) 0xc6, 0x1c, (byte) 0xf2, 0x3c, (byte) 0xa7, 0x7b,
                (byte) 0xf9, 0x2b, (byte) 0xa8, 0x68, (byte) 0xa5 };

        static final byte[] CLOSE_FRAME = new byte[] {
                (byte) 0x88, (byte) 0x82, 0x28, 0x06, (byte) 0xea, 0x57, 0x2b, (byte) 0xee };

        void handle(Socket clientSocket) throws IOException {
            try (Socket socket = clientSocket;
                 InputStream in = socket.getInputStream();
                 OutputStream out = socket.getOutputStream()) {

                // write and read HTTP request and response headers
                out.write(HTTP_REQUEST);
                OriginServer.parseHttpHeaders(in);

                // write and read websocket text frame
                out.write(TEXT_FRAME);
                readFully(in, new byte[11]);

                // write and read websocket text frame
                out.write(TEXT_FRAME);
                readFully(in, new byte[11]);

                // write and read websocket close frame
                out.write(CLOSE_FRAME);
                readFully(in, new byte[4]);

                int eof = in.read();
                if (eof != -1) {
                    throw new IOException("Gateway <--> Server is closed. Expected closing of client connection");
                }
            }
        }

    }

    private static class WebSocketServer implements OriginServer.Handler {
        static final byte[] HTTP_RESPONSE =
                ("HTTP/1.1 101 Web Socket Protocol Handshake\r\n" +
                "Connection: Upgrade\r\n" +
                "Date: Wed, 04 Mar 2015 02:26:37 GMT\r\n" +
                "Sec-WebSocket-Accept: Uq2pD+MOrXQIut+yUNUP6dvhWBw=\r\n" +
                "Server: Kaazing Gateway\r\n" +
                "Upgrade: websocket\r\n" +
                "\r\n").getBytes(UTF_8);

        static final byte[] TEXT_FRAME =
                new byte[] {(byte) 0x81, 0x09, 0x66, 0x72, 0x61, 0x67, 0x6D, 0x65, 0x6E, 0x74, 0x31};

        static final byte[] CLOSE_FRAME = new byte[] {(byte) 0x88, 0x02, 0x03, (byte) 0xe8};

        @Override
        public void handle(Socket serverSocket) throws IOException {
            try(Socket socket = serverSocket;
                InputStream in = socket.getInputStream();
                OutputStream out = socket.getOutputStream()) {

                // read and write HTTP request and response headers
                OriginServer.parseHttpHeaders(in);
                out.write(HTTP_RESPONSE);

                // read and write websocket text frame
                readFully(in, new byte[15]);
                out.write(TEXT_FRAME);

                // read and write websocket text frame
                readFully(in, new byte[15]);
                out.write(TEXT_FRAME);

                // read and write websocket close frame
                readFully(in, new byte[8]);
                out.write(CLOSE_FRAME);
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

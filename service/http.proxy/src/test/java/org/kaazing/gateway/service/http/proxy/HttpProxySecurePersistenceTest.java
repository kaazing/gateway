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

public class HttpProxySecurePersistenceTest {
    private final KeyStore keyStore = TlsTestUtil.keyStore();
    private final char[] password = TlsTestUtil.password();
    private final KeyStore trustStore = TlsTestUtil.trustStore();
    private final SSLSocketFactory clientSocketFactory = TlsTestUtil.clientSocketFactory();

    private static final int KEEP_ALIVE_TIMEOUT = 5;

    @Rule
    public TestRule timeout = new DisableOnDebug(new Timeout(15, SECONDS));

    @Test
    public void securePersistentConnection() throws Exception {
        Gateway gateway = new Gateway();
        // @formatter:off
        GatewayConfiguration configuration =
                new GatewayConfigurationBuilder()
                    .property(EarlyAccessFeatures.HTTP_PROXY_SERVICE.getPropertyName(), "true")
                    .service()
                        .accept("https://localhost:8110")
                        .connect("https://localhost:8080")
                        .type("http.proxy")
                        .connectOption("http.keepalive.timeout", String.valueOf(KEEP_ALIVE_TIMEOUT))
                    .done()
                    .security()
                        .trustStore(trustStore)
                        .keyStore(keyStore)
                        .keyStorePassword(password)
                    .done()
                .done();
        // @formatter:on

        SecureOriginServer.Handler handler = new HttpServer();
        SecureOriginServer originServer = new SecureOriginServer(8080, handler);

        try {
            originServer.start();
            gateway.start(configuration);

            Socket socket = clientSocketFactory.createSocket("localhost", 8110);
            HttpClient client = new HttpClient();
            client.handle(socket);
        } finally {
            gateway.stop();
            originServer.stop();
        }

    }

    private static class HttpClient {
        static final byte[] HTTP_REQUEST =
                ("GET / HTTP/1.1\r\n" +
                "Host: localhost:8110\r\n" +
                "Cache-Control: max-age=0\r\n" +
                "Accept: text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8\r\n" +
                "User-Agent: Mozilla/5.0 (Macintosh; Intel Mac OS X 10.6; rv:8.0) Gecko/20100101 Firefox/8.0\r\n" +
                "Accept-Encoding: gzip, deflate, sdch\r\n" +
                "Accept-Language: en-US,en;q=0.8\r\n" +
                "\r\n").getBytes(UTF_8);


        void handle(Socket clientSocket) throws IOException {
            try (Socket socket = clientSocket;
                 InputStream in = socket.getInputStream();
                 OutputStream out = socket.getOutputStream()) {

                for(int i=0; i < 3; i++) {
                    // read and write HTTP request and response headers
                    out.write(HTTP_REQUEST);
                    OriginServer.parseHttpHeaders(in);
                    readFully(in, new byte[31]);
                }

                // sleep so that server persistent connection times out
                try {
                    Thread.sleep(KEEP_ALIVE_TIMEOUT*1000 + 3);
                } catch (Exception e) {
                    // no-op
                }
            }
        }

    }

    private static class HttpServer implements OriginServer.Handler {
        static final byte[] HTTP_RESPONSE =
                ("HTTP/1.1 200 OK\r\n" +
                "Server: Apache-Coyote/1.1\r\n" +
                "Content-Type: text/html;charset=UTF-8\r\n" +
                "Transfer-Encoding: chunked\r\n" +
                "Date: Tue, 10 Feb 2015 02:17:15 GMT\r\n" +
                "\r\n" +
                "14\r\n" +
                "<html>Hellooo</html>\r\n" +
                "0\r\n" +
                "\r\n").getBytes(UTF_8);

        @Override
        public void handle(Socket serverSocket) throws IOException {
            try(Socket socket = serverSocket;
                InputStream in = socket.getInputStream();
                OutputStream out = socket.getOutputStream()) {

                for(int i=0; i < 3; i++) {
                    // read and write HTTP request and response headers
                    OriginServer.parseHttpHeaders(in);
                    out.write(HTTP_RESPONSE);
                }

                int eof = in.read();
                if (eof != -1) {
                    throw new IOException("Gateway <--> Server should close due to keep alive timeout.");
                }
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

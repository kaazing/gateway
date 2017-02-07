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
import static org.junit.Assert.assertEquals;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import javax.net.SocketFactory;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.kaazing.gateway.server.test.Gateway;
import org.kaazing.gateway.server.test.config.GatewayConfiguration;
import org.kaazing.gateway.server.test.config.builder.GatewayConfigurationBuilder;
import org.kaazing.gateway.util.feature.EarlyAccessFeatures;
import org.kaazing.test.util.ITUtil;
import static org.kaazing.gateway.util.InternalSystemProperty.TCP_PROCESSOR_COUNT;

public class HttpProxyPersistenceTest {

    private static final int KEEP_ALIVE_TIMEOUT = 5;
    private static final int KEEP_ALIVE_CONNECTIONS = 2;

    @Rule
    public TestRule timeout = ITUtil.createRuleChain(15, SECONDS);

    @Test
    public void maxPersistentIdleConnections() throws Exception {
        Gateway gateway = new Gateway();
        // @formatter:off
        GatewayConfiguration configuration =
                new GatewayConfigurationBuilder()
                    .property(EarlyAccessFeatures.HTTP_PROXY_SERVICE.getPropertyName(), "true")
                    .service()
                        .accept("http://localhost:8110")
                        .connect("http://localhost:8080")
                        .type("http.proxy")
                        .connectOption("http.keepalive.timeout", String.valueOf(KEEP_ALIVE_TIMEOUT))
                        .connectOption("http.keepalive.connections", String.valueOf(KEEP_ALIVE_CONNECTIONS))
                    .done()
                    .property(TCP_PROCESSOR_COUNT.getPropertyName(), "1")
                .done();
        // @formatter:on

        ServerHandler handler = new ServerHandler();
        OriginServer originServer = new OriginServer(8080, handler);

        try {
            originServer.start();
            gateway.start(configuration);

            // Send 4 requests concurrently
            Thread t1 = new Thread(new HttpClient());
            Thread t2 = new Thread(new HttpClient());
            Thread t3 = new Thread(new HttpClient());
            Thread t4 = new Thread(new HttpClient());
            t1.start(); t2.start(); t3.start(); t4.start();
            t1.join(); t2.join(); t3.join(); t4.join();
            // server should have received all the 4 connections
            // pool should have cached only max configured connections = 2
            assertEquals(4, handler.getConnections());

            // Send 4 more requests concurrently
            t1 = new Thread(new HttpClient());
            t2 = new Thread(new HttpClient());
            t3 = new Thread(new HttpClient());
            t4 = new Thread(new HttpClient());
            t1.start(); t2.start(); t3.start(); t4.start();
            t1.join(); t2.join(); t3.join(); t4.join();
            // gateway would have used 2 connections from pool and created 2 more new connections
            // So server should have received 2 more new connections
            assertEquals(6, handler.getConnections());
        } finally {
            gateway.stop();
            originServer.stop();
        }
    }

    private static class HttpClient implements Runnable {
        static final byte[] HTTP_REQUEST =
                ("GET / HTTP/1.1\r\n" +
                "Host: localhost:8110\r\n" +
                "Cache-Control: max-age=0\r\n" +
                "Accept: text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8\r\n" +
                "User-Agent: Mozilla/5.0 (Macintosh; Intel Mac OS X 10.6; rv:8.0) Gecko/20100101 Firefox/8.0\r\n" +
                "Accept-Encoding: gzip, deflate, sdch\r\n" +
                "Accept-Language: en-US,en;q=0.8\r\n" +
                "\r\n").getBytes(UTF_8);

        @Override
        public void run() {
            try (Socket socket = SocketFactory.getDefault().createSocket("localhost", 8110);
                 InputStream in = socket.getInputStream();
                 OutputStream out = socket.getOutputStream()) {

                // read and write HTTP request and response headers
                out.write(HTTP_REQUEST);
                OriginServer.parseHttpHeaders(in);
                readFully(in, new byte[31]);
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }

    }

    private static class ServerHandler implements OriginServer.Handler {

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

        private int connections;

        @Override
        public void handle(Socket serverSocket) throws IOException {
            connections++;
            new Thread(() -> {
                try(Socket socket = serverSocket;
                    InputStream in = socket.getInputStream();
                    OutputStream out = socket.getOutputStream()) {

                    // read and write HTTP request and response headers
                    while(OriginServer.parseHttpHeaders(in)) {
                        Thread.sleep(2000);
                        out.write(HTTP_RESPONSE);
                        out.flush();
                    }
                } catch(Exception ioe) {
                    ioe.printStackTrace();
                }
            }).start();
        }

        int getConnections() {
            return connections;
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

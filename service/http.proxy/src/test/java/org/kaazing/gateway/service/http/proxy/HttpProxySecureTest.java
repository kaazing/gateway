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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.security.KeyStore;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.kaazing.gateway.server.test.Gateway;
import org.kaazing.gateway.server.test.config.GatewayConfiguration;
import org.kaazing.gateway.server.test.config.builder.GatewayConfigurationBuilder;
import org.kaazing.gateway.util.feature.EarlyAccessFeatures;
import org.kaazing.test.util.MethodExecutionTrace;

public class HttpProxySecureTest {
    private final KeyStore keyStore = TlsTestUtil.keyStore();
    private final char[] password = TlsTestUtil.password();
    private final KeyStore trustStore = TlsTestUtil.trustStore();
    private final SSLSocketFactory clientSocketFactory = TlsTestUtil.clientSocketFactory();

    @Rule
    public TestRule testExecutionTrace = new MethodExecutionTrace();

    // client <---- ssl/http ---> gateway <---- ssl/http -----> origin server
    @Test(timeout = 5000)
    public void proxyWithTLS() throws Exception {
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

        String response =
                "HTTP/1.1 200 OK\r\n" +
                "Server: Apache-Coyote/1.1\r\n" +
                "Content-Type: text/html;charset=UTF-8\r\n" +
                "Transfer-Encoding: chunked\r\n" +
                "Date: Tue, 10 Feb 2015 02:17:15 GMT\r\n" +
                "Connection: close\r\n" +
                "\r\n" +
                "14\r\n" +
                "<html>Hellooo</html>\r\n" +
                "0\r\n" +
                "\r\n";
        OriginServer.HttpHandler handler = new OriginServer.HttpHandler(response);
        SecureOriginServer originServer = new SecureOriginServer(8080, handler);

        try {
            originServer.start();
            gateway.start(configuration);

            HttpsURLConnection con  = (HttpsURLConnection) new URL("https://localhost:8110/index.html").openConnection();
            con.setSSLSocketFactory(clientSocketFactory);
            try(BufferedReader r = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
                String line = r.readLine();
                assertEquals("<html>Hellooo</html>", line);
                assertNull(r.readLine());
            }
        } finally {
            gateway.stop();
            originServer.stop();
        }
    }

}

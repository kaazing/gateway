/**
 * Copyright 2007-2015, Kaazing Corporation. All rights reserved.
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

import java.net.URI;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.kaazing.gateway.server.test.Gateway;
import org.kaazing.gateway.server.test.config.builder.GatewayConfigurationBuilder;

public class HttpProxyIdenticalAcceptConnectIT {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void shouldFailWhenIdenticalAcceptAndConnect() throws Exception {
        thrown.expect(RuntimeException.class);
        thrown.expectMessage("Different <accept> and <connect> URIs should be provided for service of type http.proxy");
        Gateway gateway = new Gateway();
        // @formatter:off
        gateway.start(new GatewayConfigurationBuilder()
                .service()
                .accept(URI.create("http://localhost:8080/"))
                .connect(URI.create("http://localhost:8080/"))
                .type("http.proxy")
            .done()
        .done());
        // @formatter:on
    }
}

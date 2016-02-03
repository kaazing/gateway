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

import org.junit.Test;
import org.kaazing.gateway.server.config.parse.GatewayConfigParser;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URI;
import java.net.URL;

public class HttpProxyConfigTest {

    @Test
    public void testValidBalancedService() throws Exception {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        URL configUrl = classLoader.getResource("http-proxy-config.xml");
        if (configUrl == null) {
            throw new FileNotFoundException("File http-proxy-config.xml not found");
        }
        URI file = configUrl.toURI();

        GatewayConfigParser parser = new GatewayConfigParser();
        parser.parse(new File(file));
    }

}

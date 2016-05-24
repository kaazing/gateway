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

import javax.net.ssl.SSLServerSocketFactory;

class SecureOriginServer extends OriginServer {

    SecureOriginServer(int port, Handler handler) {
        super(port, handler);
    }

    @Override
    void start() throws Exception {
        SSLServerSocketFactory serverSocketFactory = TlsTestUtil.serverSocketFactory();
        socket = serverSocketFactory.createServerSocket(port);
        new Thread(this, "SSL Origin Server").start();
    }

}

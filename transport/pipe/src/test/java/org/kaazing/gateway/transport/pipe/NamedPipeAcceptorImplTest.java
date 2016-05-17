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
package org.kaazing.gateway.transport.pipe;

import java.io.IOException;

import org.apache.mina.core.service.IoHandlerAdapter;
import org.junit.Test;
import org.kaazing.gateway.transport.NamedPipeAddress;

public class NamedPipeAcceptorImplTest {

	@Test(expected = IOException.class)
    public void duplicatedBindShouldThrowException() throws Exception {
        NamedPipeAcceptorImpl acceptor = new NamedPipeAcceptorImpl();
        acceptor.setHandler(new IoHandlerAdapter());
        NamedPipeAddress localAddress = new NamedPipeAddress("duplicated");
        acceptor.bind(localAddress);
        acceptor.bind(localAddress);
    }

	@Test
    public void unrecognizedUnbindShouldNotThrowException() throws Exception {
        NamedPipeAcceptorImpl acceptor = new NamedPipeAcceptorImpl();
        acceptor.setHandler(new IoHandlerAdapter());
        NamedPipeAddress localAddress = new NamedPipeAddress("unrecognized");
        acceptor.unbind(localAddress);
    }

}

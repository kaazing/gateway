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
package org.kaazing.gateway.transport.ssl;

import static org.junit.Assert.assertFalse;

import java.nio.ByteBuffer;
import java.security.KeyStore;

import org.apache.mina.core.filterchain.IoFilterChain;
import org.apache.mina.core.service.IoHandler;
import org.apache.mina.filter.ssl.SslContextFactory;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.Test;
import org.slf4j.Logger;

import org.kaazing.gateway.transport.ssl.bridge.filter.SslFilter;
import org.kaazing.mina.core.buffer.IoBufferAllocatorEx;
import org.kaazing.mina.core.buffer.IoBufferEx;
import org.kaazing.mina.core.session.DummySessionEx;

public class SslAcceptFilterTest {

    @Test
    public void sendClientHello() throws Exception {

        Mockery context = new Mockery();

        String keyStorePassword = "testing";
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        keyStore.load(getClass().getResourceAsStream("/keystore-testing.db"), keyStorePassword.toCharArray());

        final IoHandler handler = context.mock(IoHandler.class);
        final DummySessionEx session = new DummySessionEx();
        IoBufferAllocatorEx<?> allocator = session.getBufferAllocator();
        session.setHandler(handler);

        final SslContextFactory sslContextFactory = new SslContextFactory();
        sslContextFactory.setKeyManagerFactoryKeyStore(keyStore);
        sslContextFactory.setKeyManagerFactoryKeyStorePassword(keyStorePassword);

        final Logger logger = context.mock(Logger.class);

        IoBufferEx in = allocator.wrap(ByteBuffer.wrap(new byte[] {
            0x16, 0x03, 0x01, 0x00, 0x68, 0x01, 0x00, 0x00,
            0x64, 0x03, 0x01, 0x48,-0x2f, 0x38,-0x46,-0x75,
           -0x6c, 0x1f,-0x47,-0x72,-0x03,-0x74,-0x39,-0x28,
           -0x0b, 0x7e, 0x4a,-0x19,-0x44, 0x30,-0x46, 0x71,
           -0x37, 0x12, 0x71, 0x67, 0x34,-0x07,-0x7d,-0x57,
           -0x08,-0x33, 0x6f, 0x00, 0x00, 0x24, 0x00,-0x78,
            0x00,-0x79, 0x00, 0x39, 0x00, 0x38, 0x00,-0x7c,
            0x00, 0x35, 0x00, 0x45, 0x00, 0x44, 0x00, 0x33,
            0x00, 0x32, 0x00, 0x41, 0x00, 0x04, 0x00, 0x05,
            0x00, 0x2f, 0x00, 0x16, 0x00, 0x13,-0x02,-0x01,
            0x00, 0x0a, 0x01, 0x00, 0x00, 0x17, 0x00, 0x00,
            0x00, 0x0f, 0x00, 0x0d, 0x00, 0x00, 0x0a, 0x74,
            0x65, 0x73, 0x74, 0x2e, 0x6c, 0x6f, 0x63, 0x61,
            0x6c, 0x00, 0x23, 0x00, 0x00 }));

        context.checking(new Expectations() {
            {
                allowing(logger).isTraceEnabled(); will(returnValue(false));
                allowing(logger).isDebugEnabled(); will(returnValue(false));
                never(handler);
            }
        });

        // prevent session.writeRequestQueue.flush()
        session.suspendWrite();

        IoFilterChain filterChain = session.getFilterChain();
        filterChain.addFirst("ssl", new SslFilter(sslContextFactory.newInstance(), false, logger));
        filterChain.fireMessageReceived(in);

        context.assertIsSatisfied();
        assertFalse(session.getWriteRequestQueue().isEmpty(session));
    }
}

package org.kaazing.gateway.service.proxy;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.util.concurrent.CountDownLatch;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.kaazing.gateway.service.ServiceProperties;
import org.kaazing.mina.core.session.IoSessionEx;

public class TestExtension implements ProxyServiceExtensionSpi {
    static CountDownLatch latch;

    public TestExtension() {
        latch = new CountDownLatch(3);
    }

    @Override
    public void initAcceptSession(IoSession session, ServiceProperties properties) {
        latch.countDown();
    }
    @Override
    public void initConnectSession(IoSession session, ServiceProperties properties) {
        latch.countDown();
    }
    @Override
    public void proxiedConnectionEstablished(IoSessionEx acceptSession, IoSessionEx connectSession) {
        connectSession.write(IoBuffer.wrap("injected".getBytes(UTF_8)));
        latch.countDown();
    }

}

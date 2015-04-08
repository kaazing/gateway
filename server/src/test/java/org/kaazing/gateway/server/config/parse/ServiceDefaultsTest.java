package org.kaazing.gateway.server.config.parse;

import java.net.URI;
import java.util.Collection;

import org.junit.Assert;
import org.junit.Test;
import org.kaazing.gateway.server.context.GatewayContext;
import org.kaazing.gateway.server.impl.GatewayImplTest;
import org.kaazing.gateway.server.test.Gateway;
import org.kaazing.gateway.server.test.config.GatewayConfiguration;
import org.kaazing.gateway.server.test.config.builder.GatewayConfigurationBuilder;
import org.kaazing.gateway.service.ConnectOptionsContext;
import org.kaazing.gateway.service.ServiceContext;

public class ServiceDefaultsTest {

    @Test
    public void testDefaultConnectOptions() throws Exception {
        //@formatter:off
        final String sslCipherValue = "LOW";

        GatewayConfiguration gc = new GatewayConfigurationBuilder()
            .serviceDefaults()
                .connectOption("ssl.ciphers", sslCipherValue)
                .connectOption("ssl.protocols", "TLSv1")
                .connectOption("ssl.encryption", "disabled")
                .connectOption("udp.interface", "en0")
                .connectOption("tcp.transport", "socks://localhost:8000")
                .connectOption("http.keepalive", "disabled")
                .connectOption("http.keepaliveTimeout", "5sec")
                .done()
            .service()
                .type("echo")
                .name("test1")
                .accept(URI.create("ws://localhost:8000"))
            .done()
        .done();
        //@formatter:on

        Gateway gateway = new Gateway();
        GatewayContext gatewayContext = gateway.createGatewayContext(gc);
        ServiceContext service = (ServiceContext) gatewayContext.getServices().toArray()[0];
        ConnectOptionsContext connectOptionsContext = service.getConnectOptionsContext();
        // LOW Ciphers get converted to real ssl ciphers
        Assert.assertTrue("SSL_DHE_DSS_WITH_DES_CBC_SHA".equals(connectOptionsContext.getSslCiphers()[0]));
        Assert.assertTrue("TLSv1".equals(connectOptionsContext.getSslProtocols()[0]));
        Assert.assertTrue("en0".equals(connectOptionsContext.asOptionsMap().get("udp.interface")));
        System.out.println(connectOptionsContext.getTcpTransport());
        Assert.assertTrue("socks://localhost:8000".equals(connectOptionsContext.getTcpTransport().toString().trim()));
        Assert.assertFalse(connectOptionsContext.isSslEncryptionEnabled());

        Assert.assertEquals(Integer.valueOf(5), connectOptionsContext.getHttpKeepaliveTimeout());
        Assert.assertFalse(connectOptionsContext.isHttpKeepaliveEnabled());
    }
}

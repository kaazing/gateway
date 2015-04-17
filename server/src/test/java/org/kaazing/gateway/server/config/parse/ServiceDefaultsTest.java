package org.kaazing.gateway.server.config.parse;

import static org.kaazing.gateway.service.TransportOptionNames.HTTP_KEEP_ALIVE;
import static org.kaazing.gateway.service.TransportOptionNames.HTTP_KEEP_ALIVE_TIMEOUT_KEY;

import java.net.URI;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.kaazing.gateway.server.context.GatewayContext;
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
                .connectOption("http.keepalive.timeout", "5sec")
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
        Map<String, Object> connectOptionsMap = connectOptionsContext.asOptionsMap();
        Assert.assertNotNull(((String[]) connectOptionsMap.get("ssl.ciphers"))[0]);

        String[] sslProtocols = (String[]) connectOptionsMap.get("ssl.protocols");
        Assert.assertTrue("TLSv1".equals(sslProtocols[0]));

        Assert.assertTrue("en0".equals(connectOptionsMap.get("udp.interface")));
        System.out.println(connectOptionsMap.get("tcp.transport"));
        Assert.assertTrue("socks://localhost:8000".equals(connectOptionsMap.get("tcp.transport").toString().trim()));
        Assert.assertTrue("disabled".equalsIgnoreCase((String) connectOptionsMap.get("ssl.encryption")));

        Assert.assertEquals(Integer.valueOf(5), (Integer) connectOptionsMap.get(HTTP_KEEP_ALIVE_TIMEOUT_KEY));
        Assert.assertFalse((Boolean) connectOptionsMap.get(HTTP_KEEP_ALIVE));
    }
}

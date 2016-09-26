package org.kaazing.gateway.transport.nio.internal.extensions;

import static org.kaazing.test.util.ITUtil.createRuleChain;

import java.util.HashMap;
import java.util.Map;

import org.apache.mina.core.buffer.IoBuffer;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.transport.IoHandlerAdapter;
import org.kaazing.gateway.transport.tcp.specification.DataMatcher;
import org.kaazing.gateway.transport.tcp.specification.TcpAcceptorRule;
import org.kaazing.k3po.junit.rules.K3poRule;
import org.kaazing.mina.core.session.IoSessionEx;

public class TcpRealmExtensionIT {

    private final K3poRule k3po = new K3poRule();

    private TcpAcceptorRule acceptor = new TcpAcceptorRule();

    @Rule
    public TestRule chain = createRuleChain(acceptor, k3po);

    @Test
    public void shouldAcceptIPAddressIT() {
        Map<String, Object> acceptOptions = new HashMap<>();
        acceptOptions.put("tcp.realmName", "testRealm");
        acceptOptions.put("tcp.loginContextFactory", "testRealm");
        acceptor.bind(acceptor.getAddressFactory().newResourceAddress("tcp://localhost:8000", acceptOptions),
                new IoHandlerAdapter<IoSessionEx>() {
                    @Override
                    protected void doMessageReceived(IoSessionEx session, Object message) throws Exception {
                        
                    }
                });
    }

    @Test
    public void shouldNotAcceptIPAddressIT() {

    }
}

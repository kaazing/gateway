package org.kaazing.gateway.transport.nio.internal.extensions;

import static org.kaazing.gateway.resource.address.tcp.TcpResourceAddress.REALM_NAME;

import org.apache.mina.core.session.IoSession;
import org.kaazing.gateway.resource.address.tcp.TcpResourceAddress;
import org.kaazing.gateway.transport.nio.TcpExtension;

public class TcpRealmExtension implements TcpExtension{

    @Override
    public void initializeSession(IoSession session) {
        String realmName = (String) session.getAttribute(REALM_NAME);
        if(realmName == null){
            // do nothing
        }
    }

}

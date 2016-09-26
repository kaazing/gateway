package org.kaazing.gateway.transport.nio.internal.extensions;

import static org.kaazing.gateway.transport.BridgeSession.REMOTE_ADDRESS;

import java.net.InetAddress;
import java.net.SocketAddress;
import java.net.URI;
import java.net.UnknownHostException;

import javax.security.auth.login.LoginContext;

import org.apache.mina.core.filterchain.IoFilter.NextFilter;
import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.resource.address.tcp.TcpResourceAddress;
import org.kaazing.gateway.security.LoginContextFactory;
import org.kaazing.gateway.security.TypedCallbackHandlerMap;
import org.kaazing.gateway.security.auth.InetAddressCallbackHandler;
import org.kaazing.gateway.server.spi.security.InetAddressCallback;
import org.kaazing.gateway.transport.IoFilterAdapter;
import org.kaazing.mina.core.session.IoSessionEx;

public class TcpSecurityFilter extends IoFilterAdapter<IoSessionEx> {
    // HttpLoginSecurityFilter is the example

    @Override
    protected void doMessageReceived(NextFilter nextFilter, IoSessionEx session, Object message) throws Exception {
        LoginContextFactory loginContextFactory =
                (LoginContextFactory) session.getAttribute(TcpResourceAddress.LOGIN_CONTEXT_FACTORY);
        TypedCallbackHandlerMap callbackHandlerMap = new TypedCallbackHandlerMap();
        addCallbacks(callbackHandlerMap, session);

        LoginContext ctx = loginContextFactory.createLoginContext(callbackHandlerMap);
        try {
            ctx.login();
        } catch (Exception e) {
            // TODO kill session;
        }
        // allow to login
    }

    // The IP whitelist callback is needed
    void addCallbacks(TypedCallbackHandlerMap callbackHandlerMap, IoSessionEx session){
        ResourceAddress resourceAddress = REMOTE_ADDRESS.get(session);
        // TODO convert
        InetAddress remoteAddr = session.getRemoteAddress()

        try {
            remoteAddr = InetAddress.getByName(remoteIpAddress);
        }
        catch (UnknownHostException e) {
            if (logger.isTraceEnabled()) {
                logger.trace(e.getMessage());
            }

            throw new IllegalStateException(e);
        }

        InetAddressCallbackHandler inetAddressCallbackHandler = new InetAddressCallbackHandler(remoteAddr);
        callbackHandlerMap.put(InetAddressCallback.class, inetAddressCallbackHandler);
    }
}

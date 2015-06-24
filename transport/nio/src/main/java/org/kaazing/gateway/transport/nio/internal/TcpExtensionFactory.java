package org.kaazing.gateway.transport.nio.internal;

import static java.util.ServiceLoader.load;

import java.util.Collection;
import java.util.List;
import java.util.ServiceLoader;

import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.transport.nio.TcpExtension;
import org.kaazing.gateway.transport.nio.TcpExtensionFactorySpi;

public interface TcpExtensionFactory {

    public abstract List<TcpExtension> bind(ResourceAddress address);

    public abstract Collection<TcpExtensionFactorySpi> availableExtensions();

    /**
     * Creates a new instance of WebSocketExtensionFactory. It uses the specified {@link ClassLoader} to load
     * {@link WebSocketExtensionFactorySpi} objects that are registered using META-INF/services.
     *
     * @return WebSocketExtensionFactory
     */
    public static TcpExtensionFactory newInstance(ClassLoader cl) {
        ServiceLoader<TcpExtensionFactorySpi> services = load(TcpExtensionFactorySpi.class, cl);
        return TcpExtensionFactoryImpl.newInstance(services);
    }

    /**
     * Creates a new instance of WebSocketExtensionFactory. It uses the default {@link ClassLoader} to load
     * {@link WebSocketExtensionFactorySpi} objects that are registered using META-INF/services.
     *
     * @return WebSocketExtensionFactory
     */
    public static TcpExtensionFactory newInstance() {
        ServiceLoader<TcpExtensionFactorySpi> services = load(TcpExtensionFactorySpi.class);
        return TcpExtensionFactoryImpl.newInstance(services);
    }

}
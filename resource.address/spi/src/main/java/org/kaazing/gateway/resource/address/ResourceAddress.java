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
package org.kaazing.gateway.resource.address;

import static java.lang.String.format;
import static java.net.NetworkInterface.getNetworkInterfaces;
import static java.util.Arrays.asList;
import static java.util.Collections.list;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.kaazing.gateway.resource.address.uri.URIUtils;

public abstract class ResourceAddress extends SocketAddress implements ResourceOptions {

    private static final long serialVersionUID = 1L;

    public static final ResourceOption<String> NEXT_PROTOCOL = new NextProtocolOption();
    public static final ResourceOption<String> TRANSPORT_URI = new TransportURIStringOption();
    public static final ResourceOption<ResourceAddress> TRANSPORT = new TransportOption();
    public static final ResourceOption<ResourceAddress> ALTERNATE = new AlternateOption();
    public static final ResourceOption<NameResolver> RESOLVER = new ResolverOption(); // consider moving to TcpResourceAddress, ...
    public static final ResourceOption<Boolean> BIND_ALTERNATE = new BindAlternateOption();
    public static final ResourceOption<Boolean> CONNECT_REQUIRES_INIT = new ConnectRequiresInitOption();

    @Deprecated // Move separately to WSEB, PROXY, etc (different types)
    public static final ResourceOption<Object> QUALIFIER = new QualifierOption();
    public static final ResourceOption<URI> TRANSPORTED_URI = new TransportedURIOption();

    private final ResourceAddressFactorySpi factory;
    public static final DefaultResourceOption<IdentityResolver> IDENTITY_RESOLVER = new IdentityResolverOption();
    private final String externalURI;
    private final URI resourceURI;

    private String nextProtocol;
    private ResourceAddress transport;
    private String transportURI;
    private ResourceAddress alternate;
    private NameResolver resolver;
    private Object qualifier;
    private Boolean bindAlternate;
    private Boolean connectRequiresInit;
    private IdentityResolver identityResolver;

    public ResourceAddress(ResourceAddressFactorySpi factory, String original, URI resourceURI) {
        this.factory = Objects.requireNonNull(factory, "factory");
        this.externalURI = Objects.requireNonNull(original, "externalURI");
        this.resourceURI = Objects.requireNonNull(resourceURI, "resourceURI");
    }

    // note: used by pipe://
    protected ResourceAddress(ResourceAddressFactorySpi factory, URI resourceURI) {
        this(factory, URIUtils.uriToString(resourceURI), resourceURI);
    }
    
    public URI getResource() {
        return resourceURI;
    }
    
    public String getExternalURI() {
        return externalURI;
    }
    
    public ResourceAddress getTransport() {
        return transport;
    }

    // Used to allow us to be agnostic when we write code that binds over potentially different stacks,
    // by allowing us to specify exactly which resource address layer we are after.
    public ResourceAddress findTransport(String query) {
        ResourceAddress result = null;

        if (query == null || query.length()==0) {
            return result;
        }

        String myTransportName = format("%s.", getResource().getScheme());
        String transportProtocolName = getTransport() != null ? getTransport().getOption(NEXT_PROTOCOL) : null;
        List<String> prefixes;
        if (transportProtocolName != null) {
            String transportAsProtocolFormat = format("%s[%s].", getResource().getScheme(), transportProtocolName);
            prefixes = asList(myTransportName, transportAsProtocolFormat);
        } else {
            prefixes = Collections.singletonList(myTransportName);
        }

        // strip off current transport prefix for option names
        for (String prefix : prefixes) {
            if (result == null) {

                // do we exact match except for the trailing '.' on the prefix?
                if (query.equals(prefix.substring(0, prefix.length()-1))) {
                    return this;
                }

                // if query starts with prefix but is longer
                if (query.startsWith(prefix)) {
                    query = query.substring(prefix.length());
                    result = getTransport().findTransport(query);
                }
            }
        }
        if (getTransport() == null) {
            return null;
        } else {
            return getTransport().findTransport(query);
        }
    }

    @Override
    public <T> T setOption(ResourceOption<T> key, T value) {
        throw new IllegalStateException(format("%s options are read-only", ResourceAddress.class.getSimpleName()));
    }

    @Override
    public final <T> boolean hasOption(ResourceOption<T> option) {
        if (option instanceof DefaultResourceOption) {
            DefaultResourceOption resourceOption = (DefaultResourceOption)option;
            switch(resourceOption.kind) {
                case NEXT_PROTOCOL:
                    return nextProtocol != null;
                case TRANSPORT:
                    return transport != null;
                case TRANSPORT_URI:
                    return transportURI != null;
                case ALTERNATE:
                    return alternate != null;
                case RESOLVER:
                    return resolver != null;
                case BIND_ALTERNATE:
                    return bindAlternate != null;
                case QUALIFIER:
                    return qualifier != null;
                case TRANSPORTED_URI:
                    return getTransportedURI() != null;
                case CONNECT_REQUIRES_INIT:
                    return connectRequiresInit != null;
                case IDENTITY_RESOLVER:
                    return (identityResolver != null);
            }
        }

        return hasOption0(option);
    }


    
    protected <V> boolean hasOption0(ResourceOption<V> option) {
        throw new IllegalArgumentException(format("Unrecognized option: %s", option));
    }

    @Override
    public final <V> V getOption(ResourceOption<V> option) {
        V value = getOption0(option);
        return (value != null) ? value : option.defaultValue();
    }

    protected URI getTransportedURI() {
        return null;
    }

    protected void setTransportedURI(URI transportedURI) {
    }


    public final ResourceAddress resolve(String newPath) {
        if ( newPath == null ) {
            throw new NullPointerException(newPath);
        }
        URI addressURI = getResource();
        return resolve(addressURI.getPath(), newPath);
    }

    protected final ResourceAddress resolve(String oldPath, String newPath) {
        URI addressURI = getResource();
        String externalURI = getExternalURI();

        boolean newPathDiffersFromOld = !oldPath.equals(newPath);
        if ( !newPathDiffersFromOld ) {
            return this;
        }

        boolean shouldResolveNewPath = oldPath.equals(addressURI.getPath());
        if (!shouldResolveNewPath) {
            return this;
        }

        URI newResourceURI = addressURI.resolve(newPath);
        String newExternalURI = URIUtils.resolve(externalURI, newPath);
        ResourceOptions newOptions = FACTORY.newResourceOptions(this);
        resolve(oldPath, newPath, newOptions);
        String externalUriToString = newExternalURI;
        String newResourceUriToString = URIUtils.uriToString(newResourceURI);
        return factory.newResourceAddress0(externalUriToString, newResourceUriToString, newOptions);
    }

    protected void resolve(String oldPath, String newPath, ResourceOptions newOptions) {
        ResourceAddress transport = getOption(TRANSPORT);
        if (transport != null) {
            newOptions.setOption(TRANSPORT, transport.resolve(oldPath, newPath));
        }

        ResourceAddress alternate = getOption(ALTERNATE);
        if (alternate != null) {
            newOptions.setOption(ALTERNATE, alternate.resolve(oldPath, newPath));
        }
    }

    @SuppressWarnings("unchecked")
    protected <V> V getOption0(ResourceOption<V> option) {
        if (option instanceof DefaultResourceOption) {
            DefaultResourceOption resourceOption = (DefaultResourceOption)option;
            switch(resourceOption.kind) {
                case NEXT_PROTOCOL:
                    return (V) nextProtocol;
                case TRANSPORT:
                    return (V) transport;
                case TRANSPORT_URI:
                    return (V) transportURI;
                case ALTERNATE:
                    return (V) alternate;
                case RESOLVER:
                    return (V) resolver;
                case BIND_ALTERNATE:
                    return (V) bindAlternate;
                case QUALIFIER:
                    return (V) qualifier;
                case TRANSPORTED_URI:
                    return (V) getTransportedURI();
                case CONNECT_REQUIRES_INIT:
                    return (V) connectRequiresInit;
                case IDENTITY_RESOLVER:
                    return (V) identityResolver;
            }
        }

        throw new IllegalArgumentException(format("Unrecognized option: %s", option));
    }

    protected <V> void setOption0(ResourceOption<V> option, V value) {
        if (option instanceof DefaultResourceOption) {
            DefaultResourceOption defaultOption = (DefaultResourceOption) option;
            switch (defaultOption.kind) {
                case NEXT_PROTOCOL:
                    nextProtocol = (String) value;
                    return;
                case TRANSPORT_URI:
                    transportURI = (String) value;
                    return;
                case TRANSPORT:
                    transport = (ResourceAddress) value;
                    return;
                case ALTERNATE:
                    alternate = (ResourceAddress) value;
                    return;
                case RESOLVER:
                    resolver = (NameResolver) value;
                    return;
                case BIND_ALTERNATE:
                    bindAlternate = (Boolean) value;
                    return;
                case QUALIFIER:
                    qualifier = value;
                    return;
                case TRANSPORTED_URI:
                    setTransportedURI((URI) value);
                    return;
                case CONNECT_REQUIRES_INIT:
                    connectRequiresInit = (Boolean) value;
                    return;
                case IDENTITY_RESOLVER:
                    identityResolver = (IdentityResolver) value;
                    return;
            }
        }

        throw new IllegalArgumentException(format("Unrecognized option: %s", option));
    }
    
    @Override
    public int hashCode() {
        int result = resourceURI.hashCode();
        result = 31 * result + (nextProtocol != null ? nextProtocol.hashCode() : 0);
        result = 31 * result + (transport != null ? transport.hashCode() : 0);
        result = 31 * result + (transportURI != null ? transportURI.hashCode() : 0);
        result = 31 * result + (alternate != null ? alternate.hashCode() : 0);
        result = 31 * result + (resolver != null ? resolver.hashCode() : 0);
        result = 31 * result + (qualifier != null ? qualifier.hashCode() : 0);
        result = 31 * result + (getTransportedURI() != null ? getTransportedURI().hashCode() : 0);

        return result;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ResourceAddress address = (ResourceAddress) o;

        if (!resourceURI.equals(address.resourceURI))
            return false;
        if (nextProtocol != null ? !nextProtocol.equals(address.nextProtocol) : address.nextProtocol != null)
            return false;
        if (transport != null ? !transport.equals(address.transport) : address.transport != null)
            return false;
        if (transport == null && (transportURI != null ? !transportURI.equals(address.transportURI) : address.transportURI != null))
            return false;
        if (alternate != null ? !alternate.equals(address.alternate) : address.alternate != null)
            return false;
        if (resolver != null ? !resolver.equals(address.resolver) : address.resolver != null)
            return false;
        if (qualifier != null ? !qualifier.equals(address.qualifier) : address.qualifier != null)
            return false;
        return !(getTransportedURI() != null ? !getTransportedURI().equals(address.getTransportedURI()) : address.getTransportedURI() != null);

    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        toString(builder, 0);
        return builder.toString();
    }

    protected void toString(StringBuilder builder, int level) {
            for (int i=0; i < level; i++) {
            builder.append("  ");
            }
        builder.append('[');
        builder.append(resourceURI);
        builder.append(' ');
        if (!externalURI.equals(resourceURI)) {
            builder.append('(');
            builder.append(externalURI);
            builder.append(')');
            builder.append(' ');
        }
        if (nextProtocol != null) {
            builder.append(nextProtocol);
            builder.append(' ');
        }
        builder.setCharAt(builder.length() - 1, ']');
        if (qualifier != null) {
            builder.append(' ');
            builder.append(qualifier);
        }
        if (transport != null) {
            builder.append('\n');
            transport.toString(builder, level + 1);
        }
        if (alternate != null && !Boolean.FALSE.equals(bindAlternate)) {
            builder.append('\n');
            alternate.toString(builder, level);
        }
    }

    public static class DefaultResourceOption<T> extends ResourceOption<T> {

        enum Kind { NEXT_PROTOCOL,
                           TRANSPORT,
                           TRANSPORT_URI,
                           ALTERNATE,
                           RESOLVER,
                           BIND_ALTERNATE,
                           QUALIFIER,
                           TRANSPORTED_URI,
                           CONNECT_REQUIRES_INIT,
                           IDENTITY_RESOLVER}
        
        static final Map<String, ResourceOption<?>> OPTIONS = new HashMap<>();

        private final Kind kind;
        
        private DefaultResourceOption(Kind kind, String name) {
            this(kind, name, null);
        }
        
        private DefaultResourceOption(Kind kind, String name, T defaultValue) {
            super(OPTIONS, name, defaultValue);
            this.kind = kind;
        }
    }
    
    private static class NextProtocolOption extends DefaultResourceOption<String> {
        private NextProtocolOption() {
            super(Kind.NEXT_PROTOCOL, "nextProtocol");
        }
    }
    
    private static class TransportURIStringOption extends DefaultResourceOption<String> {
        private TransportURIStringOption() {
            super(Kind.TRANSPORT_URI, "transportURI");
        }
    }
    
    private static class TransportOption extends DefaultResourceOption<ResourceAddress> {
        private TransportOption() {
            super(Kind.TRANSPORT, "transport");
        }
    }
    
    private static class AlternateOption extends DefaultResourceOption<ResourceAddress> {
        private AlternateOption() {
            super(Kind.ALTERNATE, "alternate");
        }
    }
    
    private static class ResolverOption extends DefaultResourceOption<NameResolver> {

        private ResolverOption() {
            super(Kind.RESOLVER, "resolver", new NameResolver() {
                
                private final Map<String, NameResolver> wildcards;
                
                abstract class WildcardNameResolver implements NameResolver {
                    
                    @Override
                    public Collection<InetAddress> getAllByName(String host) throws UnknownHostException {
                        try {
                            Set<InetAddress> inetAddresses = new HashSet<>();
                            for (NetworkInterface iface : list(getNetworkInterfaces())) {
                                List<InetAddress> ifaceAddresses = list(iface.getInetAddresses());
                                inetAddresses.addAll(ifaceAddresses);
                                
                                for (NetworkInterface subiface : list(iface.getSubInterfaces())) {
                                    List<InetAddress> subifaceAddresses = list(subiface.getInetAddresses());
                                    inetAddresses.addAll(subifaceAddresses);
                                }
                            }
                            return inetAddresses;
                        }
                        catch (SocketException se) {
                            UnknownHostException e = new UnknownHostException(host);
                            e.initCause(se);
                            throw e;
                        }
                    }

                }
                
                {
                    Map<String, NameResolver> wildcards = new HashMap<>();
                    wildcards.put("0.0.0.0", new WildcardNameResolver() {

                        @Override
                        public Collection<InetAddress> getAllByName(String host) throws UnknownHostException {
                            if (!"0.0.0.0".equals(host)) {
                                throw new UnknownHostException(host);
                            }
                            
                            Collection<InetAddress> inetAddresses = super.getAllByName(host);
                            retainInetAddresses(inetAddresses, IpAddressFamily.IPv4);
                            return inetAddresses;
                        }
                    });
                    this.wildcards = wildcards;
                }
                
                @Override
                public Collection<InetAddress> getAllByName(String host) throws UnknownHostException {
                    NameResolver wildcard = wildcards.get(host);
                    if (wildcard != null) {
                        return wildcard.getAllByName(host);
                    }
                    return asList(InetAddress.getAllByName(host));
                }
            });
        }
        
        private enum IpAddressFamily { IPv4, IPv6 }
        
        private static void retainInetAddresses(Iterable<InetAddress> ifaceAddresses, IpAddressFamily family) {
            for (Iterator<InetAddress> $i = ifaceAddresses.iterator(); $i.hasNext(); ) {
                InetAddress ifaceAddress = $i.next();
                switch (family) {
                case IPv4:
                    if (!(ifaceAddress instanceof Inet4Address)) {
                        $i.remove();
                    }
                    break;
                case IPv6:
                    if (!(ifaceAddress instanceof Inet6Address)) {
                        $i.remove();
                    }
                    break;
                default:
                    throw new IllegalArgumentException("Unrecognized IP address family " + family);
                }
            }
        }
    }
    
    private static class BindAlternateOption extends DefaultResourceOption<Boolean> {
        private BindAlternateOption() {
            super(Kind.BIND_ALTERNATE, "bindAlternate", Boolean.TRUE);
        }
    }
    
    private static class QualifierOption extends DefaultResourceOption<Object> {
        private QualifierOption() {
            super(Kind.QUALIFIER, "qualifier");
        }
    }

    private static class TransportedURIOption extends DefaultResourceOption<URI> {
        private TransportedURIOption() {
            super(Kind.TRANSPORTED_URI, "transportedURI");
        }
    }
    
    private static class ConnectRequiresInitOption extends DefaultResourceOption<Boolean> {
        private ConnectRequiresInitOption() {
            super(Kind.CONNECT_REQUIRES_INIT, "connectRequiresInit", Boolean.FALSE);
        }
    }

    private static final class IdentityResolverOption extends DefaultResourceOption<IdentityResolver> {
        private IdentityResolverOption() {
            super(Kind.IDENTITY_RESOLVER, "identityResolver");
        }
    }
}

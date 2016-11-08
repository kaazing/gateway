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
package org.kaazing.gateway.resource.address.udp;

import static java.lang.String.format;
import static org.kaazing.gateway.resource.address.ResourceAddress.RESOLVER;
import static org.kaazing.gateway.resource.address.ResourceAddress.TRANSPORT;
import static org.kaazing.gateway.resource.address.udp.UdpResourceAddress.BIND_ADDRESS;
import static org.kaazing.gateway.resource.address.udp.UdpResourceAddress.INTERFACE;
import static org.kaazing.gateway.resource.address.udp.UdpResourceAddress.MAXIMUM_OUTBOUND_RATE;
import static org.kaazing.gateway.resource.address.udp.UdpResourceAddress.TRANSPORT_NAME;
import static org.kaazing.gateway.resource.address.uri.URIUtils.getHost;
import static org.kaazing.gateway.resource.address.uri.URIUtils.getPort;
import static org.kaazing.gateway.resource.address.uri.URIUtils.modifyURIAuthority;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.kaazing.gateway.resource.address.NameResolver;
import org.kaazing.gateway.resource.address.ResolutionUtils;
import org.kaazing.gateway.resource.address.ResourceAddressFactorySpi;
import org.kaazing.gateway.resource.address.ResourceFactory;
import org.kaazing.gateway.resource.address.ResourceOptions;
import org.kaazing.gateway.resource.address.uri.URIUtils;

public class UdpResourceAddressFactorySpi extends ResourceAddressFactorySpi<UdpResourceAddress> {

    private static final String JAVA_NET_PREFER_IPV4_STACK = "java.net.preferIPv4Stack";
    private static final String SCHEME_NAME = "udp";
    private static final String PROTOCOL_NAME = "udp";

    private static final String FORMAT_IPV4_AUTHORITY = "%s:%d";
    private static final String FORMAT_IPV6_AUTHORITY = "[%s]:%d";
    // "@" added in the pattern below in order not to match network interface syntax
    private static final Pattern PATTERN_IPV6_HOST = Pattern.compile("\\[([^@\\]]+)\\]");
    private static final String PREFER_IPV4_STACK_IPV6_ADDRESS_EXCEPTION_FORMATTER =
            "Option java.net.preferIPv4Stack is set to true and an IPv6 address was provided in the config. No addresses"
            + " available for binding for URI: %s.";

    @Override
    public String getSchemeName() {
        return SCHEME_NAME;
    }

    @Override
    protected String getTransportName() {
        return TRANSPORT_NAME;
    }
    
    @Override
    protected String getProtocolName() {
        return PROTOCOL_NAME;
    }
    
    @Override
    protected ResourceFactory getTransportFactory() {
        return null;
    }

    @Override
    protected void parseNamedOptions0(String location, ResourceOptions options,
                                      Map<String, Object> optionsByName) {
        Object bindAddress = optionsByName.remove(BIND_ADDRESS.name());
        if (bindAddress != null) {
            InetSocketAddress bindAddress0 = parseBindAddress(bindAddress);
            options.setOption(BIND_ADDRESS, bindAddress0);
        }

        Long maximumOutboundRate = (Long) optionsByName.remove(MAXIMUM_OUTBOUND_RATE.name());
        if (maximumOutboundRate != null) {
            options.setOption(MAXIMUM_OUTBOUND_RATE, maximumOutboundRate);
        }

        String udpInterface = (String) optionsByName.remove(INTERFACE.name());
        if (udpInterface != null) {
            options.setOption(INTERFACE, udpInterface);
        }

    }

    private InetSocketAddress parseBindAddress(Object bindAddress) {
        if (bindAddress instanceof InetSocketAddress) {
            return (InetSocketAddress) bindAddress;
        }
        else if (bindAddress instanceof String) {
            return ResolutionUtils.parseBindAddress((String) bindAddress);
        }

        throw new IllegalArgumentException(BIND_ADDRESS.name());
    }

    @Override
    protected List<UdpResourceAddress> newResourceAddresses0(String original,
                                                             String location,
                                                             ResourceOptions options) {

        InetSocketAddress bindSocketAddress = options.getOption(BIND_ADDRESS);
        if (bindSocketAddress != null) {
            // apply bind option in preference to network context
            String newHost = getHostStringWithoutNameLookup(bindSocketAddress);
            int newPort = bindSocketAddress.getPort();
            InetAddress bindAddress = bindSocketAddress.getAddress();
            String authorityFormat = (bindAddress instanceof Inet6Address) ? FORMAT_IPV6_AUTHORITY : FORMAT_IPV4_AUTHORITY;
            String newAuthority = format(authorityFormat, newHost, newPort);
            location = modifyURIAuthority(location, newAuthority);
        }

        // if udp has a transport, do not resolve the authority.
        if (options.getOption(TRANSPORT) != null) {
            return Collections.singletonList(super.newResourceAddress0(original, location, options));
        }
        
        // ensure that DNS name is resolved in transport address
        NameResolver resolver = options.getOption(RESOLVER);
        assert (resolver != null);
        List<UdpResourceAddress> udpAddresses = new LinkedList<>();
        try {
            String host = getHost(location);
            Matcher matcher = PATTERN_IPV6_HOST.matcher(host);
            if (matcher.matches()) {
                host = matcher.group(1);
            }

            Collection<InetAddress> inetAddresses = new ArrayList<>();
            Collection<InetAddress> addresses = ResolutionUtils.getAllByName(host, true);
            // network interface resolution performed
            if (!addresses.isEmpty()) {
                for (InetAddress address : addresses) {
                    inetAddresses.addAll(resolver.getAllByName(address.getHostAddress()));
                }
            }
            else {
                inetAddresses = resolver.getAllByName(host);
            }
            assert (!inetAddresses.isEmpty());

            // The returned collection appears to be unmodifiable, so first clone the list (ugh!)
            Collection<InetAddress> unsortedInetAddresses = new LinkedList<>(inetAddresses);
            Collection<InetAddress> sortedInetAddresses = new LinkedList<>();
            // Make a pass through the collection and pick the IPv4 addresses first, then add the rest of the addresses
            Iterator<InetAddress> inetAddressIter = unsortedInetAddresses.iterator();
            while (inetAddressIter.hasNext()) {
                InetAddress addr = inetAddressIter.next();
                if (addr instanceof Inet4Address) {
                    if (!sortedInetAddresses.contains(addr)) {
                        sortedInetAddresses.add(addr);
                    }
                    inetAddressIter.remove();
                }
            }

            boolean preferIPv4 = "true".equalsIgnoreCase(System.getProperty(JAVA_NET_PREFER_IPV4_STACK));
            if (!preferIPv4) {
                // Add all the remaning (IPv6) addresses.  Because InetAddress.getAllByName() is lame
                // and returns duplicates when java.net.preferIPv4Stack is true, I have to add them
                // one at a time iff not already in the list.
                if (!unsortedInetAddresses.isEmpty()) {
                    for (InetAddress addr : unsortedInetAddresses) {
                        if (!sortedInetAddresses.contains(addr)) {
                            sortedInetAddresses.add(addr);
                        }
                    }
                }
            }

            // convert each alternate InetAddress to a UdpResourceAddress
            for (InetAddress inetAddress : sortedInetAddresses) {
                String ipAddress = inetAddress.getHostAddress();
                String addressFormat = (inetAddress instanceof Inet6Address) ? FORMAT_IPV6_AUTHORITY : FORMAT_IPV4_AUTHORITY;
                String newAuthority = format(addressFormat, ipAddress, getPort(location));
                location = modifyURIAuthority(location, newAuthority);
                UdpResourceAddress udpAddress = super.newResourceAddress0(original, location, options);
                udpAddresses.add(udpAddress);
            }
        }
        catch (UnknownHostException e) {
            String host = getHost(location);
            Pattern pattern = Pattern.compile(URIUtils.NETWORK_INTERFACE_AUTHORITY);
            Matcher matcher = pattern.matcher(host);
            // Test for network interface syntax and throw specific error message if found
            if (matcher.find()) {
                throw new IllegalArgumentException(format("The interface name %s is not recognized", host), e);
            }
            // Network interface syntax format not detected so generic error message can be thrown
            else {
                throw new IllegalArgumentException(format("Unable to resolve DNS name: %s", host), e);
            }
        }

        if (udpAddresses.isEmpty()) {
            throwPreferedIPv4StackIPv6AddressError(location, udpAddresses);
        }

        return udpAddresses;
    }

    @Override
    protected UdpResourceAddress newResourceAddress0(String original, String location) {

        URI uriLocation = URI.create(location);
        String path = uriLocation.getPath();

        if (uriLocation.getHost() == null) {
            throw new IllegalArgumentException(format("Missing host in URI: %s", location));
        }

        if (uriLocation.getPort() == -1) {
            throw new IllegalArgumentException(format("Missing port in URI: %s", location));
        }

        if (path != null && !path.isEmpty()) {
            throw new IllegalArgumentException(format("Unexpected path \"%s\" in URI: %s", path, location));
        }

        return new UdpResourceAddress(this, original, uriLocation);
    }
    
    @Override
    protected void setOptions(UdpResourceAddress address, ResourceOptions options, Object qualifier) {

        super.setOptions(address, options, qualifier);

        // note: no need to propagate bind address and network context,
        //       already handled during address creation
        address.setOption0(MAXIMUM_OUTBOUND_RATE, options.getOption(MAXIMUM_OUTBOUND_RATE));
        address.setOption0(INTERFACE, options.getOption(INTERFACE));
    }

    /**
     * A Java 6/7 safe way of looking up a host name from an InetSocketAddress
     * without tickling a name lookup.
     *
     * @param inetSocketAddress the address for which you want a host string
     * @return a hostname for the given address, having not triggered a name service lookup
     */
    static String getHostStringWithoutNameLookup(InetSocketAddress inetSocketAddress) {
        String newHost;
        if ( inetSocketAddress.isUnresolved() ) {
            newHost = inetSocketAddress.getHostName();
        } else {
            newHost = inetSocketAddress.getAddress().getHostAddress();
        }
        return newHost;
    }

    /**
     * Throw error on specific circumstances:
     *   - no addresses available for binding
     *   - when PreferedIPv4 flag is true and the host IP is IPV6
     * @param location
     * @param tcpAddresses
     */
    private void throwPreferedIPv4StackIPv6AddressError(String location, List<UdpResourceAddress> tcpAddresses) {
        try {
            InetAddress address = InetAddress.getByName(URIUtils.getHost(location));
            boolean preferIPv4Stack = Boolean.parseBoolean(System.getProperty(JAVA_NET_PREFER_IPV4_STACK));
            if (preferIPv4Stack && (address instanceof Inet6Address)) {
                throw new IllegalArgumentException(format(PREFER_IPV4_STACK_IPV6_ADDRESS_EXCEPTION_FORMATTER, location));
            }
        } catch (UnknownHostException e) {
            // InetAddress.getByName(hostAddress) throws an exception (hostAddress may have an
            // unsupported format, e.g. network interface syntax)
        }
    }

}
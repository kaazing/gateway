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

import static java.lang.String.CASE_INSENSITIVE_ORDER;
import static org.kaazing.gateway.resource.address.ResourceAddress.NEXT_PROTOCOL;
import static org.kaazing.gateway.resource.address.ResourceAddress.TRANSPORT;
import static org.kaazing.gateway.resource.address.ResourceAddress.TRANSPORTED_URI;

import java.net.URI;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class Comparators {

    private static final Comparator<String> STRING_COMPARATOR = compareComparable(String.class);
    private static final Comparator<String> STRING_INSENSITIVE_COMPARATOR = compareNonNull(CASE_INSENSITIVE_ORDER);
    private static final Comparator<ResourceAddress> ORIGIN_COMPARATOR = compareNonNull(new ResourceOriginComparator());
    private static final Comparator<ResourceAddress> LOCATION_COMPARATOR = compareNonNull(new ResourceLocationComparator());
    private static final Comparator<ResourceAddress> PROTOCOL_STACK_COMPARATOR = compareNonNull(new ResourceProtocolStackComparator());
    private static final Comparator<ResourceAddress> ORIGIN_AND_PROTOCOL_STACK_COMPARATOR = new ResourceOriginAndProtocolStackComparator();
    private static final Comparator<ResourceAddress> LOCATION_AND_TRANSPORT_PROTOCOL_STACK_COMPARATOR = compareNonNull(new ResourceLocationAndTransportProtocolStackComparator());
    private static final Comparator<ResourceAddress> ORIGIN_PATH_ALTERNATES_AND_PROTOCOL_STACK_COMPARATOR = compareAlternates(new ResourceOriginPathAndProtocolStackComparator());
    private static final Comparator<ResourceAddress> ORIGIN_PATH_AND_PROTOCOL_STACK_COMPARATOR = compareNonNull(new ResourceOriginPathAndProtocolStackComparator());
    private static final Comparator<ResourceAddress> TRANSPORT_PROTOCOL_STACK_AND_TRANSPORTED_URI_COMPARATOR = compareNonNull(new TransportProtocolStackAndTransportedURIComparator());
    private static final Comparator<URI> TRANSPORTED_URI_COMPARATOR = compareNonNull(new TransportedURIComparator());


    public static Comparator<String> stringComparator() {
        return STRING_COMPARATOR;
    }

    public static Comparator<ResourceAddress> compareResourceOrigin() {
        return ORIGIN_COMPARATOR;
    }

    public static Comparator<ResourceAddress> compareResourceLocation() {
        return LOCATION_COMPARATOR;
    }

    public static Comparator<ResourceAddress> compareResourceProtocolStack() {
        return PROTOCOL_STACK_COMPARATOR;
    }

    public static Comparator<ResourceAddress> compareResourceOriginAndProtocolStack() {
        return ORIGIN_AND_PROTOCOL_STACK_COMPARATOR;
    }

    public static Comparator<ResourceAddress> compareResourceOriginPathAndProtocolStack() {
        return ORIGIN_PATH_AND_PROTOCOL_STACK_COMPARATOR;
    }

    public static Comparator<ResourceAddress> compareResourceOriginPathAlternatesAndProtocolStack() {
        return ORIGIN_PATH_ALTERNATES_AND_PROTOCOL_STACK_COMPARATOR;
    }

    public static Comparator<ResourceAddress> compareResourceLocationAndTransportProtocolStack() {
        return LOCATION_AND_TRANSPORT_PROTOCOL_STACK_COMPARATOR;
    }

    public static Comparator<ResourceAddress> compareTransportProtocolStackAndTransportedURI() {
        return TRANSPORT_PROTOCOL_STACK_AND_TRANSPORTED_URI_COMPARATOR;
    }

    public static <T> Comparator<T> compareNonNull(Comparator<T> comparator) {
        return new NonNullComparator<>(comparator);
    }

    public static <T extends Comparable<T>> Comparator<T> compareComparable(Class<T> clazz) {
        return compareNonNull(new ComparableComparator<T>());
    }

    private static final class ComparableComparator<T extends Comparable<T>> implements Comparator<T> {

        @Override
        public int compare(T o1, T o2) {
            return o1.compareTo(o2);
        }
    }
    
    private static final class NonNullComparator<T> implements Comparator<T> {

        private final Comparator<T> delegate;
        
        public NonNullComparator(Comparator<T> delegate) {
            this.delegate = delegate;
        }

        @Override
        public final int compare(T o1, T o2) {

            if (o1 == null) {
                return (o2 == null) ? 0 : -1;
            }
            else if (o2 == null) {
                return 1;
            }
            
            return delegate.compare(o1, o2);
        }
    }

    private static final class ResourceOriginComparator implements Comparator<ResourceAddress> {

        @Override
        public int compare(ResourceAddress addr1, ResourceAddress addr2) {

            URI location1 = addr1.getResource();
            URI location2 = addr2.getResource();
            
            assert (location1 != null);
            assert (location2 != null);

            String scheme1 = location1.getScheme();
            String scheme2 = location2.getScheme();
            
            int compareScheme = STRING_COMPARATOR.compare(scheme1, scheme2);
            if (compareScheme != 0) {
                return compareScheme;
            }
            
            String authority1 = location1.getAuthority();
            String authority2 = location2.getAuthority();
            
            int compareAuthority = STRING_INSENSITIVE_COMPARATOR.compare(authority1, authority2);
            if (compareAuthority != 0) {
                return compareAuthority;
            }
            
            return 0;
        }
        
    }

    private static final class ResourceLocationComparator implements Comparator<ResourceAddress> {

        @Override
        public int compare(ResourceAddress addr1, ResourceAddress addr2) {

            URI location1 = addr1.getResource();
            URI location2 = addr2.getResource();
            
            assert (location1 != null);
            assert (location2 != null);

            return (location1.compareTo(location2));
        }
        
    }

    private static final class ResourceProtocolStackComparator implements Comparator<ResourceAddress> {

        @Override
        public int compare(ResourceAddress addr1, ResourceAddress addr2) {

            String protocol1 = addr1.getOption(NEXT_PROTOCOL);
            String protocol2 = addr2.getOption(NEXT_PROTOCOL);
            
            int compareProtocol = STRING_COMPARATOR.compare(protocol1, protocol2);
            if (compareProtocol != 0) {
                return compareProtocol;
            }

            ResourceAddress transport1 = addr1.getOption(TRANSPORT);
            ResourceAddress transport2 = addr2.getOption(TRANSPORT);
            
            int compareTransport = PROTOCOL_STACK_COMPARATOR.compare(transport1, transport2);
            if (compareTransport != 0) {
                return compareTransport;
            }
            
            return 0;
        }
        
    }

    private static ResourceAddress getFloorTransport(ResourceAddress address) {
        assert address != null;

        ResourceAddress transport;
        while((transport = address.getTransport()) != null) {
            address = transport;
        }
        return address;
    }

    private static final class ResourceOriginAndProtocolStackComparator implements Comparator<ResourceAddress> {

        @Override
        public int compare(ResourceAddress addr1, ResourceAddress addr2) {

            int compareOrigin = ORIGIN_COMPARATOR.compare(addr1, addr2);
            if (compareOrigin != 0) {
                return compareOrigin;
            }

            int compareNextProtocol = PROTOCOL_STACK_COMPARATOR.compare(addr1, addr2);
            if (compareNextProtocol != 0) {
                return compareNextProtocol;
            }

            ResourceAddress floor1 = getFloorTransport(addr1);
            ResourceAddress floor2 = getFloorTransport(addr2);
            return ORIGIN_PATH_ALTERNATES_AND_PROTOCOL_STACK_COMPARATOR.compare(floor1, floor2);
        }
    }

    public static Comparator<ResourceAddress> compareAlternates(Comparator<ResourceAddress> comparator) {
        return compareNonNull(new AlternateResourceAddressComparator(comparator));
    }

    private static final class AlternateResourceAddressComparator implements Comparator<ResourceAddress> {

        private final Comparator<ResourceAddress> delegate;

        private AlternateResourceAddressComparator(Comparator<ResourceAddress> delegate) {
            this.delegate = delegate;
        }

        @Override
        public int compare(ResourceAddress addr1, ResourceAddress addr2) {
            List<ResourceAddress> addresses1 = asResourceAddressList(addr1);
            List<ResourceAddress> addresses2 = asResourceAddressList(addr2);

            for (ResourceAddress anAddress1 : addresses1) {
                for (ResourceAddress anAddress2 : addresses2) {
                    int result = delegate.compare(anAddress1, anAddress2);
                    if (result == 0) {
                        return result;
                    }
                }
            }

            return delegate.compare(addr1, addr2);
        }

        static ResourceAddressFactory addressFactory = ResourceAddressFactory.newResourceAddressFactory();

        private List<ResourceAddress> asResourceAddressList(ResourceAddress addr) {
            List<ResourceAddress> result = new ArrayList<>();

            List<ResourceAddress> topLevelAddresses = new ArrayList<>();

            ResourceAddress cursor = addr;
            do {
                topLevelAddresses.add(cursor);
                cursor = cursor.getOption(ResourceAddress.ALTERNATE);
            } while (cursor != null);

            //recurse
            for ( ResourceAddress resourceAddress: topLevelAddresses ) {
                ResourceAddress transport = resourceAddress.getTransport();
                if ( transport != null ) {
                    for (ResourceAddress transportAddress: asResourceAddressList(transport)) {
                        ResourceOptions newResultOptions = ResourceOptions.FACTORY.newResourceOptions(resourceAddress);
                        newResultOptions.setOption(TRANSPORT, transportAddress);
                        ResourceAddress newResult = addressFactory.newResourceAddress(resourceAddress.getExternalURI(), newResultOptions);
                        result.add(newResult);
                    }
                } else {
                    result.add(resourceAddress);
                }
            }
            return result;
        }
    }

    private static final class ResourceOriginPathAndProtocolStackComparator implements Comparator<ResourceAddress> {

        @Override
        public int compare(ResourceAddress addr1, ResourceAddress addr2) {

            int compareOrigin = ORIGIN_COMPARATOR.compare(addr1, addr2);
            if (compareOrigin != 0) {
                return compareOrigin;
            }

            int compareURI = STRING_COMPARATOR.compare(addr1.getResource().getPath(),
                                                       addr2.getResource().getPath());
            if (compareURI != 0) {
                return compareURI;
            }

            int compareProtocol = STRING_COMPARATOR.compare(addr1.getOption(NEXT_PROTOCOL),
                                                            addr2.getOption(NEXT_PROTOCOL));
            if (compareProtocol != 0) {
                return compareProtocol;
            }

            ResourceAddress transport1 = addr1.getOption(TRANSPORT);
            ResourceAddress transport2 = addr2.getOption(TRANSPORT);

            int compareTransport = ORIGIN_PATH_AND_PROTOCOL_STACK_COMPARATOR.compare(transport1, transport2);
            if (compareTransport != 0) {
                return compareTransport;
            }

            return 0;
        }

    }

    /** Used for NextProtocolBindings and dispatchers - explicitly we don't care about next protocols at the current level
     *  because the bindings group all those possibilities together.
     */
    private static final class ResourceLocationAndTransportProtocolStackComparator implements Comparator<ResourceAddress> {

        @Override
        public int compare(ResourceAddress addr1, ResourceAddress addr2) {

            int compareLocation = LOCATION_COMPARATOR.compare(addr1, addr2);
            if (compareLocation != 0) {
                return compareLocation;
            }

            ResourceAddress transport1 = addr1.getOption(TRANSPORT);
            ResourceAddress transport2 = addr2.getOption(TRANSPORT);
            
            int compareTransport = PROTOCOL_STACK_COMPARATOR.compare(transport1, transport2);
            if (compareTransport != 0) {
                return compareTransport;
            }
            
            return 0;
        }
        
    }

    // A class that is wrapped by a NonNullComparator to handle URI comparisons
    private static final class TransportedURIComparator implements Comparator<URI> {
        @Override
        public int compare(URI uri1, URI uri2) {
            return uri1.compareTo(uri2);
        }
    }

    private static final class TransportProtocolStackAndTransportedURIComparator implements Comparator<ResourceAddress> {

        @Override
        public int compare(ResourceAddress addr1, ResourceAddress addr2) {

            URI transportedURI1 = addr1.getOption(TRANSPORTED_URI);
            URI transportedURI2 = addr2.getOption(TRANSPORTED_URI);
            int compareTransportedURI = TRANSPORTED_URI_COMPARATOR.compare(transportedURI1, transportedURI2);
            if (compareTransportedURI != 0) {
                return compareTransportedURI;
            }

            ResourceAddress transport1 = addr1.getOption(TRANSPORT);
            ResourceAddress transport2 = addr2.getOption(TRANSPORT);

            int compareTransport = PROTOCOL_STACK_COMPARATOR.compare(transport1, transport2);
            if (compareTransport != 0) {
                return compareTransport;
            }

            return 0;
        }
    }
}

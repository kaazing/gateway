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

import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.resource.address.ResourceAddressFactorySpi;
import org.kaazing.gateway.resource.address.ResourceOption;

public final class UdpResourceAddress extends ResourceAddress {
	
	private static final long serialVersionUID = 1L;

	static final String TRANSPORT_NAME = "udp";
	
	public static final ResourceOption<InetSocketAddress> BIND_ADDRESS = new UdpBindAddressOption();
	public static final ResourceOption<Long> MAXIMUM_OUTBOUND_RATE = new UdpMaximumOutboundRateOption();
	public static final ResourceOption<String> INTERFACE = new UdpInterfaceOption();

    private static final long MAXIMUM_OUTBOUND_RATE_DEFAULT = 0xFFFFFFFFL;
	
	private InetSocketAddress bindAddress;
	private long maximumOutboundRate = MAXIMUM_OUTBOUND_RATE.defaultValue();
	private NetworkInterface updInterface;

	UdpResourceAddress(ResourceAddressFactorySpi factory, String original, URI resource) {
		super(factory, original, resource);
	}

	@Override
	@SuppressWarnings("unchecked")
    protected <V> V getOption0(ResourceOption<V> option) {
        if (option instanceof UdpResourceOption) {
            UdpResourceOption udpOption = (UdpResourceOption)option;
            switch (udpOption.kind) {
                case BIND_ADDRESS:
                    return (V) bindAddress;
                case MAXIMUM_OUTBOUND_RATE:
                    return (V) Long.valueOf(maximumOutboundRate);
                case INTERFACE:
                NetworkInterface udpInterface2 = getUpdInterface();
                if(udpInterface2 == null){
                    return null;
                } else {
                    return (V) udpInterface2.getDisplayName();
                }
            }
        }
		
		return super.getOption0(option);
	}

    @Override
	protected <V> void setOption0(ResourceOption<V> option, V value) {

        if (option instanceof UdpResourceOption) {
            UdpResourceOption udpOption = (UdpResourceOption) option;
            switch (udpOption.kind) {
                case BIND_ADDRESS:
                    bindAddress = (InetSocketAddress) value;
                    return;
                case MAXIMUM_OUTBOUND_RATE:
                    maximumOutboundRate = (Long) value;
                    return;
                case INTERFACE:
                    String udpInterfaceName = (String) value;
                    if (value != null) {
                        try {
                            updInterface = NetworkInterface.getByName(udpInterfaceName);
                        } catch (SocketException e) {
                            throw new RuntimeException(String.format("Network interface %s on udp interface does not exist",
                                    udpInterfaceName));
                        }
                    }
                    return;
            }
        }

        super.setOption0(option, value);
	}
	
	public NetworkInterface getUpdInterface() {
        return updInterface;
    }

    static class UdpResourceOption<T> extends ResourceOption<T> {

		enum Kind { BIND_ADDRESS, MAXIMUM_OUTBOUND_RATE, INTERFACE }
		
		static final Map<String, ResourceOption<?>> OPTION_NAMES = new HashMap<>();

		private final Kind kind;
		
        private UdpResourceOption(Kind kind, String name) {
            this(kind, name, null);
        }
        
		private UdpResourceOption(Kind kind, String name, T defaultValue) {
			super(OPTION_NAMES, name, defaultValue);
			this.kind = kind;
		}
	}
	
	private static final class UdpBindAddressOption extends UdpResourceOption<InetSocketAddress> {
		private UdpBindAddressOption() {
			super(Kind.BIND_ADDRESS, "bind");
		}
	}
	
	private static final class UdpMaximumOutboundRateOption extends UdpResourceOption<Long> {
		private UdpMaximumOutboundRateOption() {
			super(Kind.MAXIMUM_OUTBOUND_RATE, "maximumOutboundRate", MAXIMUM_OUTBOUND_RATE_DEFAULT);
		}
	}
	
    private static final class UdpInterfaceOption extends UdpResourceOption<String> {
        private UdpInterfaceOption() {
            super(Kind.INTERFACE, "interface");
        }
    }
	
}

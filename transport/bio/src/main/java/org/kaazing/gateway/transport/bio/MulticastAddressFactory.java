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
package org.kaazing.gateway.transport.bio;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URI;
import java.net.UnknownHostException;

import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.resource.address.udp.UdpResourceAddress;
import org.kaazing.gateway.transport.SocketAddressFactory;

public class MulticastAddressFactory implements SocketAddressFactory<MulticastAddress> {

	@Override
	public MulticastAddress createSocketAddress(ResourceAddress address) {

        URI location = address.getResource();
		String host = location.getHost();
		int port = location.getPort();
		String userInfo = location.getUserInfo();
		
		if (host == null) {
			throw new IllegalArgumentException("Missing host: " + location);
		}
		
		if (port == -1) {
			throw new IllegalArgumentException("Missing port: " + location);
		}
		
		InetAddress groupAddress = (userInfo != null) ? getByName(userInfo) : getByName(host);
        NetworkInterface networkInterface = null;

        if (address instanceof UdpResourceAddress) {
            networkInterface = ((UdpResourceAddress) address).getUpdInterface();
        }

        if (networkInterface == null) {
            throw new IllegalArgumentException("Missing networkInterface: " + networkInterface);
        }

		return new MulticastAddress(groupAddress, networkInterface, port);
	}

	private InetAddress getByName(String hostname) {
		try {
			return InetAddress.getByName(hostname);
		} catch (UnknownHostException e) {
			throw new RuntimeException(e);
		}
	}
}

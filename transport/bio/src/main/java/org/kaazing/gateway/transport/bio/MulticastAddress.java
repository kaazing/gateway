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
import java.net.SocketAddress;

public class MulticastAddress extends SocketAddress {

	private static final long serialVersionUID = 1L;
	
	private final InetAddress groupAddress;
	private final NetworkInterface device;
	private final int uniqueId;
	private final int bindPort;
	
	public MulticastAddress(InetAddress groupAddress, NetworkInterface device, int bindPort) {
		this(groupAddress, device, bindPort, -1);
	}
	
	MulticastAddress(InetAddress groupAddress, NetworkInterface device, int bindPort, int uniqueId) {
	    if (device == null) {
	        throw new NullPointerException("device");
	    }
		if (groupAddress == null) {
			throw new NullPointerException("groupAddress");
		}
		this.bindPort = bindPort;
		this.groupAddress = groupAddress;
		this.device = device;
		this.uniqueId = uniqueId;
	}
	
	public InetAddress getGroupAddress() {
		return groupAddress;
	}

	public NetworkInterface getDevice() {
		return device;
	}

	public int getBindPort() {
	    return bindPort;
	}
	
	@Override
	public int hashCode() {
		return groupAddress.hashCode() << 16 | device.hashCode() | bindPort;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof MulticastAddress) ) {
			return false;
		}
		
		MulticastAddress that = (MulticastAddress)obj;
		return this.uniqueId == that.uniqueId && this.groupAddress.equals(that.groupAddress) && this.device.equals(that.device) && this.bindPort == (that.bindPort);
	}

	@Override
	public String toString() {
		if (uniqueId != -1) {
			return String.format("mcp://%s:%d on %s (%d)", groupAddress.getHostAddress(),  getBindPort(), device.getName(), uniqueId);
		}
		else {
			return String.format("mcp://%s:%d on %s", groupAddress.getHostAddress(), getBindPort(), device.getName());
		}
	}
}

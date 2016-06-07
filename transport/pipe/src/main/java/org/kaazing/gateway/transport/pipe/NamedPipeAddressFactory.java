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
package org.kaazing.gateway.transport.pipe;


import java.net.URI;

import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.transport.NamedPipeAddress;
import org.kaazing.gateway.transport.SocketAddressFactory;

public class NamedPipeAddressFactory implements SocketAddressFactory<NamedPipeAddress> {

	@Override
	public NamedPipeAddress createSocketAddress(ResourceAddress address) {
        URI location = address.getResource();
		assert ("pipe".equals(location.getScheme()));
		assert (location.getAuthority() != null);
		assert (location.getPath() == null || location.getPath().isEmpty());
		assert (location.getQuery() == null);
		assert (location.getFragment() == null);
		return new NamedPipeAddress(location.getAuthority());
	}

}

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
package org.kaazing.gateway.transport;

import org.apache.mina.core.session.IoSession;

import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.transport.BridgeSession;

public class SslUtils {

    @Deprecated // this is only needed to determine external scheme, use ResourceAddress.getExternalURI() instead
	public static boolean isSecure(IoSession session) {
        ResourceAddress address = BridgeSession.LOCAL_ADDRESS.get(session);
        while (address != null && !"ssl".equals(address.getResource().getScheme())) {
            address = address.getTransport();
        }
		return address != null;
	}

}

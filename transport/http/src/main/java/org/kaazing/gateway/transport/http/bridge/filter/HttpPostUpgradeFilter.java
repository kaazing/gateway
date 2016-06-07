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
package org.kaazing.gateway.transport.http.bridge.filter;

import org.apache.mina.core.filterchain.IoFilterAdapter;
import org.apache.mina.core.filterchain.IoFilterChain;
import org.apache.mina.core.session.IoSession;
import org.kaazing.gateway.transport.LoggingFilter;

public class HttpPostUpgradeFilter extends IoFilterAdapter {

	@Override
	public void messageReceived(NextFilter nextFilter, IoSession session, Object message) throws Exception {
        // GL.debug("http", getClass().getSimpleName() + " request received.");

		// The HttpPostUpgradeFilter prevents a bug where a 101 WebSocket upgrade response also includes
		// a WebSocket frame in the same packet.
		IoFilterChain filterChain = session.getFilterChain();
		filterChain.remove(HttpCodecFilter.class);

		// Give logging filter a chance to move after any remaining codec
		LoggingFilter.moveAfterCodec(session);

		// Fire message down the pipeline
		super.messageReceived(nextFilter, session, message);

		// We've done our job, so remove ourselves from the filter chain
		if (filterChain.contains(this)) {
		    filterChain.remove(this);
		}
	}
}

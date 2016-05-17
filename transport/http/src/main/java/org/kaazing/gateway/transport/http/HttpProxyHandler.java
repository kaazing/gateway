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
package org.kaazing.gateway.transport.http;

import java.net.URI;

import org.apache.mina.core.session.IoSession;
import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.transport.ProxyHandler;
import org.kaazing.gateway.transport.http.bridge.HttpRequestMessage;
import org.kaazing.gateway.transport.http.bridge.HttpResponseMessage;
import org.kaazing.gateway.transport.http.bridge.filter.HttpCodecFilter;


public class HttpProxyHandler extends ProxyHandler {

	private final HttpCodecFilter httpCodec;

	public HttpProxyHandler() {
		httpCodec = new HttpCodecFilter(true);
	}

	@Override
	public void sessionOpened(IoSession session) throws Exception {
		ResourceAddress remoteAddress = (ResourceAddress) session.getAttribute(REMOTE_ADDRESS_KEY);
		URI resource = remoteAddress.getResource();
		int remotePort = resource.getPort();
		String remoteHostName = resource.getHost();

		HttpRequestMessage httpRequest = new HttpRequestMessage();
		httpRequest.setMethod(HttpMethod.CONNECT);
		httpRequest.setRequestURI(new URI(null, null, remoteHostName, remotePort, null, null, null));
		httpRequest.setVersion(HttpVersion.HTTP_1_1);

		session.getFilterChain().addFirst("http", httpCodec);
		session.write(httpRequest);
	}

	@Override
	public void messageReceived(IoSession session, Object message) throws Exception {

		// handshake completed
		session.getFilterChain().remove("http");

		HttpResponseMessage httpResponse = (HttpResponseMessage) message;
		switch (httpResponse.getStatus()) {
		case SUCCESS_OK:
			// deliver delayed creation message for proxy session
			newProxySession(session);
			break;
		default:
			// fail the proxy session connection attempt
			failProxySession(session);
			break;
		}
	}
}


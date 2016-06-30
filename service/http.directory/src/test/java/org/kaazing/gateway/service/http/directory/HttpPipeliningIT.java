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
package org.kaazing.gateway.service.http.directory;

import static org.kaazing.test.util.ITUtil.createRuleChain;

import java.io.File;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.kaazing.gateway.server.test.GatewayRule;
import org.kaazing.gateway.server.test.config.GatewayConfiguration;
import org.kaazing.gateway.server.test.config.builder.GatewayConfigurationBuilder;
import org.kaazing.k3po.junit.annotation.Specification;
import org.kaazing.k3po.junit.rules.K3poRule;

public class HttpPipeliningIT {

	private static final String DIRECTORY_SERVICE_ACCEPT = "http://localhost:8000/";
	private static final String CROSS_ORIGIN_DIRECTORY_SERVICE_ACCEPT = "http://localhost:8001/";
	private static final String ASTRISK_ORIGIN_DIRECTORY_SERVICE_ACCEPT = "http://localhost:8002/";
	private static final String KEEPALIVE_DIRECTORY_SERVICE_ACCEPT = "http://localhost:8003/keepAlive";

	private final K3poRule robot = new K3poRule();

	private final GatewayRule gateway = new GatewayRule() {
		{
			// @formatter:off
			GatewayConfiguration configuration = new GatewayConfigurationBuilder()
					.webRootDirectory(new File("src/test/webapp"))
					.service()
					.accept(KEEPALIVE_DIRECTORY_SERVICE_ACCEPT)
					.type("directory")
					.property("directory", "/public")
					// We have to use this name (which is from
					// TransportOptionNames) instead of
					// "http.keepalive.timeout",
					// see Gateway.camelCaseToDottedLowerCase.
					.acceptOption("http.keepalive.timeout", "3")
					// seconds
					.done()
					.service()
					.accept(DIRECTORY_SERVICE_ACCEPT)
					.type("directory")
					.property("directory", "/public")
					.property("welcome-file", "index.html")
					.done()
					.service()
					.accept(CROSS_ORIGIN_DIRECTORY_SERVICE_ACCEPT)
					.type("directory")
					.property("directory", "/public")
					.crossOrigin()
					.allowOrigin("http://localhost:8000")
					.allowHeaders("x-websocket-protocol")
					.allowMethods("GET")
					.done()
					.done()
					.service()
					.accept(ASTRISK_ORIGIN_DIRECTORY_SERVICE_ACCEPT)
					.type("directory").property("directory", "/public")
					.crossOrigin().allowOrigin("*").done().done().done();
			// @formatter:on
			init(configuration);
		}
	};

	@Rule
	public TestRule chain = createRuleChain(gateway, robot);

	// KG-6739
	@Specification("request1.request2.response1.response2")
	@Test
	public void twoRequestsBeforeReponseOK() throws Exception {
		robot.finish();
	}
}

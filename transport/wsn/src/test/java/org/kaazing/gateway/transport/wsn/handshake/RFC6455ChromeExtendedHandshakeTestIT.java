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
package org.kaazing.gateway.transport.wsn.handshake;

import static org.kaazing.test.util.ITUtil.createRuleChain;

import java.io.File;
import java.io.FileInputStream;
import java.security.KeyStore;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.kaazing.gateway.server.test.GatewayRule;
import org.kaazing.gateway.server.test.config.GatewayConfiguration;
import org.kaazing.gateway.server.test.config.builder.GatewayConfigurationBuilder;
import org.kaazing.k3po.junit.annotation.Specification;
import org.kaazing.k3po.junit.rules.K3poRule;

public class RFC6455ChromeExtendedHandshakeTestIT {

	private K3poRule robot = new K3poRule();
	KeyStore keyStore = null;
	char[] password = "ab987c".toCharArray();
	File keyStorePwFile = new File("target/truststore/keystore.pw");

	public GatewayRule gateway = new GatewayRule() {
		{
			try {
				keyStore = KeyStore.getInstance("JCEKS");
				FileInputStream in = new FileInputStream(
						"target/truststore/keystore.db");
				keyStore.load(in, password);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}

			GatewayConfiguration configuration = new GatewayConfigurationBuilder()
					.service()
	                    .accept("ws://localhost:8001/echo")
	                    .accept("wss://localhost:9001/echo")
	                    .type("echo")
	                    .crossOrigin()
	                        .allowOrigin("http://localhost:8000")
	                    .done()
	                    .crossOrigin()
	                        .allowOrigin("https://localhost:9000")
	                    .done()
	                .done()
	                .service()
	                     .accept("ws://localhost:8001/echoAuth")
	                     .accept("wss://localhost:9001/echoAuth")
	                     .type("echo")
	                     .realmName("demo")
	                     .authorization()
	                        .requireRole("AUTHORIZED")
	                    .done()
	                    .crossOrigin()
	                        .allowOrigin("http://localhost:8000")
	                    .done()
	                    .crossOrigin()
	                        .allowOrigin("https://localhost:9000")
	                    .done()
	                .done()
	                .service()
	                    .accept("ws://localhost:8003/echo8003")
	                    .type("echo")
	                    .crossOrigin()
	                        .allowOrigin("*")
	                    .done()
	                .done()
				    .security()
					// TODO: keyStoreFile and keyStorePasswordFile are
					// deprecated method which will be removed eventually(4.0.1
					// time frame) and keyStore + keyStorePassword should be
					// sufficient.
					// KG-8840
				        .keyStore(keyStore)
				        .keyStorePassword(password)
				        .keyStorePasswordFile(keyStorePwFile)
				        .realm()
				          	.name("demo")
				          	.description("Kaazing WebSocket Gateway Demo")
				          	.httpChallengeScheme("Basic")
				        .done()
				    .done()
                   .done();
			init(configuration);

		}
	};

	@Rule
	public TestRule chain = createRuleChain(gateway, robot);

	@Specification("canCompleteAnExtendedHandshakeAndNegotiateAnApplicationExtension")
	@Test
	public void canCompleteAnExtendedHandshakeAndNegotiateAnApplicationExtension()
			throws Exception {
		robot.finish();
	}

}

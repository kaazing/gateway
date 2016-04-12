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
package org.kaazing.gateway.transport.wsn.proxy;

import static org.kaazing.test.util.ITUtil.createRuleChain;

import java.net.Authenticator;
import java.net.InetAddress;
import java.net.PasswordAuthentication;
import java.net.URL;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.kaazing.gateway.server.test.GatewayRule;
import org.kaazing.gateway.server.test.config.GatewayConfiguration;
import org.kaazing.gateway.server.test.config.builder.GatewayConfigurationBuilder;
import org.kaazing.k3po.junit.annotation.Specification;
import org.kaazing.k3po.junit.rules.K3poRule;

public class ProxyWithConnectorAuthenticatorIT {

    protected static final String TUNNEL_AUTHORIZATION_REALM = "realm";

    @Rule
    public ResetAuthenticatorRule resetAuthenticatorRule = new ResetAuthenticatorRule();

    private K3poRule robot = new K3poRule();

    public GatewayRule gateway = new GatewayRule() {
        {
            // @formatter:off
            GatewayConfiguration configuration = new GatewayConfigurationBuilder()
                .service()
                    .name("proxy")
                    .description("proxy")
                    .accept("tcp://localhost:8000/")
                    .connect("ws://localhost:8080/resource")
                    .realmName(TUNNEL_AUTHORIZATION_REALM)
                    .authorization()
                        .requireRole("AUTHORIZED")
                    .done()
                    .type("proxy")
                .done()
                .security()
                .realm()
                .name(TUNNEL_AUTHORIZATION_REALM)
                    .httpChallengeScheme("Basic")
                    .httpHeader("Authorization")
                    .authorizationMode("challenge")
                    .loginModule()
                        .success("required")
                        .type("class:" + TestLoginModule.class.getName())
                    .done()
                .done()
            .done()
            .done();
            // @formatter:on
            init(configuration);
        }
    };

    @Rule
    public TestRule chain = createRuleChain(gateway, robot);

    @Specification("basic.challenge.and.accept")
    @Test
    public void closeOnFrontBeforeConnectedFullyOnBackShouldKillBack() throws Exception {
        Authenticator.setDefault(new Authenticator(){
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                String host = this.getRequestingHost();
                int port = this.getRequestingPort();
                String prompt = this.getRequestingPrompt();
                String protocol = this.getRequestingProtocol();
                String scheme = this.getRequestingScheme();
                InetAddress site = this.getRequestingSite();
                URL url = this.getRequestingURL();
                RequestorType type = this.getRequestorType();
                System.out.println("canAuthenticate: \n" + host + "\n" + port + "\n" + prompt + "\n" + protocol + "\n" + scheme
                        + "\n" + site + "\n" + url + "\n" + type + "\n");
                return new PasswordAuthentication("joe", new char[] {'w', 'e', 'l', 'c', 'o', 'm', 'e'});
            }
        });
        robot.finish();
    }
}

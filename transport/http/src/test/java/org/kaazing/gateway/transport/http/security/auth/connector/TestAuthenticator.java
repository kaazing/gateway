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

package org.kaazing.gateway.transport.http.security.auth.connector;

import java.net.Authenticator;
import java.net.InetAddress;
import java.net.PasswordAuthentication;
import java.net.URL;

public class TestAuthenticator extends Authenticator{

    static String host;
    static int port;
    static String prompt;
    static String protocol;
    static String scheme;
    static InetAddress site;
    static URL url;
    static RequestorType type;

    @Override
    protected PasswordAuthentication getPasswordAuthentication() {
         host = this.getRequestingHost();
         port = this.getRequestingPort();
         prompt = this.getRequestingPrompt();
         protocol = this.getRequestingProtocol();
         scheme = this.getRequestingScheme();
         site = this.getRequestingSite();
         url = this.getRequestingURL();
         type = this.getRequestorType();
        return new PasswordAuthentication("joe", new char[] {'w', 'e', 'l', 'c', 'o', 'm', 'e'});
    }

    @Override
    public String toString() {
        return "host: " + host + ", " + "port: " + port + ", " + "prompt: " + prompt + ", " + "protocol: " + protocol + ", "
                + "scheme: " + scheme + ", " + "site: " + site + ", " + "url: " + url + ", " + "type: " + type;
    }
}


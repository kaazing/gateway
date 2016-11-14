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
package org.kaazing.gateway.util.feature;

/**
 * Specifies new features which are being released as early access features.
 */
public interface EarlyAccessFeatures {

    EarlyAccessFeature HTTP_PROXY_SERVICE = new EarlyAccessFeature("http.proxy", "HTTP Proxy Service", false);
    EarlyAccessFeature WSN_302_REDIRECT = new EarlyAccessFeature("wsn.302.redirect", "Send redirect for wsn via 302", false);
    EarlyAccessFeature WSX_302_REDIRECT = new EarlyAccessFeature("wsx.302.redirect", "Send redirect for wsx via 302", false);
    EarlyAccessFeature TURN_REST_SERVICE = new EarlyAccessFeature("turn.rest", "TURN REST Service", false);
    EarlyAccessFeature HTTP_AUTHENTICATOR = new EarlyAccessFeature("http.authenticator", "HTTP Authenticator", false);
    EarlyAccessFeature LOGIN_MODULE_EXPIRING_STATE = new EarlyAccessFeature("login.module.expiring.state", "Login Module Expiring State", false);
    EarlyAccessFeature TCP_REALM_EXTENSION = new EarlyAccessFeature("tcp.realm", "TCP Realm", false);
    EarlyAccessFeature HTTP_REALM_ACCEPT_OPTION = new EarlyAccessFeature("http.realm", "Http Realm", false);

}

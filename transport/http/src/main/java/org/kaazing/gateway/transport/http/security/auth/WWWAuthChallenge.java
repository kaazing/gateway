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

package org.kaazing.gateway.transport.http.security.auth;

import static java.lang.String.format;

import java.net.PasswordAuthentication;
import java.util.Arrays;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WWWAuthChallenge {

    static final String TYPE_GROUP_NAME = "scheme";
    static final String REALM_GROUP_NAME = "realm";
    static final Pattern SCHEME_PATTERN =
            Pattern.compile(format("(?<%s>[a-zA-Z_]+) realm=\"(?<%s>[^\"]+)\".*", TYPE_GROUP_NAME, REALM_GROUP_NAME));
    protected static final String[] SUPPORTED_SCHEMES = new String[]{"basic", "digest"};

    private final String challenge;
    private final String realm;
    private final String type;

    public WWWAuthChallenge(String challenge) {
        this.challenge = challenge;
        Matcher matcher = SCHEME_PATTERN.matcher(challenge);
        matcher.matches();
        this.realm = matcher.group(REALM_GROUP_NAME);
        this.type = matcher.group(TYPE_GROUP_NAME);
    }

    public String getChallenge() {
        return challenge;
    }

    public String getRealm() {
        return realm;
    }

    public String getScheme() {
        return type;
    }

    public static String[] getSupportedSchemes() {
        return SUPPORTED_SCHEMES;
    }

    public static String encodeAuthorizationHeader(String scheme, PasswordAuthentication value) {
        if(value == null){
            return null;
        }else if("basic".equalsIgnoreCase(scheme)){
            // https://tools.ietf.org/html/rfc7617
            char[] pwd = value.getPassword();
            String encodedBytes = Base64.getEncoder().encodeToString((value.getUserName() + ":" + new String(pwd)).getBytes());
            Arrays.fill(pwd, ' ');
            return "Basic " + encodedBytes;
        }
        return null;
    }

}

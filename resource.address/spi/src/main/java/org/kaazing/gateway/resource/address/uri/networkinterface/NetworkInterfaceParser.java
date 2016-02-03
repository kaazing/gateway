/**
 * Copyright 2007-2015, Kaazing Corporation. All rights reserved.
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
package org.kaazing.gateway.resource.address.uri.networkinterface;

/**
 * Class performing parsing for  NetworkInterface URI
 *
 */
public class NetworkInterfaceParser {
    private String host;
    private String scheme;
    private int port;
    private String authority;
    private String fragment;
    private String query;
    private String path;
    private String userInfo;

    /**
     * Constructor for NetworkInterfaceParser
     * @param uri
     */
    public NetworkInterfaceParser(String uri) {
        parse();
    }

    public String getHost() {
        return host;
    }

    public String getScheme() {
        return scheme;
    }

    public int getPort() {
        return port;
    }

    public String getAuthority() {
        return authority;
    }

    public String getFragment() {
        return fragment;
    }

    public String getQuery() {
        return query;
    }

    public String getPath() {
        return path;
    }

    public String getUserInfo() {
        return userInfo;
    }

    /**
     * Parse method performing NetworkInterfaceURI parsing
     */
    private final void parse() {
        // TODO Auto-generated method stub
        
    }
}

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

import org.kaazing.gateway.resource.address.uri.URIAccessor;

/**
 * Class performing logic similar to java.net.URI class which supports network interface syntax
 *
 */
public class NetworkInterfaceURI implements URIAccessor {
    private String host;
    private String scheme;
    private int port;
    private String authority;
    private String fragment;
    private String query;
    private String path;
    private String userInfo;
    private NetworkInterfaceParser parser;

    public NetworkInterfaceURI(String uri) {
        parser = new NetworkInterfaceParser(uri);
        host = parser.getHost();
        scheme = parser.getScheme();
        port = parser.getPort();
        authority = parser.getAuthority();
        fragment = parser.getFragment();
        query = parser.getQuery();
        path = parser.getPath();
        userInfo = parser.getUserInfo();
    }

    public static NetworkInterfaceURI create(String str) {
        try {
            return new NetworkInterfaceURI(str);
        } catch (IllegalArgumentException x) {
            throw new IllegalArgumentException(x.getMessage(), x);
        }
    }

    @Override
    public String getHost() {
        return host;
    }

    @Override
    public String getScheme() {
        return scheme;
    }

    @Override
    public String getAuthority() {
        return authority;
    }

    @Override
    public String getFragment() {
        return fragment;
    }

    @Override
    public String getPath() {
        return path;
    }

    @Override
    public String getQuery() {
        return query;
    }

    @Override
    public int getPort() {
        return port;
    }

    @Override
    public String getUserInfo() {
        return userInfo;
    }

    @Override
    public String resolve(String uriString) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String modifyURIScheme(String newScheme) {
        return buildURIFromTokens(newScheme, host, port, path, query, fragment);
    }

    @Override
    public String modifyURIAuthority(String newAuthority) {
        return buildURIFromTokens(scheme, newAuthority, path, query, fragment);
    }

    @Override
    public String modifyURIPort(int newPort) {
        return buildURIFromTokens(scheme, host, newPort, path, query, fragment);
    }

    @Override
    public String modifyURIPath(String newPath) {
        return buildURIFromTokens(scheme, host, port, newPath, query, fragment);
    }

    //TODO: Check whether algorithm is correct with java.net.URI
    private String buildURIFromTokens(String scheme, String host, int port, String path,
            String query, String fragment) {
        return scheme + "://" + host + ":" + port + "/" + path +
                (query != null ? "?" + query : "") +
                (fragment != null ? "#" + fragment : "");
    }

    //TODO: Check whether algorithm is correct with java.net.URI
    private String buildURIFromTokens(String scheme, String authority, String path,
            String query, String fragment) {
        return scheme + "://" + authority + "/" + path +
                (query != null ? "?" + query : "") +
                (fragment != null ? "#" + fragment : "");
    }
}

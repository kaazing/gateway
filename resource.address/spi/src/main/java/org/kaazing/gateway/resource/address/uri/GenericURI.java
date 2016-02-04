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
package org.kaazing.gateway.resource.address.uri;

import java.net.URI;
import java.net.URISyntaxException;

import org.kaazing.gateway.resource.address.uri.networkinterface.NetworkInterfaceURI;


/**
 * Class which is a wrapper either for URI, or for NetworkInterfaceURI
 *
 */
public class GenericURI implements URIAccessor {

    private URIAccessor uri;

    public GenericURI(String uriString) {
        URIAccessorFactory factory = new URIAccessorFactory();
        uri = factory.makeURIAccessor(uriString);
    }

    public static String buildURIToString(String scheme, String authority, String path, String query,
        String fragment) {
        URI helperURI = null;
        try {
            helperURI = new URI(scheme, authority, path, query, fragment);
        } catch (URISyntaxException e) {
            return NetworkInterfaceURI.buildURIToString(scheme, authority, path, query, fragment);
        }
        return helperURI.toString();
    }

    public static String buildURIToString(String scheme, String userInfo, String host, int port, String path,
        String query, String fragment) {
        URI helperURI = null;
        try {
            helperURI = new URI(scheme, userInfo, host, port, path, query, fragment);
        } catch (URISyntaxException e) {
            return NetworkInterfaceURI.buildURIToString(scheme, userInfo, host, port, path, query, fragment);
        }
        return helperURI.toString();
    }

    public static GenericURI create(String uriString) {
        return new GenericURI(uriString);
    }

    @Override
    public String getHost() {
        return uri.getHost();
    }

    @Override
    public String getScheme() {
        return uri.getScheme();
    }

    @Override
    public String getAuthority() {
        return uri.getAuthority();
    }

    @Override
    public String getFragment() {
        return uri.getFragment();
    }

    @Override
    public String getPath() {
        return uri.getPath();
    }

    @Override
    public String getQuery() {
        return uri.getQuery();
    }

    @Override
    public int getPort() {
        return uri.getPort();
    }

    @Override
    public String getUserInfo() {
        return uri.getUserInfo();
    }

    @Override
    public String resolve(String uriString) {
            return uri.resolve(uriString);
    }

    @Override
    public String modifyURIScheme(String newScheme) {
        return uri.modifyURIScheme(newScheme);
    }

    @Override
    public String modifyURIAuthority(String newAuthority) {
        return uri.modifyURIAuthority(newAuthority);
    }

    @Override
    public String modifyURIPort(int newPort) {
        return uri.modifyURIPort(newPort);
    }

    @Override
    public String modifyURIPath(String newPath) {
        return uri.modifyURIPath(newPath);
    }
}

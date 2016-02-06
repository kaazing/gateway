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

/**
 * Utils class over URI methods
 *
 */
public final class URIUtils {

    /**
     * Helper method for toString conversion
     * @param uri
     * @return
     */
    public static String uriToString(URI uri) {
         return uri.toString();
     }

    /**
     * Helper method for toString conversion
     * @param uri
     * @return
     */
    public static String uriToString(NetworkInterfaceURI uri) {
         return uri.toString();
     }

    /**
     * Helper method for retrieving host
     * @param uriString
     * @return
     */
    public static String getHost(String uriString) {
        GenericURI uri = GenericURI.create(uriString);
        return uri.getHost();
    }

    /**
     * Helper method for retrieving scheme
     * @param uriString
     * @return
     */
    public static String getScheme(String uriString) {
        GenericURI uri = GenericURI.create(uriString);
        return uri.getScheme();
    }

    /**
     * Helper method for retrieving authority
     * @param uriString
     * @return
     */
    public static String getAuthority(String uriString) {
        GenericURI uri = GenericURI.create(uriString);
        return uri.getAuthority();
    }

    /**
     * Helper method for retrieving fragment
     * @param uriString
     * @return
     */
    public static String getFragment(String uriString) {
        GenericURI uri = GenericURI.create(uriString);
        return uri.getFragment();
    }

    /**
     * Helper method for retrieving path
     * @param uriString
     * @return
     */
    public static String getPath(String uriString) {
        GenericURI uri = GenericURI.create(uriString);
        return uri.getPath();
    }

    /**
     * Helper method for retrieving query
     * @param uriString
     * @return
     */
    public static String getQuery(String uriString) {
        GenericURI uri = GenericURI.create(uriString);
        return uri.getQuery();
    }

    /**
     * Helper method for retrieving port
     * @param uriString
     * @return
     */
    public static int getPort(String uriString) {
        GenericURI uri = GenericURI.create(uriString);
        return uri.getPort();
    }

    /**
     * Helper method for retrieving port
     * @param uriString
     * @return
     */
    public static String getUserInfo(String uriString) {
        GenericURI uri = GenericURI.create(uriString);
        return uri.getUserInfo();
    }

    /**
     * Helper method for building URI as String
     * @param scheme
     * @param authority
     * @param path
     * @param query
     * @param fragment
     * @return
     * @throws URISyntaxException
     */
    public static String buildURIAsString(String scheme, String authority, String path,
            String query, String fragment) throws URISyntaxException {
        return GenericURI.buildURIToString(scheme, authority, path, query, fragment);
    }

    /**
     * Helper method for building URI as String
     * @param scheme
     * @param userInfo
     * @param host
     * @param port
     * @param path
     * @param query
     * @param fragment
     * @return
     * @throws URISyntaxException
     */
    public static String buildURIAsString(String scheme, String userInfo,
            String host, int port, String path, String query, String fragment) throws URISyntaxException {
        return GenericURI.buildURIToString(scheme, userInfo, host, port, path, query, fragment);
    }

    /**
     * Helper method for performing resolve as String
     * @param uriInitial
     * @param uriString
     * @return
     */
    public static String resolve(String uriInitial, String uriString) {
        GenericURI baseURI = GenericURI.create(uriInitial);
        return baseURI.resolve(uriString);
    }

    /**
     * Helper method for modifying URI scheme
     * @param uri
     * @param newScheme
     * @return
     */
    public static String modifyURIScheme(String uri, String newScheme) {
        GenericURI baseURI = GenericURI.create(uri);
        return baseURI.modifyURIScheme(newScheme);
    }

    /**
     * Helper method for modifying URI authority
     * @param uri
     * @param newAuthority
     * @return
     */
    public static String modifyURIAuthority(String uri, String newAuthority) {
        GenericURI baseURI = GenericURI.create(uri);
        return baseURI.modifyURIAuthority(newAuthority);
    }

    /**
     * Helper method for modifying URI port
     * @param uri
     * @param newPort
     * @return
     */
    public static String modifyURIPort(String uri, int newPort) {
        GenericURI baseURI = GenericURI.create(uri);
        return baseURI.modifyURIPort(newPort);
    }

    /**
     * Helper method for modiffying the URI path
     * @param uri
     * @param newPath
     * @return
     */
    public static String modifyURIPath(String uri, String newPath) {
        GenericURI baseURI = GenericURI.create(uri);
        return baseURI.modifyURIPath(newPath);
    }

}

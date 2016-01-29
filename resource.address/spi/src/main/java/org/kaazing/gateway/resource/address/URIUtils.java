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
package org.kaazing.gateway.resource.address;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Utils class over URI methods  
 *
 */
public class URIUtils {

    /**
     * Helper method for toString converstion 
     * @param uri
     * @return
     */
    public static String uriToString(URI uri) {
         return uri.toString();
     }

    /**
     * Helper method for retrieving host
     * @param uriString
     * @return
     */
    public static String getHost(String uriString) {
        URI uri = URI.create(uriString);
        return uri.getHost();
    }

    /**
     * Helper method for retrieving scheme
     * @param uriString
     * @return
     */
    public static String getScheme(String uriString) {
        URI uri = URI.create(uriString);
        return uri.getScheme();
    }

    /**
     * Helper method for retrieving authority
     * @param uriString
     * @return
     */
    public static String getAuthority(String uriString) {
        URI uri = URI.create(uriString);
        return uri.getAuthority();
    }

    /**
     * Helper method for retrieving fragment
     * @param uriString
     * @return
     */
    public static String getFragment(String uriString) {
        URI uri = URI.create(uriString);
        return uri.getFragment();
    }

    /**
     * Helper method for retrieving path
     * @param uriString
     * @return
     */
    public static String getPath(String uriString) {
        URI uri = URI.create(uriString);
        return uri.getPath();
    }

    /**
     * Helper method for retrieving query
     * @param uriString
     * @return
     */
    public static String getQuery(String uriString) {
        URI uri = URI.create(uriString);
        return uri.getQuery();
    }

    /**
     * Helper method for retrieving port
     * @param uriString
     * @return
     */
    public static int getPort(String uriString) {
        URI uri = URI.create(uriString);
        return uri.getPort();
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
        URI uri = new URI(scheme, authority, path, query, fragment);
        return uri.toString();
    }

    /**
     * Helper method for performing resolve as String
     * @param uriInitial
     * @param uriString
     * @return
     */
    public static String resolve(String uriInitial, String uriString) {
        URI baseURI = URI.create(uriInitial);
        URI resolvedURI = baseURI.resolve(uriString);
        return resolvedURI.toString();
    }

    /**
     * Helper method for modifying URI scheme
     * @param uri
     * @param newScheme
     * @return
     */
    public static String modifyURIScheme(String uri, String newScheme) {
        URI modifiedURI = URLUtils.modifyURIScheme(URI.create(uri), newScheme);
        return modifiedURI.toString();
    }

    /**
     * Helper method for modifying URI authority
     * @param uri
     * @param newAuthority
     * @return
     */
    public static String modifyURIAuthority(String uri, String newAuthority) {
        URI modifiedURI = URLUtils.modifyURIAuthority(URI.create(uri), newAuthority);
        return modifiedURI.toString();
    }

    /**
     * Helper method for modifying URI port
     * @param uri
     * @param newPort
     * @return
     */
    public static String modifyURIPort(String uri, int newPort) {
        URI modifiedURI = URLUtils.modifyURIPort(URI.create(uri), newPort);
        return modifiedURI.toString();
    }

    /**
     * Helper method for modiffying the URI path
     * @param uri
     * @param newPath
     * @return
     */
    public static String modifyURIPath(String uri, String newPath) {
        URI modifiedURI = URLUtils.modifyURIPath(URI.create(uri), newPath);
        return modifiedURI.toString();
    }

}

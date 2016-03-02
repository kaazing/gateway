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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.kaazing.gateway.resource.address.URLUtils;

/**
 * Utils class over URI methods
 *
 */
public final class URIUtils {
    private static final String MOCK_HOST = "127.0.0.1";

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
        try {
            return (new URI(uriString)).getHost();
        }
        catch (URISyntaxException e) {
            try {
                return (new NetworkInterfaceURI(uriString)).getHost();
            }
            catch (IllegalArgumentException ne) {
                throw new IllegalArgumentException(ne.getMessage(), ne);
            }
        }
    }

    /**
     * Helper method for retrieving scheme
     * @param uriString
     * @return
     */
    public static String getScheme(String uriString) {
        try {
            return (new URI(uriString)).getScheme();
        }
        catch (URISyntaxException e) {
            try {
                return (new NetworkInterfaceURI(uriString)).getScheme();
            }
            catch (IllegalArgumentException ne) {
                throw new IllegalArgumentException(ne.getMessage(), ne);
            }
        }
    }

    /**
     * Helper method for retrieving authority
     * @param uriString
     * @return
     */
    public static String getAuthority(String uriString) {
        try {
            return (new URI(uriString)).getAuthority();
        }
        catch (URISyntaxException e) {
            try {
                return (new NetworkInterfaceURI(uriString)).getAuthority();
            }
            catch (IllegalArgumentException ne) {
                throw new IllegalArgumentException(ne.getMessage(), ne);
            }
        }
    }

    /**
     * Helper method for retrieving fragment
     * @param uriString
     * @return
     */
    public static String getFragment(String uriString) {
        try {
            return (new URI(uriString)).getFragment();
        }
        catch (URISyntaxException e) {
            try {
                return (new NetworkInterfaceURI(uriString)).getFragment();
            }
            catch (IllegalArgumentException ne) {
                throw new IllegalArgumentException(ne.getMessage(), ne);
            }
        }
    }

    /**
     * Helper method for retrieving path
     * @param uriString
     * @return
     */
    public static String getPath(String uriString) {
        try {
            return (new URI(uriString)).getPath();
        }
        catch (URISyntaxException e) {
            try {
                return (new NetworkInterfaceURI(uriString)).getPath();
            }
            catch (IllegalArgumentException ne) {
                throw new IllegalArgumentException(ne.getMessage(), ne);
            }
        }
    }

    /**
     * Helper method for retrieving query
     * @param uriString
     * @return
     */
    public static String getQuery(String uriString) {
        try {
            return (new URI(uriString)).getQuery();
        }
        catch (URISyntaxException e) {
            try {
                return (new NetworkInterfaceURI(uriString)).getQuery();
            }
            catch (IllegalArgumentException ne) {
                throw new IllegalArgumentException(ne.getMessage(), ne);
            }
        }
    }

    /**
     * Helper method for retrieving port
     * @param uriString
     * @return
     */
    public static int getPort(String uriString) {
        try {
            return (new URI(uriString)).getPort();
        }
        catch (URISyntaxException e) {
            try {
                return (new NetworkInterfaceURI(uriString)).getPort();
            }
            catch (IllegalArgumentException ne) {
                throw new IllegalArgumentException(ne.getMessage(), ne);
            }
        }
    }

    /**
     * Helper method for retrieving port
     * @param uriString
     * @return
     */
    public static String getUserInfo(String uriString) {
        try {
            return (new URI(uriString)).getUserInfo();
        }
        catch (URISyntaxException e) {
            try {
                return (new NetworkInterfaceURI(uriString)).getUserInfo();
            }
            catch (IllegalArgumentException ne) {
                throw new IllegalArgumentException(ne.getMessage(), ne);
            }
        }
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
        URI helperURI = null;
        try {
            helperURI = new URI(scheme, authority, path, query, fragment);
        } catch (URISyntaxException e) {
            return NetworkInterfaceURI.buildURIToString(scheme, authority, path, query, fragment);
        }
        return helperURI.toString();
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
        URI helperURI = null;
        try {
            helperURI = new URI(scheme, userInfo, host, port, path, query, fragment);
        } catch (URISyntaxException e) {
            return NetworkInterfaceURI.buildURIToString(scheme, userInfo, host, port, path, query, fragment);
        }
        return helperURI.toString();
    }

    /**
     * Helper method for performing resolve as String
     * @param uriInitial
     * @param uriString
     * @return
     */
    public static String resolve(String uriInitial, String uriString) {
        try {
            return uriToString((new URI(uriInitial)).resolve(uriString));
        }
        catch (URISyntaxException e) {
            try {
                return (new NetworkInterfaceURI(uriInitial)).resolve(uriString);
            }
            catch (IllegalArgumentException ne) {
                throw new IllegalArgumentException(ne.getMessage(), ne);
            }
        }
    }

    /**
     * Helper method for modifying URI scheme
     * @param uri
     * @param newScheme
     * @return
     */
    public static String modifyURIScheme(String uri, String newScheme) {
        try {
            URI uriObj = new URI(uri);
            return uriToString(URLUtils.modifyURIScheme(uriObj, newScheme));
        }
        catch (URISyntaxException e) {
            try {
                return (new NetworkInterfaceURI(uri)).modifyURIScheme(newScheme);
            }
            catch (IllegalArgumentException ne) {
                throw new IllegalArgumentException(ne.getMessage(), ne);
            }
        }
    }

    /**
     * Helper method for modifying URI authority
     * @param uri
     * @param newAuthority
     * @return
     */
    public static String modifyURIAuthority(String uri, String newAuthority) {
        try {
            URI uriObj = new URI(uri);
            // code below modifies new authority considering also network interface syntax
            Pattern pattern = Pattern.compile("(\\[{0,1}@[a-zA-Z0-9 :]*\\]{0,1})");
            Matcher matcher = pattern.matcher(newAuthority);
            String matchedToken = MOCK_HOST;
            // if newAuthority corresponds to NetworkInterfaceURI syntax
            if (matcher.find()) {
                matchedToken = matcher.group(0);
                newAuthority = newAuthority.replace(matchedToken, MOCK_HOST);
            }
            URI modifiedURIAuthority = URLUtils.modifyURIAuthority(uriObj, newAuthority);
            String uriWithModifiedAuthority = URIUtils.uriToString(modifiedURIAuthority).replace(MOCK_HOST, matchedToken);
            return uriWithModifiedAuthority;
        }
        catch (URISyntaxException e) {
            try {
                return (new NetworkInterfaceURI(uri)).modifyURIAuthority(newAuthority);
            }
            catch (IllegalArgumentException ne) {
                throw new IllegalArgumentException(ne.getMessage(), ne);
            }
        }
    }

    /**
     * Helper method for modifying URI port
     * @param uri
     * @param newPort
     * @return
     */
    public static String modifyURIPort(String uri, int newPort) {
        try {
            URI uriObj = new URI(uri);
            return uriToString(URLUtils.modifyURIPort(uriObj, newPort));
        }
        catch (URISyntaxException e) {
            try {
                return (new NetworkInterfaceURI(uri)).modifyURIPort(newPort);
            }
            catch (IllegalArgumentException ne) {
                throw new IllegalArgumentException(ne.getMessage(), ne);
            }
        }
    }

    /**
     * Helper method for modiffying the URI path
     * @param uri
     * @param newPath
     * @return
     */
    public static String modifyURIPath(String uri, String newPath) {
        try {
            URI uriObj = new URI(uri);
            return uriToString(URLUtils.modifyURIPath(uriObj, newPath));
        }
        catch (URISyntaxException e) {
            try {
                return (new NetworkInterfaceURI(uri)).modifyURIPath(newPath);
            }
            catch (IllegalArgumentException ne) {
                throw new IllegalArgumentException(ne.getMessage(), ne);
            }
        }
    }

    public static boolean isAbsolute(String uri) {
        try {
            return (new URI(uri)).isAbsolute();
        }
        catch (URISyntaxException e) {
            try {
                return (new NetworkInterfaceURI(uri)).isAbsolute();
            }
            catch (IllegalArgumentException ne) {
                throw new IllegalArgumentException(ne.getMessage(), ne);
            }
        }
    }

}

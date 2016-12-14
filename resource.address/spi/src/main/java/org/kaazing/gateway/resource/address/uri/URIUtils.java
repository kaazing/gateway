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
    public static final String NETWORK_INTERFACE_AUTHORITY_PORT = "^(\\[@[a-zA-Z0-9 :]*\\]|@[a-zA-Z0-9:]*):([0-9]*)$";
    public static final String NETWORK_INTERFACE_AUTHORITY = "(\\[{0,1}@[a-zA-Z0-9 :]*\\]{0,1})";
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
            URI uri = new URI(uriString);
            if (uri.getAuthority().startsWith("@") && !uri.getHost().startsWith("@")) {
                return "@" + uri.getHost();
            }
            return uri.getHost();
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
        URI helperURI;
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
        URI helperURI;
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
            Pattern pattern = Pattern.compile(NETWORK_INTERFACE_AUTHORITY);
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

    /**
     * Class performing logic similar to java.net.URI class which supports network interface syntax
     *
     */
    private static class NetworkInterfaceURI {

        private static final String HOST_TEMPLATE = "127.0.0.1";

        private URI mockNetworkInterfaceURI;
        private Parser parser;

        // -- Properties and components of this instance -- similar to java.net.URI

        // Components of all URIs: [<scheme>:]<scheme-specific-part>[#<fragment>]
        private String scheme;            // null ==> relative URI
        private String fragment;

        // Hierarchical URI components: [//<authority>]<path>[?<query>]
        private String authority;         // Registry or server

        // Server-based authority: [<userInfo>@]<host>[:<port>]
        private String userInfo;
        private String host;              // null ==> registry-based
        private int port = -1;            // -1 ==> undefined

        // Remaining components of hierarchical URIs
        private String path;              // null ==> opaque
        private String query;

        private boolean absolute;

        public static String buildURIToString(String scheme, String authority, String path, String query, String fragment) {
            URI helperURI;
            try {
                helperURI = new URI(scheme, HOST_TEMPLATE, path, query, fragment);
            } catch (URISyntaxException e) {
                throw new IllegalArgumentException(e.getMessage(), e);
            }
            return helperURI.toString().replace(HOST_TEMPLATE, authority);
        }

        public static String buildURIToString(String scheme, String userInfo, String host, int port, String path, String query,
                String fragment) {
            URI helperURI;
            try {
                helperURI = new URI(scheme, userInfo, HOST_TEMPLATE, port, path, query, fragment);
            } catch (URISyntaxException e) {
                throw new IllegalArgumentException(e.getMessage(), e);
            }
            return helperURI.toString().replace(HOST_TEMPLATE, host);
        }

        public NetworkInterfaceURI(String uri) throws IllegalArgumentException {
            parser = new Parser(uri);
            parser.parse();
        }

        /**
         * Method retrieving host.
         * @return - host in uri
         */
        public String getHost() {
            return host;
        }

        /**
         * Method retrieving scheme.
         * @return - scheme in uri
         */
        public String getScheme() {
            return scheme;
        }

        /**
         * Method retrieving authority.
         * @return - authority in uri
         */
        public String getAuthority() {
            return authority;
        }

        /**
         * Method retrieving fragment.
         * @return - fragment in uri
         */
        public String getFragment() {
            return fragment;
        }

        /**
         * Method retrieving path.
         * @return - path in uri
         */
        public String getPath() {
            return path;
        }

        /**
         * Method retrieving query.
         * @return - query in uri
         */
        public String getQuery() {
            return query;
        }

        /**
         * Method retrieving port.
         * @return - port in uri
         */
        public int getPort() {
            return port;
        }

        /**
         * Method retrieving user info section.
         * @return - user info in uri
         */
        public String getUserInfo() {
            return userInfo;
        }

        /**
         * Method retrieving whether uri is absolute.
         * @return - boolean
         */
        public boolean isAbsolute() {
            return absolute;
        }

        /**
         * Method resolving uris
         * @param uriString
         * @return
         */
        public String resolve(String uriString) {
            return parser.resolve(uriString);
        }

        /**
         * Method modifying URI scheme
         * @param newScheme
         * @return - modified uri
         */
        public String modifyURIScheme(String newScheme) {
            return buildURIFromTokens(newScheme, host, port, path, query, fragment);
        }

        /**
         * Method modifying UrI authority
         * @param newAuthority
         * @return - modified uri
         */
        public String modifyURIAuthority(String newAuthority) {
            return buildURIFromTokens(scheme, newAuthority, path, query, fragment);
        }

        /**
         * Method modifying uri port
         * @param newPort
         * @return - modified uri
         */
        public String modifyURIPort(int newPort) {
            return buildURIFromTokens(scheme, host, newPort, path, query, fragment);
        }

        /**
         * Method modifying uri path
         * @param newPath
         * @return - modified uri
         */
        public String modifyURIPath(String newPath) {
            return buildURIFromTokens(scheme, host, port, newPath, query, fragment);
        }

       /**
        * Parser performing NetworkInterfaceSyntax validation and String tokens extraction
        *
        */
        private class Parser {
            private String uri;
            private String matchedToken;

            public Parser(String uri) {
                this.uri = uri;
            }

            /**
             * Method performing parsing
             */
            private void parse() throws IllegalArgumentException {
                if (!uri.startsWith("tcp://") && !uri.startsWith("udp://")) {
                    throw new IllegalArgumentException("Network interface URI syntax should only "
                            + "be applicable for tcp and udp schemes");
                }
                Pattern pattern = Pattern.compile(NETWORK_INTERFACE_AUTHORITY);
                Matcher matcher = pattern.matcher(uri);
                if (!matcher.find()) {
                    throw new IllegalArgumentException("Invalid network interface URI syntax");
                }
                matchedToken = matcher.group(0);
                if (matchedToken.matches(".*:.*:.*")) {
                    throw new IllegalArgumentException("Multiple ':' characters within network interface syntax not allowed");
                }
                if (matchedToken.contains(" ") && (!matchedToken.startsWith("[") || !matchedToken.endsWith("]"))) {
                    throw new IllegalArgumentException("Network interface syntax host contains spaces but misses bracket(s)");
                }
                mockNetworkInterfaceURI = URI.create(uri.replace(matchedToken, HOST_TEMPLATE));
                populateUriDataFromMockInterfaceURI();
            }

            private String resolve(String uriString) {
                return uriToString(mockNetworkInterfaceURI.resolve(uriString)).replace(HOST_TEMPLATE, matchedToken);
            }

            private void populateUriDataFromMockInterfaceURI() {
                scheme = mockNetworkInterfaceURI.getScheme();
                fragment = mockNetworkInterfaceURI.getFragment();
                authority = mockNetworkInterfaceURI.getAuthority().replace(HOST_TEMPLATE, matchedToken);
                userInfo = mockNetworkInterfaceURI.getUserInfo();
                host = mockNetworkInterfaceURI.getHost().replace(HOST_TEMPLATE, matchedToken);
                port = mockNetworkInterfaceURI.getPort();
                path = mockNetworkInterfaceURI.getPath();
                query = mockNetworkInterfaceURI.getQuery();
                absolute = mockNetworkInterfaceURI.isAbsolute();
            }
        }

        //TODO: Check whether algorithm is correct with java.net.URI
        private String buildURIFromTokens(String scheme, String host, int port, String path,
                String query, String fragment) {
            return scheme + "://" + host + ":" + port + (path.isEmpty() ? "" : "/") + path +
                    (query != null ? "?" + query : "") +
                    (fragment != null ? "#" + fragment : "");
        }

        //TODO: Check whether algorithm is correct with java.net.URI
        private String buildURIFromTokens(String scheme, String authority, String path,
                String query, String fragment) {
            return scheme + "://" + authority + (path.isEmpty() ? "" : "/") + path +
                    (query != null ? "?" + query : "") +
                    (fragment != null ? "#" + fragment : "");
        }

    }
    
    /**
     * Create a canonical URI from a given URI.   A canonical URI is a URI with:<ul> <li>the host part of the authority
     * lower-case since URI semantics dictate that hostnames are case insensitive <li>(optionally, NOT appropriate for Origin
     * headers) the path part set to "/" if there was no path in the input URI (this conforms to the WebSocket and HTTP protocol
     * specifications and avoids us having to do special handling for path throughout the server code). </ul>
     *
     * @param uriString        the URI to canonicalize, in string form
     * @param canonicalizePath if true, append trailing '/' when missing
     * @return a URI with the host part of the authority lower-case and (optionally) trailing / added, or null if the uri is null
     * @throws IllegalArgumentException if the uriString is not valid syntax
     */
    public static String getCanonicalURI(String uriString, boolean canonicalizePath) {
        if ((uriString != null) && !"".equals(uriString)) {
            return getCanonicalizedURI(uriString, canonicalizePath);
        }
        return null;
    }

    /**
     * Create a canonical URI from a given URI.   A canonical URI is a URI with:<ul> <li>the host part of the authority
     * lower-case since URI semantics dictate that hostnames are case insensitive <li>(optionally, NOT appropriate for Origin
     * headers) the path part set to "/" except for tcp uris if there was no path in the input URI (this conforms to the
     * WebSocket and HTTP protocol specifications and avoids us having to do special handling for path throughout the server
     * code). </ul>
     *
     * @param uri              the URI to canonicalize
     * @param canonicalizePath if true, append trailing '/' when missing
     * @return a URI with the host part of the authority lower-case and (optionally if not tcp) trailing / added, or null if the
     * uri is null
     * @throws IllegalArgumentException if the uri is not valid syntax
     */
    public static String getCanonicalizedURI(String uri, boolean canonicalizePath) {
        String canonicalURI = uri;
        if (uri != null) {
            String host = getHost(uri);
            String path = getPath(uri);
            final boolean emptyPath = "".equals(path);
            final boolean noPathToCanonicalize = canonicalizePath && (path == null || emptyPath);
            final boolean trailingSlashPath = "/".equals(path);
            final String scheme = getScheme(uri);
            final boolean pathlessScheme = "ssl".equals(scheme) || "tcp".equals(scheme) || "pipe".equals(scheme)
                    || "udp".equals(scheme) || "mux".equals(scheme);
            final boolean trailingSlashWithPathlessScheme = trailingSlashPath && pathlessScheme;
            String newPath = trailingSlashWithPathlessScheme ? "" :
                             noPathToCanonicalize ? (pathlessScheme ? null : "/") : null;
            if (((host != null) && !host.equals(host.toLowerCase())) || newPath != null) {
                path = newPath == null ? path : newPath;
                try {
                    canonicalURI = buildURIAsString(scheme, getUserInfo(uri), host == null ?
                            null : host.toLowerCase(), getPort(uri), path, getQuery(uri), getFragment(uri));
                } catch (URISyntaxException ex) {
                    throw new IllegalArgumentException("Invalid URI: " + uri + " in Gateway configuration file", ex);
                }
            }
        }
        return canonicalURI;
    }

}

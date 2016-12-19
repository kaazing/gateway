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
package org.kaazing.gateway.resource.address;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Pattern;

import org.kaazing.gateway.resource.address.uri.URIUtils;

public class URLUtils {

    private static final Pattern MULTIPLE_SLASHES = Pattern.compile("/[/]+");
    private static final String SINGLE_SLASH = "/";

    public static URI modifyURIScheme(URI uri, String newScheme) {
        String scheme = uri.getScheme();
        if (newScheme.equals(scheme)) {
            return uri;
        }
        String authority = uri.getAuthority();
        String path = uri.getPath();
        String query = uri.getQuery();
        String fragment = uri.getFragment();

        try {
            return new URI(newScheme, authority, path, query, fragment);
        } catch (URISyntaxException x) {
            IllegalArgumentException y = new IllegalArgumentException();
            y.initCause(x);
            throw y;
        }
    }

    public static URI modifyURIAuthority(URI uri, String newAuthority) {
        String authority = uri.getAuthority();
        if (newAuthority.equals(authority)) {
            return uri;
        }
        String scheme = uri.getScheme();
        String path = uri.getPath();
        String query = uri.getQuery();
        String fragment = uri.getFragment();

        try {
            return new URI(scheme, newAuthority, path, query, fragment);
        } catch (URISyntaxException x) {
            IllegalArgumentException y = new IllegalArgumentException();
            y.initCause(x);
            throw y;
        }
    }

    public static URI modifyURIPort(URI uri, int newPort) {
        int port = uri.getPort();
        if (newPort == port) {
            return uri;
        }
        String scheme = uri.getScheme();
        String userInfo = uri.getUserInfo();
        String host = uri.getHost();
        String path = uri.getPath();
        String query = uri.getQuery();
        String fragment = uri.getFragment();

        try {
            return new URI(scheme, userInfo, host, newPort, path, query, fragment);
        } catch (URISyntaxException x) {
            IllegalArgumentException y = new IllegalArgumentException();
            y.initCause(x);
            throw y;
        }
    }

    public static URI modifyURIPath(URI uri, String newPath) {
        String path = uri.getPath();
        if (newPath.equals(path)) {
            return uri;
        }
        String scheme = uri.getScheme();
        String authority = uri.getAuthority();
        String query = uri.getQuery();
        String fragment = uri.getFragment();

        try {
            return new URI(scheme, authority, newPath, query, fragment);
        } catch (URISyntaxException x) {
            IllegalArgumentException y = new IllegalArgumentException();
            y.initCause(x);
            throw y;
        }
    }

    public static URI modifyURIQuery(URI uri, String newQuery) {
        String query = uri.getPath();
        if (newQuery.equals(query)) {
            return uri;
        }
        String scheme = uri.getScheme();
        String authority = uri.getAuthority();
        String path = uri.getPath();
        String fragment = uri.getFragment();

        try {
            return new URI(scheme, authority, path, newQuery, fragment);
        } catch (URISyntaxException x) {
            IllegalArgumentException y = new IllegalArgumentException();
            y.initCause(x);
            throw y;
        }
    }

    public static URI modifyURIFragment(URI uri, String newFragment) {
        String fragment = uri.getFragment();
        if (newFragment.equals(fragment)) {
            return uri;
        }
        String scheme = uri.getScheme();
        String authority = uri.getAuthority();
        String query = uri.getQuery();
        String path = uri.getPath();

        try {
            return new URI(scheme, authority, path, query, newFragment);
        } catch (URISyntaxException x) {
            IllegalArgumentException y = new IllegalArgumentException();
            y.initCause(x);
            throw y;
        }
    }

    public static String modifyURIPath(String uri, String newPath) {
        String path = URIUtils.getPath(uri);
        if (newPath.equals(path)) {
            return uri;
        }
        String scheme = URIUtils.getScheme(uri);
        String authority = URIUtils.getAuthority(uri);
        String query = URIUtils.getQuery(uri);
        String fragment = URIUtils.getFragment(uri);

        try {
            return URIUtils.buildURIAsString(scheme, authority, newPath, query, fragment);
        } catch (URISyntaxException x) {
            IllegalArgumentException y = new IllegalArgumentException();
            y.initCause(x);
            throw y;
        }
    }

    public static URI ensureTrailingSlash(URI uri) {
        String newPath = uri.getPath();
        if ( newPath == null || newPath.equals("")) {
            newPath = "/";
        }
        try {
            String scheme = uri.getScheme();
            String authority = uri.getAuthority();
            String query = uri.getQuery();
            String fragment = uri.getFragment();
            return new URI(scheme, authority, newPath, query, fragment);
        } catch (URISyntaxException x) {
            IllegalArgumentException y = new IllegalArgumentException();
            y.initCause(x);
            throw y;
        }
    }

    public static String ensureTrailingSlash(String uri) {
        String newPath = URIUtils.getPath(uri);
        if ( newPath == null || newPath.equals("")) {
            newPath = "/";
        }
        try {
            String scheme = URIUtils.getScheme(uri);
            String authority = URIUtils.getAuthority(uri);
            String query = URIUtils.getQuery(uri);
            String fragment = URIUtils.getFragment(uri);
            return URIUtils.buildURIAsString(scheme, authority, newPath, query, fragment);
        } catch (URISyntaxException x) {
            IllegalArgumentException y = new IllegalArgumentException();
            y.initCause(x);
            throw y;
        }
    }

    public static URI appendURI(URI uri, String postfix) {
        String scheme = uri.getScheme();
        String authority = uri.getAuthority();
        String path = uri.getPath();
        String query = uri.getQuery();
        String fragment = uri.getFragment();

        try {
            return new URI(scheme, authority, MULTIPLE_SLASHES.matcher(path + postfix).replaceAll(SINGLE_SLASH), query, fragment);
        } catch (URISyntaxException x) {
            IllegalArgumentException y = new IllegalArgumentException();
            y.initCause(x);
            throw y;
        }
    }

    public static String appendURI(String uri, String postfix) {
        String scheme = URIUtils.getScheme(uri);
        String authority = URIUtils.getAuthority(uri);
        String path = URIUtils.getPath(uri);
        String query = URIUtils.getQuery(uri);
        String fragment = URIUtils.getFragment(uri);

        try {
            return URIUtils.buildURIAsString(scheme, authority, MULTIPLE_SLASHES.matcher(path + postfix).replaceAll(SINGLE_SLASH), query, fragment);
        } catch (URISyntaxException x) {
            IllegalArgumentException y = new IllegalArgumentException();
            y.initCause(x);
            throw y;
        }
    }

    public static URI truncateURI(URI uri, String postfix) {
        String scheme = uri.getScheme();
        String authority = uri.getAuthority();
        String path = uri.getPath();
        String query = uri.getQuery();
        String fragment = uri.getFragment();

        assert (path.endsWith(postfix));
        path = (path.equals(postfix) ? SINGLE_SLASH : path.substring(0, path.length() - postfix.length()));

        try {
            return new URI(scheme, authority, replaceMultipleSlashesWithSingleSlash(path), query, fragment);
        } catch (URISyntaxException x) {
            IllegalArgumentException y = new IllegalArgumentException();
            y.initCause(x);
            throw y;
        }
    }

    public static String replaceMultipleSlashesWithSingleSlash(String path) {
        return MULTIPLE_SLASHES.matcher(path).replaceAll(SINGLE_SLASH);
    }


    public static boolean isShortTcpURI(URI resource) {
        return resource != null &&
             "tcp".equals(resource.getScheme()) &&
             resource.getHost() != null &&
             resource.getHost().length() <= 256;
    }

    // We can make this tighter but IPAddressUtil is in a sun package sadly
    public static boolean hasLiteralIPAddress(URI resource) {
        String host = resource.getHost();
        if ( host == null || host.isEmpty() ) {
            return false;
        }
        // basic check for a '.'-separated numeric string for now
        return host.matches("([0-9A-Fa-f]|\\.){4,16}");
    }

    public static URI getRootUri(URI requestUri)  {
        if ( requestUri == null ) {
            throw new NullPointerException("requestUri");
        }
        try {
            return new URI(requestUri.getScheme(),
                           requestUri.getUserInfo(),
                           requestUri.getHost(),
                           requestUri.getPort(),
                           "/",
                           null,
                           null);
        } catch (URISyntaxException e) {
            IllegalArgumentException ex = new IllegalArgumentException(requestUri.toString());
            ex.initCause(e);
            throw ex;
        }
    }

    public static URI getPathAndQueryURI(URI requestUri)  {
        if ( requestUri == null ) {
            throw new NullPointerException("requestUri");
        }
        try {
            return new URI(null,
                           null,
                           requestUri.getPath(),
                           requestUri.getQuery(),
                           null);
        } catch (URISyntaxException e) {
            IllegalArgumentException ex = new IllegalArgumentException(requestUri.toString());
            ex.initCause(e);
            throw ex;
        }
    }
}

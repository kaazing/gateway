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

import org.kaazing.gateway.resource.address.URLUtils;
import org.kaazing.gateway.resource.address.uri.networkinterface.NetworkInterfaceURI;

/**
 * Class which is a wrapper either for URI, or for NetworkInterfaceURI
 *
 */
public class GenericURI implements URIAccessors {

    private URI uri;
    private NetworkInterfaceURI networkUri;

    public GenericURI(String uriString) {
        try {
            uri = URI.create(uriString);
        }
        catch (IllegalArgumentException e) {
            networkUri = NetworkInterfaceURI.create(uriString);
        }
    }

    /**
     * Method instantiating a new 
     * @param str
     * @return
     */
    public static GenericURI create(String uriString) {
        try {
            return new GenericURI(uriString);
        } catch (IllegalArgumentException x) {
            throw new IllegalArgumentException(x.getMessage(), x);
        }
    }

    @Override
    public String getHost() {
        if (uri != null) {
            return uri.getHost();
        }
        else {
            return networkUri.getHost();
        }
    }

    @Override
    public String getScheme() {
        if (uri != null) {
            return uri.getScheme();
        }
        else {
            return networkUri.getScheme();
        }
    }

    @Override
    public String getAuthority() {
        if (uri != null) {
            return uri.getAuthority();
        }
        else {
            return networkUri.getAuthority();
        }
    }

    @Override
    public String getFragment() {
        if (uri != null) {
            return uri.getFragment();
        }
        else {
            return networkUri.getFragment();
        }
    }

    @Override
    public String getPath() {
        if (uri != null) {
            return uri.getPath();
        }
        else {
            return networkUri.getPath();
        }
    }

    @Override
    public String getQuery() {
        if (uri != null) {
            return uri.getQuery();
        }
        else {
            return networkUri.getQuery();
        }
    }

    @Override
    public int getPort() {
        if (uri != null) {
            return uri.getPort();
        }
        else {
            return networkUri.getPort();
        }
    }

    @Override
    public String getUserInfo() {
        if (uri != null) {
            return uri.getUserInfo();
        }
        else {
            return networkUri.getUserInfo();
        }
    }

    @Override
    public String resolve(String uriString) {
        if (uri != null) {
            URI resolvedURI = uri.resolve(uriString);
            return URIUtils.uriToString(resolvedURI);
        }
        else {
            return networkUri.resolve(uriString);
        }
    }

    @Override
    public String modifyURIScheme(String newScheme) {
        if (uri != null) {
            URI modifiedURIScheme = URLUtils.modifyURIScheme(uri, newScheme);
            return URIUtils.uriToString(modifiedURIScheme);
        }
        else {
            return networkUri.modifyURIScheme(newScheme);
        }
    }

    @Override
    public String modifyURIAuthority(String newAuthority) {
        if (uri != null) {
            URI modifiedURIAuthority = URLUtils.modifyURIAuthority(uri, newAuthority);
            return URIUtils.uriToString(modifiedURIAuthority);
        }
        else {
            return networkUri.modifyURIScheme(newAuthority);
        }
    }

    @Override
    public String modifyURIPort(int newPort) {
        if (uri != null) {
            URI modifiedURIPort = URLUtils.modifyURIPort(uri, newPort);
            return URIUtils.uriToString(modifiedURIPort);
        }
        else {
            return networkUri.modifyURIPort(newPort);
        }
    }

    @Override
    public String modifyURIPath(String newPath) {
        if (uri != null) {
            URI modifiedURIPath = URLUtils.modifyURIPath(uri, newPath);
            return URIUtils.uriToString(modifiedURIPath);
        }
        else {
            return networkUri.modifyURIPath(newPath);
        }
    }
}

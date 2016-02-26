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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.kaazing.gateway.resource.address.URLUtils;

/**
 * Class which is a wrapper either for URI, or for NetworkInterfaceURI
 *
 */
public class URIWrapper implements URIAccessor {

    private URI uri;
    private static final String MOCK_HOST = "127.0.0.1";

    private URIWrapper(String uriString) {
        uri = URI.create(uriString);
    }

    static URIWrapper create(String uriString) {
        return new URIWrapper(uriString);
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
    public boolean isAbsolute() {
        return uri.isAbsolute();
    }

    @Override
    public String resolve(String uriString) {
        URI resolvedURI = uri.resolve(uriString);
        return URIUtils.uriToString(resolvedURI);
    }

    @Override
    public String modifyURIScheme(String newScheme) {
        URI modifiedURIScheme = URLUtils.modifyURIScheme(uri, newScheme);
        return URIUtils.uriToString(modifiedURIScheme);
    }

    @Override
    public String modifyURIAuthority(String newAuthority) {
        Pattern pattern = Pattern.compile("(\\[{0,1}@[a-zA-Z0-9 :]*\\]{0,1})");
        Matcher matcher = pattern.matcher(newAuthority);
        String matchedToken = MOCK_HOST;
        // if newAuthority corresponds to NetworkInterfaceURI syntax
        if (matcher.find()) {
            matchedToken = matcher.group(0);
            newAuthority = newAuthority.replace(matchedToken, MOCK_HOST);
        }
        URI modifiedURIAuthority = URLUtils.modifyURIAuthority(uri, newAuthority);
        return URIUtils.uriToString(modifiedURIAuthority).replace(MOCK_HOST, matchedToken);
    }

    @Override
    public String modifyURIPort(int newPort) {
        URI modifiedURIPort = URLUtils.modifyURIPort(uri, newPort);
        return URIUtils.uriToString(modifiedURIPort);
    }

    @Override
    public String modifyURIPath(String newPath) {
        URI modifiedURIPath = URLUtils.modifyURIPath(uri, newPath);
        return URIUtils.uriToString(modifiedURIPath);
    }

}

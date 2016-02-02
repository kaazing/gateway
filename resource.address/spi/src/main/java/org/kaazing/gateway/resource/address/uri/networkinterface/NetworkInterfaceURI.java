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

import org.kaazing.gateway.resource.address.uri.URIAccessors;

/**
 * Class performing logic similar to java.net.URI class which supports network interface syntax
 *
 */
public class NetworkInterfaceURI implements URIAccessors {

    public NetworkInterfaceURI(String uri) {
        new NetworkInterfaceParser(uri).parse();
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
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getScheme() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getAuthority() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getFragment() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getPath() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getQuery() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int getPort() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public String getUserInfo() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String resolve(String uriString) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String modifyURIScheme(String newScheme) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String modifyURIAuthority(String newAuthority) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String modifyURIPort(int newPort) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String modifyURIPath(String newPath) {
        // TODO Auto-generated method stub
        return null;
    }


}

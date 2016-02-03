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

import org.kaazing.gateway.resource.address.uri.networkinterface.NetworkInterfaceURI;

/**
 * Factory returning the appropriate UriAccessor
 *
 */
public class URIAccessorFactory {

    /**
     * Method for returning appropriate URIAccessor
     * @param uriString
     * @return
     */
    public URIAccessor makeURIAccessor(String uriString) {
        URIAccessor uri = null;
        try {
            uri = URIWrapper.create(uriString);
        } catch (IllegalArgumentException x) {
            // throws IllegalArgumentException by default when in accordance with NeworkInterfaceURI syntax
            uri = NetworkInterfaceURI.create(uriString);
        }
        return uri;
    }
}

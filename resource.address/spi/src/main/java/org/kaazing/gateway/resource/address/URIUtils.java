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

/**
 * Utils class over URI methods  
 *
 */
public class URIUtils {

    public static String getHost(String uriString) {
    	URI uri = URI.create(uriString);
    	return uri.getHost();
    }

    public static String getScheme(String uriString) {
    	URI uri = URI.create(uriString);
    	return uri.getScheme();
    }

    public static String getAuthority(String uriString) {
    	URI uri = URI.create(uriString);
    	return uri.getAuthority();
    }

    public static String getFragment(String uriString) {
    	URI uri = URI.create(uriString);
    	return uri.getFragment();
    }

    public static String getPath(String uriString) {
    	URI uri = URI.create(uriString);
    	return uri.getPath();
    }

    public static int getPort(String uriString) {
    	URI uri = URI.create(uriString);
    	return uri.getPort();
    }

    public static URI resolve(URI uriInitial, String uriString) {
    	return uriInitial.resolve(uriString);
    }
}

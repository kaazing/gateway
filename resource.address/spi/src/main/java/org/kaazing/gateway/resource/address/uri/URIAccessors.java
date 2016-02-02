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

/**
 * Interface for describing URI accessors API
 *
 */
public interface URIAccessors {
    /**
     * Method retrieving host
     * @return
     */
    String getHost();

    /**
     * Method retrieving scheme
     * @param uriString
     * @return
     */
    String getScheme();

    /**
     * Method retrieving authority
     * @return
     */
    String getAuthority();

    /**
     * Method retrieving fragment
     * @return
     */
    String getFragment();

    /**
     * Method retrieving path
     * @return
     */
    String getPath();

    /**
     * Method retrieving query
     * @return
     */
    String getQuery();

    /**
     * Method retrieving port
     * @return
     */
    int getPort();
    
    /**
     * Method retrieving user info
     * @return
     */
    String getUserInfo();

    /**
     * Method resolving URIs
     * @param uriString
     * @return
     */
    String resolve(String uriString);

    /**
     * Method modifying URI scheme
     * @param newScheme
     * @return
     */
    String modifyURIScheme(String newScheme);

    /**
     * Method modifying URI authority
     * @param newAuthority
     * @return
     */
    String modifyURIAuthority(String newAuthority);

    /**
     * Method modifying URI port
     * @param newPort
     * @return
     */
    String modifyURIPort(int newPort);

    /**
     * Method modifying URI path
     * @param newPath
     * @return
     */
    String modifyURIPath(String newPath);
}

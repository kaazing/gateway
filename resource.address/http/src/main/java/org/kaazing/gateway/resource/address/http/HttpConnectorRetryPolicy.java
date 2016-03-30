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

package org.kaazing.gateway.resource.address.http;

import java.util.List;
import java.util.Map;

import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.resource.address.ResourceAddressFactory;

/**
 * A HttpConnectorRetryPolicy is responsible for governing the behavior of the Http connect transport when
 * a initial connection fails
 * <p/>
 * When an attempt to access a backend sends a non successful response code to an Http Connector (i.e. not a 2xx
 * or a 101) the HttpConnectorRetryPolicy will get a chance to govern the Http connectors behavior on how or if
 * it responds.
 *
 */
public abstract class HttpConnectorRetryPolicy {

    /**
     * Can the presented status code be handled by the HttpConnectorRetryPolicy.
     *
     * @param challengeRequest a challenge request object containing a challenge
     * @return true iff this challenge handler could potentially respond meaningfully to the challenge.
     */
    public abstract boolean canHandle(int status);

    /**
     * Handle the behavior of the retry by setting the ResourceAddress to be used in retry
     * @param addressFactory Address factory on which to construct a new address
     * @param remoteAddress original address used in the response
     * @param responseCode status code of the response
     * @param responseReason status of the response
     * @param responseHeaders of the response
     * @return ResourceAddress to be used in retry, or null if it should not retry
     */
    public abstract ResourceAddress retry(ResourceAddressFactory addressFactory, ResourceAddress remoteAddress, int responseCode,
        String responseReason, Map<String, List<String>> responseHeaders);

}

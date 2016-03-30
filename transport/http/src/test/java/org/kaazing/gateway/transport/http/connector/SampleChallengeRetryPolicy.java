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

package org.kaazing.gateway.transport.http.connector;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.resource.address.ResourceAddressFactory;
import org.kaazing.gateway.resource.address.http.HttpConnectorRetryPolicy;
import org.kaazing.gateway.resource.address.http.HttpResourceAddress;

public class SampleChallengeRetryPolicy extends HttpConnectorRetryPolicy {

    @Override
    public boolean canHandle(int status) {
        return (status == 401);
    }

    @Override
    public ResourceAddress retry(ResourceAddressFactory addressFactory, ResourceAddress remoteAddress, int code, String reason,
        Map<String, List<String>> headers) {
        assert (remoteAddress instanceof HttpResourceAddress);
        if ("Basic realm=\"Kaazing Gateway Demo\"".equals(headers.get("WWW-Authenticate").get(0))) {
            HttpResourceAddress httpRemoteAddress = (HttpResourceAddress) remoteAddress;
            List<String> headerValues = new ArrayList<>();
            String credentials = Base64.getEncoder().encodeToString("joe:welcome".getBytes());
            headerValues.add(credentials);
            httpRemoteAddress.getOption(HttpResourceAddress.WRITE_HEADERS).put("AUTHORIZATION", headerValues);
            return httpRemoteAddress;
        }
        return null;
    }
}

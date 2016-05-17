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
package org.kaazing.gateway.transport.http;

import static java.lang.Character.toLowerCase;
import static java.lang.Character.toUpperCase;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.kaazing.gateway.transport.dispatch.ProtocolDispatcher;

class HttpProtocolDispatcher implements ProtocolDispatcher {

    private static final String HTTP_PROTOCOL = "http/1.1";
    private static final Collection<byte[]> HTTP_DISCRIMINATORS;
    static {
        HttpMethod[] httpMethods = HttpMethod.values();
        List<byte[]> byteArrays = new ArrayList<>(httpMethods.length * 2);
        for (HttpMethod httpMethod : httpMethods) {
            String methodName = httpMethod.name();
            char initialChar = methodName.charAt(0);
            char initialCharUppercase = toUpperCase(initialChar);
            char initialCharLowercase = toLowerCase(initialChar);
            // ASCII
            byteArrays.add(new byte[] { (byte)initialCharUppercase });
            byteArrays.add(new byte[] { (byte)initialCharLowercase });
        }
        HTTP_DISCRIMINATORS =  Collections.unmodifiableList(byteArrays);
    }

    @Override
    public int compareTo(ProtocolDispatcher pd) {
        return protocolDispatchComparator.compare(this, pd);
    }

    @Override
    public String getProtocolName() {
        return HTTP_PROTOCOL;
    }

    @Override
    public Collection<byte[]> getDiscriminators() {
        return HTTP_DISCRIMINATORS;
    }

}




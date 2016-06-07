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
package org.kaazing.gateway.resource.address.httpxdraft;


import static org.junit.Assert.assertEquals;

import java.net.URI;

import org.junit.Before;
import org.junit.Test;
import org.kaazing.gateway.resource.address.ResourceAddress;

public class HttpxDraftResourceAddressFactorySpiTest {

    private HttpxDraftResourceAddressFactorySpi addressFactorySpi;
    private String addressURI;

    @Before
    public void before() {
        addressFactorySpi = new HttpxDraftResourceAddressFactorySpi();
        addressURI = "httpx-draft://localhost:2020/";
    }

    @Test 
    public void createHttpxDraftAddress() throws Exception {
        ResourceAddress address = addressFactorySpi.newResourceAddress(addressURI);
        URI location = address.getResource();
        assertEquals(location.getPort(), 2020);
    }
    
}

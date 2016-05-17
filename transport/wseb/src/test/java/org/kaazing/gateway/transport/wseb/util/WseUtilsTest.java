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
package org.kaazing.gateway.transport.wseb.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.net.URI;

import org.junit.Test;

public class WseUtilsTest {

    @Test
    public void pathPrefixShouldMatch() throws Exception {
        URI create = URI.create("ws://acme.com:8080/path");
        URI test = URI.create("http://acme.com:8080/path/;e/ub/AwlevBRAZsv5fwl1USiGawmxyyA4IUVz?query");
        assertTrue(WseUtils.pathPrefixMatches(create, test));
    }

    @Test
    public void pathPrefixShouldMatchRootPath() throws Exception {
        URI create = URI.create("ws://localhost:8080/");
        URI test = URI.create("http://localhost:8080/3e5f8a124e");
        assertTrue(WseUtils.pathPrefixMatches(create, test));
    }

    @Test
    public void pathPrefixShouldMatchRootPathNoSlash() throws Exception {
        URI create = URI.create("ws://localhost:8080");
        URI test = URI.create("http://localhost:8080/3e5f8a124e");
        assertTrue(WseUtils.pathPrefixMatches(create, test));
    }

    @Test
    public void pathPrefixShouldNotMatch() throws Exception {
        URI create = URI.create("http://localhost:8080/path/;e/cb?query");
        URI test = URI.create("http://localhost:8080/differentPath/3e5f8a124e");
        assertFalse(WseUtils.pathPrefixMatches(create, test));
    }

    @Test
    public void pathPrefixShouldNotMatchNoSemicolon() throws Exception {
        URI create = URI.create("http://localhost:8080/path/e/cb?query");
        URI test = URI.create("http://localhost:8080/path/3e5f8a124e");
        assertFalse(WseUtils.pathPrefixMatches(create, test));
    }

    @Test
    public void pathPrefixShouldNotMatchNoSessionId() throws Exception {
        URI create = URI.create("http://localhost:8080/path;e/cb?query");
        URI test = URI.create("http://localhost:8080/path");
        assertFalse(WseUtils.pathPrefixMatches(create, test));
    }
}


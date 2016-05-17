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
package org.kaazing.gateway.resource.address;

import java.net.URI;

import junit.framework.Assert;

import org.junit.Test;

public class UriResolveTest {

    @Test(expected = NullPointerException.class)
    public void testUriResolveNullUri() throws Exception {
        URI thisURI = URI.create("http://example.com/path1");
        URI thatURI = null;
        thisURI.resolve(thatURI);
    }

    @Test
    public void testUriResolveRelativeUri() throws Exception {
        verifyURIResolveTestCase("http://example.com/", "", "http://example.com/");
        verifyURIResolveTestCase("http://example.com/", "/", "http://example.com/");
        verifyURIResolveTestCase("http://example.com/", "/path1", "http://example.com/path1");
        verifyURIResolveTestCase("http://example.com/", "/path1/path2", "http://example.com/path1/path2");


        verifyURIResolveTestCase("http://example.com/path1", "", "http://example.com/");
        verifyURIResolveTestCase("http://example.com/path1", "/", "http://example.com/");
        verifyURIResolveTestCase("http://example.com/path1", "/path1", "http://example.com/path1");
        verifyURIResolveTestCase("http://example.com/path1", "/path1/path2", "http://example.com/path1/path2");
        verifyURIResolveTestCase("http://example.com/path1", "/path1a", "http://example.com/path1a");
        verifyURIResolveTestCase("http://example.com/path1", "/path1a/path2", "http://example.com/path1a/path2");
    }

    private void verifyURIResolveTestCase(final String source, String newUri, String expectedResult) {
        URI thisURI = URI.create(source);
        URI result = thisURI.resolve(newUri);
        Assert.assertEquals(URI.create(expectedResult), result);
    }
}

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
package org.kaazing.gateway.server.util;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import org.kaazing.gateway.resource.address.URLUtils;

public class URLUtilsTest {

    @Test
    public void getRootUri() throws URISyntaxException {

        try {
            URLUtils.getRootUri(null);
            Assert.fail("expected null pointer exception for null request uri");
        } catch (NullPointerException e) {
            // ok
        }


        List<String> testUriStrings = Arrays.asList(
                "scheme://authority",
                "scheme://authority/",
                "scheme://authority/path1",
                "scheme://authority/path1/path2",
                "scheme://authority/path1/path2?q=1",
                "scheme://authority/path1/path2?q=1#fragment"
        );

        for (String uri : testUriStrings) {
            URI requestUri = URI.create(uri);
            checkRootUriMatchesRequestUri(requestUri);
        }

    }

    private void checkRootUriMatchesRequestUri(URI requestUri) throws URISyntaxException {
        URI rootUri = URLUtils.getRootUri(requestUri);

        Assert.assertEquals("getRootUri(" + requestUri + ") scheme mismatch.", requestUri.getScheme(), rootUri.getScheme());
        Assert.assertEquals(
                "getRootUri(" + requestUri + ") authority mismatch.", requestUri.getAuthority(), rootUri.getAuthority());
        Assert.assertEquals("getRootUri(" + requestUri + ") path mismatch.", "/", rootUri.getPath());

        Assert.assertNull(rootUri.getQuery());
        Assert.assertNull(rootUri.getFragment());
    }

}

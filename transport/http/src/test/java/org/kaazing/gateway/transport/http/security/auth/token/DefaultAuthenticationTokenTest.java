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
package org.kaazing.gateway.transport.http.security.auth.token;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.kaazing.gateway.security.auth.token.DefaultAuthenticationToken;

public class DefaultAuthenticationTokenTest {

    private DefaultAuthenticationToken token;

    @Test
    public void testAddAuthorizationHeaderOK() throws Exception {
        token = new DefaultAuthenticationToken();
        token.add("key", "value");
        assertEquals(1, token.size());
        assertEquals("value", token.get());
        assertEquals("value", token.get(0));
        assertNull(token.get("foo"));
        assertEquals("value", token.get("key"));
        assertEquals("[ scheme=null {'key'->'value'} ]", token.toString());
    }

    @Test
    public void testAddANamedComponentOK() throws Exception {
        token = new DefaultAuthenticationToken();
        token.add("key", "value");
        assertEquals(1, token.size());
        assertEquals("value", token.get());
        assertEquals("value", token.get(0));
        assertNull(token.get("foo"));
        assertEquals("value", token.get("key"));
        assertEquals("[ scheme=null {'key'->'value'} ]", token.toString());
    }

    @Test
    public void testAddANullComponentOK() throws Exception {
        token = new DefaultAuthenticationToken();
        token.add(null);
        assertEquals(1, token.size());
        assertNull(token.get());
        assertNull(token.get(0));
        assertNull(token.get("foo"));
        assertEquals("[ scheme=null {null} ]", token.toString());
    }

    @Test
    public void testConstructors() throws Exception {
        token = new DefaultAuthenticationToken();
        verifyInitialEmptyState();

        token = new DefaultAuthenticationToken(2);
        verifyInitialEmptyState();

        token = new DefaultAuthenticationToken("sampleToken");
        assertEquals(1, token.size());
        assertEquals("sampleToken", token.get());
        assertEquals("sampleToken", token.get(0));
        try {
            token.get(1);
            fail("Expected IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException e) {
            //ok
        }
        assertNull(token.get("foo"));
        assertEquals("[ scheme=null {'sampleToken'} ]", token.toString());
    }

    private void verifyInitialEmptyState() {
        assertEquals(0, token.size());
        assertNull(token.get());
        try {
            token.get(0);
            fail("Expected IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException e) {
            //ok
        }
        assertNull(token.get("foo"));
        assertEquals("[ scheme=null <empty authentication token> ]", token.toString());
    }
}

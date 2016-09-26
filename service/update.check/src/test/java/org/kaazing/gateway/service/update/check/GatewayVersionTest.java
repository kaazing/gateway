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
package org.kaazing.gateway.service.update.check;

import static org.junit.Assert.*;
import static org.kaazing.gateway.service.update.check.GatewayVersion.parseGatewayVersion;

import org.junit.Before;
import org.junit.Test;

public class GatewayVersionTest {

    private GatewayVersion middle;
    private GatewayVersion high1;
    private GatewayVersion high2;
    private GatewayVersion high3;
    private GatewayVersion low1;
    private GatewayVersion low2;
    private GatewayVersion low3;
    private GatewayVersion equal;
    private GatewayVersion rc1;
    private GatewayVersion rc2;
    private GatewayVersion equalrc1;


    @Before
    public void init() throws Exception {
        middle = new GatewayVersion(3, 1, 3);
        high1 = parseGatewayVersion("4.1.3");
        high2 = parseGatewayVersion("3.2.3");
        high3 = parseGatewayVersion("3.1.4");
        low1 = parseGatewayVersion("2.1.3");
        low2 = parseGatewayVersion("3.0.3");
        low3 = parseGatewayVersion("3.1.2");
        equal = parseGatewayVersion("3.1.3");
        rc1 = new GatewayVersion(3, 1, 3, "RC001");
        rc2 = parseGatewayVersion("3.1.3-RC002");
        equalrc1 = parseGatewayVersion("3.1.3-RC001");
    }

    @Test
    public void testVersioncompareTo() {
        assertTrue(middle.compareTo(high1) < 0);
        assertTrue(middle.compareTo(high2) < 0);
        assertTrue(middle.compareTo(high3) < 0);
        assertTrue(middle.compareTo(low1) > 0);
        assertTrue(middle.compareTo(low2) > 0);
        assertTrue(middle.compareTo(low3) > 0);
        assertTrue(middle.compareTo(equal) == 0);
        assertTrue(middle.compareTo(middle) == 0);
        assertTrue(rc1.compareTo(middle) < 0);
        assertTrue(rc2.compareTo(middle) < 0);
        assertTrue(rc2.compareTo(rc1) > 0);
        assertTrue(rc2.compareTo(low3) > 0);
    }

    @Test
    public void testEqual() {
        assertFalse(middle.equals(high1));
        assertFalse(middle.equals(high2));
        assertFalse(middle.equals(high3));
        assertFalse(middle.equals(low1));
        assertFalse(middle.equals(low2));
        assertFalse(middle.equals(low3));
        assertFalse(middle.equals(null));
        assertTrue(middle.equals(middle));
        assertTrue(middle.equals(equal));
        assertTrue(rc1.equals(equalrc1));
    }
}

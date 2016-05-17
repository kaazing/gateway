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
package org.kaazing.gateway.server.impl;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class GatewayImplTest {

    @Test
    public void shouldAllowGreaterJavaMajorVersion() {
        assertTrue(GatewayImpl.supportedJavaVersion("2.6.0_11", "vendor", 1, 8, "0"));
    }

    @Test
    public void shouldAllowGreaterJavaMinorVersion() {
        assertTrue(GatewayImpl.supportedJavaVersion("1.9.0_19", "vendor", 1, 8, "0"));
    }

    @Test
    public void shouldAllowGreaterJavaPointVersion() {
        assertTrue(GatewayImpl.supportedJavaVersion("1.8.1_20", "vendor", 1, 8, "0"));
    }

    @Test
    public void shouldAllowGreaterJavaBuildVersion() {
        assertTrue(GatewayImpl.supportedJavaVersion("1.8.0_5", "vendor", 1, 8, "0"));
    }

    @Test
    public void shouldAllowEqualJavaVersion() {
        assertTrue(GatewayImpl.supportedJavaVersion("1.8.0", "vendor", 1, 8, "0"));
    }

    @Test
    public void shouldDisallowSmallerJavaMajorVersion() {
        assertFalse(GatewayImpl.supportedJavaVersion("0.8.1_31", "vendor", 1, 8, "0"));
    }

    @Test
    public void shouldDisallowSmallerJavaMinorVersion() {
        assertFalse(GatewayImpl.supportedJavaVersion("1.7.1_21", "vendor", 1, 8, "0"));
    }

    @Test
    // leave this test as is: No java point version requirement
    public void shouldDisallowSmallerJavaPointVersion() {
        assertFalse(GatewayImpl.supportedJavaVersion("1.7.0_21", "vendor", 1, 7, "1_21"));
    }

    @Test
    // leave this test as is: No java build version requirement
    public void shouldDisallowSmallerJavaBuildVersion() {
        assertFalse(GatewayImpl.supportedJavaVersion("1.7.0_20", "vendor", 1, 7, "0_21"));
    }

    @Test
    public void shouldAllowGreaterJavaAzulMajorVersion() {
        assertTrue(GatewayImpl.supportedJavaVersion("2.7.0-zing_5.10.1.0", "Azul", 1, 8, "0"));
    }

    @Test
    public void shouldAllowGreaterJavaAzulMinorVersion() {
        assertTrue(GatewayImpl.supportedJavaVersion("1.9.0-zing_5.10.1.0", "Azul", 1, 8, "0"));
    }

    @Test
    public void shouldAllowEqualJavaAzulMajorAndMinorVersion() {
        assertTrue(GatewayImpl.supportedJavaVersion("1.8.0-zing_5.10.1.0", "Azul", 1, 8, "0"));
    }

    @Test
    public void shouldDisallowSmallerJavaAzulMajorVersion() {
        assertFalse(GatewayImpl.supportedJavaVersion("0.8.1-zing_14.9.1.0", "Azul", 1, 8, "0"));
    }

    @Test
    public void shouldDisallowSmallerJavaAzulMinorVersion() {
        assertFalse(GatewayImpl.supportedJavaVersion("1.7.1-zing_5.10.1.0", "Azul", 1, 8, "AAA"));
    }

}

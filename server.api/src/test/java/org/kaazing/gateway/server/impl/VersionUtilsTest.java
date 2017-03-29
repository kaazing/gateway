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

import static org.kaazing.gateway.server.impl.VersionUtils.getGatewayProductEdition;
import static org.kaazing.gateway.server.impl.VersionUtils.getGatewayProductTitle;
import static org.kaazing.gateway.server.impl.VersionUtils.getGatewayProductVersion;
import static org.kaazing.gateway.server.impl.VersionUtils.getGatewayProductVersionMinor;
import static org.kaazing.gateway.server.impl.VersionUtils.getGatewayProductVersionPatch;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

public class VersionUtilsTest {

    @After
    public void cleanupMockProduct() {
        VersionUtils.reset();
    }

    @Test public void shouldGetProductInfo() {
        System.setProperty("java.class.path", "src/test/resources/gateway.server-5.0.0.8.jar");
        Assert.assertEquals("5.0.0.8 Beta", getGatewayProductVersion());
        Assert.assertEquals("Kaazing WebSocket Gateway", getGatewayProductTitle());
        Assert.assertEquals("Test.Gateway", getGatewayProductEdition());
        Assert.assertEquals("5.0", getGatewayProductVersionMinor());
        Assert.assertEquals("5.0.0", getGatewayProductVersionPatch());
    }

    @Test public void shouldGetDefaultProductInfo() {
        System.setProperty("java.class.path", "src/test/resources/gateway.server-noMF.jar");
        Assert.assertEquals(null, getGatewayProductVersion());
        Assert.assertEquals("Kaazing WebSocket Gateway (Development)", getGatewayProductTitle());
        Assert.assertEquals(null, getGatewayProductEdition());
        Assert.assertEquals(null, getGatewayProductVersionMinor());
        Assert.assertEquals(null, getGatewayProductVersionPatch());
    }

    @Test public void shouldGetProductInfoWhenSystemHasManyJars() {
        System.setProperty("java.class.path",      "src/test/resources/gateway.server-5.0.0.7.jar" +
            System.getProperty("path.separator") + "src/test/resources/gateway.server-5.0.0.8.jar" +
            System.getProperty("path.separator") + "src/test/resources/gateway.server-5.0.0.9.jar" +
            System.getProperty("path.separator") + "src/test/resources/gateway.server-noMF.jar" +
            System.getProperty("path.separator") + "src/test/resources/gateway.server-noAttrs.jar" +
            System.getProperty("path.separator") + "src/test/resources/gateway.server-missing.jar");
        Assert.assertEquals("5.0.0.9 Beta", getGatewayProductVersion());
        Assert.assertEquals("Kaazing WebSocket Gateway", getGatewayProductTitle());
        Assert.assertEquals("Test.Gateway", getGatewayProductEdition());
    }

}

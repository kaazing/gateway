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

import org.junit.Assert;
import org.junit.Test;

public class VersionUtilsTest {

    @Test public void shouldGetProductInfo() {
        System.setProperty("java.class.path", "src/test/resources/gateway.server.test-5.0.0.8.jar");
        Assert.assertEquals("5.0.0.8 Beta", getGatewayProductVersion());
        Assert.assertEquals("Kaazing WebSocket Gateway", getGatewayProductTitle());
        Assert.assertEquals("Gateway", getGatewayProductEdition());
    }

    @Test public void shouldGetProductInfoWhenSystemHasManyJars() {
        System.setProperty("java.class.path",
                "src/test/resources/gateway.server-5.0.0.9.jar; src/test/resources/gateway.server.test-5.0.0.8.jar ");
        Assert.assertEquals("5.0.0.8 Beta", getGatewayProductVersion());
        Assert.assertEquals("Kaazing WebSocket Gateway", getGatewayProductTitle());
        Assert.assertEquals("Gateway", getGatewayProductEdition());
    }

}

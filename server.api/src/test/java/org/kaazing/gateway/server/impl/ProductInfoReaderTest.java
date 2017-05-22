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

import static org.kaazing.gateway.server.impl.ProductInfoReader.getProductInfoInstance;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.kaazing.gateway.server.util.ProductInfo;

public class ProductInfoReaderTest {
    ProductInfo productInfo = new ProductInfo();


    @Test public void shouldGetProductInfo() {
        System.setProperty("java.class.path", "src/test/resources/gateway.server-5.0.0.8.jar");
        productInfo = getProductInfoInstance();
        Assert.assertEquals("5.0.0.8 Beta", productInfo.getVersion());
        Assert.assertEquals("Kaazing WebSocket Gateway", productInfo.getTitle());
        Assert.assertEquals("Test.Gateway", productInfo.getEdition());
        Assert.assertEquals("0", productInfo.getMinor());
        Assert.assertEquals("5", productInfo.getMajor());
        Assert.assertEquals("0", productInfo.getPatch());
        Assert.assertEquals("8 Beta", productInfo.getBuild());
    }

    @Test public void shouldGetDefaultProductInfo() {
        System.setProperty("java.class.path", "src/test/resources/gateway.server-noMF.jar");
        productInfo = getProductInfoInstance();
        Assert.assertEquals(null, productInfo.getVersion());
        Assert.assertEquals("Kaazing WebSocket Gateway (Development)", productInfo.getTitle());
        Assert.assertEquals(null, productInfo.getEdition());
        Assert.assertEquals("0", productInfo.getMinor());
        Assert.assertEquals("0", productInfo.getMajor());
        Assert.assertEquals("0", productInfo.getPatch());
        Assert.assertEquals("0", productInfo.getBuild());
    }

    @Test public void shouldGetProductInfoWhenSystemHasManyJars() {
        System.setProperty("java.class.path",
                "src/test/resources/gateway.server-5.0.0.7.jar" + System.getProperty("path.separator")
                        + "src/test/resources/gateway.server-5.0.0.8.jar" + System.getProperty("path.separator")
                        + "src/test/resources/gateway.server-5.0.0.9.jar" + System.getProperty("path.separator")
                        + "src/test/resources/gateway.server-noMF.jar" + System.getProperty("path.separator")
                        + "src/test/resources/gateway.server-noAttrs.jar" + System.getProperty("path.separator")
                        + "src/test/resources/gateway.server-missing.jar");
        productInfo = getProductInfoInstance();
        Assert.assertEquals("5.0.0.9 Beta", productInfo.getVersion());
        Assert.assertEquals("Kaazing WebSocket Gateway", productInfo.getTitle());
        Assert.assertEquals("Test.Gateway", productInfo.getEdition());
    }

}

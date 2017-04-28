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

import static java.lang.String.format;
import static org.kaazing.gateway.server.impl.VersionUtils.ProductInfo.productDependencies;
import static org.kaazing.gateway.server.impl.VersionUtils.ProductInfo.productEdition;
import static org.kaazing.gateway.server.impl.VersionUtils.ProductInfo.productTitle;
import static org.kaazing.gateway.server.impl.VersionUtils.ProductInfo.productVersion;
import static org.kaazing.gateway.server.impl.VersionUtilsConst.IMPLEMENTATION_TITLE;
import static org.kaazing.gateway.server.impl.VersionUtilsConst.IMPLEMENTATION_VERSION;
import static org.kaazing.gateway.server.impl.VersionUtilsConst.KAAZING_DEPENDENCIES;
import static org.kaazing.gateway.server.impl.VersionUtilsConst.KAAZING_PRODUCT;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.TreeMap;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class VersionUtils {

    private static final Logger LOG = LoggerFactory.getLogger(VersionUtils.class);

    private VersionUtils() {
    }

    public static String getGatewayProductTitle() {
        generateProductInfo();
        return productTitle;
    }

    public static String getGatewayProductVersion() {
        generateProductInfo();
        return productVersion;
    }

    public static String getGatewayProductVersionMajor() {
        ProductInfo product = new ProductInfo(1);
        return product.toString();
    }

    public static String getGatewayProductVersionMinor() {
        ProductInfo product = new ProductInfo(2);
        return product.toString();
    }

    public static String getGatewayProductVersionPatch() {
        ProductInfo product = new ProductInfo(3);
        return product.toString();
    }

    public static String getGatewayProductVersionBuild() {
        ProductInfo product = new ProductInfo(4);
        return product.toString();
    }

    public static String getGatewayProductEdition() {
        generateProductInfo();
        return productEdition;
    }

    public static String getGatewayProductDependencies() {
        generateProductInfo();
        return productDependencies;
    }

    /**
     * Find the product information from the server JAR MANIFEST files and store it
     * in static variables here for later retrieval.
     */
    private synchronized static void generateProductInfo() {
        if (productTitle != null) {
            // We've already run through this before, so do nothing.
            return;
        }
        boolean foundJar = false;
        String[] pathEntries = System.getProperty("java.class.path").split(System.getProperty("path.separator"));
        Map<String, Attributes> products = new TreeMap<>(Collections.reverseOrder());
        HashSet<String> removals = new HashSet<>(7);
        for (String pathEntry : pathEntries) {
            if (!pathEntry.contains("gateway.server")) {
                continue;
            }
            if (LOG.isTraceEnabled()) {
                LOG.trace(format("Found product entry: %s", pathEntry));
            }
            Attributes attrs = readAttributes(pathEntry);
            if(!readProduct(attrs, products) && !readRemovals(attrs, removals))
                continue;
            foundJar = true;
        }
        // remove any products that depend on other products
        for (String removal : removals) {
            products.remove(removal);
        }
        // If running in IDE, there will be no manifest information.
        // Therefore default title to "Kaazing WebSocket Gateway (Development)"
        // and default the others to null.
        productTitle = "Kaazing WebSocket Gateway (Development)";
        productVersion = null;
        productEdition = null;
        productDependencies = null;

        if (foundJar && products.size() != 0) {
            // The remaining values in 'products' are the real top-level product names.
            Attributes attrs = products.entrySet().iterator().next().getValue();
            productTitle = attrs.getValue(IMPLEMENTATION_TITLE);
            productVersion = attrs.getValue(Attributes.Name.IMPLEMENTATION_VERSION);
            productEdition = attrs.getValue(KAAZING_PRODUCT);
            productDependencies = attrs.getValue(KAAZING_DEPENDENCIES);
            if (LOG.isTraceEnabled()) {
                LOG.trace(format("Elected: %s", productEdition));
            }
        }
    }

    private static Attributes readAttributes(String pathEntry) {
        Attributes attrs = new Attributes();
        try (JarFile jar = new JarFile(pathEntry)) {
            Manifest mf = jar.getManifest();
            if (mf != null) {
                attrs = mf.getMainAttributes();
            }
        } catch (IOException e) {
            if (LOG.isWarnEnabled()) {
                LOG.warn("An exception occurred while getting product information", e);
            }
        }
        return attrs;
    }

    private static boolean readProduct(Attributes attrs, Map<String, Attributes> products) {
        boolean result = false;
        if (attrs == null) {
            return result;
        }
        String title = attrs.getValue(IMPLEMENTATION_TITLE);
        String version = attrs.getValue(IMPLEMENTATION_VERSION);
        String product = attrs.getValue(KAAZING_PRODUCT);

        if (product != null && title != null && version != null) {
            result = true;
            if (LOG.isTraceEnabled()) {
                LOG.trace(format("Found: %s", attrs.getValue(KAAZING_PRODUCT)));
            }
            // Store the list of products found, but remove any products
            // marked as dependencies (i.e. products on which the current
            // product depends.  We want to find the product that nothing
            // else depends on.
            products.put(product, attrs);
        }
        return result;
    }

    private static boolean readRemovals(Attributes attrs, HashSet removals) {
        boolean result = false;
        if (attrs == null) {
            return result;
        }
        String title = attrs.getValue(IMPLEMENTATION_TITLE);
        String version = attrs.getValue(IMPLEMENTATION_VERSION);
        String product = attrs.getValue(KAAZING_PRODUCT);
        if (product != null && title != null && version != null) {
            result = true;
            String dependencies = attrs.getValue(KAAZING_DEPENDENCIES);
            if (dependencies != null) {
                String[] deps = dependencies.split(",");
                Collections.addAll(removals, deps);
            }
        }
        return result;
    }

    protected static class ProductInfo {

        protected static volatile String productTitle;
        protected static volatile String productVersion;
        protected static volatile String productEdition;
        protected static volatile String productDependencies;

        int digits;

        public ProductInfo(int digits) {
            this.digits = digits;
        }

        @Override
        public String toString() {
            String version = getGatewayProductVersion();
            if (version == null || version.length() == 0) {
                return null;
            }
            // Non SNAPSHOT versions will be 3 digits in value.
            // develop-SNAPSHOT will always be considered the lowest version
            // available
            if ("develop-SNAPSHOT".equals(version)) {
                return "0.0.0";
            }
            String[] splits = version.split("\\.");
            switch (splits.length) {
            case 1:
                if (digits >= 1)
                    return (version + String.join("", Collections.nCopies(digits - 1, ".0")));

            case 2:
                if (digits >= 2)
                    return (version + String.join("", Collections.nCopies(digits - 2, ".0")));
            case 3:
                if (digits >= 3)
                    return (version + String.join("", Collections.nCopies(digits - 3, ".0")));
            }
            if (digits >= 4)
                return version;
            else
                return version.substring(0, digits * 2 - 1);
        }
    }

    /**
     * Used for testing purposes
     */
    public static void reset() {
        productEdition = null;
        productTitle = null;
        productVersion = null;
        productDependencies = null;
    }

    /**
     * Used for testing purposes
     */
    public static void reset(String edition, String title, String version, String dependencies) {
        productEdition = edition;
        productTitle = title;
        productVersion = version;
        productDependencies = dependencies;
    }
}

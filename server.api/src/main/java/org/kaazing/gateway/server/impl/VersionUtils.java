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

    private static String productTitle;
    private static String productVersion;
    private static String productEdition;
    private static String productDependencies;

    private static final Logger LOG = LoggerFactory.getLogger(VersionUtils.class);

    public static final String KAAZING_PRODUCT = "Kaazing-Product";
    public static final String IMPLEMENTATION_TITLE = "Implementation-Title";
    public static final String IMPLEMENTATION_VERSION = "Implementation-Version";
    public static final String KAAZING_DEPENDENCIES = "Kaazing-Dependencies";

    private VersionUtils() {
    }

    public static String getGatewayProductTitle() {
        getGatewayProductInfo();
        return productTitle;
    }

    public static String getGatewayProductVersion() {
        getGatewayProductInfo();
        return productVersion;
    }

    public static String getGatewayProductVersionMajor() {
        String v = getGatewayProductVersion();
        if (v == null) {
            return null;
        }
        int dotPos = v.indexOf('.');
        return dotPos < 0 ? v : v.substring(0, dotPos);
    }

    public static String getGatewayProductVersionMinor() {
        String v = getGatewayProductVersion();
        if (v == null || v.length() == 0) {
            return null;
        }
        int dotPos = v.indexOf('.');
        if (dotPos < 0) {
            return v + ".0";
        }
        dotPos = v.indexOf('.', dotPos + 1);  // 2nd dot
        return dotPos < 0 ? v : v.substring(0, dotPos);
    }

    public static String getGatewayProductVersionPatch() {
        String v = getGatewayProductVersion();
        // Non SNAPSHOT versions will be 3 digits in value.
        // develop-SNAPSHOT will always be considered the lowest version
        // available
        if ("develop-SNAPSHOT".equals(v)) {
            return "0.0.0";
        }
        if (v == null || v.length() == 0) {
            return null;
        }
        int dotPos = v.indexOf('.');
        if (dotPos < 0) {
            return v + ".0.0";
        }
        dotPos = v.indexOf('.', dotPos + 1);  // 2nd dot
        if (dotPos < 0) {
            return v + ".0";
        }
        dotPos = v.indexOf('.', dotPos + 1);  // 3rd dot
        return dotPos < 0 ? v : v.substring(0, dotPos);
    }

    public static String getGatewayProductVersionBuild() {
        String v = getGatewayProductVersion();
        if (v == null || v.length() == 0) {
            return null;
        }
        int dotPos = v.indexOf('.');
        if (dotPos < 0) {
            return v + ".0.0.0";
        }
        dotPos = v.indexOf('.', dotPos + 1);  // 2nd dot
        if (dotPos < 0) {
            return v + ".0.0";
        }
        dotPos = v.indexOf('.', dotPos + 1);  // 3rd dot
        if (dotPos < 0) {
            return v + ".0";
        }
        // we know there is no 4th dot
        return v;
    }

    public static String getGatewayProductEdition() {
        getGatewayProductInfo();
        return productEdition;
    }

    public static String getGatewayProductDependencies() {
        getGatewayProductInfo();
        return productDependencies;
    }

    /**
     * Find the product information from the server JAR MANIFEST files and store it
     * in static variables here for later retrieval.
     */
    private static void getGatewayProductInfo() {
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
            if(!getProductInfoFromJar(pathEntry, products, removals))
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

    private static boolean getProductInfoFromJar(String pathEntry, Map<String, Attributes> products, HashSet<String> removals){
        boolean result = false;
        try (JarFile jar = new JarFile(pathEntry)) {
            Manifest mf = jar.getManifest();
            if (mf == null) {
                return result;
            }
            Attributes attrs = mf.getMainAttributes();
            if (attrs == null) {
                return result;
            }
            String title = attrs.getValue(IMPLEMENTATION_TITLE);
            String version = attrs.getValue(IMPLEMENTATION_VERSION);
            String product = attrs.getValue(KAAZING_PRODUCT);
            if (product != null && title != null && version != null) {
                result = true;
                String dependencies = attrs.getValue(KAAZING_DEPENDENCIES);
                if (LOG.isTraceEnabled()) {
                    LOG.trace(format("Found: %s [%s]", pathEntry, attrs.getValue(KAAZING_PRODUCT)));
                }
                // Store the list of products found, but remove any products
                // marked as dependencies (i.e. products on which the current
                // product depends.  We want to find the product that nothing
                // else depends on.
                products.put(product, attrs);
                if (dependencies != null) {
                    String[] deps = dependencies.split(",");
                    Collections.addAll(removals, deps);
                }
            }
        } catch (IOException e) {
            if (LOG.isWarnEnabled()) {
                LOG.warn("An exception occurred while getting product information", e);
            }
        }
        return result;
    }
}

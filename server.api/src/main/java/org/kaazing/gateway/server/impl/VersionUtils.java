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

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

public final class VersionUtils {

    public static String PRODUCT_TITLE;
    public static String PRODUCT_VERSION;
    public static String PRODUCT_EDITION;
    public static String PRODUCT_DEPENDENCIES;

    private VersionUtils() {
    }

    public static String getGatewayProductTitle() {
        getGatewayProductInfo();
        return PRODUCT_TITLE;
    }

    public static String getGatewayProductVersion() {
        getGatewayProductInfo();
        return PRODUCT_VERSION;
    }

    public static String getGatewayProductVersionMajor() {
        String v = getGatewayProductVersion();

        if (v == null) {
            return null;
        }

        int dotPos = v.indexOf(".");
        return dotPos < 0 ? v : v.substring(0, dotPos);
    }

    public static String getGatewayProductVersionMinor() {
        String v = getGatewayProductVersion();

        if (v == null || v.length() == 0) {
            return null;
        }

        int dotPos = v.indexOf(".");

        if (dotPos < 0) {
            return v + ".0";
        }

        dotPos = v.indexOf(".", dotPos + 1);  // 2nd dot

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

        int dotPos = v.indexOf(".");

        if (dotPos < 0) {
            return v + ".0.0";
        }

        dotPos = v.indexOf(".", dotPos + 1);  // 2nd dot

        if (dotPos < 0) {
            return v + ".0";
        }

        dotPos = v.indexOf(".", dotPos + 1);  // 3rd dot

        return dotPos < 0 ? v : v.substring(0, dotPos);
    }

    public static String getGatewayProductVersionBuild() {
        String v = getGatewayProductVersion();

        if (v == null || v.length() == 0) {
            return null;
        }

        int dotPos = v.indexOf(".");

        if (dotPos < 0) {
            return v + ".0.0.0";
        }

        dotPos = v.indexOf(".", dotPos + 1);  // 2nd dot

        if (dotPos < 0) {
            return v + ".0.0";
        }

        dotPos = v.indexOf(".", dotPos + 1);  // 3rd dot

        if (dotPos < 0) {
            return v + ".0";
        }

        // we know there is no 4th dot
        return v;
    }

    public static String getGatewayProductEdition() {
        getGatewayProductInfo();
        return PRODUCT_EDITION;
    }

    public static String getGatewayProductDependencies() {
        getGatewayProductInfo();
        return PRODUCT_DEPENDENCIES;
    }

    /**
     * Find the product information from the server JAR MANIFEST files and store it
     * in static variables here for later retrieval.
     *
     */
    private static void getGatewayProductInfo() {
        // TODO: Now that we've switched the products to include
        // an "assembly.version" JAR, this routine could be greatly
        // simplified. Removals and dependencies should no longer be needed.

        if (PRODUCT_TITLE != null) {
            // We've already run through this before, so do nothing.
            return;
        }

        boolean foundJar = false;

        String[] pathEntries = System.getProperty("java.class.path").split(System.getProperty("path.separator"));
        HashMap<String, Attributes> products = new HashMap<>(7);
        HashSet<String> removals = new HashSet<>(7);
        for (String pathEntry : pathEntries) {
            if (pathEntry.contains("gateway.server")) {
                try {
                    JarFile jar = new JarFile(pathEntry);
                    Manifest mf = jar.getManifest();
                    Attributes attrs = mf.getMainAttributes();
                    if (attrs != null) {
                        String title = attrs.getValue("Implementation-Title");
                        String version = attrs.getValue("Implementation-Version");
                        String product = attrs.getValue("Kaazing-Product");
                        String dependencies = attrs.getValue("Kaazing-Dependencies");
                        if (product != null && title != null && version != null) {
                            foundJar = true;

                            // Store the list of products found, but remove any products
                            // marked as dependencies (i.e. products on which the current
                            // product depends.  We want to find the product that nothing
                            // else depends on.
                            products.put(product != null ? product : title, attrs);
                            if (dependencies != null) {
                                String[] deps = dependencies.split(",");
                                Collections.addAll(removals, deps);
                            }
                        }
                    }
                }
                catch (IOException e) {
                    // ignore
                }
            }
        }

        // remove any products that depend on other products
        for (String removal : removals) {
            products.remove(removal);
        }

        if (!foundJar || products.size() == 0) {
            // If running in IDE, there will be no manifest information.
            // Therefore default title to "Kaazing WebSocket Gateway (Development)"
            // and default the others to null.
            PRODUCT_TITLE = "Kaazing WebSocket Gateway (Development)";
            PRODUCT_VERSION = null;
            PRODUCT_EDITION = null;
            PRODUCT_DEPENDENCIES = null;
        } else {
            // The remaining values in 'products' are the real top-level product names.
            // NOTE: Per discussion with Brian in 3.3, this should be only a single value,
            // so we're going to extract our values from that.
            Attributes attrs = products.values().iterator().next();
            PRODUCT_TITLE = attrs.getValue("Implementation-Title");
            PRODUCT_VERSION = attrs.getValue("Implementation-Version");
            PRODUCT_EDITION = attrs.getValue("Kaazing-Product");
            PRODUCT_DEPENDENCIES = attrs.getValue("Kaazing-Dependencies");
        }
    }

}

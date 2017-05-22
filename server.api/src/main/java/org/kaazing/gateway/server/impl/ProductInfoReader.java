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

import static java.util.jar.Attributes.Name.IMPLEMENTATION_TITLE;
import static java.util.jar.Attributes.Name.IMPLEMENTATION_VERSION;
import static org.kaazing.gateway.server.util.JarAttributeNames.KAAZING_DEPENDENCIES;
import static org.kaazing.gateway.server.util.JarAttributeNames.KAAZING_PRODUCT;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.TreeMap;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.kaazing.gateway.server.util.ProductInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ProductInfoReader {

    private static final Logger LOG = LoggerFactory.getLogger(ProductInfoReader.class);

    private static ProductInfo productInfo;

    private ProductInfoReader() {
    }

    public static ProductInfo getProductInfoInstance(){
        if (productInfo != null) {
            return productInfo;
        }
        return generateProductInfo();
    }


    /**
     * Find the product information from the server JAR MANIFEST files and store it
     * in static variables here for later retrieval.
     */
    private static synchronized ProductInfo generateProductInfo() {
        ProductInfo result = new ProductInfo();
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
            if((readProduct(attrs, products) || readRemovals(attrs, removals)))
                foundJar = true;
        }
        // remove any products that depend on other products
        for (String removal : removals) {
            products.remove(removal);
        }

        if (foundJar && products.size() != 0) {
            // The remaining values in 'products' are the real top-level product names.
            Attributes attrs = products.entrySet().iterator().next().getValue();
            result.setTitle(attrs.getValue(IMPLEMENTATION_TITLE));
            result.setVersion(attrs.getValue(IMPLEMENTATION_VERSION));
            result.setEdition(attrs.getValue(KAAZING_PRODUCT));
            result.setDependencies(attrs.getValue(KAAZING_DEPENDENCIES));
        }

        return result;
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

    public static void setProductInfo(ProductInfo productInfo) {
        ProductInfoReader.productInfo = productInfo;
    }
}

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

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Properties;
import org.kaazing.gateway.server.Gateway;

public class GatewayCreatorImpl implements GatewayCreator {
    // Wrap the given Gateway with an instance of GatewayImpl.  GatewayImpl is the base implementation of
    // resolving config, launching the Gateway, and destroying the Gateway.  There should be a GatewayImpl
    // in the chain of Gateways to handle this behavior, and other Gateway implementations should decorate
    // the GatewayImpl with additional behavior.
    @Override
    public Gateway createGateway(Gateway gateway) {
        return new GatewayImpl(gateway);
    }

    // Configure the Gateway with the System properties so that any -D parameters used to override
    // properties in gateway-config.xml are correctly set on the Gateway.
    // Bootstrap the Gateway instance with a GATEWAY_HOME if not already set in System properties.
    @Override
    public void configureGateway(Gateway gateway) {
        Properties properties = new Properties();
        properties.putAll(System.getProperties());

        String gatewayHome = properties.getProperty(Gateway.GATEWAY_HOME_PROPERTY);
        if ((gatewayHome == null) || "".equals(gatewayHome)) {
            ClassLoader loader = Thread.currentThread().getContextClassLoader();
            URL classUrl = loader.getResource("org/kaazing/gateway/server/Gateway.class");
            String urlStr;
            try {
                urlStr = URLDecoder.decode(classUrl.toString(), "UTF-8");
            } catch (UnsupportedEncodingException ex) {
                // it's not likely that UTF-8 will be unsupported, so in practice this will never occur
                throw new RuntimeException("Failed to configure Gateway", ex);
            }

            // The URL is coming from a JAR file.  The format of that is supposed to be
            // jar:<url>!/<packagepath>.  Normally the first part of the <url> section will be
            // 'file:', but could be different if somehow the JAR is loaded from the network.
            // We can only handle the 'file' case, so will check.
            int packageSeparatorIndex = urlStr.indexOf("!/");
            if (packageSeparatorIndex > 0) {
                urlStr = urlStr.substring(0, urlStr.indexOf("!/"));
                urlStr = urlStr.substring(4);  // remove the 'jar:' part.
            }

            if (!urlStr.startsWith("file:")) {
                throw new RuntimeException("The Gateway class was not loaded from a file, so we " +
                        "cannot determine the location of GATEWAY_HOME");
            }

            urlStr = urlStr.substring(5);  // remove the 'file:' stuff.
            File jarFile = new File(urlStr);
            gatewayHome = jarFile.getParentFile().getParent();
            properties.setProperty("GATEWAY_HOME", gatewayHome);
        }

        gateway.setProperties(properties);
    }
}

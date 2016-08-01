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
package org.kaazing.gateway.server.config.parse;


/**
 * Enumeration of known XML namespaces for Gateway config files; used by
 * {@link GatewayConfigParser}
 */
public enum GatewayConfigNamespace {

    DRAGONFIRE,
    EXCALIBUR,
    MARCH_2012,
    AUGUST_2012,
    SEPTEMBER_2012,
    SEPTEMBER_2014,
    NOVEMBER_2015,
    CURRENT_NS;

    private static final String NS_DRAGONFIRE_URI = "http://xmlns.kaazing.com/gateway-config/dragonfire";
    private static final String NS_EXCALIBUR_URI = "http://xmlns.kaazing.com/gateway-config/excalibur";
    private static final String NS_MARCH_2012_URI = "http://xmlns.kaazing.com/2012/03/gateway";
    private static final String NS_AUGUST_2012_URI = "http://xmlns.kaazing.com/2012/08/gateway";
    private static final String NS_SEPTEMBER_2012_URI = "http://xmlns.kaazing.com/2012/09/gateway";
    private static final String NS_SEPTEMBER_2014_URI = "http://xmlns.kaazing.org/2014/09/gateway";
    private static final String NS_NOVEMBER_2015_URI = "http://xmlns.kaazing.org/2015/11/gateway";
    private static final String NS_JUNE_2016_URI = "http://xmlns.kaazing.org/2016/06/gateway";

    GatewayConfigNamespace() {
    }

    public static GatewayConfigNamespace fromURI(String nsURI) {
        if (nsURI == null) {
            throw new NullPointerException(nsURI);
        }

        if (nsURI.equalsIgnoreCase(NS_DRAGONFIRE_URI)) {
            return DRAGONFIRE;
        }

        if (nsURI.equalsIgnoreCase(NS_EXCALIBUR_URI)) {
            return EXCALIBUR;
        }

        if (nsURI.equalsIgnoreCase(NS_MARCH_2012_URI)) {
            return MARCH_2012;
        }

        if (nsURI.equalsIgnoreCase(NS_AUGUST_2012_URI)) {
            return AUGUST_2012;
        }

        if (nsURI.equalsIgnoreCase(NS_SEPTEMBER_2012_URI)) {
            return SEPTEMBER_2012;
        }

        if (nsURI.equalsIgnoreCase(NS_SEPTEMBER_2014_URI)) {
            return SEPTEMBER_2014;
        }

        if (nsURI.equalsIgnoreCase(NS_NOVEMBER_2015_URI)) {
            return NOVEMBER_2015;
        }

        if (nsURI.equalsIgnoreCase(NS_JUNE_2016_URI)) {
            return CURRENT_NS;
        }

        throw new IllegalArgumentException(String.format("Unknown/unsupported XML namespace URI '%s'", nsURI));
    }

    public String toURI() {
        String uri = "<Unknown namespace>";

        switch (this) {
            case DRAGONFIRE:
                uri = NS_DRAGONFIRE_URI;
                break;

            case EXCALIBUR:
                uri = NS_EXCALIBUR_URI;
                break;

            case MARCH_2012:
                uri = NS_MARCH_2012_URI;
                break;

            case AUGUST_2012:
                uri = NS_AUGUST_2012_URI;
                break;

            case SEPTEMBER_2012:
                uri = NS_SEPTEMBER_2012_URI;
                break;

            case SEPTEMBER_2014:
                uri = NS_SEPTEMBER_2014_URI;
                break;

            case NOVEMBER_2015:
                uri = NS_NOVEMBER_2015_URI;
                break;
                
            case CURRENT_NS:
                uri = NS_JUNE_2016_URI;
                break;
        }

        return uri;
    }
}

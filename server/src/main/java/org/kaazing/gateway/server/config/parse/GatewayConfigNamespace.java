/**
 * Copyright (c) 2007-2014 Kaazing Corporation. All rights reserved.
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.kaazing.gateway.server.config.parse;

/**
 * Enumeration of known XML namespaces for Gateway config files; used by {@link GatewayConfigParser}
 */
public enum GatewayConfigNamespace {

    DRAGONFIRE,
    EXCALIBUR,
    MARCH_2012,
    AUGUST_2012,
    SEPTEMBER_2012,
    SEPTEMBER_2014;

    private static final String NS_DRAGONFIRE_URI = "http://xmlns.kaazing.com/gateway-config/dragonfire";
    private static final String NS_EXCALIBUR_URI = "http://xmlns.kaazing.com/gateway-config/excalibur";
    private static final String NS_MARCH_2012_URI = "http://xmlns.kaazing.com/2012/03/gateway";
    private static final String NS_AUGUST_2012_URI = "http://xmlns.kaazing.com/2012/08/gateway";
    private static final String NS_SEPTEMBER_2012_URI = "http://xmlns.kaazing.com/2012/09/gateway";
    private static final String NS_SEPTEMBER_2014_URI = "http://xmlns.kaazing.org/2014/09/gateway";

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
        }

        return uri;
    }
}

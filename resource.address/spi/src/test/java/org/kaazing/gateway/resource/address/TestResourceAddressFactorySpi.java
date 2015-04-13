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

package org.kaazing.gateway.resource.address;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class TestResourceAddressFactorySpi extends ResourceAddressFactorySpi<TestResourceAddress> {


    @Override
    public String getSchemeName() {
        return "test";
    }

    @Override
    protected TestResourceAddress newResourceAddress0(URI original, URI location) {
        return new TestResourceAddress(original, location);
    }

    @Override
    protected List<TestResourceAddress> newResourceAddresses0(URI original,
                                                              URI location,
                                                              ResourceOptions options) {

        return getAlternateStrategy().makeAlternates(original, location, options);
    }

    AlternateStrategy getAlternateStrategy() {
        if ("differentAuthorities".equals(System.getProperty("TestResourceAddressFactorySpi.alternateStrategy"))) {
            return new DifferentAuthorities();
        } else if ("duplicateAlternates".equals(System.getProperty("TestResourceAddressFactorySpi.alternateStrategy"))) {
            return new DuplicateAlternates();
        } else {
            return new DifferentPaths();
        }
    }


    @Override
    protected String getTransportName() {
        return "test";
    }

    @Override
    protected String getProtocolName() {
        return "testable";
    }

    @Override
    protected ResourceFactory getTransportFactory() {
        return null;
    }

    @Override
    protected void parseNamedOptions0(URI location,
                                      ResourceOptions options, Map<String, Object> optionsByName) {

        String option = (String) optionsByName.remove(TestResourceAddress.OPTION.name());
        if (option != null) {
            options.setOption(TestResourceAddress.OPTION, option);
        }
    }

    @Override
    protected void setOptions(TestResourceAddress address,
            ResourceOptions options, Object qualifier) {
        super.setOptions(address, options, qualifier);
        
        address.setOption(TestResourceAddress.OPTION, options.getOption(TestResourceAddress.OPTION));
    }

    class DifferentPaths implements AlternateStrategy {
        public DifferentPaths() {
        }

        public List<TestResourceAddress> makeAlternates(URI original,
                                                                  URI location,
                                                                  ResourceOptions options) {

            List<TestResourceAddress> addresses = new ArrayList<>();
            addresses.add(TestResourceAddressFactorySpi.super.newResourceAddress0(original, location, options));
            String path = location.getPath();
            if (path == null || "".equals(path)) {
                path = "/";
            }

            for (int i = 0; i < 3; i++) {
                addresses.add(TestResourceAddressFactorySpi.super.newResourceAddress0(original,
                        URLUtils.modifyURIPath(location, path + String.valueOf(i)),
                        options));
            }

            return addresses;
        }
    }

    class DuplicateAlternates implements AlternateStrategy {
        public DuplicateAlternates() {
        }

        public List<TestResourceAddress> makeAlternates(URI original,
                                                        URI location,
                                                        ResourceOptions options) {

            List<TestResourceAddress> addresses = new ArrayList<>();
            addresses.add(TestResourceAddressFactorySpi.super.newResourceAddress0(original, location, options));
            for (int i = 0; i < 3; i++) {
                addresses.add(TestResourceAddressFactorySpi.super.newResourceAddress0(original,
                        location,
                        options));
            }

            return addresses;
        }
    }

    class DifferentAuthorities implements AlternateStrategy {
        public DifferentAuthorities() {
        }

        public List<TestResourceAddress> makeAlternates(URI original,
                                                        URI location,
                                                        ResourceOptions options) {

            List<TestResourceAddress> addresses = new ArrayList<>();
            addresses.add(TestResourceAddressFactorySpi.super.newResourceAddress0(original, location, options));
            for (int i = 0; i < 3; i++) {
                addresses.add(TestResourceAddressFactorySpi.super.newResourceAddress0(original,
                        URLUtils.modifyURIAuthority(location, location.getAuthority() + String.valueOf(i)),
                        options));
            }

            return addresses;
        }
    }

    interface AlternateStrategy {
         List<TestResourceAddress> makeAlternates(URI original,
                                                  URI location,
                                                  ResourceOptions options);
    }
}

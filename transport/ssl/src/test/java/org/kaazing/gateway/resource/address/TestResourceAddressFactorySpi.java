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
package org.kaazing.gateway.resource.address;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.kaazing.gateway.resource.address.uri.URIUtils;

public final class TestResourceAddressFactorySpi extends ResourceAddressFactorySpi<TestResourceAddress> {


    @Override
    public String getSchemeName() {
        return "test";
    }

    @Override
    protected TestResourceAddress newResourceAddress0(String original, String location) {
        return new TestResourceAddress(this, original, URI.create(location));
    }

    @Override
    protected List<TestResourceAddress> newResourceAddresses0(String original,
                                                              String location,
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
    protected void parseNamedOptions0(String location,
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

        @Override
        public List<TestResourceAddress> makeAlternates(String original,
                                                        String location,
                                                        ResourceOptions options) {

            List<TestResourceAddress> addresses = new ArrayList<>();
            addresses.add(TestResourceAddressFactorySpi.super.newResourceAddress0(original, location, options));
            String path = URIUtils.getPath(location);
            if (path == null || "".equals(path)) {
                path = "/";
            }

            for (int i = 0; i < 3; i++) {
                addresses.add(TestResourceAddressFactorySpi.super.newResourceAddress0(original,
                        (URIUtils.modifyURIPath(location, path + String.valueOf(i))),
                        options));
            }

            return addresses;
        }
    }

    class DuplicateAlternates implements AlternateStrategy {
        public DuplicateAlternates() {
        }

        @Override
        public List<TestResourceAddress> makeAlternates(String original,
                                                        String location,
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

        @Override
        public List<TestResourceAddress> makeAlternates(String original,
                                                        String location,
                                                        ResourceOptions options) {

            List<TestResourceAddress> addresses = new ArrayList<>();
            addresses.add(TestResourceAddressFactorySpi.super.newResourceAddress0(original, location, options));
            for (int i = 0; i < 3; i++) {
                addresses.add(TestResourceAddressFactorySpi.super.newResourceAddress0(original,
                        (URIUtils.modifyURIAuthority(location, URIUtils.getAuthority(location) + String.valueOf(i))),
                        options));
            }

            return addresses;
        }
    }

    interface AlternateStrategy {
         List<TestResourceAddress> makeAlternates(String original,
                                                  String location,
                                                  ResourceOptions options);
    }
}

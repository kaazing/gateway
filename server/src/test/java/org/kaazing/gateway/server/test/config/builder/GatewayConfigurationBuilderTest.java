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
package org.kaazing.gateway.server.test.config.builder;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.kaazing.gateway.server.test.config.GatewayConfiguration;
import org.kaazing.gateway.server.test.config.NestedServicePropertiesConfiguration;


public class GatewayConfigurationBuilderTest {

    @Test
    public void shouldBuildWithSimpleProperties() throws Exception {
        GatewayConfiguration configuration =
                new GatewayConfigurationBuilder()
                        .webRootDirectory(new File("src/test/webapp"))
                        .service()
                        .accept("ws://localhost:8001/jms")
                        .type("test.service")
                        .property("a", "aValue")
                        .property("b", "bValue")
                        .done()
                        .done();

        Map<String, String> properties = configuration.getServices().iterator().next().getProperties();
        assertEquals("aValue", properties.get("a"));
        assertEquals("bValue", properties.get("b"));
    }

    @Test
    public void shouldBuildWithNestedProperties() throws Exception {
        GatewayConfiguration configuration =
                new GatewayConfigurationBuilder()
                        .webRootDirectory(new File("src/test/webapp"))
                        .service()
                        .type("test.service")
                        .accept("ws://localhost:8001/jms")
                        .property("a", "aValue")
                        .property("b", "bValue")
                        .nestedProperty("nested1")
                        .property("1a", "1aValue1")
                        .property("1b", "1bValue1")
                        .done()
                        .nestedProperty("nested1")
                        .property("1a", "1aValue2")
                        .property("1b", "1bValue2")
                        .done()
                        .property("c", "cValue")
                        .nestedProperty("nested2")
                        .property("2a", "2aValue")
                        .property("2b", "2bValue")
                        .done()
                        .done()
                        .done();

        Map<String, String> properties = configuration.getServices().iterator().next().getProperties();
        assertEquals("aValue", properties.get("a"));
        assertEquals("bValue", properties.get("b"));
        assertEquals("cValue", properties.get("c"));
        List<NestedServicePropertiesConfiguration> nestedPropertyConfigs = configuration.getServices().iterator()
                .next().getNestedProperties();
        assertEquals(3, nestedPropertyConfigs.size());

        Map<String, String> nested1Map = nestedPropertyConfigs.get(0).getSimpleProperties();
        Map<String, String> nested2Map = nestedPropertyConfigs.get(1).getSimpleProperties();
        Map<String, String> nested3Map = nestedPropertyConfigs.get(2).getSimpleProperties();
        assertEquals("1aValue1", nested1Map.get("1a"));
        assertEquals("1bValue1", nested1Map.get("1b"));
        assertEquals("1aValue2", nested2Map.get("1a"));
        assertEquals("1bValue2", nested2Map.get("1b"));
        assertEquals("2aValue", nested3Map.get("2a"));
        assertEquals("2bValue", nested3Map.get("2b"));
    }

}

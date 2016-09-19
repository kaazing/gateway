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
import java.util.Collection;
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

        Map<String, List<String>> properties = configuration.getServices().iterator().next().getProperties();
        assertEquals("aValue", properties.get("a").get(0));
        assertEquals("bValue", properties.get("b").get(0));
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
                            .nestedProperty("nested1_level1")
                                .property("1a", "1aValue1")
                                .property("1b", "1bValue1")
                            .done()
                            .nestedProperty("nested1_level1")
                                .property("1a", "1aValue2")
                                .property("1b", "1bValue2")
                            .done()
                            .nestedProperty("nested2_level1")
                                .property("2a", "2aLevel1")
                                .property("2b", "2bLevel1")
                                .nestedProperty("nested2_level2")
                                    .property("2c", "2cLevel2")
                                    .property("2d", "2dLevel2")
                                    .nestedProperty("nested2_level3")
                                        .property("2e", "2eLevel3")
                                        .property("2f", "2fLevel3")
                                    .done()
                                .done()
                            .done()
                            .property("c", "cValue")
                            .nestedProperty("nested3_level1")
                                .property("1a", "1aNested3Level1")
                                .property("3a", "3aValue")
                                .property("3b", "3bValue")
                            .done()
                        .done()
                    .done();

        Map<String, List<String>> properties = configuration.getServices().iterator().next().getProperties();
        assertEquals("aValue", properties.get("a").get(0));
        assertEquals("bValue", properties.get("b").get(0));
        assertEquals("cValue", properties.get("c").get(0));
        List<NestedServicePropertiesConfiguration> nestedPropertyConfigs = configuration.getServices().iterator()
                .next().getNestedProperties();
        assertEquals(4, nestedPropertyConfigs.size());

        Map<String, List<String>> nested1Level1Map1 = nestedPropertyConfigs.get(0).getSimpleProperties();
        Map<String, List<String>> nested1Level1Map2 = nestedPropertyConfigs.get(1).getSimpleProperties();
        assertEquals("1aValue1", nested1Level1Map1.get("1a").get(0));
        assertEquals("1bValue1", nested1Level1Map1.get("1b").get(0));
        assertEquals("1aValue2", nested1Level1Map2.get("1a").get(0));
        assertEquals("1bValue2", nested1Level1Map2.get("1b").get(0));

        Map<String, List<String>> nested2Level1Map = nestedPropertyConfigs.get(2).getSimpleProperties();
        assertEquals("2aLevel1", nested2Level1Map.get("2a").get(0));
        assertEquals("2bLevel1", nested2Level1Map.get("2b").get(0));

        Map<String, List<String>> nested3Level1Map = nestedPropertyConfigs.get(3).getSimpleProperties();
        assertEquals("3aValue", nested3Level1Map.get("3a").get(0));
        assertEquals("3bValue", nested3Level1Map.get("3b").get(0));
        assertEquals("1aNested3Level1", nested3Level1Map.get("1a").get(0));

        Collection<NestedServicePropertiesConfiguration> nested2Level2Configs = nestedPropertyConfigs.get(2).getNestedProperties();
        assertEquals(1, nested2Level2Configs.size());

        NestedServicePropertiesConfiguration nested2Level2Config = nested2Level2Configs.iterator().next();
        Map<String, List<String>> nested2Level2Map = nested2Level2Config.getSimpleProperties();
        assertEquals("2cLevel2", nested2Level2Map.get("2c").get(0));
        assertEquals("2dLevel2", nested2Level2Map.get("2d").get(0));
        assertEquals(2, nested2Level2Map.size());

        Collection<NestedServicePropertiesConfiguration> nested2Level3Configs = nested2Level2Config.getNestedProperties();
        assertEquals(1, nested2Level3Configs.size());

        NestedServicePropertiesConfiguration nested2Level3Config = nested2Level3Configs.iterator().next();
        Map<String, List<String>> nested2Level3Map = nested2Level3Config.getSimpleProperties();
        assertEquals("2eLevel3", nested2Level3Map.get("2e").get(0));
        assertEquals("2fLevel3", nested2Level3Map.get("2f").get(0));
        assertEquals(2, nested2Level3Map.size());
    }
}

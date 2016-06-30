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
package org.kaazing.gateway.server.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.util.List;

import org.junit.Test;
import org.kaazing.gateway.server.context.GatewayContext;
import org.kaazing.gateway.server.test.config.GatewayConfiguration;
import org.kaazing.gateway.server.test.config.builder.GatewayConfigurationBuilder;
import org.kaazing.gateway.service.ServiceProperties;

public class GatewayTest {

    @Test
    public void shouldCreateGatewayContextWithSimpleProperties() throws Exception {
        GatewayConfiguration configuration =
                new GatewayConfigurationBuilder()
                        .webRootDirectory(new File("src/test/webapp"))
                        .service()
                        .accept("ws://localhost:8001/jms")
                        .type("echo")
                        .property("a", "aValue")
                        .property("b", "bValue")
                        .done()
                        .done();

        GatewayContext gateway = new Gateway().createGatewayContext(configuration);
        ServiceProperties properties = gateway.getServices().iterator().next().getProperties();
        assertEquals("aValue", properties.get("a"));
        assertEquals("bValue", properties.get("b"));
    }

    @Test
    public void shouldCreateGatewayContextWithNestedProperties() throws Exception {
        GatewayConfiguration configuration =
                new GatewayConfigurationBuilder()
                        .webRootDirectory(new File("src/test/webapp"))
                        .service()
                            .type("echo")
                            .accept("ws://localhost:8001/jms")
                            .property("a", "aValue")
                            .property("b", "bValue")
                            .nestedProperty("nestedA")
                                .property("Aa", "AaValue1")
                                .property("Ab", "AbValue1")
                            .done()
                            .nestedProperty("nestedA")
                                .property("Aa", "AaValue2")
                                .property("Ab", "AbValue2")
                            .done()
                            .property("c", "cValue")
                            .nestedProperty("nestedB")
                                .property("Ba", "BaValue")
                                .property("Bb", "BbValue")
                                .nestedProperty("nestedC")
                                    .property("Ca", "CaValue")
                                    .property("Cb", "CbValue")
                                    .nestedProperty("nestedD")
                                        .property("Da", "DaValue")
                                        .property("Db", "DbValue")
                                    .done()
                                .done()
                            .done()
                        .done()
                    .done();


        GatewayContext gateway = new Gateway().createGatewayContext(configuration);

        ServiceProperties properties = gateway.getServices().iterator().next().getProperties();

        assertEquals("aValue", properties.get("a"));
        assertEquals("bValue", properties.get("b"));
        assertEquals("cValue", properties.get("c"));

        List<ServiceProperties> nestedA = properties.getNested("nestedA");
        assertNotNull(nestedA);
        assertEquals(2, nestedA.size());
        assertEquals("AaValue1", nestedA.get(0).get("Aa"));
        assertEquals("AbValue1", nestedA.get(0).get("Ab"));
        assertEquals("AaValue2", nestedA.get(1).get("Aa"));
        assertEquals("AbValue2", nestedA.get(1).get("Ab"));

        List<ServiceProperties> nestedB = properties.getNested("nestedB");
        assertNotNull(nestedB);
        assertEquals("BaValue", nestedB.get(0).get("Ba"));
        assertEquals("BbValue", nestedB.get(0).get("Bb"));
        assertEquals(1, nestedB.size());

        List<ServiceProperties> nestedC = nestedB.get(0).getNested("nestedC");
        assertNotNull(nestedC);
        assertEquals("CaValue", nestedC.get(0).get("Ca"));
        assertEquals("CbValue", nestedC.get(0).get("Cb"));
        assertEquals(1, nestedC.size());

        List<ServiceProperties> nestedD = nestedC.get(0).getNested("nestedD");
        assertNotNull(nestedD);
        assertEquals("DaValue", nestedD.get(0).get("Da"));
        assertEquals("DbValue", nestedD.get(0).get("Db"));
        assertEquals(1, nestedD.size());
    }
}

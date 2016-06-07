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
package org.kaazing.gateway.management.impl;

import java.util.Arrays;
import java.util.Collections;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.kaazing.gateway.management.config.ServiceConfigurationBeanImpl;
import org.kaazing.gateway.management.gateway.GatewayManagementBean;
import org.kaazing.gateway.service.ServiceContext;
import org.kaazing.gateway.service.ServiceProperties;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ServiceConfigurationBeanImplTest {

    @Test
    public void simplePropertiesShouldBeReportedAsJSONString() throws Exception {
        Mockery context = new Mockery();
        final ServiceContext service = context.mock(ServiceContext.class);
        final ServiceProperties properties = context.mock(ServiceProperties.class);
        final GatewayManagementBean gateway = context.mock(GatewayManagementBean.class);

        context.checking(new Expectations() {{
            oneOf(service).getProperties();
            will(returnValue(properties));
            oneOf(properties).simplePropertyNames();
            will(returnValue(Arrays.asList("a", "b")));
            oneOf(properties).get("a");
            will(returnValue("aValue"));
            oneOf(properties).get("b");
            will(returnValue("bValue"));
            oneOf(properties).nestedPropertyNames();
            will(returnValue(Collections.<String>emptyList()));
        }});
        ServiceConfigurationBeanImpl bean = new ServiceConfigurationBeanImpl(service, gateway);
        String expected = "{\"b\":\"bValue\",\"a\":\"aValue\"}";
        String result = bean.getProperties();
        equalsJson(new JSONObject(expected), new JSONObject(result));
    }

    @Test
    public void propertiesIncludingNestedShouldBeReportedAsJSONString() throws Exception {
        Mockery context = new Mockery();
        final ServiceContext service = context.mock(ServiceContext.class);
        final ServiceProperties properties = context.mock(ServiceProperties.class);
        final ServiceProperties nested1a = context.mock(ServiceProperties.class, "nested1a");
        final ServiceProperties nested1b = context.mock(ServiceProperties.class, "nested1b");
        final ServiceProperties nested2 = context.mock(ServiceProperties.class, "nested2");
        final GatewayManagementBean gateway = context.mock(GatewayManagementBean.class);

        context.checking(new Expectations() {{
            oneOf(service).getProperties();
            will(returnValue(properties));
            oneOf(properties).simplePropertyNames();
            will(returnValue(Arrays.asList("a", "b")));
            oneOf(properties).get("a");
            will(returnValue("aValue"));
            oneOf(properties).get("b");
            will(returnValue("bValue"));

            oneOf(properties).nestedPropertyNames();
            will(returnValue(Arrays.asList("nested1", "nested2")));
            oneOf(properties).getNested("nested1");
            will(returnValue(Arrays.asList(nested1a, nested1b)));
            oneOf(properties).getNested("nested2");
            will(returnValue(Collections.singletonList(nested2)));

            oneOf(nested1a).simplePropertyNames();
            will(returnValue(Arrays.asList("aa", "ab")));
            oneOf(nested1a).get("aa");
            will(returnValue("aaValue"));
            oneOf(nested1a).get("ab");
            will(returnValue("abValue"));
            oneOf(nested1a).nestedPropertyNames();
            will(returnValue(Collections.<String>emptyList()));

            oneOf(nested1b).simplePropertyNames();
            will(returnValue(Arrays.asList("ba", "bb")));
            oneOf(nested1b).get("ba");
            will(returnValue("baValue"));
            oneOf(nested1b).get("bb");
            will(returnValue("bbValue"));
            oneOf(nested1b).nestedPropertyNames();
            will(returnValue(Collections.<String>emptyList()));

            oneOf(nested2).simplePropertyNames();
            will(returnValue(Collections.<String>emptyList()));
            oneOf(nested2).nestedPropertyNames();
            will(returnValue(Collections.<String>emptyList()));

        }});
        ServiceConfigurationBeanImpl bean = new ServiceConfigurationBeanImpl(service, gateway);
        String expected = "{\"b\":\"bValue\",\"a\":\"aValue\",\"nested1\":"
                + "[{\"aa\":\"aaValue\",\"ab\":\"abValue\"},{\"ba\":\"baValue\",\"bb\":\"bbValue\"}],\"nested2\":[{}]}";
        String result = bean.getProperties();
        equalsJson(new JSONObject(expected), new JSONObject(result));
    }

    private static void equalsJson(JSONObject obj1, JSONObject obj2) throws Exception {
        assertEquals(obj1.length(), obj2.length());
        JSONArray names1 = obj1.names();
        for (int i = 0; i < obj1.length(); i++) {
            String key1 = (String) names1.get(i);
            assertTrue(obj2.has(key1));
            equalsJson(obj1.get(key1), obj2.get(key1));
        }
    }

    private static void equalsJson(JSONArray arr1, JSONArray arr2) throws Exception {
        assertEquals(arr1.length(), arr2.length());
        for (int i = 0; i < arr1.length(); i++) {
            equalsJson(arr1.get(i), arr2.get(i));
        }
    }

    private static void equalsJson(Object obj1, Object obj2) throws Exception {
        if (obj1 instanceof JSONObject && obj2 instanceof JSONObject) {
            equalsJson((JSONObject) obj1, (JSONObject) obj2);
        } else if (obj1 instanceof JSONArray && obj2 instanceof JSONArray) {
            equalsJson((JSONArray) obj1, (JSONArray) obj2);
        } else {
            assertEquals(obj1, obj2);
        }
    }
}

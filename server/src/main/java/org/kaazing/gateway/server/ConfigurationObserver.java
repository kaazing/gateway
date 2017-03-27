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

package org.kaazing.gateway.server;

import static java.util.Collections.synchronizedSet;
import static java.util.Collections.unmodifiableList;
import static java.util.ServiceLoader.load;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;

import org.kaazing.gateway.server.config.june2016.LoginModuleOptionsType;

public class ConfigurationObserver implements ConfigurationExtensionApi {
    private final List<ConfigurationExtensionApi> configurationListenerSpi;

    private ConfigurationObserver(Set<ConfigurationExtensionApi> configurationListenerSpis) {
        List<ConfigurationExtensionApi> list = new ArrayList<>(configurationListenerSpis);
        this.configurationListenerSpi = unmodifiableList(list);
    }

    public static ConfigurationObserver newInstance() {
        return newInstance(load(ConfigurationExtensionApi.class));
    }

    public static ConfigurationObserver newInstance(ClassLoader loader) {
        return newInstance(load(ConfigurationExtensionApi.class, loader));
    }

    private static ConfigurationObserver newInstance(
        ServiceLoader<ConfigurationExtensionApi> configurationListenerSpis) {
        Set<ConfigurationExtensionApi> configurationListenerSpiList = synchronizedSet(new HashSet<>());
        for (ConfigurationExtensionApi configurationListenerSpi : configurationListenerSpis) {
            configurationListenerSpiList.add(configurationListenerSpi);
        }
        return new ConfigurationObserver(configurationListenerSpiList);
    }

    @Override
    public void parseCustomOptions(Map<String, Object> options, LoginModuleOptionsType rawOptions) {
        for (ConfigurationExtensionApi configurationListener : configurationListenerSpi) {
            configurationListener.parseCustomOptions(options, rawOptions);
        }
    }

}

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
package org.kaazing.gateway.security.auth.context;



import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.security.auth.login.Configuration;

import org.kaazing.gateway.security.LoginContextFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A static class that manages access to an instantiation of login context factories.
 *
 */
public final class LoginContextFactories {

    private LoginContextFactories() {
    }

    /**
     * Cache login factories on a per realm/configuration basis.
     */
    private static Map<String, LoginContextFactory> CACHE =
            new ConcurrentHashMap<>();

    public static LoginContextFactory create(String name, Configuration configuration) {
        LoginContextFactory defaultProvider;
        String key = name + (configuration != null ? configuration.hashCode() : "null");
        if (CACHE.containsKey(key)) {
            defaultProvider = CACHE.get(key);
        } else {
            defaultProvider = new DefaultLoginContextFactory(name, configuration);
            CACHE.put(key, defaultProvider);
        }
        return defaultProvider;
    }

    private static Logger LOG = LoggerFactory.getLogger("login.context");

    static Logger getLogger() {
        return LOG;
    }

}


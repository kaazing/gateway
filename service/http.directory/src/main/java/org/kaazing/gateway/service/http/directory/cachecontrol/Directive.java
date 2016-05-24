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
package org.kaazing.gateway.service.http.directory.cachecontrol;

import java.util.HashMap;
import java.util.Map;

/**
 * Interface for Cache-Control directives
 */
public enum Directive {

    MAX_AGE("max-age"),
    MAX_AGE_MPLUS("max-age"),

    NO_CACHE("no-cache"),
    NO_STORE("no-store"),
    NO_TRANSFORM("no-transform"),

    S_MAX_AGE("s-maxage"),
    MUST_REVALIDATE("must-revalidate"),
    PUBLIC("public"),
    PRIVATE("private"),
    PROXY_REVALIDATE("proxy-revalidate");

    private String name;

    private static final Map<String, Directive> lookup = new HashMap<>();

    static {
        for (Directive entry : Directive.values()) {
            lookup.put(entry.getName(), entry);
        }
    }

    Directive(String name) {
        this.name = name;
    }

    /**
     * Returns the name of the Cache-Control directive
     * @return name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the Directive associated with the given name
     * @param name
     * @return the Directive associated with the given name or null if there is nothing associated
     */
    public static Directive get(String name) {
        return lookup.get(name);
    }
}

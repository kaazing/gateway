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
package org.kaazing.gateway.server.test.config;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import org.kaazing.gateway.server.test.config.SuppressibleConfiguration.Suppression;

public class Suppressible<T> {
    private final T value;
    private final Set<Suppression> suppressions;

    public Suppressible(T value, Suppression... suppressions) {
        this.suppressions = EnumSet.noneOf(Suppression.class);
        this.value = value;
        if (suppressions.length == 0) {
            this.suppressions.add(Suppression.NONE);
        } else {
            Collections.addAll(this.suppressions, suppressions);
        }
    }

    public Suppressible(T value, Set<Suppression> suppressions) {
        this.value = value;
        this.suppressions = suppressions;
    }

    @Override
    public String toString() {
        return value.toString();
    }

    public Set<Suppression> getSuppressions() {
        return suppressions;
    }

    public T value() {
        return value;
    }
}

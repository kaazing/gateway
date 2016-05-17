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

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.EnumSet;
import java.util.Set;
import org.kaazing.gateway.server.test.config.Configuration;
import org.kaazing.gateway.server.test.config.SuppressibleConfiguration.Suppression;

public abstract class AbstractConfigurationBuilder<C extends Configuration<?>, R> {

    protected final C configuration;
    protected final R result;
    protected final Set<Suppression> defaultSuppressions;
    protected final Deque<Set<Suppression>> currentSuppressions = new ArrayDeque<>();

    public R done() {
        return result;
    }

    protected AbstractConfigurationBuilder(C configuration, R result, Set<Suppression> defaultSuppressions) {
        this.configuration = configuration;
        this.result = result;
        this.defaultSuppressions = defaultSuppressions;
        configuration.getSuppressibleConfiguration().setSuppression(defaultSuppressions);
    }

    public Set<Suppression> getCurrentSuppressions() {
        if (currentSuppressions.isEmpty()) {
            return defaultSuppressions;
        }
        return currentSuppressions.peek();
    }

    protected void unresolvedSuppress(Set<UnresolvedSuppression> suppressions) {
        Set<Suppression> newSuppressions = EnumSet.noneOf(Suppression.class);
        for (UnresolvedSuppression suppression : suppressions) {
            switch (suppression) {
                case INHERITED:
                    currentSuppressions.clear();
                    newSuppressions.clear();
                    break;
                case NONE:
                    newSuppressions.add(Suppression.NONE);
                    break;
                case UNIFIED:
                    newSuppressions.add(Suppression.UNIFIED);
                    break;
            }
        }
        if (newSuppressions.isEmpty()) {
            return;
        }
        currentSuppressions.push(newSuppressions);
    }

    public abstract AbstractConfigurationBuilder<?, ?> suppress(Suppression... suppressions);

    protected void addCurrentSuppressions(Suppression... suppressions) {
        // The last suppression is the most significant
        Set<Suppression> newSuppressions = EnumSet.noneOf(Suppression.class);
        for (int i = suppressions.length; i > 0;) {
            Suppression currentSuppression = suppressions[--i];
            if (currentSuppression == Suppression.NONE) {
                newSuppressions.add(Suppression.NONE);
                currentSuppressions.push(newSuppressions);
                return;
            } else if (currentSuppression == Suppression.UNIFIED) {
                newSuppressions.add(Suppression.UNIFIED);
                currentSuppressions.push(newSuppressions);
                return;
            }
        }
        return;
    }
}

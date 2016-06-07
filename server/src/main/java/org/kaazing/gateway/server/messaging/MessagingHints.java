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
package org.kaazing.gateway.server.messaging;

import java.util.HashMap;
import java.util.Map;

public class MessagingHints {

    private Map<Class<? extends MessagingHint>, MessagingHint> hints;

    public MessagingHints(MessagingHint... hints) {
        this.hints = new HashMap<>();
        for (MessagingHint hint : hints) {
            this.hints.put(hint.getClass(), hint);
        }
    }

    @SuppressWarnings("unchecked")
    public <T> T getHint(Class<T> type) {
        return (T) hints.get(type);
    }

    public <T> T getHint(Class<T> type, T def) {
        T hint = getHint(type);
        return hint == null ? def : hint;
    }
}

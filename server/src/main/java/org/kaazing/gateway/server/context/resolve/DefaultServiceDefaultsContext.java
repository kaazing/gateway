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
package org.kaazing.gateway.server.context.resolve;

import java.util.Map;

import org.kaazing.gateway.server.context.ServiceDefaultsContext;
import org.kaazing.gateway.service.AcceptOptionsContext;
import org.kaazing.gateway.service.ConnectOptionsContext;

public class DefaultServiceDefaultsContext implements ServiceDefaultsContext {

    private final AcceptOptionsContext acceptOptionsContext;
    private final ConnectOptionsContext connectOptionsContext;
    private final Map<String, String> mimeMappings;

    public DefaultServiceDefaultsContext(AcceptOptionsContext acceptOptionsContext,
                                         ConnectOptionsContext connectOptionsContext,
                                         Map<String, String> mimeMappings) {
        this.acceptOptionsContext = acceptOptionsContext;
        this.connectOptionsContext = connectOptionsContext;
        this.mimeMappings = mimeMappings;
    }

    @Override
    public AcceptOptionsContext getAcceptOptionsContext() {
        return acceptOptionsContext;
    }

    @Override
    public ConnectOptionsContext getConnectOptionsContext() {
        return connectOptionsContext;
    }

    @Override
    public Map<String, String> getMimeMappings() {
        return mimeMappings;
    }
}

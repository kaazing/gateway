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
package org.kaazing.gateway.server.impl;

import org.kaazing.gateway.server.Gateway;

public interface GatewayCreator {
    // Decorate an instance of Gateway with additional functionality during creation process.
    // If the gateway parameter is null, instatiate a Gateway.
    Gateway createGateway(Gateway gateway);

    // Configure a Gateway instance with any properties required for correct operation.
    void configureGateway(Gateway gateway);
}

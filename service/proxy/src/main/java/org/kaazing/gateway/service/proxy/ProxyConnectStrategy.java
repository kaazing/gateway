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
package org.kaazing.gateway.service.proxy;

public final class ProxyConnectStrategy {

    static enum Strategy { PREPARED, IMMEDIATE, DEFERRED }

    private final Strategy strategy;
    private final int connectionCount;

    private ProxyConnectStrategy(
        Strategy strategy,
        int connectionCount)
    {
        this.strategy = strategy;
        this.connectionCount = connectionCount;
    }

    public Strategy getStrategy()
    {
        return strategy;
    }

    public int getConnectionCount()
    {
        return connectionCount;
    }

    @Override
    public String toString() {

        final String strategyName = strategy.name().toLowerCase();

        switch (strategy) {
        case PREPARED:
            return String.format("%s (%d)", strategyName, connectionCount);
        default:
            return strategyName;
        }
    }

    public static ProxyConnectStrategy newInstance(Strategy strategy, int connectionCount, int maxConnectionCount) {
        switch (strategy) {
        case PREPARED:
            if (connectionCount == 0) {
                connectionCount = maxConnectionCount;
            }
            break;
        case IMMEDIATE:
        case DEFERRED:
            if (connectionCount > 0) {
                throw new IllegalArgumentException(String.format("Must not prepare connections for connect strategy: %s", strategy));
            }
            break;
        default:
            throw new IllegalArgumentException(String.format("Unexpected value for connect strategy: %s", strategy));
        }

        return new ProxyConnectStrategy(strategy, connectionCount);
    }
}

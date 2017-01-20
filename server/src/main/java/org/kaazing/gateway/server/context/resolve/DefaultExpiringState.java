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

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.kaazing.gateway.server.spi.security.ExpiringState;

import com.hazelcast.core.IMap;

final class DefaultExpiringState implements ExpiringState {
    private final Supplier<IMap<Object, Object>> supplier;
    private IMap<Object, Object> delegate;

    DefaultExpiringState(Supplier<IMap<Object, Object>> supplier) {
        this.supplier = supplier;
    }

    @Override
    public Object putIfAbsent(String key, Object value, long ttl, TimeUnit timeunit) {
        return delegate().putIfAbsent(key, value, ttl, timeunit);
    }

    @Override
    public Object get(String key) {
        return delegate().get(key);
    }

    @Override
    public Object remove(String key, Object value) {
        return delegate().remove(key, value);
    }

	private IMap<Object, Object> delegate() {
		if (delegate == null) {
			delegate = supplier.get();
		}
		return delegate;
	}
}



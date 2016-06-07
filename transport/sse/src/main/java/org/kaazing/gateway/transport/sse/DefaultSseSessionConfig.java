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
package org.kaazing.gateway.transport.sse;

import org.kaazing.mina.core.session.AbstractIoSessionConfigEx;
import org.kaazing.mina.core.session.IoSessionConfigEx;

public class DefaultSseSessionConfig extends AbstractIoSessionConfigEx implements SseSessionConfig {

	private int retry = 3000;
	private boolean reconnecting;
	private String lastId;
	
    protected DefaultSseSessionConfig() {
        // Do nothing
    }

    /**
     * {@inheritDoc}
     */
    @Override
	protected final void doSetAll(IoSessionConfigEx config) {
    	SseSessionConfig sseConfig = (SseSessionConfig)config;
    	setRetry(sseConfig.getRetry());
    	setReconnecting(sseConfig.isReconnecting());
    	setLastId(sseConfig.getLastId());
    }

    @Override
	public void setRetry(int retry) {
		this.retry = retry;
	}

    @Override
	public int getRetry() {
		return retry;
	}

    @Override
	public void setReconnecting(boolean reconnecting) {
		this.reconnecting = reconnecting;
	}

    @Override
	public boolean isReconnecting() {
		return reconnecting;
	}

    @Override
	public void setLastId(String lastId) {
		this.lastId = lastId;
	}

    @Override
	public String getLastId() {
		return lastId;
	}
}

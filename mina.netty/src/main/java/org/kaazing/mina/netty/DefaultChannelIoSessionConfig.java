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
package org.kaazing.mina.netty;

import org.jboss.netty.channel.ChannelConfig;
import org.jboss.netty.channel.DefaultChannelConfig;

import org.kaazing.mina.core.session.AbstractIoSessionConfigEx;
import org.kaazing.mina.core.session.IoSessionConfigEx;

public class DefaultChannelIoSessionConfig extends ChannelIoSessionConfig<ChannelConfig> {
    // Push Mina default config settings into the channelConfig
    private static final IoSessionConfigEx DEFAULT = new AbstractIoSessionConfigEx() {
        @Override
        protected void doSetAll(IoSessionConfigEx config) {
        }
    };

    public DefaultChannelIoSessionConfig() {
        this(new DefaultChannelConfig());
    }

    public DefaultChannelIoSessionConfig(ChannelConfig channelConfig) {
        super(channelConfig, DEFAULT);
    }
}

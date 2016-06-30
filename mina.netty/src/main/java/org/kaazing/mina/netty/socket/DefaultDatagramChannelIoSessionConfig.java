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
package org.kaazing.mina.netty.socket;

import java.net.DatagramSocket;
import java.net.SocketException;

import org.apache.mina.transport.socket.DatagramSessionConfigEx;
import org.apache.mina.transport.socket.DefaultDatagramSessionConfigEx;
import org.jboss.netty.channel.socket.DefaultDatagramChannelConfig;

// TODO: create NIO-specific subclasses and remove this class (like DefaultSocketChannelIoSessionConfig)
public class DefaultDatagramChannelIoSessionConfig extends DatagramChannelIoSessionConfig {

    // Push Mina default config settings into the channelConfig
    private static final DatagramSessionConfigEx DEFAULT = new DefaultDatagramSessionConfigEx();

    public DefaultDatagramChannelIoSessionConfig() {
        super(new DefaultDatagramChannelConfig(newDatagramSocket()), DEFAULT);
    }

    private static DatagramSocket newDatagramSocket() {
        DatagramSocket result;
        try {
            result = new DatagramSocket();
        }
        catch (SocketException e) {
            throw new RuntimeException(e);
        }
        return result;
    }

}

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
package org.kaazing.gateway.transport.ws;

import static org.junit.Assert.assertEquals;

import org.junit.Test;


public class WsCommandMessageTest {

    @Test
    public void toStringShouldListMultipleCommands() {
        WsCommandMessage message = new WsCommandMessage(Command.close(), Command.reconnect());
        String result = message.toString();
        assertEquals("COMMAND: [CLOSE, RECONNECT]", result);
    }

    @Test
    public void toStringShouldListSingleCommand() {
        WsCommandMessage message = new WsCommandMessage(Command.noop());
        String result = message.toString();
        assertEquals("COMMAND: [NOOP]", result);
    }

    @Test
    public void toStringShouldNotListAllCommandsWhenThereAreMany() {
        Command[] padding = new Command[1000];
        for (int i=0; i<1000; i++) {
            padding[i] = Command.noop();
        }
        WsCommandMessage message = new WsCommandMessage(padding);
        String result = message.toString();
        assertEquals("COMMAND: [NOOP, NOOP, NOOP]...", result);
    }

}

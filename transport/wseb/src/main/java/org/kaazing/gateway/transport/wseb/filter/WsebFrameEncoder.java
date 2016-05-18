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
package org.kaazing.gateway.transport.wseb.filter;

import static org.kaazing.gateway.transport.bridge.CachingMessageEncoder.IO_MESSAGE_ENCODER;

import java.nio.ByteBuffer;

import org.kaazing.gateway.transport.bridge.CachingMessageEncoder;
import org.kaazing.gateway.transport.http.bridge.filter.HttpGzipEncoder;
import org.kaazing.gateway.transport.ws.Command;
import org.kaazing.gateway.transport.ws.WsCloseMessage;
import org.kaazing.gateway.transport.ws.WsCommandMessage;
import org.kaazing.gateway.transport.ws.WsMessage;
import org.kaazing.gateway.transport.ws.WsPingMessage;
import org.kaazing.gateway.transport.ws.WsPongMessage;
import org.kaazing.gateway.transport.ws.bridge.filter.AbstractWsFrameEncoder;
import org.kaazing.gateway.transport.ws.bridge.filter.WsDraftHixieFrameEncodingSupport;
import org.kaazing.mina.core.buffer.IoBufferAllocatorEx;
import org.kaazing.mina.core.buffer.IoBufferEx;

public class WsebFrameEncoder extends AbstractWsFrameEncoder {

    private static final byte[] TO_HEX = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};

    private static final byte COMMAND_TYPE_BYTE  = (byte)0x01;
    private static final byte[] EMPTY_PING_BYTES = new byte[]{(byte)0x89, (byte)0x00};
    private static final byte[] EMPTY_PONG_BYTES = new byte[]{(byte)0x8A, (byte)0x00};

	/* Block Padding is used in cases where we need to guarantee that at least 4K of data arrives
	 * to ensure timely delivery in buffering clients (e.g. Android)
	 */
	public static final WsMessage BLOCK_PADDING_MESSAGE;
	static {
    	// each command uses 2 bytes on the wire so we need half as many commands as padding bytes
    	int len = 4096 / 2 - 2;
    	Command[] commands = new Command[len];
    	for (int i = 0; i < len; i++) {
    		commands[i] = Command.noop();
    	}
        BLOCK_PADDING_MESSAGE = new WsCommandMessage(commands);
	}


    public WsebFrameEncoder(IoBufferAllocatorEx<?> allocator) {
        this(IO_MESSAGE_ENCODER, allocator);
    }

    public WsebFrameEncoder(CachingMessageEncoder cachingEncoder, IoBufferAllocatorEx<?> allocator) {
        super(cachingEncoder, allocator);
    }

    @Override
    protected IoBufferEx doEncode(IoBufferAllocatorEx<?> allocator, int flags, WsMessage message) {
        switch (message.getKind()) {
            case BINARY: {
                return doBinaryEncode(allocator, flags, message);
            }
            case TEXT: {
                return doTextEncode(allocator, flags, message);
            }
            case COMMAND: {
                return doCommandEncode(allocator, flags, message);
            }
            case PING: {
                return doPingEncode(allocator, flags, message);
            }
            case PONG: {
                return doPongEncode(allocator, flags, message);
            }
            default:
                throw new IllegalStateException("Unrecognized frame type: " + message.getKind());
        }
    }

    private IoBufferEx doCommandEncode(IoBufferAllocatorEx<?> allocator, int flags, WsMessage message) {

        // check for special block padding message
        if (message == BLOCK_PADDING_MESSAGE) {
            // wrap using allocator (OK) since we need an exact == match in HttpGzipEncoder later
            return allocator.wrap(ByteBuffer.wrap(HttpGzipEncoder.WSEB_4K_PADDING_BYTES), flags);
        }

        WsCommandMessage wsebCommand = (WsCommandMessage)message;
        Command[] commands = wsebCommand.getCommands();
        ByteBuffer text = allocator.allocate(2 * commands.length + 2, flags);
        int offset = text.position();
        text.put(COMMAND_TYPE_BYTE);
        for (Command command : commands) {
            int value = command.value();
            text.put(TO_HEX[(value >> 4) & 0x000F]);
            text.put(TO_HEX[value & 0x000F]);
        }
        text.put(TEXT_TERMINATOR_BYTE);
        text.flip();
        text.position(offset);
        return allocator.wrap(text, flags);
    }

    protected IoBufferEx doPingEncode(IoBufferAllocatorEx<?> allocator, int flags, WsMessage message) {
        WsPingMessage ping = (WsPingMessage)message;
        assert ping.getBytes().remaining() == 0 : "PING with payload not supported";
        ByteBuffer text = allocator.allocate(EMPTY_PING_BYTES.length, flags);
        int offset = text.position();
        text.put(EMPTY_PING_BYTES);
        text.flip();
        text.position(offset);
        return allocator.wrap(text, flags);
    }

    protected IoBufferEx doPongEncode(IoBufferAllocatorEx<?> allocator, int flags, WsMessage message) {
        WsPongMessage ping = (WsPongMessage)message;
        assert ping.getBytes().remaining() == 0 : "PONG with payload not supported";
        ByteBuffer text = allocator.allocate(EMPTY_PONG_BYTES.length, flags);
        int offset = text.position();
        text.put(EMPTY_PONG_BYTES);
        text.flip();
        text.position(offset);
        return allocator.wrap(text, flags);
    }

    @Override
    protected IoBufferEx doTextEncode(IoBufferAllocatorEx<?> allocator, int flags, WsMessage message) {
        return WsDraftHixieFrameEncodingSupport.doSpecifiedLengthTextEncode(allocator, flags, message);
    }

    @Override
    protected IoBufferEx doContinuationEncode(IoBufferAllocatorEx<?> ioBufferAllocatorEx, int i, WsMessage wsMessage) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected IoBufferEx doBinaryEncode(IoBufferAllocatorEx<?> allocator, int flags, WsMessage message) {
        return WsDraftHixieFrameEncodingSupport.doBinaryEncode(allocator, flags, message);
    }

    @Override
    protected IoBufferEx doCloseEncode(IoBufferAllocatorEx<?> allocator, int flags, WsCloseMessage message) {
        return WsDraftHixieFrameEncodingSupport.doCloseEncode(allocator, flags);
    }

}

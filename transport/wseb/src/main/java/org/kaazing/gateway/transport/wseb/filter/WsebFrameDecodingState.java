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

import static java.lang.String.format;

import java.util.Iterator;
import java.util.List;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.filter.codec.ProtocolDecoderException;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;
import org.apache.mina.filter.codec.statemachine.DecodingState;
import org.apache.mina.filter.codec.statemachine.SingleByteDecodingState;
import org.kaazing.gateway.transport.DecodingStateMachine;
import org.kaazing.gateway.transport.ws.Command;
import org.kaazing.gateway.transport.ws.WsBinaryMessage;
import org.kaazing.gateway.transport.ws.WsCommandMessage;
import org.kaazing.gateway.transport.ws.WsPingMessage;
import org.kaazing.gateway.transport.ws.WsPongMessage;
import org.kaazing.gateway.transport.ws.WsTextMessage;
import org.kaazing.gateway.util.ErrorHandler;
import org.kaazing.gateway.util.Utf8Util;
import org.kaazing.mina.core.buffer.IoBufferAllocatorEx;
import org.kaazing.mina.core.buffer.IoBufferEx;
import org.kaazing.mina.filter.codec.statemachine.ConsumeToTerminatorDecodingState;
import org.kaazing.mina.filter.codec.statemachine.FixedLengthDecodingState;

public class WsebFrameDecodingState extends DecodingStateMachine {

    private static final byte TEXT_TYPE_BYTE = (byte)0x00;
    private static final byte COMMAND_TYPE_BYTE = (byte)0x01;
    private static final byte BINARY_TYPE_BYTE = (byte)0x80;
    private static final byte SPECIFIED_LENGTH_TEXT_TYPE_BYTE = (byte)0x81;

    private static final byte PING_TYPE_BYTE = (byte)0x89;
    private static final byte PONG_TYPE_BYTE = (byte)0x8A;

    private static final byte ZERO_BYTE = (byte)'0';
    private static final byte ONE_BYTE =  (byte)'1';
    private static final byte TWO_BYTE =  (byte)'2';

    private final int maxDataSize;
    private final boolean pingEnabled;

    private final DecodingState READ_FRAME_TYPE = new SingleByteDecodingState() {

        @Override
        protected DecodingState finishDecode(byte frameType, ProtocolDecoderOutput out) throws Exception {
            switch (frameType) {
            case TEXT_TYPE_BYTE:
                return READ_TEXT_FRAME;
            case COMMAND_TYPE_BYTE:
                return READ_COMMAND_FRAME;
            case BINARY_TYPE_BYTE:
                return READ_BINARY_FRAME_LENGTH;
            case SPECIFIED_LENGTH_TEXT_TYPE_BYTE:
                return READ_SPECIFIED_LENGTH_TEXT_FRAME_LENGTH;
            case PING_TYPE_BYTE:
                if (!pingEnabled) {
                    throw new ProtocolDecoderException("ping and pong commands are not enabled");
                }
                return READ_PING_FRAME;
            case PONG_TYPE_BYTE:
                if (!pingEnabled) {
                    throw new ProtocolDecoderException("ping and pong commands are not enabled");
                }
                return READ_PONG_FRAME;
            default:
                throw new ProtocolDecoderException("Unexpected frame type: " + Integer.toHexString((frameType & 0xff)));
            }
        }

        @Override
        public DecodingState finishDecode(ProtocolDecoderOutput out) throws Exception {
            // end-of-session while awaiting next frame is valid
            return null;
        }
    };

    private final DecodingState READ_TEXT_FRAME = new ConsumeToTerminatorDecodingState(allocator, (byte) 0xff) {
        private boolean finished = false;
        private int messageSizeSoFar = 0;

        @Override
        // Make sure we fail before reading the entire text frame if the size limit is exceeded
        public DecodingState decode(IoBuffer in, ProtocolDecoderOutput out) throws Exception {
            finished = false;
            int startRemaining = in.remaining();
            DecodingState ret = super.decode(in, out);
            // if super.decode already called finishDecode we mustn't check twice
            if (!finished) {
                // Make sure we fail as soon as the limit is exceeded
                messageSizeSoFar += (startRemaining - in.remaining());
                checkSizeLimit(messageSizeSoFar);
            }
            return ret;
        }

        @Override
        protected DecodingState finishDecode(IoBuffer buffer, ProtocolDecoderOutput out) throws Exception {
            // buffer contains the whole text frame
            messageSizeSoFar = 0;
            finished = true;
            checkSizeLimit(buffer.remaining());
            validateUTF8(buffer);
            out.write(new WsTextMessage((IoBufferEx) buffer));

            return READ_FRAME_TYPE;
        }
    };

    private final DecodingState READ_COMMAND_FRAME = new ConsumeToTerminatorDecodingState(allocator, (byte)0xff) {
        @Override
        protected DecodingState finishDecode(IoBuffer buffer, ProtocolDecoderOutput out) throws Exception {
            // read the command frame contents
            int nbCommands =  buffer.remaining() / 2;
            Command[] commands = new Command[nbCommands];
            for (int i=0; i<nbCommands; i++) {
                commands[i] = readCommand((IoBufferEx)buffer);
            }
            out.write(new WsCommandMessage(commands));
            return READ_FRAME_TYPE;
        }
    };

    private final DecodingState READ_BINARY_FRAME_LENGTH = new DecodingState() {

        private int dynamicFrameSize;

        /**
         * {@inheritDoc}
         */
        @Override
        public DecodingState decode(IoBuffer in, ProtocolDecoderOutput out) throws Exception {

            while (in.hasRemaining()) {
                byte b = in.get();
                dynamicFrameSize <<= 7;
                dynamicFrameSize |= (b & 0x7f);
                if ((b & 0x80) == 0x00) {
                    int frameSize = dynamicFrameSize;
                    dynamicFrameSize = 0;
                    return finishDecode(frameSize, out);
                }
            }

            return this;
        }

        @Override
        public DecodingState finishDecode(ProtocolDecoderOutput out) throws Exception {
            throw new ProtocolDecoderException("Unexpected end of session while waiting for frame length.");
        }

        protected DecodingState finishDecode(final int frameSize, ProtocolDecoderOutput out) throws Exception {
            if (frameSize == 0) {
                out.write(new WsBinaryMessage(allocator.wrap(allocator.allocate(0))));
                return READ_FRAME_TYPE;
            }
            else {
                checkSizeLimit(frameSize);
                return new FixedLengthDecodingState(allocator, frameSize) {
                    @Override
                    protected DecodingState finishDecode(IoBuffer product, ProtocolDecoderOutput out) throws Exception {
                        // read the binary frame contents
                        out.write(new WsBinaryMessage((IoBufferEx) product));
                        return READ_FRAME_TYPE;
                    }
                };
            }
        }
    };

    private final DecodingState READ_PING_FRAME =  new SingleByteDecodingState() {
        @Override
        protected DecodingState finishDecode(byte lengthByte1, ProtocolDecoderOutput out) throws Exception {
            if ( lengthByte1 != (byte)0 ) {
                throw new ProtocolDecoderException("Ping frame length must be 0, but got: " + Integer.toHexString((lengthByte1 & 0xff)));
            }
            out.write(new WsPingMessage(allocator.wrap(allocator.allocate(0))));
            return READ_FRAME_TYPE;
        }
    };

    private final DecodingState READ_PONG_FRAME =  new SingleByteDecodingState() {
        @Override
        protected DecodingState finishDecode(byte lengthByte1, ProtocolDecoderOutput out) throws Exception {
            if ( lengthByte1 != (byte)0 ) {
                throw new ProtocolDecoderException("Pong frame length must be 0, but got: " + Integer.toHexString((lengthByte1 & 0xff)));
            }
            out.write(new WsPongMessage(allocator.wrap(allocator.allocate(0))));
            return READ_FRAME_TYPE;
        }
    };

    private final DecodingState READ_SPECIFIED_LENGTH_TEXT_FRAME_LENGTH = new DecodingState() {

        private int dynamicFrameSize;

        /**
         * {@inheritDoc}
         */
        @Override
        public DecodingState decode(IoBuffer in, ProtocolDecoderOutput out) throws Exception {

            while (in.hasRemaining()) {
                byte b = in.get();
                dynamicFrameSize <<= 7;
                dynamicFrameSize |= (b & 0x7f);
                if ((b & 0x80) == 0x00) {
                    int frameSize = dynamicFrameSize;
                    dynamicFrameSize = 0;
                    return finishDecode(frameSize, out);
                }
            }

            return this;
        }

        @Override
        public DecodingState finishDecode(ProtocolDecoderOutput out) throws Exception {
            throw new ProtocolDecoderException("Unexpected end of session while waiting for frame length.");
        }

        protected DecodingState finishDecode(final int frameSize, ProtocolDecoderOutput out) throws Exception {
            if (frameSize == 0) {
                out.write(new WsTextMessage(allocator.wrap(allocator.allocate(0))));
                return READ_FRAME_TYPE;
            }
            else {
                checkSizeLimit(frameSize);
                return new FixedLengthDecodingState(allocator, frameSize) {
                    @Override
                    protected DecodingState finishDecode(IoBuffer product, ProtocolDecoderOutput out) throws Exception {
                        validateUTF8(product);
                        // read the text frame contents
                        out.write(new WsTextMessage((IoBufferEx) product));
                        return READ_FRAME_TYPE;
                    }
                };
            }
        }
    };

    WsebFrameDecodingState(IoBufferAllocatorEx<?> allocator, int maxDataSize, boolean pingEnabled) {
        super(allocator);
        this.maxDataSize = maxDataSize;
        this.pingEnabled = pingEnabled;
    }

    @Override
    protected DecodingState init() throws Exception {
        return READ_FRAME_TYPE;
    }

    @Override
    public DecodingState decode(IoBuffer in, ProtocolDecoderOutput out) throws Exception {
        DecodingState decodingState = super.decode(in, out);
        flush(childProducts, out);
        return decodingState;
    }

    @Override
    protected DecodingState finishDecode(List<Object> childProducts, ProtocolDecoderOutput out) throws Exception {
        flush(childProducts, out);
        return null;
    }

    private void flush(List<Object> childProducts, ProtocolDecoderOutput out) {
        // flush child products to parent output before decode is complete
        for (Iterator<Object> i = childProducts.iterator(); i.hasNext();) {
            Object product = i.next();
            i.remove();
            out.write(product);
        }
    }

    private void checkSizeLimit(int sizeSoFar) throws ProtocolDecoderException {
        if (maxDataSize > 0 && sizeSoFar > maxDataSize) {
            throw new ProtocolDecoderException("incoming message size exceeds permitted maximum of " + maxDataSize + " bytes");
        }
    }

    private void validateUTF8(IoBuffer buffer) throws ProtocolDecoderException {
        final StringBuffer error = new StringBuffer("WebSocket text frame content is not valid UTF-8: ");
        int result = Utf8Util.validateUTF8(buffer.buf(), buffer.position(), buffer.remaining(), new ErrorHandler() {

            @Override
            public void handleError(String message) {
                error.append(message);

            }
        });
        if (result != 0) {
            if (result > 0) {
                error.append(format("final character is incomplete, missing %d bytes", result));
            }
            throw new ProtocolDecoderException(error.toString());
        }
    }

    @Override
    protected void destroy() throws Exception {
    }

    public static Command readCommand(IoBufferEx buf) {
        switch (buf.get()) {
        case ZERO_BYTE:
            switch (buf.get()) {
            case ZERO_BYTE:
                return Command.noop();
            case ONE_BYTE:
                return Command.reconnect();
            case TWO_BYTE:
                return Command.close();
            default:
                throw new IllegalArgumentException("Unrecognized command: " + buf.getHexDump());
            }
        default:
            throw new IllegalArgumentException("Unrecognized command: " + buf.getHexDump());
        }
    }

}

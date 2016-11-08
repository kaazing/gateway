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
package org.kaazing.gateway.transport.ws.bridge.filter;

import static org.kaazing.gateway.transport.ws.WsMessage.Kind.BINARY;
import static org.kaazing.gateway.transport.ws.WsMessage.Kind.CONTINUATION;
import static org.kaazing.gateway.transport.ws.WsMessage.Kind.TEXT;

import java.nio.ByteBuffer;

import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolDecoderException;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;
import org.kaazing.gateway.transport.ws.WsBinaryMessage;
import org.kaazing.gateway.transport.ws.WsCloseMessage;
import org.kaazing.gateway.transport.ws.WsContinuationMessage;
import org.kaazing.gateway.transport.ws.WsMessage.Kind;
import org.kaazing.gateway.transport.ws.WsPingMessage;
import org.kaazing.gateway.transport.ws.WsPongMessage;
import org.kaazing.gateway.transport.ws.WsTextMessage;
import org.kaazing.gateway.transport.ws.bridge.filter.WsFrameEncodingSupport.Opcode;
import org.kaazing.gateway.transport.ws.util.WSMessageTooLongException;
import org.kaazing.mina.core.buffer.IoBufferAllocatorEx;
import org.kaazing.mina.core.buffer.IoBufferEx;
import org.kaazing.mina.filter.codec.CumulativeProtocolDecoderEx;

public class WsFrameDecoder extends CumulativeProtocolDecoderEx {

    private final int maxMessageSize;
    private BinaryTextMessageDecoder binaryTextDecoder = DEFAULT_BINARY_TEXT_DECODER;
    private boolean prevDataFin = true;
    private long currentMessageSize;           // accumulates frame sizes of a message
    private final boolean maskingExpected;

    WsFrameDecoder(IoBufferAllocatorEx<?> allocator, int maxMessageSize, boolean maskingExpected) {
        super(allocator);
        this.maxMessageSize = maxMessageSize;
        this.maskingExpected = maskingExpected;
    }

    @Override
    protected boolean doDecode(IoSession session, IoBufferEx in, ProtocolDecoderOutput out) throws Exception {
        if (in.remaining() < 2) {
            return false;
        }

        in.mark();

        byte opcodeByte = in.get();
        validateRSV(opcodeByte);

        int i = (opcodeByte & 0x0f);
        Opcode opcode;

        try {
            opcode = Opcode.valueOf(i);
        }
        catch (IllegalArgumentException e) {
            throw new ProtocolDecoderException("Unrecognized WebSocket frame opcode: " + i + " on session " + session.getLocalAddress(), e);
        }

        // FIN bit validation for opcode
        boolean fin = (opcodeByte & 0x80) != 0;
        validateOpcodeUsingFin(opcode, fin);

        byte maskAndPayloadLenByte = in.get();
        boolean masked = (maskAndPayloadLenByte & 0x80) != 0;
        validateMaskingAllowed(masked);
        int payloadLenByte = maskAndPayloadLenByte & 0x7f;

        // calculate actual payload length by checking if there is
        // extended payload length
        long frameSize = 0;

        if (payloadLenByte < 126) {
            frameSize = payloadLenByte;
        }
        else if (payloadLenByte == 126) {
            if (in.remaining() < 2) {
                in.reset();
                return false;
            }
            frameSize = in.getUnsignedShort();
        }
        else if (payloadLenByte == 127){
            if (in.remaining() < 8) {
                in.reset();
                return false;
            }
            frameSize = in.getLong();
        }

        validateFrameSize(frameSize, opcode);

        long currentMessageSizeCandidate = frameSize;

        if (opcode == Opcode.CONTINUATION) {
            currentMessageSizeCandidate += currentMessageSize;
        }

        if (opcode == Opcode.CONTINUATION || opcode == Opcode.TEXT || opcode == Opcode.BINARY) {
            validateMessageSize(currentMessageSizeCandidate);
        }

        // actual payload length plus additional 4 bytes if masked
        long totalRemainingBytesNeeded = (masked ? 4 : 0) + frameSize;
        if (in.remaining() < totalRemainingBytesNeeded) {
            in.reset();
            return false;
        }

        int mask = masked ? in.getInt() : 0;

        IoBufferEx buf = in.getSlice((int) frameSize);
        if (mask != 0) {
            unmask(buf.buf(), mask);
        }

        switch (opcode) {
            case CONTINUATION:
                binaryTextDecoder.decodeWsMessage(buf, CONTINUATION, fin, out);
                prevDataFin = fin;
                currentMessageSize = currentMessageSizeCandidate;
                break;
            case BINARY:
                binaryTextDecoder.decodeWsMessage(buf, BINARY, fin, out);
                prevDataFin = fin;
                currentMessageSize = currentMessageSizeCandidate;
                break;
            case TEXT:
                binaryTextDecoder.decodeWsMessage(buf, TEXT, fin, out);
                prevDataFin = fin;
                currentMessageSize = currentMessageSizeCandidate;
                break;
            case PING:
                out.write(new WsPingMessage(buf));
                break;
            case PONG:
                out.write(new WsPongMessage(buf));
                break;
            case CLOSE:
                WsCloseMessage close;
                if (buf.hasRemaining()) {
                    int status = buf.getUnsignedShort();
                    validateWireCloseCode(status);
                    close = new WsCloseMessage(status, buf.buf());
                } else {
                    close = new WsCloseMessage();
                }
                out.write(close);
                break;
            default:
                throw new ProtocolDecoderException("Unknown WebSocket opcode: " + opcode  + " on session " + session.getLocalAddress());
        }
        return true;
    }

    // Validates opcode w.r.t FIN bit
    private void validateOpcodeUsingFin(Opcode opcode, boolean fin) throws ProtocolDecoderException {
        switch (opcode) {
            case CONTINUATION:
                if (prevDataFin) {
                    throw new ProtocolDecoderException("Not expecting CONTINUATION frame");
                }
                break;

            case TEXT:
            case BINARY:
                if (!prevDataFin) {
                    throw new ProtocolDecoderException("Expecting CONTINUATION frame, but got "+opcode+" frame");
                }
                break;

            case PING:
            case PONG:
            case CLOSE:
                if (!fin) {
                    throw new ProtocolDecoderException("Expected FIN for "+opcode+" frame");
                }
                break;
        default:
            break;
        }
    }

    // Validates RSV bits
    private void validateRSV(byte opcodeByte) throws ProtocolDecoderException {
        if ((opcodeByte & 0x70) != 0) {
            // We don't support negotiated extensions that deal with RSV bits
            if ((opcodeByte & 0x40) != 0) {
                throw new ProtocolDecoderException("RSV1 is set");
            }
            if ((opcodeByte & 0x20) != 0) {
                throw new ProtocolDecoderException("RSV2 is set");
            }
            if ((opcodeByte & 0x10) != 0) {
                throw new ProtocolDecoderException("RSV3 is set");
            }
        }
    }

    /*
     * Unmask a buffer in place
     */
    protected static void unmask(ByteBuffer buf, int mask) {
        if (!buf.hasRemaining()) {
            return;
        }

        byte b;
        int start = buf.position();
        int remainder = buf.remaining() % 4;
        int remaining = buf.remaining() - remainder;
        int end = remaining + buf.position();

        // xor a 32bit word at a time as long as possible
        while (buf.position() < end) {
            int plaintext = buf.getInt(buf.position()) ^ mask;
            buf.putInt(plaintext);
        }

        // xor the remaining 3, 2, or 1 bytes
        switch (remainder) {
        case 3:
            b = (byte) (buf.get(buf.position()) ^ ((mask >> 24) & 0xff));
            buf.put(b);
            b = (byte) (buf.get(buf.position()) ^ ((mask >> 16) & 0xff));
            buf.put(b);
            b = (byte) (buf.get(buf.position()) ^ ((mask >> 8) & 0xff));
            buf.put(b);
            break;
        case 2:
            b = (byte) (buf.get(buf.position()) ^ ((mask >> 24) & 0xff));
            buf.put(b);
            b = (byte) (buf.get(buf.position()) ^ ((mask >> 16) & 0xff));
            buf.put(b);
            break;
        case 1:
            b = (byte) (buf.get(buf.position()) ^ (mask >> 24));
            buf.put(b);
            break;
        case 0:
        default:
                break;
        }
        buf.position(start);
    }

    private void validateMessageSize(long messageSize) throws WSMessageTooLongException {
        // note: negative size indicates large unsigned value, larger than Long.MAX_VALUE
        if (maxMessageSize > 0 && (messageSize < 0 || messageSize > maxMessageSize)) {
            throw new WSMessageTooLongException(String.format("Incoming message size %d bytes exceeds permitted maximum of %d bytes", messageSize, maxMessageSize));
        }
    }


    private void validateFrameSize(long frameSize, Opcode opcode) throws ProtocolDecoderException {
        // Control frame payload size cannot be greater than 125 bytes
        if (frameSize > 125) {
            switch (opcode) {
                case PING:
                case PONG:
                case CLOSE:
                    throw new ProtocolDecoderException("Invalid "+opcode+" frame payload length = "+frameSize);
            default:
                break;
            }
        }

        // If there is data in CLOSE frame, first 2-bytes represent close status code
        if (opcode == Opcode.CLOSE && frameSize == 1) {
            throw new ProtocolDecoderException("Invalid CLOSE frame payload length = "+frameSize);
        }
    }

    private interface BinaryTextMessageDecoder {
        void decodeWsMessage(IoBufferEx payload, Kind messageKind, boolean fin, ProtocolDecoderOutput out);
    }

    private static final BinaryTextMessageDecoder DEFAULT_BINARY_TEXT_DECODER = new BinaryTextMessageDecoder() {
        @Override
        public void decodeWsMessage(IoBufferEx payload, Kind messageKind, boolean fin, ProtocolDecoderOutput out) {
            switch(messageKind) {
            case CONTINUATION:
                out.write(new WsContinuationMessage(payload, fin));
                break;
            case BINARY:
                out.write(new WsBinaryMessage(payload, fin));
                break;
            case TEXT:
                out.write(new WsTextMessage(payload, fin));
                break;
            default:
                assert false;
            }
        }
    };

    private static void validateWireCloseCode(int statusCode) throws ProtocolDecoderException {
        if (statusCode == 1005 || statusCode == 1006) {
            throw new ProtocolDecoderException("Invalid close code: " + statusCode);
        }
        try {
            WsCloseMessage.validateCloseCode(statusCode);
        } catch (IllegalArgumentException ie) {
            throw new ProtocolDecoderException("Invalid close code: " + statusCode);
        }
    }

    /**
     * Checks if masking is allowed/expected for the frame being decoded.
     */
    private void validateMaskingAllowed(boolean masked) throws ProtocolDecoderException {
        if (masked != maskingExpected) {
            throw new ProtocolDecoderException(String.format("Received unexpected %s frame", masked ? "masked" : "unmasked"));
        }
    }

}

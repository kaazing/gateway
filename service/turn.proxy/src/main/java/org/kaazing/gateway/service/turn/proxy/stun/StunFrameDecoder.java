package org.kaazing.gateway.service.turn.proxy.stun;

import static org.kaazing.gateway.service.turn.proxy.stun.StunMessage.MAGIC_COOKIE;
import static org.kaazing.gateway.service.turn.proxy.stun.StunMessage.attributePaddedLength;

import java.util.ArrayList;
import java.util.List;

import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;
import org.kaazing.gateway.service.turn.proxy.TurnProxyHandler;
import org.kaazing.gateway.service.turn.proxy.TurnSessionState;
import org.kaazing.mina.core.buffer.IoBufferAllocatorEx;
import org.kaazing.mina.core.buffer.IoBufferEx;
import org.kaazing.mina.filter.codec.CumulativeProtocolDecoderEx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StunFrameDecoder extends CumulativeProtocolDecoderEx {
    private static final Logger LOGGER = LoggerFactory.getLogger("service.turn.proxy");

    public StunFrameDecoder(IoBufferAllocatorEx<?> allocator) {
        super(allocator);
    }

    @Override
    protected boolean doDecode(IoSession session, IoBufferEx in, ProtocolDecoderOutput out) throws Exception {
        if (session.getAttribute(TurnProxyHandler.TURN_STATE_KEY) == TurnSessionState.ALLOCATED) {
            // No need to decode once allocated
            out.write(in.duplicate());
            in.position(in.limit());
            return true;
        }

        LOGGER.trace("Decoding STUN message: " + in);
        if (in.remaining() < 20) {
            return false;
        }
        in.mark();

        // https://tools.ietf.org/html/rfc5389#section-6
        short leadingBitsAndMessageType = in.getShort();

        validateIsStun(leadingBitsAndMessageType);

        StunMessageClass messageClass = StunMessageClass.valueOf(leadingBitsAndMessageType);

        StunMessageMethod method = StunMessageMethod.valueOf(leadingBitsAndMessageType);

        short messageLength = in.getShort();

        int magicCookie = in.getInt();
        validateMagicCookie(magicCookie);

        byte[] transactionId = new byte[12];
        in.get(transactionId);

        if (in.remaining() < messageLength) {
            in.reset();
            return false;
        }

        List<StunMessageAttribute> attributes = decodeAttributes(in, messageLength);

        StunMessage stunMessage = new StunMessage(messageClass, method, transactionId, attributes);
        in.mark();
        out.write(stunMessage);
        return true;
    }

    private List<StunMessageAttribute> decodeAttributes(IoBufferEx in, short remaining) {
        List<StunMessageAttribute> stunMessageAttributes = new ArrayList<>();
        // Any attribute type MAY appear more than once in a STUN message.
        // Unless specified otherwise, the order of appearance is significant:
        // only the first occurrence needs to be processed by a receiver, and
        // any duplicates MAY be ignored by a receiver.
        do {
            short type = in.getShort();
            short length = in.getShort();
            remaining -= 4;

            // get variable
            byte[] variable = new byte[length];
            in.get(variable);
            stunMessageAttributes.add(StunMessageAttribute.Factory.get(type, length, variable));
            remaining -= length;

            // remove padding
            for (int i = length; i < attributePaddedLength(length); i++) {
                in.get();
                remaining -= 1;
            }
        } while (remaining > 0);
        return stunMessageAttributes;
    }

    private void validateIsStun(short leadingBitsAndMessageType) {
        int leadingBytes = (leadingBitsAndMessageType & 0xC00);
        if (0 != leadingBytes) {
            throw new IllegalArgumentException("Illegal leading bytes in STUN message: " + leadingBytes);
        }
    }

    private void validateMagicCookie(int magicCookie) {
        if (magicCookie != MAGIC_COOKIE) {
            throw new IllegalArgumentException("Illegal magic cookie value: " + magicCookie);
        }

    }
}

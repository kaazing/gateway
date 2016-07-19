package org.kaazing.gateway.service.turn.proxy.stun;

import static org.kaazing.gateway.service.turn.proxy.stun.StunMessage.MAGIC_COOKIE;

import java.util.ArrayList;
import java.util.List;

import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;
import org.kaazing.mina.core.buffer.IoBufferAllocatorEx;
import org.kaazing.mina.core.buffer.IoBufferEx;
import org.kaazing.mina.filter.codec.CumulativeProtocolDecoderEx;

public class StunFrameDecoder extends CumulativeProtocolDecoderEx {

    public StunFrameDecoder(IoBufferAllocatorEx<?> allocator) {
        super(allocator);
    }

    @Override
    protected boolean doDecode(IoSession session, IoBufferEx in, ProtocolDecoderOutput out) throws Exception {
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

        in.mark();
        if (in.remaining() < messageLength) {
            return false;
        }

        List<StunMessageAttribute> attributes = decodeAttributes(in, messageLength);

        StunMessage stunMessage = new StunMessage(messageClass, method, messageLength, transactionId, attributes);
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
            int padding = 4 - (length % 4);
            for (int i = 0; i < padding; i++) {
                in.get();
            }
            remaining -= padding;
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

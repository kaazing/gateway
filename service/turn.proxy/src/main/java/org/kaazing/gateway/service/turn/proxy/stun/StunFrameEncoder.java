package org.kaazing.gateway.service.turn.proxy.stun;

import java.nio.ByteBuffer;

import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolEncoderAdapter;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;
import org.kaazing.mina.core.buffer.IoBufferAllocatorEx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StunFrameEncoder extends ProtocolEncoderAdapter {

    private final IoBufferAllocatorEx<?> allocator;
    private static final Logger LOGGER = LoggerFactory.getLogger("service.turn.proxy");

    public StunFrameEncoder(IoBufferAllocatorEx<?> allocator) {
        this.allocator = allocator;
    }

    @Override
    public void encode(IoSession session, Object message, ProtocolEncoderOutput out) throws Exception {
        if (!(message instanceof StunMessage)) {
            // easiest way to avoid race condition where decoder is removed on the filter chain prior to encoder
            out.write(message);
        }
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Encoding STUN message: " + message);
        }
        StunMessage stunMessage = (StunMessage) message;
        ByteBuffer buf = allocator.allocate(20 + stunMessage.getMessageLength());

        short messageMethod = stunMessage.getMethod().getValue();
        short messageClass = stunMessage.getMessageClass().getValue();
        buf.putShort((short) (messageMethod | messageClass));

        buf.putShort(stunMessage.getMessageLength());

        buf.putInt(StunMessage.MAGIC_COOKIE);

        buf.put(stunMessage.getTransactionId());

        for (StunMessageAttribute attribute : stunMessage.getAttributes()) {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Encoding STUN attribute: " + attribute);
            }
            buf.putShort(attribute.getType());
            short length = attribute.getLength();
            buf.putShort(length);
            byte[] variable = attribute.getVariable();
            buf.put(variable);
            for (int i = length; i < StunMessage.attributePaddedLength(length); i++) {
                buf.put((byte) 0x00);
            }

        }

        buf.flip();
        out.write(allocator.wrap(buf));
    }

}

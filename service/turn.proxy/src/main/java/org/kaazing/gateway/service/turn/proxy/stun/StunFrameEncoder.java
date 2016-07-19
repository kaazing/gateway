package org.kaazing.gateway.service.turn.proxy.stun;

import java.nio.ByteBuffer;

import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolEncoder;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;
import org.kaazing.mina.core.buffer.IoBufferAllocatorEx;

public class StunFrameEncoder implements ProtocolEncoder {

    private final IoBufferAllocatorEx<?> allocator;

    public StunFrameEncoder(IoBufferAllocatorEx<?> allocator) {
        this.allocator = allocator;
    }

    @Override
    public void encode(IoSession session, Object message, ProtocolEncoderOutput out) throws Exception {
        StunMessage stunMessage = (StunMessage) message;
        ByteBuffer buf = allocator.allocate(20 + stunMessage.getMessageLength());

        short messageMethod = stunMessage.getMethod().getValue();
        short messageClass = stunMessage.getMessageClass().getValue();
        buf.putShort((short) (messageMethod | messageClass));

        buf.putShort(stunMessage.getMessageLength());

        buf.putInt(StunMessage.MAGIC_COOKIE);

        buf.put(stunMessage.getTransactionId());

        for (StunMessageAttribute attribute : stunMessage.getAttributes()) {
            if (attribute instanceof KaazingNoopAttribute) {
                buf.putShort(attribute.getType());
                short length = attribute.getLength();
                buf.putShort(length);
                byte[] variable = ((KaazingNoopAttribute) attribute).getVariable();
                buf.put(variable);
                int padding = 4 - (length % 4);
                for(int i = 0; i < padding; i++){
                    buf.put((byte) 0x00);
                }
            }

        }

        out.write(buf);

    }

    @Override
    public void dispose(IoSession session) throws Exception {
    }

}

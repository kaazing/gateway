package org.kaazing.gateway.service.turn.proxy.stun;

import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFactory;
import org.apache.mina.filter.codec.ProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolEncoder;
import org.kaazing.mina.core.buffer.IoBufferAllocatorEx;
import org.kaazing.mina.core.session.IoSessionEx;
import org.kaazing.mina.filter.codec.ProtocolCodecFilter;

public class StunCodecFilter extends ProtocolCodecFilter { //IoFilterAdapter<IoSessionEx> {

    public StunCodecFilter(){
        super(new TurnCodecFactory());
    }
    
    private static class TurnCodecFactory implements ProtocolCodecFactory{

        @Override
        public ProtocolEncoder getEncoder(IoSession session) throws Exception {
            IoSessionEx sessionEx = (IoSessionEx) session;
            IoBufferAllocatorEx<?> allocator = sessionEx.getBufferAllocator();

            return new StunFrameEncoder(allocator);
        }

        @Override
        public ProtocolDecoder getDecoder(IoSession session) throws Exception {
            IoSessionEx sessionEx = (IoSessionEx) session;
            IoBufferAllocatorEx<?> allocator = sessionEx.getBufferAllocator();
            return new StunFrameDecoder(allocator);
        }
        
    }
}

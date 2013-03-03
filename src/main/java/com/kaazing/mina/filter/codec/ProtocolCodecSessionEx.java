/**
 * Copyright (c) 2007-2012, Kaazing Corporation. All rights reserved.
 */

package com.kaazing.mina.filter.codec;

import java.util.Queue;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.filterchain.IoFilter.NextFilter;
import org.apache.mina.core.future.DefaultWriteFuture;
import org.apache.mina.core.future.WriteFuture;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.AbstractProtocolDecoderOutput;
import org.apache.mina.filter.codec.AbstractProtocolEncoderOutput;
import org.apache.mina.filter.codec.ProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;
import org.apache.mina.filter.codec.ProtocolEncoder;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;

import com.kaazing.mina.core.session.DummySessionEx;

/**
 * A virtual {@link IoSession} that provides {@link ProtocolEncoderOutput}
 * and {@link ProtocolDecoderOutput}.  It is useful for unit-testing
 * codec and reusing codec for non-network-use (e.g. serialization).
 *
 * <h2>Encoding</h2>
 * <pre>
 * ProtocolCodecSession session = new ProtocolCodecSession();
 * ProtocolEncoder encoder = ...;
 * MessageX in = ...;
 *
 * encoder.encode(session, in, session.getProtocolEncoderOutput());
 *
 * IoBuffer buffer = session.getProtocolDecoderOutputQueue().poll();
 * </pre>
 *
 * <h2>Decoding</h2>
 * <pre>
 * ProtocolCodecSession session = new ProtocolCodecSession();
 * ProtocolDecoder decoder = ...;
 * IoBuffer in = ...;
 *
 * decoder.decode(session, in, session.getProtocolDecoderOutput());
 *
 * Object message = session.getProtocolDecoderOutputQueue().poll();
 * </pre>
 */
public class ProtocolCodecSessionEx extends DummySessionEx {

    private final WriteFuture notWrittenFuture =
        DefaultWriteFuture.newNotWrittenFuture(this, new UnsupportedOperationException());

    private final AbstractProtocolEncoderOutput encoderOutput =
        new AbstractProtocolEncoderOutput() {
            public WriteFuture flush() {
                return notWrittenFuture;
            }
    };

    private final AbstractProtocolDecoderOutput decoderOutput =
        new AbstractProtocolDecoderOutput() {
            public void flush(NextFilter nextFilter, IoSession session) {
                // Do nothing
            }
    };

    /**
     * Creates a new instance.
     */
    public ProtocolCodecSessionEx() {
        // Do nothing
    }

    /**
     * Returns the {@link ProtocolEncoderOutput} that buffers
     * {@link IoBuffer}s generated by {@link ProtocolEncoder}.
     */
    public ProtocolEncoderOutput getEncoderOutput() {
        return encoderOutput;
    }

    /**
     * Returns the {@link Queue} of the buffered encoder output.
     */
    public Queue<Object> getEncoderOutputQueue() {
        return encoderOutput.getMessageQueue();
    }

    /**
     * Returns the {@link ProtocolEncoderOutput} that buffers
     * messages generated by {@link ProtocolDecoder}.
     */
    public ProtocolDecoderOutput getDecoderOutput() {
        return decoderOutput;
    }

    /**
     * Returns the {@link Queue} of the buffered decoder output.
     */
    public Queue<Object> getDecoderOutputQueue() {
        return decoderOutput.getMessageQueue();
    }
}

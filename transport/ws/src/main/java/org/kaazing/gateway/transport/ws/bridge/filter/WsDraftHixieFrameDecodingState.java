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

import java.util.Iterator;
import java.util.List;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.filter.codec.ProtocolDecoderException;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;
import org.apache.mina.filter.codec.statemachine.DecodingState;
import org.apache.mina.filter.codec.statemachine.SingleByteDecodingState;
import org.kaazing.gateway.transport.DecodingStateMachine;
import org.kaazing.gateway.transport.ws.WsBinaryMessage;
import org.kaazing.gateway.transport.ws.WsCloseMessage;
import org.kaazing.gateway.transport.ws.WsTextMessage;
import org.kaazing.gateway.util.ConsumeToTerminatorDecodingState;
import org.kaazing.mina.core.buffer.IoBufferAllocatorEx;
import org.kaazing.mina.core.buffer.IoBufferEx;
import org.kaazing.mina.filter.codec.statemachine.FixedLengthDecodingState;

public class WsDraftHixieFrameDecodingState extends DecodingStateMachine {

	private static final byte TEXT_TYPE_BYTE = (byte)0x00;
	private static final byte BINARY_TYPE_BYTE = (byte)0x80;
	private static final byte CLOSE_TYPE_BYTE = (byte)0xff;
	
    private final int maxDataSize;

    private final DecodingState READ_FRAME_TYPE = new SingleByteDecodingState() {

    	@Override
		protected DecodingState finishDecode(byte frameType, ProtocolDecoderOutput out) throws Exception {
        	switch (frameType) {
        	case TEXT_TYPE_BYTE:
        		return READ_TEXT_FRAME;
        	case BINARY_TYPE_BYTE:
        		return READ_BINARY_FRAME_LENGTH;
        	case CLOSE_TYPE_BYTE:
        		return READ_CLOSE_FRAME_LENGTH;
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

    private final DecodingState READ_TEXT_FRAME = new ConsumeToTerminatorDecodingState(allocator, (byte)0xff) {
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
        	out.write(new WsTextMessage((IoBufferEx) buffer));

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
					protected DecodingState finishDecode(IoBuffer product,
							ProtocolDecoderOutput out) throws Exception {
	
						// read the binary frame contents
			        	out.write(new WsBinaryMessage((IoBufferEx) product));
	
			        	return READ_FRAME_TYPE;
					}
				};
			}
		}
    };

    private final DecodingState READ_CLOSE_FRAME_LENGTH = new DecodingState() {

		@Override
		public DecodingState decode(IoBuffer in, ProtocolDecoderOutput out)
				throws Exception {
            while (in.hasRemaining()) {
				byte b = in.get();
				if (b == (byte) 0x00) {
					return finishDecode(out);
				} else {
					throw new ProtocolDecoderException("Expected zero length frame.");
				}
            }
            
            return this;
		}

		@Override
		public DecodingState finishDecode(ProtocolDecoderOutput out)
				throws Exception {
			out.write(new WsCloseMessage());
			return null;
		}
    	
    };
    
    WsDraftHixieFrameDecodingState(IoBufferAllocatorEx<?> allocator, int maxDataSize) {
        super(allocator);
        this.maxDataSize = maxDataSize;
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
	
	@Override
    protected void destroy() throws Exception {
    }
}

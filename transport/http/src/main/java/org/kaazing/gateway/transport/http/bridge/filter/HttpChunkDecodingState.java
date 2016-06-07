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
package org.kaazing.gateway.transport.http.bridge.filter;

import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.filter.codec.ProtocolDecoderException;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;
import org.apache.mina.filter.codec.statemachine.CrLfDecodingState;
import org.apache.mina.filter.codec.statemachine.DecodingState;
import org.apache.mina.filter.codec.statemachine.SkippingState;
import org.kaazing.gateway.transport.DecodingStateMachine;
import org.kaazing.mina.core.buffer.IoBufferAllocatorEx;
import org.kaazing.mina.core.buffer.IoBufferEx;
import org.kaazing.mina.filter.codec.statemachine.ConsumeToCrLfDecodingState;
import org.kaazing.mina.filter.codec.statemachine.FixedLengthDecodingState;

public abstract class HttpChunkDecodingState extends DecodingStateMachine {

	  private static final Charset US_ASCII = Charset.forName("US-ASCII");
	  
	  private final CharsetDecoder asciiDecoder = US_ASCII.newDecoder();
	  
	  HttpChunkDecodingState(IoBufferAllocatorEx<?> allocator) {
        super(allocator);
	  }

      @Override
	  protected void destroy() throws Exception {
	  }

	  @Override
	  protected DecodingState init() throws Exception {
	    return READ_CHUNK_LENGTH;
	  }
	  
	  private final DecodingState READ_CHUNK_LENGTH = new ConsumeToCrLfDecodingState(allocator) {
	    @Override
	    protected DecodingState finishDecode(IoBuffer product, ProtocolDecoderOutput out) throws Exception {
	      if (!product.hasRemaining()) {
	        throw new ProtocolDecoderException("Expected a chunk length");
	      }
	      
	      String length = product.getString(asciiDecoder);
	      int semiAt = length.indexOf(';');
	      if (semiAt != -1) {
	          length = length.substring(0, semiAt);
	      }
	      int chunkLength = Integer.parseInt(length, 16);
	      if (chunkLength <= 0) {
              IoBufferEx unsharedEmpty = allocator.wrap(allocator.allocate(0));
              out.write(unsharedEmpty);
              return FIND_END_OF_TRAILER;
	      }
	      else {
	        return new FixedLengthDecodingState(allocator, chunkLength) {
	          @Override
	          protected DecodingState finishDecode(IoBuffer readData, ProtocolDecoderOutput out) throws Exception {
	            out.write(readData);
	            return AFTER_CHUNK_DATA;
	          }
	        };
	      }
	    }
	  };
	  
	  private final DecodingState AFTER_CHUNK_DATA = new CrLfDecodingState() {
	    @Override
	    protected DecodingState finishDecode(boolean foundCRLF, ProtocolDecoderOutput out) throws Exception {
	      if (!foundCRLF) {
	        throw new ProtocolDecoderException("Expected CRLF after a chunk data");
	        
	      }
	      return null;
	    }
	  };
	  
	  private final DecodingState FIND_END_OF_TRAILER = new CrLfDecodingState() {
	    @Override
	    protected DecodingState finishDecode(boolean foundCRLF, ProtocolDecoderOutput out) throws Exception {
	      if (foundCRLF) {
	        return null; // Finish
	      } else {
	        return SKIP_ENTITY_HEADER;
	      }
	    }
	  };
	  
	  private final DecodingState SKIP_ENTITY_HEADER = new SkippingState() {

	    @Override
	    protected boolean canSkip(byte b) {
	      return (b != '\r');
	    }

	    @Override
	    protected DecodingState finishDecode(int skippedBytes) throws Exception {
	      return AFTER_SKIP_ENTITY_HEADER;
	    }
	  };
	  
	  private final DecodingState AFTER_SKIP_ENTITY_HEADER = new CrLfDecodingState() {
	    @Override
	    protected DecodingState finishDecode(boolean foundCRLF, ProtocolDecoderOutput out) throws Exception {
	      return FIND_END_OF_TRAILER;
	    }
	  };
	}

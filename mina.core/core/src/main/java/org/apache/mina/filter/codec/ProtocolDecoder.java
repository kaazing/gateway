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
package org.apache.mina.filter.codec;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;

/**
 * Decodes binary or protocol-specific data into higher-level message objects.
 * MINA invokes {@link #decode(IoSession, IoBuffer, ProtocolDecoderOutput)}
 * method with read data, and then the decoder implementation puts decoded
 * messages into {@link ProtocolDecoderOutput} by calling
 * {@link ProtocolDecoderOutput#write(Object)}.
 * <p>
 * Please refer to
 * <a href="../../../../../xref-examples/org/apache/mina/examples/reverser/TextLineDecoder.html"><code>TextLineDecoder</code></a>
 * example.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 * 
 * @see ProtocolDecoderException
 */
public interface ProtocolDecoder {
    /**
     * Decodes binary or protocol-specific content into higher-level message objects.
     * MINA invokes {@link #decode(IoSession, IoBuffer, ProtocolDecoderOutput)}
     * method with read data, and then the decoder implementation puts decoded
     * messages into {@link ProtocolDecoderOutput}.
     *
     * @throws Exception if the read data violated protocol specification
     */
    void decode(IoSession session, IoBuffer in, ProtocolDecoderOutput out)
            throws Exception;

    /**
     * Invoked when the specified <tt>session</tt> is closed.  This method is useful
     * when you deal with the protocol which doesn't specify the length of a message
     * such as HTTP response without <tt>content-length</tt> header. Implement this
     * method to process the remaining data that {@link #decode(IoSession, IoBuffer, ProtocolDecoderOutput)}
     * method didn't process completely.
     *
     * @throws Exception if the read data violated protocol specification
     */
    void finishDecode(IoSession session, ProtocolDecoderOutput out)
            throws Exception;

    /**
     * Releases all resources related with this decoder.
     *
     * @throws Exception if failed to dispose all resources
     */
    void dispose(IoSession session) throws Exception;
}
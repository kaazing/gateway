/**
 * Copyright (c) 2007-2014 Kaazing Corporation. All rights reserved.
 * 
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.kaazing.gateway.transport.ws.extension;

import java.util.ArrayList;
import java.util.List;

import org.kaazing.mina.core.buffer.IoBufferEx;

/**
 * Manages a collection of "escape" byte sequences that can be injected into an encoder to escape naturally-occurring
 * payloads commencing with themselves.
 *
 */
public class EscapeSequencer {

    private static byte[] EMPTY_BYTES = new byte[0];
    private static final int ESCAPE_SEQUENCE_LENGTH_IN_BYTES = 4;

    private List<byte[]> escapeSequences = new ArrayList<>(3);
    private List<Integer> escapeSequenceNumbers = new ArrayList<>(3);
    private Integer mask;

    public EscapeSequencer(List<byte[]> escapeSequences) {
        this(escapeSequences, null);
    }

    public EscapeSequencer(List<byte[]> escapeSequences, Integer mask) {
        this.escapeSequences = escapeSequences;
        this.escapeSequenceNumbers = from(escapeSequences);
        this.mask = mask;
    }

    /**
     * Returns required escape bytes for the provider buffer,
     * or a zero-length byte array if no escape bytes are required.
     *
     * @param buffer  the buffer whose contents may be subject to escaping
     * @return required escape bytes for the provider buffer,
     * or a zero-length byte array if no escape bytes are required.
     */
    public byte[] getEscapeBytes(IoBufferEx buffer) {
        if ( escapeSequences == null || escapeSequences.size() == 0) {
            return EMPTY_BYTES;
        }
        if ( buffer.remaining() >= ESCAPE_SEQUENCE_LENGTH_IN_BYTES) {

            int bufferBytes = buffer.getInt(buffer.position());
            if ( mask != null ) {
                // Do a quick check
                final int idx = mask & bufferBytes;
                if (idx != 0) {
                    return EMPTY_BYTES;
                }
                for ( int i = 0; i < escapeSequences.size(); i++) {
                    if ( bufferBytes == escapeSequenceNumbers.get(i)) {
                        return escapeSequences.get(i);
                    }
                }
            } else {
                for ( int i = 0; i < escapeSequences.size(); i++) {
                    if ( bufferBytes == escapeSequenceNumbers.get(i)) {
                        return escapeSequences.get(i);
                    }
                }
            }

        }
        return EMPTY_BYTES;
    }

    private static List<Integer> from(List<byte[]> sequences) {
        List<Integer> result = new ArrayList<>(sequences.size());
        for ( byte[] bytes: sequences) {
            result.add(bytesToInteger(bytes));
        }
        return result;
    }

    private static int bytesToInteger(byte[] bytes) {
        return (((bytes[0] & 0xff) << 24) |
                ((bytes[1] & 0xff) << 16) |
                ((bytes[2] & 0xff) <<  8) |
                ((bytes[3] & 0xff) <<  0));
    }

}

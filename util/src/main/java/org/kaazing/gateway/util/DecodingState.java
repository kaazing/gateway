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
package org.kaazing.gateway.util;

/**
 * This is used by Encoder to hold a "stray" byte that is not yet UTF-8 decode-able.
 * It is prepended onto the next message to form a
 * valid (possibly escaped) UTF-8 byte sequence.
 * This is needed to handle case where a UTF8 character or escaped UTF8 character is
 * represented as 2 bytes and gets split between packets.  See KG-3124 for a situation where this
 * makes a difference, and KG-4013 to note that this is important to get
 * right for all sorts of UTF-8 buffers, and KG-4372.
 */
public interface DecodingState {

    Object get();

    void set(Object state);

    DecodingState NONE = new DecodingState() {
        @Override
        public Object get() {
            return null;
        }
        @Override
        public void set(Object state) {
            if (state != null) {
                throw new UnsupportedOperationException("Cannot set state on IMMUTABLE DecodingState");
            }
        }
    };

}

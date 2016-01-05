/**
 * Copyright 2007-2015, Kaazing Corporation. All rights reserved.
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
package org.kaazing.gateway.transport;

import org.kaazing.mina.filter.codec.ProtocolCodecFilter;

import org.apache.mina.core.filterchain.IoFilterChain;
import org.apache.mina.core.filterchain.IoFilterChain.Entry;
import org.apache.mina.core.session.IoSession;
import org.slf4j.Logger;

public class ObjectLoggingFilter extends LoggingFilter {

    public ObjectLoggingFilter(Logger logger, String format) {
        super(logger, format);
    }

    @Override
    public void sessionOpened(NextFilter nextFilter, IoSession session) throws Exception {
        super.sessionOpened(nextFilter, session);    // move after codec to log readable objects instead of buffers

        IoFilterChain filterChain = session.getFilterChain();
        Entry codecEntry = filterChain.getEntry(ProtocolCodecFilter.class);
        if (codecEntry != null) {
            Entry loggingEntry = filterChain.getEntry(this);
            assert (loggingEntry != null);
            loggingEntry.remove();
            codecEntry.addAfter(loggingEntry.getName(), loggingEntry.getFilter());
        }
    }

    public static void moveAfterCodec(IoSession session) {
        // move logging filter after codec to log readable objects instead of buffers
        IoFilterChain filterChain = session.getFilterChain();
        Entry loggingEntry = filterChain.getEntry(ObjectLoggingFilter.class);
        if (loggingEntry == null) {
            return; // fail fast when tracing not active
        }
        Entry codecEntry = filterChain.getEntry(ProtocolCodecFilter.class);
        if (codecEntry != null) {
            loggingEntry.remove();
            codecEntry.addAfter(loggingEntry.getName(), loggingEntry.getFilter());
        }
    }

}

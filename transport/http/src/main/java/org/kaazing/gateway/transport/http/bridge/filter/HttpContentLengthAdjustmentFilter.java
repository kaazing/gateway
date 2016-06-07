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

import static java.lang.Integer.parseInt;
import static java.lang.String.valueOf;
import static org.kaazing.gateway.transport.http.HttpHeaders.HEADER_CONTENT_LENGTH;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.filterchain.IoFilterChain;
import org.apache.mina.core.write.WriteRequest;
import org.kaazing.gateway.transport.IoFilterAdapter;
import org.kaazing.gateway.transport.http.HttpSession;

public class HttpContentLengthAdjustmentFilter extends IoFilterAdapter<HttpSession> {

    @Override
    protected void doFilterWrite(NextFilter nextFilter, HttpSession session, WriteRequest writeRequest) throws Exception {
        //  GL.debug("http", getClass().getSimpleName() + " filter write.");

        // note: this assumes the session is still not committed
        //       but commitFuture is on HttpAcceptSession only,
        //       so need to add to HttpConnectSession interface too
        //       and then promote to HttpSession.getCommitFuture()

        String contentLengthHeader = session.getWriteHeader(HEADER_CONTENT_LENGTH);
        int contentLength = (contentLengthHeader != null) ? parseInt(contentLengthHeader) : 0;

        Object message = writeRequest.getMessage();
        IoBuffer buf = (IoBuffer) message;
        int remaining = buf.remaining();
        
        int newContentLength = contentLength + remaining;
        session.setWriteHeader(HEADER_CONTENT_LENGTH, valueOf(newContentLength));

        // GL.debug("http", String.format(getClass().getSimpleName() + " reset content length header from %d to %d.",
        //                               contentLength, newContentLength));

        // auto-remove after a single HttpRequestMessage or HttpResponseMessage
        IoFilterChain filterChain = session.getFilterChain();
        filterChain.remove(this);

        super.doFilterWrite(nextFilter, session, writeRequest);
        
    }

}

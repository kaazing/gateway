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

import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.transport.http.bridge.HttpRequestMessage;
import org.kaazing.gateway.transport.http.bridge.HttpResponseMessage;
import org.kaazing.mina.core.future.DefaultWriteFutureEx;
import org.kaazing.mina.core.write.DefaultWriteRequestEx;
import org.apache.mina.core.future.IoFutureListener;
import org.apache.mina.core.future.WriteFuture;
import org.apache.mina.core.session.IoSession;

import java.util.Arrays;
import java.util.List;

import static org.kaazing.gateway.transport.http.HttpStatus.CLIENT_NOT_FOUND;
import static org.kaazing.gateway.transport.http.HttpVersion.HTTP_1_1;

/**
 * This filter matches entire request path with bind address path in certain cases.
 *
 * In WSE case, binding contains /echo, /echo/;e/db/sessionid1, /echo/;e/db/sessionid2 etc.
 * If there is a request with /echo/;e/db/sessionid-foo, it matches /echo binding.
 * This catches it and sends 404 earlier before the request go to wse layer.
 */
public class HttpPathMatchingFilter extends HttpFilterAdapter<IoSession> {

    private static final List<String> WSE_FULL_MATCHING_PATHS = Arrays.asList("/;e/ub", "/;e/db", "/;e/ut", "/;e/dt",
            "/;e/ute", "/;e/dte", "/;e/ubm", "/;e/dbm", "/;e/utm", "/;e/dtm", "/;e/utem", "/;e/dtem");

    @Override
    protected void httpRequestReceived(NextFilter nextFilter, IoSession session, HttpRequestMessage httpRequest)
            throws Exception {

        ResourceAddress bindAddress = httpRequest.getLocalAddress();
        String nextProtocol = bindAddress.getOption(ResourceAddress.NEXT_PROTOCOL);
        if (nextProtocol == null || !nextProtocol.equals("httpxe/1.1")) {
            super.httpRequestReceived(nextFilter, session, httpRequest);
            return;
        }

        String bindPath = bindAddress.getResource().getPath();
        String requestPath = httpRequest.getRequestURI().getPath();
        // Fix requestPath to overcome javascript client bug (which sends URI with & )
        // for e.g. /echo/;e/utm/waGcY3k5CDLMxy0mNvijdpXRtHxFjt5Z&.krn=0.7393842079985082)
        int index = requestPath.indexOf('&');
        if (index != -1) {
            requestPath = requestPath.substring(0, index);
        }

        boolean match = true;
        for(String wsePath : WSE_FULL_MATCHING_PATHS) {
            if (requestPath.contains(wsePath)) {
                match = requestPath.equals(bindPath);
            }
        }

        if (match) {
            super.httpRequestReceived(nextFilter, session, httpRequest);
        } else {
            HttpResponseMessage httpResponse = new HttpResponseMessage();
            httpResponse.setVersion(HTTP_1_1);
            httpResponse.setStatus(CLIENT_NOT_FOUND);

            DefaultWriteFutureEx writeFuture = new DefaultWriteFutureEx(session);
            DefaultWriteRequestEx writeRequest = new DefaultWriteRequestEx(httpResponse, writeFuture);
            nextFilter.filterWrite(session, writeRequest);

            // Close after write
            writeRequest.getFuture().addListener(new IoFutureListener<WriteFuture>() {
                @Override
                public void operationComplete(WriteFuture future) {
                    IoSession session = future.getSession();

                    // close on flush at server
                    session.close(false);
                }
            });
        }
    }

}

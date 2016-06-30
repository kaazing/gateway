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

import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.write.WriteRequest;
import org.kaazing.gateway.transport.http.bridge.HttpContentMessage;
import org.kaazing.gateway.transport.http.bridge.HttpMessage;
import org.kaazing.gateway.transport.http.bridge.HttpRequestMessage;
import org.kaazing.gateway.transport.http.bridge.HttpResponseMessage;
import org.kaazing.mina.filter.util.WriteRequestFilterEx;


public class HttpFilterAdapter<S extends IoSession> extends WriteRequestFilterEx {

    @Override
    public void messageReceived(NextFilter nextFilter, IoSession session,
            Object message) throws Exception {

        HttpMessage httpMessage = (HttpMessage) message;
        S sessionEx = (S) session;
        switch (httpMessage.getKind()) {
        case REQUEST:
            HttpRequestMessage httpRequest = (HttpRequestMessage) httpMessage;
			httpRequestReceived(nextFilter, sessionEx, httpRequest);
            break;
        case RESPONSE:
            HttpResponseMessage httpResponse = (HttpResponseMessage) httpMessage;
			httpResponseReceived(nextFilter, sessionEx, httpResponse);
            break;
        case CONTENT:
            HttpContentMessage httpContent = (HttpContentMessage) httpMessage;
			httpContentReceived(nextFilter, sessionEx, httpContent);
            break;
        default:
            throw new IllegalArgumentException("Unrecognized HTTP message kind: " + httpMessage.getKind());
        }
    }

    @Override
    public void filterWrite(NextFilter nextFilter, IoSession session, WriteRequest writeRequest) throws Exception {

        Object message = writeRequest.getMessage();
        HttpMessage httpMessage = (HttpMessage) message;
        switch (httpMessage.getKind()) {
        case REQUEST:
            HttpRequestMessage httpRequest = (HttpRequestMessage) httpMessage;
            filterWriteHttpRequest(nextFilter, (S) session, writeRequest, httpRequest);
            break;
        case RESPONSE:
            HttpResponseMessage httpResponse = (HttpResponseMessage) httpMessage;
            filterWriteHttpResponse(nextFilter, (S) session, writeRequest, httpResponse);
            break;
        case CONTENT:
            HttpContentMessage httpContent = (HttpContentMessage) httpMessage;
            filterWriteHttpContent(nextFilter, (S) session, writeRequest, httpContent);
            break;
        default:
            throw new IllegalArgumentException("Unrecognized HTTP message kind: " + httpMessage.getKind());
        }
    }

    protected void filterWriteHttpRequest(NextFilter nextFilter, S session, WriteRequest writeRequest,
            HttpRequestMessage httpRequest) throws Exception {
        super.filterWrite(nextFilter, session, writeRequest);
    }

    protected void filterWriteHttpResponse(NextFilter nextFilter, S session, WriteRequest writeRequest,
            HttpResponseMessage httpResponse) throws Exception {
        super.filterWrite(nextFilter, session, writeRequest);
    }

    protected void filterWriteHttpContent(NextFilter nextFilter, S session, WriteRequest writeRequest,
            HttpContentMessage httpContent) throws Exception {
        super.filterWrite(nextFilter, session, writeRequest);
    }

    @Override
    protected Object doFilterWrite(NextFilter nextFilter, IoSession session,
			WriteRequest writeRequest, Object message) throws Exception {

        HttpMessage httpMessage = (HttpMessage) message;
		S sessionEx = (S) session;
        switch (httpMessage.getKind()) {
        case REQUEST:
            HttpRequestMessage httpRequest = (HttpRequestMessage) httpMessage;
			return doFilterWriteHttpRequest(nextFilter, sessionEx, writeRequest, httpRequest);
        case RESPONSE:
            HttpResponseMessage httpResponse = (HttpResponseMessage) httpMessage;
			return doFilterWriteHttpResponse(nextFilter, sessionEx, writeRequest, httpResponse);
        case CONTENT:
            HttpContentMessage httpContent = (HttpContentMessage) httpMessage;
			return doFilterWriteHttpContent(nextFilter, sessionEx, writeRequest, httpContent);
        default:
            throw new IllegalArgumentException("Unrecognized HTTP message kind: " + httpMessage.getKind());
        }
    }

	protected void httpRequestReceived(NextFilter nextFilter, S session,
            HttpRequestMessage httpRequest) throws Exception {
        super.messageReceived(nextFilter, session, httpRequest);
    }

	protected void httpResponseReceived(NextFilter nextFilter, S session,
            HttpResponseMessage httpResponse) throws Exception {
        super.messageReceived(nextFilter, session, httpResponse);
    }

	protected void httpContentReceived(NextFilter nextFilter, S session,
            HttpContentMessage httpContent) throws Exception {
        super.messageReceived(nextFilter, session, httpContent);
    }

	protected Object doFilterWriteHttpRequest(NextFilter nextFilter, S session,
            WriteRequest writeRequest, HttpRequestMessage httpRequest) throws Exception {
        return null;
    }

	protected Object doFilterWriteHttpResponse(NextFilter nextFilter, S session,
            WriteRequest writeRequest, HttpResponseMessage httpResponse) throws Exception {
        return null;
    }

	protected Object doFilterWriteHttpContent(NextFilter nextFilter, S session,
            WriteRequest writeRequest, HttpContentMessage httpContent) throws Exception {
        return null;
    }
}

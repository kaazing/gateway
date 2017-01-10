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

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.kaazing.gateway.transport.http.HttpStatus;
import org.kaazing.gateway.transport.http.bridge.HttpRequestMessage;
import org.kaazing.mina.core.session.IoSessionEx;

public class HttpHostHeaderFilter extends HttpFilterAdapter<IoSessionEx> {

    public static final String HEADER_HOST = "Host";

    @Override
    protected void httpRequestReceived(NextFilter nextFilter, IoSessionEx session, HttpRequestMessage httpRequest) throws Exception {
        Map<String, List<String>> headers = httpRequest.getHeaders();
        List<String> hostHeaderValues = headers.get(HEADER_HOST);
        // KG-12034 / KG-11219: Send 400 Bad Request when Host header is absent OR multiple Host header found OR value is empty
        // http://tools.ietf.org/html/rfc7230#section-5.4
        // A server MUST respond with a 400 (Bad Request) status code to any
        // HTTP/1.1 request message that lacks a Host header field and to any
        // request message that contains more than one Host header field or a
        // Host header field with an invalid field-value.
        URI requestURI = httpRequest.getRequestURI();
        if (hostHeaderValues == null) {
            String msg = String.format("HTTP request for URI %s doesn't have Host header", requestURI);
            throw new HttpProtocolDecoderException(msg, HttpStatus.CLIENT_BAD_REQUEST);
        } else if (hostHeaderValues.size() != 1) {
            String msg = String.format("HTTP request for URI %s has multiple Host headers %s", requestURI, hostHeaderValues);
            throw new HttpProtocolDecoderException(msg, HttpStatus.CLIENT_BAD_REQUEST);
        } else if (hostHeaderValues.get(0).isEmpty()) {
            String msg = String.format("HTTP request for URI %s has empty Host header field-value", requestURI);
            throw new HttpProtocolDecoderException(msg, HttpStatus.CLIENT_BAD_REQUEST);
        }

        URI absoluteURI = httpRequest.getAbsoluteRequestURI();
        if (absoluteURI != null) {
            String expectedHostHeader = absoluteURI.getHost()
                    + (absoluteURI.getPort() == -1 ? "" : ":" + absoluteURI.getPort());

            String gotHostHeader = hostHeaderValues.get(0);
            if (!expectedHostHeader.equalsIgnoreCase(gotHostHeader)) {
                String msg = String.format("Request URI %s is in absolute-form, hence expecting Host header %s, but got %s",
                        absoluteURI, expectedHostHeader, gotHostHeader);
                throw new HttpProtocolDecoderException(msg, HttpStatus.CLIENT_BAD_REQUEST);
            }
        }
        
        super.httpRequestReceived(nextFilter, session, httpRequest);
    }

}

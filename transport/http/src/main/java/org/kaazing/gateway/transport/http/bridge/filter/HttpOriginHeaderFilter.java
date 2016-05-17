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
import java.util.List;

import org.kaazing.gateway.transport.http.HttpAcceptor;
import org.kaazing.gateway.transport.http.HttpUtils;
import org.kaazing.gateway.transport.http.bridge.HttpRequestMessage;
import org.kaazing.mina.core.session.IoSessionEx;

public class HttpOriginHeaderFilter extends HttpFilterAdapter<IoSessionEx> {

    @Override
    protected void httpRequestReceived(NextFilter nextFilter, IoSessionEx session, HttpRequestMessage httpRequest) throws Exception {
        // GL.debug("http", getClass().getSimpleName() + " request received.");

        // TODO: revisit this logic
        //       suggestion: (if in same origin by referrer, observe X-Origin, .ko), falling back to Origin
        //       error: if no effective Origin is present (see compatibility origin header filter rejection for Silverlight checks)
        emulateOriginHeader(httpRequest, session);
        
        super.httpRequestReceived(nextFilter, session, httpRequest);
    }

    private void emulateOriginHeader(HttpRequestMessage httpRequest, IoSessionEx session) {
        // lookup request Origin header
        String origin = httpRequest.getHeader("Origin");

        // default to .ko parameter for iframe streaming emulation
        // prefer X-Origin header to .ko
        if (!httpRequest.hasHeader("X-Origin")) {
            String candidateOrigin = httpRequest.removeParameter(".ko");
            if (candidateOrigin != null) {
                // if ".ko" parameter is present, use this value as "X-Origin" header
                httpRequest.setHeader("X-Origin", candidateOrigin);
            }
        }

        // Origin header emulation fallback
        // default to X-Origin header or .ko parameter for same-origin
        // (used by cross-origin emulation)
        // default to X-Origin header for emulation
        String emulatedOrigin = null;
        List<String> emulatedOrigins = httpRequest.getHeaderValues("X-Origin", false);
        if (emulatedOrigins != null && !emulatedOrigins.isEmpty()) {
            if ( origin != null ) {
                String candidateOrigin = emulatedOrigins.get(0);
                emulatedOrigin = getEmulatedOriginIfRequestMatchesOrigin(httpRequest, candidateOrigin);
            } else {
                boolean httpxeSpecCompliant = HttpAcceptor.HTTPXE_SPEC_KEY.get(session);
                if (httpxeSpecCompliant) {
                    emulatedOrigin = getEmulatedOriginIfReferrerMatches(httpRequest, emulatedOrigins.get(0));
                } else {
                    emulatedOrigin = emulatedOrigins.get(0);
                }
            }
        }

        // handle the case where both Origin and X-Origin / .ko are on the wire
        // Origin header should take precedence, but emulated origin implies envelope response expected
        if (emulatedOrigin != null) {

            // KG-3514: Always prefer X-Origin in the case where both X-Origin
            // and Origin are (incorrectly) included in the same request.
            // This is OK because no sandboxed code can set the x-origin header.

            // KG-1474 Canonicalize hostname portion of Origin header to lowercase to avoid spurious same origin rejection
            // by HttpCrossSiteFilter due to use of mixed case client-side WebSocket connect URI
            if ("null".equals(emulatedOrigin)) {
                httpRequest.setHeader("Origin", "null");
            } else {
                try {
                    String emulatedOriginLC = HttpUtils.getCanonicalURI(emulatedOrigin, false).toString();
                    httpRequest.setHeader("Origin", emulatedOriginLC);
                } catch (Exception e) {
                    // cannot parse the emulatedOrigin
                    // it should not have been serialized except as "null"
                    httpRequest.setHeader("Origin", "null");
                }
            }

        }
    }

    private String getEmulatedOriginIfReferrerMatches(HttpRequestMessage httpRequest, String candidateOrigin) {
        String emulatedOrigin = null;
        // same-origin iframe will always send Referer (never cross-scheme)
        // so verify same-origin request to allow .ko query parameter
        String referer = httpRequest.getHeader("Referer");
        if (referer != null) {
            URI refererURI = URI.create(referer);
            boolean isSecure = httpRequest.isSecure();
            String scheme = isSecure ? "https" : "http";
            String authority = HttpUtils.getHostAndPort(httpRequest, isSecure);
            String refererAuthority = HttpUtils.getHostAndPort(refererURI.getAuthority(), isSecure);
            if (refererURI.getScheme().equals(scheme) && refererAuthority.equals(authority)) {
                // cross-origin request emulated via same-origin request,
                // use .ko query parameter for Origin
                emulatedOrigin = candidateOrigin;
            } else {
                emulatedOrigin = "null";
            }
        }
        return emulatedOrigin;
    }

    private String getEmulatedOriginIfRequestMatchesOrigin(HttpRequestMessage httpRequest, String candidateOrigin) {
        String emulatedOrigin = null;
        
        String origin = httpRequest.getHeader("Origin");
        URI requestURI = HttpUtils.getCanonicalURI(httpRequest.getRequestURI(), false);

        if (origin != null && requestURI != null) {
            URI originURI = HttpUtils.getCanonicalURI(origin, false);
            String originScheme = originURI.getScheme();
            String originAuthority = originURI.getAuthority();
            if (originAuthority != null && originAuthority.indexOf(':') == -1) {
                int port = "https".equals(originScheme) ? 443 : 80;
                originAuthority += ":" + port;
            }

            boolean isSecure = httpRequest.isSecure();
            String scheme = isSecure ? "https" : "http";
            String authority = HttpUtils.getHostAndPort(httpRequest, isSecure);

            if (scheme.equals(originScheme) && authority.equals(originAuthority)) {
                // cross-origin request emulated via same-origin request,
                // use .ko query parameter for Origin
                emulatedOrigin = candidateOrigin;
            }
        }
        return emulatedOrigin;
    }
}

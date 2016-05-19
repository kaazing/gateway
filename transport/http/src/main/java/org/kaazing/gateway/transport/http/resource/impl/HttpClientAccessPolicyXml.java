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
package org.kaazing.gateway.transport.http.resource.impl;

import static java.lang.String.format;
import static java.lang.String.valueOf;
import static org.jboss.netty.util.CharsetUtil.UTF_8;
import static org.kaazing.gateway.resource.address.http.HttpResourceAddress.GATEWAY_ORIGIN_SECURITY;
import static org.kaazing.gateway.transport.http.HttpHeaders.HEADER_AUTHORIZATION;
import static org.kaazing.gateway.transport.http.HttpHeaders.HEADER_CONTENT_LENGTH;
import static org.kaazing.gateway.transport.http.HttpHeaders.HEADER_MAX_AGE;
import static org.kaazing.gateway.transport.http.HttpHeaders.HEADER_WEBSOCKET_EXTENSIONS;
import static org.kaazing.gateway.transport.http.HttpHeaders.HEADER_WEBSOCKET_VERSION;
import static org.kaazing.gateway.transport.http.HttpHeaders.HEADER_X_ACCEPT_COMMANDS;
import static org.kaazing.gateway.transport.http.HttpHeaders.HEADER_X_ORIGIN;
import static org.kaazing.gateway.transport.http.HttpHeaders.HEADER_X_SEQUENCE_NO;
import static org.kaazing.mina.core.buffer.IoBufferEx.FLAG_SHARED;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.charset.CharsetEncoder;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import org.apache.mina.core.future.WriteFuture;
import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.resource.address.http.GatewayHttpOriginSecurity;
import org.kaazing.gateway.resource.address.uri.URIUtils;
import org.kaazing.gateway.security.CrossSiteConstraintContext;
import org.kaazing.gateway.transport.http.HttpAcceptSession;
import org.kaazing.gateway.transport.http.HttpStatus;
import org.kaazing.gateway.transport.http.resource.HttpDynamicResource;
import org.kaazing.mina.core.buffer.IoBufferAllocatorEx;
import org.kaazing.mina.core.buffer.IoBufferEx;
import org.kaazing.mina.netty.util.threadlocal.VicariousThreadLocal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public final class HttpClientAccessPolicyXml extends HttpDynamicResource {

    private static final URI NULL_ORIGIN = URI.create("null");
    private final ThreadLocal<ConcurrentMap<String, CachedResult>> cacheByAuthorityRef = new VicariousThreadLocal<ConcurrentMap<String, CachedResult>>() {

        @Override
        protected ConcurrentMap<String, CachedResult> initialValue() {
            return new ConcurrentHashMap<>();
        }
    };
    private static final Logger LOGGER = LoggerFactory.getLogger(HttpClientAccessPolicyXml.class);

    // Note: need one <policy> per <cross-site-constraint> in
    // gateway configuration file.
    //
    // <?xml version="1.0" encoding="utf-8"?>
    // <access-policy>
    // <cross-domain-access>
    // <policy>
    // <allow-from http-request-headers="X-Origin, X-Origin-[origin], [headers]">
    // <domain uri="[origin]"/>
    // </allow-from>
    // <grant-to>
    // <resource path="/;put[accept-path]" include-subpaths="true"/> PUT
    // <resource path="/;delete[accept-path]" include-subpaths="true"/> DELETE
    // <resource path="/;post[accept-path]" include-subpaths="true"/> POST
    // <resource path="/;get[accept-path]" include-subpaths="true"/> GET
    // </grant-to>
    // </policy>
    // </cross-domain-access>
    // </access-policy>

    @Override
    public void writeFile(HttpAcceptSession httpSession) throws IOException {
        CharsetEncoder utf8Encoder = UTF_8.newEncoder();

        ResourceAddress localAddress = httpSession.getLocalAddress();
        URI requestURI = localAddress.getResource();
        String requestAuthority = requestURI.getAuthority();

        ConcurrentMap<String, CachedResult> cacheByAuthority = cacheByAuthorityRef.get();
        CachedResult cache = cacheByAuthority.get(requestAuthority);
        if (cache != null) {
            cache.writeFile(httpSession);
            return;
        }

        GatewayHttpOriginSecurity gatewayHttpOriginSecurity = localAddress.getOption(GATEWAY_ORIGIN_SECURITY);

        List<Map<String, Map<String, CrossSiteConstraintContext>>> listOfAcceptConstraintsByURI = gatewayHttpOriginSecurity
                .getAuthorityToSetOfAcceptConstraintsByURI();

        Integer minimumMaximumAge = null;
        IoBufferAllocatorEx<?> bufferAllocator = httpSession.getBufferAllocator();
        IoBufferEx buf = bufferAllocator.wrap(ByteBuffer.allocate(10240), FLAG_SHARED).setAutoExpander(bufferAllocator);

        buf.putString("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n", utf8Encoder);
        buf.putString("<access-policy>\n", utf8Encoder);
        buf.putString("<cross-domain-access>\n", utf8Encoder);

        boolean createdPolicy = false;

        HashSet<String> alreadyVisited = new HashSet<>();
        // for all the services
        for (Map<String, Map<String, CrossSiteConstraintContext>> servicesAcceptsToCrossSiteConstraints : listOfAcceptConstraintsByURI) {
            // for each accept on the service
            for (Entry<String, Map<String, CrossSiteConstraintContext>> acceptToConstraints : servicesAcceptsToCrossSiteConstraints
                    .entrySet()) {
                String acceptURI = acceptToConstraints.getKey();
                // if the accept authority matches the request athority
                if (URIUtils.getAuthority(acceptURI).equals(requestAuthority)) {
                    // for each cross site constraint on the accept
                    for (Entry<String, CrossSiteConstraintContext> urlToConstraint : acceptToConstraints
                            .getValue().entrySet()) {
                        CrossSiteConstraintContext constraint = urlToConstraint.getValue();
                        String urlOfConstraint = urlToConstraint.getKey();
                        // limit to purely cross-origin constraints, excluding same-origin constraints
                        // because Silverlight does not fetch or enforce clientaccesspolicy.xml for same-origin HTTP
                        // requests
                        if (!urlOfConstraint.equals(requestURI.toString())) {

                            Integer maximumAge = constraint.getMaximumAge();

                            if (maximumAge != null) {
                                if (minimumMaximumAge == null) {
                                    minimumMaximumAge = maximumAge;
                                } else {
                                    minimumMaximumAge = Math.min(maximumAge, minimumMaximumAge);
                                }
                            }

                            String acceptURIPath = URIUtils.getPath(acceptURI);
                            String sourceOrigin = constraint.getAllowOrigin();
                            String allowHeaders = constraint.getAllowHeaders();
                            String allowOrigin = constraint.getAllowOrigin();
                            String allowMethods = constraint.getAllowMethods();
                            String idString = String.format("%s%s%s%s%s", sourceOrigin, allowHeaders, allowOrigin,
                                    allowMethods, acceptURIPath);

                            // We can get multiple identical definitions due to the Map<URI <Map<String,
                            // DefaultCrossSiteConstraintContext>>>
                            // where we have identical maps on the inside
                            if (!alreadyVisited.contains(idString)) {
                                alreadyVisited.add(idString);

                                createdPolicy = true;
                                buf.putString("<policy>\n", utf8Encoder);

                                // Adding all the relevant headers that are submitted,
                                // by the (silverlight) client!
                                buf.putString("<allow-from http-request-headers=\"", utf8Encoder);

                                // applying the Authoization header:
                                buf.putString(HEADER_AUTHORIZATION + ",", utf8Encoder);

                                // 'X-WebSocket-Extensions' header, which is used to issue
                                // (Kaazing) WebSocket extensions
                                buf.putString(HEADER_WEBSOCKET_EXTENSIONS + ",", utf8Encoder);

                                // 'X-WebSocket-Version' header is used to identify the underlying version of the
                                // (websocket)
                                // protocol;
                                // For our emulation strategy the value looks like "wseb-1.0"
                                buf.putString(HEADER_WEBSOCKET_VERSION + ",", utf8Encoder);

                                // X-Origin header:
                                buf.putString(HEADER_X_ORIGIN + ",", utf8Encoder);

                                // X-Accept-Commands
                                buf.putString(HEADER_X_ACCEPT_COMMANDS + ",", utf8Encoder);

                                // X-Sequence-No
                                buf.putString(HEADER_X_SEQUENCE_NO + ",", utf8Encoder);

                                // original header:
                                URI sourceOriginURI = ("*".equals(sourceOrigin)) ? NULL_ORIGIN : URI.create(URLEncoder
                                        .encode(sourceOrigin, "UTF-8"));
                                String originHeader = format("X-Origin-%s", sourceOriginURI.toASCIIString());
                                buf.putString(originHeader, utf8Encoder);

                                if (allowHeaders != null) {
                                    buf.putString("," + allowHeaders, utf8Encoder);
                                }
                                buf.putString("\">\n", utf8Encoder);

                                if ("*".equals(allowOrigin)) {
                                    buf.putString("<domain uri=\"http://*\"/>\n", utf8Encoder);
                                    buf.putString("<domain uri=\"https://*\"/>\n", utf8Encoder);
                                    buf.putString("<domain uri=\"file:///\"/>\n", utf8Encoder);
                                } else {
                                    buf.putString("<domain uri=\"" + allowOrigin + "\"/>\n", utf8Encoder);
                                }
                                buf.putString("</allow-from>\n", utf8Encoder);

                                buf.putString("<grant-to>\n", utf8Encoder);
                                if (allowMethods != null) {
                                    String[] allowedMethods = allowMethods.toLowerCase().split(",");
                                    for (String allowedMethod : allowedMethods) {
                                        buf.putString("<resource path=\"/;" + allowedMethod, utf8Encoder);
                                        buf.putString(acceptURIPath, utf8Encoder);
                                        buf.putString("\" include-subpaths=\"true\"/>\n", utf8Encoder);
                                    }
                                }
                                buf.putString("</grant-to>\n", utf8Encoder);
                                buf.putString("</policy>\n", utf8Encoder);
                            }

                        }
                    }
                }
            }
        }

        buf.putString("</cross-domain-access>\n", utf8Encoder);
        buf.putString("</access-policy>\n", utf8Encoder);
        buf.flip();

        if (createdPolicy) {
            if (minimumMaximumAge != null) {
                httpSession.setWriteHeader(HEADER_MAX_AGE, valueOf(minimumMaximumAge));
            }
            httpSession.setWriteHeader(HEADER_CONTENT_LENGTH, valueOf(buf.remaining()));

            CachedResult candidateResult = new ValidResult(minimumMaximumAge, buf);
            CachedResult result = cacheByAuthority.putIfAbsent(requestAuthority, candidateResult);
            if (result == null) {
                result = candidateResult;
            }
            result.writeFile(httpSession);
        } else {
            cacheByAuthority.put(requestAuthority, new InvalidResult());
            httpSession.setStatus(HttpStatus.CLIENT_NOT_FOUND);
            httpSession.close(true);
        }
    }

    private interface CachedResult {
        void writeFile(HttpAcceptSession httpSession);

    }

    private static class ValidResult implements CachedResult {
        private final IoBufferEx result;
        private final Integer maxAge;

        public ValidResult(Integer maxAge, IoBufferEx result) {
            this.maxAge = maxAge;
            this.result = result;
        }

        @Override
        public void writeFile(HttpAcceptSession httpSession) {
            if (maxAge != null) {
                httpSession.setWriteHeader(HEADER_MAX_AGE, valueOf(maxAge));
            }
            httpSession.setWriteHeader(HEADER_CONTENT_LENGTH, valueOf(result.remaining()));

            WriteFuture f = httpSession.write(result.duplicate());
            try {
                f.await(10, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                if (LOGGER.isWarnEnabled()) {
                    LOGGER.warn(String.format("Write future in %s never fired", HttpClientAccessPolicyXml.class));
                }
            }
            httpSession.close(true);
        }
    }

    private static class InvalidResult implements CachedResult {

        @Override
        public void writeFile(HttpAcceptSession httpSession) {
            httpSession.setStatus(HttpStatus.CLIENT_NOT_FOUND);
            httpSession.close(true);
        }

    }

}

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

import static java.util.Arrays.asList;
import static org.kaazing.gateway.resource.address.http.HttpResourceAddress.ORIGIN_SECURITY;

import java.net.URI;
import java.util.Collection;

import org.apache.mina.core.session.AttributeKey;
import org.apache.mina.core.write.WriteRequest;
import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.resource.address.http.HttpOriginSecurity;
import org.kaazing.gateway.resource.address.http.HttpOriginSecurity.HttpOriginConstraint;
import org.kaazing.gateway.resource.address.http.HttpResourceAddress;
import org.kaazing.gateway.resource.address.uri.URIUtils;
import org.kaazing.gateway.transport.http.HttpMethod;
import org.kaazing.gateway.transport.http.HttpStatus;
import org.kaazing.gateway.transport.http.HttpUtils;
import org.kaazing.gateway.transport.http.HttpVersion;
import org.kaazing.gateway.transport.http.bridge.HttpRequestMessage;
import org.kaazing.gateway.transport.http.bridge.HttpResponseMessage;
import org.kaazing.mina.core.future.DefaultWriteFutureEx;
import org.kaazing.mina.core.session.IoSessionEx;
import org.kaazing.mina.core.write.DefaultWriteRequestEx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpOriginSecurityFilter extends HttpFilterAdapter<IoSessionEx> {

    private static final String PARAM_ACCESS_CONTROL = ".kac";
    private static final String PARAM_VALUE_ACCESS_CONTROL_EXPLICIT = "ex";

    private static final AttributeKey ACCESS_CONTROL_ALLOW_ORIGIN_KEY = new AttributeKey(HttpOriginSecurityFilter.class,
            "accessControlAllowOrigin");

    private final Logger logger = LoggerFactory.getLogger(HttpOriginSecurityFilter.class);

    @Override
    protected void httpRequestReceived(NextFilter nextFilter, IoSessionEx session, HttpRequestMessage httpRequest) throws Exception {
        // GL.debug("http", getClass().getSimpleName() + " request received.");

        ResourceAddress localAddress = httpRequest.getLocalAddress();
        if (localAddress == null) {
            throw new IllegalStateException("localAddress is null");
        }

        // lookup request Origin header
        String crossOrigin = httpRequest.getHeader("Origin");

        // compare with requested URI to detect same-origin
        String wireOrigin = crossOrigin;
        if (crossOrigin != null && !"null".equals(crossOrigin)) {
            try {
                URI originURI = new URI(crossOrigin);
                String originScheme = originURI.getScheme();
                String originAuthority = originURI.getAuthority();
                originAuthority = HttpUtils.getHostAndPort(originAuthority, originScheme.equals("https"));


                if (!originAuthority.equals(originURI.getAuthority())) {
                    crossOrigin = originScheme + "://" + originAuthority;
                }
                // if accept uri matches origin header, let it in
                if (localAddress.getResource().getAuthority().equals(originAuthority)) {
                    crossOrigin = null;
                } else {
                    Collection<String> balanceOrigins = localAddress.getOption(HttpResourceAddress.BALANCE_ORIGINS);
                    if (balanceOrigins != null && !balanceOrigins.isEmpty()) {
                        for (String targetURI : balanceOrigins) {
                            boolean targetIsSecure = "https".equals(URIUtils.getScheme(targetURI));
                            String targetScheme = URIUtils.getScheme(targetURI);
                            String targetAuthority = HttpUtils.getHostAndPort(URIUtils.getAuthority(targetURI), targetIsSecure);
                            if ("privileged".equals(originScheme)
                                    || ((targetScheme.equals(originScheme) && targetAuthority.equals(originAuthority)))) {
                                crossOrigin = null;
                                break;
                            }
                        }
                    } else {

                        boolean targetIsSecure = httpRequest.isSecure();
                        String targetScheme = targetIsSecure ? "https" : "http";
                        String targetAuthority = HttpUtils.getHostAndPort(httpRequest, targetIsSecure);

                        if ("privileged".equals(originScheme)
                                || ((targetScheme.equals(originScheme) && targetAuthority.equals(originAuthority)))) {
                            crossOrigin = null;
                        }

                    }
                }                

            } catch (Exception e) {
                // Bugfix for older Chrome browsers
                crossOrigin = "null";
            }
        }

        // Cross-site requests for URLs with no corresponding service registration
        // should still respond to preflight request automatically
        HttpOriginConstraint crossSiteConstraint = null;
        HttpOriginSecurity crossOriginSecurity = localAddress.getOption(ORIGIN_SECURITY);
        if (crossOriginSecurity != null) {
            crossSiteConstraint = crossOriginSecurity.getConstraint(crossOrigin);
            if (crossSiteConstraint == null && crossOrigin != null) {
                crossSiteConstraint = crossOriginSecurity.getConstraint("*");
            }
        }

        String allowOrigin = (crossSiteConstraint != null) ? crossSiteConstraint.getAllowOrigin() : null;
        // KG-7235 IE9 downstream uses XDR, this requests allowOrigin header in response.  Thus for IE9 (and other versions of IE), we need to make sure to set the response header.
        // KG-9263 IE8+ XDR requires explicit access control response headers (even for same origin requests)
        boolean explicitAccessControl = PARAM_VALUE_ACCESS_CONTROL_EXPLICIT.equals(httpRequest.getParameter(PARAM_ACCESS_CONTROL));
        if (allowOrigin != null || explicitAccessControl) {
            // allow-credentials cannot be combined with allow-origin "*", so we need to be explicit
            // also, some browsers are particular about the Origin matching precisely (regarding default HTTP ports)
            // so when we get a match, then make sure we use the Origin header in the response Access-Control-Allow-Origin
            allowOrigin = wireOrigin;
        }
        session.setAttribute(ACCESS_CONTROL_ALLOW_ORIGIN_KEY, allowOrigin);

        // Cross-site access control preflight method is an OPTIONS request
        // triggered
        // by the use of a specific HTTP method or header name
        HttpMethod httpMethod = httpRequest.getMethod();
        switch (httpMethod) {
        case OPTIONS:
            String requestMethod = httpRequest.getHeader("Access-Control-Request-Method");
            String requestHeaders = httpRequest.getHeader("Access-Control-Request-Headers");
            if (crossOrigin != null && (requestMethod != null || requestHeaders != null)) {

                HttpResponseMessage httpResponse = new HttpResponseMessage();
                httpResponse.setVersion(HttpVersion.HTTP_1_1);
                httpResponse.setStatus(HttpStatus.SUCCESS_OK);

                // fill in cross-site access control constraints, if service registration exists
                // otherwise, lack of preflight response headers will cause browser to reject
                if (crossSiteConstraint != null) {
                    String allowMethods = crossSiteConstraint.getAllowMethods();
                    String allowHeaders = crossSiteConstraint.getAllowHeaders();
                    Integer maximumAge = crossSiteConstraint.getMaximumAge();

                    if (allowMethods != null) {
                        httpResponse.setHeader("Access-Control-Allow-Methods", allowMethods);
                    }

                    if (allowHeaders != null) {
                        httpResponse.setHeader("Access-Control-Allow-Headers", allowHeaders);
                    }

                    // credentials always allowed by emulation layer
                    httpResponse.setHeader("Access-Control-Allow-Credentials", "true");

                    if (maximumAge != null) {
                        httpResponse.setHeader("Access-Control-Max-Age", maximumAge.toString());
                        // "Max-Age" needed by emulation
                        httpResponse.setHeader("Max-Age", maximumAge.toString());
                    }
                }

                // write cross-site access control preflight response
                filterWrite(nextFilter, session, new DefaultWriteRequestEx(httpResponse, new DefaultWriteFutureEx(session)));
                return;
            }
            break;
        }

        // enforce existence of cross site access control constraints
        // for services, specifically when preflight is not necessary
        if (crossOrigin != null
                && (crossSiteConstraint == null || !crossSiteConstraint.getAllowMethods().contains(httpMethod.toString()))) {
            HttpResponseMessage httpResponse = new HttpResponseMessage();
            httpResponse.setVersion(HttpVersion.HTTP_1_1);
            httpResponse.setStatus(HttpStatus.CLIENT_FORBIDDEN);

            if (logger.isDebugEnabled()) {
                logger.debug("Rejected cross-origin request for location \"{}\" from origin \"{}\"", 
                             new Object[] { localAddress.getExternalURI(), crossOrigin });
            }

            filterWrite(nextFilter, session, new DefaultWriteRequestEx(httpResponse, new DefaultWriteFutureEx(session)));
            return;
        }

        // cross-site access control constraint passed, propagate request
        super.httpRequestReceived(nextFilter, session, httpRequest);
    }

    @Override
    protected Object doFilterWriteHttpResponse(NextFilter nextFilter, IoSessionEx session, WriteRequest writeRequest,
            HttpResponseMessage httpResponse) throws Exception {

        String allowOrigin = (String) session.removeAttribute(ACCESS_CONTROL_ALLOW_ORIGIN_KEY);
        if (allowOrigin != null) {

            httpResponse.setHeader("Access-Control-Allow-Origin", allowOrigin);

            // credentials always allowed by emulation layer
            httpResponse.setHeader("Access-Control-Allow-Credentials", "true");

            // the following headers are allowed by emulation layer resources
            // http://www.w3.org/TR/cors/#list-of-headers
            //     NOTE: NEVER EVER EVER add X-Origin to this list.
            //     Your code might start working, but you will have broken security.
            httpResponse.getHeaderValues("Access-Control-Allow-Headers").addAll(asList("content-type", "authorization", "x-websocket-extensions", "x-websocket-version", "x-websocket-protocol"));
        }

        return super.doFilterWriteHttpResponse(nextFilter, session, writeRequest, httpResponse);
    }

}

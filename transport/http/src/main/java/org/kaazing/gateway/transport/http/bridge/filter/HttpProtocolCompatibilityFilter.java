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

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableMap;
import static org.kaazing.gateway.resource.address.ResourceAddress.NEXT_PROTOCOL;
import static org.kaazing.gateway.transport.http.HttpHeaders.HEADER_AUTHORIZATION;
import static org.kaazing.gateway.transport.http.HttpHeaders.HEADER_CONTENT_LENGTH;
import static org.kaazing.gateway.transport.http.HttpHeaders.HEADER_CONTENT_TYPE;
import static org.kaazing.gateway.transport.http.HttpHeaders.HEADER_X_NEXT_PROTOCOL;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.mina.core.filterchain.IoFilter;
import org.apache.mina.core.filterchain.IoFilterChain;
import org.apache.mina.core.future.DefaultWriteFuture;
import org.apache.mina.core.future.WriteFuture;
import org.apache.mina.core.session.AttributeKey;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.write.DefaultWriteRequest;
import org.apache.mina.core.write.WriteRequest;
import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.transport.BridgeSession;
import org.kaazing.gateway.transport.IoFilterAdapter;
import org.kaazing.gateway.transport.http.DefaultHttpSession;
import org.kaazing.gateway.transport.http.HttpAcceptFilter;
import org.kaazing.gateway.transport.http.HttpAcceptSession;
import org.kaazing.gateway.transport.http.HttpCookie;
import org.kaazing.gateway.transport.http.HttpHeaders;
import org.kaazing.gateway.transport.http.HttpMethod;
import org.kaazing.gateway.transport.http.HttpStatus;
import org.kaazing.gateway.transport.http.HttpUtils;
import org.kaazing.gateway.transport.http.HttpVersion;
import org.kaazing.gateway.transport.http.bridge.HttpContentMessage;
import org.kaazing.gateway.transport.http.bridge.HttpRequestMessage;
import org.kaazing.gateway.transport.http.bridge.HttpResponseMessage;
import org.kaazing.mina.core.buffer.IoBufferAllocatorEx;
import org.kaazing.mina.core.buffer.IoBufferEx;
import org.kaazing.mina.core.future.DefaultWriteFutureEx;
import org.kaazing.mina.core.session.IoSessionEx;
import org.kaazing.mina.core.write.DefaultWriteRequestEx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpProtocolCompatibilityFilter extends HttpFilterAdapter<IoSessionEx> {

    public static final String PROTOCOL_HTTPXE_1_1 = "httpxe/1.1";

    private static final String CONTENT_TYPE_APPLICATION_X_MESSAGE_HTTP = "application/x-message-http";

    private static final String PROTOCOL_WSE_1_0 = "wse/1.0";

    private static final String HEADER_CREATE_ENCODING = "X-Create-Encoding";
    private static final String HEADER_X_HTTP_VERSION = "X-Http-Version";
    private static final String HEADER_VALUE_HTTPE_VERSION_1_0 = "httpe-1.0";

    public static final String PARAMETER_X_SEQUENCE_NO = ".ksn";

    private static final String QUERY_PARAM_METHOD = ".km";
    private static final String QUERY_PARAM_RESOURCE = ".kr";
    private static final String QUERY_PARAM_RANDOM_NUMBER = ".krn";

    private static final String RESOURCE_NAME_CLIENT_ACCESS_POLICY_XML = "clientaccesspolicy.xml";
    private static final String REQUEST_PATH_CLIENT_ACCESS_POLICY_XML = format("/%s", RESOURCE_NAME_CLIENT_ACCESS_POLICY_XML);

    private static final String RESOURCE_NAME_PATTERN = "/;resource/%s";

    public static final String REVALIDATE_SUFFIX = "/;a";
    public static final String EMULATED_REVALIDATE_SUFFIX = REVALIDATE_SUFFIX +'e';
    public static final String RTMP_REVALIDATE_SUFFIX = REVALIDATE_SUFFIX +'r';

    private static final Collection<String> REVALIDATE_PATHS = asList(REVALIDATE_SUFFIX,
                                                                      EMULATED_REVALIDATE_SUFFIX,
                                                                      RTMP_REVALIDATE_SUFFIX);

    private static final String EXTENSION_PATH = "/;e/";
    private static final int EXTENSION_PATH_LENGTH = EXTENSION_PATH.length();
    private static final Collection<String> EXTENDED_PATHS = asList("ct", "ut", "dt", "cte", "ute", "dte", "cb", "ub", "db", "ctm", "utm", "dtm", "ctem", "utem", "dtem", "cbm", "ubm", "dbm", "cr");

    private static final String AUTHORIZATION_PATH = "/;a";
    private static final Collection<String> AUTHORIZATION_KINDS = asList("a", "ae", "ar");

    private static final String[] HTTPXE_ENVELOPE_HEADERS = new String[] { HEADER_AUTHORIZATION, HEADER_CONTENT_LENGTH, HEADER_CONTENT_TYPE };

    private static final String PATH_ENCODED_METHOD_PREFIX = "/;";
    private static final int PATH_ENCODED_METHOD_PREFIX_LENGTH = PATH_ENCODED_METHOD_PREFIX.length();

    public static final String API_PATH = "/;api";

    public static final AttributeKey EMPTY_PACKET_PRODUCER_FILTER = new AttributeKey(HttpProtocolCompatibilityFilter.class, "emptyPacketProducerFilter");


    private static final Collection<String> PATH_ENCODED_METHODS;
    static {
        HashSet<String> pathEncodedMethods = new HashSet<>();
        for (HttpMethod httpMethod : HttpMethod.values()) {
            pathEncodedMethods.add(httpMethod.name().toLowerCase());
        }
        PATH_ENCODED_METHODS = pathEncodedMethods;
    }


    private static final Map<String, String> CREATE_ENCODINGS;
    static {
        Map<String, String> createEncodings = new HashMap<>();
        createEncodings.put("ct", "text");
        createEncodings.put("cte", "text-escaped");
        createEncodings.put("cb", "binary");
        createEncodings.put("ctm", "text/mixed");
        createEncodings.put("ctem", "text-escaped/mixed");
        createEncodings.put("cbm", "binary/mixed");
        CREATE_ENCODINGS = unmodifiableMap(createEncodings);
    }

    private static final Logger logger = LoggerFactory.getLogger("transport.http");

    @Override
    public void sessionOpened(NextFilter nextFilter, final IoSession session) throws Exception {

        // For old 3.x clients:
        // Ensure we send back empty packets on httpx responses as soon as the wsn[x-kaazing-handshake] session opens
        IoFilter emptyFilter = (IoFilter)session.getAttribute(EMPTY_PACKET_PRODUCER_FILTER);
        if (emptyFilter != null) {
            session.removeAttribute(EMPTY_PACKET_PRODUCER_FILTER);
            session.getFilterChain().addAfter(HttpAcceptFilter.CODEC.filterName(),
                    "http#emptyPacketProducer",
                    emptyFilter);
            nextFilter.sessionOpened(session);
            return;
        }

        if ( HttpElevateEmulatedRequestFilter.elevateEmulatedRequestRequired(session)) {
            DefaultHttpSession httpSession = (DefaultHttpSession) session;

            nextFilter.sessionOpened(session);
            HttpRequestMessage httpxeRequest = HttpElevateEmulatedRequestFilter.asElevatedRequest(session);


            String contentLengthStr = httpSession.getReadHeader("Content-Length");
            if ( contentLengthStr != null && !"0".equals(contentLengthStr)) {
                if(httpSession.getFilterChain().contains(HttpAcceptFilter.CODEC.filterName())) {
                    session.getFilterChain().addBefore(HttpAcceptFilter.CODEC.filterName(),
                                               HttpAcceptFilter.ELEVATE_EMULATED_REQUEST.filterName(),
                                               new HttpElevateEmulatedRequestFilter(Long.valueOf(contentLengthStr)));
                } else {
                    session.getFilterChain().addFirst(HttpAcceptFilter.ELEVATE_EMULATED_REQUEST.filterName(),
                                                          new HttpElevateEmulatedRequestFilter(Long.valueOf(contentLengthStr)));
                }
            }

            httpRequestReceived(nextFilter,
                                (IoSessionEx)session,
                                httpxeRequest);
        }
    }

    @Override
    protected void httpRequestReceived(NextFilter nextFilter, IoSessionEx session, HttpRequestMessage httpRequest)
            throws Exception {
        // GL.debug("http", getClass().getSimpleName() + " request received.");

        if (deferOriginSecurityToHttpxe(session, httpRequest)) {
            if (session.getFilterChain().contains(HttpAcceptFilter.ORIGIN_SECURITY.filterName())) {
                session.getFilterChain().remove(HttpAcceptFilter.ORIGIN_SECURITY.filterName());
            }
        }

        if (HttpConditionalWrappedResponseFilter.conditionallyWrappedResponsesRequired(session)) {

            // mutate the session to conform to httpxe
            // i.e. mutate the underlying session to be conformant as a outer 'request'
            HttpAcceptSession httpSession = (HttpAcceptSession) session;
            httpSession.setMethod(HttpMethod.POST);

            // KG-3514: X-origin existence implies a wrapped response.
            if (isWrappedResponseMandated(httpSession)) {
                // response must always be wrapped because the request mandates it
                // so do not add conditional wrapping filter.
            } else {
                // install a filter to conditionally wrap the response -
                // sometimes we want to respond wrapped(e.g. create response 401),
                // and other times we do not (e.g. create response 201)

                session.getFilterChain().addBefore(HttpAcceptFilter.PROTOCOL_HTTPXE.filterName(),
                                                   HttpAcceptFilter.CONDITIONAL_WRAPPED_RESPONSE.filterName(),
                                                   HttpAcceptFilter.CONDITIONAL_WRAPPED_RESPONSE.filter());
            }
        }

        // remove random number used to defeat aggressive GET response caching
        httpRequest.removeParameter(QUERY_PARAM_RANDOM_NUMBER);

        // default next-protocol header for backwards compatibility based on content-type
        if (!httpRequest.hasHeader(HEADER_X_NEXT_PROTOCOL)) {
            String contentType = httpRequest.getHeader(HEADER_CONTENT_TYPE);
            if (CONTENT_TYPE_APPLICATION_X_MESSAGE_HTTP.equals(contentType)) {
                httpRequest.setHeader(HEADER_X_NEXT_PROTOCOL, PROTOCOL_HTTPXE_1_1);
            }
        }

        // validate httpe version (if present)
        if (!httpRequest.hasHeader(HEADER_X_NEXT_PROTOCOL)) {
            List<String> httpVersions = httpRequest.removeHeader(HEADER_X_HTTP_VERSION);
            if (httpVersions != null && !httpVersions.isEmpty()) {
                String httpVersion = httpVersions.get(0);

                // TODO: remove this code when we verify that missing binds present a good enough response (404)
                if (!httpVersion.equals(HEADER_VALUE_HTTPE_VERSION_1_0)) {
                    HttpResponseMessage httpResponse = new HttpResponseMessage();
                    httpResponse.setVersion(httpRequest.getVersion());
                    httpResponse.setStatus(HttpStatus.SERVER_NOT_IMPLEMENTED);
                    httpResponse.setReason("Http-Version not supported");

                    WriteFuture writeFuture = new DefaultWriteFuture(session);
                    nextFilter.filterWrite(session, new DefaultWriteRequest(httpResponse, writeFuture));
                    nextFilter.filterClose(session);
                    return;
                }
            }
        }

        if (!httpRequest.hasHeader(HEADER_X_NEXT_PROTOCOL)) {
            List<String> emulatedOrigins = httpRequest.getHeaderValues("X-Origin", false);
            // set whether or not the request should be an emulated response
            // prefer X-Origin header to .ko
            // default to .ko parameter for iframe streaming emulation
            if (emulatedOrigins != null && !emulatedOrigins.isEmpty() && session.getFilterChain().getEntry(WrappedHttpTextEventStreamFilter.getFilterName()) == null) {
                session.getFilterChain().addAfter(HttpAcceptFilter.CODEC.filterName(), WrappedHttpTextEventStreamFilter.getFilterName(), new  WrappedHttpTextEventStreamFilter());
            }
        }

        // default next-protocol header for backwards compatibility with httpe-1.0, wse-1.0,
        if (!httpRequest.hasHeader(HEADER_X_NEXT_PROTOCOL)) {
            URI requestURI = httpRequest.getRequestURI();
            String path = requestURI.getPath();

            int extensionAt = path.indexOf(EXTENSION_PATH);
            if (extensionAt != -1) {
                int extendedPathAt = extensionAt + EXTENSION_PATH_LENGTH;
                int nextSlashAt = path.indexOf('/', extendedPathAt + 1);
                String extendedPath = (nextSlashAt != -1) ? path.substring(extendedPathAt, nextSlashAt) : path.substring(extendedPathAt);
                // TODO: once WsebAcceptor create handler generates upstream and downstream URLs with .knp query parameter,
                //       remove ut, dt, ute, dte, ub, db from extended paths
                if (EXTENDED_PATHS.contains(extendedPath)) {
                    String nextProtocol = getNextProtocol(session);
                    if (PROTOCOL_HTTPXE_1_1.equals(nextProtocol)) {
                        switch (extendedPath.charAt(1)) {
                        case 't':
                        case 'b':
                            // handle ;e balancer bindings by inspecting transport
                            final String sessionPath = BridgeSession.LOCAL_ADDRESS.get(session).getResource().getPath();
                            if (isBalancerPath(sessionPath)) {
                                // balancer requests are terminal so have no next protocol
                                httpRequest.removeHeader(HEADER_X_NEXT_PROTOCOL);
                            } else {
                                // ct, cte, cb, ut, ute, ub, dt, dte, db, cbm, ubm, dbm, ctm, utm, dtm, ctem, utem, dtem
                                httpRequest.setHeader(HEADER_X_NEXT_PROTOCOL, PROTOCOL_WSE_1_0);
                            }

                            if (extendedPath.charAt(0) == 'c') {
                                String createEncoding = CREATE_ENCODINGS.get(extendedPath);
                                httpRequest.setHeader(HEADER_CREATE_ENCODING, createEncoding);
                            }
                            break;

                        }

                        if (extendedPath.charAt(0) == 'c') {
                            //httpRequest.setRequestURI(truncateURI(requestURI, format(";e/%s", extendedPath)));
                        }
                    }
                    else if (!PROTOCOL_HTTPXE_1_1.equals(nextProtocol)) {
                            httpRequest.setHeader(HEADER_X_NEXT_PROTOCOL, PROTOCOL_HTTPXE_1_1);
                    }

                }
            }

            int authorizationAt = path.indexOf(AUTHORIZATION_PATH);
            if (authorizationAt != -1) {
                int nextSlashAt = path.indexOf('/', authorizationAt+2); // skips the '/', ';'
                String authorizationKind = (nextSlashAt != -1) ? path.substring(authorizationAt+2, nextSlashAt) : path.substring(authorizationAt+2);
                if (AUTHORIZATION_KINDS.contains(authorizationKind)) {
                    String nextProtocol = getNextProtocol(session);
                    if (!PROTOCOL_HTTPXE_1_1.equals(nextProtocol)) {
                        httpRequest.setHeader(HEADER_X_NEXT_PROTOCOL, PROTOCOL_HTTPXE_1_1);
                    }
                }
            }

            if (isApiPath(path)) {
                if (httpRequest.getHeader(HttpHeaders.HEADER_X_ORIGIN) != null) {
                    // Remove operation filter at http layer so that request will flow to httpxe layer
                    session.getFilterChain().remove(HttpAcceptFilter.OPERATION.filter());
                    httpRequest.setHeader(HEADER_X_NEXT_PROTOCOL, PROTOCOL_HTTPXE_1_1);
                }
            }
        }

        // support .km query parameter as compatibility *only*
        // since going forward the real method should be placed the POST request body
        if (!httpRequest.hasHeader(HEADER_X_NEXT_PROTOCOL)) {
            emulateMethod(httpRequest);
        }

        // process path-encoded methods for Silverlight emulation
        URI requestURI = httpRequest.getRequestURI();
        String requestPath = requestURI.getPath();
        if (requestPath.startsWith(PATH_ENCODED_METHOD_PREFIX)) {
            // path-encoded-method
            int nextForwardSlashAt = requestPath.indexOf('/', PATH_ENCODED_METHOD_PREFIX_LENGTH);
            if (nextForwardSlashAt != -1) {
                // extract the candidate path-encoded method from the request
                // path
                String pathEncodedMethod = requestPath.substring(PATH_ENCODED_METHOD_PREFIX_LENGTH, nextForwardSlashAt);
                if (PATH_ENCODED_METHODS.contains(pathEncodedMethod)) {
                    // found a valid candidate path-encoded method
                    HttpMethod method = httpRequest.getMethod();
                    HttpMethod newMethod = HttpMethod.valueOf(pathEncodedMethod.toUpperCase());
                    List<String> emulatedOrigins = httpRequest.getHeaderValues("X-Origin", false);

                    String emulatedOrigin = null;
                    if (emulatedOrigins != null &&
                        !emulatedOrigins.isEmpty()) {
                        emulatedOrigin = emulatedOrigins.get(0);
                    }

                    List<String> validatedOrigins;
                    if (emulatedOrigin != null) {
                        String validatedOriginsHeader = "X-Origin-" + URLEncoder.encode(emulatedOrigin, "UTF-8");
                        validatedOrigins = httpRequest.removeHeader(validatedOriginsHeader);
                        if (validatedOrigins == null) {
                            validatedOrigins = Collections.emptyList();
                        }

                    } else {
                        validatedOrigins = Collections.emptyList();
                    }

                    String validatedOrigin = !validatedOrigins.isEmpty() ? validatedOrigins.get(0) : null;
                    // GET -> /;get/... [OK]
                    // GET -> /;post/... [NOT-OK]
                    // GET -> /;delete/... [NOT-OK]
                    // POST -> /;post/... [OK]
                    // DELETE -> /;delete/... [OK]
                    // POST -> /;delete/... [OK]
                    // POST -> /;get/... [OK]
                    if ((newMethod.equals(method) || method == HttpMethod.POST) && validatedOrigin != null
                            && validatedOrigin.equals(emulatedOrigin)) {
                        // update the now-trusted request origin, method and path
                        String requestScheme = requestURI.getScheme();
                        String requestAuthority = requestURI.getAuthority();
                        String newRequestPath = requestPath.substring(nextForwardSlashAt);
                        String requestQuery = requestURI.getQuery();
                        String requestFragment = requestURI.getFragment();
                        URI newRequestURI = new URI(requestScheme, requestAuthority, newRequestPath, requestQuery,
                                requestFragment);
                        httpRequest.setMethod(newMethod);
                        httpRequest.setRequestURI(newRequestURI);
                        httpRequest.setHeader("Origin", validatedOrigin);
                        if (!PROTOCOL_HTTPXE_1_1.equals(getNextProtocol(session))
                                && isEmulatedWebSocketPath(newRequestPath)) {
                            httpRequest.setHeader(HEADER_X_NEXT_PROTOCOL, PROTOCOL_HTTPXE_1_1);
                        }
                    }
                    else {
                        // origin either not supplied or not validated
                        HttpResponseMessage httpResponse = new HttpResponseMessage();
                        httpResponse.setVersion(HttpVersion.HTTP_1_1);
                        httpResponse.setStatus(HttpStatus.CLIENT_FORBIDDEN);

                        // TODO: move this reaction to origin header filter, which would require that Origin is present on filter chain, even for same-origin requests
                        if (logger.isDebugEnabled()) {
                            if (method != HttpMethod.POST &&
                                !newMethod.equals(method)) {
                                logger.debug(String.format("Rejected cross-origin %s request for URI \"%s\" due to mismatch with path-encoded method %s", method, requestURI, newMethod));

                            } else {
                                if (validatedOrigin == null) {
                                    logger.debug(String.format("Rejected cross-origin %s request for URI \"%s\" from emulated origin \"%s\" due to lack of validation (validated origins %s)", method, requestURI, emulatedOrigin, validatedOrigins));

                                } else {
                                    logger.debug(String.format("Rejected cross-origin %s request for URI \"%s\" from emulated origin \"%s\" mismatch with validated origin \"%s\"", method, requestURI, emulatedOrigin, validatedOrigin));
                                }
                            }
                        }

                        filterWrite(nextFilter, session, new DefaultWriteRequestEx(httpResponse, new DefaultWriteFutureEx(session)));
                        return;
                    }
                }
            }
        }

        // Use .ksn for x-sequence-no header for wse/1.0, httpxe/1.1 cases
        // other cases, don't do anything as some services (for e.g http.proxy) want to pass through
        String protocol = httpRequest.getHeader(HEADER_X_NEXT_PROTOCOL);
        if (PROTOCOL_HTTPXE_1_1.equals(protocol) || PROTOCOL_WSE_1_0.equals(protocol)) {
            if (!httpRequest.hasHeader(HttpHeaders.HEADER_X_SEQUENCE_NO)) {
                String candidateSequenceNo = httpRequest.removeParameter(PARAMETER_X_SEQUENCE_NO);
                if (candidateSequenceNo != null) {
                    // if ".ksn" parameter is present, use this value as "X-Sequence-No" header
                    httpRequest.setHeader(HttpHeaders.HEADER_X_SEQUENCE_NO, candidateSequenceNo);
                }
            }
        }

        // convert the resource requests into their version-based equivalents
        // Note: 3.5 javascript client use .kr parameter as a random number instead of .krn in revalidate request. we must allow it to pass through
        String resourceName = httpRequest.removeParameter(QUERY_PARAM_RESOURCE);
        if (resourceName != null && !isRevalidateWebSocketRequest(httpRequest)) {
            URI requestURI1 = httpRequest.getRequestURI();
            URI newRequestURI = requestURI1.resolve(format(RESOURCE_NAME_PATTERN, resourceName));
            httpRequest.setRequestURI(newRequestURI);
        }

        // convert /clientaccesspolicy.xml to a dynamic resource request
        URI requestURI1 = httpRequest.getRequestURI();
        String requestPath1 = requestURI1.getPath();
        if (REQUEST_PATH_CLIENT_ACCESS_POLICY_XML.equals(requestPath1)) {
            URI newRequestURI = requestURI.resolve(format(RESOURCE_NAME_PATTERN, RESOURCE_NAME_CLIENT_ACCESS_POLICY_XML));
            httpRequest.setRequestURI(newRequestURI);
        }

        super.httpRequestReceived(nextFilter, session, httpRequest);
    }

    private boolean deferOriginSecurityToHttpxe(IoSessionEx session, HttpRequestMessage httpRequest) {
        ResourceAddress resourceAddress = BridgeSession.LOCAL_ADDRESS.get(session);
        String nextProtocol = resourceAddress.getOption(NEXT_PROTOCOL);
        return "http/1.1".equals(nextProtocol) &&
                isLegacyClient(httpRequest) &&
                (isEmulatedWebSocketRequest(httpRequest) || isRevalidateWebSocketRequest(httpRequest));
    }


    private boolean isWrappedResponseMandated(HttpAcceptSession httpSession) {
        return null != httpSession.getReadHeader("X-Origin");
    }

    private String getNextProtocol(IoSession session) {
        if (session instanceof BridgeSession) {
            ResourceAddress localAddress = ((BridgeSession) session).getLocalAddress();
            return localAddress.getOption(NEXT_PROTOCOL);
        }

        return null;
    }


    private void emulateMethod(HttpRequestMessage httpRequest) {
        // check for method override from client
        if (httpRequest.getMethod() == HttpMethod.POST) {
            String method = httpRequest.getParameter(QUERY_PARAM_METHOD);
            if (method != null && method.length() == 1) {
                switch (method.charAt(0)) {
                case 'G':
                    httpRequest.setMethod(HttpMethod.GET);
                    //httpRequest.removeParameter(QUERY_PARAM_METHOD);
                    break;
                case 'D':
                    httpRequest.setMethod(HttpMethod.DELETE);
                    //httpRequest.removeParameter(QUERY_PARAM_METHOD);
                    break;
                case 'O':
                    httpRequest.setMethod(HttpMethod.OPTIONS);
                    //httpRequest.removeParameter(QUERY_PARAM_METHOD);
                    break;
                case 'P':
                    httpRequest.setMethod(HttpMethod.PUT);
                    //httpRequest.removeParameter(QUERY_PARAM_METHOD);
                    break;
                case 'T':
                    httpRequest.setMethod(HttpMethod.TRACE);
                    //httpRequest.removeParameter(QUERY_PARAM_METHOD);
                    break;
                case 'H':
                    httpRequest.setMethod(HttpMethod.HEAD);
                    //httpRequest.removeParameter(QUERY_PARAM_METHOD);
                    break;
                }
            }
        }
    }

    static class HttpElevateEmulatedRequestFilter extends IoFilterAdapter<DefaultHttpSession> {

        long contentLength;

        public HttpElevateEmulatedRequestFilter(long contentLength) {
            this.contentLength = contentLength;
        }

        @Override
        public void onPreRemove(IoFilterChain parent, String name, NextFilter nextFilter) throws Exception {
            super.onPreRemove(parent, name, nextFilter);    //To change body of overridden methods use File | Settings | File Templates.
        }

        @Override
        protected void doMessageReceived(NextFilter nextFilter,
                                         DefaultHttpSession session,
                                         Object message) throws Exception {
            IoBufferEx buffer = (IoBufferEx) message;

            long bytesRead = session.getReadBytes();
            if (bytesRead > contentLength) {
                String format = format("httpxe/1.1 expecting %d content bytes, but we have read %d bytes",
                                       contentLength,
                                       bytesRead);
                throw new RuntimeException(format);
            }

            HttpContentMessage httpContentMessage = new HttpContentMessage(buffer, bytesRead==contentLength);
            nextFilter.messageReceived(session, httpContentMessage);
        }



        static boolean elevateEmulatedRequestRequired(IoSession session) {
            if ("httpxe/1.1".equals(BridgeSession.LOCAL_ADDRESS.get(session).getOption(NEXT_PROTOCOL))) {
                DefaultHttpSession httpSession = (DefaultHttpSession) session;
                // XDR needs to explicitly envelope the request, so we must not treat this as requiring implicit elevation
                return !isExplicitEnvelopedContentType(httpSession) &&
                        isLegacyClient(httpSession) &&
                        (isEmulatedWebSocketRequest(httpSession) || isRevalidateWebSocketRequest(httpSession) || isApiRequest(httpSession));
            }
            return false;
        }

        static HttpRequestMessage asElevatedRequest(IoSession ioSession) throws Exception {
            DefaultHttpSession session = (DefaultHttpSession) ioSession;

            // build up a valid httpxe request
            HttpRequestMessage req = new HttpRequestMessage();
            req.setVersion(session.getVersion());
            req.setMethod(session.getMethod());
            req.setParameters(session.getParameters());
            req.setRequestURI(session.getRequestURI());
            req.setSecure(session.isSecure());
            req.setCookies(session.getReadCookies());
            // Populate with incomplete content message so that HttpRequestMessage.isComplete() returns correctly
            // This logic is based on Content-Length, otherwise, DefaultHttpSession needs to be populated
            // when it is constructed in HttpAcceptor and then use it to populate here.
            String contentLengthStr = session.getReadHeader("Content-Length");
            if (contentLengthStr != null && !"0".equals(contentLengthStr)) {
                IoBufferAllocatorEx<? extends HttpBuffer> allocator = session.getBufferAllocator();
                HttpContentMessage httpContent = new HttpContentMessage(allocator.wrap(allocator.allocate(0)), false);
                req.setContent(httpContent);
            }

            // establish all the headers (mutable) and then restrict to just the ones we want.
            Map<String,List<String>> requestHeaders = new HashMap<>(session.getReadHeaders());
            req.setHeaders(requestHeaders);
            HttpUtils.restrictHeaders(req, HTTPXE_ENVELOPE_HEADERS);

            // Propagate the subject and login context onto the new httprequest for use in HttpSubjectSecurityFilter
            req.setSubject(session.getSubject());
            req.setLoginContext(session.getLoginContext());

            // mutate the session, defer changing the GET method to a post if there is a GET method.
            // that will be handled at the httpxe-filter-chain's protocol compat filter.
            // for now, make sure the outer content type is valid httpxe value
            DefaultHttpSession httpSession = session;
            Map<String,List<String>> sessionHeaders = new HashMap<>(httpSession.getReadHeaders());
            sessionHeaders.put(HEADER_CONTENT_TYPE, Collections.singletonList(CONTENT_TYPE_APPLICATION_X_MESSAGE_HTTP));
            httpSession.setReadHeaders(sessionHeaders);

            return req;
        }

    }

    private static boolean isEmulatedWebSocketRequest(HttpRequestMessage message) {
        return isEmulatedWebSocketPath(message.getRequestURI().getPath());
    }
    private static boolean isEmulatedWebSocketRequest(HttpAcceptSession session) {
        // EmulatedWebSocket requests need elevating.
        // they're elevated from the session data.
        return isEmulatedWebSocketPath(session.getRequestURI().getPath());
    }

    private static boolean isApiRequest(HttpAcceptSession session) {
        // /;api requests need elevating.
        // they're elevated from the session data.
        return isApiPath(session.getRequestURI().getPath());
    }

    private static boolean isRevalidateWebSocketRequest(HttpRequestMessage message) {
        return isRevalidateWebSocketPath(message.getRequestURI().getPath());
    }
    private static boolean isRevalidateWebSocketRequest(HttpAcceptSession session) {
        // Revalidate requests need elevating.
        // they're elevated from the session data.
        return isRevalidateWebSocketPath(session.getRequestURI().getPath());
    }

    private static boolean isBalancerRequest(HttpAcceptSession session) {
        return isBalancerPath(session.getRequestURI().getPath());
    }

    private static boolean isRevalidateWebSocketPath(String path) {
        if ( path != null ) {
            int extensionAt = path.indexOf(REVALIDATE_SUFFIX);
            if (extensionAt != -1) {
                int nextSlashAt = path.indexOf('/', extensionAt + 1);
                String revalidatePath = (nextSlashAt != -1) ?
                        path.substring(extensionAt, nextSlashAt) :
                        path.substring(extensionAt);

                return REVALIDATE_PATHS.contains(revalidatePath);
            }
        }
        return false;
    }

    private static boolean isBalancerPath(String path) {
        return path != null && path.endsWith(";e");
    }

        private static boolean isEmulatedWebSocketPath(String path) {
        if ( path != null ) {
            int extensionAt = path.indexOf(EXTENSION_PATH);
            if (extensionAt != -1) {
                int extendedPathAt = extensionAt + EXTENSION_PATH_LENGTH;
                int nextSlashAt = path.indexOf('/', extendedPathAt + 1);
                String extendedPath = (nextSlashAt != -1) ?
                        path.substring(extendedPathAt, nextSlashAt) :
                        path.substring(extendedPathAt);

                return EXTENDED_PATHS.contains(extendedPath);
            }
        }
        return false;
    }

    private static boolean isApiPath(String path) {
        return path != null && path.contains(API_PATH);
    }

    public static class HttpConditionalWrappedResponseFilter extends HttpFilterAdapter<IoSessionEx> {

        public static boolean conditionallyWrappedResponsesRequired(IoSessionEx session) {
            ResourceAddress resourceAddress = BridgeSession.LOCAL_ADDRESS.get(session);
            String nextProtocol = resourceAddress.getOption(NEXT_PROTOCOL);
            if ("httpxe/1.1".equals(nextProtocol)) {
                HttpAcceptSession httpSession = (HttpAcceptSession) session;
                // note: would prefer to use explicit "application/x-message-http" content type header,
                //       but that has already been faulted in by compatibility filter before the test
                //       is made for conditionally wrapped response
                return //!isExplicitEnvelopedContentType(httpSession) &&
                       !isExplicitAccessControl(httpSession) &&
                        isLegacyClient(httpSession) &&
                        (httpSession.getMethod() == HttpMethod.GET // java clients
                         || isEmulatedWebSocketRequest(httpSession)
                         || isRevalidateWebSocketRequest(httpSession)
                         || isBalancerRequest(httpSession)
                         );
            }
            return false;
        }

        @Override
        protected void filterWriteHttpResponse(NextFilter nextFilter,
                                               IoSessionEx session,
                                               WriteRequest writeRequest,
                                               HttpResponseMessage httpResponse) throws Exception {

            DefaultHttpSession httpSession = (DefaultHttpSession) session;

            switch(httpResponse.getStatus()) {
            case CLIENT_UNAUTHORIZED:
                // we want wrapping so remove this filter
                session.getFilterChain().remove(this);
                break;

            case REDIRECT_FOUND:
            case CLIENT_BAD_REQUEST:
            case SUCCESS_CREATED:
                boolean httpxeSpecCompliant = httpSession.isHttpxeSpecCompliant();
                if (httpxeSpecCompliant) {
                    // we want wrapping so remove this filter
                    session.getFilterChain().remove(this);
                    break;
                }
                // fall-through
            default:

                // we do not want wrapped responses so push data to parent http/1.1 session
                httpSession.setStatus(httpResponse.getStatus());
                httpSession.setVersion(httpResponse.getVersion());
                Map<String,List<String>> remainingHttpxeLevelHeaders = httpResponse.getHeaders();

                // take care to add headers (not trample them) from the
                // httpResponse to this session to flatten this session
                if (remainingHttpxeLevelHeaders != null) {
                    for (String headerName: remainingHttpxeLevelHeaders.keySet()) {
                        List<String> values = remainingHttpxeLevelHeaders.get(headerName);
                        if ( values != null ) {
                            for ( String value: values) {
                                httpSession.addWriteHeader(headerName,value);
                            }
                        }
                    }

                    // remove duplicate vlaues if they exist
                    Map<String, List<String>> writeHeaders = httpSession.getWriteHeaders();
                    for (String headerName: writeHeaders.keySet()) {
                        List<String> possiblyDuplicateStrings = writeHeaders.get(headerName);
                        if ( possiblyDuplicateStrings != null ) {
                            List<String> values = new ArrayList<>(new HashSet<>(possiblyDuplicateStrings));
                            writeHeaders.put(headerName, values);
                        }
                    }
                }

                // take care to add httpxe-level cookies
                HashSet<HttpCookie> httpxeAndSessionCookies = new HashSet<>(httpSession.getWriteCookies());
                httpxeAndSessionCookies.addAll(httpResponse.getCookies());
                httpSession.setWriteCookies(httpxeAndSessionCookies);

                httpSession.setReason(httpResponse.getReason());

                // now after header additions, do we have a content length?  if so no adjustments needed -> remove filter
                boolean hasExplicitDefinedContentLength = httpSession.getWriteHeader(HEADER_CONTENT_LENGTH) != null;
                if ( hasExplicitDefinedContentLength ) {
                    IoFilterChain chain = session.getFilterChain();
                    IoFilter filter = HttpAcceptFilter.CONTENT_LENGTH_ADJUSTMENT.filter();
                    if (chain.contains(filter)) {
                        session.getFilterChain().remove(filter);
                    }
                }

                // now tie an empty buffer to the http response message cache to avoid it getting encoded.
                if (!httpResponse.hasCache()) {
                    httpResponse.initCache();
                }

                IoBufferAllocatorEx<?> allocator = httpSession.getBufferAllocator();

                IoBufferEx oldBuffer = httpResponse.getCache().putIfAbsent("httpxe/1.1", allocator.wrap(allocator.allocate(0)));
                if (oldBuffer != null) {
                    if (logger.isDebugEnabled()) {
                        String msgFormat = "Unexpected existing buffer associated with old websocket "
                        +"emulated create response: '%s'";
                        logger.warn(format(msgFormat, oldBuffer));
                    }
                }
            }
            super.filterWriteHttpResponse(nextFilter, session, writeRequest, httpResponse);
        }
    }

    private static boolean isExplicitEnvelopedContentType(HttpAcceptSession httpSession) {
        return CONTENT_TYPE_APPLICATION_X_MESSAGE_HTTP.equals(httpSession.getReadHeader(HEADER_CONTENT_TYPE));
    }

    private static boolean isExplicitAccessControl(HttpAcceptSession httpSession) {
        return "ex".equals(httpSession.getParameter(".kac"));
    }

    private static boolean isLegacyClient(HttpAcceptSession httpSession) {
        // detect old clients
        boolean hasOld3xClientVersionParam = "10.05".equals(httpSession.getParameter(".kv"));
        boolean hasOld3xBalancerRequestParameter = httpSession.getParameter(".kl") != null;

        // detect old connectors
        boolean hasNextProtocolDefined =
                (null != httpSession.getReadHeader("X-Next-Protocol") ||
                 null != httpSession.getParameter(".knp"));

        return hasOld3xClientVersionParam || hasOld3xBalancerRequestParameter || !hasNextProtocolDefined;
    }

    private static boolean isLegacyClient(HttpRequestMessage httpRequest) {
        boolean hasOld3xClientVersionParam = "10.05".equals(httpRequest.getParameter(".kv"));
        boolean hasOld3xBalancerRequestParameter = httpRequest.getParameter(".kl") != null;
        boolean hasNextProtocolDefined =
                (null != httpRequest.getHeader("X-Next-Protocol") ||
                        null != httpRequest.getParameter(".knp"));

        return hasOld3xClientVersionParam || hasOld3xBalancerRequestParameter || !hasNextProtocolDefined;
    }

    public static class WrappedHttpTextEventStreamFilter extends HttpFilterAdapter<IoSessionEx> {
        private static final Charset UTF_8 = Charset.forName("UTF-8");
        private static final CharsetEncoder UTF_8_ENCODER = UTF_8.newEncoder();
        private static final CharSequence WRAPPED_HTTP = "HTTP/1.1 200 OK\r\nContent-Type: text/event-stream\r\n\r\n";
        private static final String FILTER_NAME = "WrappedHttpTextEventStreamFilter";
        private static final int TO_ALLOCATE = WRAPPED_HTTP.length();

        @Override
        protected void filterWriteHttpResponse(NextFilter nextFilter, IoSessionEx session, WriteRequest writeRequest,
                HttpResponseMessage httpResponse) throws Exception {
            if (httpResponse.getStatus() == HttpStatus.SUCCESS_OK
                    && "text/event-stream".equals(httpResponse.getHeader(HEADER_CONTENT_TYPE))) {
                IoBufferAllocatorEx<?> allocator = session.getBufferAllocator();
                allocator.allocate(TO_ALLOCATE);
                IoBufferEx buffer = httpResponse.getContent().asBuffer();
                buffer.expand(0, TO_ALLOCATE, allocator);
                buffer.putString(WRAPPED_HTTP, UTF_8_ENCODER);
                buffer.flip();
            }
            super.filterWriteHttpResponse(nextFilter, session, writeRequest, httpResponse);
        }

        public static String getFilterName() {
            return FILTER_NAME;
        }

    }

}

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
package org.kaazing.gateway.transport.http;

import static java.lang.Math.abs;
import static java.lang.Math.floor;
import static org.kaazing.gateway.transport.http.HttpHeaders.HEADER_CONTENT_LENGTH;
import static org.kaazing.gateway.transport.http.HttpStatus.CLIENT_NOT_FOUND;
import static org.kaazing.gateway.transport.http.HttpStatus.REDIRECT_NOT_MODIFIED;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TimeZone;

import org.apache.mina.core.session.IoSession;
import org.kaazing.gateway.resource.address.uri.URIUtils;
import org.kaazing.gateway.transport.SslUtils;
import org.kaazing.gateway.transport.http.bridge.HttpContentMessage;
import org.kaazing.gateway.transport.http.bridge.HttpRequestMessage;
import org.kaazing.gateway.transport.http.bridge.HttpResponseMessage;
import org.kaazing.mina.core.buffer.IoBufferAllocatorEx;
import org.kaazing.mina.core.buffer.IoBufferEx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpUtils {
    
    // An optional header for requests to the gateway to turn on long-polling
    // X-Kaazing-Proxy-Buffering: on | off
    // "on" means an intermediary between the client and gateway is buffering.
    // "off" is the default value
    private static final String PROXY_BUFFERING_HEADER = "X-Kaazing-Proxy-Buffering";
    private static final Logger LOGGER = LoggerFactory.getLogger(HttpUtils.class);

    private static final Charset UTF_8 = Charset.forName("UTF-8");
    private static final Random SESSION_SEQUENCE = new SecureRandom();
    private static final char[] BASE_62_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789".toCharArray();
    private static final int BASE_62_CHARS_LENGTH = BASE_62_CHARS.length;

	private static final DateFormat[] RFC822_PARSE_PATTERNS = new DateFormat[] {
            new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.ENGLISH),
            new SimpleDateFormat("EEE, d MMM yy HH:mm:ss z", Locale.ENGLISH),
            new SimpleDateFormat("EEE, d MMM yy HH:mm z", Locale.ENGLISH),
            new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss z", Locale.ENGLISH),
            new SimpleDateFormat("EEE, d MMM yyyy HH:mm z", Locale.ENGLISH),
            new SimpleDateFormat("d MMM yy HH:mm z", Locale.ENGLISH),
            new SimpleDateFormat("d MMM yy HH:mm:ss z", Locale.ENGLISH),
            new SimpleDateFormat("d MMM yyyy HH:mm z", Locale.ENGLISH),
            new SimpleDateFormat("d MMM yyyy HH:mm:ss z", Locale.ENGLISH)
    };

    private static final DateFormat RFC822_FORMAT_PATTERN =
            new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.ENGLISH);
    static {
        RFC822_FORMAT_PATTERN.setTimeZone(TimeZone.getTimeZone("GMT"));
    }

    public static String getHostDomain(HttpRequestMessage httpRequest) {
        String host = httpRequest.getHeader("Host");
        int index = host.indexOf(':');
        if (index != -1) {
            host = host.substring(0, index);
        }
        return host;
    }

    public static String getHostAndPort(HttpRequestMessage httpRequest, boolean secure) {
        String authority = httpRequest.getHeader("Host");
        return getHostAndPort(authority, secure);
    }

    public static String getHostAndPort(String authority, boolean secure) {
        // default port if necessary
        if (authority != null && authority.indexOf(':') == -1) {
            int port = secure ? 443 : 80;
            authority += ":" + port;
        }
        return authority;
    }

    public static long parseDateHeader(String header) {
        if (header != null && header.length() > 0) {
            for (DateFormat rfc822Format : RFC822_PARSE_PATTERNS) {
                try {
                    Date headerDate = rfc822Format.parse(header);
                    return headerDate.getTime();
                } catch (NumberFormatException e) {
                    // ignore, try next RFC822 format
                    continue;
                } catch (ParseException e) {
                    // ignore, try next RFC822 format
                    continue;
                }
            }
        }

        throw new IllegalArgumentException("Unable to parse date header: " + header);
    }

    public static String formatDateHeader(long millis) {
        return RFC822_FORMAT_PATTERN.format(millis);
    }

    public static void fileRequested(IoBufferAllocatorEx<?> allocator, HttpRequestMessage httpRequest,
        HttpResponseMessage httpResponse, File requestFile) throws IOException {
        if (requestFile.isFile() && requestFile.exists()) {
            String etag = getETagHeaderValue(requestFile);
            String ifNoneMatch = httpRequest.getHeader("If-None-Match");
            String ifModifiedSince = httpRequest.getHeader("If-Modified-Since");
            if (!hasBeenModified(requestFile, etag, ifNoneMatch, ifModifiedSince)) {
                httpResponse.setHeader("ETag", etag);
                httpResponse.setStatus(HttpStatus.REDIRECT_NOT_MODIFIED);
            } else {
                FileInputStream in = new FileInputStream(requestFile);

                byte[] buf = new byte[8192];
                IoBufferEx out = allocator.wrap(allocator.allocate(in.available())).setAutoExpander(allocator);
                int length;
                while ((length = in.read(buf)) > 0) {
                    out.put(buf, 0, length);
                }
                out.flip();
                in.close();

                httpResponse.setHeader("ETag", etag);
                httpResponse.setHeader("Last-Modified", RFC822_FORMAT_PATTERN.format(requestFile.lastModified()));
                httpResponse.setHeader("Expires", RFC822_FORMAT_PATTERN.format(System.currentTimeMillis()));
                httpResponse.setContent(new HttpContentMessage(out, true));

                // Note: callers are responsible for adding the Content-Type header,
                // per KG-866. See HttpCrossSiteBridgeFilter for an example.
            }
        } else {
            httpResponse.setStatus(HttpStatus.CLIENT_NOT_FOUND);
        }
    }

	// ported from httpFileRequested (above)
    public static void writeIfModified(HttpAcceptSession httpSession, File requestFile) throws IOException {
        if (requestFile.isFile() && requestFile.exists()) {
            String etag = getETagHeaderValue(requestFile);
            String ifNoneMatch = httpSession.getReadHeader("If-None-Match");
            String ifModifiedSince = httpSession.getReadHeader("If-Modified-Since");
            if (!hasBeenModified(requestFile, etag, ifNoneMatch, ifModifiedSince)) {
                httpSession.setWriteHeader("ETag", etag);
                httpSession.setStatus(REDIRECT_NOT_MODIFIED);
            }
            else {
                FileInputStream in = new FileInputStream(requestFile);

                byte[] buf = new byte[8192];
                IoBufferAllocatorEx<?> allocator = httpSession.getBufferAllocator();
                IoBufferEx out = allocator.wrap(allocator.allocate(in.available())).setAutoExpander(allocator);
                int length;
                while ((length = in.read(buf)) > 0) {
                    out.put(buf, 0, length);
                }
                out.flip();
                in.close();
                httpSession.setWriteHeader("ETag", etag);
                httpSession.setWriteHeader("Last-Modified", RFC822_FORMAT_PATTERN.format(requestFile.lastModified()));
                httpSession.setWriteHeader("Expires", RFC822_FORMAT_PATTERN.format(System.currentTimeMillis()));
                httpSession.suspendWrite();
                httpSession.write(out);
                httpSession.shutdownWrite();
                httpSession.resumeWrite();

                // Note: callers are responsible for adding the Content-Type header,
                // per KG-866.  See HttpCrossSiteBridgeFilter for an example.
            }
        }
        else {
            httpSession.setStatus(CLIENT_NOT_FOUND);
        }
    }

	// TODO: should be able to remove this once we can send File down the pipe
    public static IoBufferEx getBufferForFile(IoBufferAllocatorEx<?> allocator, File requestFile) throws IOException {
        FileInputStream in = new FileInputStream(requestFile);
        IoBufferEx out = allocator.wrap(allocator.allocate(in.available())).setAutoExpander(allocator);
        try {
            int pos = out.position();
            byte[] buf = new byte[1024 * 8];
            int length;
            while ((length = in.read(buf)) > 0) {
                out.put(buf, 0, length);
            }
            out.position(pos);
        } finally {
            in.close();
        }

        return out;
    }

    public static boolean hasBeenModified(HttpSession session, String etag, File requestFile) {
        String ifNoneMatch = session.getReadHeader("If-None-Match");
        String ifModifiedSince = session.getReadHeader("If-Modified-Since");
        return hasBeenModified(requestFile, etag, ifNoneMatch, ifModifiedSince);
    }

    private static boolean hasBeenModified(File requestFile, String eTag, String ifNoneMatch, String ifModifiedSince) {
        // "*" indicates skip ETag check, just use if-modified-since semantics, if present
        if (ifNoneMatch != null && !"*".equals(ifNoneMatch)) {
            // if ETag match is found, then not modified
            String[] candidateETags = ifNoneMatch.split(",\\s?");
            for (String candidateETag : candidateETags) {
                if (candidateETag.equals(eTag)) {
                    return false;
                }
            }

            // if no ETag match is found, then must not return 304 (Not Modified) response
            return true;
        }

        // no modified header sent
        if (ifModifiedSince == null || ifModifiedSince.length() == 0) {
            return true;
        }

        long lastModified = requestFile.lastModified();
        Date ifModifiedSinceDate = null;

        // parse date format
        for (DateFormat rfc822Format : RFC822_PARSE_PATTERNS) {
            try {
                ifModifiedSinceDate = rfc822Format.parse(ifModifiedSince);
            } catch (NumberFormatException e) {
                // ignore, try next RFC822 format
                continue;
            } catch (ParseException e) {
                // ignore, try next RFC822 format
                continue;
            }
            break;
        }

        // could not parse date
        if (ifModifiedSinceDate == null) {
            return true;
        }

        // check modified time against file last modified
        double time_difference = floor(abs(lastModified - ifModifiedSinceDate.getTime()) / 1000);
        return time_difference > 0;
    }

    public static void addLastModifiedHeader(HttpSession session, File requestFile) {
        long lastModified = requestFile.lastModified();
        session.setWriteHeader("Last-Modified", RFC822_FORMAT_PATTERN.format(lastModified));
    }

    public static String getETagHeaderValue(File requestFile) {
        long lastModified = requestFile.lastModified();
        String absolutePath = requestFile.getAbsolutePath();

        // construct the MDS hash
        ByteBuffer buf = ByteBuffer.allocate(16);
        MessageDigest algorithm = getMD5();
        algorithm.reset();
        algorithm.update(absolutePath.getBytes(UTF_8));
        buf.putLong(lastModified).flip();
        algorithm.update(buf);
        byte[] digest = algorithm.digest();
        buf.position(0);
        buf.limit(buf.capacity());
        buf.put(digest);
        buf.flip();

        // set ETag header value (weak validator for now, hence "W/" prefix)
        StringBuilder builder = new StringBuilder();
        builder.append("W/\"");
        builder.append(Long.toHexString(buf.getLong()));
        builder.append(Long.toHexString(buf.getLong()));
        builder.append('"');
        String headerValue = builder.toString();
        return headerValue;
    }

    public static void supplyScriptAsHtml(File xdBridgeFile, long startTime, String resourcePath) throws IOException {
        // include the resource name as a heading to aid integration setup verification
        String bridgeFileName = xdBridgeFile.getName();
        String heading = bridgeFileName.substring(0, bridgeFileName.lastIndexOf('.'));
        String preamble = "<html><head></head><body><script>";
        String postamble = "</script><h3>" + heading + "</h3></body></html>";
        supplyBridgeFile(xdBridgeFile, startTime, resourcePath, preamble, postamble);
    }

    public static void supplyFile(File bridgeFile, long startTime, String resourcePath) throws IOException {
        supplyBridgeFile(bridgeFile, startTime, resourcePath, null, null);
    }

    private static void supplyBridgeFile(File xdBridgeFile, long startTime, String resourcePath, String preamble,
        String postamble) throws IOException {
        if (!xdBridgeFile.exists() || xdBridgeFile.lastModified() < startTime) {
            ClassLoader loader = HttpUtils.class.getClassLoader();
            URL resource = loader.getResource(resourcePath);
            if (resource == null) {
                HttpUtils.LOGGER.error("Unable to find resource on classpath: " + resourcePath);
            } else {
                xdBridgeFile.getParentFile().mkdirs();
                InputStream in = resource.openStream();
                FileOutputStream fos = new FileOutputStream(xdBridgeFile);
                // write header
                if (preamble != null) {
                    fos.write(preamble.getBytes());
                }
                // write script contents
                byte[] buf = new byte[8192];
                while (true) {
                    int len = in.read(buf);
                    if (len <= 0) {
                        break;
                    }
                    fos.write(buf, 0, len);
                }
                // write footer
                if (postamble != null) {
                    fos.write(postamble.getBytes());
                }
                in.close();
                fos.close();
                xdBridgeFile.getParentFile().mkdirs();
            }
        }
    }

    public static boolean canStream(HttpSession session) {
        if ("p".equals(session.getParameter(".ki"))) {
            return false;
        }
        String responseMode = session.getReadHeader(PROXY_BUFFERING_HEADER);
        if (responseMode != null) {
            assert responseMode.equals("on") || responseMode.equals("off");
            return responseMode.equals("off");
        }
        return true;
    }

    public static String newSessionId() {
        // base-62, 32 chars long, random
        int size = 32;
        StringBuilder sessionId = new StringBuilder(size);
        for (int i = 0; i < size; i++) {
            int randomInt = Math.abs(SESSION_SEQUENCE.nextInt());
            sessionId.append(BASE_62_CHARS[randomInt % BASE_62_CHARS_LENGTH]);
        }
        return sessionId.toString();
    }

    // constructs an http specific request uri with host, port (or explicit default port), and path
    public static URI getRequestURI(HttpRequestMessage request, IoSession session) {
        URI requestURI = request.getRequestURI();
        String host = request.getHeader("Host");
        return getRequestURI(requestURI, host, session);
    }

    // constructs an http specific request uri with host, port (or explicit default port), and path
    public static URI getRequestURI(URI requestURI, String hostHeader, IoSession session) {
        boolean secure = SslUtils.isSecure(session);
        String authority = HttpUtils.getHostAndPort(hostHeader, secure);
        // Use getRawPath to get the un-decoded path; getPath returns the post-decode value.
        // This is required to handle special characters like spaces in the URI (KG-831).
        URI uri = URI.create("//" + authority + requestURI.getRawPath());
        return uri;
    }

    /*
     * Returns whether there is Connection: close header
     *
     * @param list of Connection header values
     * 
     * @return true if Connection: close header is part of the values
     */
    public static boolean hasCloseHeader(List<String> connectionValues) {
        if (connectionValues == null) {
            return false;
        }
        return connectionValues.stream().anyMatch(v -> v.equalsIgnoreCase("close"));
    }

    public static URI getTransportURI(HttpRequestMessage request, IoSession session) {
        URI requestURI = request.getRequestURI();
        String hostHeader = request.getHeader("Host");
        boolean secure = SslUtils.isSecure(session);
        String authority = HttpUtils.getHostAndPort(hostHeader, secure);
        // Use getRawPath to get the un-decoded path; getPath returns the post-decode value.
        // This is required to handle special characters like spaces in the URI (KG-831).
        return URI.create("http://" + authority + requestURI.getRawPath());
    }

    public static boolean isChunked(String transferEncoding) {
        if (transferEncoding != null) {
            int semicolonAt = transferEncoding.indexOf(';');
            if (semicolonAt != -1) {
                transferEncoding = transferEncoding.substring(0, semicolonAt);
            }

            if ("chunked".equalsIgnoreCase(transferEncoding)) {
                return true;
            }
        }

        return false;
    }

    public static boolean isChunked(HttpResponseMessage response) {
        String transferEncoding = response.getHeader("Transfer-Encoding");
        return isChunked(transferEncoding) && HttpVersion.HTTP_1_1.equals(response.getVersion());
    }

    public static boolean isGzipped(HttpResponseMessage response) {
        return response.isBlockPadding();
    }

    /**
     * Create a canonical URI from a given URI.   A canonical URI is a URI with:<ul>
     * <li>the host part of the authority lower-case since URI semantics dictate that hostnames are case insensitive
     * <li>(optionally, NOT appropriate for Origin headers) the path part set to "/" except for tcp uris if there was no path in the
     * input URI (this conforms to the WebSocket and HTTP protocol specifications and avoids us having to do special
     * handling for path throughout the server code).
     * </ul>
     * @param uri               the URI to canonicalize
     * @param canonicalizePath  if true, append trailing '/' when missing
     * @return a URI with the host part of the authority lower-case and (optionally if not tcp) trailing / added, or null if the uri is null
     * @throws IllegalArgumentException if the uri is not valid syntax
     */
    public static URI getCanonicalURI(URI uri, boolean canonicalizePath) {
        URI canonicalURI = uri;
        if (uri != null) {
            String host = uri.getHost();
            String path = uri.getPath();
            final boolean emptyPath = "".equals(path);
            final boolean noPathToCanonicalize = canonicalizePath && (path == null || emptyPath);
            final boolean trailingSlashPath = "/".equals(path);
            final boolean pathlessScheme = "ssl".equals(uri.getScheme()) || "tcp".equals(uri.getScheme())
                    || "pipe".equals(uri.getScheme()) || "udp".equals(uri.getScheme());
            final boolean trailingSlashWithPathlessScheme = trailingSlashPath && pathlessScheme;
            String newPath = trailingSlashWithPathlessScheme ? "" : noPathToCanonicalize ? (pathlessScheme ? null : "/") : null;
            if (((host != null) && !host.equals(host.toLowerCase())) || newPath != null) {
                path = newPath == null ? path : newPath;
                try {
                    canonicalURI = new URI(uri.getScheme(), uri.getUserInfo(), host == null ? null : host.toLowerCase(),
                            uri.getPort(), path, uri.getQuery(), uri.getFragment());
                } catch (URISyntaxException ex) {
                    throw new IllegalArgumentException("Invalid URI: " + uri + " in Gateway configuration file", ex);
                }
            }
        }
        return canonicalURI;
    }

    /**
     * Create a canonical URI from a given URI.   A canonical URI is a URI with:<ul>
     * <li>the host part of the authority lower-case since URI semantics dictate that hostnames are case insensitive
     * <li>(optionally, NOT appropriate for Origin headers) the path part set to "/" if there was no path in the
     * input URI (this conforms to the WebSocket and HTTP protocol specifications and avoids us having to do special
     * handling for path throughout the server code).
     * </ul>
     * @param uriString         the URI to canonicalize, in string form
     * @param canonicalizePath  if true, append trailing '/' when missing
     * @return a URI with the host part of the authority lower-case and (optionally) trailing / added, or null if the uri is null
     * @throws IllegalArgumentException if the uriString is not valid syntax
     */
    public static URI getCanonicalURI(String uriString, boolean canonicalizePath) {
        if ((uriString != null) && !"".equals(uriString)) {
            return getCanonicalURI(URI.create(uriString), canonicalizePath);
        }
        return null;
    }

    private static MessageDigest getMD5() {
        try {
            return MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public static String[] getPathComponents(URI uri) {
        if (uri == null || uri.getPath() == null) {
            return new String[0];
        }

        return uri.getPath().split("/");
    }

    public static String getAuthenticationTokenFromPath(URI uri) {
        String urlEncodedToken = null;
        String[] pathComponents = getPathComponents(uri);
        if (pathComponents != null && pathComponents.length >= 3) {
            if (pathComponents[pathComponents.length - 3].equals(";e")
                    && pathComponents[pathComponents.length - 2].startsWith("u")
                    || pathComponents[pathComponents.length - 2].startsWith("d")) {
                urlEncodedToken = pathComponents[pathComponents.length - 1];
            }
        }
        return urlEncodedToken;
    }

    public static boolean containsForbiddenHeaders(HttpRequestMessage request, String[] allowedHeaders) {
        if (request == null) {
            throw new IllegalArgumentException("Request is null.");
        }

        Map<String, List<String>> headers = request.getHeaders();
        if (headers == null || headers.size() == 0) {
            return false;
        }

        boolean allowedHeadersEmpty = (allowedHeaders == null || allowedHeaders.length == 0);

        for (String header : headers.keySet()) {

            boolean headerIsForbidden = isForbiddenHeader(header);
            boolean headerIsAllowed = false;

            if (headerIsForbidden) {
                if (!allowedHeadersEmpty) {
                    // Is the header an exception? If so, it is not fobidden
                    for (String allowedHeader : allowedHeaders) {
                        if (allowedHeader.equalsIgnoreCase(header)) {
                            headerIsAllowed = true;
                            break;
                        }
                    }
                }
            }

            if (headerIsForbidden && !headerIsAllowed) {
                return true;
            }
        }

        return false;
    }

    private static final String[] FORBIDDEN_HEADERS =
            new String[]{"Accept-Charset", "Accept-Encoding", "Connection", HEADER_CONTENT_LENGTH, "Content-Transfer-Encoding",
                    "Date", "Expect", "Host", "Keep-Alive", "Referer", "TE", "Trailer", "Transfer-Encoding", "Upgrade", "Via"};

    public static boolean isForbiddenHeader(String header) {
        /*
         * From the XMLHttpRequest spec: http://www.w3.org/TR/XMLHttpRequest/#setrequestheader
         *
         * For security reasons, these steps should be terminated if the header argument case-insensitively matches one
         * of the following headers:
         *
         * Accept-Charset Accept-Encoding Connection Content-Length Content-Transfer-Encoding Date Expect Host
         * Keep-Alive Referer TE Trailer Transfer-Encoding Upgrade Via Proxy-* Sec-*
         *
         * Also for security reasons, these steps should be terminated if the start of the header argument
         * case-insensitively matches Proxy- or Se
         */
        if (header == null || (header.length() == 0)) {
            throw new IllegalArgumentException("Invalid header in the HTTP request");
        }
        String lowerCaseHeader = header.toLowerCase();
        if (lowerCaseHeader.startsWith("proxy-") || lowerCaseHeader.startsWith("sec-")) {
            return true;// "Headers starting with Proxy-* or Sec-* are prohibited"
        }
        for (String prohibited : FORBIDDEN_HEADERS) {
            if (header.equalsIgnoreCase(prohibited)) {
                return true; // "Certain headers are prohibited"
            }
        }
        return false;
    }

    public static Map<String, List<String>> EMPTY_HEADERS = new HashMap<>(0);

    public static void excludeHeaders(HttpRequestMessage request, String[] exclusions) {
        if (request == null) {
            throw new IllegalArgumentException("Request is null.");
        }

        if (exclusions == null || exclusions.length == 0) {
            return;
        }

        Map<String, List<String>> headers = request.getHeaders();
        if (headers == null || headers.size() == 0) {
            return;
        }

        // Get a mutable map
        headers = new HashMap<>(headers);

        final Set<String> headerNames = new HashSet<>(headers.keySet());

        for (String header : headerNames) {

            if (header == null || (header.length() == 0)) {
                throw new IllegalArgumentException("Invalid header in the HTTP request");
            }

            boolean ok = true;
            for (String excludedHeaderName : exclusions) {
                if (header.equalsIgnoreCase(excludedHeaderName)) {
                    ok = false;
                    break;
                }
            }

            if (!ok) {
                headers.remove(header);
            }
        }

        // Set the (possibly mutated) map
        request.setHeaders(headers);
    }

    // includesHeaders
    public static void restrictHeaders(HttpRequestMessage request, String[] restrictions) {
        if (request == null) {
            throw new IllegalArgumentException("Request is null.");
        }

        if (restrictions == null || restrictions.length == 0) {
            request.setHeaders(EMPTY_HEADERS);
            return;
        }

        Map<String, List<String>> headers = request.getHeaders();
        if (headers == null || headers.size() == 0) {
            return;
        }

        // Get a mutable map
        headers = new HashMap<>(headers);

        final Set<String> headerNames = new HashSet<>(headers.keySet());

        for (String header : headerNames) {

            if (header == null || (header.length() == 0)) {
                throw new IllegalArgumentException("Invalid header in the HTTP request");
            }

            boolean ok = false;
            for (String restrictedHeaderName : restrictions) {
                if (header.equalsIgnoreCase(restrictedHeaderName)) {
                    ok = true;
                    break;
                }
            }

            if (!ok) {
                headers.remove(header);
            }
        }

        // Set the (possibly mutated) map
        request.setHeaders(headers);
    }

    public static void mergeHeaders(HttpRequestMessage from, HttpRequestMessage to, String[] ignoreHeaders) {
        if (from == null || to == null) {
            throw new IllegalArgumentException("Request is null.");
        }

        if (ignoreHeaders == null) {
            ignoreHeaders = new String[0];
        }

        Map<String, List<String>> fromHeaders = from.getHeaders();
        if (fromHeaders == null || fromHeaders.isEmpty()) {
            return;
        }

        if (to.getHeaders() == null) {
            to.setHeaders(new HashMap<>(fromHeaders.size()));
        }

        // Get mutable headers
        fromHeaders = new HashMap<>(fromHeaders);
        final Map<String, List<String>> toHeaders = new HashMap<>(to.getHeaders());

        for (String ignoreHeader : ignoreHeaders) {
            fromHeaders.remove(ignoreHeader);
        }

        for (String fromHeader : fromHeaders.keySet()) {
            if (!toHeaders.containsKey(fromHeader)) {
                toHeaders.put(fromHeader, fromHeaders.get(fromHeader));
            } else {
                List<String> fromValues = fromHeaders.get(fromHeader);
                List<String> toValues = toHeaders.get(fromHeader);
                if (fromValues == null || toValues == null) {
                    throw new IllegalArgumentException("Illegal null header values from header: " + fromHeader);
                }
                Set<String> result = new LinkedHashSet<>(fromValues);
                result.addAll(toValues);
                toHeaders.put(fromHeader, new ArrayList<>(result));
            }
        }

        // Put back the (possibly mutated) headers
        to.setHeaders(toHeaders);
    }

    public static void removeValueFromHeaders(HttpRequestMessage request, String[] headerNames, String valueToRemove) {
        if (request == null) {
            throw new IllegalArgumentException("Request is null.");
        }

        if (headerNames == null || headerNames.length == 0) {
            return; // nothing to do
        }

        if (valueToRemove == null || valueToRemove.length() == 0) {
            return; // nothing to do
        }

        Map<String, List<String>> requestHeaders = request.getHeaders();
        if (requestHeaders == null || requestHeaders.size() == 0) {
            return; // nothing to do
        }

        // Get mutable requestHeaders
        requestHeaders = new HashMap<>(requestHeaders);

        // Remove the value to remove
        for (String header : headerNames) {
            List<String> values = requestHeaders.get(header);
            if (values == null || values.size() == 0) {
                continue;
            }
            //
            // make sure we have a mutable list; remove the value; install updated values
            //
            values = new ArrayList<>(values);
            values.remove(valueToRemove);
            requestHeaders.put(header, values);
        }

        // Put back (possibly mutated) headers
        request.setHeaders(requestHeaders);
    }

    public static void mergeParameters(HttpRequestMessage from, HttpRequestMessage to) {
        if (from == null || to == null) {
            throw new IllegalArgumentException("Request is null.");
        }

        Map<String, List<String>> fromParameters = from.getParameters();
        if (fromParameters == null || fromParameters.isEmpty()) {
            return;
        }

        if (to.getParameters() == null) {
            to.setParameters(new HashMap<>(fromParameters.size()));
        }

        // Get mutable parameters
        fromParameters = new HashMap<>(fromParameters);
        final Map<String, List<String>> toParameters = new HashMap<>(to.getParameters());

        for (String fromParameter : fromParameters.keySet()) {
            if (!toParameters.containsKey(fromParameter)) {
                toParameters.put(fromParameter, fromParameters.get(fromParameter));
            } else {
                List<String> fromValues = fromParameters.get(fromParameter);
                List<String> toValues = toParameters.get(fromParameter);
                if (fromValues == null) {
                    fromValues = new ArrayList<>();
                }
                if (toValues == null) {
                    toValues = new ArrayList<>();
                }
                Set<String> result = new LinkedHashSet<>(fromValues);
                result.addAll(toValues);
                toParameters.put(fromParameter, new ArrayList<>(result));
            }
        }

        // Put back the (possibly mutated) headers
        to.setParameters(toParameters);
    }

    public static void mergeCookies(HttpRequestMessage from, HttpRequestMessage to) {
        if (from == null || to == null) {
            throw new IllegalArgumentException("Request is null.");
        }

        Set<HttpCookie> fromCookies = from.getCookies();
        if (fromCookies == null || fromCookies.isEmpty()) {
            return;
        }

        if (to.getCookies() == null) {
            HashSet<HttpCookie> cookies = new HashSet<>(fromCookies);
            to.setCookies(cookies);
        } else {
            Set<HttpCookie> toCookies = new HashSet<>(to.getCookies());

            // add all the cookies in the from RequestMessage
            toCookies.addAll(fromCookies);
            to.setCookies(toCookies);
        }
    }

    public static String mergeQueryParameters(URI from, String into) throws URISyntaxException {

        if (into == null) {
            throw new URISyntaxException("<null>", "Cannot merge into a null URI");
        }

        if (from == null) {
            return into;
        }

        final String fromQuery = from.getQuery();
        final String intoQuery = URIUtils.getQuery(into);
        String query;
        if (intoQuery == null) {
            query = from.getQuery();
        } else if (fromQuery == null) {
            query = intoQuery;
        } else {

            query = intoQuery;
            if (fromQuery.length() > 0) {
                query += '&' + fromQuery;
            }
        }

        return URIUtils.buildURIAsString(URIUtils.getScheme(into), URIUtils.getAuthority(into), URIUtils.getPath(into), query,
                URIUtils.getFragment(into));
    }

    public static boolean hasStreamingScheme(URI uri) {
        if (uri == null || uri.getScheme() == null) {
            return false;
        }

        final String scheme = uri.getScheme();
        return ("tcp".equalsIgnoreCase(scheme) || "ssl".equalsIgnoreCase(scheme));
    }

    public static boolean hasStreamingScheme(String uri) {
        if (uri == null || URIUtils.getScheme(uri) == null) {
            return false;
        }

        final String scheme = URIUtils.getScheme(uri);
        return ("tcp".equalsIgnoreCase(scheme) || "ssl".equalsIgnoreCase(scheme));
    }

}

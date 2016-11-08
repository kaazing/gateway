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

import static java.lang.String.format;
import static org.kaazing.gateway.transport.http.bridge.filter.HttpProtocolCompatibilityFilter.HttpConditionalWrappedResponseFilter.conditionallyWrappedResponsesRequired;
import static org.kaazing.gateway.util.InternalSystemProperty.HTTPXE_SPECIFICATION;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Queue;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.security.auth.Subject;

import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.service.IoProcessor;
import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.security.auth.context.ResultAwareLoginContext;
import org.kaazing.gateway.transport.AbstractBridgeSession;
import org.kaazing.gateway.transport.CommitFuture;
import org.kaazing.gateway.transport.DefaultCommitFuture;
import org.kaazing.gateway.transport.DefaultUpgradeFuture;
import org.kaazing.gateway.transport.Direction;
import org.kaazing.gateway.transport.SslUtils;
import org.kaazing.gateway.transport.UpgradeFuture;
import org.kaazing.gateway.transport.bridge.CachingMessageEncoder;
import org.kaazing.gateway.transport.bridge.Message;
import org.kaazing.gateway.transport.bridge.MessageEncoder;
import org.kaazing.gateway.transport.http.bridge.HttpHeaderNameComparator;
import org.kaazing.gateway.transport.http.bridge.HttpRequestMessage;
import org.kaazing.gateway.transport.http.bridge.filter.HttpBuffer;
import org.kaazing.mina.core.buffer.IoBufferAllocatorEx;
import org.kaazing.mina.core.buffer.IoBufferEx;
import org.kaazing.mina.core.service.IoProcessorEx;
import org.kaazing.mina.core.service.IoServiceEx;
import org.kaazing.mina.core.session.IoSessionEx;

// TODO: change to just support cookie list and internal differential for write
public class DefaultHttpSession extends AbstractBridgeSession<DefaultHttpSession, HttpBuffer> implements HttpAcceptSession, HttpConnectSession {

    private static final String DEFAULT_CACHE_KEY = "http";
    private static final String GZIPPED_CACHE_KEY = "http/gzipped";

	private static final String UTF_8 = "utf-8";

    // read/write
    private final Map<String, List<String>> writeHeaders;
    private final Set<HttpCookie> writeCookies;
    private HttpVersion version;

    // read-only
    private Map<String, List<String>> readHeaders;
    private Collection<HttpCookie> readCookies;
    private final boolean secure;
    private URI requestURL;

    // --

    // read/write on connect - read-only on accept
    private HttpMethod method;
    private URI requestURI;  // connector needs to set path
    private Map<String, List<String>> parameters;

    // read/write on accept - read-only on connect
    private HttpStatus status;
    private String reason;

    // --

    // read-only on accept - undefined on connect
    private URI servicePath;
    private URI pathInfo;


    // internal
    private IoHandler upgradeHandler;
    private final DefaultUpgradeFuture upgradeFuture;
    private final CommitFuture commitFuture;
    private final ResponseFuture responseFuture;
    private final AtomicBoolean committing;
    private final AtomicBoolean connectionClose;
    private ResultAwareLoginContext loginContext;
    private final AtomicBoolean shutdownWrite;
    private Queue<IoBufferEx> deferredReads = new ConcurrentLinkedQueue<>();

	private boolean isChunked;

	private boolean isGzipped;
    private boolean httpxeSpecCompliant;

	private int redirectsAllowed;
	private ResourceAddress redirectlocalAddress;
    private ResourceAddress redirectRemoteAddress;

    @SuppressWarnings("deprecation")
    private DefaultHttpSession(IoServiceEx service,
                               IoProcessorEx<DefaultHttpSession> processor,
                               ResourceAddress address,
                               ResourceAddress remoteAddress,
                               IoSessionEx parent,
                               IoBufferAllocatorEx<HttpBuffer> allocator,
                               Direction direction,
                               Properties configuration) {
        super(service, processor, address, remoteAddress, parent, allocator, direction);

        writeHeaders = new LinkedHashMap<>();
        writeCookies = new HashSet<>();
        status = direction == Direction.READ ? HttpStatus.SUCCESS_OK : null;
        reason = null;

        secure = SslUtils.isSecure(parent);
        committing = new AtomicBoolean(false);
        connectionClose = new AtomicBoolean(false);
        shutdownWrite = new AtomicBoolean(false);

        upgradeFuture = new DefaultUpgradeFuture(parent);
        commitFuture = new DefaultCommitFuture(this);
        responseFuture = direction == Direction.READ ? null : new DefaultResponseFuture(this);

        httpxeSpecCompliant = configuration == null ? false : HTTPXE_SPECIFICATION.getBooleanProperty(configuration);
        // TODO: add and use new HttpResourceAddress "maximum.redirects" option of type Integer, default 5
        //redirectsAllowed = ((HttpResourceAddress) remoteAddress).getOption(HttpResourceAddress.MAXIMUM_REDIRECTS);
        redirectsAllowed = 0;
    }

    public DefaultHttpSession(IoServiceEx service,
                              IoProcessorEx<DefaultHttpSession> processor,
                              ResourceAddress address,
                              ResourceAddress remoteAddress,
                              IoSessionEx parent,
                              IoBufferAllocatorEx<HttpBuffer> allocator,
                              Properties configuration) {
        this(service, processor, address, remoteAddress, parent, allocator, Direction.WRITE, configuration);

        requestURL = remoteAddress.getResource();

        try {
            requestURI = new URI(null, null, null, 0, requestURL.getPath(), requestURL.getQuery(), requestURL.getFragment());
        }
        catch (URISyntaxException e) {
        }

        parameters = new HashMap<>();

        String query = requestURL.getRawQuery();
        if (query != null) {
            String[] nvPairs = query.split("&");
            for (String nvPair : nvPairs) {
                int equalAt = nvPair.indexOf('=');
                if (equalAt != -1) {
                    String parameterName = nvPair.substring(0, equalAt);
                    String parameterValue = nvPair.substring(equalAt + 1);
                    _addParameter(decodeURL(parameterName), decodeURL(parameterValue));
                }
                else {
                    _addParameter(decodeURL(nvPair), "");
                }
            }
        }

        version = HttpVersion.HTTP_1_1;
        method = HttpMethod.GET;

        readHeaders = new TreeMap<>(HttpHeaderNameComparator.INSTANCE);
        readCookies = Collections.emptySet();

        servicePath = null;
        pathInfo = null;

        // TODO: add and use new HttpResourceAddress "maximum.redirects" option of type Integer, default 5
        //redirectsAllowed = ((HttpResourceAddress) remoteAddress).getOption(HttpResourceAddress.MAXIMUM_REDIRECTS);
        redirectsAllowed = 0;
    }

    @Override
    public void setSubject(Subject subject) {
        super.setSubject(subject);
    }

    @Override
    public CachingMessageEncoder getMessageEncoder() {
        // KG-8134: we only know at encoding time if the response is chunked or gzipped, and we must change the cacheKey
        // accordingly to ensure we don't get conflicts when shared copy is active
        return new CachingMessageEncoder() {

            @Override
            public <T extends Message> IoBufferEx encode(MessageEncoder<T> encoder, T message, IoBufferAllocatorEx<?> allocator, int flags) {
                String cacheKey = isGzipped() ? GZIPPED_CACHE_KEY : DEFAULT_CACHE_KEY;
                if (isChunked()) {
                    cacheKey = cacheKey + "/chunked";
                }
                return encode(cacheKey, encoder, message, allocator, flags);
            }

        };
    }

    // TODO: put this into a utility
    // per http://www.w3.org/TR/html40/appendix/notes.html#non-ascii-chars this should be UTF-8 to provide for broadest compatibility
    private static String decodeURL(String url) {
    	try {
			return URLDecoder.decode(url, UTF_8);
		} catch (UnsupportedEncodingException e) {
		}
		return null;
    }

    // TODO: need to clean this up and provide some mix-in or other for param managment
    private void _addParameter(String parameterName, String parameterValue) {
        if (parameterName == null) {
            throw new NullPointerException("parameterName");
        }
        if (parameterValue == null) {
            throw new NullPointerException("parameterValue");
        }
        List<String> parameterValues = _getParameterValues(parameterName);
        parameterValues.add(parameterValue);
    }

    private List<String> _getParameterValues(String parameterName) {
        List<String> parameterValues = parameters.get(parameterName);
        if (parameterValues == null) {
            parameterValues = new ArrayList<>();
            parameters.put(parameterName, parameterValues);
        }
        return parameterValues;
    }
    //--

    // TODO: need to change http session direction after read is complete
    public DefaultHttpSession(IoServiceEx service,
                              IoProcessorEx<DefaultHttpSession> processor,
                              ResourceAddress address,
                              ResourceAddress remoteAddress,
                              IoSessionEx parent,
                              IoBufferAllocatorEx<HttpBuffer> allocator,
                              HttpRequestMessage request,
                              URI serviceURI,
                              Properties configuration) {
        this(service, processor, address, remoteAddress, parent, allocator, Direction.READ, configuration);

        // Never elevate the X-Next-Protocol header from HTTP request to the session.
        // It will already be captured in this session's local address anyhow. (address.getOption(NEXT_PROTOCOL))
        HttpUtils.excludeHeaders(request, new String[]{HttpHeaders.HEADER_X_NEXT_PROTOCOL});

        readHeaders = request.getModifiableHeaders();
        readCookies = request.getCookies();
        version = request.getVersion();
        method = request.getMethod();
        requestURI = request.getRequestURI();
        parameters = request.getParameters();

        String host = request.getHeader("Host");
        requestURL = URI.create((secure ? "https" : "http") + "://" + host + requestURI);

        servicePath = URI.create(serviceURI.getPath());

        URI relative = servicePath.relativize(requestURI);
        String relativePath = relative.getPath();
        if (relativePath != null && !relativePath.isEmpty()) {
            pathInfo = relativePath.startsWith("/")? relative : URI.create("/" + relative);
        }
        else {
            pathInfo = relative;
        }
    }

    @Override
    public HttpVersion getVersion() {
        return version;
    }

    @Override
    public void setVersion(HttpVersion version) {
        this.version = version;
    }

    @Override
    public Set<String> getReadHeaderNames() {
        return readHeaders.keySet();
    }

    @Override
    public String getReadHeader(String name) {
        List<String> header = readHeaders.get(name);
        if (header != null && header.size() > 0) {
            return header.get(0);
        }
        return null;
    }

    @Override
    public List<String> getReadHeaders(String name) {
        List<String> header = readHeaders.get(name);
        if (header != null && header.size() > 0) {
            return Collections.unmodifiableList(header);
        }
        return null;
    }

    @Override
    public Map<String, List<String>> getReadHeaders() {
        return Collections.unmodifiableMap(readHeaders);
    }

	public void setReadHeaders(Map<String, List<String>> headers) {
		readHeaders.clear();
		readHeaders.putAll(headers);
	}

	@Override
    public String getWriteHeader(String name) {
        List<String> header = writeHeaders.get(name);
        if (header != null && header.size() > 0) {
            return header.get(0);
        }
        return null;
    }

    @Override
    public List<String> getWriteHeaders(String name) {
        List<String> header = writeHeaders.get(name);
        if (header != null && header.size() > 0) {
            return header;
        }
        return null;
    }

    @Override
    public void setWriteHeader(String name, String value) {
        List<String> header = new ArrayList<>();
        writeHeaders.put(name, header);
        header.add(value);
    }

    @Override
    public void setWriteHeaders(String name, List<String> value) {
        if (commitFuture.isCommitted()) {
            String format = "Attempted to modify http session %d write headers when the session is already committed.";
            throw new IllegalStateException(format(format, getId()));
        }
        writeHeaders.put(name, value);
    }

    @Override
    public void setWriteHeaders(Map<String, List<String>> headers) {
        if (commitFuture.isCommitted()) {
            String format = "Attempted to modify http session %d write headers when the session is already committed.";
            throw new IllegalStateException(format(format, getId()));
        }
    	writeHeaders.clear();
    	writeHeaders.putAll(headers);
    }

    @Override
    public void addWriteHeader(String name, String value) {
        if (commitFuture.isCommitted()) {
            String format = "Attempted to modify http session %d write header %s when the session is already committed.";
            throw new IllegalStateException(format(format, getId(), name));
        }
        List<String> header = writeHeaders.get(name);
        if (header == null) {
            header = new ArrayList<>();
            writeHeaders.put(name, header);
        }
        header.add(value);
    }

    @Override
    public void clearWriteHeaders(String name) {
        if (commitFuture.isCommitted()) {
            String format = "Attempted to clear http session %d write headers when the session is already committed.";
            throw new IllegalStateException(format(format, getId()));
        }
        writeHeaders.remove(name);
    }

    @Override
    public Map<String, List<String>> getWriteHeaders() {
        return writeHeaders;
    }

    @Override
    public void setWriteCookies(Set<HttpCookie> cookies) {
        writeCookies.clear();
        writeCookies.addAll(cookies);
    }

    @Override
    public Collection<HttpCookie> getReadCookies() {
        return Collections.unmodifiableCollection(readCookies);
    }

    @Override
    public Set<HttpCookie> getWriteCookies() {
        return writeCookies;
    }

    @Override
    public HttpMethod getMethod() {
        return method;
    }

    @Override
    public void setMethod(HttpMethod method) {
        this.method = method;
    }

    @Override
    public URI getRequestURI() {
        return requestURI;
    }

    @Override
    public void setRequestURI(URI requestURI) {
        this.requestURI = requestURI;
    }

    @Override
    public String getParameter(String name) {
        List<String> parameter = parameters.get(name);
        if (parameter != null && parameter.size() > 0) {
            return parameter.get(0);
        }
        return null;
    }

    @Override
    public List<String> getParameterValues(String name) {
        List<String> parameter = parameters.get(name);
        if (parameter != null && parameter.size() > 0) {
            return Collections.unmodifiableList(parameter);
        }
        return null;
    }

    @Override
    public Map<String, List<String>> getParameters() {
        return Collections.unmodifiableMap(parameters);
    }

    @Override
    public HttpStatus getStatus() {
        return status;
    }

    @Override
    public void setStatus(HttpStatus status) {
        this.status = status;
    }

    @Override
    public String getReason() {
        return reason;
    }

    @Override
    public void setReason(String reason) {
        this.reason = reason;
    }

    @Override
    public URI getServicePath() {
        return servicePath;
    }

    @Override
    public URI getPathInfo() {
        return pathInfo;
    }

    @Override
    public URI getRequestURL() {
        return requestURL;
    }

	@Override
    public boolean isSecure() {
        return secure;
    }

    @Override
    public final boolean isCommitting() {
        return committing.get() || commitFuture.isCommitted();
    }

    @Override
    public ResponseFuture getResponseFuture() {
        return responseFuture;
    }

    public IoHandler getUpgradeHandler() {
        return upgradeHandler;
    }

    @Override
    public UpgradeFuture getUpgradeFuture() {
        return upgradeFuture;
    }

    @Override
    public UpgradeFuture upgrade(IoHandler handler) {
        upgradeHandler = handler;
        return upgradeFuture;
    }

    @Override
    public CommitFuture getCommitFuture() {
        return commitFuture;
    }

    @Override
    public CommitFuture commit() {
        if (committing.compareAndSet(false, true)) {
            IoProcessor<DefaultHttpSession> processor = getProcessor();
            // for now handle this with an instance check. It could require an abstract processor
            // with commit as a no-op for the connector for now
            if (processor instanceof HttpAcceptProcessor) {
                HttpAcceptProcessor acceptProcessor = (HttpAcceptProcessor)processor;
                acceptProcessor.commit(this);
            }
        }
        return commitFuture;
    }

    @Override
    public void shutdownWrite() {
        shutdownWrite.set(true);
    }

    public boolean isWriteShutdown() {
        return shutdownWrite.get();
    }

    @Override
    public ResourceAddress getLocalAddress() {
        return (this.redirectlocalAddress != null) ? this.redirectlocalAddress: super.getLocalAddress();
    }

    @Override
    public ResourceAddress getRemoteAddress() {
        return (this.redirectRemoteAddress != null) ? this.redirectRemoteAddress: super.getRemoteAddress();
    }

    public Queue<IoBufferEx> getDeferredReads() {
        return deferredReads;
    }

    public void addDeferredRead(IoBufferEx buffer) {
        deferredReads.add(buffer);
    }

    public boolean isConnectionClose() {
        return connectionClose.get();
    }

    boolean setConnectionClose() {
        return connectionClose.compareAndSet(false, true);
    }

	public boolean isChunked() {
		return this.isChunked;
	}

	public void setChunked(boolean isChunked) {
		this.isChunked = isChunked;
	}

	public boolean isGzipped() {
		return this.isGzipped;
	}

	public void setGzipped(boolean isGzipped) {
		this.isGzipped = isGzipped;
	}

    @Override
    public ResultAwareLoginContext getLoginContext() {
        return loginContext;
    }

	public void setLoginContext(ResultAwareLoginContext loginContext) {
	    this.loginContext = loginContext;
	}

    public boolean isChunkingNecessary() {
        // if there is no content length specified, is chunking necessary?
        // well, yes usually.  but if you are a wrapped response at the httpxe layer and a legacy client, then no.
        // why? because the content is length-encoded already.

        ResourceAddress address = getLocalAddress().getTransport();
        return address != null &&
                ( ! "httpxe/1.1".equals(address.getOption(ResourceAddress.NEXT_PROTOCOL)) &&
                  ! conditionallyWrappedResponsesRequired(this));
    }

    public boolean isHttpxeSpecCompliant() {
        return httpxeSpecCompliant;
    }

    public IoSessionEx setParent(IoSessionEx newParent){
        this.setLocalAddress(LOCAL_ADDRESS.get(newParent));
        // newParent.getRemoteAddress(); httpSession.setLocalAddress((ResourceAddress)redirectRemoteAddress);
        upgradeFuture.setSession(newParent);
        if (!SslUtils.isSecure(newParent) && secure) {
            throw new InvalidParameterException("Can not switch from a secure session to a non secure session");
        }

        return super.setParent(newParent);
    }

    int getAndDecrementRedirectsAllowed() {
        int result = redirectsAllowed;
        if (result > 0) {
            redirectsAllowed--;
        }
        return result;
    }

    public void setLocalAddress(ResourceAddress redirectlocalAddress) {
        this.redirectlocalAddress = redirectlocalAddress;
    }

    public void setRemoteAddress(ResourceAddress redirectRemoteAddress) {
        this.redirectRemoteAddress = redirectRemoteAddress;
    }

}

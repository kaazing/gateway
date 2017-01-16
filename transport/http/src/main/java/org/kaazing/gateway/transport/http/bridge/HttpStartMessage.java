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
package org.kaazing.gateway.transport.http.bridge;

import static java.util.Collections.unmodifiableMap;
import static java.util.Collections.unmodifiableSet;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.kaazing.gateway.transport.http.HttpCookie;
import org.kaazing.gateway.transport.http.HttpVersion;


public abstract class HttpStartMessage extends HttpMessage {

    private static final Map<String, List<String>> EMPTY_HEADERS = Collections.emptyMap();
    private static final Set<String> EMPTY_HEADER_NAMES = Collections.emptySet();
    private static final List<String> EMPTY_HEADER = Collections.emptyList();
    private static final Set<HttpCookie> EMPTY_COOKIES = Collections.emptySet();

    private Set<HttpCookie> cookies;
    private Map<String, List<String>> headers;
    private HttpVersion version;
    private HttpContentMessage content;
    private boolean contentLengthImplicit;

    public HttpStartMessage() {
        content = null;
    }

    @Override
    public boolean isComplete() {
        return (content == null) || content.isComplete();
    }

    public boolean isContentLengthImplicit() {
        return contentLengthImplicit;
    }
    
    public void setContentLengthImplicit(boolean contentLengthImplicit) {
        this.contentLengthImplicit = contentLengthImplicit;
    }
    
    public boolean hasCookies() {
        return (cookies != null && !cookies.isEmpty());
    }
    
    public Iterator<HttpCookie> iterateCookies() {
        return (cookies != null) ? cookies.iterator() : EMPTY_COOKIES.iterator();
    }
    
	public Set<HttpCookie> getCookies() {
		Set<HttpCookie> cookies = getCookies(false);
		return (cookies != null && !cookies.isEmpty()) ? unmodifiableSet(cookies) : EMPTY_COOKIES;
	}
	
	public void addCookie(HttpCookie cookie) {
		if (cookie == null) {
			throw new NullPointerException("cookie");
		}
		Set<HttpCookie> cookies = getCookies(true);
		cookies.add(cookie);
	}
	
	public void removeCookie(HttpCookie cookie) {
        if (cookie == null) {
            throw new NullPointerException("cookie");
        }
		Set<HttpCookie> cookies = getCookies(false);
		if (cookies != null) {
			cookies.remove(cookie);
		}
	}
	
	public void clearCookies() {
		Set<HttpCookie> cookies = getCookies(false);
		if (cookies != null) {
			cookies.clear();
		}
	}
	
	public void setCookies(Collection<HttpCookie> newCookies) {
		Set<HttpCookie> cookies = getCookies(true);
		cookies.clear();
		cookies.addAll(newCookies);
	}

	public void setHeader(String headerName, String headerValue) {
		if (headerName == null) {
			throw new NullPointerException("headerName");
		}
		if (headerValue == null) {
			throw new NullPointerException("headerValue");
		}
		List<String> headerValues = getHeaderValues(headerName, true);
		headerValues.clear();
		headerValues.add(headerValue);
	}
	
	public void addHeader(String headerName, String headerValue) {
		if (headerName == null) {
			throw new NullPointerException("headerName");
		}
		if (headerValue == null) {
			throw new NullPointerException("headerValue");
		}
		List<String> headerValues = getHeaderValues(headerName, true);
		headerValues.add(headerValue);
	}

	public List<String> removeHeader(String headerName) {
		Map<String, List<String>> headers = getHeaders(false);

		if (headers != null) {
			return headers.remove(headerName);
		}
		
		return EMPTY_HEADER;
	}
	
	public void clearHeaders() {
		Map<String, List<String>> headers = getHeaders(false);
		
		if (headers != null) {
			headers.clear();
		}
	}
	
	public void setHeaders(Map<String, List<String>> newHeaders) {
		Map<String, List<String>> headers = getHeaders(true);
		headers.clear();
		headers.putAll(newHeaders);
	}

	public void putHeaders(Map<String, List<String>> newHeaders) {
		Map<String, List<String>> headers = getHeaders(true);
		headers.putAll(newHeaders);
	}

    public Iterator<String> iterateHeaderNames() {
        return (headers != null && !headers.isEmpty()) ? headers.keySet().iterator() : EMPTY_HEADER_NAMES.iterator();
    }
    
    public Set<String> getHeaderNames() {
        Map<String, List<String>> headers = getHeaders(false);
        return (headers != null && !headers.isEmpty()) ? unmodifiableSet(headers.keySet()) : EMPTY_HEADER_NAMES;
    }
    
	public Map<String, List<String>> getHeaders() {
		Map<String, List<String>> headers = getHeaders(false);
		return (headers != null && !headers.isEmpty()) ? unmodifiableMap(headers) : EMPTY_HEADERS;
	}

    public Map<String, List<String>> getModifiableHeaders() {
        Map<String, List<String>> headers = getHeaders(false);
        return (headers != null && !headers.isEmpty()) ? headers : EMPTY_HEADERS;
    }

	public List<String> getHeaderValues(String headerName) {
		return getHeaderValues(headerName, true);
	}
	
	public String getHeader(String headerName) {

		List<String> headerValues = getHeaderValues(headerName, false);
		if (headerValues == null) {
			return null;
		}
		
		return headerValues.isEmpty() ? null : headerValues.get(0);
	}

    public boolean hasHeaders() {
        return (headers != null && !headers.isEmpty());
    }
	
	public boolean hasHeader(String headerName) {
		return (headers != null && headers.containsKey(headerName));
	}
	
	public void setVersion(HttpVersion version) {
		this.version = version;
	}

	public HttpVersion getVersion() {
		return version;
	}
	
	public void setContent(HttpContentMessage content) {
		this.content = content;
	}
	
	public HttpContentMessage getContent() {
		return content;
	}

	private Set<HttpCookie> getCookies(boolean createIfNull) {
		if (cookies == null && createIfNull) {
			cookies = new TreeSet<>(HttpCookieComparator.INSTANCE);
		}
		return cookies;
	}
	
	private Map<String, List<String>> getHeaders(boolean createIfNull) {
		if (headers == null && createIfNull) {
			headers = createHeaders();
		}
		return headers;
	}

	protected abstract Map<String, List<String>> createHeaders();

	public List<String> getHeaderValues(String headerName, boolean createIfNull) {
		Map<String, List<String>> headers = getHeaders(createIfNull);
		if (headers == null) {
			return null;
		}
		
		List<String> headerValues = headers.get(headerName);
		if (headerValues == null && createIfNull) {
			headerValues = new LinkedList<>();
			headers.put(headerName, headerValues);
		}
		return headerValues;
	}

    @Override
	public int hashCode() {
	    int hashCode = 0;
	    if (version != null) {
	        hashCode ^= version.hashCode();
	    }
	    if (headers != null) {
	        hashCode ^= headers.hashCode();
	    }
        if (cookies != null) {
            hashCode ^= cookies.hashCode();
        }
        if (content != null) {
            hashCode ^= content.hashCode();
        }
        return hashCode;
	}
	
	protected boolean equals(HttpStartMessage that) {
	    return (sameOrEquals(this.version, that.version) &&
	            sameOrEquals(this.headers, that.headers) &&
	            sameOrEquals(this.cookies, that.cookies) &&
	            sameOrEquals(this.content, that.content));
	}

    static boolean sameOrEquals(Object this_, Object that) {
        return (this_ == that) || (this_ != null && this_.equals(that));
    }

    public static <K, V> boolean sameOrEquals(Map<K, V> this_, Map<K, V> that) {
        return (this_ == that) ||
                (this_ == null && that.isEmpty()) || (that == null && this_.isEmpty()) ||
                (this_ != null && this_.equals(that));
    }

    static <T> boolean sameOrEquals(Collection<T> this_, Collection<T> that) {
        return (this_ == that) ||
                (this_ == null && that.isEmpty()) || (that == null && this_.isEmpty()) ||
                (this_ != null && this_.equals(that));
    }
}

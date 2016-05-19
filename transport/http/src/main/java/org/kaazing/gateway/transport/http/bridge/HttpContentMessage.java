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

import static java.util.EnumSet.noneOf;
import static java.util.EnumSet.of;
import static org.kaazing.gateway.transport.http.bridge.filter.HttpEncoding.CHUNKED;
import static org.kaazing.gateway.transport.http.bridge.filter.HttpEncoding.GZIPPED;

import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.util.EnumSet;
import java.util.Set;

import org.kaazing.gateway.transport.http.bridge.filter.HttpBufferAllocator;
import org.kaazing.gateway.transport.http.bridge.filter.HttpEncoding;
import org.kaazing.mina.core.buffer.IoBufferEx;
import org.kaazing.mina.core.buffer.SimpleBufferAllocator;

public class HttpContentMessage extends HttpMessage {

	private static final EnumSet<HttpContentInfo> CONTENT_INFO_NONE = noneOf(HttpContentInfo.class);
    private static final EnumSet<HttpContentInfo> CONTENT_INFO_COMPLETE = of(HttpContentInfo.COMPLETE);

    private static final EnumSet<HttpEncoding> ENCODING_NONE = noneOf(HttpEncoding.class);
    private static final EnumSet<HttpEncoding> ENCODING_GZIPPED_ONLY = of(GZIPPED);
    private static final EnumSet<HttpEncoding> ENCODING_CHUNKED_ONLY = of(CHUNKED);
    private static final EnumSet<HttpEncoding> ENCODING_CHUNKED_AND_GZIPPED = of(CHUNKED, GZIPPED);

    private static HttpBufferAllocator httpAllocator = new HttpBufferAllocator(SimpleBufferAllocator.BUFFER_ALLOCATOR);

    public static final HttpContentMessage EMPTY = new HttpContentMessage(httpAllocator.wrap(httpAllocator.allocate(0)), true);
    public static final HttpContentMessage FLUSH = new HttpContentMessage(httpAllocator.wrap(httpAllocator.allocate(0)), true);
    
	private enum HttpContentInfo { INJECTED, COMPLETE }

    private final IoBufferEx data;
    private final Set<HttpEncoding> encodings;
    private final Set<HttpContentInfo> contentInfos;

	public HttpContentMessage(IoBufferEx data, boolean complete) {
	    this(data, asContentInfos(complete), ENCODING_NONE);
	}
	
	public HttpContentMessage(IoBufferEx data, boolean complete, boolean chunked, boolean gzipped) {
	    this(data, asContentInfos(complete), asEncodings(chunked, gzipped));
	}
	
    private HttpContentMessage(IoBufferEx data, Set<HttpContentInfo> contentInfos, Set<HttpEncoding> encodings) {
        if (data == null) {
            throw new NullPointerException("data");
        }

		this.data = data;
        if (contentInfos == null) {
            throw new NullPointerException("contentInfos");
        }

        if (encodings == null) {
            throw new NullPointerException("encodings");
        }

        this.contentInfos = contentInfos;
        this.encodings = encodings;
    }
    
	@Override
	public Kind getKind() {
		return Kind.CONTENT;
	}

	public IoBufferEx asBuffer() {
		return data;
	}
	
	@Deprecated // TODO: move to tests, only used there
	public String asText(CharsetDecoder decoder) throws CharacterCodingException {
		return data.slice().getString(decoder);
	}
	
	public int length() {
		return data.remaining();
	}

    @Override
	public boolean isComplete() {
        return contentInfos.contains(HttpContentInfo.COMPLETE);
    }
    
    public boolean isInjected() {
        return contentInfos.contains(HttpContentInfo.INJECTED);
    }
    
	public final boolean isChunked() {
		return encodings.contains(CHUNKED);
	}
	
	public final boolean isGzipped() {
		return encodings.contains(GZIPPED);
	}
	
	@Override
	public boolean equals(Object o) {
	    if (o == null || !(o instanceof HttpContentMessage)) {
	        return false;
	    }
	    
		if (o == this) {
			return true;
		}
		
		HttpContentMessage that = (HttpContentMessage)o;
		return (that.data.equals(this.data) && that.contentInfos.equals(this.contentInfos) && that.encodings.equals(this.encodings));
	}

	@Override
    public int hashCode() {
	    int result = 17;
        result <<= 8;
        result ^= data.hashCode();
        result <<= 8;
        result ^= contentInfos.hashCode();
        result <<= 8;
        result ^= encodings.hashCode();
        return result;
    }

	@Override
	public String toString() {
		return String.format("%s: %s %s%s%s", getKind(), data,
				(isChunked() ? "[CHUNKED]" : ""), (isGzipped() ? "[GZIPPED]" : ""), (isComplete() ? "" : "[...]")); 
	}
	
    private static Set<HttpContentInfo> asContentInfos(boolean complete) {
        if (complete) {
            return CONTENT_INFO_COMPLETE;
        }
        else {
            return CONTENT_INFO_NONE;
        }
    }
    
	private static Set<HttpEncoding> asEncodings(boolean chunked, boolean gzipped) {
	    if (chunked && gzipped) {
	        return ENCODING_CHUNKED_AND_GZIPPED;
	    }
	    else if (chunked) {
            return ENCODING_CHUNKED_ONLY;
	    }
	    else if (gzipped) {
           return ENCODING_GZIPPED_ONLY;
	    }
	    else {
	        return ENCODING_NONE;
	    }
	}
}

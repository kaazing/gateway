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

import static java.util.Collections.emptySet;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.kaazing.gateway.resource.address.http.HttpInjectableHeader;
import org.kaazing.gateway.transport.http.HttpStatus;

public class HttpResponseMessage extends HttpStartMessage {

    private static final Set<HttpInjectableHeader> EMPTY_INJECTABLE_HEADERS = emptySet();
    
	private HttpStatus status;
	private String reason;
	private String bodyReason;
	private Set<HttpInjectableHeader> injectableHeaders = EMPTY_INJECTABLE_HEADERS;

	private boolean contentExcluded;
    private boolean blockPadding;

    public HttpResponseMessage() {
	}
	
	@Override
	public Kind getKind() {
		return Kind.RESPONSE;
	}

	public boolean hasReason() {
	    return reason != null;
	}
	
	public String getReason() {
		return (reason == null && status != null) ? status.reason() : reason;
	}

	public String getBodyReason() {
		return (bodyReason == null ? getReason() : bodyReason);
        }

    public boolean isContentExcluded() {
        return contentExcluded;
    }

	public HttpStatus getStatus() {
		return status;
	}

	public void setReason(String reason) {
		this.reason = reason;
	}

	public Set<HttpInjectableHeader> getInjectableHeaders() {
	    return injectableHeaders;
	}
	
	public void setInjectableHeaders(Set<HttpInjectableHeader> injectableHeaders) {
	    if (injectableHeaders == null) {
	        throw new NullPointerException("injectableHeaders");
	    }
	    this.injectableHeaders = injectableHeaders;
	}
	
	/**
	 * Customize the reason string that will be sent in HTTP response
	 * body (rather than in the status line).
	 */
	public void setBodyReason(String bodyReason) {
		this.bodyReason = bodyReason;
	}

	public void setStatus(HttpStatus status) {
		this.status = status;
	}
	
    public void setContentExcluded(boolean value) {
        contentExcluded = value;
    }

    public void setBlockPadding(boolean blockPadding) {
        this.blockPadding = blockPadding;
    }
    
    public boolean isBlockPadding() {
        return blockPadding;
    }

    @Override
    public int hashCode() {
        int hashCode = super.hashCode();
        if (status != null) {
            hashCode ^= status.hashCode();
        }
        if (reason != null) {
            hashCode ^= reason.hashCode();
        }
    	if (bodyReason != null) {
    	    hashCode ^= bodyReason.hashCode();
    	}
        return hashCode;
    }
    
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof HttpResponseMessage)) {
            return false;
        }
        
        return equals((HttpResponseMessage)o);
    }
    
    protected boolean equals(HttpResponseMessage that) {
        return (super.equals(that) &&
                this.contentExcluded == that.contentExcluded &&
                sameOrEquals(this.bodyReason, that.bodyReason) &&
                sameOrEquals(this.reason, that.reason) &&
                sameOrEquals(this.status, that.status));
    }
    
    @Override
    public String toString() {
        return String.format("%s: %s %s %s %s %s", getKind(), getStatus(), getReason(), getVersion(), getContent(), (isComplete() ? "" : " [...]")); 
    }

    @Override
    public String toVerboseString() {
        return String.format("%s: %s %s %s HEADERS: %s %s %s", getKind(), getStatus(), getReason(), getVersion(), getHeaders(), getContent(), (isComplete() ? "" : " [...]")); 
    }

	@Override
    protected Map<String, List<String>> createHeaders() {
        return new TreeMap<>(COMPARE_IGNORE_CASE);
	}
	
	private static Comparator<String> COMPARE_IGNORE_CASE = new Comparator<String>() {

        @Override
        public int compare(String o1, String o2) {
            return o1.compareToIgnoreCase(o2);
        }
	    
	};
}

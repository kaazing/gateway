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
package org.apache.mina.core.write;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

import org.apache.mina.util.MapBackedSet;

/**
 * An exception which is thrown when one or more write operations were failed.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class WriteException extends IOException {

    private static final long serialVersionUID = -4174407422754524197L;
    
    private final List<WriteRequest> requests;

    /**
     * Creates a new exception.
     */
    public WriteException(WriteRequest request) {
        super();
        this.requests = asRequestList(request);
    }

    /**
     * Creates a new exception.
     */
    public WriteException(WriteRequest request, String s) {
        super(s);
        this.requests = asRequestList(request);
    }

    /**
     * Creates a new exception.
     */
    public WriteException(WriteRequest request, String message, Throwable cause) {
        super(message);
        initCause(cause);
        this.requests = asRequestList(request);
    }

    /**
     * Creates a new exception.
     */
    public WriteException(WriteRequest request, Throwable cause) {
        initCause(cause);
        this.requests = asRequestList(request);
    }

    /**
     * Creates a new exception.
     */
    public WriteException(Collection<WriteRequest> requests) {
        super();
        this.requests = asRequestList(requests);
    }

    /**
     * Creates a new exception.
     */
    public WriteException(Collection<WriteRequest> requests, String s) {
        super(s);
        this.requests = asRequestList(requests);
    }

    /**
     * Creates a new exception.
     */
    public WriteException(Collection<WriteRequest> requests, String message, Throwable cause) {
        super(message);
        initCause(cause);
        this.requests = asRequestList(requests);
    }

    /**
     * Creates a new exception.
     */
    public WriteException(Collection<WriteRequest> requests, Throwable cause) {
        initCause(cause);
        this.requests = asRequestList(requests);
    }

    /**
     * Returns the list of the failed {@link WriteRequest}, in the order of occurrance.
     */
    public List<WriteRequest> getRequests() {
        return requests;
    }

    /**
     * Returns the firstly failed {@link WriteRequest}. 
     */
    public WriteRequest getRequest() {
        return requests.get(0);
    }
    
    private static List<WriteRequest> asRequestList(Collection<WriteRequest> requests) {
        if (requests == null) {
            throw new NullPointerException("requests");
        }
        if (requests.isEmpty()) {
            throw new IllegalArgumentException("requests is empty.");
        }

        // Create a list of requests removing duplicates.
        Set<WriteRequest> newRequests = new MapBackedSet<>(new LinkedHashMap<>());
        for (WriteRequest r: requests) {
            newRequests.add(r.getOriginalRequest());
        }
        
        return Collections.unmodifiableList(new ArrayList<>(newRequests));
    }

    private static List<WriteRequest> asRequestList(WriteRequest request) {
        if (request == null) {
            throw new NullPointerException("request");
        }
        
        List<WriteRequest> requests = new ArrayList<>(1);
        requests.add(request.getOriginalRequest());
        return Collections.unmodifiableList(requests);
    }
}
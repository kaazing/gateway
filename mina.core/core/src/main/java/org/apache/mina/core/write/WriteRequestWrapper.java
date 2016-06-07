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

import java.net.SocketAddress;

import org.apache.mina.core.future.WriteFuture;

/**
 * A wrapper for an existing {@link WriteRequest}.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class WriteRequestWrapper implements WriteRequest {

    private final WriteRequest parentRequest;

    /**
     * Creates a new instance that wraps the specified request.
     */
    public WriteRequestWrapper(WriteRequest parentRequest) {
        if (parentRequest == null) {
            throw new NullPointerException("parentRequest");
        }
        this.parentRequest = parentRequest;
    }

    public SocketAddress getDestination() {
        return parentRequest.getDestination();
    }

    public WriteFuture getFuture() {
        return parentRequest.getFuture();
    }

    public Object getMessage() {
        return parentRequest.getMessage();
    }

    public WriteRequest getOriginalRequest() {
        return parentRequest.getOriginalRequest();
    }

    /**
     * Returns the wrapped request object.
     */
    public WriteRequest getParentRequest() {
        return parentRequest;
    }

    @Override
    public String toString() {
        if (getDestination() == null) {
            return getMessage().toString();
        }
        
        return getMessage().toString() + " => " + getDestination();
    }
}

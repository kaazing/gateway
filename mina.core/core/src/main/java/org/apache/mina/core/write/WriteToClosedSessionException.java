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

import java.util.Collection;


/**
 * An exception which is thrown when one or more write operations were
 * attempted on a closed session.
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class WriteToClosedSessionException extends WriteException {

    private static final long serialVersionUID = 5550204573739301393L;

    public WriteToClosedSessionException(Collection<WriteRequest> requests,
            String message, Throwable cause) {
        super(requests, message, cause);
    }

    public WriteToClosedSessionException(Collection<WriteRequest> requests,
            String s) {
        super(requests, s);
    }

    public WriteToClosedSessionException(Collection<WriteRequest> requests,
            Throwable cause) {
        super(requests, cause);
    }

    public WriteToClosedSessionException(Collection<WriteRequest> requests) {
        super(requests);
    }

    public WriteToClosedSessionException(WriteRequest request, String message,
            Throwable cause) {
        super(request, message, cause);
    }

    public WriteToClosedSessionException(WriteRequest request, String s) {
        super(request, s);
    }

    public WriteToClosedSessionException(WriteRequest request, Throwable cause) {
        super(request, cause);
    }

    public WriteToClosedSessionException(WriteRequest request) {
        super(request);
    }
}
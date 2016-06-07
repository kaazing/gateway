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

import org.apache.mina.core.session.IoSession;


/**
 * Stores {@link WriteRequest}s which are queued to an {@link IoSession}.
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public interface WriteRequestQueue {

    /**
     * Get the first request available in the queue for a session.
     * @param session The session 
     * @return The first available request, if any. 
     */
    WriteRequest poll(IoSession session);
    
    /**
     * Add a new WriteRequest to the session write's queue
     * @param session The session
     * @param writeRequest The writeRequest to add
     */
    void offer(IoSession session, WriteRequest writeRequest);
    
    /**
     * Tells if the WriteRequest queue is empty or not for a session
     * @param session The session to check
     * @return <code>true</code> if the writeRequest is empty
     */
    boolean isEmpty(IoSession session);
    
    /**
     * Removes all the requests from this session's queue.
     * @param session The associated session
     */
    void clear(IoSession session);
    
    /**
     * Disposes any releases associated with the specified session.
     * This method is invoked on disconnection.
     * @param session The associated session
     */
    void dispose(IoSession session);
}

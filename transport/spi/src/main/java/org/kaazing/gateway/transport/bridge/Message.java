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
package org.kaazing.gateway.transport.bridge;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.kaazing.mina.core.buffer.IoBufferEx;
import org.kaazing.mina.netty.util.threadlocal.VicariousThreadLocal;


/**
 * Message is the common top-level superclass for all message types.  Includes optional 
 * storage for different transport encodings of this message.
 */
public class Message implements Cloneable {

    private static final class CacheRef extends VicariousThreadLocal<ConcurrentMap<String, IoBufferEx>> {
        
        @Override
        protected ConcurrentMap<String, IoBufferEx> initialValue() {
            return new ConcurrentHashMap<>();
        }
    }
    
    /**
     * The optional transport encoded buffer cache.
     */
    private ThreadLocal<ConcurrentMap<String, IoBufferEx>> cacheRef;

    /**
     * Initializes the transport buffer cache.
     */
    public void initCache() {
        if (cacheRef != null) {
            throw new IllegalStateException("Cache already initialized");
        }
        cacheRef = new CacheRef();
    }
    
    public ConcurrentMap<String, IoBufferEx> getCache() {
        return cacheRef.get();
    }
    
    public boolean hasCache() {
        return (cacheRef != null);
    }

    // A more verbose (and expensive-to-generate) version of toString(),
    // used mainly for verbose logging.
    public String toVerboseString() {
        return toString();
    }
    
    @Override
    public Object clone() throws CloneNotSupportedException {
        Message clonedMessage = (Message)super.clone();
        clonedMessage.cacheRef = null;
        return clonedMessage;
    }
    
}

/**
 * Copyright (c) 2007-2014 Kaazing Corporation. All rights reserved.
 * 
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.kaazing.gateway.service.echo;

import java.util.concurrent.atomic.AtomicInteger;

import org.apache.mina.core.future.IoFutureListener;
import org.apache.mina.core.future.WriteFuture;
import org.kaazing.gateway.transport.IoHandlerAdapter;
import org.kaazing.gateway.util.LoggingUtils;
import org.kaazing.mina.core.buffer.IoBufferEx;
import org.kaazing.mina.core.session.IoSessionEx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class EchoServiceHandler extends IoHandlerAdapter<IoSessionEx> {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    static final int DEFAULT_REPEAT_COUNT = 1;

    private final int repeatCount;

    EchoServiceHandler() {
        this(DEFAULT_REPEAT_COUNT);
    }

    EchoServiceHandler(int repeatCount) {
        this.repeatCount = repeatCount;
    }

    @Override
    protected void doMessageReceived(final IoSessionEx session, Object message) throws Exception {
        if (message instanceof IoBufferEx) {
            final IoBufferEx buf = (IoBufferEx)message;
            final int maximumStackDepth = 10;
            final AtomicInteger listenerInvocationCount = new AtomicInteger();
            final AtomicInteger listenerPendingCount = new AtomicInteger(maximumStackDepth);
            final AtomicInteger remainingMessages = new AtomicInteger(repeatCount);
            IoFutureListener<WriteFuture> listener = new IoFutureListener<WriteFuture>() {
                @Override
                public void operationComplete(WriteFuture future) {
                    if(listenerInvocationCount.get() < maximumStackDepth) {
                        while(listenerInvocationCount.incrementAndGet() < maximumStackDepth) {
                            if (remainingMessages.decrementAndGet() >= 0) {
                                session.write(buf).addListener(this);
                            }
                        }
                    }
                    
                    if (listenerPendingCount.decrementAndGet() <= 0) {
                        listenerInvocationCount.set(0);
                        listenerPendingCount.set(maximumStackDepth);
                        
                        if (remainingMessages.decrementAndGet() >= 0) {
                            session.write(buf).addListener(this);
                        }
                    }

                }
            };

            if (remainingMessages.decrementAndGet() >= 0) {
                session.write(buf).addListener(listener);
            }
        }
        else {
            for (int i=0; i < repeatCount; i++) {
                session.write(message);
            }
        }
    }

    @Override
    protected void doExceptionCaught(IoSessionEx session, Throwable cause) throws Exception {
        LoggingUtils.log(logger, cause);
    }
}

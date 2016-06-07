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
/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */
/* Differences from class of same name in Mina 2.0.0-RC1 include:
 * 1. Use IoSessionEx.BUFFER_ALLOCATOR instead of calling IoBuffer.allocate
 * 2. Use non-static attribute keys
 */
package org.kaazing.mina.filter.util;

import org.apache.mina.core.filterchain.IoFilter;
import org.apache.mina.core.filterchain.IoFilterAdapter;
import org.apache.mina.core.session.IoEventType;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.write.WriteRequest;

import org.kaazing.mina.core.write.WriteRequestEx;

/**
 * An abstract {@link IoFilter} that simplifies the implementation of
 * an {@link IoFilter} that filters an {@link IoEventType#WRITE} event
 * even if the write request is mutable.
 */
public abstract class WriteRequestFilterEx extends IoFilterAdapter {

    @Override
    public void filterWrite(NextFilter nextFilter, IoSession session,
            WriteRequest writeRequest) throws Exception {
        Object filteredMessage = doFilterWrite(nextFilter, session, writeRequest);
        if (filteredMessage != null && filteredMessage != writeRequest.getMessage()) {
            WriteRequestEx writeRequestEx = (WriteRequestEx) writeRequest;
            writeRequestEx.setMessage(filteredMessage);
        }
        nextFilter.filterWrite(session, writeRequest);
    }

    @Override
    public void messageSent(NextFilter nextFilter, IoSession session,
            WriteRequest writeRequest) throws Exception {
        nextFilter.messageSent(session, writeRequest);
    }

    protected final Object doFilterWrite(NextFilter nextFilter, IoSession session,
            WriteRequest writeRequest) throws Exception {
        Object message = writeRequest.getMessage();
        return doFilterWrite(nextFilter, session, writeRequest, message);
    }

    protected abstract Object doFilterWrite(
            NextFilter nextFilter, IoSession session, WriteRequest writeRequest, Object message) throws Exception;
}

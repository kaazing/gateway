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

package org.kaazing.gateway.transport.wsr;

import static java.util.Collections.singleton;

import java.util.Collection;

import org.kaazing.gateway.transport.dispatch.ProtocolDispatcher;

class RtmpProtocolDispatcher implements ProtocolDispatcher {

    private static final String RTMP_PROTOCOL = "rtmp/1.0";
    private static final Collection<byte[]> RTMP_DISCRIMINATORS = singleton(new byte[] { 0x03 });

    @Override
    public int compareTo(ProtocolDispatcher pd) {
        return protocolDispatchComparator.compare(this, pd);
    }

    @Override
    public String getProtocolName() {
        return RTMP_PROTOCOL;
    }

    @Override
    public Collection<byte[]> getDiscriminators() {
        return RTMP_DISCRIMINATORS;
    }

}

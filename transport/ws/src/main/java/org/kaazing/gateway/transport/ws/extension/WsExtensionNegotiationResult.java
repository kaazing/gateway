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

package org.kaazing.gateway.transport.ws.extension;

import java.util.ArrayList;
import java.util.List;

import org.kaazing.gateway.transport.ws.extension.WsExtension.EndpointKind;

public final class WsExtensionNegotiationResult {
    public static final WsExtensionNegotiationResult OK_EMPTY 
              = new WsExtensionNegotiationResult(new ArrayList<WsExtension>(0), EndpointKind.SERVER);

    enum Status {
        FAILURE,
        OK
    }

    private final Status status;
    private final String failureReason;
    private final ActiveWsExtensions extensions;

    public WsExtensionNegotiationResult(List<WsExtension> extensions, EndpointKind endpointKind) {
        this.status = Status.OK;
        this.failureReason = null;
        this.extensions = new ActiveWsExtensions(extensions, endpointKind);
    }

    public WsExtensionNegotiationResult(Status status,
                                        String failureReason) {
        this.status = status;
        this.failureReason = failureReason;
        this.extensions = null;
    }

    public ActiveWsExtensions getExtensions() {
        return extensions;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public boolean isFailure() {
        return (status.equals(Status.FAILURE));
    }
}

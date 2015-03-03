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

package org.kaazing.gateway.server.context.resolve;

import org.kaazing.gateway.security.AuthenticationContext;

public class DefaultAuthenticationContext implements AuthenticationContext {

    private String httpChallengeScheme;
    private String[] httpHeaders;
    private String[] httpQueryParameters;
    private String[] httpCookieNames;
    private String authorizationMode;
    private String sessionTimeout;


    public DefaultAuthenticationContext(String httpChallengeScheme,
                                        String[] httpHeaders,
                                        String[] httpQueryParameters,
                                        String[] httpCookieNames,
                                        String authorizationMode,
                                        String sessionTimeout) {
        this.httpChallengeScheme = httpChallengeScheme;
        this.httpHeaders = httpHeaders;
        this.httpQueryParameters = httpQueryParameters;
        this.httpCookieNames = httpCookieNames;
        this.authorizationMode = authorizationMode;
        this.sessionTimeout = sessionTimeout;
        this.sessionTimeout = sessionTimeout;
    }

    @Override
    public String getHttpChallengeScheme() {
        return httpChallengeScheme;
    }

    @Override
    public String[] getHttpHeaders() {
        return httpHeaders;
    }

    @Override
    public String[] getHttpQueryParameters() {
        return httpQueryParameters;
    }

    @Override
    public String[] getHttpCookieNames() {
        return httpCookieNames;
    }

    @Override
    public String getAuthorizationMode() {
        return authorizationMode;
    }

    @Override
    public String getSessionTimeout() {
        return sessionTimeout;
    }
}

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

package org.kaazing.gateway.service;

import java.net.URI;
import java.util.Map;

@Deprecated
public interface AcceptOptionsContext {

    Map<String, Object> asOptionsMap();

    URI getInternalURI(URI externalURI);

    /**
     * Add a binding to the accept-options from the given scheme to the given authority.  If a service needs to
     * use a specific accept URI, e.g. wsn://<authority>/<path>, then it might need to add a binding for the
     * scheme of that URI in order to preserve the behavior of the configuration.  In the given example, having
     * <ws.bind>some_port</ws.bind> in the accept-options for the service will result in a failure to bind to
     * some_port due to the change in scheme in the URI.  By allowing the service to add the necessary binding,
     * the configured behavior will be preserved.
     *
     * An example of a service that needs to add bindings at runtime is the HttpBalancerService.
     *
     * @param scheme
     * @param authority
     */
    void addBind(String scheme, String authority);

    /**
     * @return the configured binds for a service
     */
    Map<String, String> getBinds();

    /**
     * Set the map of options on the AcceptOptionsContext.  The options are String key/value pairs.
     * @param options the map of String key/value pairs to set on the AcceptOptionsContext
     */
    void setOptions(Map<String, String> options);

    /**
     * Set the map of default options on the AcceptOptionsContext.  The options are String key/value pairs.
     * If the given default option does not exist in the option map it is added.  Any options already set
     * are left untouched.
     * @param defaultOptions the map of String key/value pairs to set as the default options on the AcceptOptionsContext
     */
    void setDefaultOptions(Map<String, String> defaultOptions);

    class Wrapper implements AcceptOptionsContext {

        private final AcceptOptionsContext delegate;

        public Wrapper(AcceptOptionsContext delegate) {
            this.delegate = delegate;
        }

        @Override
        public void addBind(String scheme, String authority) {
            delegate.addBind(scheme, authority);
        }

        @Override
        public Map<String, String> getBinds() {
            return delegate.getBinds();
        }

        @Override
        public URI getInternalURI(URI externalURI) {
            return delegate.getInternalURI(externalURI);
        }

        @Override
        public Map<String, Object> asOptionsMap() {
            return delegate.asOptionsMap();
        }

        @Override
        public void setOptions(Map<String, String> options) {
            delegate.setOptions(options);
        }

        @Override
        public void setDefaultOptions(Map<String, String> options) {
            delegate.setDefaultOptions(options);
        }
    }
}

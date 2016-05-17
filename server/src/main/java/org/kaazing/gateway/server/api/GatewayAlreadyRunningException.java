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
package org.kaazing.gateway.server.api;

/**
 * Exception raised when an attempt is made to launch a Gateway that is already running.
 */
public class GatewayAlreadyRunningException extends Exception {

    // Auto-generated
    //
    private static final long serialVersionUID = 901385335928386515L;

    public GatewayAlreadyRunningException(String message) {
        super(message);
    }
}

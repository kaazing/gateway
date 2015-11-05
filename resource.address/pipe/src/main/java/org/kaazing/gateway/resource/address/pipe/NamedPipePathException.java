/**
 * Copyright 2007-2015, Kaazing Corporation. All rights reserved.
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
package org.kaazing.gateway.resource.address.pipe;

public class NamedPipePathException extends RuntimeException {

    private static final long serialVersionUID = 1L;
    private String pathFromError;
    private String authorityFromError;

    public NamedPipePathException() {
    }

    public NamedPipePathException(String message) {
        super(message);
    }

    public NamedPipePathException(String pathFromError, String authorityFromError) {
        this.pathFromError = pathFromError;
        this.authorityFromError = authorityFromError;
    }

    public NamedPipePathException(String message, String pathFromError, String authorityFromError) {
        this(message);
        this.pathFromError = pathFromError;
        this.authorityFromError = authorityFromError;
    }

    public String getPathFromError() {
        return this.pathFromError;
    }

    public String getAuthorityFromError() {
        return this.authorityFromError;
    }

}

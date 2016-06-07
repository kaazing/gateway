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
package org.kaazing.gateway.transport.ssl.cert;

/*
 * This exception the superclass of possible certificate exceptions
 * thrown while initializing or binding to (virtual) host:port. 
 */
public abstract class CertificateBindingException
    extends Exception {

    private static final long serialVersionUID = -7403355498072170507L;

    public CertificateBindingException() {
        super();
    }

    public CertificateBindingException(String msg, Throwable cause) {
        super(msg, cause);
    }

    public CertificateBindingException(Throwable cause) {
        super(cause);
    }

    public CertificateBindingException(String msg) {
        super(msg);
    }
}

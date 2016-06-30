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

import java.security.KeyStore;
import java.security.KeyStoreException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultKeySelector
    extends AbstractKeySelector {

    private static final Logger logger = LoggerFactory.getLogger(DefaultKeySelector.class);

    public DefaultKeySelector() {
        super();
    }

    @Override
    public void init(KeyStore keyStore, char[] keyStorePassword)
        throws KeyStoreException {
    }
}

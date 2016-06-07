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
package org.kaazing.gateway.transport.nio;

import org.kaazing.gateway.resource.address.ResourceAddress;

/**
 * {@link TcpExtensionFactorySpi} is part of <i>Service Provider Interface</i> <em>(SPI)</em> developing extensions to the TCP transport.
 * Implementations of this interface are registered by making one or more resource files named
 * META-INF/services/org.kaazing.gateway.transport.nio.spi.TcpExtensionFactorySpi available on the classpath.
 * <p>
 * Developing a TCP extension involves implementing:
 * <UL>
 *   <LI> a sub-class of {@link TcpExtensionFactorySpi}
 *   <LI> a sub-class of {@link TcpExtension}
 * </UL>
 * <p>
 */
public interface TcpExtensionFactorySpi {

    /**
     * Called whenever a TCP accept URI is bound
     * @param address  TCP address being bound (URI plus accept options)
     * @return  A TcpExtension object to be used for all accepted sessions for this bind, or null if no extension is needed
     */
    TcpExtension bind(ResourceAddress address);

}

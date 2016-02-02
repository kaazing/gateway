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
package org.kaazing.gateway.service.http.proxy;

import java.util.Collection;
import java.util.Iterator;

import org.kaazing.gateway.resource.address.uri.URIUtils;
import org.kaazing.gateway.service.ServiceContext;
import org.kaazing.gateway.service.proxy.AbstractProxyService;

/**
 * Http proxy service
 */
public class HttpProxyService extends AbstractProxyService<HttpProxyServiceHandler> {

    @Override
    public String getType() {
        return "http.proxy";
    }

    @Override
    public void init(ServiceContext serviceContext) throws Exception {
        super.init(serviceContext);
        Collection<String> connectURIs = serviceContext.getConnects();
        if (connectURIs == null || connectURIs.isEmpty()) {
            throw new IllegalArgumentException("Missing required element: <connect>");
        }

        checkForTrailingSlashes(serviceContext);

        HttpProxyServiceHandler handler = getHandler();
        handler.setConnectURIs(connectURIs);
        handler.initServiceConnectManager();
    }

    private void checkForTrailingSlashes(ServiceContext serviceContext) {
        Collection<String> acceptURIs = serviceContext.getAccepts();
        Collection<String> connectURIs = serviceContext.getConnects();
        Iterator<String> acceptIterator = acceptURIs.iterator();
        Iterator<String> connectIterator = connectURIs.iterator();

        while (acceptIterator.hasNext() && connectIterator.hasNext()) {
            String acceptPath = URIUtils.getPath(acceptIterator.next());
            String connectPath = URIUtils.getPath(connectIterator.next());
            boolean acceptPathEndsInSlash = acceptPath.endsWith("/");
            boolean connectPathEndsInSlash = connectPath.endsWith("/");

            if (acceptPathEndsInSlash != connectPathEndsInSlash) {
                throw new IllegalArgumentException(
                        "Please fix the gateway configuration file for service '" + serviceContext.getServiceName()
                                + "'. Accept and connect must both either have trailing slashes,"
                                + " or both must not end in slashes.");
            }
        }
    }

    @Override
    protected HttpProxyServiceHandler createHandler() {
        return new HttpProxyServiceHandler();
    }
}

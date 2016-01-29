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

import org.kaazing.gateway.service.ServiceContext;
import org.kaazing.gateway.service.proxy.AbstractProxyService;

import java.net.URI;
import java.util.Collection;
import java.util.Iterator;

/**
 * Http proxy service
 */
public class HttpProxyService extends AbstractProxyService<HttpProxyServiceHandler> {
    private static final String ACCEPT_PATH_IS_SLASH_ERROR_MESSAGE = "Please fix the gateway configuration file for service '"
            + "%s'. Using accept URL '%s' ( empty path in accept must be a slash ) and connect URL '%s'."
            + " Accept and connect must both either have trailing slashes," + " or both must not end in slashes.";
    private static final String CONNECT_PATH_IS_SLASH_ERROR_MESSAGE = "Please fix the gateway configuration file for service '"
            + "%s'. Using accept URL '%s' and connect URL '%s' ( empty path in connect must be a slash )."
            + " Accept and connect must both either have trailing slashes," + " or both must not end in slashes.";
    private static final String ACCEPT_CONNECT_ERROR_MESSAGE = "Please fix the gateway configuration file for service '%s"
            + "'. Accept and connect must both either have trailing slashes," + " or both must not end in slashes.";

    @Override
    public String getType() {
        return "http.proxy";
    }

    @Override
    public void init(ServiceContext serviceContext) throws Exception {
        super.init(serviceContext);
        Collection<URI> connectURIs = serviceContext.getConnects();
        if (connectURIs == null || connectURIs.isEmpty()) {
            throw new IllegalArgumentException("Missing required element: <connect>");
        }

        checkForTrailingSlashes(serviceContext);

        HttpProxyServiceHandler handler = getHandler();
        handler.setConnectURIs(connectURIs);
        handler.initServiceConnectManager();
    }

    private void checkForTrailingSlashes(ServiceContext serviceContext) {
        Collection<URI> acceptURIs = serviceContext.getAccepts();
        Collection<URI> connectURIs = serviceContext.getConnects();
        Iterator<URI> acceptIterator = acceptURIs.iterator();
        Iterator<URI> connectIterator = connectURIs.iterator();

        while (acceptIterator.hasNext() && connectIterator.hasNext()) {
            URI acceptURI = acceptIterator.next();
            URI connectURI = connectIterator.next();
            String acceptPath = acceptURI.getPath();
            String connectPath = connectURI.getPath();
            boolean acceptPathIsSlash = "/".equals(acceptPath);
            boolean connectPathIsSlash = "/".equals(connectPath);
            boolean acceptPathNoTrailingSlash = !acceptPath.endsWith("/") && acceptPath.length() > 1;
            boolean connectPathNoTrailingSlash = !connectPath.endsWith("/") && connectPath.length() > 1;

            if (acceptPathIsSlash && connectPathNoTrailingSlash) {
                throw new IllegalArgumentException(String.format(ACCEPT_PATH_IS_SLASH_ERROR_MESSAGE,
                        serviceContext.getServiceName(), acceptURI, connectURI));
            } else if (acceptPathNoTrailingSlash && connectPathIsSlash) {
                throw new IllegalArgumentException(String.format(CONNECT_PATH_IS_SLASH_ERROR_MESSAGE,
                        serviceContext.getServiceName(), acceptURI, connectURI));
            } else if (acceptPathNoTrailingSlash != connectPathNoTrailingSlash) {
                throw new IllegalArgumentException(
                        String.format(ACCEPT_CONNECT_ERROR_MESSAGE, serviceContext.getServiceName()));
            }
        }
    }

    @Override
    protected HttpProxyServiceHandler createHandler() {
        return new HttpProxyServiceHandler();
    }
}

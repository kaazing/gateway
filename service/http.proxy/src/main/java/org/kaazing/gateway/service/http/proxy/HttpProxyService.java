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

import static java.lang.String.format;

/**
 * Http proxy service
 */
public class HttpProxyService extends AbstractProxyService<HttpProxyServiceHandler> {
    private static final String TRAILING_SLASH_ERROR = "Accept URI is '%s' and connect URI is '%s'. Either both URI should end with / or not.";

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
        handler.init();
    }

    private void checkForTrailingSlashes(ServiceContext serviceContext) {
        Collection<URI> acceptURIs = serviceContext.getAccepts();
        Collection<URI> connectURIs = serviceContext.getConnects();

        assert acceptURIs.size() == 1;
        assert connectURIs.size() == 1;

        URI acceptURI = acceptURIs.iterator().next();
        URI connectURI = connectURIs.iterator().next();
        String acceptPath = acceptURI.getPath();
        String connectPath = connectURI.getPath();

        boolean acceptPathIsSlash = acceptPath.endsWith("/");
        boolean connectPathIsSlash = connectPath.endsWith("/");
        if (acceptPathIsSlash ^ connectPathIsSlash) {
            throw new IllegalArgumentException(format(TRAILING_SLASH_ERROR, acceptURI, connectURI));
        }
    }

    @Override
    protected HttpProxyServiceHandler createHandler() {
        return new HttpProxyServiceHandler();
    }
}

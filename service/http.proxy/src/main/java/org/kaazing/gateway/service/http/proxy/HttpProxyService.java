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
package org.kaazing.gateway.service.http.proxy;

import static java.lang.String.format;

import java.util.Collection;
import java.util.Properties;

import javax.annotation.Resource;

import org.kaazing.gateway.resource.address.uri.URIUtils;
import org.kaazing.gateway.service.ServiceContext;
import org.kaazing.gateway.service.proxy.AbstractProxyService;
import org.kaazing.gateway.util.feature.EarlyAccessFeatures;

/**
 * Http proxy service
 */
public class HttpProxyService extends AbstractProxyService<HttpProxyServiceHandler> {
    private static final String TRAILING_SLASH_ERROR = "The accept URI is '%s' and the connect URI is '%s'. "
            + "One has a trailing slash and one doesn't. Both URIs either need to include a trailing slash or omit it.";

    private Properties configuration;

    @Override
    public String getType() {
        return "http.proxy";
    }

    @Override
    public void init(ServiceContext serviceContext) throws Exception {
        EarlyAccessFeatures.HTTP_PROXY_SERVICE.assertEnabled(configuration, serviceContext.getLogger());
        super.init(serviceContext);
        Collection<String> connectURIs = serviceContext.getConnects();
        if (connectURIs == null || connectURIs.isEmpty()) {
            throw new IllegalArgumentException("Missing required element: <connect>");
        }

        checkForTrailingSlashes(serviceContext);

        HttpProxyServiceHandler handler = getHandler();
        handler.setConnectURIs(connectURIs);
        handler.init();
    }

    @Resource(name = "configuration")
    public void setConfiguration(Properties configuration) {
        this.configuration = configuration;
    }

    private void checkForTrailingSlashes(ServiceContext serviceContext) {
        Collection<String> acceptURIs = serviceContext.getAccepts();
        Collection<String> connectURIs = serviceContext.getConnects();

        assert acceptURIs.size() == 1;
        assert connectURIs.size() == 1;

        String acceptURI = acceptURIs.iterator().next();
        String connectURI = connectURIs.iterator().next();
        String acceptPath = URIUtils.getPath(acceptURI);
        String connectPath = URIUtils.getPath(connectURI);

        boolean acceptPathIsSlash = acceptPath.endsWith("/");
        boolean connectPathIsSlash = connectPath.endsWith("/");
        if (!acceptPathIsSlash) {
            String msg = String.format("The path %s of accept URI %s for service %s needs to end with /",
                    acceptPath, acceptURI, serviceContext.getServiceName());
            throw new IllegalArgumentException(msg);
        }
        if (!connectPathIsSlash) {
            String msg = String.format("The path %s of connect URI %s for service %s needs to end with /",
                    connectPath, connectURI, serviceContext.getServiceName());
            throw new IllegalArgumentException(msg);
        }
    }

    @Override
    protected HttpProxyServiceHandler createHandler() {
        return new HttpProxyServiceHandler();
    }
}

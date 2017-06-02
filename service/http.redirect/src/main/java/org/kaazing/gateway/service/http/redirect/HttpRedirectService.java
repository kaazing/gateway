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
package org.kaazing.gateway.service.http.redirect;

import java.util.Collection;
import java.util.Properties;

import javax.annotation.Resource;

import org.apache.mina.core.session.IoSession;
import org.kaazing.gateway.service.Service;
import org.kaazing.gateway.service.ServiceContext;
import org.kaazing.gateway.service.ServiceProperties;
import org.kaazing.gateway.transport.http.HttpStatus;
import org.kaazing.gateway.util.feature.EarlyAccessFeatures;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Gateway service of type "directory".
 */
public class HttpRedirectService implements Service {

    private final Logger logger = LoggerFactory.getLogger("http.redirect");

    private HttpRedirectServiceHandler handler;
    private ServiceContext serviceContext;
    private Properties configuration;

    @Resource(
            name = "configuration")
    public void setConfiguration(Properties configuration) {
        this.configuration = configuration;
    }

    @Override
    public void init(ServiceContext serviceContext) throws Exception {
        logger.info("Entering http.redirect init");
        EarlyAccessFeatures.HTTP_REDIRECT.assertEnabled(configuration, serviceContext.getLogger());

        Collection<String> acceptURIs = serviceContext.getAccepts();
        for (String acceptURI : acceptURIs) {
            logger.info("acceptURI=" + acceptURI);
            if (!(acceptURI.startsWith("http:") || acceptURI.startsWith("https:"))) {
                logger.info("Throwing exception");
                String msg = String.format(
                        "The accept URI %s for service %s needs to start either with http: or with https:",
                        acceptURI, serviceContext.getServiceName());
                throw new IllegalArgumentException(msg);
            }
        }

        this.serviceContext = serviceContext;
        handler = new HttpRedirectServiceHandler(logger);
        ServiceProperties properties = serviceContext.getProperties();

        String location = properties.get("location");
        if (location == null || "".equals(location)) {
            throw new IllegalArgumentException("Missing required property: location");
        }

        HttpStatus statusCode;
        String strStatusCode = properties.get("status-code");
        if (strStatusCode == null)
            statusCode = HttpStatus.REDIRECT_MULTIPLE_CHOICES;
        else
            statusCode = HttpStatus.getHttpStatus(strStatusCode);

        if (statusCode.code() > 399 || statusCode.code() < 300) {
            throw new IllegalArgumentException(
                    "Wrong value for status-code:" + statusCode + ". Valid values are integers between 300 and 399");
        }

        String cacheControlDirectives = properties.get("cache-control"); // this is null-able

        handler.setLocation(location);
        handler.setStatusCode(statusCode);
        handler.setCacheControl(cacheControlDirectives);
    }

    @Override
    public String getType() {
        return "http.redirect";
    }

    @Override
    public void start() throws Exception {
        serviceContext.bind(serviceContext.getAccepts(), handler);
    }

    @Override
    public void stop() throws Exception {
        quiesce();

        if (serviceContext != null) {
            for (IoSession session : serviceContext.getActiveSessions()) {
                session.close(true);
            }
        }
    }

    @Override
    public void quiesce() throws Exception {
        if (serviceContext != null) {
            serviceContext.unbind(serviceContext.getAccepts(), handler);
        }
    }

    @Override
    public void destroy() throws Exception {
    }

    public HttpRedirectServiceHandler getHandler() {
        return handler;
    }

    public void setHandler(HttpRedirectServiceHandler handler) {
        this.handler = handler;
    }
}

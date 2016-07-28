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
package org.kaazing.gateway.service.turn.rest;

import static org.kaazing.gateway.service.ServiceProperties.LIST_SEPARATOR;

import java.security.KeyStore;
import java.util.Properties;

import javax.annotation.Resource;

import org.apache.mina.core.session.IoSession;
import org.kaazing.gateway.security.SecurityContext;
import org.kaazing.gateway.service.Service;
import org.kaazing.gateway.service.ServiceContext;
import org.kaazing.gateway.service.ServiceProperties;
import org.kaazing.gateway.util.feature.EarlyAccessFeatures;

/**
 * Gateway service of type "turn.rest".
 */
public class TurnRestService implements Service {
    
    private static final String CLASS_PREFIX = "class:";
    
    private TurnRestServiceHandler handler;
    private ServiceContext serviceContext;
    private SecurityContext securityContext;
    private Properties configuration;

    public TurnRestService() {
    }

    @Resource(name = "securityContext")
    public void setSecurityContext(SecurityContext securityContext) {
        this.securityContext = securityContext;
    }

    @Override
    public String getType() {
        return "turn.rest";
    }

    @Override
    public void init(ServiceContext serviceContext) throws Exception {
        this.serviceContext = serviceContext;
        ServiceProperties properties = serviceContext.getProperties();
        KeyStore keystore = securityContext.getKeyStore();
        
        this.configuration = new Properties();
        String propertyName = EarlyAccessFeatures.TURN_REST_SERVICE.getPropertyName();
        this.configuration.setProperty(propertyName, properties.get(propertyName));
        EarlyAccessFeatures.TURN_REST_SERVICE.assertEnabled(configuration, serviceContext.getLogger());
        
        ServiceProperties options = properties.getNested("options").get(0);
        
        StringBuilder u = new StringBuilder();
        for (String uri: properties.getNested("uris").get(0).get("uri").split(LIST_SEPARATOR)) {
            u.append("\"").append(uri).append("\",");
        }
        u.setLength(u.length() - 1);
        String uris = u.toString();
        
        String credentialGeneratorClassName = properties.get("generate.credentials");
        TurnRestCredentialsGenerator credentialGeneratorInstance = null;
        if (credentialGeneratorClassName != null) {
            Class<? extends TurnRestCredentialsGenerator> credentialGeneratorClass;
            if (credentialGeneratorClassName.startsWith(CLASS_PREFIX)) {
                String className = credentialGeneratorClassName.substring(CLASS_PREFIX.length());
                try {
                    Class<?> clazz = Class.forName(className);
                    if (!TurnRestCredentialsGenerator.class.isAssignableFrom(clazz)) {
                        throw new IllegalArgumentException("Invalid credential generator class: " + className);
                    }
                    credentialGeneratorClass = (Class<? extends TurnRestCredentialsGenerator>) clazz;
                    credentialGeneratorInstance = credentialGeneratorClass.newInstance();
                } catch (ClassNotFoundException e) {
                    throw new IllegalArgumentException("Unknown credential generator class: " + className);
                }
            } else {
                throw new IllegalArgumentException("Class name must have \"class:\" prefix.");
            }
        }
        
        handler = new TurnRestServiceHandler(options, keystore, credentialGeneratorInstance, uris);
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
}

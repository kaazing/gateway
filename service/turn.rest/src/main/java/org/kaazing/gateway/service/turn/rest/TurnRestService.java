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

import org.apache.mina.core.session.IoSession;
import org.kaazing.gateway.service.Service;
import org.kaazing.gateway.service.ServiceContext;
import org.kaazing.gateway.service.ServiceProperties;
import org.kaazing.gateway.service.util.ServiceUtils;

/**
 * Gateway service of type "turn.rest".
 */
public class TurnRestService implements Service {
    
    private static final String CLASS_PREFIX = "class:";
    
    private TurnRestServiceHandler handler;
    private ServiceContext serviceContext;

    public TurnRestService() {
    }

    @Override
    public String getType() {
        return "turn.rest";
    }

    @Override
    public void init(ServiceContext serviceContext) throws Exception {
        this.serviceContext = serviceContext;
        
        ServiceProperties properties = serviceContext.getProperties();
        
        String credentialGeneratorClassName = ServiceUtils.getRequiredProperty(properties, "generate.credentials");
        Class<? extends TurnRestCredentialGenerator> credentialGeneratorClass;
        TurnRestCredentialGenerator credentialGeneratorInstance = null;
        
        ServiceProperties options = properties.getNested("options").get(0);
        
        if (credentialGeneratorClassName.startsWith(CLASS_PREFIX)) {
            String className = credentialGeneratorClassName.substring(CLASS_PREFIX.length());
            try {
                Class<?> clazz = Class.forName(className);
                if (!TurnRestCredentialGenerator.class.isAssignableFrom(clazz)) {
                    throw new IllegalArgumentException("Invalid credential generator class: " + className);
                }
                credentialGeneratorClass = (Class<? extends TurnRestCredentialGenerator>) clazz;
                credentialGeneratorInstance = credentialGeneratorClass.newInstance();
            } catch (ClassNotFoundException e) {
                throw new IllegalArgumentException("Unknown credential generator class: " + className);
            }
        } else {
            throw new IllegalArgumentException("Class name must have \"class:\" prefix.");
        }
        handler = new TurnRestServiceHandler(credentialGeneratorInstance, options);
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

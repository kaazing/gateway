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
package org.kaazing.gateway.service.turn.rest.internal;

import java.security.Key;
import java.security.KeyStore;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;
import javax.naming.ConfigurationException;

import org.apache.mina.core.session.IoSession;
import org.kaazing.gateway.security.SecurityContext;
import org.kaazing.gateway.service.Service;
import org.kaazing.gateway.service.ServiceContext;
import org.kaazing.gateway.service.ServiceProperties;
import org.kaazing.gateway.service.turn.rest.TurnRestCredentialsGenerator;
import org.kaazing.gateway.util.Utils;
import org.kaazing.gateway.util.feature.EarlyAccessFeatures;
import org.kaazing.gateway.util.turn.TurnUtils;

/**
 * Gateway service of type "turn.rest".
 */
public class TurnRestService implements Service {

    private static final String CLASS_PREFIX = "class:";
    private static final char DEFAULT_USER_SEPARATOR = ':';
    private static final String DEFAULT_CREDENTIALS_TTL = "86400";
    private static final String DEFAULT_KEY_ALGORITHM = "HmacSHA1";

    private TurnRestServiceHandler handler;
    private ServiceContext serviceContext;
    private SecurityContext securityContext;
    private Properties configuration;

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

        EarlyAccessFeatures.TURN_REST_SERVICE.assertEnabled(getConfiguration(), serviceContext.getLogger());
        ServiceProperties properties = serviceContext.getProperties();

        String urls = properties.get("url");
        TurnRestCredentialsGenerator credentialGeneratorInstance = setUpCredentialsGenerator(properties);

        String ttl = properties.get("credentials.ttl") != null ? properties.get("credentials.ttl") : DEFAULT_CREDENTIALS_TTL;
        handler = new TurnRestServiceHandler(Long.toString(Utils.parseTimeInterval(ttl, TimeUnit.SECONDS, 0)),
                        credentialGeneratorInstance, urls);
    }

    private TurnRestCredentialsGenerator setUpCredentialsGenerator(ServiceProperties properties)
            throws ConfigurationException, InstantiationException, IllegalAccessException {
        TurnRestCredentialsGenerator credentialGeneratorInstance = resolveCredentialsGenerator(properties);

        Key sharedSecret = resolveSharedSecret(properties);
        String algorithm = properties.get("key.algorithm") != null ? properties.get("key.algorithm") : DEFAULT_KEY_ALGORITHM;
        char separator = properties.get("username.separator") != null ? properties.get("username.separator").charAt(0)
                        : DEFAULT_USER_SEPARATOR;

        credentialGeneratorInstance.setAlgorithm(algorithm);
        credentialGeneratorInstance.setSharedSecret(sharedSecret);
        credentialGeneratorInstance.setUsernameSeparator(separator);
        return credentialGeneratorInstance;
    }

    private Key resolveSharedSecret(ServiceProperties properties) {
        KeyStore ks = securityContext.getKeyStore();
        String alias = properties.get("key.alias");
        return TurnUtils.getSharedSecret(ks, alias, securityContext.getKeyStorePassword());
    }

    @SuppressWarnings("unchecked")
    private TurnRestCredentialsGenerator resolveCredentialsGenerator(ServiceProperties properties)
            throws ConfigurationException, InstantiationException, IllegalAccessException {
        String credentialGeneratorClassName = properties.get("credentials.generator");
        TurnRestCredentialsGenerator credentialGeneratorInstance;
        if (credentialGeneratorClassName == null) {
            throw new ConfigurationException("No credential generator specified");
        }
        Class<? extends TurnRestCredentialsGenerator> credentialGeneratorClass;
        if (!credentialGeneratorClassName.startsWith(CLASS_PREFIX)) {
            throw new IllegalArgumentException("Class name must have \"class:\" prefix.");
        }

        String className = credentialGeneratorClassName.substring(CLASS_PREFIX.length());

        try {
            Class<?> clazz = Class.forName(className);
            if (!TurnRestCredentialsGenerator.class.isAssignableFrom(clazz)) {
                throw new IllegalArgumentException("Invalid credential generator class: " + className);
            }
            credentialGeneratorClass = (Class<? extends TurnRestCredentialsGenerator>) clazz;
            credentialGeneratorInstance = credentialGeneratorClass.newInstance();
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("Unknown credential generator class: " + className, e);
        }
        return credentialGeneratorInstance;
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
        // nothing to do when closing TurnRestService
    }

    public Properties getConfiguration() {
        return configuration;
    }

    @Resource(name = "configuration")
    public void setConfiguration(Properties configuration) {
        this.configuration = configuration;
    }
}

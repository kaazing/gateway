/**
 * Copyright (c) 2007-2014, Kaazing Corporation. All rights reserved.
 */

package com.kaazing.demo.protocolinjection;

import java.nio.charset.Charset;
import java.security.Principal;
import java.util.Map;
import java.util.Set;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kaazing.gateway.server.spi.ProtocolInjection;

/**
 * <p>
 * A LoginModule to prepare bytes that will be injected into the back-end connection. The bytes must conform with the
 * protocol expected by the back-end server receiving them.
 * </p>
 * 
 * <p>
 * This LoginModule can be place in the LoginModule chain independently of other LoginModules. One of the earlier
 * LoginModules will be responsible for authentication and establishing the identity associated with the connection.
 * This LoginModule will find that identity and construct the correct bytes to inject that identity into the protocol.
 * </p>
 * 
 * <p>
 * To inject bytes into the connection, create an object that implements the
 * com.kaazing.gateway.server.spi.ProtocolInjection interface and overrides the <code>getInjectableBytes()</code>
 * method. The Kaazing Gateway will see that this principal is present and automatically inject the bytes.
 * </p>
 */
public class IdentityInjectionLoginModule implements LoginModule {

    private static class ProtocolInjectionImpl implements ProtocolInjection {

        private String name;
        private byte[] bytes;

        public ProtocolInjectionImpl(String name, byte[] bytes) {
            this.name = name;
            this.bytes = bytes;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public byte[] getInjectableBytes() {
            return bytes;
        }

    }

    private static final Charset UTF8 = Charset.forName("UTF-8");

    /**
     * <p>
     * Logger for runtime, debugging, or trace information. You set the logger level and where the output should go in
     * GATEWAY_HOME/conf/log4j-config.xml. For example, add the following to the log4-config.xml file to show log output
     * in the Gateway console and the log file:
     * </p>
     * 
     * <pre>
     * <code>&lt;appender name="injection-STDOUT" class="org.apache.log4j.ConsoleAppender">
     *     &lt;layout class="org.apache.log4j.PatternLayout">
     *         &lt;param name="ConversionPattern" value="[PI] %-5p %m%n"/>
     *     &lt;/layout>
     * &lt;/appender>
     * 
     * &lt;logger name="com.kaazing.demo.protocolinjection.IdentityInjectionLoginModule">
     *     &lt;level value="trace"/>
     *     &lt;appender-ref ref="injection-STDOUT"/>
     * &lt;/logger></code>
     * </pre>
     */
    private static final Logger logger = LoggerFactory.getLogger(IdentityInjectionLoginModule.class.getName());

    /**
     * The name of the element in the configuration to specify the identity principal class name.
     * 
     * @see #identityPrincipalClass
     */
    private static final String ELEMENT_IDENTITY_PRINCIPAL_CLASS = "identityPrincipalClass";

    /**
     * The subject associated with this session (i.e., connection).
     */
    private Subject subject;

    /**
     * The Principal class containing the identity to be injected. This LoginModule assumes another LoginModule earlier
     * in the chain has resolved and validated the identity. In this LoginModule we will retrieve that identity from the
     * class specified by this variable.
     * 
     * This is set from the configuration. For example:
     * 
     * <pre>
     * <code>&lt;login-module>;
     *   &lt;type>;class:com.kaazing.demo.protocolinjection.IdentityInjectionLoginModule&lt;/type>;
     *   &lt;success>;required&lt;/success>;
     *   &lt;options>;
     *     &lt;identityPrincipalClass>;com.kaazing.gateway.server.auth.config.parse.DefaultUserConfig&lt;/identityPrincipalClass>;
     *   &lt;/options>;
     * &lt;/login-module>;</code>
     * </pre>
     * 
     * This setting is required from the configuration.
     * 
     * @see #ELEMENT_IDENTITY_PRINCIPAL_CLASS
     */
    private Class<Principal> identityPrincipalClass;

    @SuppressWarnings("unchecked")
    @Override
    public void initialize(Subject subject, CallbackHandler handler, Map<String, ?> sharedState, Map<String, ?> options) {

        logger.debug("initialize()");

        this.subject = subject;

        // Get the class name from the configuration of the Principal that will contain the identity. The identity will
        // have been set by a LoginModule earlier in the LoginModule chain.
        String identityPrincipalClassName = (String) options.get(ELEMENT_IDENTITY_PRINCIPAL_CLASS);
        if ( identityPrincipalClassName != null ) {
            // Validate that the class supplied by the configuration can be found.
            try {
                identityPrincipalClass = (Class<Principal>) Class.forName(identityPrincipalClassName);
            }
            catch (ClassNotFoundException e) {
                logger.error("The class name for the identity Principal specified in configuration was not found.", e);
                throw new RuntimeException(e);
            }
        }
        else {
            String errorMessage = "No class name for the identity Principal was specified in the configuration.";
            RuntimeException e = new RuntimeException(errorMessage);
            logger.error(errorMessage, e);
            throw e;
        }
        logger.debug("Identity principal class: {}", identityPrincipalClass.getName());

    }

    @Override
    public boolean login() throws LoginException {
        logger.debug("login()");

        return true;
    }

    @Override
    public boolean commit() throws LoginException {
        logger.debug("commit()");

        // The principal that will be attached the LoginResult upon a successful authentication, containing the bytes to
        // be injected into the protocol.
        ProtocolInjection identityInjection = null;

        // Check that a LoginModule earlier in the chain added a principal of the expected type. If so, then
        // create a principal with the bytes to be injected.
        //
        // The check needs to be done here in the commit() method because it is the first time this LoginModule can be
        // invoked after previous LoginModules have had a chance to add their principals.
        Set<Principal> principals = subject.getPrincipals(identityPrincipalClass);
        if ( !principals.isEmpty() ) {
            // Use the first, and presumably only, Principal of the specified class. A more advanced implementation
            // could loop over multiple Principals of the same type and differentiate them by name.
            Principal p = principals.iterator().next();

            String identity = p.getName();

            // Create the bytes which conform to the protocol that the back-end server is expecting. In this fictitious
            // example, the back-end server is expecting the frame to contain:
            //
            // Id=<identity>\n
            //
            String frame = String.format("Id=%s\n", identity);
            byte[] bytes = frame.getBytes(UTF8);

            // Create the Principal that will hold the bytes to inject. If this Principal type is present, the Gateway
            // will automatically inject the bytes after the entire LoginModule chain completed successfully.
            identityInjection = new ProtocolInjectionImpl(p.getName() + ".injected", bytes);

            logger.debug("Principal for injection: name={}, injectable bytes={}", identityInjection.getName(),
                    identityInjection.getInjectableBytes());

            // Add the principal to the subject, so it will be available to the Gateway to inject upon
            // successfully establishing a connection to the back-end server.
            subject.getPrincipals().add(identityInjection);
        }

        if ( identityInjection == null ) {
            logger.debug("No identity found, so no bytes will be injected");
            return false;
        }

        return true;
    }

    @Override
    public boolean logout() throws LoginException {
        logger.debug("logout()");
        // To clean up fully, remove any principals that this LoginModule added.
        subject.getPrincipals().removeAll(subject.getPrincipals(ProtocolInjectionImpl.class));
        return true;
    }

    @Override
    public boolean abort() throws LoginException {
        logger.debug("abort()");
        // To clean up fully, remove any principals that this LoginModule added.
        subject.getPrincipals().removeAll(subject.getPrincipals(ProtocolInjectionImpl.class));
        return false;
    }
}

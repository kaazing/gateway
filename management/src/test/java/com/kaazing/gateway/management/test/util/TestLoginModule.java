/**
 * Copyright (c) 2007-2015, Kaazing Corporation. All rights reserved.
 */

package com.kaazing.gateway.management.test.util;

import java.security.Principal;
import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;

/**
 * Login module that always logs in
 *
 */
public class TestLoginModule implements LoginModule {
    private static final Principal ROLE_PRINCIPAL = new RolePrincipal();

    private Subject subject;

    @Override
    public void initialize(Subject subject, CallbackHandler callbackHandler, Map<String, ?> sharedState,
        Map<String, ?> options) {
        this.subject = subject;
    }

    @Override
    public boolean login() throws LoginException {
        return true;
    }

    @Override
    public boolean commit() throws LoginException {
        subject.getPrincipals().add(ROLE_PRINCIPAL);
        return true;
    }

    @Override
    public boolean abort() throws LoginException {
        return true;
    }

    @Override
    public boolean logout() throws LoginException {
        return true;
    }

    static class RolePrincipal implements Principal {
        @Override
        public String getName() {
            return "AUTHORIZED";
        }
    }

}

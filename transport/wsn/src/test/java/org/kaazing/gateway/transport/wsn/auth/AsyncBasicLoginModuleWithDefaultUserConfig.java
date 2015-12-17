package org.kaazing.gateway.transport.wsn.auth;

import javax.security.auth.login.LoginException;

import org.kaazing.gateway.security.auth.config.parse.DefaultUserConfig;

public class AsyncBasicLoginModuleWithDefaultUserConfig extends AsyncBasicLoginModule {
    private static final String TEST_PRINCIPAL_PASS = "testPrincipalPass";
    private static final String TEST_PRINCIPAL_NAME = "testPrincipalName";
    private DefaultUserConfig defaultPrincipal = new DefaultUserConfig();

    @Override
    public boolean commit() throws LoginException {
        if (super.commit()) {
            defaultPrincipal.setName(TEST_PRINCIPAL_NAME);
            defaultPrincipal.setPassword(TEST_PRINCIPAL_PASS);
            subject.getPrincipals().add(defaultPrincipal);
            return true;
        }
        return false;
    }

    @Override
    public boolean logout() throws LoginException {
        super.logout();
        subject.getPrincipals().remove(defaultPrincipal);
        return true;
    }
}
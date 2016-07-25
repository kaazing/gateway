package org.kaazing.gateway.service.turn.rest;

import java.security.Principal;
import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;

public class TestTurnRestLoginModule implements LoginModule {

    // initial state
    private Subject subject;
    private Map<String, ?> sharedState;

    private RolePrincipal userPrincipal;
    
    @Override
    public void initialize(Subject subject, CallbackHandler callbackHandler, Map<String, ?> sharedState,
        Map<String, ?> options) {
        this.subject = subject;
        this.sharedState = sharedState;
    }

    @Override
    public boolean login() throws LoginException {
        return true;
    }

    @Override
    public boolean commit() throws LoginException {
        String username = (String)sharedState.get("javax.security.auth.login.name");
        userPrincipal = new RolePrincipal(username);
        subject.getPrincipals().add(userPrincipal);
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
    
    private static class RolePrincipal implements Principal {

        private final String name;

        public RolePrincipal(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String toString() {
            return("Role:  " + name);
        }

        @Override
        public boolean equals(Object o) {
            if (o == null)
                return false;

            if (this == o)
                return true;

            if (!(o instanceof RolePrincipal))
                return false;
            RolePrincipal that = (RolePrincipal)o;

            return this.getName().equals(that.getName());
        }

        @Override
        public int hashCode() {
            return name.hashCode();
        }
    }
}
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
package org.kaazing.gateway.transport.wsn.auth;

import java.security.Principal;
import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.FailedLoginException;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;

/**
 *
 * A simple basic authentication login module. It also checks
 * execution thread and filter suspend/resume
 */
public class AsyncBasicLoginModule implements LoginModule {

    private static final int LOGIN_MODULE_TIME = 200;     // In ms

    // initial state
    protected Subject subject;
    private Map<String, ?> sharedState;

    // the authentication status
    private boolean succeeded;
    private boolean commitSucceeded;

    // testUser's RolePrincipal
    private RolePrincipal userPrincipal;

    @Override
    public void initialize(Subject subject, CallbackHandler callbackHandler,
                           Map<String, ?> sharedState, Map<String, ?> options) {
        this.subject = subject;
        this.sharedState = sharedState;
    }

    @Override
    public boolean login() throws LoginException {
        // Verify the execution thread
        String expectedThread = "gtwy_bg_tasks";
        String gotThread = Thread.currentThread().getName();

        if (!gotThread.startsWith(expectedThread)) {
            throw new FailedLoginException("Wrong LoginContext task's execution thread, Expected="+
                    expectedThread+", but Got="+gotThread);
        }

        // Add some sleep to test filter suspend/resume
        try {
            Thread.sleep(LOGIN_MODULE_TIME);
        } catch (InterruptedException ie) {
            throw new FailedLoginException("LoginContext task thread is interrupted");
        }

        // verify the username/password
        String username = (String)sharedState.get("javax.security.auth.login.name");
        char[] password = (char[])sharedState.get("javax.security.auth.login.password");
        if (username == null && password == null) {
            throw new FailedLoginException("No UserName/Password to authenticate");
        }
        if (username.equals("joe") && password.length == 7 && password[0] == 'w' &&
                password[1] == 'e' && password[2] == 'l' && password[3] == 'c' &&
                password[4] == 'o' && password[5] == 'm' && password[6] == 'e') {
            // authentication succeeded!!!
            succeeded = true;
            return true;
        } else {
            // authentication failed
            succeeded = false;
            throw new FailedLoginException("UserName/Password is Incorrect");
        }
    }

    @Override
    public boolean commit() throws LoginException {
        if (!succeeded) {
            return false;
        } else {
            // add a Principal (authenticated identity) to the Subject
            userPrincipal = new RolePrincipal("USER");
            subject.getPrincipals().add(userPrincipal);
            commitSucceeded = true;
            return true;
        }
    }

    @Override
    public boolean abort() throws LoginException {
        if (!succeeded) {
            return false;
        } else if (!commitSucceeded) {
            // login succeeded but overall authentication failed
            succeeded = false;
            userPrincipal = null;
        } else {
            // overall authentication succeeded and commit succeeded,
            // but someone else's commit failed
            logout();
        }
        return true;
    }

    @Override
    public boolean logout() throws LoginException {
        subject.getPrincipals().remove(userPrincipal);
        succeeded = false;
        commitSucceeded = false;
        userPrincipal = null;
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

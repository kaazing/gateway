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
package org.kaazing.gateway.security.auth;

import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;

public abstract class BaseStateDrivenLoginModule implements LoginModule {

    protected enum State { INITIALIZE_REQUIRED, INITIALIZE_COMPLETE, LOGIN_COMPLETE, COMMIT_COMPLETE }

    protected State state = State.INITIALIZE_REQUIRED;
    protected Subject subject;
    protected CallbackHandler handler;
    protected Map<String, ?> sharedState;
    protected Map<String, ?> options;

    @Override
    public void initialize(Subject subject,
                           CallbackHandler callbackHandler,
                           Map<String, ?> sharedState,
                           Map<String, ?> options) {
        this.subject = subject;
        this.handler = callbackHandler;
        this.sharedState = sharedState;
        this.options = options;
        this.state = State.INITIALIZE_COMPLETE;

    }

    @Override
    public boolean login() throws LoginException {
        switch (state) {
            case INITIALIZE_COMPLETE:
                return login0();
            case LOGIN_COMPLETE:
                return login0();
            case COMMIT_COMPLETE:
                return true;
            case INITIALIZE_REQUIRED:
            default:
                throw new LoginException("Login module is not initialized");
        }
    }

    @Override
    public boolean commit() throws LoginException {
        switch (state) {
            case COMMIT_COMPLETE:
                return true;
            case INITIALIZE_COMPLETE:
                logout0();
                return false;
            case LOGIN_COMPLETE:
                commit0();
                return true;
            default:
            case INITIALIZE_REQUIRED:
                throw new LoginException("Login module is not initialized");
        }
    }

    private boolean login0() throws LoginException {
        boolean result = doLogin();
        if (result) {
            this.state = State.LOGIN_COMPLETE;
        }
        return result;
    }


    private boolean commit0()  throws LoginException {
        boolean result = doCommit();
        if (result) {
            this.state = State.COMMIT_COMPLETE;
        }
        return result;
    }

    private boolean logout0() throws LoginException {
        boolean result = doLogout();
        if (result) {
            this.state = State.INITIALIZE_COMPLETE;
        }
        return result;
    }


    @Override
    public boolean abort() throws LoginException {
        switch (state) {
            case COMMIT_COMPLETE:
            case LOGIN_COMPLETE:
                logout0();
                return true;
            default:
            case INITIALIZE_COMPLETE:
            case INITIALIZE_REQUIRED:
                return false;
        }
    }



    @Override
    public boolean logout() throws LoginException {
        switch (state) {
            case COMMIT_COMPLETE:
                logout0();
                return true;
            case INITIALIZE_REQUIRED:
                throw new LoginException("Login module is not initialized");
            case INITIALIZE_COMPLETE:
            case LOGIN_COMPLETE:
            default:
                return logout0();
        }
    }


    protected abstract boolean doLogin() throws LoginException;

    protected abstract boolean doCommit() throws LoginException;

    protected abstract boolean doLogout() throws LoginException;

}


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
package org.kaazing.gateway.transport.http.acceptor.specification.rfc7235;

import java.io.File;
import java.io.IOException;
import java.security.Principal;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.FailedLoginException;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;

import org.kaazing.gateway.security.auth.config.JaasConfig;
import org.kaazing.gateway.security.auth.config.RoleConfig;
import org.kaazing.gateway.security.auth.config.UserConfig;
import org.kaazing.gateway.security.auth.config.parse.JaasConfigParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class FileLoginModule implements LoginModule {

    public static final String CLASS_NAME = FileLoginModule.class.getName();
    public static final Logger LOG = LoggerFactory.getLogger(CLASS_NAME);

    private static final String FILE_KEY = "file";
    private static final String NAME = "javax.security.auth.login.name";
    private static final String PWD = "javax.security.auth.login.password";

    private enum State { INITIALIZE_REQUIRED, INITIALIZE_COMPLETE, LOGIN_COMPLETE, COMMIT_COMPLETE }
    private static final ConcurrentMap<String, JaasConfig> SHARED_STATE = new ConcurrentHashMap<>();

    private State state;
    private Subject subject;
    private UserConfig user;
    private Collection<RoleConfig> userRoles;
    private CallbackHandler handler;
    private JaasConfig jaasConfig;
    private Map sharedState;
    private boolean tryFirstPass;
    private boolean debug;

    private String username;
    private char[] password;

    public FileLoginModule() {
        state = State.INITIALIZE_REQUIRED;
    }

    @Override
    public void initialize(Subject subject, CallbackHandler callback,
                           Map<String, ?> sharedState, Map<String, ?> options) {

        this.sharedState = sharedState;
        this.tryFirstPass = "true".equalsIgnoreCase((String) options.get("tryFirstPass"));
        this.debug = "true".equalsIgnoreCase((String) options.get("debug"));

        // TODO: retrieve parsed XML from sharedState
        String jaasFilename = (String) options.get(FILE_KEY);
        if (jaasFilename == null) {
            throw new RuntimeException("Missing required option \"" + FILE_KEY + "\" to locate JAAS configuration file");
        }

        JaasConfig jaasConfig = SHARED_STATE.get(jaasFilename);
        if (jaasConfig == null) {
            File jaasFile = new File(jaasFilename);
            if (!jaasFile.isAbsolute()) {
                String configDir = (String) options.get("GATEWAY_CONFIG_DIRECTORY");
                if (configDir != null) {
                    jaasFile = new File(configDir, jaasFilename);
                }
            }

            if (jaasFile.exists() && jaasFile.isFile()) {
                try {
                    jaasConfig = new JaasConfigParser().parse(jaasFile.toURI().toURL());
                    SHARED_STATE.put(jaasFilename, jaasConfig);
                }
                catch (Exception e) {
                    throw new RuntimeException(e);
                }

            } else {
//  throw new RuntimeException(
//      String.format("Unable to use '%s' for file-based login: File does not exist or is directory", jaasFile));
                        }
        }

        this.state = State.INITIALIZE_COMPLETE;
        this.subject = subject;
        this.handler = callback;
        this.jaasConfig = jaasConfig;
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
                 return false;
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

             case INITIALIZE_REQUIRED:
             default:
                 throw new LoginException("Login module is not initialized");
        }
    }

    @Override
    public boolean abort() throws LoginException {
        switch (state) {
            case COMMIT_COMPLETE:
            case LOGIN_COMPLETE:
                logout0();
                return true;

            case INITIALIZE_COMPLETE:
            case INITIALIZE_REQUIRED:
            default:
                return false;
        }
    }

    private boolean login0() throws LoginException {

        if (tryFirstPass) {
            try {
                boolean result = attemptAuthenticate(true);
                if (result) {
                    return true;

                } else {
                    cleanState();
                    if (debug) {
                        LOG.debug("[FileLoginModule] " + "read from shared state failed.");
                    }
                }
            } catch (LoginException le) {
                cleanState();
                if (debug) {
                    LOG.debug("[FileLoginModule] " + "login failed: " + le.getMessage());
                }
            }
        }

        try {
            return attemptAuthenticate(false);

        } catch (LoginException loginException) {
            cleanState();
            if (debug) {
                LOG.debug("[FileLoginModule] " + "regular authentication failed: " + loginException.getMessage());
            }
            throw loginException;
        }
    }

    private void cleanState() {
        user = null;
        userRoles = null;
        username = null;
        if (password != null) {
            Arrays.fill(password, (char) 0);
        }
        password = null;
    }

    private boolean attemptAuthenticate(boolean useSharedState)
        throws LoginException {

        getUsernamePassword(useSharedState);

        if (username == null) {
            LOG.debug("[FileLoginModule] " + "username not found, returning false");
            return false;
        }

        if (password == null) {
            LOG.debug("[FileLoginModule] " + "password not found, returning false");
            return false;
        }

        Map<String, UserConfig> users = jaasConfig.getUsers();
        Map<String, RoleConfig> roles = jaasConfig.getRoles();
        user = users.get(username);

        if (user == null) {
            String usernameNotFoundStr = username;


            cleanState();

            if (LOG.isTraceEnabled()) {
                LOG.trace(String.format("Did not find username '%s'.", usernameNotFoundStr));
            }

            throw new FailedLoginException("Missing username");
        }

        boolean passwordOK = new String(password).equals(user.getPassword());
        if (!passwordOK) {
            cleanState();

            throw new FailedLoginException("Wrong password");
        }

        userRoles = new HashSet<>();

        // assign user the transitive closure of authorized roles
        Queue<String> roleNames = new LinkedList<>();
        roleNames.addAll(user.getRoleNames());
        while (!roleNames.isEmpty()) {
            String roleName = roleNames.poll();
            RoleConfig role = roles.get(roleName);
            if (role == null) {
                cleanState();
                throw new IllegalArgumentException("Unrecognized role \"" + roleName + "\"");
            }

            if (userRoles.add(role)) {
                roleNames.addAll(role.getRoleNames());
            }
        }

        state = State.LOGIN_COMPLETE;
        return true;
    }

    private void getUsernamePassword(boolean useSharedState) throws LoginException {
        if (useSharedState) {
            username = (String) sharedState.get(NAME);
            password = (char[]) sharedState.get(PWD);
            return;
        }

        NameCallback nameCB = new NameCallback("username");
        PasswordCallback passwordCB = new PasswordCallback("password", false);

        try {
            handler.handle(new Callback[] { nameCB, passwordCB });

        } catch (IOException e) {
//            throw (LoginException) (new LoginException(e.getMessage()).initCause(e));
          return;

        } catch (UnsupportedCallbackException e) {
            if (LOG.isTraceEnabled()) {
                LOG.trace("[FileLoginModule] - UnsupportedCallbackException handling name, password callbacks.");
            }
            return;
        }

        username = nameCB.getName();
        password = passwordCB.getPassword();
        passwordCB.clearPassword();
    }

    private void logout0() throws LoginException {
        Set<Principal> principals = subject.getPrincipals();
        principals.remove(user);
        if (userRoles != null) {
            principals.removeAll(userRoles);
        }

        user = null;
        userRoles = null;

        state = State.INITIALIZE_COMPLETE;
    }

    private void commit0() throws LoginException {
        Set<Principal> principals = subject.getPrincipals();
        principals.add(user);
        principals.addAll(userRoles);

        state = State.COMMIT_COMPLETE;
    }
}

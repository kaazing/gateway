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
package org.kaazing.gateway.server.context.resolve;

import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;

/**
 * A simple basic authentication login module.
 */
public class CustomParserLoginModule implements LoginModule {

    @Override
    public boolean abort() throws LoginException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean commit() throws LoginException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void initialize(Subject arg0, CallbackHandler arg1, Map<String, ?> arg2, Map<String, ?> arg3) {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean login() throws LoginException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean logout() throws LoginException {
        // TODO Auto-generated method stub
        return false;
    }
}

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

import javax.security.auth.login.LoginException;

import org.kaazing.gateway.security.auth.YesLoginModule;

/**
 * A {@link YesLoginModule} which adds a delay to login() execution and also
 * checks the execution thread(shouldn't be I/O thread)
 */
public class DelayedLoginModule extends YesLoginModule {

    static final int LOGIN_MODULE_TIME = 200;     // In ms

    @Override
    public boolean login() throws LoginException {
        String expectedThread = "gtwy_bg_tasks";
        String gotThread = Thread.currentThread().getName();

        if (!gotThread.startsWith(expectedThread)) {
            throw new LoginException("Wrong LoginContext task's execution thread, Expected="+
                    expectedThread+", but Got="+gotThread);
        }

        try {
            Thread.sleep(LOGIN_MODULE_TIME);
        } catch (InterruptedException ie) {
            throw new LoginException("LoginContext task thread is interrupted");
        }

        return super.login();
    }

}

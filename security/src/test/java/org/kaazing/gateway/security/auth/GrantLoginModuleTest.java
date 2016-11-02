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

import static org.junit.Assert.*;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginException;

import static java.util.Collections.singletonMap;

import org.junit.Test;

public class GrantLoginModuleTest {

    @Test
    public void testLoginAndCommit() throws LoginException {
        String name = "AUTHORIZED";
        Map<String, ?> options = singletonMap("name", name);
        Subject subject = new Subject();
        GrantLoginModule grantLoginModule = new GrantLoginModule();
        CallbackHandler handler = cbs -> {};
        Map<String, Object> sharedState = new HashMap<>();
        grantLoginModule.initialize(subject, handler, sharedState, options);
        grantLoginModule.login();
        grantLoginModule.commit();
        assertEquals(1, subject.getPrincipals().size());
        Principal principal = subject.getPrincipals().iterator().next();
        assertTrue(principal instanceof GrantPrincipal);
        assertEquals(name, principal.getName());
    }

}

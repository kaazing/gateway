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
package org.kaazing.gateway.transport.http;

import static org.junit.Assert.assertEquals;

import java.security.Principal;
import java.util.ArrayList;
import java.util.Collection;
import javax.management.remote.JMXPrincipal;

import javax.security.auth.Subject;

import org.junit.Test;

import org.kaazing.gateway.resource.address.IdentityResolver;
import org.kaazing.gateway.resource.address.http.HttpIdentityResolver;
import org.kaazing.gateway.security.auth.config.parse.DefaultUserConfig;

public class HttpIdentityResolverTest {

    private static final String FIRST = "first";
    private static final String SECOND = "second";

    @Test
    public void testResolve() {
        Collection<Class<? extends Principal>> userPrincipalClasses = new ArrayList<>();
        IdentityResolver resolver = new HttpIdentityResolver(userPrincipalClasses);
        Subject subject = buildSubject();

        //Verify first principal is resolved
        userPrincipalClasses.add(JMXPrincipal.class);
        userPrincipalClasses.add(DefaultUserConfig.class);
        assertEquals(resolver.resolve(subject), FIRST);

        //Verify second principal is resolved
        userPrincipalClasses.clear();
        userPrincipalClasses.add(DefaultUserConfig.class);
        userPrincipalClasses.add(JMXPrincipal.class);
        assertEquals(resolver.resolve(subject), SECOND);
    }

    /**
     * Method building subject
     * @return
     */
    private Subject buildSubject() {
        Subject subject = new Subject();
        subject.getPrincipals().add(new JMXPrincipal(FIRST));
        DefaultUserConfig second = new DefaultUserConfig();
        second.setName(SECOND);
        subject.getPrincipals().add(second);
        return subject;
    }

}
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
        Collection<Class<? extends Principal>> userPrincipalClasses = new ArrayList<Class<? extends Principal>>();
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
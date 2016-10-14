package org.kaazing.gateway.security.auth;

import static org.junit.Assert.*;

import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.login.LoginException;

import static java.util.Collections.singletonMap;

import org.junit.Test;

public class GrantLoginModuleTest {

	@Test
	public void testLoginAndCommit() throws LoginException {
		String rolename = "AUTHORIZED";
		Map<String, ?> options = singletonMap("grant.role", rolename);
		Subject subject=new Subject();
		GrantLoginModule grantLoginModule=new GrantLoginModule();
		grantLoginModule.initialize(subject, null, null, options);
		grantLoginModule.login();
		grantLoginModule.commit();
		assertEquals(1, subject.getPrincipals().size());
		assertEquals(rolename, subject.getPrincipals().iterator().next().getName());
	}

}

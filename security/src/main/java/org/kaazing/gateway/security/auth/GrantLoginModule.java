package org.kaazing.gateway.security.auth;

import java.security.Principal;
import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;

public class GrantLoginModule implements LoginModule {
	private Principal principal;
	private Subject subject;
	private String grantRole;
	@Override
	public void initialize(Subject subject, CallbackHandler callbackHandler, Map<String, ?> sharedState, Map<String, ?> options) {
		this.subject=subject;
		this.grantRole = (String) options.get("grant.role");
	}

	@Override
	public boolean login() throws LoginException {
		this.principal = new Principal() {
			public String getName() {
				return grantRole;
			}
		};

		return true;
	}

	@Override
	public boolean commit() throws LoginException {
		this.subject.getPrincipals().add(this.principal);
		return true;
	}

	@Override
	public boolean abort() throws LoginException {
		this.subject.getPrincipals().remove(this.principal);	
		return true;
	}

	@Override
	public boolean logout() throws LoginException {
		this.subject.getPrincipals().remove(this.principal);	
		return true;
	}

}

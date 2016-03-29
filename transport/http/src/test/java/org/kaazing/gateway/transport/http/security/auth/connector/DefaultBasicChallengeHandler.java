package org.kaazing.gateway.transport.http.security.auth.connector;

import java.net.PasswordAuthentication;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import org.kaazing.gateway.security.connector.auth.ChallengeHandler;
import org.kaazing.gateway.security.connector.auth.ChallengeRequest;
import org.kaazing.gateway.security.connector.auth.ChallengeResponse;

/**
 * Challenge handler for Basic authentication. See RFC 2617.
 */
public class DefaultBasicChallengeHandler extends BasicChallengeHandler {

    // ------------------------------ FIELDS ------------------------------

    private static final String CLASS_NAME = DefaultBasicChallengeHandler.class.getName();
    private static final Logger LOG = Logger.getLogger(CLASS_NAME);

    private Map<String, LoginHandler> loginHandlersByRealm = new ConcurrentHashMap<String, LoginHandler>();

    @Override
    public void setRealmLoginHandler(String realm, LoginHandler loginHandler) {
        if (realm == null) {
            throw new NullPointerException("realm");
        }
        if (loginHandler == null) {
            throw new NullPointerException("loginHandler");
        }

        loginHandlersByRealm.put(realm, loginHandler);
    }

    /**
     * If specified, this login handler is responsible for assisting in the
     * production of challenge responses.
     */
    private LoginHandler loginHandler;

    /**
     * Provide a login handler to be used in association with this challenge handler.
     * The login handler is used to assist in obtaining credentials to respond to challenge requests.
     *
     * @param loginHandler a login handler for credentials.
     */
    public BasicChallengeHandler setLoginHandler(LoginHandler loginHandler) {
        this.loginHandler = loginHandler;
        return this;
    }

    /**
     * Get the login handler associated with this challenge handler.
     * A login handler is used to assist in obtaining credentials to respond to challenge requests.
     *
     * @return a login handler to assist in providing credentials, or {@code null} if none has been established yet.
     */
    public LoginHandler getLoginHandler() {
        return loginHandler;
    }

    @Override
    public boolean canHandle(ChallengeRequest challengeRequest) {
        return challengeRequest != null && "Basic".equals(challengeRequest.getAuthenticationScheme());
    }

    @Override
    public ChallengeResponse handle(ChallengeRequest challengeRequest) {

        LOG.entering(CLASS_NAME, "handle",
                new String[]{challengeRequest.getLocation(), challengeRequest.getAuthenticationParameters()});

        if (challengeRequest.getLocation() != null) {

            // Start by using this generic Basic handler
            LoginHandler loginHandler = getLoginHandler();

            // Try to delegate to a realm-specific login handler if we can
            String realm = RealmUtils.getRealm(challengeRequest);
            if (realm != null && loginHandlersByRealm.get(realm) != null) {
                loginHandler = loginHandlersByRealm.get(realm);
            }
            LOG.finest("BasicChallengeHandler.getResponse: login handler = " + loginHandler);
            if (loginHandler != null) {
                PasswordAuthentication creds = loginHandler.getCredentials();
                if (creds != null && creds.getUserName() != null && creds.getPassword() != null) {
                    return BasicChallengeResponseFactory.create(creds, this);
                }
            }
        }
        return null;
    }

    static class BasicChallengeResponseFactory {

        public static ChallengeResponse create(PasswordAuthentication creds, ChallengeHandler nextChallengeHandler) {
            String unencoded = String.format("%s:%s", creds.getUserName(), new String(creds.getPassword()));
            byte[] bytesEncoded = Base64.getEncoder().encode(unencoded.getBytes());
            return new ChallengeResponse(new String(bytesEncoded), nextChallengeHandler);
        }
    }

}

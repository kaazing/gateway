package org.kaazing.gateway.security.connector.auth;

public interface ChallengeHandler {
    
    public boolean canHandle(ChallengeRequest challengeRequest);
    public abstract ChallengeResponse handle(ChallengeRequest challengeRequest);

}

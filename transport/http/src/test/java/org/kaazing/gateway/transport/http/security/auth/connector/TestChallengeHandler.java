package org.kaazing.gateway.transport.http.security.auth.connector;

import org.kaazing.netx.http.auth.ChallengeHandler;
import org.kaazing.netx.http.auth.ChallengeRequest;
import org.kaazing.netx.http.auth.ChallengeResponse;

/**
 * Challenge handler for Basic authentication. See RFC 2617.
 */
public class TestChallengeHandler extends ChallengeHandler {

    @Override
    public boolean canHandle(ChallengeRequest challengeRequest) {
        // TODO Auto-generated method stub
        return true;
    }

    @Override
    public ChallengeResponse handle(ChallengeRequest challengeRequest) {
        char[] creds = {'C','R','E','D','S'};
        return new ChallengeResponse(creds, null);
    }


}

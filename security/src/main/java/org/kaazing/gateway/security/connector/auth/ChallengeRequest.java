package org.kaazing.gateway.security.connector.auth;

public interface ChallengeRequest {

    String getAuthenticationParameters();

    String getAuthenticationScheme();

    String getLocation();

    String toString();
}

package org.kaazing.gateway.transport.http.security.auth.connector;

/**
 * Copyright (c) 2007-2015, Kaazing Corporation. All rights reserved.
 */

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.kaazing.gateway.security.connector.auth.ChallengeRequest;

public final class RealmUtils {

    private RealmUtils() {
        // prevent object creation
    }

    private static final String REALM_REGEX = "(.*)\\s?(?i:realm=)(\"(.*)\")(.*)";
    private static final Pattern REALM_PATTERN = Pattern.compile(REALM_REGEX);

    /**
     * A realm parameter is a valid authentication parameter for all authentication schemes
     * according to <a href="http://tools.ietf.org/html/rfc2617#section-2.1">RFC 2617 Section 2.1"</a>.
     *
     * <quote>
     *     The realm directive (case-insensitive) is required for all
     *     authentication schemes that issue a challenge. The realm value
     *     (case-sensitive), in combination with the canonical root URL (the
     *     absoluteURI for the server whose abs_path is empty) of the server
     *     being accessed, defines the protection space.
     * </quote>
     *
     * @param challengeRequest the challenge request to extract a realm from
     *
     * @return the unquoted realm parameter value if present, or {@code null} if no such parameter exists.
     */
    public static String getRealm(ChallengeRequest challengeRequest) {
        String authenticationParameters = challengeRequest.getAuthenticationParameters();
        if (authenticationParameters == null) {
            return null;
        }
        Matcher m = REALM_PATTERN.matcher(authenticationParameters);
        if (m.matches() && m.groupCount() >= 3) {
            return m.group(3);
        }
        return null;
    }
}

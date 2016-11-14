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

package org.kaazing.gateway.transport.http.security.auth;

import static org.kaazing.gateway.transport.http.security.auth.WWWAuthenticateHeaderUtils.getChallenges;
import static org.kaazing.gateway.transport.http.security.auth.WWWAuthenticateHeaderUtils.splitWWWAuthenticateHeaderBySchemes;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;

public class WWWAuthenticateHeaderUtilsTest {

    private static final String SINGLE_TYPE_WWW_AUTH_HEADER = "Basic realm=\"factor1\"";
    private static final String SINGLE_TYPE_WWW_AUTH_HEADER_WITH_PARAMS =
            "Newauth realm=\"apps\", type=1, title=\"Login to \\\"apps\\\"";
    private static final String MULTIPLE_TYPE_WWW_AUTH_HEADER_WITH_PARAMS =
            "Newauth realm=\"apps\", type=1, title=\"Login to \\\"apps\\\"\", Basic realm=\"simple\"";

    @Test
    public void getChallengeFromSingleWWWAuthHeader() {
        // single value
        String[] result = splitWWWAuthenticateHeaderBySchemes(SINGLE_TYPE_WWW_AUTH_HEADER);
        Assert.assertEquals(1, result.length);
        Assert.assertEquals(SINGLE_TYPE_WWW_AUTH_HEADER, result[0]);

        WWWAuthChallenge challenge = new WWWAuthChallenge(result[0]);
        Assert.assertEquals("Basic", challenge.getScheme());
        Assert.assertEquals("factor1", challenge.getRealm());
        Assert.assertEquals(SINGLE_TYPE_WWW_AUTH_HEADER, challenge.getChallenge());
    }

    @Test
    public void getChallengeFromSingleWWWAuthHeaderWithParams() {
        String[] result = splitWWWAuthenticateHeaderBySchemes(SINGLE_TYPE_WWW_AUTH_HEADER_WITH_PARAMS);
        Assert.assertEquals(1, result.length);
        Assert.assertEquals(SINGLE_TYPE_WWW_AUTH_HEADER_WITH_PARAMS, result[0]);

        WWWAuthChallenge challenge = new WWWAuthChallenge(result[0]);
        Assert.assertEquals("Newauth", challenge.getScheme());
        Assert.assertEquals("apps", challenge.getRealm());
        Assert.assertEquals(SINGLE_TYPE_WWW_AUTH_HEADER_WITH_PARAMS, challenge.getChallenge());
    }

    @Test
    public void getChallengeFromMultipleWWWAuthHeader() {
        String[] result = splitWWWAuthenticateHeaderBySchemes(MULTIPLE_TYPE_WWW_AUTH_HEADER_WITH_PARAMS);
        Assert.assertEquals(2, result.length);
        final String expectedChallenge1 = "Newauth realm=\"apps\", type=1, title=\"Login to \\\"apps\\\"\"";
        final String expectedChallenge2 = "Basic realm=\"simple\"";
        Assert.assertEquals(expectedChallenge1, result[0]);
        Assert.assertEquals(expectedChallenge2, result[1]);

        WWWAuthChallenge challenge = new WWWAuthChallenge(result[0]);
        Assert.assertEquals("Newauth", challenge.getScheme());
        Assert.assertEquals("apps", challenge.getRealm());
        Assert.assertEquals(expectedChallenge1, challenge.getChallenge());
        challenge = new WWWAuthChallenge(result[1]);
        Assert.assertEquals("Basic", challenge.getScheme());
        Assert.assertEquals("simple", challenge.getRealm());
        Assert.assertEquals(expectedChallenge2, challenge.getChallenge());
    }

    @Test
    public void getChallengesFromMultipleWWWAuthHeader() {
        final String expectedChallenge1 = "Newauth realm=\"apps\", type=1, title=\"Login to \\\"apps\\\"\"";
        final String expectedChallenge2 = "Basic realm=\"simple\"";

        List<WWWAuthChallenge> result = getChallenges(MULTIPLE_TYPE_WWW_AUTH_HEADER_WITH_PARAMS);

        WWWAuthChallenge challenge = result.get(0);
        Assert.assertEquals("Newauth", challenge.getScheme());
        Assert.assertEquals("apps", challenge.getRealm());
        Assert.assertEquals(expectedChallenge1, challenge.getChallenge());
        challenge = result.get(1);
        Assert.assertEquals("Basic", challenge.getScheme());
        Assert.assertEquals("simple", challenge.getRealm());
        Assert.assertEquals(expectedChallenge2, challenge.getChallenge());
    }

}

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
package org.kaazing.gateway.security.auth;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.kaazing.gateway.server.spi.security.LoginResult;

public class DefaultLoginResultTest {
    private DefaultLoginResult loginResult;

    private void assertLoginResultChallenge() {
        Assert.assertEquals("Expected LoginResult object to model successful login.",
                LoginResult.Type.CHALLENGE, loginResult.getType());
    }

    private void assertLoginResultFailure() {
        Assert.assertEquals("Expected LoginResult object to model successful login.",
                LoginResult.Type.FAILURE, loginResult.getType());
    }

    private void assertLoginResultSuccess() {
        Assert.assertEquals("Expected LoginResult object to model successful login.",
                LoginResult.Type.SUCCESS, loginResult.getType());
    }


    @Before
    public void setup() {
        loginResult = new DefaultLoginResult();
    }

    @Test
    public void testGetLoginChallengeData() throws Exception {
        Object[] data = loginResult.getLoginChallengeData();
        Assert.assertTrue(String.format("Expected null login challenge data, got %s", data), data == null);

        // Test the no-argument case
        loginResult.challenge();
        assertLoginResultChallenge();
        data = loginResult.getLoginChallengeData();
        Assert.assertTrue(String.format("Expected null login challenge data, got %s", data), data == null);

        // Test the empty array argument case
        loginResult.challenge();
        assertLoginResultChallenge();
        data = loginResult.getLoginChallengeData();
        Assert.assertTrue(String.format("Expected null login challenge data, got %s", data), data == null);

        // Test the single argument case
        String param1 = "foo=\"bar\"";
        loginResult.challenge(param1);
        assertLoginResultChallenge();
        data = loginResult.getLoginChallengeData();
        Assert.assertTrue("Expected login challenge data, got null", data != null);
        Assert.assertTrue(String.format("Expected 1 login challenge datum, got %d", data.length), data.length == 1);

        // Test the mulitple argument case
        String param2 = "baz=\"quxx\"";
        String param3 = "quzz=\"alef\"";
        loginResult.challenge(param2, param3);
        assertLoginResultChallenge();

        data = loginResult.getLoginChallengeData();
        Assert.assertTrue("Expected login challenge data, got null", data != null);
        Assert.assertTrue(String.format("Expected 3 login challenge data, got %d", data.length), data.length == 3);

        int expectedDataCount = 0;
        for (Object datum : data) {
            if (param1.equals(datum)) {
                expectedDataCount++;
                continue;
            }

            if (param2.equals(datum)) {
                expectedDataCount++;
                continue;
            }

            if (param3.equals(datum)) {
                expectedDataCount++;
                continue;
            }
        }

        Assert.assertTrue("Found all expected challenge data", expectedDataCount == 3);
    }
}

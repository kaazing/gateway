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

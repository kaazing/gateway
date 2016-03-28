/**
 * Copyright 2007-2015, Kaazing Corporation. All rights reserved.
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

import org.kaazing.gateway.security.connector.auth.ChallengeHandler;
import org.kaazing.gateway.security.connector.auth.ChallengeRequest;
import org.kaazing.gateway.security.connector.auth.ChallengeResponse;

public class 
TestChallengeHandler extends ChallengeHandler {
        
        public TestChallengeHandler() {
            super();
        };
        @Override
        public ChallengeResponse handle(ChallengeRequest arg0) {
            System.out.println("handle");
            return null;
        }
        
        @Override
        public boolean canHandle(ChallengeRequest arg0) {
            System.out.println("canHandle");
            return true;
        }
        
//        @Override
//        public void setRealmLoginHandler(String arg0, LoginHandler arg1) {
//            System.out.println("setRealmLoginHandler");
//        }
//        
//        @Override
//        public BasicChallengeHandler setLoginHandler(LoginHandler arg0) {
//            System.out.println("setLoginHandler");
//            return null;
//        }
//        
//        @Override
//        public LoginHandler getLoginHandler() {
//            System.out.println("getLoginHandler");
//            return null;
//        }
}

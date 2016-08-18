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
package org.kaazing.gateway.service.turn.rest.internal;

import java.text.MessageFormat;

public final class TurnRestJSONResponse {

    private TurnRestJSONResponse() {
    }

    public static String createResponse(String username, char[] password, String ttl, String uris) {
        String response = "";
        if (username != null && password != null) {
            response = MessageFormat.format("\"username\":\"{0}\",\"password\":\"{1}\",", username, new String(password));
        }
        response = MessageFormat.format("'{'{0}\"ttl\":{1},\"uris\":[{2}]'}'", response, ttl, uris);
        return response;
    }
}

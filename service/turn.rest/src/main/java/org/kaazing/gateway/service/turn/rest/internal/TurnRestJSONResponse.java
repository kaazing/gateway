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

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class TurnRestJSONResponse {

    private static final Logger LOGGER = LoggerFactory.getLogger(TurnRestJSONResponse.class);

    private TurnRestJSONResponse() {
    }

    public static String createResponse(String username, char[] password, String ttl, String urls) {
        JSONObject jsonObj = new JSONObject();

        try {
            if (username != null && password != null) {
                jsonObj.put("username", username);
                jsonObj.put("credential", new String(password));
            }
            jsonObj.put("ttl", ttl);
            jsonObj.put("urls", urls.split(","));
        } catch (JSONException e) {
            LOGGER.warn("Exception while building the turn.rest JSON response", e);
        }

        return jsonObj.toString();
    }
}

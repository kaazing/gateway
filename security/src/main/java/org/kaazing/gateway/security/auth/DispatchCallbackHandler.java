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

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;

import org.kaazing.gateway.security.TypedCallbackHandlerMap;

/**
 * Manages a map from callback class to callback handlers.
 * This allows login modules higher in the chain to add
 * callback handlers as they see necessary, so that lower
 * login modules in the chain can be written without
 * regard to the sharedState/callback choice, and always
 * use callback.
 */
public class DispatchCallbackHandler implements CallbackHandler {

    private Map<Class<? extends Callback>, CallbackHandler> dispatchMap =
            new ConcurrentHashMap<>();

    public DispatchCallbackHandler register(Class<? extends Callback> callbackClass, CallbackHandler callbackHandler) {
        if (callbackClass == null) {
            throw new NullPointerException("callbackClass");
        }
        if (callbackHandler == null) {
            throw new NullPointerException("callbackHandler");
        }

        dispatchMap.put(callbackClass, callbackHandler);

        return this;
    }

    public DispatchCallbackHandler registerAll(TypedCallbackHandlerMap callbackHandlers) {

        if (callbackHandlers == null) {
            throw new NullPointerException("callbackHandlers");
        }

        dispatchMap.putAll(callbackHandlers.getMap());

        return this;
    }

    public CallbackHandler unregister(Class<? extends Callback> callbackClass) {
        if (callbackClass == null) {
            throw new NullPointerException("callbackClass");
        }
        dispatchMap.remove(callbackClass);
        return this;
    }


    @Override
    public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
        if (dispatchMap != null && dispatchMap.size() > 0) {

            if (callbacks != null && callbacks.length > 0) {
                for (Callback callback : callbacks) {
                    if (callback != null) {
                        CallbackHandler handler = dispatchMap.get(callback.getClass());
                        if (handler != null) {
                             handler.handle(new Callback[]{callback});
                        } else {
                            throw new UnsupportedCallbackException(callback, "Could not find a handler for callback class "
                                    + callback.getClass().getName());
                        }
                    } else {
                        throw new UnsupportedCallbackException(callback,
                                "A null callback was encountered in the callbacks provided.");

                    }
                }
            }
        } else {
            throw new UnsupportedCallbackException(null, "No callback handlers are available.");
        }
    }

    /**
     * <strong>for testing purposes only</strong>
     * @return the dispatch map
     */
    Map<Class<? extends Callback>, CallbackHandler> getDispatchMap() {
        return dispatchMap;
    }
}

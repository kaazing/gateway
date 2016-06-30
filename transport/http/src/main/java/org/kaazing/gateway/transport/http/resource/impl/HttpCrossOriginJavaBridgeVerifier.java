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
package org.kaazing.gateway.transport.http.resource.impl;

import org.apache.mina.core.buffer.IoBuffer;
import org.kaazing.gateway.transport.http.HttpAcceptSession;
import org.kaazing.gateway.transport.http.resource.HttpDynamicResource;

public final class HttpCrossOriginJavaBridgeVerifier extends HttpDynamicResource {

    @Override
    public void writeFile(HttpAcceptSession httpSession) {
        String sOriginId = httpSession.getParameter("soid");
        String authority = httpSession.getParameter("authority");
        
        String call = "(function() { var element = document.getElementById(\"" + sOriginId + "\");" 
              + "if (element) { "
              +"  element.Packages.org.kaazing.gateway.client.html5.impl.BridgeUtil.bridgeJsCall(\"" + authority +"\");}})();";
        IoBuffer buf = IoBuffer.wrap(call.getBytes());
        
        httpSession.write(buf);
    }
}

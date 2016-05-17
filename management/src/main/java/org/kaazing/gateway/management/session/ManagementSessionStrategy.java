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
package org.kaazing.gateway.management.session;

import org.apache.mina.core.write.WriteRequest;
import org.kaazing.gateway.management.ManagementStrategy;


/**
 * Do full management processing for both service and session. This is only done on non-management session requests.
 * <p/>
 * Sessions are either handled fully (collect and send notifications) or not at all (no collect, no notifications). That's really
 * handled at the managementFilter level most easily, so we really only need one ManagementSessionStrategy. But we'll set things
 * up so we can create new strategies later if necessary.
 * <p/>
 * ALL REQUESTS WILL BE ON ONE OR ANOTHER OF THE IO THREADS, SO MUST NOT BLOCK!
 */
public interface ManagementSessionStrategy extends ManagementStrategy {

    // session-level lifecycle methods coming from the management filter.
    // THESE ARE ALL CALLED ON THE IO THREAD. WE MUST NOT BLOCK.

    void doSessionCreated(final SessionManagementBean sessionBean) throws Exception;

    void doSessionClosed(final SessionManagementBean sessionBean) throws Exception;

    void doMessageReceived(final SessionManagementBean sessionBean, final Object message) throws Exception;

    void doFilterWrite(final SessionManagementBean sessionBean, final WriteRequest writeRequest) throws Exception;

    void doExceptionCaught(final SessionManagementBean sessionBean, final Throwable cause) throws Exception;
}


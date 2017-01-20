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

/**
 * Do full management processing for both service and session. This is only done on non-management session requests.
 * <p/>
 * Sessions are either handled fully (collect and send notifications) or not at all (no collect, no notifications). That's really
 * handled at the managementFilter level most easily, so we really only need one ManagementSessionStrategy.
 * <p/>
 * ALL REQUESTS WILL BE ON ONE OR ANOTHER OF THE IO THREADS, SO MUST NOT BLOCK!
 */
public class FullManagementSessionStrategy extends CollectOnlyManagementSessionStrategy {

    public FullManagementSessionStrategy() {
    }

    // session-level lifecycle methods coming from the management filter.
    // THESE ARE ALL CALLED ON THE IO THREAD. WE MUST NOT BLOCK.

    @Override
    public void doSessionCreated(final SessionManagementBean sessionBean) throws Exception {
        super.doSessionCreated(sessionBean);
        sessionBean.doSessionCreatedListeners();
    }

    @Override
    public void doMessageReceived(final SessionManagementBean sessionBean, final Object message) throws Exception {
        super.doMessageReceived(sessionBean, message);
        sessionBean.doMessageReceivedListeners(message);
    }

    @Override
    public void doFilterWrite(final SessionManagementBean sessionBean, final WriteRequest writeRequest) throws Exception {
        super.doMessageReceived(sessionBean, writeRequest);
        sessionBean.doFilterWriteListeners(writeRequest);
    }

    @Override
    public void doExceptionCaught(final SessionManagementBean sessionBean, final Throwable cause) throws Exception {
        super.doExceptionCaught(sessionBean, cause);
        sessionBean.doExceptionCaughtListeners(cause);
    }

    public String toString() {
        return "FULL_SESSION_STRATEGY";
    }
}


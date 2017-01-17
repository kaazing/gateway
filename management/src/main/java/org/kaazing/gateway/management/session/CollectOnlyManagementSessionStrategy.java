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
 * Do collect-only management processing for session. This is only done on non-management session requests.
 * <p/>
 * ALL REQUESTS WILL BE ON ONE OR ANOTHER OF THE IO THREADS, SO MUST NOT BLOCK!
 */
public class CollectOnlyManagementSessionStrategy implements ManagementSessionStrategy {

    // session-level lifecycle methods coming from the management filter.

    @Override
    public void doSessionCreated(final SessionManagementBean sessionBean) throws Exception {
        sessionBean.doSessionCreated();
    }

    @Override
    public void doSessionClosed(final SessionManagementBean sessionBean) throws Exception {
        if (sessionBean != null) {
            sessionBean.doSessionClosed();
            sessionBean.doSessionClosedListeners();
        }
    }

    @Override
    public void doMessageReceived(final SessionManagementBean sessionBean, final Object message) throws Exception {
        sessionBean.doMessageReceived(message);
    }

    @Override
    public void doFilterWrite(final SessionManagementBean sessionBean, final WriteRequest writeRequest) throws Exception {
        sessionBean.doFilterWrite(writeRequest);
    }

    @Override
    public void doExceptionCaught(final SessionManagementBean sessionBean, final Throwable cause) throws Exception {
        sessionBean.doExceptionCaught(cause);
    }

    public String toString() {
        return "COLLECT_ONLY_SESSION_STRATEGY";
    }
}


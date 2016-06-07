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


/**
 * Interface to be implemented by those objects that want to act as listeners for session-level management events on particular
 * SessionManagementBean instances. Presumably each implementer of this interface would be protocol-specific.
 * <p/>
 * NOTE: as of 4.0, the SessionManagementListener's only function is to send out notifications (if enabled) of events. Other
 * parts of the process are responsible for creating the session management beans. During collect-only processing (see the
 * ManagementFilter object) session beans (and thus these listeners) are not created at all.
 */
public interface SessionManagementListener {

    // Session-level event handlers, ultimately called from session strategy object.
    // Pass the beans as arguments so we can use stateless listeners where possible.

    void doSessionCreated(final SessionManagementBean sessionBean) throws Exception;

    void doSessionClosed(final SessionManagementBean sessionBean) throws Exception;

    void doMessageReceived(final SessionManagementBean sessionBean, final Object message) throws Exception;

    void doFilterWrite(final SessionManagementBean sessionBean, final Object message, final Object originalMessage) throws
            Exception;

    void doExceptionCaught(final SessionManagementBean sessionBean, final Throwable cause);
}

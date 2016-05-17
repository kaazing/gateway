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
package org.kaazing.mina.core.session;

import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSessionConfig;

/**
 * Extended version of IoSessionConfig to add support for millisecond precision for idle timeouts.
 */
public interface IoSessionConfigEx extends IoSessionConfig {

    /**
     * New method added for millisecond precise idle times
     */
    void setIdleTimeInMillis(IdleStatus status, long idleTimeMillis);

    void setChangeListener(ChangeListener listener);

    interface ChangeListener {

        void idleTimeInMillisChanged(IdleStatus status, long idleTimeMillis);

    }
}

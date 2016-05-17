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
package org.kaazing.gateway.transport.test;

import org.apache.mina.core.session.IoSession;
import org.jmock.api.Invocation;
import org.jmock.lib.action.CustomAction;

public final class SetIoSessionClosed extends CustomAction {
    private final IoSession session;

    public SetIoSessionClosed(IoSession session) {
        super("Complete IoSession CloseFuture");
        this.session = session;
    }

    @Override
    public Object invoke(Invocation invocation)
            throws Throwable {
        session.getCloseFuture().setClosed();
        return null;
    }
}

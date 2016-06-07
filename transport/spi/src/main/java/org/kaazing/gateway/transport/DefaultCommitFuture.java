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
package org.kaazing.gateway.transport;

import org.apache.mina.core.future.DefaultIoFuture;
import org.apache.mina.core.future.IoFutureListener;
import org.apache.mina.core.session.IoSession;

public class DefaultCommitFuture extends DefaultIoFuture implements CommitFuture {

    public DefaultCommitFuture(IoSession session) {
        super(session);
    }

    @Override
    public boolean isCommitted() {
        if (isDone()) {
            return (Boolean) getValue();
        } else {
            return false;
        }
    }

    @Override
    public void setCommited() {
        setValue(Boolean.TRUE);
    }

    @Override
    public CommitFuture await() throws InterruptedException {
        return (CommitFuture) super.await();
    }

    @Override
    public CommitFuture awaitUninterruptibly() {
        return (CommitFuture) super.awaitUninterruptibly();
    }

    @Override
    public CommitFuture addListener(IoFutureListener<?> listener) {
        return (CommitFuture) super.addListener(listener);
    }

    @Override
    public CommitFuture removeListener(IoFutureListener<?> listener) {
        return (CommitFuture) super.removeListener(listener);
    }

}

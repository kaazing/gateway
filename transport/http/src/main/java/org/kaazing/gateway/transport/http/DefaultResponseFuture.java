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
package org.kaazing.gateway.transport.http;

import org.apache.mina.core.future.DefaultIoFuture;
import org.apache.mina.core.future.IoFutureListener;
import org.apache.mina.core.session.IoSession;

public class DefaultResponseFuture extends DefaultIoFuture implements ResponseFuture {

    public DefaultResponseFuture(IoSession session) {
        super(session);
    }

    @Override
    public boolean isReady() {
        return isDone();
    }

    @Override
    public void setReady() {
        setValue(Boolean.TRUE);
    }

    @Override
    public ResponseFuture await() throws InterruptedException {
        return (ResponseFuture) super.await();
    }

    @Override
    public ResponseFuture awaitUninterruptibly() {
        return (ResponseFuture) super.awaitUninterruptibly();
    }

    @Override
    public ResponseFuture addListener(IoFutureListener<?> listener) {
        return (ResponseFuture) super.addListener(listener);
    }

    @Override
    public ResponseFuture removeListener(IoFutureListener<?> listener) {
        return (ResponseFuture) super.removeListener(listener);
    }

}

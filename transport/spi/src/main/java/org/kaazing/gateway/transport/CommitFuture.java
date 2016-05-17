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

import org.apache.mina.core.future.IoFuture;
import org.apache.mina.core.future.IoFutureListener;

public interface CommitFuture extends IoFuture {

    boolean isCommitted();

    void setCommited();

    @Override
    CommitFuture await() throws InterruptedException;

    @Override
    CommitFuture awaitUninterruptibly();

    @Override
    CommitFuture addListener(IoFutureListener<?> listener);

    @Override
    CommitFuture removeListener(IoFutureListener<?> listener);

}
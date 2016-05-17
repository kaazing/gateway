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

import org.apache.mina.core.future.IoFuture;
import org.apache.mina.core.future.IoFutureListener;

public interface ResponseFuture extends IoFuture {

    boolean isReady();

    void setReady();

    @Override
    ResponseFuture await() throws InterruptedException;

    @Override
    ResponseFuture awaitUninterruptibly();

    @Override
    ResponseFuture addListener(IoFutureListener<?> listener);

    @Override
    ResponseFuture removeListener(IoFutureListener<?> listener);

}

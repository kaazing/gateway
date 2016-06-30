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
package org.kaazing.mina.core.write;

import java.net.SocketAddress;

import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.write.WriteRequest;

import org.kaazing.mina.core.future.WriteFutureEx;

/**
 * Extended version of WriteRequest to add support for mutating the
 * message during encoding to avoid undesirable allocation.
 */
public interface WriteRequestEx extends WriteRequest {

    void setMessage(Object message);

    @Override
    WriteFutureEx getFuture();

    boolean isResetable();
    void reset(IoSession session, Object message);
    void reset(IoSession session, Object message, SocketAddress destination);
}

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
package org.kaazing.gateway.transport.ws.util;

import java.util.Iterator;

import org.apache.mina.core.session.IoSession;
import org.kaazing.gateway.transport.BridgeSession;

public class BridgeSessionIterator implements Iterator<IoSession> {

    private IoSession next;

    public BridgeSessionIterator(IoSession next) {
        this.next = next;
    }

    @Override
    public boolean hasNext() {
        return next != null;
    }

    @Override
    public IoSession next() {
        IoSession result = next;
        if ( next == null ) {
            result = null;
        } else if (next instanceof BridgeSession) {
            next = ((BridgeSession) next).getParent();
        } else {
            next = null;
        }
        return result;
    }



    @Override
    public void remove() {
        throw new UnsupportedOperationException("cannot remove sessions using "+BridgeSessionIterator.class);
    }

}

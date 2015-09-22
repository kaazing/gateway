/**
 * Copyright (c) 2007-2014 Kaazing Corporation. All rights reserved.
 * 
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.kaazing.gateway.transport;

import org.apache.mina.core.future.DefaultIoFuture;
import org.apache.mina.core.future.IoFutureListener;
import org.apache.mina.core.session.IoSession;

public class DefaultUpgradeFuture extends DefaultIoFuture implements UpgradeFuture {

    public DefaultUpgradeFuture(IoSession session) {
        super(session);
    }

    @Override
    public boolean isUpgraded() {
        if (isDone()) {
            return (Boolean) getValue();
        } else {
            return false;
        }
    }

    @Override
    public void setUpgraded() {
        setValue(Boolean.TRUE);
    }

    @Override
    public UpgradeFuture await() throws InterruptedException {
        return (UpgradeFuture) super.await();
    }

    @Override
    public UpgradeFuture awaitUninterruptibly() {
        return (UpgradeFuture) super.awaitUninterruptibly();
    }

    @Override
    public UpgradeFuture addListener(IoFutureListener<?> listener) {
        return (UpgradeFuture) super.addListener(listener);
    }

    @Override
    public UpgradeFuture removeListener(IoFutureListener<?> listener) {
        return (UpgradeFuture) super.removeListener(listener);
    }

}

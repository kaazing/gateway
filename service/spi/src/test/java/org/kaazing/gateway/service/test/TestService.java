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
package org.kaazing.gateway.service.test;

import org.kaazing.gateway.service.Service;
import org.kaazing.gateway.service.ServiceContext;

public class TestService implements Service {

    @Override
    public String getType() {
        return "test";
    }

    @Override
    public void init(ServiceContext serviceContext) throws Exception {
        // needs to initialization
    }

    @Override
    public void start() throws Exception {
    }

    @Override
    public void stop() throws Exception {
    }

    @Override
    public void quiesce() throws Exception {
    }

    @Override
    public void destroy() throws Exception {
    }
}

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
package org.kaazing.gateway.service.update.check;

import static java.util.Collections.singleton;

import java.util.Collection;

import org.kaazing.gateway.service.Service;
import org.kaazing.gateway.service.ServiceFactorySpi;

public final class UpdateCheckServiceFactorySpi extends ServiceFactorySpi {
    
    @Override
    public Collection<String> getServiceTypes() {
        return singleton("update.check");
    }

    @Override
    public Service newService(String serviceType) {
        assert "update.check".equals(serviceType);
        return new UpdateCheckService();
    }
}

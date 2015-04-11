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

package org.kaazing.gateway.management.snmp;

import java.util.Collection;
import java.util.Collections;
import org.kaazing.gateway.service.Service;
import org.kaazing.gateway.service.ServiceFactorySpi;

/**
 * The SPI for SNMP management services.
 * <p/>
 * Kaazing's SNMP support is based on the SNMP4J open-source library under the Apache 2.0 license. To see the full text of the
 * license, please see the Kaazing third-party licenses file.
 */
public class SnmpManagementServiceFactorySpi extends ServiceFactorySpi {

    @Override
    public Collection<String> getServiceTypes() {
        return Collections.singletonList("management.snmp");
    }

    @Override
    public Service newService(String serviceType) {
        assert "management.snmp".equals(serviceType);
        return new SnmpManagementService();
    }

}

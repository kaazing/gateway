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

package org.kaazing.gateway.management.jmx;

import javax.management.ObjectName;

public interface GatewayMXBean {

    public ObjectName getObjectName();

    public int getIndex();

    public String getId();

    public String getProductTitle();
    public String getProductBuild();
    public String getProductEdition();
    public long getTotalCurrentSessions();
    public long getTotalBytesReceived();
    public long getTotalBytesSent();
    public long getUptime();
    public long getStartTime();
    public String getInstanceKey();
    public String getClusterMembers();
    public String getClusterBalancerMap();
    public String getManagementServiceMap();
    public String getAvailableUpdateVersion();
    public void forceUpdateVersionCheck();
}

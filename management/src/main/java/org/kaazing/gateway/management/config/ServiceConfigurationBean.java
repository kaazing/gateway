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
package org.kaazing.gateway.management.config;

import org.kaazing.gateway.management.gateway.GatewayManagementBean;

public interface ServiceConfigurationBean {
    GatewayManagementBean getGatewayManagementBean();

    int getId();

    String getType();

    String getServiceName();

    String getServiceDescription();

    String getAccepts();

    String getAcceptOptions();

    String getBalances();

    /**
     * Return the current connects as a string. If connects are legal in the service but none has as yet been defined, return "".
     * If the service does not even support having connects (e.g. Stomp JMS allows 0 connects), return null.
     */
    String getConnects();

    /**
     * Return the current connect options as a string. If connects are legal in the service but none has as yet been defined,
     * return "". If the service does not even support having connects (e.g. Stomp JMS allows 0 connects), return null.
     */
    String getConnectOptions();

    String getCrossSiteConstraints();

    String getProperties();

    String getRequiredRoles();

    String getServiceRealm();

    /**
     * Return the current MIME mappings as a string. If MIME mappings are legal in the service but none has as yet been defined
     * (unlikely, as the default MIME mappings are included, if there are any), return "". If the service does not even support
     * having MIME mappings (i.e., is anything but a directory service), return null.
     *
     * @return
     */
    String getMimeMappings();
}

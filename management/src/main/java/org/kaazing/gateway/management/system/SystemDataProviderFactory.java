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
package org.kaazing.gateway.management.system;

import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.Uptime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Create a SystemDataProvider instance. If SIGAR is functioning correctly, that's preferred because it actually supports all the
 * SystemDataProvider values. If not, the alternative only provides 'fake' data and zeroes.
 *
 */
public class SystemDataProviderFactory {

    protected SystemDataProviderFactory() { }

    public static SystemDataProvider createProvider() {
        try {
            Sigar sigar = new Sigar();
            Uptime uptime = sigar.getUptime();

            return new SigarSystemDataProvider();
        } catch (Throwable t) {
            Logger logger = LoggerFactory.getLogger(SystemDataProviderFactory.class);
            logger.info("Management services are unable to access system-level management statistics");
            logger.info("   (CPU, NIC, System data). Management will continue without them.");

            return new NonSigarSystemDataProvider();
        }
    }
}

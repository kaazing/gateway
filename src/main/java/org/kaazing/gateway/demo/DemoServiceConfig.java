/**
 * Copyright (c) 2007-2014, Kaazing Corporation. All rights reserved.
 */

package org.kaazing.gateway.demo;

import java.util.Properties;

public interface DemoServiceConfig {
    /**
     * Config the demo services with the correct environment
     * @return the properties that make up the environment for the demo services
     */
    Properties configure();
}

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

package org.kaazing.gateway.server;

import org.apache.commons.cli.HelpFormatter;

/**
 * Main entry point for gateway process (at least when being called by the
 * start scripts--it's possible to call Gateway directly from other
 * Java code as well.) This particular version is for the standalone Linux
 * installation, where the directories are set up to be
 * directly under GATEWAY_HOME and we use Posix-style command parsing.
 * 
 * As of 3.2 we support two arguments to main:
 *    --config <configFile>
 *    --help
 *    (we also support the form /config <configFile>)
 *    The value of <configFile> is considered as a path to the gateway
 *    configuration file--NOT just a file name.
 * If provided by the caller, the property is converted to a system property
 * and passed to Gateway, rather than requiring Gateway to 
 * handle a new input vector.
 */
public class Main {
    
    /**
     * @param args
     */
    public static void main(String... args) throws Exception {
        GatewayCommandProcessor commandProcessor = 
            new GatewayCommandProcessor(new HelpFormatter());
        commandProcessor.launchGateway(args);
    }
}

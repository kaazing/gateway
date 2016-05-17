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
package org.kaazing.gateway.server;

import org.apache.commons.cli.HelpFormatter;

/**
 * Main entry point for gateway process (at least when being called by the start scripts--it's possible to call Gateway
 * directly from other Java code as well.) This particular version is for the standalone Linux installation, where the
 * directories are set up to be directly under GATEWAY_HOME and we use Posix-style command parsing.
 * <p/>
 * For supported command line arguments @see {@link GatewayCommandLineProcessor}
 */
public class Main {

    protected Main() {
    }

    /**
     * @param args
     */
    public static void main(String... args) throws Exception {
        GatewayCommandLineProcessor gatewayCommandLineProcessor = new GatewayCommandLineProcessor(new HelpFormatter());
        gatewayCommandLineProcessor.launchGateway(args);
    }
}

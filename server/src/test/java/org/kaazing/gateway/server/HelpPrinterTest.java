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
import org.apache.commons.cli.Options;
import org.junit.Before;
import org.junit.Test;

public class HelpPrinterTest {

    HelpPrinter help = null;
    Options options = null;

    @Before
    public void SetUp() {
        help = HelpPrinter.getInstance();
        options = createOptions();
    }

    @Test
    public void testExceptionHelpPrinter() {

        Exception ex = new Exception ("Test exception");
        help.printCliHelp("There was a problem with a command-line argument:\n" + ex.getMessage(), options);
    }
    
    
    private Options createOptions() {
        Options options = new Options();
        options.addOption(null, "config", true, "path to gateway configuration file");
        options.addOption(null, "help", false, "print the help text");
        return options;
    }
}

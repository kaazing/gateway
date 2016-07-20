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

import java.util.ServiceLoader;

import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;

public class HelpPrinter {

    protected HelpFormatter helpFormatter;
    private static HelpPrinter instance = null;

    
    protected HelpPrinter() {
    }

    // Singleton method
    public static HelpPrinter getInstance() {
        if (instance != null)  { 
            return instance;
        }
        
        // try to load some special help class
        ServiceLoader<HelpPrinter> serviceLoader =
                ServiceLoader.load(HelpPrinter.class);
        //checking if load was successful
        for (HelpPrinter provider : serviceLoader) {
            HelpPrinter.instance = provider;
            return HelpPrinter.instance;
        } 
        
        //if no help class, return the default help
        HelpPrinter.instance = new HelpPrinter();
        return HelpPrinter.instance;
    }

    public void printCliHelp(String message, Options options) {
        if (message != null) {
            System.out.println(message);
        }

        helpFormatter.printHelp("gateway.start", options, true);
        return;
    }

    public void setFormatter(HelpFormatter formatter) {
        this.helpFormatter=formatter;
    }


}

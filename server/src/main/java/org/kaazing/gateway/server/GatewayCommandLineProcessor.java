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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Properties;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.Parser;
import org.apache.commons.cli.PosixParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import au.com.bytecode.opencsv.CSVReader;

/**
 * Main entry point for gateway process when started by the command line.
 * <p/>
 * Supported command line arguments are --config <configFile> and --help. The value of <configFile> is considered as a
 * path to the gateway configuration file--NOT just a file name. If provided by the caller, the property is converted to
 * a system property and passed to Gateway, rather than requiring Gateway to handle a new input vector.
 */
public class GatewayCommandLineProcessor {
    private static final String CONFIG_ARG = "config";

    private static final String HELP_ARG = "help";

    private static final Logger LOGGER = LoggerFactory.getLogger(GatewayCommandLineProcessor.class);

    private HelpFormatter helpFormatter;

    protected GatewayCommandLineProcessor(HelpFormatter helpFormatter) {
        this.helpFormatter = helpFormatter;
    }

    public void launchGateway(String[] args) {
        launchGateway(args, System.getProperties());
    }

    private void launchGateway(String[] args, Properties properties) {
        CommandLine cmd = null;
        Options options = createOptions();

        try {
            Parser parser = new PosixParser();
            cmd = parser.parse(options, args);
        } catch (ParseException ex) {
            printCliHelp("There was a problem with a command-line argument:\n" + ex.getMessage(), options, cmd);
            return;
        }

        String[] nonProcessedArgs = cmd.getArgs();
        if (nonProcessedArgs != null && nonProcessedArgs.length > 0) {
            System.out.println("There was a problem with the command-line arguments.");
            System.out.println("One or more unknown arguments were not processed:");
            for (String nonProcessedArg : nonProcessedArgs) {
                System.out.println("   " + nonProcessedArg);
            }
            printCliHelp(null, options, cmd);
            return;
        }

        if (cmd.hasOption(HELP_ARG)) {
            printCliHelp(null, options, cmd);
            return;
        }

        // get the various options
        String config = cmd.getOptionValue(CONFIG_ARG);
        if (config != null) {
            properties.setProperty(Gateway.GATEWAY_CONFIG_PROPERTY, config);
        }

        // Because Gateway already has checking for defaults (and they default
        // to directories under $GATEWAY_HOME), we don't actually have to do anything
        // here (we do in the InstalledLinux case.)
        final Gateway gateway = GatewayFactory.createGateway();
        gateway.setProperties(properties);

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                try {
                    gateway.destroy();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        try {
            gateway.launch();
        } catch (Exception ex) {
            // Log the exception then exit. It's possible log4j won't be initialized by the time
            // the exception occurred, so log and print stacktrace (to System.err)
            LOGGER.error("Gateway failed to launch", ex);
            ex.printStackTrace();
            System.exit(-1);
        }
    }

    private void printCliHelp(String message, Options options, CommandLine cmd) {
        if (message != null) {
            System.out.println(message);
        }

        Options all_opt = new Options();

        // add the options that are parsed by the java binary, except for script-arg
        // since this argument is internal, we shouldn't be using it
        for (Object o : options.getOptions()) {
            Option o1 = (Option) o;
            all_opt.addOption(o1);
        }

        // also, get the file that has documentation for the values parsed in the launching script
        File helpScript = getScriptedOptsFile();
        appendScriptedOptions(helpScript, all_opt);
        helpFormatter.printHelp("gateway.start", all_opt, true);
    }

    private File getScriptedOptsFile() {
        File helpScript = null;
        String helpScriptPath = System.getenv("SCRIPTED_ARGS");

        if (helpScriptPath != null) {
            helpScript = new File(helpScriptPath);
        }
        return helpScript;
    }

    private void appendScriptedOptions(File helpScript, Options all_opt) {
        try {
            if (helpScript != null && helpScript.exists()) {
                InputStreamReader input_opt;
                input_opt = new InputStreamReader(new FileInputStream(helpScript));

                CSVReader script_options = new CSVReader(input_opt);
                // add options that are parsed by script
                String[] nextLine;

                while ((nextLine = script_options.readNext()) != null) {
                    if (nextLine.length >= 3 ) {
                        Option o = new Option(null, nextLine[0], Boolean.parseBoolean(nextLine[1]), nextLine[2]);
                        all_opt.addOption(o);
                    }
                }
                script_options.close();
            }

        } catch (IOException e) {
            // if this try catch block fails, don't do anything
            // it will only mean that we cannot show the "scripted args"
            LOGGER.debug("Exception when trying  to get scripted arguments", e);
        }
    }

    private Options createOptions() {
        Options options = new Options();
        options.addOption(null, CONFIG_ARG, true, "path to gateway configuration file");
        options.addOption(null, HELP_ARG, false, "print the help text");
        return options;
    }
}


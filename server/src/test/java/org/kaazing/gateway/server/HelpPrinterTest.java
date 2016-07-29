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

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class HelpPrinterTest {

    HelpPrinter help = null;
    Options options = null;
    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();

    @Before
    public void SetUp() {
        help = HelpPrinter.getInstance();
        options = createOptions();
        System.setOut(new PrintStream(outContent));
    }

    @After
    public void cleanUpStreams() {
        System.setOut(null);
    }

    String[] expected_help = new String[]{"usage: gateway.start [--config <arg>] [--help]",
            "--config <arg>   path to gateway configuration file", "--help           print the help text"};

    @Test
    public void testExceptionHelpPrinter() {
        Exception ex = new Exception("Test exception");
        help.setFormatter(new HelpFormatter());
        help.printCliHelp("There was a problem with a command-line argument:\n" + ex.getMessage(), options);
        String output = outContent.toString();
        String[] output_lines = output.split("\n");

        assertEquals(expected_help.length, output_lines.length - 2); // we subtract 2 because of the extra-lines in the
                                                                     // exceptio

        assertEquals(output_lines[0].trim(), "There was a problem with a command-line argument:");
        assertEquals(output_lines[1].trim(), "Test exception");

        for (int i = 0; i < expected_help.length; i++) {
            assertEquals(expected_help[i], output_lines[i + 2].trim());
        }
    }

    private Options createOptions() {
        Options options = new Options();
        options.addOption(null, "config", true, "path to gateway configuration file");
        options.addOption(null, "help", false, "print the help text");
        return options;
    }
}

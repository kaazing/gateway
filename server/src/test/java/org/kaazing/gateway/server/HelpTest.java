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
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class HelpTest {
    String[] expected_help = new String[]{"usage: gateway.start [--config <arg>] [--help]",
            "/config <arg>   path to gateway configuration file", "/help           print the help text"};
    String[] expected_help2 = new String[]{"usage: gateway.start [--broker] [--config <arg>] [--help]",
            "/broker         the broker to be started with the Gateway. e.g.:",
                "\"--broker jms\" for ActiveMQ or \"--broker amqp\" for",
                "AMQP",
            "/config <arg>   path to gateway configuration file", "/help           print the help text"};
    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();

    final PrintStream originalPS = System.out;
    @Before
    public void SetUp() {
        System.setOut(new PrintStream(outContent));
    }
    @After
    public void cleanUpStreams() {
        System.setOut(originalPS);
    }

    @Test
    public void testHelpMessageWithoutScriptedArgs() throws Exception {
        WindowsMain.main("--help");
        String output = outContent.toString();
        String[] output_lines = output.split("\n");

        assertEquals(expected_help.length, output_lines.length);
        for (int i = 0; i < expected_help.length; i++) {
            assertEquals(expected_help[i], output_lines[i].trim());
        }
    }
    
    @Test
    public void testHelpMessageWithScriptedArgs() throws Exception {
        String pathToArgsFile = "./enterprise-args.test";
        writeArgsFile(pathToArgsFile);
        modifyEnv(pathToArgsFile);
        WindowsMain.main("--help");
        String output = outContent.toString();
        String[] output_lines = output.split("\n");

        assertEquals(expected_help2.length, output_lines.length);
        for (int i = 0; i < expected_help2.length; i++) {
            assertEquals(expected_help2[i], output_lines[i].trim());
        }
        
        deleteArgsFile(pathToArgsFile);
    }
    private void deleteArgsFile(String pathToArgsFile) {
        File fileToDelete = new File(pathToArgsFile);
        fileToDelete.delete();
    }
    
    private void writeArgsFile(String pathToArgsFile) throws IOException {
        FileWriter fw = new FileWriter(new File(pathToArgsFile));
        fw.write("\"broker\", true, \"the broker to be started with the Gateway. e.g.: \"--broker jms\" for ActiveMQ or \"--broker amqp\" for AMQP\"");
        // reproduce the issue: https://github.com/kaazing/tickets/issues/926
        fw.write("\n");
        fw.write("\n");
        fw.flush();
        fw.close();
    }
    private void modifyEnv(String pathToArgsFile) {
        Map<String, String> env = new HashMap<String, String>();
        env.putAll(System.getenv());
        env.put("SCRIPTED_ARGS", pathToArgsFile);
        setEnv(env);
        assertEquals(System.getenv("SCRIPTED_ARGS"), pathToArgsFile);
    }
    
    protected static void setEnv(Map<String, String> newenv)
    {
      try
        {
            Class<?> processEnvironmentClass = Class.forName("java.lang.ProcessEnvironment");
            Field theEnvironmentField = processEnvironmentClass.getDeclaredField("theEnvironment");
            theEnvironmentField.setAccessible(true);
            Map<String, String> env = (Map<String, String>) theEnvironmentField.get(null);
            env.putAll(newenv);
            Field theCaseInsensitiveEnvironmentField = processEnvironmentClass.getDeclaredField("theCaseInsensitiveEnvironment");
            theCaseInsensitiveEnvironmentField.setAccessible(true);
            Map<String, String> cienv = (Map<String, String>)     theCaseInsensitiveEnvironmentField.get(null);
            cienv.putAll(newenv);
        }
        catch (NoSuchFieldException e)
        {
          try {
            Class[] classes = Collections.class.getDeclaredClasses();
            Map<String, String> env = System.getenv();
            for(Class cl : classes) {
                if("java.util.Collections$UnmodifiableMap".equals(cl.getName())) {
                    Field field = cl.getDeclaredField("m");
                    field.setAccessible(true);
                    Object obj = field.get(env);
                    Map<String, String> map = (Map<String, String>) obj;
                    map.clear();
                    map.putAll(newenv);
                }
            }
          } catch (Exception e2) {
            e2.printStackTrace();
          }
        } catch (Exception e1) {
            e1.printStackTrace();
        } 
    }
}

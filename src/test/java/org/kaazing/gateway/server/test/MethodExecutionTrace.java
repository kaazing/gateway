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

package org.kaazing.gateway.server.test;


import org.apache.log4j.PropertyConfigurator;
import org.junit.internal.AssumptionViolatedException;
import org.junit.rules.TestWatchman;
import org.junit.runners.model.FrameworkMethod;

/**
 * This class can be used to print out a message at the start and end of each test method in a JUnit test class
 * by including the following at the start of the class:
    @Rule
    public MethodRule testExecutionTrace = new MethodExecutionTrace();
 * 
 * TODO: this is a copy of the version from itests.util with the static initialization block calling Log4jTestUtil.initialize()
 * removed, because it interferes with tests like GatewayContextResolverTest that need to manipulate Loggers used by the Gateway.
 * We should avoid the duplication by fixing this issue in itests.util and consuming itests.util. 
 *
 */
public class MethodExecutionTrace extends TestWatchman {
    //if we ever want the full test name (including method),
    //we can call the public static method which retrieves this
    //variable. This variable is set in the starting method, and
    //and unset in the fail/success methods
    private static String currentFullTestName = null;
    
    public static String getFullTestName() {
        return currentFullTestName;
    }
    
    /**
     * Use this constructor from integration tests that do not actually start up the gateway but which
     * need to set up a particular log4j configuration using a properties file, e.g. to use MemoryAppender.
     * See gateway.server src/test/resources/log4j-trace.properties for an example.
     * @param log4jConfigPropertiesFileName
     */
    public MethodExecutionTrace(String log4jConfigPropertiesFileName) {
        MemoryAppender.initialize();
        if (log4jConfigPropertiesFileName != null) {
            PropertyConfigurator.configure(log4jConfigPropertiesFileName);
        }
    }
    
    public MethodExecutionTrace() {
        this(null);
    }
    
    @Override
    public void starting(FrameworkMethod method) {
        currentFullTestName = getFullMethodName(method);
        System.out.println(currentFullTestName + " starting");
    }

    @Override
    public void failed(Throwable e, FrameworkMethod method) {
        if (e instanceof AssumptionViolatedException) {
            System.out.println(String.format("%s skipped programmatically with reason: %s", getFullMethodName(method) , e.getMessage()));            
        }
        else {
            System.out.println(getFullMethodName(method) + " FAILED with exception " + e);
            e.printStackTrace();
            System.out.println("=================== BEGIN STORED LOG MESSAGES ===========================");
            MemoryAppender.printAllMessages();
            System.out.println("=================== END STORED LOG MESSAGES ===========================");
        }
    }

    @Override
    public void succeeded(FrameworkMethod method) {
            System.out.println(getFullMethodName(method) + " " + "success");
    }

    private String getFullMethodName(FrameworkMethod method) {
        return method.getMethod().getDeclaringClass().getSimpleName() + "." + method.getName();
    }
    
}


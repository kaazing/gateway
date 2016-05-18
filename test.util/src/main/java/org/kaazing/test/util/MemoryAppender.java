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
package org.kaazing.test.util;

import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.spi.LoggingEvent;

/**
 * This class is a subclass of log4j ConsoleAppender. It stores all logged messages in memory until (and if) static method
 * printAllMessages is called. The in-memory list of log messages is reset by calling the static initialize method.
 * This class automatically sets the level of the root logger to Level.TRACE, the first time a messages is logged using it.
 * USAGE: configure this class as the ONLY appender on the root Logger (on the <root> element log4j-config.xml) in order
 * to store trace level log messages in memory during a test run without seriously impacting performance or thread concurrency.
 * If a test fails, the printAllMessages method can be called to write all the messages to standard output.
 *
 * TODO: remove this class and consume the version from itests.util instead (which is identical).
 */
public class MemoryAppender extends ConsoleAppender {
    private static final boolean DEBUG = false;

    private static Queue<LoggingEvent> eventsList = new LinkedBlockingQueue<>();
    private static MemoryAppender lastInstance;
    private static int MAX_MESSAGES = 30000;
    private static AtomicInteger messageCount = new AtomicInteger(0);
    private static String gatewayBeingStarted;

    private boolean printNow;
    private String gatewayName;

    static {
        initialize();
    }

    public static void initialize() {
        eventsList.clear();
        lastInstance = null;
        messageCount.set(0);
        gatewayBeingStarted = null;
    }

    private static Queue<LoggingEvent> getEvents() {
        return eventsList;
    }

    public static void assertMessagesLogged(Collection<String> expectedPatternsRO,
                                            Collection<String> unexpectedPatterns,
                                            String filterPattern,
                                            boolean verbose) {
        Set<String> encounteredPatterns = new TreeSet<>();
        List<String> encounteredUnexpectedMessages = new ArrayList<>();
        List<String> expectedPatterns = new ArrayList<>(expectedPatternsRO);
        Collection<String> unexpected = unexpectedPatterns == null ? Collections.emptyList() : unexpectedPatterns;

        for (LoggingEvent event : MemoryAppender.getEvents()) {
            String message = event.getMessage().toString();
            if (filterPattern == null || message.matches(filterPattern)) {
                if (verbose) {
                    System.out.println(event.getLevel() + " " + message);
                }
                Iterator<String> iterator = expectedPatterns.iterator();
                while (iterator.hasNext()) {
                    String pattern = iterator.next();
                    if (message.matches(".*" + pattern + ".*")) {
                        encounteredPatterns.add(pattern);
                        iterator.remove();
                    }
                }
                iterator = unexpected.iterator();
                while (iterator.hasNext()) {
                    String pattern = iterator.next();
                    if (message.matches(".*" + pattern + ".*")) {
                        encounteredUnexpectedMessages.add(message);
                    }
                }
            }
        }
        StringBuffer errorMessage = new StringBuffer();
        if (!encounteredUnexpectedMessages.isEmpty()) {
            errorMessage.append("\n- the following unexpected messages were encountered: ");
            for (String message : encounteredUnexpectedMessages) {
                errorMessage.append("\n  " + message);
            }
        }
        if (!expectedPatterns.isEmpty()) {
            errorMessage.append("\n- the following patterns of log messages were not logged: " + expectedPatterns
                    + (verbose ? ",\nonly these were logged: " + encounteredPatterns : ""));
        }
        assertTrue("Log messages were not as expected" + errorMessage.toString(),
                expectedPatterns.isEmpty() && encounteredUnexpectedMessages.isEmpty());
    }

    public static void printAllMessages() {
        if (lastInstance == null) {
            System.out.println("Unable to print out trace level root logger messages - please "
                    + "configure MemoryAppender on the <root> logger in log4j - config.xml");
        } else {
            System.out.println(String.format("Printing last %d of %d log messages", eventsList.size(), messageCount.get()));
            lastInstance.appendAll();
        }
    }

    /**
     * Call this to identify each gateway when starting multiple embedded gateways. That way, each
     * log message will be prefixed by the name of the gateway that issued the message (though unfortunately
     * this can only be done during gateway startup, see injectGatewayName).
     */
    public static void setGatewayBeingStarted(String gatewayName) {
        gatewayBeingStarted = gatewayName;
    }

    public MemoryAppender() {
        super();
        lastInstance = this;
        gatewayName = gatewayBeingStarted;
        debug("MemoryAppender instance " + this.toString() + " created");
    }

    @Override
    protected void subAppend(LoggingEvent event) {
        if (printNow) {
            super.subAppend(event);
        } else {
            // set name of current thread on the event so it's correct when/if we print the message later
            event.getThreadName();

            if (gatewayName != null) {
                injectGatewayName(event);
            }
            eventsList.add(event);
            // To avoid OOM, limit number of cached messages
            if (messageCount.incrementAndGet() > MAX_MESSAGES) {
                eventsList.poll(); // remove oldest message
            }
        }
    }

    @Override
    public void close() {
        debug("MemoryAppender instance " + this + " closed");
    }

    @Override
    public boolean requiresLayout() {
        return true;
    }

    @Override
    public String toString() {
        String ret = super.toString();
        ret = ret + (gatewayName == null ? "" : " for gateway " + gatewayName);
        return ret;
    }

    private void appendAll() {
        printNow = true;
        try {
            for (LoggingEvent event : eventsList) {
                super.append(event);
            }
        } finally {
            // Make sure we always free up memory
            eventsList.clear();
        }
        printNow = false;
    }

    private void debug(String message) {
        if (DEBUG) {
            System.out.println(message);
        }
    }

    private void injectGatewayName(LoggingEvent event) {
        if (!injectGatewayName(event, "renderedMessage")) {
            injectGatewayName(event, "message");
        }
    }

    private boolean injectGatewayName(LoggingEvent event, String fieldName) {
        Field field;
        try {
            field = LoggingEvent.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            String oldMessage = (String) field.get(event);
            if (oldMessage != null) {
                String newMessage = String.format("[%s gateway] %s", gatewayName, oldMessage);
                field.set(event,  newMessage);
                if (oldMessage.contains("Started server successfully in ")) {
                    // Unfortunately when multiple gateways are being used, each time one is started, all appender instances
                    // are closed and recreated. So we can only rely on gatewayName being correct during gateway startup.
                    // So we must unset it once the gateway is started.
                    gatewayName = null;
                }
                return true;
            }
            else {
                return false;
            }
        } catch (Exception e) {
            System.out.println(this + ": caught exception " + e);
            return true;
        }
    }
}

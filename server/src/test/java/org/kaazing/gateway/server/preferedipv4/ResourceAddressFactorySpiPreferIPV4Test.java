/**
 * Copyright 2007-2015, Kaazing Corporation. All rights reserved.
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
package org.kaazing.gateway.server.preferedipv4;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.net.URI;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.kaazing.gateway.server.test.Gateway;
import org.kaazing.gateway.server.test.config.GatewayConfiguration;
import org.kaazing.gateway.server.test.config.builder.GatewayConfigurationBuilder;

public class ResourceAddressFactorySpiPreferIPV4Test {
	@Before
    public void setUp() {
		System.setProperty("java.net.preferIPv4Stack" , "true");
    }

    @After
    public void tearDown() {
    	System.setProperty("java.net.preferIPv4Stack" , "false");
    }

    @Test
    public void testEmptyCollection() {
    	GatewayConfiguration gc = new GatewayConfigurationBuilder().
    			service().
    				name("echo").
    				type("echo")
    				.accept(URI.create("ws://[::1]:8000/echo/")).
    			done().
    		done();

    	Gateway gateway = new Gateway();
    	
    	try {
    		gateway.start(gc);
    		assertTrue("This should not be reached", false);
    	} 
    	catch (AssertionError e) {
    		assertTrue(false); 
    	}
    	catch (Exception e) {
    		assertTrue(
    				"Exception message on binding",
    				e.getMessage()
    						.startsWith(
    								"Error binding to ws://[::1]:8000/: Tried to bind address"));
    	}

    	try {
    		gateway.stop();
    	} catch (Exception e) {
    		assertFalse(e instanceof java.lang.NullPointerException);
    	}
    }
}

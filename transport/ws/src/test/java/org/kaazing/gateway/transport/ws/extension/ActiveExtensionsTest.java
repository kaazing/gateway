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

package org.kaazing.gateway.transport.ws.extension;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public class ActiveExtensionsTest {

    @Test
    public void emptyShouldReturnEmptyList() {
        assertEquals(0, ActiveExtensions.EMPTY.asList().size());
    }

    @Test
    public void shouldMaintainExtensionOrder() {
        List<WebSocketExtension> negotiatedExtensions = new ArrayList<>();
        negotiatedExtensions.add(new WebSocketExtension() {

            @Override
            public ExtensionHeader getExtensionHeader() {
                return new ExtensionHeaderBuilder("1");
            }
        });
        negotiatedExtensions.add(new WebSocketExtension() {

            @Override
            public ExtensionHeader getExtensionHeader() {
                return new ExtensionHeaderBuilder("2");
            }
        });
        negotiatedExtensions.add(new WebSocketExtension() {

            @Override
            public ExtensionHeader getExtensionHeader() {
                return new ExtensionHeaderBuilder("3");
            }
        });
        ActiveExtensions activeExtensions = new ActiveExtensions(negotiatedExtensions);
        List<WebSocketExtension> maintainedList = activeExtensions.asList();
        for (int i = 0; i < negotiatedExtensions.size(); i++) {
            assertSame(negotiatedExtensions.get(i), maintainedList.get(i));
        }
        assertEquals(negotiatedExtensions.size(), maintainedList.size());
    }

    @Test
    public void testHasExtension() {
        List<WebSocketExtension> negotiatedExtensions = new ArrayList<>();
        negotiatedExtensions.add(new MockWebSocketExtension());
        ActiveExtensions activeExtensions = new ActiveExtensions(negotiatedExtensions);
        assertTrue(activeExtensions.hasExtension(MockWebSocketExtension.class));
        assertFalse(activeExtensions.hasExtension(FooWebSocketExtension.class));
    }

    private class MockWebSocketExtension extends WebSocketExtension {

        @Override
        public ExtensionHeader getExtensionHeader() {
            return new ExtensionHeaderBuilder("mock");
        }

    }

    private class FooWebSocketExtension extends WebSocketExtension {

        @Override
        public ExtensionHeader getExtensionHeader() {
            return new ExtensionHeaderBuilder("foo");
        }

    }
}

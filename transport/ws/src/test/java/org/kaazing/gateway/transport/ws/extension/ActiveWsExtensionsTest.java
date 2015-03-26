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

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.kaazing.gateway.transport.ws.WsMessage.Kind.BINARY;
import static org.kaazing.gateway.transport.ws.WsMessage.Kind.TEXT;
import static org.kaazing.gateway.transport.ws.extension.WsExtension.EndpointKind.SERVER;
import static org.kaazing.gateway.util.Utils.asByteBuffer;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;

import org.apache.mina.filter.codec.ProtocolDecoderOutput;
import org.jmock.Mockery;
import org.jmock.api.Invocation;
import org.jmock.lib.action.CustomAction;
import org.junit.Test;
import org.kaazing.gateway.transport.ws.WsMessage;
import org.kaazing.gateway.transport.ws.WsMessage.Kind;
import org.kaazing.gateway.transport.ws.extension.WsExtension.EndpointKind;
import org.kaazing.gateway.transport.ws.util.Expectations;
import org.kaazing.mina.core.buffer.IoBufferEx;
import org.kaazing.mina.core.buffer.SimpleBufferAllocator;

public class ActiveWsExtensionsTest {
    private static final byte[] CONTROL1 = new byte[]{(byte)0x01,(byte)0x02,(byte)0x03,(byte)0x04};
    private static final byte[] CONTROL2 = new byte[]{(byte)0x01,(byte)0x02,(byte)0x03,(byte)0x05};
    private static final byte[] CONTROL3 = new byte[]{(byte)0x01,(byte)0x02,(byte)0x03,(byte)0x06};
    
    @Test
    public void commonBytesShouldBe0() {
        WsExtension extension1 = new TestExtension("x-kaazing-test", new byte[]{(byte)0x01,(byte)0x02,(byte)0x03,(byte)0x04});
        WsExtension extension2 = new TestExtension("x-kaazing-test-nomatch", new byte[]{(byte)0x00,(byte)0x02,(byte)0x03,(byte)0x04});
        List<WsExtension> extensions = asList(extension1, extension2);
        byte[] result = ActiveWsExtensions.getCommonBytes(extensions, SERVER);
        assertEquals(0, result.length);
        assertTrue(Arrays.equals(new byte[0], result));
    }
    
    @Test
    public void commonBytesShouldBe3() {
        WsExtension extension1 = new TestExtension("x-kaazing-test", CONTROL1);
        WsExtension extension2 = new TestExtension("x-kaazing-test-nomatch", CONTROL2);
        List<WsExtension> extensions = asList(extension1, extension2);
        byte[] result = ActiveWsExtensions.getCommonBytes(extensions, SERVER);
        assertEquals(3, result.length);
        assertTrue(Arrays.equals(new byte[]{(byte)0x01,(byte)0x02,(byte)0x03}, result));
    }

    
    @Test
    public void emptyShouldReturnEmptyList() {
        assertEquals(0, ActiveWsExtensions.EMPTY.asList().size());
    }
    
    @Test
    public void emptyShouldNotDecode() {
        Mockery context = new Mockery();
        final ProtocolDecoderOutput out = context.mock(ProtocolDecoderOutput.class);
        {
            context.checking(new Expectations() {
                {
                }
            });
        }
        assertFalse(ActiveWsExtensions.EMPTY.decode(null, TEXT, out));
    }
    
    @Test
    public void extensionsShouldBeOrdered() {
        WsExtension extension1 = new TestExtension("x-kaazing-test1", CONTROL1) {
            @Override
            // Make sure this extension is last
            public String getOrdering() {
                return "y";
            }
        };
        WsExtension extension2 = new TestExtension("x-kaazing-test2", CONTROL2);
        WsExtension extension3 = new TestExtension("x-kaazing-test3", CONTROL3);
        ActiveWsExtensions extensions = new ActiveWsExtensions(asList(extension1, extension2, extension3), SERVER);
        assertEquals(asList(extension2, extension3, extension1), extensions.asList());
    }
    
    @Test
    public void mergeShouldCombineInOrder() {
        WsExtension extension1 = new TestExtension("x-kaazing-test1", CONTROL1) {
            @Override
            // Make sure this extension is last
            public String getOrdering() {
                return "zzz";
            }
        };
        WsExtension extension2 = new TestExtension("x-kaazing-test2", CONTROL2);
        WsExtension extension3 = new TestExtension("x-kaazing-test3", CONTROL3);
        ActiveWsExtensions extensions1 = new ActiveWsExtensions(asList(extension1, extension2), SERVER);
        ActiveWsExtensions extensions2 = new ActiveWsExtensions(asList(extension3), SERVER);
        ActiveWsExtensions result = ActiveWsExtensions.merge(extensions1, extensions2, SERVER);
        assertEquals(asList(extension2, extension3, extension1), result.asList());
    }
    
    @Test
    public void shouldDecode() {
        Mockery context = new Mockery();
        final ProtocolDecoderOutput out = context.mock(ProtocolDecoderOutput.class);
        final WsMessage[] result = new WsMessage[1];
        {
            context.checking(new Expectations() {
                {
                    oneOf(out).write(with(any(WsMessage.class)));
                    will(new CustomAction("save result") {
                        @Override
                        public Object invoke(Invocation invocation) throws Throwable {
                            result[0] = (WsMessage)invocation.getParameter(0);
                            return null;
                        }
                        
                    });
                }
            });
        }
        ByteBuffer  buf = ByteBuffer.allocate(9);
        buf.put(CONTROL1);
        buf.put(asByteBuffer("hello"));
        buf.flip();
        final IoBufferEx payload = SimpleBufferAllocator.BUFFER_ALLOCATOR.wrap(buf);
        
        WsExtension extension = new TestExtension("x-kaazing-test", CONTROL1);
        WsExtension[] negotiated = new WsExtension[]{extension};
        ActiveWsExtensions extensions = new ActiveWsExtensions(asList(negotiated), SERVER);
        assertTrue(extensions.decode(payload, TEXT, out));
        assertSame(extension, result[0].getExtension());
        assertEquals(asByteBuffer("hello"), result[0].getBytes().buf());
        context.assertIsSatisfied();
    }
    
    @Test
    public void shouldDecodeWithMultipleDecodingExtensions() {
        Mockery context = new Mockery();
        final ProtocolDecoderOutput out = context.mock(ProtocolDecoderOutput.class);
        final WsMessage[] result = new WsMessage[1];
        {
            context.checking(new Expectations() {
                {
                    exactly(2).of(out).write(with(any(WsMessage.class)));
                    will(new CustomAction("save result") {
                        @Override
                        public Object invoke(Invocation invocation) throws Throwable {
                            result[0] = (WsMessage)invocation.getParameter(0);
                            return null;
                        }
                        
                    });
                }
            });
        }
        WsExtension extension1 = new TestExtension("x-kaazing-test-1", CONTROL1);
        WsExtension extension2 = new TestExtension("x-kaazing-test-2", CONTROL2);
        WsExtension[] negotiated = new WsExtension[]{extension1, extension2};
        ActiveWsExtensions extensions = new ActiveWsExtensions(Arrays.asList(negotiated), EndpointKind.SERVER);
        
        ByteBuffer  buf = ByteBuffer.allocate(9);
        buf.put(extension1.getControlBytes());
        buf.put(asByteBuffer("hello"));
        buf.flip();
        final IoBufferEx payload1 = SimpleBufferAllocator.BUFFER_ALLOCATOR.wrap(buf);
        assertTrue(extensions.decode(payload1, BINARY, out));
        assertSame(extension1, result[0].getExtension());
        assertEquals(asByteBuffer("hello"), result[0].getBytes().buf());
        
        buf.clear();
        buf.put(CONTROL2);
        buf.put(asByteBuffer("hello"));
        buf.flip();
        final IoBufferEx payload2 = SimpleBufferAllocator.BUFFER_ALLOCATOR.wrap(buf);
        assertTrue(extensions.decode(payload2, TEXT, out));
        assertSame(extension2, result[0].getExtension());
        assertEquals(asByteBuffer("hello"), result[0].getBytes().buf());
    }
    
    @Test
    public void shouldNotDecodeWithMultipleEncodingOnlyExtensions() {
        Mockery context = new Mockery();
        final ProtocolDecoderOutput out = context.mock(ProtocolDecoderOutput.class);
        final WsMessage[] result = new WsMessage[1];
        {
            context.checking(new Expectations() {
                {
                    atLeast(1).of(out).write(with(any(WsMessage.class)));
                    will(new CustomAction("save result") {
                        @Override
                        public Object invoke(Invocation invocation) throws Throwable {
                            result[0] = (WsMessage)invocation.getParameter(0);
                            return null;
                        }
                        
                    });
                }
            });
        }
        final WsExtension extension1 = context.mock(WsExtension.class, "extension1");
        final WsExtension extension2 = context.mock(WsExtension.class, "extensions2");
        WsExtension[] negotiated = new WsExtension[]{extension1, extension2};
        
        context.checking(new Expectations() {
            {
                allowing(extension1).getExtensionToken(); will(returnValue("extension1"));
                allowing(extension2).getExtensionToken(); will(returnValue("extension2"));
                allowing(extension1).getOrdering(); will(returnValue("extension1"));
                allowing(extension2).getOrdering(); will(returnValue("extension2"));
                allowing(extension1).getControlBytes(); will(returnValue(asByteBuffer("ext1").array()));
                allowing(extension2).getControlBytes(); will(returnValue(asByteBuffer("ext2").array()));
                allowing(extension1).canDecode( with(any(EndpointKind.class)), with(any(Kind.class)) );
                will(returnValue(false));
                allowing(extension2).canDecode( with(any(EndpointKind.class)), with(any(Kind.class)) );
                will(returnValue(false));
                allowing(extension1).canEncode( with(any(EndpointKind.class)), with(any(Kind.class)) );
                will(returnValue(true));
                allowing(extension2).canEncode( with(any(EndpointKind.class)), with(any(Kind.class)) );
                will(returnValue(true));
            }
        });
        ActiveWsExtensions extensions = new ActiveWsExtensions(Arrays.asList(negotiated), EndpointKind.SERVER);
        
        ByteBuffer  buf = ByteBuffer.allocate(9);
        buf.put(extension1.getControlBytes());
        buf.put(asByteBuffer("hello"));
        buf.flip();
        final IoBufferEx payload1 = SimpleBufferAllocator.BUFFER_ALLOCATOR.wrap(buf);
        assertFalse(extensions.decode(payload1, BINARY, out));
    }
    
    @Test
    public void shouldReturnTrueFromDecodeEscapeFrameWithNoOutput() {
        Mockery context = new Mockery();
        final ProtocolDecoderOutput out = context.mock(ProtocolDecoderOutput.class);
        context.checking(new Expectations() {
            {
                // out.write should not be invoked
            }
        });
        ByteBuffer  buf = ByteBuffer.allocate(4);
        buf.put(CONTROL1);
        buf.flip();
        final IoBufferEx payload = SimpleBufferAllocator.BUFFER_ALLOCATOR.wrap(buf);
        
        WsExtension extension = new TestExtension("x-kaazing-test", CONTROL1);
        WsExtension[] negotiated = new WsExtension[]{extension};
        ActiveWsExtensions extensions = new ActiveWsExtensions(asList(negotiated), SERVER);
        assertTrue(extensions.decode(payload, TEXT, out));
        context.assertIsSatisfied();
    }
    
    
    class TestExtension extends WsExtensionBuilder {
        private final byte[] controlBytes;

        public TestExtension(String extensionToken, byte[] controlBytes) {
            super(extensionToken);
            this.controlBytes = controlBytes;
        }
        
        @Override
        public byte[] getControlBytes() {
            return controlBytes;
        }
        
        @Override
        public boolean canDecode(EndpointKind endpointKind, Kind messageKind) {
            return true;
        }
        
        @Override
        public boolean canEncode(EndpointKind endpointKind, Kind messageKind) {
            return true;
        }
        
        @Override
        public WsMessage decode(IoBufferEx payload) {
            WsMessage result = new WsMessage() {

                @Override
                public Kind getKind() {
                    return Kind.BINARY;
                }
                
            };
            result.setExtension(this);
            result.setBytes(payload);
            return result;
        }
    }

}


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
package org.kaazing.gateway.transport.ws.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;

import org.apache.mina.core.filterchain.IoFilter;
import org.apache.mina.core.filterchain.IoFilter.NextFilter;
import org.apache.mina.core.filterchain.IoFilterChain;
import org.apache.mina.core.filterchain.IoFilterChain.Entry;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFactory;
import org.apache.mina.filter.codec.ProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolEncoder;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.Before;
import org.junit.Test;
import org.kaazing.gateway.transport.http.bridge.HttpRequestMessage;
import org.kaazing.gateway.transport.ws.extension.ExtensionHeader;
import org.kaazing.gateway.transport.ws.extension.ExtensionHelper;
import org.kaazing.gateway.transport.ws.extension.WebSocketExtension;
import org.kaazing.gateway.transport.ws.util.WsDigestException;
import org.kaazing.gateway.transport.ws.util.WsUtils;
import org.kaazing.gateway.util.ws.WebSocketWireProtocol;
import org.kaazing.mina.core.filterchain.DefaultIoFilterChain;
import org.kaazing.mina.core.session.AbstractIoSession;
import org.kaazing.mina.core.session.DummySessionEx;
import org.kaazing.mina.filter.codec.ProtocolCodecFilter;

public class WsUtilsTest {

    private Mockery context;
    private WebSocketExtension extension1;
    private WebSocketExtension extension2;
    private WebSocketExtension extension3;
    private List<WebSocketExtension> extensions;
    private ExtensionHeader extensionHeader1;
    private ExtensionHeader extensionHeader2;
    private ExtensionHeader extensionHeader3;
    private IoFilter extensionFilter2;
    private IoFilter extensionFilter3;




    @Before
    public void before() {
        context = new Mockery() {
            {
                setImposteriser(ClassImposteriser.INSTANCE);
            }
        };
        extension1 = context.mock(WebSocketExtension.class, "extension1");
        extension2 = context.mock(WebSocketExtension.class, "extension2");
        extension3  = context.mock(WebSocketExtension.class, "extension3");
        extensions = Arrays.asList(extension1, extension2, extension3);
        extensionHeader1 = context.mock(ExtensionHeader.class, "extensionHeader1");
        extensionHeader2 = context.mock(ExtensionHeader.class, "extensionHeader2");
        extensionHeader3 = context.mock(ExtensionHeader.class, "extensionHeader3");
        extensionFilter2 = context.mock(IoFilter.class, "extensionFilter2");
        extensionFilter3 = context.mock(IoFilter.class, "extensionFilter3");

    }

    @Test(expected = WsDigestException.class)
    public void testComputeHashMissingKey() throws Exception {
        String key1 = "67 89";
        String key2 = "12 34";
        ByteBuffer key3 = ByteBuffer.allocate(0);
        WsUtils.computeHash(key1, key2, key3);
    }

    @Test(expected = WsDigestException.class)
    public void testCompteHashKeyOverrun() throws Exception {
        String key1 = "67 89";
        String key2 = "12 34";
        ByteBuffer key3 = ByteBuffer.allocate(20);

        for (int i = 0; i < 5; i++) {
            key3.putInt(0);
        }
        key3.flip();

        WsUtils.computeHash(key1, key2, key3);
    }

    @Test
    public void testComputeHash() throws Exception {
        String key1 = "3 8Z116563   04>";
        String key2 = "]   x*CmF1 05 4 )9     558Y 80";
        ByteBuffer key3 = ByteBuffer.allocate(8);
        key3.put((byte) 0xf7);
        key3.put((byte) 0x66);
        key3.put((byte) 0x3e);
        key3.put((byte) 0x5d);
        key3.put((byte) 0xeb);
        key3.put((byte) 0x27);
        key3.put((byte) 0x10);
        key3.put((byte) 0x35);
        key3.flip();

        ByteBuffer expected = ByteBuffer.allocate(16);
        expected.put((byte) 0xca);
        expected.put((byte) 0xf8);
        expected.put((byte) 0xdf);
        expected.put((byte) 0xfa);
        expected.put((byte) 0x2e);
        expected.put((byte) 0xbb);
        expected.put((byte) 0xf6);
        expected.put((byte) 0x6d);
        expected.put((byte) 0xfd);
        expected.put((byte) 0x84);
        expected.put((byte) 0xc9);
        expected.put((byte) 0x90);
        expected.put((byte) 0x59);
        expected.put((byte) 0x1d);
        expected.put((byte) 0x20);
        expected.put((byte) 0x09);
        expected.flip();

        ByteBuffer result = WsUtils.computeHash(key1, key2, key3);
        assertEquals(expected, result);
    }


    @Test(expected = IllegalArgumentException.class)
    public void calculateLengthEncodedSizeForNegative() {
        WsUtils.calculateEncodedLengthSize(-1);
    }

    @Test
    public void calculateLengthEncodedSizeFor0() {
        assertEquals(1, WsUtils.calculateEncodedLengthSize(0));
    }

    @Test
    public void calculateLengthEncodedSizeFor127() {
        assertEquals(1, WsUtils.calculateEncodedLengthSize(127));
    }

    @Test
    public void calculateLengthEncodedSizeFor128() {
        assertEquals(2, WsUtils.calculateEncodedLengthSize(128));
    }

    @Test
    public void calculateLengthEncodedSizeFor16383() {
        assertEquals(2, WsUtils.calculateEncodedLengthSize(16383));
    }

    @Test
    public void calculateLengthEncodedSizeFor16384() {
        assertEquals(3, WsUtils.calculateEncodedLengthSize(16384));
    }

    @Test
    public void calculateLengthEncodedSizeFor2097151() {
        assertEquals(3, WsUtils.calculateEncodedLengthSize(2097151));
    }

    @Test
    public void calculateLengthEncodedSizeFor2097152() {
        assertEquals(4, WsUtils.calculateEncodedLengthSize(2097152));
    }

    @Test
    public void calculateLengthEncodedSizeFor268435455() {
        assertEquals(4, WsUtils.calculateEncodedLengthSize(268435455));
    }

    @Test
    public void calculateLengthEncodedSizeFor268435456() {
        assertEquals(5, WsUtils.calculateEncodedLengthSize(268435456));
    }

    @Test
    public void calculateLengthEncodedSizeForMaxInt() {
        assertEquals(5, WsUtils.calculateEncodedLengthSize(Integer.MAX_VALUE));
    }

    @Test
    public void testHixie75() throws Exception {
        HttpRequestMessage request = new HttpRequestMessage();
        WebSocketWireProtocol protocol = WsUtils.guessWireProtocolVersion(request);
        assertEquals(WebSocketWireProtocol.HIXIE_75, protocol);
    }

    @Test
    public void testHixie76() throws Exception {
        HttpRequestMessage request = new HttpRequestMessage();
        request.addHeader(WsUtils.SEC_WEB_SOCKET_KEY1, "nonempty");
        request.addHeader(WsUtils.SEC_WEB_SOCKET_KEY2, "nonempty");
        WebSocketWireProtocol protocol = WsUtils.guessWireProtocolVersion(request);
        assertEquals(WebSocketWireProtocol.HIXIE_76, protocol);

    }

    @Test
    public void ensureThatIfHixie76IsMissingKeyWeAssumeHixie75() throws Exception {
        HttpRequestMessage request = new HttpRequestMessage();
        request.addHeader(WsUtils.SEC_WEB_SOCKET_KEY1, "nonempty");
        WebSocketWireProtocol protocol = WsUtils.guessWireProtocolVersion(request);
        assertEquals(WebSocketWireProtocol.HIXIE_75, protocol);
    }

    @Test
    public void testInvalidSpecificationVersionsShouldReturnNull() throws Exception {
        HttpRequestMessage request = new HttpRequestMessage();
        request.addHeader(WsUtils.SEC_WEB_SOCKET_VERSION, String.valueOf(-10));
        WebSocketWireProtocol protocol = WsUtils.guessWireProtocolVersion(request);
        assertNull(protocol);

        request = new HttpRequestMessage();
        request.addHeader(WsUtils.SEC_WEB_SOCKET_VERSION, String.valueOf(500));
        protocol = WsUtils.guessWireProtocolVersion(request);
        assertNull(protocol);
    }

    @Test
    public void testWireProtocolExplicitVersionsExhaustive() throws Exception {
        HttpRequestMessage request = new HttpRequestMessage();
        request.addHeader(WsUtils.SEC_WEB_SOCKET_VERSION, String.valueOf(-5));
        WebSocketWireProtocol protocol = WsUtils.guessWireProtocolVersion(request);
        assertSame(WebSocketWireProtocol.HIXIE_75, protocol);

        request = new HttpRequestMessage();
        request.addHeader(WsUtils.SEC_WEB_SOCKET_VERSION, String.valueOf(-4));
        protocol = WsUtils.guessWireProtocolVersion(request);
        assertSame(WebSocketWireProtocol.HIXIE_76, protocol);

        for (int hybi = 1; hybi <= 17; hybi++) {
            request = new HttpRequestMessage();
            if (hybi <= 7) {
                request.addHeader(WsUtils.SEC_WEB_SOCKET_VERSION, String.valueOf(hybi));
                protocol = WsUtils.guessWireProtocolVersion(request);
                assertSame(WebSocketWireProtocol.valueOf(hybi), protocol);
            }
            if (hybi >= 8 && hybi < 13) {
                request.addHeader(WsUtils.SEC_WEB_SOCKET_VERSION, String.valueOf(8));
                protocol = WsUtils.guessWireProtocolVersion(request);
                assertSame(WebSocketWireProtocol.HYBI_8, protocol);
            }
            if (hybi >= 13 && hybi <= 17) {
                request.addHeader(WsUtils.SEC_WEB_SOCKET_VERSION, String.valueOf(hybi));
                protocol = WsUtils.guessWireProtocolVersion(request);
                assertSame(WebSocketWireProtocol.RFC_6455, protocol);
            }
        }
    }

    @Test
    public void shouldAddExtensionFiltersAtStartWhenNoCodec() throws Exception {
        AbstractIoSession session = new DummySessionEx();
        final IoFilter filter1 = context.mock(IoFilter.class, "filter1");
        final IoFilter filter2 = context.mock(IoFilter.class, "filter2");
        final ExtensionHelper helper = context.mock(ExtensionHelper.class, "helper");

        final IoFilterChain filterChain = new DefaultIoFilterChain(session);

        context.checking(new Expectations() {
            {
                oneOf(filter1).onPreAdd(with(filterChain), with(any(String.class)), with(any(NextFilter.class)));
                oneOf(filter1).onPostAdd(with(filterChain), with(any(String.class)), with(any(NextFilter.class)));
                oneOf(filter2).onPreAdd(with(filterChain), with(any(String.class)), with(any(NextFilter.class)));
                oneOf(filter2).onPostAdd(with(filterChain), with(any(String.class)), with(any(NextFilter.class)));
                oneOf(extension1).getFilter(); will(returnValue((null)));
                oneOf(extension2).getFilter(); will(returnValue((extensionFilter2)));
                oneOf(extension2).getExtensionHeader(); will(returnValue(extensionHeader2));
                oneOf(extensionFilter2).onPreAdd(with(filterChain), with(any(String.class)), with(any(NextFilter.class)));
                oneOf(extensionFilter2).onPostAdd(with(filterChain), with(any(String.class)), with(any(NextFilter.class)));
                oneOf(extensionFilter3).onPreAdd(with(filterChain), with(any(String.class)), with(any(NextFilter.class)));
                oneOf(extensionFilter3).onPostAdd(with(filterChain), with(any(String.class)), with(any(NextFilter.class)));
                oneOf(extension3).getFilter(); will(returnValue((extensionFilter3)));
                oneOf(extension3).getExtensionHeader(); will(returnValue(extensionHeader3));
                oneOf(extensionHeader2).getExtensionToken(); will(returnValue(("extension2")));
                oneOf(extensionHeader3).getExtensionToken(); will(returnValue(("extension3")));
            }
        });
        filterChain.addLast("filter1", filter1);
        filterChain.addLast("filter2", filter2);
        WsUtils.addExtensionFilters(extensions, helper, filterChain, false);

        String[] expected = new String[]{"extension3", "extension2", "filter1", "filter2"};
        int i = 0;
        for (Entry entry : filterChain.getAll()) {
            assertEquals(expected[i++], entry.getName());
        }
        context.assertIsSatisfied();
    }

    @Test
    public void shouldAddExtensionFiltersAfterCodec() throws Exception {
        shouldAddExtensionFiltersAfterCodec_withResult();
    }

    private IoFilterChain shouldAddExtensionFiltersAfterCodec_withResult() throws Exception {
        final ProtocolEncoder protocolEncoder = context.mock(ProtocolEncoder.class);
        final ProtocolDecoder protocolDecoder = context.mock(ProtocolDecoder.class);
        final ExtensionHelper helper = context.mock(ExtensionHelper.class, "helper");

        AbstractIoSession session = new DummySessionEx();
        final IoFilter filter1 = context.mock(IoFilter.class, "filter1");
        final IoFilter filter2 = context.mock(IoFilter.class, "filter2");
        final IoFilter codec = new ProtocolCodecFilter(new ProtocolCodecFactory() {

            @Override
            public ProtocolEncoder getEncoder(IoSession arg0) throws Exception {
                return protocolEncoder;
            }

            @Override
            public ProtocolDecoder getDecoder(IoSession arg0) throws Exception {
                return protocolDecoder;
            }
        });

        final IoFilterChain filterChain = new DefaultIoFilterChain(session);

        context.checking(new Expectations() {
            {
                oneOf(extension1).getFilter(); will(returnValue((null)));
                oneOf(extension2).getFilter(); will(returnValue((extensionFilter2)));
                oneOf(extension2).getExtensionHeader(); will(returnValue(extensionHeader2));
                oneOf(filter1).onPreAdd(with(filterChain), with(any(String.class)), with(any(NextFilter.class)));
                oneOf(filter1).onPostAdd(with(filterChain), with(any(String.class)), with(any(NextFilter.class)));
                oneOf(filter2).onPreAdd(with(filterChain), with(any(String.class)), with(any(NextFilter.class)));
                oneOf(filter2).onPostAdd(with(filterChain), with(any(String.class)), with(any(NextFilter.class)));
                oneOf(extensionFilter2).onPreAdd(with(filterChain), with(any(String.class)), with(any(NextFilter.class)));
                oneOf(extensionFilter2).onPostAdd(with(filterChain), with(any(String.class)), with(any(NextFilter.class)));
                oneOf(extensionFilter3).onPreAdd(with(filterChain), with(any(String.class)), with(any(NextFilter.class)));
                oneOf(extensionFilter3).onPostAdd(with(filterChain), with(any(String.class)), with(any(NextFilter.class)));
                oneOf(extension3).getFilter(); will(returnValue((extensionFilter3)));
                oneOf(extension3).getExtensionHeader(); will(returnValue(extensionHeader3));
                oneOf(extensionHeader2).getExtensionToken(); will(returnValue(("extension2")));
                oneOf(extensionHeader3).getExtensionToken(); will(returnValue(("extension3")));
            }
        });
        filterChain.addLast("filter1", filter1);
        filterChain.addLast("codec", codec);
        filterChain.addLast("filter2", filter2);
        WsUtils.addExtensionFilters(extensions, helper, filterChain, true);

        String[] expected = new String[]{"filter1", "codec", "extension3", "extension2", "filter2"};
        int i = 0;
        for (Entry entry : filterChain.getAll()) {
            assertEquals(expected[i++], entry.getName());
        }
        context.assertIsSatisfied();
        return filterChain;
    }

    @Test
    public void shouldRemoveExtensionFilters() throws Exception {
        final IoFilterChain filterChain = shouldAddExtensionFiltersAfterCodec_withResult();

        context.checking(new Expectations() {
            {
                oneOf(extension1).getFilter(); will(returnValue((null)));
                oneOf(extension2).getFilter(); will(returnValue((extensionFilter2)));
                oneOf(extension1).getExtensionHeader(); will(returnValue(extensionHeader1));
                oneOf(extensionHeader1).getExtensionToken(); will(returnValue(("extension1")));
                oneOf(extension2).getExtensionHeader(); will(returnValue(extensionHeader2));
                oneOf(extensionHeader2).getExtensionToken(); will(returnValue(("extension2")));
                oneOf(extension3).getFilter(); will(returnValue((extensionFilter3)));
                oneOf(extension3).getExtensionHeader(); will(returnValue(extensionHeader3));
                oneOf(extensionHeader3).getExtensionToken(); will(returnValue(("extension3")));
                oneOf(extensionFilter2).onPreRemove(with(filterChain), with(any(String.class)), with(any(NextFilter.class)));
                oneOf(extensionFilter2).onPostRemove(with(filterChain), with(any(String.class)), with(any(NextFilter.class)));
                oneOf(extensionFilter3).onPreRemove(with(filterChain), with(any(String.class)), with(any(NextFilter.class)));
                oneOf(extensionFilter3).onPostRemove(with(filterChain), with(any(String.class)), with(any(NextFilter.class)));
            }
        });

        WsUtils.removeExtensionFilters(extensions, filterChain);

    }

}


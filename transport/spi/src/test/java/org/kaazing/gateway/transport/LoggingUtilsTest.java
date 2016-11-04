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
package org.kaazing.gateway.transport;

import static org.junit.Assert.assertEquals;
import static org.kaazing.gateway.resource.address.ResourceAddress.IDENTITY_RESOLVER;

import java.io.IOException;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicReference;

import javax.security.auth.Subject;

import org.apache.mina.core.service.TransportMetadata;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.Rule;
import org.junit.Test;
import org.kaazing.gateway.resource.address.IdentityResolver;
import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.resource.address.ResourceAddressFactory;
import org.kaazing.gateway.resource.address.ResourceOptions;
import org.kaazing.gateway.resource.address.http.HttpIdentityResolver;
import org.kaazing.gateway.security.auth.config.parse.DefaultUserConfig;
import org.kaazing.gateway.transport.test.Expectations;
import org.kaazing.mina.core.service.IoAcceptorEx;
import org.kaazing.mina.core.service.IoConnectorEx;
import org.kaazing.mina.core.service.IoServiceEx;
import org.kaazing.mina.core.session.IoSessionEx;
import org.slf4j.Logger;

/**
 * NOTE: tests for Utils.parseTimeInterval are in separate unit test class ParseTimeIntervalTest
 */
public class LoggingUtilsTest {

    public static long TEST_SESSION_NUMBER = 23;

    @Rule
    public JUnitRuleMockery context = new JUnitRuleMockery();

    private final IoSessionEx sessionEx = context.mock(IoSessionEx.class, "sessionEx");

    @Test
    public void getIdShouldReturnAndCacheCorrectValue() {
        final TransportMetadata metadata = context.mock(TransportMetadata.class, "metadata");
        final IoServiceEx service = context.mock(IoServiceEx.class, "service");
        ResourceAddressFactory addressFactory = ResourceAddressFactory.newResourceAddressFactory();
        String addressURI = "ws://localhost:2121/jms";
        final SocketAddress address = addressFactory.newResourceAddress(addressURI);
        final Subject subject = new Subject();

        final String expected = "wsn#23 127.0.0.1:2121";
        AtomicReference<String> value = new AtomicReference<String>();

        context.checking(new Expectations() {
            {
                oneOf(sessionEx).getAttribute(with(any(Object.class))); will(returnValue(null));
                oneOf(sessionEx).getId(); will(returnValue(TEST_SESSION_NUMBER));
                oneOf(sessionEx).getTransportMetadata(); will(returnValue(metadata));
                oneOf(metadata).getName(); will(returnValue("wsn"));
                oneOf(sessionEx).getService(); will(returnValue(service));
                oneOf(sessionEx).getLocalAddress(); will(returnValue(address));
                oneOf(sessionEx).getSubject(); will(returnValue(subject));
                oneOf(sessionEx).getRemoteAddress(); will(returnValue(address));
                oneOf(sessionEx).setAttribute(with(any(Object.class)), with(any(String.class)));
                will(saveParameter(value, 1));
            }
        });

        assertEquals(expected, LoggingUtils.getId(sessionEx));
        context.assertIsSatisfied();

        context.checking(new Expectations() {
            {
                oneOf(sessionEx).getAttribute(with(any(Object.class))); will(returnValue(value.get()));
            }
        });

        assertEquals(expected, LoggingUtils.getId(sessionEx));
    }

    @Test
    public void getUserIdentifierShouldReturnLocalAddress() throws Exception {

        final IoConnectorEx  connector = context.mock(IoConnectorEx .class, "connector");

        final InetSocketAddress address = new InetSocketAddress(InetAddress.getByName("localhost"), 2121);

        context.checking(new Expectations() {
            {
                oneOf(sessionEx).getService(); will(returnValue(connector));
                oneOf(sessionEx).getLocalAddress(); will(returnValue(address));
                oneOf(sessionEx).getRemoteAddress(); will(returnValue(address));
            }
        });
        assertEquals("localhost:2121", LoggingUtils.getUserIdentifier(sessionEx));
    }

    @Test
    public void getUserIdentifierShouldReturnRemoteIpv4AddressForIoAcceptor() throws Exception {

        final IoAcceptorEx  service = context.mock(IoAcceptorEx .class, "service");

        final InetSocketAddress address = new InetSocketAddress(InetAddress.getByAddress(new byte[]{127,0,0,1}), 2121);

        context.checking(new Expectations() {
            {
                oneOf(sessionEx).getService(); will(returnValue(service));
                oneOf(sessionEx).getRemoteAddress(); will(returnValue(address));
                oneOf(sessionEx).getLocalAddress(); will(returnValue(address));
            }
        });
        assertEquals("127.0.0.1:2121", LoggingUtils.getUserIdentifier(sessionEx));
    }

    @Test
    public void getUserIdentifierShouldReturnIpv6AddressForIoAcceptor() throws Exception {

        final IoAcceptorEx  service = context.mock(IoAcceptorEx .class, "service");

        final InetSocketAddress address = new InetSocketAddress(InetAddress.getByAddress(
                new byte[]{(byte) 0xfe, (byte) 0x80, 0, 0, 0, 0, 0, 0, (byte) 0x90,
                        (byte) 0xea, 0x3e, (byte) 0xe4, 0x77, (byte) 0xad, 0x77, (byte) 0xec}), 2121);

        context.checking(new Expectations() {
            {

                oneOf(sessionEx).getService(); will(returnValue(service));
                oneOf(sessionEx).getRemoteAddress(); will(returnValue(address));
                oneOf(sessionEx).getLocalAddress(); will(returnValue(address));
            }
        });
        assertEquals("fe80:0:0:0:90ea:3ee4:77ad:77ec:2121", LoggingUtils.getUserIdentifier(sessionEx));
    }

    @Test
    public void getUserIdentifierShouldReturnScopedIpv6AddressForIoAcceptor() throws Exception {

        final IoAcceptorEx  service = context.mock(IoAcceptorEx .class, "service");

        final InetSocketAddress address = new InetSocketAddress(Inet6Address.getByAddress(
                null,
                new byte[]{(byte) 0xfe, (byte) 0x80, 0, 0, 0, 0, 0, 0, (byte) 0x90, (byte) 0xea, 0x3e,
                           (byte) 0xe4, 0x77, (byte) 0xad, 0x77, (byte) 0xec},
                15),
                2121);

        context.checking(new Expectations() {
            {
                oneOf(sessionEx).getService(); will(returnValue(service));
                oneOf(sessionEx).getRemoteAddress(); will(returnValue(address));
                oneOf(sessionEx).getLocalAddress(); will(returnValue(address));
            }
        });
        assertEquals("fe80:0:0:0:90ea:3ee4:77ad:77ec%15:2121", LoggingUtils.getUserIdentifier(sessionEx));
    }

    @Test
    public void getUserIdentifierShouldReturnRemoteAddressForBridgeAcceptor() throws Exception {
        context.setImposteriser(ClassImposteriser.INSTANCE);

        final AbstractBridgeAcceptor<?, ?> service = context.mock(AbstractBridgeAcceptor.class, "service");

        final InetSocketAddress address = new InetSocketAddress(InetAddress.getByAddress(new byte[]{127,0,0,1}), 2121);

        context.checking(new Expectations() {
            {
                oneOf(sessionEx).getService(); will(returnValue(service));
                oneOf(sessionEx).getRemoteAddress(); will(returnValue(address));
                oneOf(sessionEx).getLocalAddress(); will(returnValue(address));
            }
        });
        assertEquals("127.0.0.1:2121", LoggingUtils.getUserIdentifier(sessionEx));
    }

    @Test
    public void getUserIdentifierShouldReturnTcpEndpointFromTransport() throws Exception {

        ResourceAddressFactory addressFactory = ResourceAddressFactory.newResourceAddressFactory();
        String transportURI = "tcp://localhost:2121";
        final ResourceAddress transport = addressFactory.newResourceAddress(transportURI);

        final IoAcceptorEx  service = context.mock(IoAcceptorEx .class, "service");
        final Subject subject = new Subject();

        context.checking(new Expectations() {
            {
                oneOf(sessionEx).getService(); will(returnValue(service));
                oneOf(sessionEx).getLocalAddress(); will(returnValue(transport));
                oneOf(sessionEx).getSubject(); will(returnValue(subject));
                oneOf(sessionEx).getRemoteAddress(); will(returnValue(transport));
            }
        });
        assertEquals("127.0.0.1:2121", LoggingUtils.getUserIdentifier(sessionEx));
    }

    @Test
    public void getUserIdentifierShouldReturnTcpEndpointFromHttpAddress() throws Exception {
        ResourceAddressFactory addressFactory = ResourceAddressFactory.newResourceAddressFactory();
        String addressURI = "http://localhost:2121/jms";
        final ResourceAddress address = addressFactory.newResourceAddress(addressURI);
        final IoServiceEx service = context.mock(IoServiceEx.class, "service");
        final Subject subject = new Subject();
        context.checking(new Expectations() {
            {
                oneOf(sessionEx).getService(); will(returnValue(service));
                oneOf(sessionEx).getLocalAddress(); will(returnValue(address));
                oneOf(sessionEx).getSubject(); will(returnValue(subject));
                oneOf(sessionEx).getRemoteAddress(); will(returnValue(address));
            }
        });

        assertEquals("127.0.0.1:2121", LoggingUtils.getUserIdentifier(sessionEx));
    }

    @Test
    public void getUserIdentifierShouldReturnPrincipalFromHttpAddressIdentifierSet() throws Exception {
        ResourceAddressFactory addressFactory = ResourceAddressFactory.newResourceAddressFactory();
        String addressURI = "http://localhost:2121/jms";
        ResourceOptions options = ResourceOptions.FACTORY.newResourceOptions();
        buildIdentityResolverOption(options);

        final ResourceAddress address = addressFactory.newResourceAddress(addressURI, options);
        final IoServiceEx service = context.mock(IoServiceEx.class, "service");
        final Subject subject = buildSubject();
        context.checking(new Expectations() {
            {
                oneOf(sessionEx).getService(); will(returnValue(service));
                oneOf(sessionEx).getLocalAddress(); will(returnValue(address));
                oneOf(sessionEx).getSubject(); will(returnValue(subject));
                oneOf(sessionEx).getRemoteAddress(); will(returnValue(address));
            }
        });
        assertEquals("test 127.0.0.1:2121", LoggingUtils.getUserIdentifier(sessionEx));
    }

    @Test
    public void getUserIdentifierShouldReturnTcpEndpointFromWsAddress() throws Exception {
        ResourceAddressFactory addressFactory = ResourceAddressFactory.newResourceAddressFactory();
        String addressURI = "ws://localhost:2121/jms";
        final SocketAddress address = addressFactory.newResourceAddress(addressURI);

        final IoServiceEx service = context.mock(IoServiceEx.class, "service");
        final Subject subject = new Subject();
        context.checking(new Expectations() {
            {
                oneOf(sessionEx).getService(); will(returnValue(service));
                oneOf(sessionEx).getLocalAddress(); will(returnValue(address));
                oneOf(sessionEx).getSubject(); will(returnValue(subject));
                oneOf(sessionEx).getRemoteAddress(); will(returnValue(address));
            }
        });
        assertEquals("127.0.0.1:2121", LoggingUtils.getUserIdentifier(sessionEx));
    }

    @Test
    public void getUserIdentifierShouldReturnPrincipalFromWsAddressIdentifierSet() throws Exception {
        ResourceAddressFactory addressFactory = ResourceAddressFactory.newResourceAddressFactory();
        String addressURI = "ws://localhost:2121/jms";
        ResourceOptions options = ResourceOptions.FACTORY.newResourceOptions();
        buildIdentityResolverOption(options);

        final SocketAddress address = addressFactory.newResourceAddress(addressURI, options);
        final IoServiceEx service = context.mock(IoServiceEx.class, "service");
        final Subject subject = buildSubject();
        context.checking(new Expectations() {
            {
                oneOf(sessionEx).getService(); will(returnValue(service));
                oneOf(sessionEx).getLocalAddress(); will(returnValue(address));
                oneOf(sessionEx).getSubject(); will(returnValue(subject));
                oneOf(sessionEx).getRemoteAddress(); will(returnValue(address));
            }
        });
        assertEquals("test 127.0.0.1:2121", LoggingUtils.getUserIdentifier(sessionEx));
    }

    @Test
    public void logWithErrorLevelLoggerShouldNotLog() throws Exception {
        final Logger logger = context.mock(Logger.class);

        context.checking(new Expectations() {
            {
                oneOf(logger).isWarnEnabled(); will(returnValue(false));
            }
        });
        LoggingUtils.log(sessionEx, logger, new Exception());
    }

    @Test
    public void logWithWarnLevelLoggerShouldNotLogIOException() throws Exception {
        final Logger logger = context.mock(Logger.class);

        context.checking(new Expectations() {
            {
                oneOf(logger).isInfoEnabled(); will(returnValue(false));
            }
        });
        LoggingUtils.log(sessionEx, logger, new IOException());
    }

    @Test
    public void logWithWarnLevelLoggerShouldLogThrowableWithoutStackTrace() throws Exception {
        final TransportMetadata metadata = context.mock(TransportMetadata.class, "metadata");
        final IoServiceEx service = context.mock(IoServiceEx.class, "service");
        ResourceAddressFactory addressFactory = ResourceAddressFactory.newResourceAddressFactory();
        String addressURI = "ws://localhost:2121/jms";
        final SocketAddress address = addressFactory.newResourceAddress(addressURI);
        final Subject subject = new Subject();

        final Logger logger = context.mock(Logger.class);
        Exception e = new Exception("exception message");

        context.checking(new Expectations() {
            {
                oneOf(sessionEx).getAttribute(with(any(Object.class))); will(returnValue(null));
                oneOf(sessionEx).getId(); will(returnValue(TEST_SESSION_NUMBER));
                oneOf(sessionEx).getTransportMetadata(); will(returnValue(metadata));
                oneOf(metadata).getName(); will(returnValue("wsn"));
                oneOf(sessionEx).getService(); will(returnValue(service));
                oneOf(sessionEx).getLocalAddress(); will(returnValue(address));
                oneOf(sessionEx).getSubject(); will(returnValue(subject));
                oneOf(sessionEx).getRemoteAddress(); will(returnValue(address));
                oneOf(sessionEx).setAttribute(with(any(Object.class)), with(any(String.class)));
                oneOf(logger).isWarnEnabled(); will(returnValue(true));
                oneOf(logger).isInfoEnabled(); will(returnValue(false));
                oneOf(logger).warn("[wsn#23 127.0.0.1:2121] java.lang.Exception: exception message");
            }
        });
        LoggingUtils.log(sessionEx, logger, e);
    }

    @Test
    public void logWithInfoLevelLoggerShouldLogThrowableWithStackTrace() throws Exception {
        final TransportMetadata metadata = context.mock(TransportMetadata.class, "metadata");
        final IoServiceEx service = context.mock(IoServiceEx.class, "service");
        ResourceAddressFactory addressFactory = ResourceAddressFactory.newResourceAddressFactory();
        String addressURI = "ws://localhost:2121/jms";
        final SocketAddress address = addressFactory.newResourceAddress(addressURI);
        final Subject subject = new Subject();

        final Logger logger = context.mock(Logger.class);
        Exception e = new Exception("exception message");

        context.checking(new Expectations() {
            {
                oneOf(sessionEx).getAttribute(with(any(Object.class))); will(returnValue(null));
                oneOf(sessionEx).getId(); will(returnValue(TEST_SESSION_NUMBER));
                oneOf(sessionEx).getTransportMetadata(); will(returnValue(metadata));
                oneOf(metadata).getName(); will(returnValue("wsn"));
                oneOf(sessionEx).getService(); will(returnValue(service));
                oneOf(sessionEx).getLocalAddress(); will(returnValue(address));
                oneOf(sessionEx).getSubject(); will(returnValue(subject));
                oneOf(sessionEx).getRemoteAddress(); will(returnValue(address));
                oneOf(sessionEx).setAttribute(with(any(Object.class)), with(any(String.class)));
                oneOf(logger).isWarnEnabled(); will(returnValue(true));
                oneOf(logger).isInfoEnabled(); will(returnValue(true));
                oneOf(logger).warn("[wsn#23 127.0.0.1:2121] java.lang.Exception: exception message", e);
            }
        });
        LoggingUtils.log(sessionEx, logger, e);
    }

    @Test
    public void logWithInfoLevelLoggerShouldLogIOExceptionWithoutStackTrace() throws Exception {
        final TransportMetadata metadata = context.mock(TransportMetadata.class, "metadata");
        final IoServiceEx service = context.mock(IoServiceEx.class, "service");
        ResourceAddressFactory addressFactory = ResourceAddressFactory.newResourceAddressFactory();
        String addressURI = "ws://localhost:2121/jms";
        final SocketAddress address = addressFactory.newResourceAddress(addressURI);
        final Subject subject = new Subject();

        final Logger logger = context.mock(Logger.class);
        Exception e = new IOException("exception message");

        context.checking(new Expectations() {
            {
                oneOf(sessionEx).getAttribute(with(any(Object.class))); will(returnValue(null));
                oneOf(sessionEx).getId(); will(returnValue(TEST_SESSION_NUMBER));
                oneOf(sessionEx).getTransportMetadata(); will(returnValue(metadata));
                oneOf(metadata).getName(); will(returnValue("wsn"));
                oneOf(sessionEx).getService(); will(returnValue(service));
                oneOf(sessionEx).getLocalAddress(); will(returnValue(address));
                oneOf(sessionEx).getSubject(); will(returnValue(subject));
                oneOf(sessionEx).getRemoteAddress(); will(returnValue(address));
                oneOf(sessionEx).setAttribute(with(any(Object.class)), with(any(String.class)));
                oneOf(logger).isWarnEnabled(); will(returnValue(true));
                oneOf(logger).isInfoEnabled(); will(returnValue(true));
                oneOf(logger).info("[wsn#23 127.0.0.1:2121] java.io.IOException: exception message");
            }
        });
        LoggingUtils.log(sessionEx, logger, e);
    }

    @Test
    public void logWithInfoLevelLoggerShouldLogIOExceptionWithCauseStackTrace() throws Exception {
        final TransportMetadata metadata = context.mock(TransportMetadata.class, "metadata");
        final IoServiceEx service = context.mock(IoServiceEx.class, "service");
        ResourceAddressFactory addressFactory = ResourceAddressFactory.newResourceAddressFactory();
        String addressURI = "ws://localhost:2121/jms";
        final SocketAddress address = addressFactory.newResourceAddress(addressURI);
        final Subject subject = new Subject();

        final Logger logger = context.mock(Logger.class);
        Exception cause = new Exception("cause message");
        Exception e = new IOException("exception message", cause);

        context.checking(new Expectations() {
            {
                oneOf(sessionEx).getAttribute(with(any(Object.class))); will(returnValue(null));
                oneOf(sessionEx).getId(); will(returnValue(TEST_SESSION_NUMBER));
                oneOf(sessionEx).getTransportMetadata(); will(returnValue(metadata));
                oneOf(metadata).getName(); will(returnValue("wsn"));
                oneOf(sessionEx).getService(); will(returnValue(service));
                oneOf(sessionEx).getLocalAddress(); will(returnValue(address));
                oneOf(sessionEx).getSubject(); will(returnValue(subject));
                oneOf(sessionEx).getRemoteAddress(); will(returnValue(address));
                oneOf(sessionEx).setAttribute(with(any(Object.class)), with(any(String.class)));
                oneOf(logger).isWarnEnabled(); will(returnValue(true));
                oneOf(logger).isInfoEnabled(); will(returnValue(true));
                oneOf(logger).info("[wsn#23 127.0.0.1:2121] java.io.IOException: exception message, "
                        + "caused by java.lang.Exception: cause message", cause);
            }
        });
        LoggingUtils.log(sessionEx, logger, e);
    }

    /**
     * Method building identity resolver option
     * @param options
     */
    private void buildIdentityResolverOption(ResourceOptions options) {
        Collection<Class<? extends Principal>> realmUserPrincipalClasses = new ArrayList<>();
        realmUserPrincipalClasses.add(DefaultUserConfig.class);
        IdentityResolver httpIdentityResolver = new HttpIdentityResolver(realmUserPrincipalClasses );
        options.setOption(IDENTITY_RESOLVER, httpIdentityResolver);
    }

    /**
     * Method building subject set with default principal
     * @return
     */
    private Subject buildSubject() {
        final Subject subject = new Subject();
        DefaultUserConfig defaultPrincipal = new DefaultUserConfig();
        defaultPrincipal.setName("test");
        defaultPrincipal.setPassword("test");
        subject.getPrincipals().add(defaultPrincipal);
        return subject;
    }

}

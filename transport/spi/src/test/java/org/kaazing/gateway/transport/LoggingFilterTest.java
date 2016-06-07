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

import javax.security.auth.Subject;

import org.apache.mina.core.filterchain.IoFilter.NextFilter;
import org.apache.mina.core.filterchain.IoFilterChain;
import org.apache.mina.core.session.IoSession;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.Rule;
import org.junit.Test;
import org.kaazing.gateway.resource.address.IdentityResolver;
import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.resource.address.ResourceAddressFactories;
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

public class LoggingFilterTest {

    @Rule
    public JUnitRuleMockery context = new  JUnitRuleMockery();

    final IoSession session = context.mock(IoSession.class, "session");
    final IoSessionEx sessionEx = context.mock(IoSessionEx.class, "sessionEx");

    final NextFilter nextFilter = context.mock(NextFilter.class);
    final Logger logger = context.mock(Logger.class);

    @Test
    public void shouldLogExceptionWithStack() throws Exception {
        LoggingFilter filter = new ExceptionLoggingFilter(logger, "tcp%s");
        final Exception exception = new NullPointerException();
        exception.fillInStackTrace();

        context.checking(new Expectations() {
            {
                oneOf(logger).isInfoEnabled(); will(returnValue(true));
                oneOf(session).getId(); will(returnValue(123L));
                oneOf(logger).info(with(stringMatching(".*NullPointerException.*")), with(exception));
                oneOf(nextFilter).exceptionCaught(session, exception);
            }
        });

        filter.exceptionCaught(nextFilter, session, exception);
    }

    @Test
    public void shouldLogIOExceptionWithoutStackWhenThereIsNoCause() throws Exception {
        LoggingFilter filter = new ExceptionLoggingFilter(logger, "tcp%s");
        final Exception exception = new IOException();
        exception.fillInStackTrace();

        context.checking(new Expectations() {
            {
                oneOf(logger).isInfoEnabled(); will(returnValue(true));
                oneOf(session).getId(); will(returnValue(123L));
                oneOf(logger).info(with(stringMatching(".*IOException.*")));
                oneOf(nextFilter).exceptionCaught(session, exception);
            }
        });

        filter.exceptionCaught(nextFilter, session, exception);
    }

    @Test
    public void shouldLogIOExceptionWithMessageIncludingCauseAndCauseExceptionStack() throws Exception {
        LoggingFilter filter = new ExceptionLoggingFilter(logger, "tcp%s");
        Exception cause = new Exception("Cause exception");
        final Exception exception = new IOException("Oops", cause);
        exception.fillInStackTrace();

        context.checking(new Expectations() {
            {
                oneOf(logger).isInfoEnabled(); will(returnValue(true));
                oneOf(session).getId(); will(returnValue(123L));
                oneOf(logger).info(with(stringMatching(".*IOException.*Oops.*Cause exception")), with(cause));
                oneOf(nextFilter).exceptionCaught(session, exception);
            }
        });

        filter.exceptionCaught(nextFilter, session, exception);
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
        assertEquals("localhost:2121", LoggingFilter.getUserIdentifier(sessionEx));
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
        assertEquals("127.0.0.1:2121", LoggingFilter.getUserIdentifier(sessionEx));
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
        assertEquals("fe80:0:0:0:90ea:3ee4:77ad:77ec:2121", LoggingFilter.getUserIdentifier(sessionEx));
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
        assertEquals("fe80:0:0:0:90ea:3ee4:77ad:77ec%15:2121", LoggingFilter.getUserIdentifier(sessionEx));
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
        assertEquals("127.0.0.1:2121", LoggingFilter.getUserIdentifier(sessionEx));
    }

    @Test
    public void getUserIdentifierShouldReturnTcpEndpointFromTransport() throws Exception {

        ResourceAddressFactory addressFactory = ResourceAddressFactories.newResourceAddressFactory();
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
        assertEquals("127.0.0.1:2121", LoggingFilter.getUserIdentifier(sessionEx));
    }

    @Test
    public void getUserIdentifierShouldReturnTcpEndpointFromHttpAddress() throws Exception {
        ResourceAddressFactory addressFactory = ResourceAddressFactories.newResourceAddressFactory();
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

        assertEquals("127.0.0.1:2121", LoggingFilter.getUserIdentifier(sessionEx));
    }

    @Test
    public void getUserIdentifierShouldReturnPrincipalFromHttpAddressIdentifierSet() throws Exception {
        ResourceAddressFactory addressFactory = ResourceAddressFactories.newResourceAddressFactory();
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
        assertEquals("test 127.0.0.1:2121", LoggingFilter.getUserIdentifier(sessionEx));
    }

    @Test
    public void getUserIdentifierShouldReturnTcpEndpointFromWsAddress() throws Exception {
        ResourceAddressFactory addressFactory = ResourceAddressFactories.newResourceAddressFactory();
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
        assertEquals("127.0.0.1:2121", LoggingFilter.getUserIdentifier(sessionEx));
    }

    @Test
    public void getUserIdentifierShouldReturnPrincipalFromWsAddressIdentifierSet() throws Exception {
        ResourceAddressFactory addressFactory = ResourceAddressFactories.newResourceAddressFactory();
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
        assertEquals("test 127.0.0.1:2121", LoggingFilter.getUserIdentifier(sessionEx));
    }

    @Test
    public void shouldAddLoggingFilterWhenUserIdIsScopedIpv6Address() throws Exception {

        final IoConnectorEx  connector = context.mock(IoConnectorEx .class, "connector");
        final IoFilterChain filterChain = context.mock(IoFilterChain.class, "filterChain");
        
        final InetSocketAddress address = new InetSocketAddress(Inet6Address.getByAddress(
                null,
                new byte[]{(byte) 0xfe, (byte) 0x80, 0, 0, 0, 0, 0, 0, (byte) 0x90, (byte) 0xea, 0x3e,
                           (byte) 0xe4, 0x77, (byte) 0xad, 0x77, (byte) 0xec},
                15),
                2121);

        context.checking(new Expectations() {
            {
                oneOf(logger).isInfoEnabled(); will(returnValue(true));
                oneOf(logger).isTraceEnabled(); will(returnValue(false));
                oneOf(sessionEx).getService(); will(returnValue(connector));
                oneOf(sessionEx).getLocalAddress(); will(returnValue(address));
                oneOf(sessionEx).getRemoteAddress(); will(returnValue(address));
                oneOf(sessionEx).getFilterChain(); will(returnValue(filterChain));

                oneOf(filterChain).addLast(with("tcp#logging"), with(any(ExceptionLoggingFilter.class)));
            }
        });
        LoggingFilter.addIfNeeded(logger, sessionEx, "tcp");
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

/**
 * Copyright (c) 2007-2014, Kaazing Corporation. All rights reserved.
 */

package org.kaazing.gateway.transport.wsn;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.resource.address.ResourceAddressFactory;
import org.kaazing.gateway.transport.BridgeServiceFactory;
import org.kaazing.gateway.transport.TransportFactory;
import org.kaazing.gateway.transport.http.HttpConnector;
import org.kaazing.gateway.transport.nio.internal.NioSocketAcceptor;
import org.kaazing.gateway.transport.nio.internal.NioSocketConnector;
import org.apache.log4j.PropertyConfigurator;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.service.IoHandler;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.kaazing.gateway.util.scheduler.SchedulerProvider;


/**
 * Declaring an instance of this class as a @Rule causes the gateway to be
 * started in process before each test method and stopped after it. The rule
 * can be chained with a K3poRule for use with robot (this causes Robot to be
 * started before the gateway and stopped after it).
 */
public class WsnConnectorRule implements TestRule {

    private final String log4jPropertiesResourceName;
    private ResourceAddressFactory addressFactory;
    private WsnConnector wsnConnector;


    @Override
    public Statement apply(Statement base, Description description) {
        return new ConnectorStatement(base);
    }

    public WsnConnectorRule() {
        this(null);
    }

    public WsnConnectorRule(String log4jPropertiesResourceName) {
        this.log4jPropertiesResourceName = log4jPropertiesResourceName;
    }

    public ConnectFuture connect(String connect, Long wsInactivityTimeout, IoHandler connectHandler)
            throws InterruptedException {
        Map<String, Object> connectOptions = new HashMap<>();
        if (wsInactivityTimeout != null) {
            connectOptions.put("inactivityTimeout", wsInactivityTimeout);
        }
        final ResourceAddress connectAddress =
                addressFactory.newResourceAddress(URI.create(connect), connectOptions);

        return wsnConnector.connect(connectAddress, connectHandler, null);
    }

    private final class ConnectorStatement extends Statement {

        private final Statement base;

        private NioSocketConnector tcpConnector;
        private NioSocketAcceptor tcpAcceptor;
        private HttpConnector httpConnector;
        private SchedulerProvider schedulerProvider;

        public ConnectorStatement(Statement base) {
            this.base = base;
        }

        @Override
        public void evaluate() throws Throwable {
            if (log4jPropertiesResourceName != null) {
                // Initialize log4j using a properties file available on the class path
                Properties log4j = new Properties();
                InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(log4jPropertiesResourceName);
                if (in == null) {
                    throw new IOException(String.format("Could not load resource %s", log4jPropertiesResourceName));
                }
                log4j.load(in);
                in.close();
                PropertyConfigurator.configure(log4j);
            }
            try {
                // Connector setup
                schedulerProvider = new SchedulerProvider();

                addressFactory = ResourceAddressFactory.newResourceAddressFactory();
                TransportFactory transportFactory = TransportFactory.newTransportFactory(Collections.<String, Object> emptyMap());
                BridgeServiceFactory serviceFactory = new BridgeServiceFactory(transportFactory);

                tcpAcceptor = (NioSocketAcceptor)transportFactory.getTransport("tcp").getAcceptor();
                tcpAcceptor.setResourceAddressFactory(addressFactory);
                tcpAcceptor.setBridgeServiceFactory(serviceFactory);
                tcpAcceptor.setSchedulerProvider(schedulerProvider);

                tcpConnector = (NioSocketConnector)transportFactory.getTransport("tcp").getConnector();
                tcpConnector.setResourceAddressFactory(addressFactory);
                tcpConnector.setBridgeServiceFactory(serviceFactory);
                tcpConnector.setTcpAcceptor(tcpAcceptor);

                httpConnector = (HttpConnector)transportFactory.getTransport("http").getConnector();
                httpConnector.setBridgeServiceFactory(serviceFactory);
                httpConnector.setResourceAddressFactory(addressFactory);

                wsnConnector = (WsnConnector)transportFactory.getTransport("wsn").getConnector();
                wsnConnector.setConfiguration(new Properties());
                wsnConnector.setBridgeServiceFactory(serviceFactory);
                wsnConnector.setSchedulerProvider(schedulerProvider);
                wsnConnector.setResourceAddressFactory(addressFactory);

                base.evaluate();
            } finally {
                tcpConnector.dispose();
                tcpAcceptor.dispose();
                httpConnector.dispose();
                wsnConnector.dispose();
                schedulerProvider.shutdownNow();
            }
        }

    }


}
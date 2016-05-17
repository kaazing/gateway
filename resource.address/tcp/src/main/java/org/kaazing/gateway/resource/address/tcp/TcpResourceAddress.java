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
package org.kaazing.gateway.resource.address.tcp;

import static java.lang.Long.valueOf;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.resource.address.ResourceAddressFactorySpi;
import org.kaazing.gateway.resource.address.ResourceOption;

public final class TcpResourceAddress extends ResourceAddress {

    private static final long serialVersionUID = 1L;

    static final String TRANSPORT_NAME = "tcp";

    public static final ResourceOption<InetSocketAddress> BIND_ADDRESS = new TcpBindAddressOption();
    public static final ResourceOption<Long> MAXIMUM_OUTBOUND_RATE = new TcpMaximumOutboundRateOption();
    public static final ResourceOption<Long> HANDSHAKE_TIMEOUT = new HandshakeTimeoutOption();

    private static final long MAXIMUM_OUTBOUND_RATE_DEFAULT = 0xFFFFFFFFL;
    private static final long HANDSHAKE_TIMEOUT_MILLIS_DEFAULT = 10000;

    private InetSocketAddress bindAddress;
    private long maximumOutboundRate = MAXIMUM_OUTBOUND_RATE.defaultValue();
    private Long handshakeTimeout = HANDSHAKE_TIMEOUT.defaultValue();

    TcpResourceAddress(ResourceAddressFactorySpi factory, String original, URI resource) {
        super(factory, original, resource);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected <V> V getOption0(ResourceOption<V> option) {
        if (option instanceof TcpResourceOption) {
            TcpResourceOption tcpOption = (TcpResourceOption) option;
            switch (tcpOption.kind) {
                case BIND_ADDRESS:
                    return (V) bindAddress;
                case MAXIMUM_OUTBOUND_RATE:
                    return (V) valueOf(maximumOutboundRate);
                case HANDSHAKE_TIMEOUT:
                    return (V) valueOf(handshakeTimeout);
            }
        }

        return super.getOption0(option);
    }

    @Override
    protected <V> void setOption0(ResourceOption<V> option, V value) {
        if (option instanceof TcpResourceOption) {
            TcpResourceOption tcpOption = (TcpResourceOption) option;
            switch (tcpOption.kind) {
                case BIND_ADDRESS:
                    bindAddress = (InetSocketAddress) value;
                    return;
                case MAXIMUM_OUTBOUND_RATE:
                    maximumOutboundRate = (Long) value;
                    return;
                case HANDSHAKE_TIMEOUT:
                    handshakeTimeout = (Long) value;
                    return;
            }
        }

        super.setOption0(option, value);
    }

    static class TcpResourceOption<T> extends ResourceOption<T> {

        enum Kind {
            BIND_ADDRESS, MAXIMUM_OUTBOUND_RATE, HANDSHAKE_TIMEOUT
        }

        static final Map<String, ResourceOption<?>> OPTION_NAMES = new HashMap<>();

        private final Kind kind;

        private TcpResourceOption(Kind kind, String name) {
            this(kind, name, null);
        }

        private TcpResourceOption(Kind kind, String name, T defaultValue) {
            super(OPTION_NAMES, name, defaultValue);
            this.kind = kind;
        }
    }

    private static final class TcpBindAddressOption extends TcpResourceOption<InetSocketAddress> {
        private TcpBindAddressOption() {
            super(Kind.BIND_ADDRESS, "bind");
        }
    }

    private static final class TcpMaximumOutboundRateOption extends TcpResourceOption<Long> {
        private TcpMaximumOutboundRateOption() {
            super(Kind.MAXIMUM_OUTBOUND_RATE, "maximumOutboundRate", MAXIMUM_OUTBOUND_RATE_DEFAULT);
        }
    }

    private static final class HandshakeTimeoutOption extends TcpResourceOption<Long> {
        private HandshakeTimeoutOption() {
            super(Kind.HANDSHAKE_TIMEOUT, "handshake.timeout", HANDSHAKE_TIMEOUT_MILLIS_DEFAULT);
        }
    }

}

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
package org.kaazing.gateway.resource.address.ws;

import static java.lang.Integer.valueOf;
import static java.lang.Long.valueOf;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.resource.address.ResourceAddressFactorySpi;
import org.kaazing.gateway.resource.address.ResourceOption;

public class WsResourceAddress extends ResourceAddress {

    private static final long serialVersionUID = 1L;
    
    static final String TRANSPORT_NAME = "ws";
    
    private static final int MAX_MESSAGE_SIZE_DEFAULT = 0;
    public static final long INACTIVITY_TIMEOUT_DEFAULT =  0L;
    private static final String[] SUPPORTED_PROTOCOLS_DEFAULT = new String[0];
    private static final String[] REQUIRED_PROTOCOLS_DEFAULT = new String[0];

    public static final ResourceOption<Boolean> CODEC_REQUIRED = new WsCodecRequiredOption();
    public static final ResourceOption<Boolean> LIGHTWEIGHT = new WsLightweightOption();
    public static final ResourceOption<Integer> MAX_MESSAGE_SIZE = new WsMaxMessageSizeOption();
    public static final ResourceOption<Long> INACTIVITY_TIMEOUT = new WsInactivityTimeoutOption();
    public static final ResourceOption<String[]> SUPPORTED_PROTOCOLS = new WsSupportedProtocolsOption();
    public static final ResourceOption<String[]> REQUIRED_PROTOCOLS = new WsRequiredProtocolsOption();

    private Boolean codecRequired;
    private Boolean lightweight;
    private int maxMessageSize = MAX_MESSAGE_SIZE.defaultValue();
    private long inactivityTimeout = INACTIVITY_TIMEOUT.defaultValue();
    private String[] supportedProtocols;
    private String[] requiredProtocols = REQUIRED_PROTOCOLS.defaultValue();

    WsResourceAddress(ResourceAddressFactorySpi factory, String original, URI resource) {
        super(factory, original, resource);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected <V> V getOption0(ResourceOption<V> option) {
        if (option instanceof WsResourceOption) {
            WsResourceOption wsOption = (WsResourceOption)option;
            switch (wsOption.kind) {
                case CODEC_REQUIRED:
                    return (V) codecRequired;
                case LIGHTWEIGHT:
                    return (V) lightweight;
                case MAX_MESSAGE_SIZE:
                    return (V) valueOf(maxMessageSize);
                case INACTIVITY_TIMEOUT:
                    return (V) valueOf(inactivityTimeout);
                case SUPPORTED_PROTOCOLS:
                    return (V) supportedProtocols;
                case REQUIRED_PROTOCOLS:
                    return (V) requiredProtocols;
            }
        }
        
        return super.getOption0(option);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected <V> void setOption0(ResourceOption<V> option, V value) {
        if (option instanceof WsResourceOption) {
            WsResourceOption wsOption = (WsResourceOption) option;
            switch (wsOption.kind) {
                case CODEC_REQUIRED:
                    codecRequired = (Boolean) value;
                    return;
                case LIGHTWEIGHT:
                    lightweight = (Boolean) value;
                    return;
                case MAX_MESSAGE_SIZE:
                    maxMessageSize = (Integer) value;
                    return;
                case INACTIVITY_TIMEOUT:
                    inactivityTimeout = (Long) value;
                    return;
                case SUPPORTED_PROTOCOLS:
                    supportedProtocols = (String[]) value;
                    return;
                case REQUIRED_PROTOCOLS:
                    requiredProtocols = (String[]) value;
                    return;
            }
        }

        super.setOption0(option, value);
    }

    static class WsResourceOption<T> extends ResourceOption<T> {

        protected enum Kind { CODEC_REQUIRED, LIGHTWEIGHT, MAX_MESSAGE_SIZE,
                                     INACTIVITY_TIMEOUT, SUPPORTED_PROTOCOLS,
                                     REQUIRED_PROTOCOLS }
        
        private static final Map<String, ResourceOption<?>> OPTION_NAMES = new HashMap<>();

        private final Kind kind;
        
        private WsResourceOption(Kind kind, String name) {
            this(kind, name, null);
        }
        
        private WsResourceOption(Kind kind, String name, T defaultValue) {
            super(OPTION_NAMES, name, defaultValue);
            this.kind = kind;
        }
    }
    
    private static final class WsCodecRequiredOption extends WsResourceOption<Boolean> {
        private WsCodecRequiredOption() {
            super(Kind.CODEC_REQUIRED, "codecRequired", Boolean.TRUE);
        }
    }
    
    private static final class WsLightweightOption extends WsResourceOption<Boolean> {
        private WsLightweightOption() {
            super(Kind.LIGHTWEIGHT, "lightweight", Boolean.FALSE);
        }
    }
    
    private static final class WsMaxMessageSizeOption extends WsResourceOption<Integer> {
        private WsMaxMessageSizeOption() {
            super(Kind.MAX_MESSAGE_SIZE, "maxMessageSize", MAX_MESSAGE_SIZE_DEFAULT);
        }
    }
    
    private static final class WsInactivityTimeoutOption extends WsResourceOption<Long> {
        private WsInactivityTimeoutOption() {
            super(Kind.INACTIVITY_TIMEOUT, "inactivityTimeout", INACTIVITY_TIMEOUT_DEFAULT);
        }
    }
    
    private static final class WsSupportedProtocolsOption extends WsResourceOption<String[]> {
        private WsSupportedProtocolsOption() {
            super(Kind.SUPPORTED_PROTOCOLS, "supportedProtocols", SUPPORTED_PROTOCOLS_DEFAULT);
        }
    }
    
    private static final class WsRequiredProtocolsOption extends WsResourceOption<String[]> {
        private WsRequiredProtocolsOption() {
            super(Kind.REQUIRED_PROTOCOLS, "requiredProtocols", REQUIRED_PROTOCOLS_DEFAULT);
        }
    }

}

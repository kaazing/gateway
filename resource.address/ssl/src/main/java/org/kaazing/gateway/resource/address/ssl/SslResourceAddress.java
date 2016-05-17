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
package org.kaazing.gateway.resource.address.ssl;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.lang.Boolean.valueOf;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.resource.address.ResourceAddressFactorySpi;
import org.kaazing.gateway.resource.address.ResourceOption;
import org.kaazing.gateway.security.KeySelector;

public final class SslResourceAddress extends ResourceAddress {
	
	private static final long serialVersionUID = 1L;

	static final String TRANSPORT_NAME = "ssl";

    public static final ResourceOption<String[]> CIPHERS = new SslCiphersOption();
    public static final ResourceOption<Boolean> ENCRYPTION_ENABLED = new SslEncryptionOption();
    public static final ResourceOption<Boolean> WANT_CLIENT_AUTH = new SslWantClientAuthOption();
    public static final ResourceOption<Boolean> NEED_CLIENT_AUTH = new SslNeedClientAuthOption();
    public static final ResourceOption<KeySelector> KEY_SELECTOR = new SslKeySelectorOption();
    public static final ResourceOption<String[]> PROTOCOLS = new SslProtocolsOption();

    private String[] ciphers;
    private String[] protocols;
    private boolean encryption = true;  // default
    private boolean wantClientAuth;
    private boolean needClientAuth;
    private KeySelector keySelector;

	SslResourceAddress(ResourceAddressFactorySpi factory, String original, URI resource) {
		super(factory, original, resource);
	}

    @Override
	@SuppressWarnings("unchecked")
    protected <V> V getOption0(ResourceOption<V> option) {
        if (option instanceof SslResourceOption) {
            SslResourceOption sslOption = (SslResourceOption)option;
            switch (sslOption.kind) {
                case CIPHERS:
                    return (V) ciphers;
                case ENCRYPTION:
                    return (V) valueOf(encryption);
                case WANT_CLIENT_AUTH:
                    return (V) valueOf(wantClientAuth);
                case NEED_CLIENT_AUTH:
                    return (V) valueOf(needClientAuth);
                case KEY_SELECTOR:
                    return (V) keySelector;
                case PROTOCOLS:
                    return (V) protocols;
            }
        }
		
		return super.getOption0(option);
	}

    @Override
	protected <V> void setOption0(ResourceOption<V> option, V value) {
        if (option instanceof SslResourceOption) {
            SslResourceOption sslOption = (SslResourceOption)option;
            switch (sslOption.kind) {
                case CIPHERS:
                    ciphers = (String[]) value;
                    return;
                case ENCRYPTION:
                    encryption = (Boolean) value;
                    return;
                case WANT_CLIENT_AUTH:
                    wantClientAuth = (Boolean) value;
                    return;
                case NEED_CLIENT_AUTH:
                    needClientAuth = (Boolean) value;
                    return;
                case KEY_SELECTOR:
                    keySelector = (KeySelector) value;
                    return;
                case PROTOCOLS:
                    protocols = (String[]) value;
                    return;
            }
        }

        super.setOption0(option, value);
	}
	
	static class SslResourceOption<T> extends ResourceOption<T> {

	    protected enum Kind { CIPHERS, ENCRYPTION, WANT_CLIENT_AUTH,
                                    NEED_CLIENT_AUTH, KEY_SELECTOR, PROTOCOLS }
		
		private static final Map<String, ResourceOption<?>> OPTION_NAMES = new HashMap<>();

		private final Kind kind;
		
		private SslResourceOption(Kind kind, String name) {
            this(kind, name, null);
		}
		
        private SslResourceOption(Kind kind, String name, T defaultValue) {
			super(OPTION_NAMES, name, defaultValue);
			this.kind = kind;
		}
	}
	
    private static final class SslCiphersOption extends SslResourceOption<String[]> {
        private SslCiphersOption() {
            super(Kind.CIPHERS, "ciphers", new String[] { "DEFAULT" });
        }
    }
    
    private static final class SslEncryptionOption extends SslResourceOption<Boolean> {
        private SslEncryptionOption() {
            super(Kind.ENCRYPTION, "encryptionEnabled", TRUE);
        }
    }
    
    private static final class SslWantClientAuthOption extends SslResourceOption<Boolean> {
        private SslWantClientAuthOption() {
            super(Kind.WANT_CLIENT_AUTH, "wantClientAuth", FALSE);
        }
    }
    
    private static final class SslNeedClientAuthOption extends SslResourceOption<Boolean> {
        private SslNeedClientAuthOption() {
            super(Kind.NEED_CLIENT_AUTH, "needClientAuth", FALSE);
        }
    }
    
    private static final class SslKeySelectorOption extends SslResourceOption<KeySelector> {
        private SslKeySelectorOption() {
            super(Kind.KEY_SELECTOR, "keySelector");
        }
    }

    private static final class SslProtocolsOption extends SslResourceOption<String[]> {
        private SslProtocolsOption() {
            super(Kind.PROTOCOLS, "protocols");
        }
    }
}

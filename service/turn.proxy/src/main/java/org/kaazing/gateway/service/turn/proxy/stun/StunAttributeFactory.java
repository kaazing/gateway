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
package org.kaazing.gateway.service.turn.proxy.stun;

import org.kaazing.gateway.service.turn.proxy.stun.attributes.Attribute;
import org.kaazing.gateway.service.turn.proxy.stun.attributes.AttributeType;
import org.kaazing.gateway.service.turn.proxy.stun.attributes.Fingerprint;
import org.kaazing.gateway.service.turn.proxy.stun.attributes.MappedAddress;
import org.kaazing.gateway.service.turn.proxy.stun.attributes.MessageIntegrity;
import org.kaazing.gateway.service.turn.proxy.stun.attributes.ProxyNoopAttribute;
import org.kaazing.gateway.service.turn.proxy.stun.attributes.Username;
import org.kaazing.gateway.service.turn.proxy.stun.attributes.XorMappedAddress;
import org.kaazing.gateway.service.turn.proxy.stun.attributes.XorPeerAddress;
import org.kaazing.gateway.service.turn.proxy.stun.attributes.XorRelayAddress;

public class StunAttributeFactory {

    public enum CredentialType {
        SHORT_TERM, LONG_TERM
    }

    private final CredentialType credentialType;

    public StunAttributeFactory(CredentialType credentialType) {
        this.credentialType = credentialType;
    }

    public Attribute get(int type, short length, byte[] value, byte[] transactionId) {
        switch (AttributeType.valueOf(type)) {
            case USERNAME:
                return new Username(value);
            case MAPPED_ADDRESS:
                return new MappedAddress(value);
            case XOR_MAPPED_ADDRESS:
                return new XorMappedAddress(value, transactionId);
            case XOR_PEER_ADDRESS:
                return new XorPeerAddress(value, transactionId);
            case XOR_RELAY_ADDRESS:
                return new XorRelayAddress(value, transactionId);
            case MESSAGE_INTEGRITY:
                return new MessageIntegrity(value, credentialType);
            case FINGERPRINT:
                return new Fingerprint(value);
            default:
                // TODO: consider hard failing if white list of attributes is not allowed
                return new ProxyNoopAttribute((short) type, length, value);
        }
    }
}

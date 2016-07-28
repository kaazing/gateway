package org.kaazing.gateway.service.turn.proxy.stun;

import static org.kaazing.gateway.service.turn.proxy.stun.StunAttributeFactory.CredentialType.SHORT_TERM;

import org.kaazing.gateway.service.turn.proxy.stun.attributes.Attribute;
import org.kaazing.gateway.service.turn.proxy.stun.attributes.AttributeType;
import org.kaazing.gateway.service.turn.proxy.stun.attributes.EvenPort;
import org.kaazing.gateway.service.turn.proxy.stun.attributes.Fingerprint;
import org.kaazing.gateway.service.turn.proxy.stun.attributes.InvalidAttributeException;
import org.kaazing.gateway.service.turn.proxy.stun.attributes.MappedAddress;
import org.kaazing.gateway.service.turn.proxy.stun.attributes.MessageIntegrity;
import org.kaazing.gateway.service.turn.proxy.stun.attributes.ReservationToken;
import org.kaazing.gateway.service.turn.proxy.stun.attributes.XorMappedAddress;
import org.kaazing.gateway.service.turn.proxy.stun.attributes.XorPeerAddress;
import org.kaazing.gateway.service.turn.proxy.stun.attributes.XorRelayAddress;

public class StunAttributeFactory {

    public enum CredentialType {
        SHORT_TERM, LONG_TERM;
    }

    private final CredentialType credentialType;

    public StunAttributeFactory(CredentialType credentialType) {
        super();
        this.credentialType = credentialType;
    }

    public Attribute get(int type, short length, byte[] value) {
        switch (AttributeType.valueOf(type)) {
        case MAPPED_ADDRESS:
            return new MappedAddress(value);
        case XOR_MAPPED_ADDRESS:
            return new XorMappedAddress(value);
        case XOR_PEER_ADDRESS:
            return new XorPeerAddress(value);
        case XOR_RELAY_ADDRESS:
            return new XorRelayAddress(value);
        case EVEN_PORT:
            return new EvenPort(value);
        case RESERVATION_TOKEN:
            return new ReservationToken(value);
        case MESSAGE_INTEGRITY:
            if (credentialType == SHORT_TERM) {
                return new MessageIntegrity(value);
            } else {
                throw new InvalidAttributeException("Longterm credential message integrity is not implemented");
            }
        case FINGERPRINT:
            return new Fingerprint(value);
        default:
            return new Attribute() {
                @Override
                public short getType() {
                    return (short) type;
                }

                @Override
                public short getLength() {
                    return length;
                }

                @Override
                public byte[] getVariable() {
                    return value;
                }
            };
        }
    }
}

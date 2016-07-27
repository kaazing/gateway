package org.kaazing.gateway.service.turn.proxy.stun.attributes;

public abstract class Attribute {

    public static class Factory {

        private Factory() {
            super();
        }

        public static Attribute get(int type, short length, byte[] value) {
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
                return new MessageIntegrity(value);
            default:
                return new Noop((short) type, length, value);
            }
        }
    }

    public abstract short getType();

    @Override
    public String toString() {
        int type = getType();
        return String.format("%s", AttributeType.valueOf(type));
    }

    public abstract short getLength();

    public abstract byte[] getVariable();

}

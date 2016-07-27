package org.kaazing.gateway.service.turn.proxy.stun;

public abstract class StunMessageAttribute {

    public static class Factory {

        private Factory() {
            super();
        }

        public static StunMessageAttribute get(int type, short length, byte[] value) {
            switch (StunMessageAttributeType.valueOf(type)) {
            case MAPPED_ADDRESS:
                return new MappedAddressAttribute(value);
            default:
                return new NoopAttribute((short) type, length, value);
            }
        }
    }

    public abstract short getType();

    @Override
    public String toString() {
        int type = getType();
        return String.format("%s", StunMessageAttributeType.valueOf(type));
    }

    public abstract short getLength();

    public abstract byte[] getVariable();

}

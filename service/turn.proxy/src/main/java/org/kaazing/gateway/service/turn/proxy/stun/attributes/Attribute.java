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

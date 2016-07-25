package org.kaazing.gateway.service.turn.proxy.stun;

import static org.kaazing.gateway.service.turn.proxy.stun.StunMessageAttribute.Type.MAPPED_ADDRESS;

public abstract class StunMessageAttribute {

    public enum Type {
        // @formatter:off
        UNKNOWN("UNKNOWN",                          (short) -0x001),
        RESERVED0("Reserved",                       (short) 0x0000),
        MAPPED_ADDRESS("MAPPED-ADDRESS",            (short) 0x0001),
        RESERVED1("was RESPONSE-ADDRESS",           (short) 0x0002),
        RESERVED2("was CHANGE-ADDRESS",             (short) 0x0003),
        RESERVED3("was SOURCE-ADDRESS",             (short) 0x0004),
        RESERVED5("was CHANGED-ADDRESS",            (short) 0x0005),
        USERNAME("USERNAME",                        (short) 0x0006),
        RESERVED6("was PASSWORD",                   (short) 0x0007),
        MESSAGE_INTEGRITY("MESSAGE-INTEGRITY",      (short) 0x0008),
        ERROR_CODE("ERROR-CODE",                    (short) 0x0009),
        UNKNOWN_ATTRIBUTES("UNKNOWN-ATTRIBUTES",    (short) 0x000A),
        RESERVED7("was REFLECTED-FROM",             (short) 0x000B),
        REALM("REALM",                              (short) 0x0014),
        NONCE("NONCE",                              (short) 0x0015),
        XOR_MAPPED_ADDRESS("XOR-MAPPED-ADDRESS",    (short) 0x0020),
        SOFTWARE("SOFTWARE",                        (short) 0x8022),
        ALTERNATE_SERVER("ALTERNATE-SERVER",        (short) 0x8023),
        FINGERPRINT("FINGERPRINT",                  (short) 0x8028);
        // @formatter:on

        private final String name;
        private short hexCode;

        Type(String name, short type) {
            this.name = name;
            this.hexCode = type;
        }

        public String getName() {
            return name;
        }

        public short getType() {
            return hexCode;
        }

        public static Type valueOf(int type) {
            switch (type) {
            case 0x0000:
                return RESERVED0;
            case 0x0001:
                return MAPPED_ADDRESS;
            case 0x0002:
                return RESERVED1;
            case 0x0003:
                return RESERVED2;
            case 0x0004:
                return RESERVED3;
            case 0x0005:
                return RESERVED5;
            case 0x0006:
                return USERNAME;
            case 0x0007:
                return RESERVED6;
            case 0x0008:
                return MESSAGE_INTEGRITY;
            case 0x0009:
                return ERROR_CODE;
            case 0x000A:
                return UNKNOWN_ATTRIBUTES;
            case 0x000B:
                return RESERVED7;
            case 0x0014:
                return REALM;
            case 0x0015:
                return NONCE;
            case 0x0020:
                return XOR_MAPPED_ADDRESS;
            case 0x8022:
                return SOFTWARE;
            case 0x8023:
                return ALTERNATE_SERVER;
            case 0x8028:
                return FINGERPRINT;
            default:
                Type result = Type.UNKNOWN;
                result.setType((short) type);
                return Type.UNKNOWN;
            }
        }

        private void setType(short type) {
            this.hexCode = type;
        }
    }

    public static class Factory {

        private Factory() {
            super();
        }

        public static StunMessageAttribute get(int type, short length, byte[] variable) {
            switch (Type.valueOf(type)) {
            case MAPPED_ADDRESS:
                return new MappedAddressAttribute(variable);
            default:
                return new NoopAttribute((short) type, length, variable);
            }
        }
    }

    public abstract short getType();

    @Override
    public String toString() {
        int type = getType();
        return String.format("%s", Type.valueOf(type));
    }

    public abstract short getLength();

    public abstract byte[] getVariable();

}

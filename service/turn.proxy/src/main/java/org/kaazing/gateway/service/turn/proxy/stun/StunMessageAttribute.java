package org.kaazing.gateway.service.turn.proxy.stun;

public interface StunMessageAttribute {

    public enum Type{
        RESERVED0("Reserved", 0x0000),
        MAPPED_ADDRESS("MAPPED-ADDRESS", 0x0001),
        RESERVED1("was RESPONSE-ADDRESS", 0x0002),
        RESERVED2("was CHANGE-ADDRESS", 0x0003),
        RESERVED3("was SOURCE-ADDRESS", 0x0004),
        RESERVED5("was CHANGED-ADDRESS", 0x0005),
        USERNAME("USERNAME", 0x0006),
        RESERVED6("was PASSWORD", 0x0007),
        MESSAGE_INTEGRITY("MESSAGE-INTEGRITY", 0x0008),
        ERROR_CODE("ERROR-CODE", 0x0009),
        UNKNOWN_ATTRIBUTES("UNKNOWN-ATTRIBUTES", 0x000A),
        RESERVED7("was REFLECTED-FROM", 0x000B),
        REALM("REALM", 0x0014),
        NONCE("NONCE", 0x0015),
        XOR_MAPPED_ADDRESS("XOR-MAPPED-ADDRESS", 0x0020),
        SOFTWARE("SOFTWARE", 0x8022),
        ALTERNATE_SERVER("ALTERNATE-SERVER", 0x8023),
        FINGERPRINT("FINGERPRINT", 0x8028);

        private final String name;
        private final int encodedType;

        Type(String name, int type){
            this.name = name;
            this.encodedType = type;
        }

        public String getName() {
            return name;
        }

        public int getType() {
            return encodedType;
        }
    }

    public static class Factory{

        private Factory(){
            super();
        }

        public static StunMessageAttribute get(short type, short length, byte[] variable) {
            switch(type){
                default:
                    return new KaazingNoopAttribute(type, length, variable);
            }
        }
    }

    public abstract short getType();

    public abstract short getLength();

}

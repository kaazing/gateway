package org.kaazing.gateway.service.turn.proxy.stun;

public enum StunMessageClass {
    //@formatter:off
    REQUEST((short) 0x000),
    INDICATION((short) 0x010),
    RESPONSE((short) 0x100),
    ERROR((short) 0x110);
    //@formatter:on

    private static final short SIGNIFICANT_BITS = 0x110;
    private final short value;

    StunMessageClass(short value) {
        this.value = value;
    }

    public static StunMessageClass valueOf(short leadingBitesAndMessageType) {
        switch (leadingBitesAndMessageType & SIGNIFICANT_BITS) {
        case 0x000:
            return REQUEST;
        case 0x010:
            return INDICATION;
        case 0x100:
            return RESPONSE;
        case 0x110:
            return ERROR;
        default:
            throw new IllegalStateException("No such STUN class from: " + leadingBitesAndMessageType);
        }
    }

    public short getValue() {
        return value;
    }
}

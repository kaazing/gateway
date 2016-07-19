package org.kaazing.gateway.service.turn.proxy.stun;

enum StunMessageMethod {
    //@formatter:off
    RESERVED            ((short) 0x000),
    BINDING             ((short) 0x001),
    RESERVED2           ((short) 0x002),
    ALLOCATE            ((short) 0x003),
    REFRESH             ((short) 0x004),
    SEND                ((short) 0x006),
    DATA                ((short) 0x007),
    CREATE_PERMISSION   ((short) 0x008),
    CHANNEL_BIND        ((short) 0x009);
    //@formatter:on

    private static final short SIGNIFICANT_BITS = 0xEEF;
    private final short value;

    StunMessageMethod(short value) {
        this.value = value;
    }

    public static StunMessageMethod valueOf(short leadingBitesAndMessageType) {
        int method = leadingBitesAndMessageType & SIGNIFICANT_BITS;
        switch (method) {
        case 0x000:
            return RESERVED;
        case 0x001:
            return BINDING;
        case 0x002:
            return RESERVED2;
        case 0x003:
            return ALLOCATE;
        case 0x004:
            return REFRESH;
        case 0x006:
            return SEND;
        case 0x007:
            return DATA;
        case 0x008:
            return CREATE_PERMISSION;
        case 0x009:
            return CHANNEL_BIND;
        default:
            throw new IllegalStateException("No such STUN method from: " + method);
        }
    }
    
    short getValue(){
        return value;
    }
}

package org.kaazing.gateway.service.turn.proxy.stun.attributes;

public class EvenPort extends Attribute {

    protected boolean nextIp;

    public EvenPort(byte[] value) {
        nextIp = value[0] == ((byte) 0x80);
    }

    @Override
    public short getType() {
        return AttributeType.EVEN_PORT.getType();
    }

    @Override
    public short getLength() {
        return 4;
    }

    @Override
    public byte[] getVariable() {
        return (nextIp) ? new byte[] {(byte)0x80, 0x00, 0x00, 0x00} :
            new byte[] {0x00, 0x00, 0x00, 0x00};
    }
    
    public void setEvenPort(boolean flag) {
        nextIp = flag;
    }

}

package org.kaazing.gateway.service.turn.proxy.stun.attributes;

public class MessageIntegrity extends Attribute {

    private String username;
    private String realm;
    private String password;
    protected byte[] value;

    public MessageIntegrity(byte[] key) {
        this.value = key;
        // TODO: decoding of the key
        username = "user";
        realm = "realm";
        password = "pass";
    }

    @Override
    public short getType() {
        return AttributeType.MESSAGE_INTEGRITY.getType();
    }

    @Override
    public short getLength() {
        return 16;
    }

    @Override
    public byte[] getVariable() {
        return value;
    }
    
    public String getUsername() {
        return username;
    }
    
    public String getRealm() {
        return realm;
    }
    
    public String getPassword() {
        return password;
    }

}

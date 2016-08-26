/**
 * Copyright 2007-2016, Kaazing Corporation. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.kaazing.gateway.service.turn.proxy.stun.attributes;

/**
 * Attribute types as defined in https://tools.ietf.org/html/rfc5389#section-18.2.
 *
 */
public enum AttributeType {
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
    XOR_PEER_ADDRESS("XOR-PEER-ADDRESS",        (short) 0x0012),
    REALM("REALM",                              (short) 0x0014),
    NONCE("NONCE",                              (short) 0x0015),
    XOR_RELAY_ADDRESS("XOR-RELAYED-ADDRESS",    (short) 0x0016),
    EVEN_PORT("EVEN-PORT",                      (short) 0x0018),
    XOR_MAPPED_ADDRESS("XOR-MAPPED-ADDRESS",    (short) 0x0020),
    RESERVATION_TOKEN("RESERVATION-TOKEN",      (short) 0x0022),
    SOFTWARE("SOFTWARE",                        (short) 0x8022),
    ALTERNATE_SERVER("ALTERNATE-SERVER",        (short) 0x8023),
    FINGERPRINT("FINGERPRINT",                  (short) 0x8028);
    // @formatter:on

    private final String name;
    private short hexCode;

    AttributeType(String name, short type) {
        this.name = name;
        this.hexCode = type;
    }

    public String getName() {
        return name;
    }

    public short getType() {
        return hexCode;
    }

    public static AttributeType valueOf(int type) {
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
        case 0x0012:
            return XOR_PEER_ADDRESS;
        case 0x000A:
            return UNKNOWN_ATTRIBUTES;
        case 0x000B:
            return RESERVED7;
        case 0x0014:
            return REALM;
        case 0x0015:
            return NONCE;
        case 0x0016:
            return XOR_RELAY_ADDRESS;
        case 0x0018:
            return EVEN_PORT;
        case 0x0020:
            return XOR_MAPPED_ADDRESS;
        case 0x0022:
            return RESERVATION_TOKEN;
        case (short)0x8022:
            return SOFTWARE;
        case (short)0x8023:
            return ALTERNATE_SERVER;
        case (short)0x8028:
            return FINGERPRINT;
        default:
            AttributeType result = AttributeType.UNKNOWN;
            result.setType((short) type);
            return AttributeType.UNKNOWN;
        }
    }

    private void setType(short type) {
        this.hexCode = type;
    }
}
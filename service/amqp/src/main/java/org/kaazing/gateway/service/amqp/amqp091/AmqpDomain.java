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
package org.kaazing.gateway.service.amqp.amqp091;

public enum AmqpDomain {
    BIT(AmqpType.BIT),
    CLASS_ID(AmqpType.SHORT),
    CONSUMER_TAG(AmqpType.SHORTSTRING),
    DELIVERY_TAG(AmqpType.LONG),
    EXCHANGE_NAME(AmqpType.SHORTSTRING),
    LONG(AmqpType.INT),
    LONGLONG(AmqpType.LONG),
    LONGSTR(AmqpType.LONGSTRING),
    MESSAGE_COUNT(AmqpType.INT),
    METHOD_ID(AmqpType.SHORT),
    NO_ACK(AmqpType.BIT),
    NO_LOCAL(AmqpType.BIT),
    NO_WAIT(AmqpType.BIT),
    OCTET(AmqpType.UNSIGNED),
    PATH(AmqpType.SHORTSTRING),
    PEER_PROPERTIES(AmqpType.TABLE),
    QUEUE_NAME(AmqpType.SHORTSTRING),
    REDELIVERED(AmqpType.BIT),
    REPLY_CODE(AmqpType.SHORT),
    REPLY_TEXT(AmqpType.SHORTSTRING),
    SHORT(AmqpType.SHORT),
    SHORTSTR(AmqpType.SHORTSTRING),
    TABLE(AmqpType.TABLE),
    TIMESTAMP(AmqpType.TIMESTAMP),
    VOID(AmqpType.VOID);
    
    private final AmqpType    type;
    
    AmqpDomain(AmqpType type) {
        this.type = type;
    }
    
    public AmqpType type() {
        return type;
    }
}

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


public enum AmqpProperty {
    CONTENT_TYPE(AmqpDomain.SHORTSTR),
    CONTENT_ENCODING(AmqpDomain.SHORTSTR),
    HEADERS(AmqpDomain.TABLE),
    DELIVERY_MODE(AmqpDomain.OCTET),
    PRIORITY(AmqpDomain.OCTET),
    CORRELATION_ID(AmqpDomain.SHORTSTR),
    REPLY_TO(AmqpDomain.SHORTSTR),
    EXPIRATION(AmqpDomain.SHORTSTR),
    MESSAGE_ID(AmqpDomain.SHORTSTR),
    TIMESTAMP(AmqpDomain.TIMESTAMP),
    TYPE(AmqpDomain.SHORTSTR),
    USER_ID(AmqpDomain.SHORTSTR),
    APP_ID(AmqpDomain.SHORTSTR),
    RESERVED(AmqpDomain.SHORTSTR);

    private final AmqpDomain    domain;

    AmqpProperty(AmqpDomain domain) {
        this.domain = domain;
    }

    public AmqpDomain domain() {
        return domain;
    }
}

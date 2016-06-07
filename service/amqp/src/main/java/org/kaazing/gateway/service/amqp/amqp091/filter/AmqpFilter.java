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
package org.kaazing.gateway.service.amqp.amqp091.filter;

import org.apache.mina.core.filterchain.IoFilter.NextFilter;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.write.WriteRequest;
import org.kaazing.gateway.server.util.io.IoFilter;
import org.kaazing.gateway.service.amqp.amqp091.message.AmqpCloseMessage;
import org.kaazing.gateway.service.amqp.amqp091.message.AmqpCloseOkMessage;
import org.kaazing.gateway.service.amqp.amqp091.message.AmqpMessage;
import org.kaazing.gateway.service.amqp.amqp091.message.AmqpOpenMessage;
import org.kaazing.gateway.service.amqp.amqp091.message.AmqpOpenOkMessage;
import org.kaazing.gateway.service.amqp.amqp091.message.AmqpProtocolHeaderMessage;
import org.kaazing.gateway.service.amqp.amqp091.message.AmqpSecureMessage;
import org.kaazing.gateway.service.amqp.amqp091.message.AmqpSecureOkMessage;
import org.kaazing.gateway.service.amqp.amqp091.message.AmqpStartMessage;
import org.kaazing.gateway.service.amqp.amqp091.message.AmqpStartOkMessage;
import org.kaazing.gateway.service.amqp.amqp091.message.AmqpTuneMessage;
import org.kaazing.gateway.service.amqp.amqp091.message.AmqpTuneOkMessage;

public interface AmqpFilter<S extends IoSession> extends IoFilter<S, AmqpMessage> {
    void messageReceived(NextFilter nextFilter, S session, AmqpProtocolHeaderMessage message) throws Exception;
    void messageSent(NextFilter nextFilter, S session, WriteRequest writeRequest, AmqpProtocolHeaderMessage message) throws Exception;
    void filterWrite(NextFilter nextFilter, S session, WriteRequest writeRequest, AmqpProtocolHeaderMessage message) throws Exception;
    
    void messageReceived(NextFilter nextFilter, S session, AmqpCloseMessage message) throws Exception;
    void messageSent(NextFilter nextFilter, S session, WriteRequest writeRequest, AmqpCloseMessage message) throws Exception;
    void filterWrite(NextFilter nextFilter, S session, WriteRequest writeRequest, AmqpCloseMessage message) throws Exception;

    void messageReceived(NextFilter nextFilter, S session, AmqpCloseOkMessage message) throws Exception;
    void messageSent(NextFilter nextFilter, S session, WriteRequest writeRequest, AmqpCloseOkMessage message) throws Exception;
    void filterWrite(NextFilter nextFilter, S session, WriteRequest writeRequest, AmqpCloseOkMessage message) throws Exception;

    void messageReceived(NextFilter nextFilter, S session, AmqpOpenMessage message) throws Exception;
    void messageSent(NextFilter nextFilter, S session, WriteRequest writeRequest, AmqpOpenMessage message) throws Exception;
    void filterWrite(NextFilter nextFilter, S session, WriteRequest writeRequest, AmqpOpenMessage message) throws Exception;

    void messageReceived(NextFilter nextFilter, S session, AmqpOpenOkMessage message) throws Exception;
    void messageSent(NextFilter nextFilter, S session, WriteRequest writeRequest, AmqpOpenOkMessage message) throws Exception;
    void filterWrite(NextFilter nextFilter, S session, WriteRequest writeRequest, AmqpOpenOkMessage message) throws Exception;

    void messageReceived(NextFilter nextFilter, S session, AmqpSecureMessage message) throws Exception;
    void messageSent(NextFilter nextFilter, S session, WriteRequest writeRequest, AmqpSecureMessage message) throws Exception;
    void filterWrite(NextFilter nextFilter, S session, WriteRequest writeRequest, AmqpSecureMessage message) throws Exception;

    void messageReceived(NextFilter nextFilter, S session, AmqpSecureOkMessage message) throws Exception;
    void messageSent(NextFilter nextFilter, S session, WriteRequest writeRequest, AmqpSecureOkMessage message) throws Exception;
    void filterWrite(NextFilter nextFilter, S session, WriteRequest writeRequest, AmqpSecureOkMessage message) throws Exception;

    void messageReceived(NextFilter nextFilter, S session, AmqpStartMessage message) throws Exception;
    void messageSent(NextFilter nextFilter, S session, WriteRequest writeRequest, AmqpStartMessage message) throws Exception;
    void filterWrite(NextFilter nextFilter, S session, WriteRequest writeRequest, AmqpStartMessage message) throws Exception;

    void messageReceived(NextFilter nextFilter, S session, AmqpStartOkMessage message) throws Exception;
    void messageSent(NextFilter nextFilter, S session, WriteRequest writeRequest, AmqpStartOkMessage message) throws Exception;
    void filterWrite(NextFilter nextFilter, S session, WriteRequest writeRequest, AmqpStartOkMessage message) throws Exception;

    void messageReceived(NextFilter nextFilter, S session, AmqpTuneMessage message) throws Exception;
    void messageSent(NextFilter nextFilter, S session, WriteRequest writeRequest, AmqpTuneMessage message) throws Exception;
    void filterWrite(NextFilter nextFilter, S session, WriteRequest writeRequest, AmqpTuneMessage message) throws Exception;

    void messageReceived(NextFilter nextFilter, S session, AmqpTuneOkMessage message) throws Exception;
    void messageSent(NextFilter nextFilter, S session, WriteRequest writeRequest, AmqpTuneOkMessage message) throws Exception;
    void filterWrite(NextFilter nextFilter, S session, WriteRequest writeRequest, AmqpTuneOkMessage message) throws Exception;


    @SuppressWarnings("unchecked")
    class Adapter<S extends IoSession> extends IoFilter.Adapter<S, AmqpMessage, AmqpFilter<S>> {

        public Adapter(AmqpFilter<S> filter) {
            super(filter);
        }

        @Override
        public void filterWrite(NextFilter nextFilter, IoSession session,
                WriteRequest writeRequest) throws Exception {
            AmqpMessage message = (AmqpMessage)writeRequest.getMessage();
            message.filterWrite(filter, nextFilter, (S)session, writeRequest);
        }

        @Override
        public void messageReceived(NextFilter nextFilter, IoSession session,
                Object message) throws Exception {
            ((AmqpMessage)message).messageReceived(filter, nextFilter, (S)session);
        }

        @Override
        public void messageSent(NextFilter nextFilter, IoSession session,
                WriteRequest writeRequest) throws Exception {
            AmqpMessage message = (AmqpMessage)writeRequest.getMessage();
            message.messageSent(filter, nextFilter, (S)session, writeRequest);
        }
        
        // for unit tests
        public AmqpFilter<S> getAmqpFilter() {
            return filter;
        }        
    }

}

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
import org.kaazing.gateway.server.util.io.IoFilterAdapter;
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


public class AmqpFilterAdapter<S extends IoSession> extends
        IoFilterAdapter<S, AmqpMessage> implements AmqpFilter<S> {

    // -------------------- AmqpProtocolHeaderMessage -------------------------
    @Override
    public void messageReceived(NextFilter nextFilter, S session,
            AmqpProtocolHeaderMessage message) throws Exception { 
        nextFilter.messageReceived(session, message); 
    }

    @Override
    public void messageSent(NextFilter nextFilter, S session,
            WriteRequest writeRequest, AmqpProtocolHeaderMessage message)
            throws Exception {
        nextFilter.messageSent(session, writeRequest);
    }

    @Override
    public void filterWrite(NextFilter nextFilter, S session,
            WriteRequest writeRequest, AmqpProtocolHeaderMessage message)
            throws Exception {
        nextFilter.filterWrite(session, writeRequest);        
    }

    // --------------------------- AmqpCloseMessage --------------------------
    @Override
    public void messageReceived(NextFilter nextFilter, S session,
            AmqpCloseMessage message) throws Exception {
        nextFilter.messageReceived(session, message); 
    }

    @Override
    public void messageSent(NextFilter nextFilter, S session,
            WriteRequest writeRequest, AmqpCloseMessage message)
            throws Exception {
        nextFilter.messageSent(session, writeRequest);
    }

    @Override
    public void filterWrite(NextFilter nextFilter, S session,
            WriteRequest writeRequest, AmqpCloseMessage message)
            throws Exception {
        nextFilter.filterWrite(session, writeRequest);        
    }

    // -------------------------- AmqpCloseOkMessage -------------------------
    @Override
    public void messageReceived(NextFilter nextFilter, S session,
            AmqpCloseOkMessage message) throws Exception {
        nextFilter.messageReceived(session, message); 
    }

    @Override
    public void messageSent(NextFilter nextFilter, S session,
            WriteRequest writeRequest, AmqpCloseOkMessage message)
            throws Exception {
        nextFilter.messageSent(session, writeRequest);
    }

    @Override
    public void filterWrite(NextFilter nextFilter, S session,
            WriteRequest writeRequest, AmqpCloseOkMessage message)
            throws Exception {
        nextFilter.filterWrite(session, writeRequest);        
    }

    // --------------------------- AmqpOpenMessage --------------------------
    @Override
    public void messageReceived(NextFilter nextFilter, S session,
            AmqpOpenMessage message) throws Exception {
        nextFilter.messageReceived(session, message); 
    }

    @Override
    public void messageSent(NextFilter nextFilter, S session,
            WriteRequest writeRequest, AmqpOpenMessage message)
            throws Exception {
        nextFilter.messageSent(session, writeRequest);
    }

    @Override
    public void filterWrite(NextFilter nextFilter, S session,
            WriteRequest writeRequest, AmqpOpenMessage message)
            throws Exception {
        nextFilter.filterWrite(session, writeRequest);        
    }

    // --------------------------- AmqpOpenOkMessage --------------------------
    @Override
    public void messageReceived(NextFilter nextFilter, S session,
            AmqpOpenOkMessage message) throws Exception {
        nextFilter.messageReceived(session, message); 
    }

    @Override
    public void messageSent(NextFilter nextFilter, S session,
            WriteRequest writeRequest, AmqpOpenOkMessage message)
            throws Exception {
        nextFilter.messageSent(session, writeRequest);
    }

    @Override
    public void filterWrite(NextFilter nextFilter, S session,
            WriteRequest writeRequest, AmqpOpenOkMessage message)
            throws Exception {
        nextFilter.filterWrite(session, writeRequest);        
    }

    // --------------------------- AmqpSecureMessage --------------------------
    @Override
    public void messageReceived(NextFilter nextFilter, S session,
            AmqpSecureMessage message) throws Exception {
        nextFilter.messageReceived(session, message); 
    }

    @Override
    public void messageSent(NextFilter nextFilter, S session,
            WriteRequest writeRequest, AmqpSecureMessage message)
            throws Exception {
        nextFilter.messageSent(session, writeRequest);
    }

    @Override
    public void filterWrite(NextFilter nextFilter, S session,
            WriteRequest writeRequest, AmqpSecureMessage message)
            throws Exception {
        nextFilter.filterWrite(session, writeRequest);        
    }

    // ------------------------- AmqpSecureOkMessage --------------------------
    @Override
    public void messageReceived(NextFilter nextFilter, S session,
            AmqpSecureOkMessage message) throws Exception {
        nextFilter.messageReceived(session, message); 
    }

    @Override
    public void messageSent(NextFilter nextFilter, S session,
            WriteRequest writeRequest, AmqpSecureOkMessage message)
            throws Exception {
        nextFilter.messageSent(session, writeRequest);
    }

    @Override
    public void filterWrite(NextFilter nextFilter, S session,
            WriteRequest writeRequest, AmqpSecureOkMessage message)
            throws Exception {
        nextFilter.filterWrite(session, writeRequest);        
    }

    // --------------------------- AmqpStartMessage --------------------------
    @Override
    public void messageReceived(NextFilter nextFilter, S session,
            AmqpStartMessage message) throws Exception {
        nextFilter.messageReceived(session, message); 
    }

    @Override
    public void messageSent(NextFilter nextFilter, S session,
            WriteRequest writeRequest, AmqpStartMessage message)
            throws Exception {
        nextFilter.messageSent(session, writeRequest);
    }

    @Override
    public void filterWrite(NextFilter nextFilter, S session,
            WriteRequest writeRequest, AmqpStartMessage message)
            throws Exception {
        nextFilter.filterWrite(session, writeRequest);        
    }

    // -------------------------- AmqpStartOkMessage --------------------------
    @Override
    public void messageReceived(NextFilter nextFilter, S session,
            AmqpStartOkMessage message) throws Exception {
        nextFilter.messageReceived(session, message); 
    }

    @Override
    public void messageSent(NextFilter nextFilter, S session,
            WriteRequest writeRequest, AmqpStartOkMessage message)
            throws Exception {
        nextFilter.messageSent(session, writeRequest);
    }

    @Override
    public void filterWrite(NextFilter nextFilter, S session,
            WriteRequest writeRequest, AmqpStartOkMessage message)
            throws Exception {
        nextFilter.filterWrite(session, writeRequest);        
    }
    
    // --------------------------- AmqpTuneMessage ---------------------------
    @Override
    public void messageReceived(NextFilter nextFilter, S session,
            AmqpTuneMessage message) throws Exception {
        nextFilter.messageReceived(session, message); 
    }

    @Override
    public void messageSent(NextFilter nextFilter, S session,
            WriteRequest writeRequest, AmqpTuneMessage message)
            throws Exception {
        nextFilter.messageSent(session, writeRequest);
    }

    @Override
    public void filterWrite(NextFilter nextFilter, S session,
            WriteRequest writeRequest, AmqpTuneMessage message)
            throws Exception {
        nextFilter.filterWrite(session, writeRequest);        
    }

    // --------------------------- AmqpTuneOkMessage ---------------------------
    @Override
    public void messageReceived(NextFilter nextFilter, S session,
            AmqpTuneOkMessage message) throws Exception {
        nextFilter.messageReceived(session, message); 
    }

    @Override
    public void messageSent(NextFilter nextFilter, S session,
            WriteRequest writeRequest, AmqpTuneOkMessage message)
            throws Exception {
        nextFilter.messageSent(session, writeRequest);
    }

    @Override
    public void filterWrite(NextFilter nextFilter, S session,
            WriteRequest writeRequest, AmqpTuneOkMessage message)
            throws Exception {
        nextFilter.filterWrite(session, writeRequest);        
    }
}

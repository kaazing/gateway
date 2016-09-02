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

package org.kaazing.gateway.service.turn.proxy.stun;

import java.util.Base64;
import java.util.Map;

import org.apache.mina.core.filterchain.IoFilterAdapter;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.write.WriteRequest;
import org.kaazing.gateway.service.turn.proxy.stun.attributes.Username;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StunUserameFilter extends IoFilterAdapter {

    static final Logger LOGGER = LoggerFactory.getLogger(StunUserameFilter.class);

    private final Map<String, String> currentTransactions;

    public StunUserameFilter(Map<String, String> currentTransactions) {
        this.currentTransactions = currentTransactions;
    }

    @Override
    public void messageReceived(NextFilter nextFilter, IoSession session, Object message) throws Exception {
        if (message instanceof StunMessage) {
            // Store the incoming username for this transaction, will be reused if generating the message integrity
            StunMessage stunMessage = (StunMessage) message;
            stunMessage.getAttributes().stream().filter(attr -> attr instanceof Username).forEach(attr -> {
                String transactionId = Base64.getEncoder().encodeToString(stunMessage.getTransactionId());
                String username = ((Username) attr).getUsername();
                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace(String.format("Storing username in transactionsMap %s -> %s", transactionId, username));
                }
                currentTransactions.put(transactionId, username);
            });
        }
        super.messageReceived(nextFilter, session, message);
    }
    
    @Override
    public void messageSent(NextFilter nextFilter, IoSession session,
           WriteRequest writeRequest) throws Exception {
        Object message = writeRequest.getMessage();
        if (message instanceof StunMessage) {
            StunMessage stunMessage = (StunMessage) message;
            String username = currentTransactions.remove(Base64.getEncoder().encodeToString(stunMessage.getTransactionId()));
            if (username != null) {
                stunMessage.setUsername(username);
            }
        }
       super.messageSent(nextFilter, session, writeRequest);
   }

}

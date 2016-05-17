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
package org.kaazing.gateway.service.amqp.amqp091.message;

import org.apache.mina.core.filterchain.IoFilter.NextFilter;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.write.WriteRequest;
import org.kaazing.gateway.service.amqp.amqp091.filter.AmqpFilter;

public class AmqpCloseMessage extends AmqpConnectionMessage {
    private int           replyCode = 0;
    private String        replyText = "";
    private ClassKind     reasonClassKind = null;
    private int           reasonMethodId = 0;

    @Override
    public ConnectionMethodKind getMethodKind() {
        return ConnectionMethodKind.CLOSE;
    }
    
    @Override
    public <S extends IoSession> void messageReceived(AmqpFilter<S> filter,
            NextFilter nextFilter, S session) throws Exception {
        filter.messageReceived(nextFilter, session, this);
    }

    @Override
    public <S extends IoSession> void filterWrite(AmqpFilter<S> filter,
            NextFilter nextFilter, S session, WriteRequest writeRequest)
            throws Exception {
        filter.filterWrite(nextFilter, session, writeRequest, this);
    }

    @Override
    public <S extends IoSession> void messageSent(AmqpFilter<S> filter,
            NextFilter nextFilter, S session, WriteRequest writeRequest)
            throws Exception {
        filter.messageSent(nextFilter, session, writeRequest, this);
    }

    public ClassKind getReasonClassKind() {
        return reasonClassKind;
    }

    public int getReasonClassId() {
        if (reasonClassKind == null) {
            return 0;
        }
        
        return reasonClassKind.classId();
    }

    public int getReasonMethodId() {
        return reasonMethodId;
    }

    public int getReplyCode() {
        return replyCode;
    }

    public String getReplyText() {
        return replyText;
    }

    public void setReasonClassKind(ClassKind reasonClassKind) {
        this.reasonClassKind = reasonClassKind;
    }

    // This could be the MethodKind from other classes.
    public void setReasonMethodId(int methodId) { 
        this.reasonMethodId = methodId;
    }

    public void setReplyCode(int replyCode) {
        this.replyCode = replyCode;
    }

    public void setReplyText(String replyText) {
        this.replyText = replyText;
    }
    
    public String toString() {
        StringBuffer buffer = new StringBuffer("CLOSE: ");
        buffer.append("reply-code = '").append(replyCode).append("'")
              .append("   reply-text = '").append(replyText).append("'")
              .append("   reason-class-id = '").append(reasonClassKind).append("'")
              .append("   reason-method-id = '").append(reasonMethodId).append("'");
        
        return buffer.toString();
    }
}

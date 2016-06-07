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

public class AmqpOpenMessage extends AmqpConnectionMessage {
    private String  virtualHost = "/";
    private String  reserved1 = "";
    private Boolean reserved2 = Boolean.FALSE;

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
    
    @Override
    public ConnectionMethodKind getMethodKind() {
        return ConnectionMethodKind.OPEN;
    }

    public String getReserved1() {
        return reserved1;
    }

    public Boolean getReserved2() {
        return reserved2;
    }

    public String getVirtualHost() {
        return virtualHost;
    }
    
    public void setReserved1(String value) {
        if (value == null) {
            value = "";
        }
        
        this.reserved1 = value;
    }
    
    public void setReserved2(Boolean value) {
        if (value == null) {
            value = Boolean.FALSE;
        }
        
        this.reserved2 = value;
    }

    public void setVirtualHost(String virtualHost) {
        if (virtualHost == null) {
            virtualHost = "";
        }

        this.virtualHost = virtualHost;
    }
    
    public String toString() {
        StringBuffer buffer = new StringBuffer("OPEN: ");
        buffer.append("virtual-host = '").append(virtualHost).append("'")
              .append("   reserved1 = '").append(reserved1).append("'")
              .append("   reserved2 = '").append(reserved2).append("'");
        
        return buffer.toString();
    }
}

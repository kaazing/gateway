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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.mina.core.filterchain.IoFilter.NextFilter;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.write.WriteRequest;
import org.kaazing.gateway.service.amqp.amqp091.AmqpTable;
import org.kaazing.gateway.service.amqp.amqp091.AmqpTable.AmqpTableEntry;
import org.kaazing.gateway.service.amqp.amqp091.filter.AmqpFilter;


public class AmqpStartOkMessage extends AmqpConnectionMessage {
    private AmqpTable    clientProperties;
    private String       securityMechanism;
    private String       username;
    private char[]       password;
    private String       locale;

    public AmqpStartOkMessage() {
        clientProperties = new AmqpTable();
        securityMechanism = "AMQPLAIN";
        locale = Locale.getDefault().toString();
    }

    @Override
    public ConnectionMethodKind getMethodKind() {
        return ConnectionMethodKind.START_OK;
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
    
    public AmqpTable getClientProperties() {
        AmqpTable            table = new AmqpTable();
        List<AmqpTableEntry> tableEntries = clientProperties.getEntries();

        List<AmqpTableEntry> entries = new ArrayList<>();
        entries.addAll(tableEntries);
        
        table.setEntries(entries);
        return table;
    }

    public String getLocale() {
        return locale;
    }

    public char[] getPassword() {
        return this.password;
    }
    
    public String getSecurityMechanism() {
        return this.securityMechanism;
    }
    
    public String getUsername() {
        return this.username;
    }
    
    public void setClientProperties(AmqpTable props) {
        if (props == null) {
            props = new AmqpTable();
        }

        this.clientProperties = props;
    }
    
    public void setLocale(String locale) {
        if (locale == null) {
            locale = "";
        }

        this.locale = locale;
    }

    public void setPassword(char[] password) {
        if (password == null) {
            password = new char[0];
        }

        this.password = password;
    }

    public void setSecurityMechanism(String mechanism) {
        if (mechanism == null) {
            mechanism = "";
        }

        this.securityMechanism = mechanism;
    }
    
    public void setUsername(String username) {
        if (username == null) {
            username = "";
        }

        this.username = username;
    }
    
    public String toString() {
        StringBuffer buffer = new StringBuffer("START_OK: ");
        buffer.append("client-props = '").append(clientProperties).append("'")
              .append("   mechanism = '").append(securityMechanism).append("'")
              .append("   username = '").append(username).append("'")
              .append("   locale = '").append(locale).append("'");

        return buffer.toString();
    }
}

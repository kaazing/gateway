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

public class AmqpStartMessage extends AmqpConnectionMessage {
    private byte        versionMajor;
    private byte        versionMinor; 
    private AmqpTable   serverProperties;
    private String      securityMechanisms;
    private String      locales;

    public AmqpStartMessage() {
        versionMajor = 0;
        versionMinor = 9;
        serverProperties = new AmqpTable();
        locales = Locale.getDefault().toString();
        securityMechanisms = "AMQPLAIN";
    }

    @Override
    public ConnectionMethodKind getMethodKind() {
        return ConnectionMethodKind.START;
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

    public String getLocales() {
        return locales;
    }

    public String getSecurityMechanisms() {
        return securityMechanisms;
    }
    
    public AmqpTable getServerProperties() {
        AmqpTable            table = new AmqpTable();
        List<AmqpTableEntry> tableEntries = serverProperties.getEntries();

        List<AmqpTableEntry> entries = new ArrayList<>();
        entries.addAll(tableEntries);
        
        table.setEntries(entries);
        return table;
    }
    
    public byte getVersionMajor() {
        return versionMajor;
    }
    
    public byte getVersionMinor() {
        return versionMinor;
    }
    
    public void setLocales(String locales) {
        if (locales == null) {
            locales = "";
        }
        
        this.locales = locales;
    }

    public void setSecurityMechanisms(String mechanisms) {
        if (mechanisms == null) {
            mechanisms = "";
        }
        
        this.securityMechanisms = mechanisms;
    }
    
    public void setServerProperties(AmqpTable properties) {
        if (properties == null) {
            properties = new AmqpTable();
        }

        this.serverProperties = properties;
    }
    
    public void setVersionMajor(byte versionMajor) {
        this.versionMajor = versionMajor;
    }

    public void setVersionMinor(byte versionMinor) {
        this.versionMinor = versionMinor;
    }
    
    public String toString() {
        StringBuffer buffer = new StringBuffer("START: ");
        buffer.append("version-major = '").append(versionMajor).append("'")
              .append("   version-minor = '").append(versionMinor).append("'")
              .append("   server-props = '").append(serverProperties).append("'")
              .append("   security-mechanisms = '").append(securityMechanisms).append("'")
              .append("   locales = '").append(locales).append("'");

        return buffer.toString();
    }
}

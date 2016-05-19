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
package org.kaazing.gateway.service.cluster;

import java.io.Serializable;

public class MemberId implements Serializable {

    private static final long serialVersionUID = 5081622065110987496L;

    private int port;
    private String host;
    private String id;
    private String path;
    private String protocol;

    public MemberId(String protocol, String host, int port) {
        this(protocol, host, port, null);
    }

    public MemberId(String protocol, String host, int port, String path) {
        this.port = port;
        this.host = host;
        this.protocol = protocol;
        this.path = path;
        String s = (path != null) ? path : "";
        this.id = protocol.toLowerCase() + "://" + host + ":" + port + s;
    }

    public int getPort() {
        return port;
    }

    public String getId() {
        return id;
    }

    public boolean equals(Object otherObject) {
        if (otherObject instanceof MemberId) {
            MemberId other = (MemberId)otherObject;
            return (this.id == other.id || this.id.equals(other.id));
        }
        return false;
    }

    public int hashCode() {
        return id.hashCode();
    }

    public String getHost() {
        return host;
    }

    public String getPath() {
        return path;
    }

    @Override
    public String toString() {
        return id;
    }

    public String getProtocol() {
        return protocol;
    }
}

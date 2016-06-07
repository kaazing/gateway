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
package org.kaazing.gateway.transport.sse.bridge;

import org.kaazing.gateway.transport.bridge.Message;
import org.kaazing.mina.core.buffer.IoBufferEx;

public class SseMessage extends Message {

	private String type;
	private IoBufferEx data;
	private String id;
	private int retry = -1;
	private String comment;
	private String location;
	private boolean reconnect;

	public SseMessage() {
		this(null, null);
	}
	
	public SseMessage(String type, IoBufferEx data) {
		this.type = type;
		this.data = data;
	}
		
	public String getComment() {
		return comment;
	}

	public IoBufferEx getData() {
		return data;
	}

	public String getId() {
		return id;
	}

	public String getType() {
		return type;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

	public void setData(IoBufferEx data) {
		this.data = data;		
	}

	public void setId(String id) {
		this.id = id;
	}

	public void setType(String type) {
		if ("message".equals(type)) {
			type = null;
		}
		this.type = type;
	}
	
	public boolean isReconnect() {
		return reconnect;
	}

	public void setReconnect(boolean reconnect) {
		this.reconnect = reconnect;
	}

    public void setLocation(String location) {
		this.location = location;
	}

	public String getLocation() {
		return location;
	}

	public int getRetry() {
        return retry;
    }

    public void setRetry(int retry) {
        this.retry = retry;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append('[');
        if (type != null) {
            builder.append("event:");
            builder.append(type);
            builder.append(",");
        }
        if (data != null) {
            builder.append("data:");
            builder.append(data);
            builder.append(",");
        }        
        if (id != null) {
            builder.append("id:");
            builder.append(id);
            builder.append(",");
        }        
        if (retry >= 0) {
            builder.append("retry:");
            builder.append(retry);
            builder.append(",");
        }
        if (comment != null) {
            builder.append("comment:");
            builder.append(comment);
            builder.append(",");
        }
        if (location != null) {
            builder.append("location:");
            builder.append(location);
            builder.append(",");
        }        
        if (reconnect) {
        	builder.append("reconnect");
            builder.append(",");
        }        
        builder.setCharAt(builder.length() - 1, ']');
        return builder.toString();
    }
}

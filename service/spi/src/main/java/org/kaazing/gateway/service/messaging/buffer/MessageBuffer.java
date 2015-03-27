/**
 * Copyright (c) 2007-2014 Kaazing Corporation. All rights reserved.
 * 
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.kaazing.gateway.service.messaging.buffer;

import org.kaazing.gateway.service.messaging.MessagingMessage;

public interface MessageBuffer {

	// TODO: this could return null for expired messages and possibly null them to clear storage     
	MessageBufferEntry get(int id);	
    MessageBufferEntry add(MessagingMessage message);
    MessageBufferEntry set(int index, MessagingMessage message);
	int getYoungestId();
	int getOldestId();
	int getCapacity();
	
	void addMessageBufferListener(MessageBufferListener listener);
	void removeMessageBufferListener(MessageBufferListener listener);
}

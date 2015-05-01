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

package org.kaazing.gateway.transport.ws;

public abstract class Command {
		
	public abstract int value();

	private static final Command NOOP = new SingleInstructionCommand(0x00);
    private static final Command RECONNECT = new SingleInstructionCommand(0x01);
    private static final Command CLOSE = new SingleInstructionCommand(0x02);
	
	public static Command noop() {
		return NOOP;
	}

	public static Command reconnect() {
		return RECONNECT;
	}
	
	public static Command close() {
	    return CLOSE;
	}
	
	private static class SingleInstructionCommand extends Command {
	    
	    private final int value;
	    
	    public SingleInstructionCommand(int value) {
	        this.value = value;
	    }
	    
	    @Override
        public int value() {
            return value;
        }
	}
}

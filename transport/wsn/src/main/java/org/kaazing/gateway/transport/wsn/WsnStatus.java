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
package org.kaazing.gateway.transport.wsn;

public enum WsnStatus {
	NORMAL_CLOSE(1000),
	GOING_AWAY(1001),
	PROTOCOL_ERROR(1002),
	UNACCEPTABLE_TYPE(1003),
	RESERVED(1004),
	RESERVED_NO_STATUS(1005),
	RESERVED_ABNORMAL_CLOSE(1006),
	BAD_DATA(1007),
	BAD_MESSAGE(1008),
	MESSAGE_TOO_LARGE(1009),
	EXTENSIONS_REQUIRED(1010);

	private final int code;
	private final String reason;
	WsnStatus(int code) {
		this.code = code;
		this.reason = "";
	}
	
	WsnStatus(int code, String reason) {
		this.code = code;
		this.reason = reason;
	}
}

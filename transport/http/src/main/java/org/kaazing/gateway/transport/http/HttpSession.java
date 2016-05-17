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
package org.kaazing.gateway.transport.http;

import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.kaazing.gateway.transport.BridgeSession;

public interface HttpSession extends BridgeSession {
	
	HttpVersion getVersion();
	
	void setVersion(HttpVersion version);
	
	String getWriteHeader(String name);
	
	List<String> getWriteHeaders(String name);
	
	void setWriteHeader(String name, String value);
    
    void setWriteHeaders(String name, List<String> value);
    
    void setWriteHeaders(Map<String, List<String>> writeHeaders);
	
	void addWriteHeader(String name, String value);
	
	void clearWriteHeaders(String name);
	
	Map<String, List<String>> getWriteHeaders();
	
	Set<HttpCookie> getWriteCookies();

    void setWriteCookies(Set<HttpCookie> singleton);

    String getReadHeader(String name);
	
	List<String> getReadHeaders(String name);
	
    Collection<String> getReadHeaderNames();
    
	Map<String, List<String>> getReadHeaders();
	
	Collection<HttpCookie> getReadCookies();

	boolean isSecure();
		
	boolean isCommitting();

	URI getRequestURL();
	
	HttpMethod getMethod();
	
	URI getRequestURI();
	
	String getParameter(String name);

	List<String> getParameterValues(String name);

	Map<String, List<String>> getParameters();
	
	HttpStatus getStatus();
		
	String getReason();

	void shutdownWrite();

}

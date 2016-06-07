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
package org.kaazing.gateway.transport.http.bridge;

import java.util.Comparator;

import org.kaazing.gateway.transport.http.HttpCookie;


public class HttpCookieComparator implements Comparator<HttpCookie> {

    public static final HttpCookieComparator INSTANCE = new HttpCookieComparator();

    @Override
	public int compare(HttpCookie thisCookie, HttpCookie thatCookie) {
    	
    	int comparison = _equalsOrCompare(thisCookie.getName(), thatCookie.getName());
    	
    	if (comparison == 0) {
    		comparison = _equalsOrCompare(thisCookie.getPath(), thatCookie.getPath());

        	if (comparison == 0) {
        		comparison = _equalsOrCompare(thisCookie.getDomain(), thatCookie.getDomain());
        	}
    	}
    	
        return comparison;
    }
    
    private int _equalsOrCompare(String s0, String s1) {
    	if (s0 == s1) {
    		return 0;
    	}
    	
    	if (s0 == null) {
    		return -1;
    	}
    	
    	if (s1 == null) {
    		return 1;
    	}
    	
    	return s0.compareTo(s1);
    }

    private HttpCookieComparator() {
    	// singleton
    }
}

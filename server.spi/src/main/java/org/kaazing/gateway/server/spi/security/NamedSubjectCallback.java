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
package org.kaazing.gateway.server.spi.security;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;

/**
 * A callback class that retrieves the Subject for a previous realm when authorizing multiple realms
 */
public class NamedSubjectCallback implements Callback {

	private final String realm;

	private Subject subject;

	public NamedSubjectCallback(String realm) {
		this.realm = realm;
	}

	public String getName() {
		return realm;
	}

    public void setSubject(Subject subject) {
        this.subject = subject;
    }

	public Subject getSubject() {
        return subject;
    }
}

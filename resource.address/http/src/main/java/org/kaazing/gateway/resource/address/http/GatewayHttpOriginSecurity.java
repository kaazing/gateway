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
package org.kaazing.gateway.resource.address.http;

import static java.util.Collections.unmodifiableList;

import java.util.List;
import java.util.Map;

import org.kaazing.gateway.security.CrossSiteConstraintContext;

public class GatewayHttpOriginSecurity {
    private final List<Map<String, Map<String, CrossSiteConstraintContext>>> authorityToSetOfAcceptConstraintsByURI;

    public GatewayHttpOriginSecurity(
            List<Map<String, Map<String, CrossSiteConstraintContext>>> authorityToSetOfAcceptConstraintsByURI) {
        super();
        this.authorityToSetOfAcceptConstraintsByURI = authorityToSetOfAcceptConstraintsByURI;
    }

    public List<Map<String, Map<String, CrossSiteConstraintContext>>> getAuthorityToSetOfAcceptConstraintsByURI() {
        return unmodifiableList(authorityToSetOfAcceptConstraintsByURI);
    }

}

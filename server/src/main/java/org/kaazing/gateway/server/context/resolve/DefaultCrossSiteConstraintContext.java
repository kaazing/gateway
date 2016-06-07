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
package org.kaazing.gateway.server.context.resolve;

import org.kaazing.gateway.security.CrossSiteConstraintContext;

public class DefaultCrossSiteConstraintContext implements CrossSiteConstraintContext {

    private final String allowOrigin;
    private final String allowMethods;
    private final String allowHeaders;
    private final Integer maximumAge;

    public DefaultCrossSiteConstraintContext(String allowOrigin, String allowMethods, String allowHeaders, Integer maximumAge) {
        this.allowOrigin = allowOrigin;
        this.allowMethods = allowMethods;
        this.allowHeaders = allowHeaders;
        this.maximumAge = maximumAge;
    }

    @Override
    public String getAllowOrigin() {
        return allowOrigin;
    }

    @Override
    public String getAllowMethods() {
        return allowMethods;
    }

    @Override
    public String getAllowHeaders() {
        return allowHeaders;
    }

    @Override
    public Integer getMaximumAge() {
        return maximumAge;
    }

    public String toString() {
        return String.format("%s[allowOrigin=%s, allowMethods=%s, allowHeaders=%s, maximumAge=%s",
                DefaultCrossSiteConstraintContext.class.getSimpleName(),
                allowOrigin, allowMethods, allowHeaders, maximumAge);
    }
}

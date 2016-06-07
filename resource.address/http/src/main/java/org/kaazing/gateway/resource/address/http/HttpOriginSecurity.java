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

import static java.util.Collections.unmodifiableMap;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.kaazing.gateway.security.CrossSiteConstraintContext;

public class HttpOriginSecurity {
    
    private final Map<String, HttpOriginConstraint> constraints;
    public HttpOriginSecurity(Map<String, ? extends CrossSiteConstraintContext> acceptConstraints) {
        if ( acceptConstraints == null ) {
            throw new NullPointerException("acceptConstraints");
        }

        HashMap<String, HttpOriginConstraint> constraints = new HashMap<>();
            for (Map.Entry<String, ? extends CrossSiteConstraintContext> entry : acceptConstraints.entrySet()) {
                String sourceOrigin = entry.getKey();
                CrossSiteConstraintContext originConstraint = entry.getValue();
                constraints.put(sourceOrigin, new HttpOriginConstraint(originConstraint));
            }
        this.constraints = unmodifiableMap(constraints);
    }
    
    public Collection<String> getSourceOrigins() {
        return constraints.keySet();
    }
    
    public HttpOriginConstraint getConstraint(String sourceOrigin) {
        return constraints.get(sourceOrigin);
    }

    public static final class HttpOriginConstraint {

        private final CrossSiteConstraintContext constraint;
        
        HttpOriginConstraint(CrossSiteConstraintContext constraint) {
            this.constraint = constraint;
        }
        
        public String getAllowOrigin() {
            return constraint.getAllowOrigin();
        }
        
        public String getAllowMethods() {
            return constraint.getAllowMethods();
        }

        public String getAllowHeaders() {
            return constraint.getAllowHeaders();
        }

        public Integer getMaximumAge() {
            return constraint.getMaximumAge();
       }
    }

}

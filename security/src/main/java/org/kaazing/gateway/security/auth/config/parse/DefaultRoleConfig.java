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
package org.kaazing.gateway.security.auth.config.parse;

import java.util.Collection;
import java.util.HashSet;

import org.kaazing.gateway.security.auth.config.RoleConfig;


public class DefaultRoleConfig implements RoleConfig {

    private String name;
    private String description;
    private Collection<String> roleNames;

    public DefaultRoleConfig() {
        roleNames = new HashSet<>();
    }

    @Override
    public Collection<String> getRoleNames() {
        return roleNames;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String getDescription() {
        return description;
    }

}

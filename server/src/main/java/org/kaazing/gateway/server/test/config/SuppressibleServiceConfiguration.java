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
package org.kaazing.gateway.server.test.config;

import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class SuppressibleServiceConfiguration implements SuppressibleConfiguration {

    // accepts
    public abstract Set<Suppressible<String>> getAccepts();

    public abstract void addAccept(Suppressible<String> acceptURI);

    // type
    public abstract Suppressible<String> getType();

    public abstract void setType(Suppressible<String> type);

    // description
    public abstract Suppressible<String> getDescription();

    public abstract void setDescription(Suppressible<String> description);

    // name
    public abstract Suppressible<String> getName();

    public abstract void setName(Suppressible<String> name);

    // realm name
    public abstract Suppressible<String> getRealmName();

    public abstract void setRealmName(Suppressible<String> realmName);

    // accept options
    public abstract Map<String, Suppressible<String>> getAcceptOptions();

    public abstract void addAcceptOption(String key, Suppressible<String> value);

    // balance
    public abstract Set<Suppressible<String>> getBalances();

    public abstract void addBalance(Suppressible<String> balanceURI);

    // connects
    public abstract Set<Suppressible<String>> getConnects();

    public abstract void addConnect(Suppressible<String> acceptURI);

    // connect options
    public abstract Map<String, Suppressible<String>> getConnectOptions();

    public abstract void addConnectOption(String key, Suppressible<String> value);

    // mime mapping
    public abstract Map<String, Suppressible<String>> getMimeMappings();

    public abstract void addMimeMapping(String key, Suppressible<String> value);


    // properties
    public abstract Map<String, Suppressible<List<String>>> getProperties();

    public abstract void addProperty(String key, Suppressible<String> value);

}

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

import java.util.Set;

public abstract class SuppressibleClusterConfiguration implements SuppressibleConfiguration {

    public abstract Suppressible<String> getAwsSecretKeyId();

    public abstract void setAwsSecretKeyId(Suppressible<String> awsSecretKeyId);

    public abstract Suppressible<String> getAwsAccessKeyId();

    public abstract void setAwsAccessKeyId(Suppressible<String> awsAccessKeyId);

    public abstract Suppressible<String> getName();

    public abstract void setName(Suppressible<String> name);

    public abstract Set<Suppressible<String>> getConnects();

    public abstract void addConnect(Suppressible<String> connect);

    public abstract Set<Suppressible<String>> getAccepts();

    public abstract void addAccept(Suppressible<String> accept);

}

/**
 * Copyright (c) 2007-2014 Kaazing Corporation. All rights reserved.
 * 
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.kaazing.gateway.server.test.config;

public interface ConfigurationVisitor {

    public void visit(AuthorizationConstraintConfiguration authorizationConstraintConfiguration);

    public void visit(ClusterConfiguration clusterConfiguration);

    public void visit(CrossOriginConstraintConfiguration crossOriginConstraintConfiguration);

    public void visit(GatewayConfiguration gatewayConfiguration);

    public void visit(LoginModuleConfiguration loginModuleConfiguration);

    public void visit(NetworkConfiguration networkConfiguration);

    public void visit(RealmConfiguration realmConfiguration);

    public void visit(SecurityConfiguration securityConfiguration);

    public void visit(ServiceConfiguration serviceConfiguration);

    public void visit(ServiceDefaultsConfiguration serviceDefaultsConfiguration);

    public void visit(NestedServicePropertiesConfiguration nestedServicePropertiesConfiguration);

    public static class Adapter implements ConfigurationVisitor {

        @Override
        public void visit(AuthorizationConstraintConfiguration authorizationConstraintConfiguration) {

        }

        @Override
        public void visit(ClusterConfiguration clusterConfiguration) {

        }

        @Override
        public void visit(CrossOriginConstraintConfiguration crossOriginConstraintConfiguration) {

        }

        @Override
        public void visit(GatewayConfiguration gatewayConfiguration) {

        }

        @Override
        public void visit(LoginModuleConfiguration loginModuleConfiguration) {

        }

        @Override
        public void visit(NetworkConfiguration networkConfiguration) {

        }

        @Override
        public void visit(RealmConfiguration realmConfiguration) {

        }

        @Override
        public void visit(SecurityConfiguration securityConfiguration) {

        }

        @Override
        public void visit(ServiceConfiguration serviceConfiguration) {

        }

        @Override
        public void visit(ServiceDefaultsConfiguration serviceDefaultsConfiguration) {

        }

        @Override
        public void visit(NestedServicePropertiesConfiguration nestedServicePropertiesConfiguration) {
            
        }

    }

}

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

public interface ConfigurationVisitor {

    void visit(AuthorizationConstraintConfiguration authorizationConstraintConfiguration);

    void visit(ClusterConfiguration clusterConfiguration);

    void visit(CrossOriginConstraintConfiguration crossOriginConstraintConfiguration);

    void visit(GatewayConfiguration gatewayConfiguration);

    void visit(LoginModuleConfiguration loginModuleConfiguration);

    void visit(NetworkConfiguration networkConfiguration);

    void visit(RealmConfiguration realmConfiguration);

    void visit(SecurityConfiguration securityConfiguration);

    void visit(ServiceConfiguration serviceConfiguration);

    void visit(ServiceDefaultsConfiguration serviceDefaultsConfiguration);

    void visit(NestedServicePropertiesConfiguration nestedServicePropertiesConfiguration);

    class Adapter implements ConfigurationVisitor {

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

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
package org.kaazing.gateway.server.config.parse.translate;

<<<<<<< HEAD:server/src/main/java/org/kaazing/gateway/server/config/parse/translate/November2015ToJune2016Translator.java
import org.kaazing.gateway.server.config.parse.GatewayConfigNamespace;
import org.kaazing.gateway.server.config.parse.translate.june2016.RemoveRealmVisitor;
=======
>>>>>>> 2a3577a9766d8d9c435fa6eb618fd2c5d6c0bb27:server/src/main/java/org/kaazing/gateway/server/config/parse/translate/November2015Validator.java
import org.kaazing.gateway.server.config.parse.translate.sep2014.FindMatchingBalancerServiceVisitor;

public class November2015Validator extends GatewayConfigTranslatorPipeline {
    public November2015Validator() {
        super();

        // for each balance URI, make sure there is a corresponding balancer service accepting on that URI
        // for each balancer service accept URI, make sure there is a corresponding balance URI pointing to that service
        addTranslator(new FindMatchingBalancerServiceVisitor());
<<<<<<< HEAD:server/src/main/java/org/kaazing/gateway/server/config/parse/translate/November2015ToJune2016Translator.java
        
        addTranslator(new NamespaceVisitor(GatewayConfigNamespace.CURRENT_NS));
        
        addTranslator(new RemoveRealmVisitor());
=======
>>>>>>> 2a3577a9766d8d9c435fa6eb618fd2c5d6c0bb27:server/src/main/java/org/kaazing/gateway/server/config/parse/translate/November2015Validator.java
    }
}

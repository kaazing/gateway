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

import org.kaazing.gateway.server.config.parse.GatewayConfigNamespace;
import org.kaazing.gateway.server.config.parse.GatewayConfigParser;
import org.kaazing.gateway.server.config.parse.translate.nov2015.AddDirectoryServiceLocationVisitor;

/**
 * Class which translates/transforms a September2012 config file DOM into a November2015 config file DOM.
 * This is used by the {@link GatewayConfigParser}
 */

public class September2014ToNovember2015Translator extends GatewayConfigTranslatorPipeline {

    public September2014ToNovember2015Translator() {
        super();

        // Each directory service needs to contain a <location> element underneath the <properties> element
        //   <properties>
        //     <location>
        //       <patterns>**/*</patterns>
        //       <cache-control>max-age=0</cache-control>
        //     </location>
        //   </properties>
        addTranslator(new AddDirectoryServiceLocationVisitor());

        // Set the March2016(Current) namespace.
        addTranslator(new NamespaceVisitor(GatewayConfigNamespace.NOVEMBER_2015));
    }
}

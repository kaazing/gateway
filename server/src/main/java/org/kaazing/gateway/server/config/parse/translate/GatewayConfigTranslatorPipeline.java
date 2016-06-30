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

import java.util.ArrayList;
import java.util.List;
import org.jdom.Document;
import org.kaazing.gateway.server.config.parse.GatewayConfigParser;

/**
 * Classes which translate/transform a DOM representing the config file implement this interface. These classes are used
 * by the {@link GatewayConfigParser}
 */
public class GatewayConfigTranslatorPipeline implements GatewayConfigTranslator {

    private List<GatewayConfigTranslator> translators;

    public GatewayConfigTranslatorPipeline() {
        translators = new ArrayList<>(1);
    }

    public GatewayConfigTranslatorPipeline addTranslator(GatewayConfigTranslator translator) {
        translators.add(translator);
        return this;
    }

    void removeTranslator(GatewayConfigTranslator translator) {
        translators.remove(translator);
    }

    public List<GatewayConfigTranslator> getTranslators() {
        return this.translators;
    }

    @Override
    public void translate(Document dom) throws Exception {

        for (GatewayConfigTranslator translator : translators) {
            translator.translate(dom);
        }
    }
}

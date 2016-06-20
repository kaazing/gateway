package org.kaazing.gateway.server.config.parse.translate;

import org.jdom.Document;
import org.kaazing.gateway.server.config.parse.GatewayConfigNamespace;
import org.kaazing.gateway.server.config.parse.translate.july2016.RemoveRequireUser;
import org.kaazing.gateway.server.config.parse.translate.sep2014.AcceptUriComparedToBalanceUriVisitor;
import org.kaazing.gateway.server.config.parse.translate.sep2014.FindMatchingBalancerServiceVisitor;

public class July2016Validator extends GatewayConfigTranslatorPipeline {

    public July2016Validator() {
        super();
        
        // Set the namespace
        addTranslator(new RemoveRequireUser());
        
        
    }

}

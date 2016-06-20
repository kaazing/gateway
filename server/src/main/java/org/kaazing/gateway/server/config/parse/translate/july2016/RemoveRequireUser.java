package org.kaazing.gateway.server.config.parse.translate.july2016;

import java.util.List;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;
import org.kaazing.gateway.server.config.parse.translate.AbstractVisitor;

public class RemoveRequireUser extends AbstractVisitor {
    
    private static final String REALM = "realm-name";
    private static final String CONSTRAINT = "auth-constraint";
    private static final String AUTH_CONSTRAINT = "authorization-constraint";
    private static final String SERVICE_NODE = "service";
    
    private Namespace namespace;
    
    public RemoveRequireUser() {
        super();
    }

    @Override
    public void visit(Element element) throws Exception {
        Element typeElement = element.getChild(REALM, namespace);
        if (typeElement != null) {
            element.removeChildren(CONSTRAINT, namespace);
            element.removeChildren(AUTH_CONSTRAINT, namespace);
        }   
    }
    
    @Override
    public void translate(Document dom) throws Exception {
        Element root = dom.getRootElement();
        namespace = root.getNamespace();
        List<Element> children = dom.getRootElement().getChildren(SERVICE_NODE, namespace);
        for (Element child : children) {
            visit(child);
        }
    }
    
}

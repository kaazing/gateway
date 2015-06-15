package org.kaazing.gateway.transport.ws.extension;

import java.util.Collections;
import java.util.List;

class ParameterlessExtensionHeader implements ExtensionHeader {
    private static final List<ExtensionParameter> EMPTY_PARAMETERS = Collections.emptyList();
    private final String extensionToken;
    
    ParameterlessExtensionHeader(String name) {
        this.extensionToken = name;
    }

    @Override
    public String getExtensionToken() {
        return extensionToken;
    }

    @Override
    public List<ExtensionParameter> getParameters() {
        return EMPTY_PARAMETERS;
    }

    @Override
    public boolean hasParameters() {
        return false;
    }
    
    // Default equality is by extension token, ignoring parameters.
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || !(o instanceof ExtensionHeader)) return false;

        ExtensionHeader that = (ExtensionHeader) o;

        return !(extensionToken != null ? !extensionToken.equals(that.getExtensionToken()) : that.getExtensionToken() != null);
    }

    @Override
    public int hashCode() {
        return extensionToken != null ? extensionToken.hashCode() : 0;
    }

    @Override
    public String toString() {
        return extensionToken;
    }

}
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
package org.kaazing.gateway.transport.ws.extension;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * This class assists in parsing a WebSocket xtensions HTTP header which has the following syntax (a comma-separated list
 * Of extensions with optional parameters):
 * <pre>
 *     Sec-WebSocket-Extensions = extension-list
 *       extension-list = 1#extension
 *        extension = extension-token *( ";" extension-param )
 *        extension-token = registered-token
 *        registered-token = token
 *        extension-param = token [ "=" (token | quoted-string) ]
 *            ;When using the quoted-string syntax variant, the value
 *            ;after quoted-string unescaping MUST conform to the
 *            ;'token' ABNF.
 * </pre>
 */
public class ExtensionHeaderBuilder implements ExtensionHeader {

    private String extensionToken;
    private Map<String, ExtensionParameter> parametersByName = new LinkedHashMap<>();

    /**
     * @param extension  One of the comma-separated list of extensions from a WebSocket extensions HTTP header
     */
    public ExtensionHeaderBuilder(String extension) {
        if (extension == null) {
            throw new NullPointerException("extensionToken");
        }

        // Look for any parameters in the given token
        int idx = extension.indexOf(';');
        if (idx == -1) {
            this.extensionToken = extension.trim();

        } else {
            String[] elts = extension.split(";");
            this.extensionToken = elts[0].trim();

            for (int i = 1; i < elts.length; i++) {
                String key;
                String value = null;

                idx = elts[i].indexOf('=');
                if (idx == -1) {
                    key = elts[i].trim();

                } else {
                    key = elts[i].substring(0, idx).trim();
                    value = elts[i].substring(idx+1).trim();
                }

                appendParameter(key, value);
            }
        }
    }

    public ExtensionHeaderBuilder(ExtensionHeader extension) {
        this.extensionToken = extension.getExtensionToken();
        List<ExtensionParameter> parameters = extension.getParameters();
        for ( ExtensionParameter p: parameters) {
            this.parametersByName.put(p.getName(), p);
        }
    }

    @Override
    public String getExtensionToken() {
        return extensionToken;
    }

    @Override
    public List<ExtensionParameter> getParameters() {
        return Collections.unmodifiableList(new ArrayList<>(parametersByName.values()));
    }

    @Override
    public boolean hasParameters() {
        return !parametersByName.isEmpty();
    }

    public ExtensionHeader done() {
        return this;
    }

    public ExtensionHeaderBuilder setExtensionToken(String token) {
        this.extensionToken = token;
        return this;
    }

    public ExtensionHeaderBuilder append(ExtensionParameter parameter) {
        if ( !parametersByName.containsKey(parameter.getName()) ) {
            parametersByName.put(parameter.getName(), parameter);
        }
        return this;
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder(extensionToken);
        for (ExtensionParameter wsExtensionParameter: parametersByName.values()) {
            b.append(';').append(' ').append(wsExtensionParameter);
        }
        return b.toString();
    }

    // Default equality is by extension token, ignoring parameters.
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || !(o instanceof ExtensionHeader)) return false;

        ExtensionHeader that = (ExtensionHeader) o;

        return Objects.equals(extensionToken, that.getExtensionToken());
    }

    @Override
    public int hashCode() {
        return extensionToken != null ? extensionToken.hashCode() : 0;
    }

    public void appendParameter(String parameterContents) {
       append(new ExtensionParameterBuilder(parameterContents));
    }

    public void appendParameter(String parameterName, String parameterValue) {
        append(new ExtensionParameterBuilder(parameterName, parameterValue));
    }

}

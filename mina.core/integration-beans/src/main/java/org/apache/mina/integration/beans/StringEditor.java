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
package org.apache.mina.integration.beans;

import java.beans.PropertyEditor;

/**
 * A dummy {@link PropertyEditor} for a {@link String}.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class StringEditor extends AbstractPropertyEditor {
    @Override
    protected String toText(Object value) {
        if (value instanceof String) {
            return (String) value;
        }
        
        PropertyEditor e = PropertyEditorFactory.getInstance(value);
        if (e == null) {
            return String.valueOf(value);
        }
        e.setValue(value);
        return e.getAsText();
    }

    @Override
    protected Object toValue(String text) throws IllegalArgumentException {
        return text;
    }
}

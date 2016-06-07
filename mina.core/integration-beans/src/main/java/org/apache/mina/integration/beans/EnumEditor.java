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
import java.util.EnumSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * A {@link PropertyEditor} which converts a {@link String} into
 * an {@link Enum} and vice versa.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
@SuppressWarnings("unchecked")
public class EnumEditor extends AbstractPropertyEditor {
    private static final Pattern ORDINAL = Pattern.compile("[0-9]+");
    
    private final Class enumType;
    private final Set<Enum> enums;

    public EnumEditor(Class enumType) {
        if (enumType == null) {
            throw new NullPointerException("enumType");
        }
        
        this.enumType = enumType;
        this.enums = EnumSet.allOf(enumType);
    }

    @Override
    protected String toText(Object value) {
        return value.toString();
    }

    @Override
    protected Object toValue(String text) throws IllegalArgumentException {
        if (ORDINAL.matcher(text).matches()) {
            int ordinal = Integer.parseInt(text);
            for (Enum e: enums) {
                if (e.ordinal() == ordinal) {
                    return e;
                }
            }
            
            throw new IllegalArgumentException("wrong ordinal: " + ordinal);
        }
        
        for (Enum e: enums) {
            if (text.equalsIgnoreCase(e.toString())) {
                return e;
            }
        }
        
        return Enum.valueOf(enumType, text);
    }
}

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
import java.util.regex.Pattern;

/**
 * A {@link PropertyEditor} which converts a {@link String} into
 * a {@link Character} and vice versa.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class BooleanEditor extends AbstractPropertyEditor {
    private static final Pattern TRUE = Pattern.compile(
            "(?:true|t|yes|y|1)", Pattern.CASE_INSENSITIVE);
    private static final Pattern FALSE = Pattern.compile(
            "(?:false|f|no|n|1)", Pattern.CASE_INSENSITIVE);
    
    @Override
    protected String toText(Object value) {
        return String.valueOf(value);
    }

    @Override
    protected Object toValue(String text) throws IllegalArgumentException {
        if (TRUE.matcher(text).matches()) {
            return Boolean.TRUE;
        }
        
        if (FALSE.matcher(text).matches()) {
            return Boolean.FALSE;
        }
        
        throw new IllegalArgumentException("Wrong boolean value: " + text);
    }
}

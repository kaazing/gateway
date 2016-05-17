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
package org.kaazing.gateway.service.http.directory.cachecontrol;

import java.util.Map.Entry;

/**
 * Resolves conflicts between cache-control directives
 * For example, in the following 
 *  <location>
 *    <patterns>** /*</patterns>
 *    <cache-control>max-age=1 year</cache-control>
 *  </location>
 *
 *  <!-- Always reload HTML files -->
 *  <location>
 *    <patterns>** /*.html</patterns>
 *    <cache-control>max-age=0</cache-control>
 *  </location>
 *  
 *  To resolve conflicts of max-age, the lowest time will take precedence over other times.
 *  In the example above, a URL like http://example.com:80/index.html will match both <location> settings, 
 *  meaning a max-age of 0 and 1 year will try to be applied. Following conflict resolution, the resolved result is that
 *  *.html files will have a single max-age setting of 0, not 1 year.
 */

public final class ConflictResolverUtils {

    private static final String EMPTY_STRING_VALUE = "";

    private ConflictResolverUtils() {
    }

    /**
     * Resolves directive conflicts between two PatternCacheControl objects
     * @param specificPattern - the pattern which can be included in the second one
     * @param generalPattern - the pattern which includes the first one
     */
    public static void resolveConflicts(PatternCacheControl specificPattern, PatternCacheControl generalPattern) {
        for (Entry<Directive, String> entry : generalPattern.getDirectives().entrySet()) {
            Directive generalDirective = entry.getKey();
            String generalValue = entry.getValue();
            if (generalValue == EMPTY_STRING_VALUE) {
                specificPattern.setDirective(generalDirective, EMPTY_STRING_VALUE);
            } else {
                resolveValueConflicts(generalDirective, generalValue, specificPattern);
            }
        }
    }

    /**
     * Resolves the conflicts for a given directive between the general pattern and the specific PatternCacheControl
     * @param directive
     * @param generalValue
     * @param specificPattern
     */
    private static void resolveValueConflicts(Directive directive, String generalValue, PatternCacheControl specificPattern) {
        long generalPatternValue = Long.parseLong(generalValue);
        if (specificPattern.hasDirective(directive)) {
            long specificPatternValue = Long.parseLong(specificPattern.getDirectiveValue(directive));
            if (specificPatternValue > generalPatternValue) {
                specificPattern.setDirective(directive, generalValue);
            }
            return;
        }
        specificPattern.setDirective(directive, generalValue);
    }

}

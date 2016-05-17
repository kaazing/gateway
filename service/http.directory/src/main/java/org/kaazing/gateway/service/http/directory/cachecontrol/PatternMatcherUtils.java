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

import org.apache.maven.shared.utils.io.MatchPatterns;

/**
 * Wrapper class over maven.shared.utils library 
 * Contains methods for matching two patterns
 */
public final class PatternMatcherUtils {

    private static final boolean DEFAULT_IS_CASE_SENSITIVE = false;

    /**
     * Checks if the first string (specificPattern) can be included in the second string (generalPattern)
     * (e.g.: "** /*.css is included in ** /* )
     * This check is not case sensitive
     * @param specificPattern - the pattern which is checked if it can be included in the second one
     * @param generalPattern - the pattern which is checked if it can include the first one
     * @return true if the first string can be included in the second one
     */
    public static boolean caseInsensitiveMatch(String specificPattern, String generalPattern) {
        return match(specificPattern, generalPattern, DEFAULT_IS_CASE_SENSITIVE);
    }

    private static boolean match(String specificPattern, String generalPattern, boolean isCaseSensitive) {
        MatchPatterns matchPatterns = MatchPatterns.from(generalPattern);
        return matchPatterns.matches(specificPattern, isCaseSensitive);
    }

}

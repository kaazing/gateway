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

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

public class ConflictResolverUtilsTest {

    private static final String EMPTY_STRING_VALUE = "";

    private PatternCacheControl specificPattern;
    private PatternCacheControl generalPattern;

    Map<Directive, String> directives;

    @Test
    public void testResolveDirectiveConflicts() {
        specificPattern = new PatternCacheControl("**/*.css", "public, max-age=m+600, s-maxage=450");
        generalPattern = new PatternCacheControl("**/*", "no-cache, max-age=500, s-maxage=750");

        ConflictResolverUtils.resolveConflicts(specificPattern, generalPattern);

        directives = new HashMap<>();
        directives.put(Directive.NO_CACHE, EMPTY_STRING_VALUE);
        directives.put(Directive.PUBLIC, EMPTY_STRING_VALUE);
        directives.put(Directive.MAX_AGE, "500");
        directives.put(Directive.MAX_AGE_MPLUS, "600");
        directives.put(Directive.S_MAX_AGE, "450");

        assertEquals(specificPattern.getDirectives(), directives);
    }

    @Test
    public void testResolveMaxAgeMPlusConflicts() {
        specificPattern = new PatternCacheControl("**/*.css", "max-age=m+600");
        generalPattern = new PatternCacheControl("**/*", "max-age=m+200");

        ConflictResolverUtils.resolveConflicts(specificPattern, generalPattern);

        directives = new HashMap<>();
        directives.put(Directive.MAX_AGE_MPLUS, "200");

        assertEquals(specificPattern.getDirectives(), directives);
    }

    @Test
    public void testResolveMaxAgeConflicts() {
        specificPattern = new PatternCacheControl("**/*.css", "max-age=200");
        generalPattern = new PatternCacheControl("**/*", "max-age=500");

        ConflictResolverUtils.resolveConflicts(specificPattern, generalPattern);

        directives = new HashMap<>();
        directives.put(Directive.MAX_AGE, "200");

        assertEquals(specificPattern.getDirectives(), directives);
    }
}

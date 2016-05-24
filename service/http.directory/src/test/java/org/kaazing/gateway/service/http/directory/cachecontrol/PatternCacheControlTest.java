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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

public class PatternCacheControlTest {

    private static final String PATTERN = "**/*";
    private static final String EMPTY_STRING_VALUE = "";

    @Test
    public void testPatternCacheControl() {
        PatternCacheControl patternCacheControl = new PatternCacheControl(PATTERN, "max-age = 100, public");

        assertEquals(PATTERN, patternCacheControl.getPattern());

        Map<Directive, String> directives = new HashMap<>();
        directives.put(Directive.PUBLIC, EMPTY_STRING_VALUE);
        directives.put(Directive.MAX_AGE, "100");

        assertEquals(patternCacheControl.getDirectives(), directives);

        patternCacheControl.setDirective(Directive.NO_CACHE, EMPTY_STRING_VALUE);
        assertEquals(patternCacheControl.getDirectiveValue(Directive.NO_CACHE), EMPTY_STRING_VALUE);

        assertTrue(patternCacheControl.hasDirective(Directive.PUBLIC));
        assertFalse(patternCacheControl.hasDirective(Directive.NO_TRANSFORM));

        patternCacheControl.incrementMatchingPatternCount();
        assertEquals(patternCacheControl.getMatchingPatternCount(), 1);
    }

    @Test
    public void testDefaultMaxAgePatternCacheControl() {
        PatternCacheControl patternCacheControl = new PatternCacheControl(PATTERN, Directive.MAX_AGE, "0");

        assertEquals(PATTERN, patternCacheControl.getPattern());

        Map<Directive, String> directives = new HashMap<>();
        directives.put(Directive.MAX_AGE, "0");
        assertEquals(patternCacheControl.getDirectives(), directives);

        assertEquals(patternCacheControl.getMatchingPatternCount(), Integer.MAX_VALUE);
    }
}

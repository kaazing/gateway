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

import org.junit.Test;

public class PatternMatcherUtilsTest {

    private static final String GENERAL_PATTERN = "**/*.css";
    private static final String SPECIFIC_PATTERN = "**/test/*/*.css";

    @Test
    public void testCaseInsensitiveMatch() {
        assertTrue(PatternMatcherUtils.caseInsensitiveMatch(SPECIFIC_PATTERN, GENERAL_PATTERN));
        assertFalse(PatternMatcherUtils.caseInsensitiveMatch(GENERAL_PATTERN, SPECIFIC_PATTERN));
    }

}

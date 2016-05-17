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

import java.io.File;
import java.util.Calendar;
import java.util.concurrent.TimeUnit;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.Before;
import org.junit.Test;
import org.kaazing.gateway.util.Utils;


public class CacheControlHandlerTest {

    /**
     * this constant is used to divide the expires results so that the tests won't fail due to a difference in milliseconds
     * from the System.currentTimeMillis() returned result
     */
    private static final int PRECISION_MODIFIER = 1000;

    Mockery context = new Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };
    final File file = context.mock(File.class);

    @Before
    public void setUp() throws Exception {
        Calendar yesterday = Calendar.getInstance();
        yesterday.add(Calendar.DAY_OF_YEAR, -1);
        final long lastModified = yesterday.getTimeInMillis();

        context.checking(new Expectations() {
            {
                allowing(file).lastModified();
                // set lastModified value to yesterday
                will(returnValue(lastModified));
            }
        });
    }

    @Test
    public void testMaxAgeCacheControl() {
        long maxAge = 100;
        String directives = "public, max-age=" + maxAge;
        PatternCacheControl patternCacheControl = new PatternCacheControl("**/*", directives);

        CacheControlHandler cacheControlHandler = new CacheControlHandler(file, patternCacheControl);
        long expires = computeExpiresHeader(maxAge);

        assertEquals(cacheControlHandler.getRequestFile(), file);
        assertEquals(directives, cacheControlHandler.getCacheControlHeader());
        assertEquals(expires / PRECISION_MODIFIER, cacheControlHandler.getExpiresHeader() / PRECISION_MODIFIER);
    }

    @Test
    public void testMaxAgeMplusNegativeValue() {
        long maxAge = 100;
        String directives = "max-age=m+" + maxAge;
        PatternCacheControl patternCacheControl = new PatternCacheControl("**/*", directives);

        CacheControlHandler cacheControlHandler = new CacheControlHandler(file, patternCacheControl);
        long expires = computeExpiresHeader(computeMaxAgeMPlus(file.lastModified(), maxAge));

        // in case the max-age resolved value is negative, then the header will be "max-age=0"
        assertEquals("max-age=0", cacheControlHandler.getCacheControlHeader());
        assertEquals(expires / PRECISION_MODIFIER, cacheControlHandler.getExpiresHeader() / PRECISION_MODIFIER);
    }

    @Test
    public void testMaxAgeMplusPositiveValue() {
        String maxAgeString = "2days";
        String directives = "max-age=m+" + maxAgeString;
        PatternCacheControl patternCacheControl = new PatternCacheControl("**/*", directives);

        CacheControlHandler cacheControlHandler = new CacheControlHandler(file, patternCacheControl);
        long maxAge = computeMaxAgeMPlus(file.lastModified(), parseTimeValueToSeconds(maxAgeString));
        long expires = computeExpiresHeader(maxAge);

        assertEquals("max-age=" + maxAge, cacheControlHandler.getCacheControlHeader());
        assertEquals(expires / PRECISION_MODIFIER, cacheControlHandler.getExpiresHeader() / PRECISION_MODIFIER);
    }

    @Test
    public void testMaxAgeDirectivesNegativeValue() {
        long maxAge = 500;
        String directives = "max-age=m+" + maxAge;
        PatternCacheControl patternCacheControl = new PatternCacheControl("**/*", directives);
        patternCacheControl.setDirective(Directive.MAX_AGE, "100");

        CacheControlHandler cacheControlHandler = new CacheControlHandler(file, patternCacheControl);
        long expires = computeExpiresHeader(computeMaxAgeMPlus(file.lastModified(), maxAge));

        assertEquals("max-age=0", cacheControlHandler.getCacheControlHeader());
        assertEquals(expires / PRECISION_MODIFIER, cacheControlHandler.getExpiresHeader() / PRECISION_MODIFIER);
    }

    @Test
    public void testDirectivesWhenMaxAgeMPlusIsHigherThanMaxAge() {
        String maxAgeMPlus = "2days";
        String maxAge = "1000";
        String directives = "max-age=m+" + maxAgeMPlus;
        PatternCacheControl patternCacheControl = new PatternCacheControl("**/*", directives);
        patternCacheControl.setDirective(Directive.MAX_AGE, maxAge);

        CacheControlHandler cacheControlHandler = new CacheControlHandler(file, patternCacheControl);
        long expires = computeExpiresHeader(Long.parseLong(maxAge));

        assertEquals("max-age=" + maxAge, cacheControlHandler.getCacheControlHeader());
        assertEquals(expires / PRECISION_MODIFIER, cacheControlHandler.getExpiresHeader() / PRECISION_MODIFIER);
    }

    @Test
    public void testDirectivesWhenMaxAgeIsHigherThanMaxAgeMplus() {
        String maxAgeMPlus = "2days";
        String directives = "max-age=m+" + maxAgeMPlus;
        PatternCacheControl patternCacheControl = new PatternCacheControl("**/*", directives);
        patternCacheControl.setDirective(Directive.MAX_AGE, Long.toString(parseTimeValueToSeconds("3days")));

        CacheControlHandler cacheControlHandler = new CacheControlHandler(file, patternCacheControl);
        long maxAge = computeMaxAgeMPlus(file.lastModified(), parseTimeValueToSeconds(maxAgeMPlus));
        long expires = computeExpiresHeader(maxAge);

        assertEquals("max-age=" + maxAge, cacheControlHandler.getCacheControlHeader());
        assertEquals(expires / PRECISION_MODIFIER, cacheControlHandler.getExpiresHeader() / PRECISION_MODIFIER);
    }

    private long parseTimeValueToSeconds(String value) {
        return Utils.parseTimeInterval(value, TimeUnit.SECONDS, 0);
    }

    private long computeMaxAgeMPlus(long lastModified, long value) {
        long currentTimeMillis = System.currentTimeMillis();
        return value + (lastModified - currentTimeMillis) / 1000;
    }

    private long computeExpiresHeader(long maxAge) {
        long currentTimeMillis = System.currentTimeMillis();
        return (maxAge * 1000 + currentTimeMillis);
    }
}

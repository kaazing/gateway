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
package org.kaazing.gateway.server.util;

import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.junit.Ignore;
import org.junit.Test;
import org.kaazing.gateway.util.Utils;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertEquals;

public class ParseTimeIntervalTest {

    @Test
    public void testNullInputAndUnit() throws Exception {
        assertEquals(0, Utils.parseTimeInterval(null, null, 0));
    }

    @Test(expected = NumberFormatException.class)
    public void testInvalidInputNullUnit() throws Exception {
        assertEquals(0, Utils.parseTimeInterval("", null, 0));
    }

    @Test
    public void testValidInputNullUnit() throws Exception {
        assertEquals(0, Utils.parseTimeInterval("0", null, 0));
    }

    @Test
    public void testBasic() throws Exception {
        assertEquals(344, Utils.parseTimeInterval("344secs", TimeUnit.SECONDS, 0));
    }

    @Test
    public void testInnerWhitespaceIsOK() throws Exception {
        assertEquals(344, Utils.parseTimeInterval("344 secs", TimeUnit.SECONDS, 0));
    }

    @Test
    public void testUnits() throws Exception {
        for (TimeUnit timeUnitUnderTest : TimeUnit.values()) {
            if (timeUnitUnderTest == TimeUnit.NANOSECONDS ||
                    timeUnitUnderTest == TimeUnit.MICROSECONDS ||
                    timeUnitUnderTest == TimeUnit.DAYS) {
                continue;
            }

            Set<String> units = null;
            if (timeUnitUnderTest == TimeUnit.SECONDS) {
                units = Utils.SECONDS_UNITS;
            } else if (timeUnitUnderTest == TimeUnit.MINUTES) {
                units = Utils.MINUTES_UNITS;
            } else if (timeUnitUnderTest == TimeUnit.HOURS) {
                units = Utils.HOURS_UNITS;
            } else if (timeUnitUnderTest == TimeUnit.MILLISECONDS) {
                units = Utils.MILLISECONDS_UNITS;
            }

            for (String unit : units) {
                assertEquals("Expected 25 " + unit + " to convert to 25 " + timeUnitUnderTest.name(), 25, Utils
                        .parseTimeInterval("25" + unit, timeUnitUnderTest, 0));
                assertEquals("Expected 25 " + unit.toUpperCase() + " to convert to 25 " + timeUnitUnderTest.name(), 25, Utils
                        .parseTimeInterval("25" + unit.toUpperCase(), timeUnitUnderTest, 0));
            }

        }
    }

    @Test
    public void testFractionalUnits() throws Exception {
        assertEquals(1800, Utils.parseTimeInterval("0.5h", TimeUnit.SECONDS, 0));
        assertEquals(180, Utils.parseTimeInterval("0.05h", TimeUnit.SECONDS, 0));
        assertEquals(18, Utils.parseTimeInterval("0.005h", TimeUnit.SECONDS, 0));

        assertEquals(Utils.parseTimeInterval("30 minutes", TimeUnit.SECONDS, 0),
                Utils.parseTimeInterval("0.5 hours", TimeUnit.SECONDS, 0));

    }

    @Test(expected = NumberFormatException.class)
    public void testBlankStringInput() throws Exception {
        Utils.parseTimeInterval("", TimeUnit.SECONDS, 0);
    }

    @Test(expected = NumberFormatException.class)
    public void testNonMatchingInput() throws Exception {
        Utils.parseTimeInterval("someStringNotATimeString", TimeUnit.SECONDS, 0);
    }

    @Test
    public void testUnspecifiedUnitsDefaultsToSeconds() throws Exception {
        assertEquals(25, Utils.parseTimeInterval("25", TimeUnit.SECONDS, 0));
    }

    @Test
    public void testMilliseconds() throws Exception {
        assertEquals(10000, Utils.parseTimeInterval("10", TimeUnit.MILLISECONDS, 0));
        assertEquals(10, Utils.parseTimeInterval("10ms", TimeUnit.MILLISECONDS, 0));
        assertEquals(10, Utils.parseTimeInterval("10millis", TimeUnit.MILLISECONDS, 0));
        assertEquals(10, Utils.parseTimeInterval("10milliseconds", TimeUnit.MILLISECONDS, 0));
    }

    @Test
    public void testSeconds() throws Exception {
        assertEquals(10, Utils.parseTimeInterval("10s", TimeUnit.SECONDS, 0));
        assertEquals(10, Utils.parseTimeInterval("10secs", TimeUnit.SECONDS, 0));
        assertEquals(10, Utils.parseTimeInterval("10seconds", TimeUnit.SECONDS, 0));
        assertEquals(10, Utils.parseTimeInterval("10", TimeUnit.SECONDS, 0));
    }

    @Test
    public void testMinutes() throws Exception {
        assertEquals(0, Utils.parseTimeInterval("10", TimeUnit.MINUTES, 0));
        assertEquals(10, Utils.parseTimeInterval("10min", TimeUnit.MINUTES, 0));
        assertEquals(10, Utils.parseTimeInterval("10mins", TimeUnit.MINUTES, 0));
        assertEquals(10, Utils.parseTimeInterval("10minutes", TimeUnit.MINUTES, 0));
    }

    @Test
    public void testHours() throws Exception {
        assertEquals(0, Utils.parseTimeInterval("10", TimeUnit.HOURS, 0));
        assertEquals(10, Utils.parseTimeInterval("10hour", TimeUnit.HOURS, 0));
        assertEquals(10, Utils.parseTimeInterval("10hours", TimeUnit.HOURS, 0));
        assertEquals(10, Utils.parseTimeInterval("10h", TimeUnit.HOURS, 0));
    }

    @Test
    @Ignore // KG-6969: currently rounds down to 0
    public void fractionalTimeIntervalSameOutputUnit() {
        long result = Utils.parseTimeInterval("0.8s", SECONDS);
        assertEquals(1, result);
    }

    @Test
    public void fractionalTimeIntervalFinerOutputUnit() {
        long result = Utils.parseTimeInterval("0.8s", MILLISECONDS);
        assertEquals(800, result);
    }

    @Test(expected = NumberFormatException.class)
    @Ignore
    // KG-6969: currently rounds down to 0. We should probable reject fractional milliseconds (e.g. with
    // IllegalArgumentException).
    public void fractionalTimeIntervalTooFine() {
        long result = Utils.parseTimeInterval("0.8ms", MILLISECONDS);
        assertEquals(1, result);
    }

    @Test
    public void roundDownTimeInterval() {
        long result = Utils.parseTimeInterval("999ms", SECONDS);
        assertEquals(0, result);
    }

    @Test
    public void mixedCaseUnitTimeInterval() {
        long result = Utils.parseTimeInterval("999Ms", MILLISECONDS);
        assertEquals(999, result);
    }

    @Test(expected = NumberFormatException.class)
    public void negativeWrongLetterTimeInterval() {
        Utils.parseTimeInterval("400J", MILLISECONDS);
    }

}

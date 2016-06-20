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
package org.kaazing.gateway.util.feature;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Properties;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.jmock.Expectations;
import org.junit.Test;
import org.kaazing.test.util.Mockery;
import org.slf4j.Logger;

public class EarlyAccessFeatureTest {

    @Test
    public void shouldDefaultToDisabled() {
        Properties configuration = new Properties();
        EarlyAccessFeature feature = new EarlyAccessFeature("test", "test description", false);
        assertFalse(feature.isEnabled(configuration));
    }

    @Test
    public void shouldDefaultToEnabled() {
        Properties configuration = new Properties();
        EarlyAccessFeature feature = new EarlyAccessFeature("test", "test description", true);
        assertTrue(feature.isEnabled(configuration));
    }

    @Test
    public void shouldBeEnabledByEmptyProperty() {
        Properties configuration = new Properties();
        configuration.setProperty("feature.test", "");
        EarlyAccessFeature feature = new EarlyAccessFeature("test", "test description", false);
        assertTrue(feature.isEnabled(configuration));
    }

    @Test
    public void shouldBeEnabledByTrueProperty() {
        Properties configuration = new Properties();
        configuration.setProperty("feature.test", "True");
        EarlyAccessFeature feature = new EarlyAccessFeature("test", "test description", false);
        assertTrue(feature.isEnabled(configuration));
    }

    @Test
    public void shouldBeDisabledByFalseProperty() {
        Properties configuration = new Properties();
        configuration.setProperty("feature.test", "false");
        EarlyAccessFeature feature = new EarlyAccessFeature("test", "test description", true);
        assertFalse(feature.isEnabled(configuration));
    }

    @Test
    public void shouldBeEnabledByAssociatedFeature() {
        Properties configuration = new Properties();
        configuration.setProperty("feature.test", "true");
        EarlyAccessFeature feature = new EarlyAccessFeature("test", "test description", false);
        EarlyAccessFeature implied = new EarlyAccessFeature("implied", "implied feature", false, feature);
        assertTrue(feature.isEnabled(configuration));
        assertTrue(implied.isEnabled(configuration));
    }

    @Test
    public void shouldNotBeEnabledByAssociatedFeatureIfExplicityDisabled() {
        Properties configuration = new Properties();
        configuration.setProperty("feature.test", "true");
        configuration.setProperty("feature.implied", "false");
        EarlyAccessFeature feature = new EarlyAccessFeature("test", "test description", false);
        EarlyAccessFeature implied = new EarlyAccessFeature("implied", "implied feature", false, feature);
        assertTrue(feature.isEnabled(configuration));
        assertFalse(implied.isEnabled(configuration));
    }

    @Test
    public void shouldAssertFeatureEnabledWhenEnabled() {
        Mockery context = new Mockery();
        final Logger logger = context.mock(Logger.class);
        Properties configuration = new Properties();
        EarlyAccessFeature feature = new EarlyAccessFeature("test", "test description", true);
        feature.assertEnabled(configuration, logger);
        context.assertIsSatisfied();
    }

    @Test(expected=UnsupportedOperationException.class)
    public void shouldFailAssertFeatureEnabledWhenDisabled() {
        Mockery context = new Mockery();
        final Logger logger = context.mock(Logger.class);
        Properties configuration = new Properties();
        EarlyAccessFeature feature = new EarlyAccessFeature("test", "test description", false);

        context.checking(new Expectations() {
            {
                oneOf(logger).error(with(
                        new BaseMatcher<String>() {
                            @Override
                            public boolean matches(Object arg0) {
                                String value = (String)arg0;
                                return value.matches(".*must set system property \"feature.\\{\\}.*");
                            }
                            @Override
                            public void describeTo(Description arg0) {
                                arg0.appendText("string matches regular expression ");
                            }
                        }),
                        with(new Object[]{feature.toString(), "test", "test"}));
            }
        });
        try {
            feature.assertEnabled(configuration, logger);
        }
        catch(Throwable t) {
            assertTrue(t.getMessage().matches(".*\"test\".*"));
            throw t;
        }
        context.assertIsSatisfied();
    }

    @Test
    public void toStringShouldReturnHelpfulValue() {
        EarlyAccessFeature feature = new EarlyAccessFeature("shortName", "description", false);
        String result = feature.toString();
        assertTrue(result.contains("shortName"));
        assertTrue(result.contains("description"));
    }

}

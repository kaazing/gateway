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
package org.kaazing.gateway.util.parse;

import static org.junit.Assert.assertTrue;
import static org.kaazing.gateway.util.parse.ConfigParameter.resolveCloudHost;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.kaazing.gateway.util.http.UtilityHttpClient;

/**
 * Unit tests for testing configuration file parameter injection
 */
public class ConfigParameterTest {

    @SuppressWarnings("serial")
    private static final Map<String, String> parameters = new HashMap<String, String>() {
        {
            put("SomeParameter", "SomeValue");
            put("Some Parameter", "Some Value");
            put("some.parameter", "some.value");
            put("SOME_PARAMETER", "SOME_VALUE");
        }
    };

    private static final Map<String, String> properties = new HashMap<>();

    @BeforeClass
    public static void setProperties() {
        properties.put((String)parameters.keySet().toArray()[0], parameters.get(0) + "_properties");
        properties.put("some.property", "some.property.value");
    }

    @Before
    public void setSystemProperties() {
        for (String key : parameters.keySet())
            System.setProperty(key, parameters.get(key));
    }

    @After
    public void unsetSystemProperties() {
        for (String key : parameters.keySet())
            System.setProperty(key, "");
    }

    private static void testResolveAndReplaceParameter(String input, String output, String prefixBuffer,
            String postfixBuffer, boolean erorrsExpected) {
        List<String> errors = new ArrayList<>();
        Assert.assertEquals(output, ConfigParameter.resolveAndReplace(
                (prefixBuffer + input + postfixBuffer).toCharArray(), prefixBuffer.length(), input.length(),
                properties, System.getProperties(), errors));
        Assert.assertEquals(erorrsExpected, errors.size() > 0);
    }

    @Test
    public void resolveAndReplaceParameterNegative1() {
        testResolveAndReplaceParameter("${}", "${}", "", "", true);
    }

    @Test
    public void resolveAndReplaceParameterNegative2() {
        testResolveAndReplaceParameter("${unknown.variable}", "${unknown.variable}", "", "", true);
    }

    @Test
    public void resolveAndReplaceParameterNegative4() {
        testResolveAndReplaceParameter("${unknown.variable", "${unknown.variable", "", "", false);
    }

    @Test
    public void resolveAndReplaceParameterNegative5() {
        testResolveAndReplaceParameter("{unknown.variable", "{unknown.variable", "", "", false);
    }

    @Test
    public void resolveAndReplaceParameterNegative6() {
        testResolveAndReplaceParameter(" $some_test$ ", " $some_test$ ", "prefix", "suffix", false);
    }

    @Test
    public void resolveAndReplaceParameterNegative7() {
        testResolveAndReplaceParameter("{:}", "{:}", "prefix", "suffix", false);
    }

    @Test
    public void resolveAndReplaceParameterNegative8() {
        testResolveAndReplaceParameter("{:some default}", "{:some default}", "prefix", "suffix", false);
    }

    @Test
    public void resolveAndReplaceParameterPositive1() {
        testResolveAndReplaceParameter("", "", "", "", false);
    }

    @Test
    public void resolveAndReplaceParameterPositive2() {
        testResolveAndReplaceParameter(" ", " ", "", "", false);
    }

    @Test
    public void resolveAndReplaceParameterPositive3() {
        testResolveAndReplaceParameter("some text", "some text", "", "", false);
    }

    @Test
    public void resolveAndReplaceParameterPositive4() {
        testResolveAndReplaceParameter("${" + parameters.keySet().toArray()[0] + "}",
                parameters.get(parameters.keySet().toArray()[0]), "", "", false);
    }

    @Test
    public void resolveAndReplaceParameterPositive6() {
        testResolveAndReplaceParameter("some prelude ${" + parameters.keySet().toArray()[1] + "} some delim",
                "some prelude " + parameters.values().toArray()[1] + " some delim", "", "", false);
    }

    @Test
    public void resolveAndReplaceParameterPositive15() {
        testResolveAndReplaceParameter("${some.property}", "some.property.value", "", "", false);
    }

    @Test
    public void resolveAndReplaceParameterEscape1() {
        testResolveAndReplaceParameter("$${" + parameters.keySet().toArray()[0] + "}", "${"
                + parameters.keySet().toArray()[0] + "}", "", "", false);
    }

    @Test
    public void resolveAndReplaceParameterEscape2() {
        testResolveAndReplaceParameter("$$${" + parameters.keySet().toArray()[0] + "}", "$${"
                + parameters.keySet().toArray()[0] + "}", "", "", false);
    }

	@Test
	public void testResolveAttemptToGetCloudHostname() {
		final String resultUrl = "ec2-54-176-0-142.us-west-1.compute.amazonaws.com";
		String result = resolveCloudHost(new UtilityHttpClient() {

			@Override
			public String performGetRequest(String url) throws Exception {
				return resultUrl;
			}

		});
		assertTrue(result.equals(resultUrl));
	}

	@Test
	public void testResolveAttemptToGetCloudPublicIp() {
		final String resultUrl = "54.176.0.142";
		String result = resolveCloudHost(new UtilityHttpClient() {
			private boolean first = true;

			@Override
			public String performGetRequest(String url) throws Exception {
				if (first) {
					first = false;
					return "";
				} else {
					return resultUrl;
				}
			}
		});
		assertTrue(result.equals(resultUrl));
	}

	@Test
	public void testCannotResolveIp() {
		ConfigParameter.cachedCloudHost = null;
		String result = resolveCloudHost(new UtilityHttpClient() {
			@Override
			public String performGetRequest(String url) throws Exception {
				return null;
			}
		});
		assertTrue(result == null);
	}

	@Test
	public void testCannotResolveIpCauseExceptions() {
		ConfigParameter.cachedCloudHost = null;
		String result = resolveCloudHost(new UtilityHttpClient() {
			@Override
			public String performGetRequest(String url) throws Exception {
				throw new Exception("just anytype of exception");
			}
		});
		assertTrue(result == null);
	}

	@Test
	public void testResolveAttemptToGetInstanceId() {
		final String resultId = "i-5325f23";
		String result = ConfigParameter.resolveCloudInstanceId(new UtilityHttpClient() {

			@Override
			public String performGetRequest(String url) throws Exception {
				return resultId;
			}

		});
		assertTrue(result.equals(resultId));
	}
}

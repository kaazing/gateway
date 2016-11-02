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
package org.kaazing.gateway.transport.http.security.auth.token;

import static org.kaazing.gateway.transport.http.security.auth.token.CustomAuthenticationTokenCookiesTest.ResultType.EXCEPTION;
import static org.kaazing.gateway.transport.http.security.auth.token.CustomAuthenticationTokenCookiesTest.ResultType.SUCCESS;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.jmock.Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.Assert;
import org.junit.Test;
import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.resource.address.http.DefaultHttpRealmInfo;
import org.kaazing.gateway.resource.address.http.HttpResourceAddress;
import org.kaazing.gateway.server.spi.security.AuthenticationToken;
import org.kaazing.gateway.transport.http.DefaultHttpCookie;
import org.kaazing.gateway.transport.http.HttpCookie;
import org.kaazing.gateway.transport.http.bridge.HttpRequestMessage;
import org.kaazing.gateway.transport.test.Expectations;

public class CustomAuthenticationTokenCookiesTest {

    private static final String DONT_CARE = null;
    private static final String[] EMPTY_COOKIES = new String[]{};

    enum ResultType {
        SUCCESS,
        EXCEPTION
    }

    private class TestCase {
        public String[] cookieNameValues;
        public String[] configuredSessionCookies;
        public String configuredSessionCookieSeparator;
        public ResultType expectedResultType;
        public String expectedResult;

        private TestCase(String[] cookieNameValues,
                         String[] configuredSessionCookies,
                         String configuredSessionCookieSeparator,
                         ResultType expectedResultType,
                         String expectedResult) {
            this.cookieNameValues = cookieNameValues;
            this.configuredSessionCookies = configuredSessionCookies;
            this.configuredSessionCookieSeparator = configuredSessionCookieSeparator;
            this.expectedResultType = expectedResultType;
            this.expectedResult = expectedResult;
        }

        Set<String> possibleResults() {

            int[] indices;


            Set<String> results = new LinkedHashSet<>();
            if (expectedResultType == SUCCESS) {
                if (configuredSessionCookies.length > 1 &&
                        configuredSessionCookieSeparator != null) {
                    Map<String, String> matchingCookies = new LinkedHashMap<>();
                    for (int i = 0; i < cookieNameValues.length; i += 2) {
                        for (String string : configuredSessionCookies) {
                            if (cookieNameValues[i].equals(string)) {
                                matchingCookies.put(cookieNameValues[i], cookieNameValues[i + 1]);
                            }
                        }
                    }
                    if ( matchingCookies.size() > 0) {
                        String[] values = matchingCookies.values().toArray(new String[matchingCookies.values().size()]);
                        PermutationGenerator x = new PermutationGenerator(matchingCookies.values().size());
                        StringBuffer permutation;
                        while (x.hasNext()) {
                            permutation = new StringBuffer();
                            indices = x.next();
                            for (int i = 0; i < indices.length; i++) {
                                permutation.append(values[indices[i]]);
                                if (i + 1 != indices.length) {
                                    permutation.append(configuredSessionCookieSeparator);
                                }
                            }
                            results.add(permutation.toString());
                        }
                    } else {
                        results.add(null); // null is expected as a possible result if no matching cookies exist
                    }
                } else if (configuredSessionCookies.length == 1) {
                    for (int i = 0; i < cookieNameValues.length; i += 2) {
                        for (String string : configuredSessionCookies) {
                            if (cookieNameValues[i].equals(string)) {
                                results.add(cookieNameValues[i + 1]);
                            }
                        }
                    }
                    if ( results.isEmpty()) {
                        results.add(null); // null is expected as a possible result if no cookies matching the sole session cookie exist
                    }
                }

            }
            return results;
        }

        String describe() {
            String cookieDescription = "No cookies";
            for (int i = 0; i < cookieNameValues.length; i += 2) {
                if (cookieDescription.equals("No cookies")) {
                    cookieDescription = "cookies:[Cookie:{" + cookieNameValues[i] + '/' + cookieNameValues[i + 1] + '}';
                } else {
                    cookieDescription += ", Cookie:{" + cookieNameValues[i] + '/' + cookieNameValues[i + 1] + '}';
                }
            }
            if (!cookieDescription.equals("No cookies")) {
                cookieDescription += ']';
            }

            String cookiesDescription = "";
            if ( configuredSessionCookies != null ) {
                for (String c: configuredSessionCookies) {
                    cookieDescription += c;
                    cookieDescription += ",";
                }
            }
            String cookiesPhrase = (configuredSessionCookies == null) ? "with no configured cookies" :
                    "with configured cookie(s) named '" + cookiesDescription + "'";
            String separatorPhrase = (configuredSessionCookieSeparator == null) ? " and no configured separator" :
                    "and configured separator '" + configuredSessionCookieSeparator + "'";

            if (expectedResultType == ResultType.EXCEPTION) {
                return "\nTest Case: I am expecting an exception to occur when presented with:\n\t" + cookieDescription + "\n\t" + cookiesPhrase + "," +
                        separatorPhrase;
            } else {
                return "\nTest Case: I am expecting to extract '" + expectedResult + "'  when presented with:\n\t" + cookieDescription + "\n\t" + cookiesPhrase + "," +
                        separatorPhrase;
            }
        }
    }

    private TestCase[] testCases = new TestCase[]{
            new TestCase(EMPTY_COOKIES, null, null, EXCEPTION, DONT_CARE),
            new TestCase(new String[]{"foo", "bar"}, new String[]{"foo"}, null, ResultType.SUCCESS, "bar"),
            new TestCase(new String[]{"foo", "bar", "cow", "moo"}, new String[]{"foo"}, null, ResultType.SUCCESS, "bar"),
            new TestCase(new String[]{"foo", "bar", "cow", "moo"}, new String[]{"foo","cow"}, null, ResultType.EXCEPTION, "bar"),
            new TestCase(new String[]{"foo", "bar", "cow", "moo"}, new String[]{"foo","cow"}, "&", ResultType.SUCCESS, "bar&moo"),
            new TestCase(new String[]{"foo", "bar", "cow", "moo"}, new String[]{"gah"}, null, ResultType.SUCCESS, null)
    };

    AuthenticationTokenExtractor extractor = DefaultAuthenticationTokenExtractor.INSTANCE;

    @Test
    public void runTestCases() throws Exception {
        for (TestCase testCase : testCases) {
            runTestCase(testCase);
        }
    }

    public void runTestCase(final TestCase testCase) throws Exception {
        try {
            Mockery context = new Mockery();
            context.setImposteriser(ClassImposteriser.INSTANCE);
            final ResourceAddress address = context.mock(ResourceAddress.class);

            System.out.print(testCase.describe());
            resolveConfiguration(testCase.configuredSessionCookies, testCase.configuredSessionCookieSeparator);
            HttpRequestMessage requestMessage = new HttpRequestMessage();
            requestMessage.setCookies(makeCookies(testCase.cookieNameValues));

            requestMessage.setLocalAddress(address);

            context.checking(new Expectations() {{
                allowing(address).getOption(HttpResourceAddress.REALMS);
                will(returnValue(null));
            }});

            AuthenticationToken actual = extractor.extract(requestMessage, new DefaultHttpRealmInfo(null, "BASIC", null, null, new String[]{}, testCase.configuredSessionCookies, null, null));

            context.assertIsSatisfied();

            if ( actual == null ) {
                org.junit.Assert.assertTrue(testCase.possibleResults().contains(null));
            } else if ( actual.size() == 1) {
                Assert.assertTrue(testCase.possibleResults().contains(actual.get()));
            } else {
                String actualString = "";
                for(int i = 0; i < actual.size(); i++) {
                    if ( i == 0 ) {
                        actualString += actual.get(i);
                    } else {
                        actualString +=(testCase.configuredSessionCookieSeparator+actual.get(i));
                    }
                }
            }
        } catch (Exception e) {
            if (testCase.expectedResultType != ResultType.EXCEPTION) {
                throw e;
            }
        }
        System.out.println("....success!");
    }

    private Collection<HttpCookie> makeCookies(String[] cookieNameValues) {
        Collection<HttpCookie> result = new ArrayList<>();
        for (int i = 0; i < cookieNameValues.length; i += 2) {
            result.add(new DefaultHttpCookie(cookieNameValues[i], cookieNameValues[i + 1]));
        }
        return result;
    }

    public void resolveConfiguration(String[] sessionCookies, String sessionCookieSeparator) throws Exception {
        // If you have more than one cookie name specified with no separator, it's a problem.
        if ((sessionCookies != null && sessionCookies.length > 1 && sessionCookieSeparator == null)) {
            throw new IllegalArgumentException("Both session cookies and session cookie separator are required.");

        }

        // If you specify a separator but no session cookie names, that's a problem.
        if (sessionCookies == null && sessionCookieSeparator != null) {
            throw new IllegalArgumentException("Both session cookies and session cookie separator are required.");
        }
    }

    /*
     * From wikipedia, next permutation algo
     *
     * Find the largest index k such that a[k] < a[k + 1]. If no such index exists, the permutation is the last permutation.
     * Find the largest index l greater than k such that a[k] < a[l].
     * Swap the value of a[k] with that of a[l].
     *Reverse the sequence from a[k + 1] up to and including the final element a[n].
     */
    private static final class PermutationGenerator implements Iterator<int[]> {
        private final int size;
        private final int[] current;
        private boolean first = true;

        PermutationGenerator(int size) {
            if (size < 1) {
                throw new IllegalArgumentException();
            }

            this.size = size;
            current = new int[size];
            for(int i=0; i < size; i++) {
                current[i] = i;
            }
        }

        @Override
        public boolean hasNext() {
            return first || hasNext0() != -1;
        }

        private int hasNext0() {
            // largest index k such that a[k] < a[k + 1]
            int k = -1;
            for(int i=0; i < current.length - 1; i++) {
                if (current[i] < current[i+1]) {
                    k = i;
                }
            }
            return k;
        }

        @Override
        public int[] next() {
            if (first) {
                first = false;
                return current;
            }
            // largest index k such that a[k] < a[k + 1]
            int k = hasNext0();
            if (k == -1) {
                throw new NoSuchElementException();
            }

            int l = -1;
            for(int i=k+1; i < current.length; i++) {
                if (current[k] < current[i]) {
                    l = i;
                }
            }

            // swap a[k] and a[l]
            int tmp = current[k];
            current[k] = current[l];
            current[l] = tmp;

            // reverse from a[k+1] to last
            int mid = (size - k-1)/2;
            for(int i=0; i < mid; i++) {
                tmp = current[k+1+i];
                current[k+1+i] = current[size-1-i];
                current[size-1-i] = tmp;
            }

            return current;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

}

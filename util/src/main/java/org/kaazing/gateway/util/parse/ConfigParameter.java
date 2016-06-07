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

import static java.lang.String.format;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.CharBuffer;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.kaazing.gateway.util.http.DefaultUtilityHttpClient;
import org.kaazing.gateway.util.http.UtilityHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provide parameterization services for configuration files.
 */
public abstract class ConfigParameter {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigParameter.class);

    /**
     * regex to capture parameter definitions amongst free text as per
     * {@link # resolveAndReplace(char[], int, int, Properties, List)}
     */
    private static final Pattern PARAM_REGEX = Pattern.compile("([\\$]?)([\\$]+\\{)([^\\{\\}]*)(\\})");

    /**
     * match indexes into {@link #PARAM_REGEX}
     */
    private enum ParameterRegex {
        ESCAPE(1), OPEN(2), PARAM(3), CLOSE(4);

        public final int index;

        ParameterRegex(int index) {
            this.index = index;
        }
    }

    private enum ParameterResolutionStrategy {
        PARAMETER_DEFINITION_DEFAULT, PARAMETER_PROPERTIES_FILE, SYSTEM_PROPERTIES, ENVIRONMENT_VARIABLES,
        ESCAPED_PARAMETER_DEFINITION, UNRESOLVED_PARAMETER_DEFINITION, CLOUD_RESOLUTION_STRATEGY, HOSTNAME
    }

    /**
     * Resolve and replace parameters with their value from within {@code chars}.<br>
     * <br>
     * Parameter definition valid syntaxes: <code><ul>
     * <li>${some.parameter}
     * <li>${some.parameter:}
     * <li>${:some.default.value}
     * <li>${some.parameter:some.default.value}
     * <li>$${escaped out parameter definition}
     * <li>$$$${triple escaped out parameter definition}
     * </ul></code><br>
     * Parameters values are resolved from the following stores (in order of precedence):
     * <ul>
     * <li>{@link System System Properties}
     * <li>{@link Properties parameterValuePairs Properties}, as passed in (likely sourced from file)
     * <li>{@link System#getenv() Environment Variables}
     * <li>{@code Default}, as defined by parameter definition
     * </ul>
     * <br>
     * Parameter definitions can be escaped with a leading '$' char.<br>
     * <br>
     * @param chars the source
     * @param offset the offset into <code>chars</code>
     * @param length the length of <code>chars</code> to consider
     * @param parameterValuePairs parameter name value pairs used for resolution
     * @param configuration Properties used to override parameterValuePairs.  The latter is specified in the config file,
     *                      the former is passed to the Gateway either on the command line or when configuring a Gateway.
     * @param errors list of <code>String</code> errors
     * @return the new string, with all parameters replaced by their resolved value
     */
    public static String resolveAndReplace(char[] chars, int offset, int length, Map<String, String> parameterValuePairs,
            Properties configuration, List<String> errors) {

        int lastMatchCharIndex = 0;
        StringBuffer string = new StringBuffer();
        Matcher matcher = PARAM_REGEX.matcher(CharBuffer.wrap(chars, offset, length));
        while (matcher.find()) {

            // we have a match, pull out the details and assign defaults
            String name = matcher.group(ParameterRegex.PARAM.index);
            String value = null;
            ParameterResolutionStrategy strategy = ParameterResolutionStrategy.PARAMETER_DEFINITION_DEFAULT;

            // process the prefix to the match
            string.append(chars, offset + lastMatchCharIndex, matcher.start() - lastMatchCharIndex);
            lastMatchCharIndex = matcher.end(ParameterRegex.CLOSE.index);

            // match escape sequence
            if (matcher.start(ParameterRegex.ESCAPE.index) != matcher.end(ParameterRegex.ESCAPE.index)) {
                // we have an escaped out match, drop leading escape tokens and pass the match through unchanged
                value = new String(chars, offset + matcher.start(ParameterRegex.OPEN.index),
                        matcher.end(ParameterRegex.CLOSE.index) - matcher.start(ParameterRegex.OPEN.index));
                strategy = ParameterResolutionStrategy.ESCAPED_PARAMETER_DEFINITION;
            }
            // match parameter definition
            else {
                // attempt to resolve value from hierarchy of value stores
                if (!"".equals(name)) {
                    if ((value = configuration.getProperty(name)) != null && !"".equals(value)) {
                        strategy = ParameterResolutionStrategy.SYSTEM_PROPERTIES;
                    }
                    else if ((value = parameterValuePairs.get(name)) != null && !"".equals(value)) {
                        strategy = ParameterResolutionStrategy.PARAMETER_DEFINITION_DEFAULT;
                    }
                }
            }

            // resolve cloud.host
            if (value == null && "cloud.host".equals(matcher.group(3))) {
                LOGGER.info("${cloud.host} found in config, attempting resolution by searching cloud provider");
                strategy = ParameterResolutionStrategy.CLOUD_RESOLUTION_STRATEGY;
                value = resolveCloudHost(new DefaultUtilityHttpClient());
                // value may be returned as null but that is good so it is caught as can't
                // determine value
                if (value == null) {
                    LOGGER.warn("${cloud.host} detected in config but could not " +
                            "determine cloud enviroment and find valid replacement for it");
                }
            }

            // resolve hostname
            if (value == null && "hostname".equals(matcher.group(3))) {
                LOGGER.debug("${hostname} found in config, using box hostname");
                strategy = ParameterResolutionStrategy.HOSTNAME;
                value = resolveHostname();
                // value may be returned as null but that is good so it is caught as can't
                // determine value
                if (value == null) {
                    LOGGER.warn("${hostname} detected in config but could not " +
                            "determine cloud enviroment and find valid replacement for it");
                }
            }

            // resolve cloud.instanceId
            if (value == null && "cloud.instanceId".equals(matcher.group(3))) {
                LOGGER.info("${cloud.instanceId} found in config, attempting resolution by searching cloud provider");
                strategy = ParameterResolutionStrategy.CLOUD_RESOLUTION_STRATEGY;
                value = resolveCloudInstanceId(new DefaultUtilityHttpClient());
                // value may be returned as null but that is good so it is caught as can't
                // determine value
                if (value == null) {
                    LOGGER.warn("${cloud.instanceId} detected in config but could not " +
                            "determine cloud enviroment and find valid replacement for it");
                }
            }

            // if not resolved, add error and pass match through unchanged
            if (value == null || "".equals(value)) {
                value = matcher.group();
                strategy = ParameterResolutionStrategy.UNRESOLVED_PARAMETER_DEFINITION;
                errors.add("Could not determine non-null value for parameter definition "
                        + matcher.group());
            }

            // add resolved value
            string.append(value);

            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("Detected configuration parameter [" + matcher.group() + "], replaced with [" + value
                        + "], as a result of resolution strategy [" + strategy + "]");
            }
        }

        // we have a match/non-match, process suffix/entire buffer
        string.append(chars, offset + lastMatchCharIndex, length - lastMatchCharIndex);

        return string.toString();
    }

    public static String cachedCloudHost;
    public static String cachedCloudInstanceId;

    // public with UtilityHttpClient is for testing
    public static String resolveCloudHost(UtilityHttpClient httpClient) {
        // cached so we only attempt it once
        if (cachedCloudHost == null) {
            String value = null;
            // AWS
            LOGGER.debug("Attempting to get AWS host information");

            try {
                String response = httpClient.performGetRequest("http://169.254.169.254/2014-02-25/meta-data/public-hostname");
                // confirm it has a aws hostname
                if (response != null && response.contains("amazonaws.com")) {
                    value = response;
                    LOGGER.debug(format("Found AWS hostname: %s", value));
                } else if (response != null && "".equals(response)) {
                    // sometimes aws does not get a hostname, so we fallback to use
                    // the public ip
                    response = httpClient.performGetRequest("http://169.254.169.254/2014-02-25/meta-data/public-ipv4");
                    if (response != null) {
                        value = response;
                        LOGGER.debug(format(
                                "Couldn't find AWS hostname, but did find AWS public ip: %s",
                                value));
                    }
                }

            } catch (Exception e) {
                // NOOP try next supported cloud provider if it exists
                LOGGER.debug(format("failed to get value due to exception with message: %s ", e.getMessage()));
            }
            // TODO: Add more as we support cloud platforms
            cachedCloudHost = value;
        }
        return cachedCloudHost;
    }

    /**
     * Resolves the hostname
     * @return value of hostname, or null if it can't be determined
     */
    public static String resolveHostname() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            return null;
        }
    }

    // public with UtilityHttpClient is for testing
    public static String resolveCloudInstanceId(UtilityHttpClient httpClient) {
        // cached so we only attempt it once
        if (cachedCloudInstanceId == null) {
            String value = null;
            // AWS
            LOGGER.debug("Attempting to get AWS instanceId information");

            try {
                String response = httpClient
                        .performGetRequest("http://169.254.169.254/2014-02-25/meta-data/instance-id");
                // confirm it has a aws hostname
                if (response != null) {
                    value = response;
                    LOGGER.debug(format("Found AWS instanceId: %s", value));
                }
            } catch (Exception e) {
                // NOOP try next supported cloud provider if it exists
            }
            // TODO: Add more as we support cloud platforms
            cachedCloudInstanceId = value;
        }
        return cachedCloudInstanceId;
    }

}

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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.kaazing.gateway.util.Utils;

/**
 * Class for keeping patterns with the associated cache-control directives 
 * and the number of matching patterns (used for determining how general a pattern is)
 */
public final class PatternCacheControl {

    private static final String EMPTY_STRING_VALUE = "";
    private static final String M_PLUS_STRING = "m+";

    private boolean defaultMaxAge;
    private String pattern;
    private int matchingPatternCount;
    private Map<Directive, String> directives = new HashMap<>();

    public PatternCacheControl(String pattern, String directives) {
        this.pattern = pattern;
        setDirectiveList(directives);
    }

    public PatternCacheControl(String pattern, Directive directive, String value) {
        this.pattern = pattern;
        // this is to be considered the most general pattern
        defaultMaxAge = true;
        directives.put(directive, value);
    }

    /**
     * Returns the associated pattern
     * @return pattern
     */
    public String getPattern() {
        return pattern;
    }

    /**
     * Returns the map of directives
     * @return directives
     */
    public Map<Directive, String> getDirectives() {
        return directives;
    }

    /**
     * Returns the number of matching patterns (used for determining how general a pattern is)
     * @return number of matching patterns
     */
    public int getMatchingPatternCount() {
        return defaultMaxAge ? Integer.MAX_VALUE : matchingPatternCount;
    }

    /**
     * Sets the value for the given directive (it changes the value if the directive exists, or inserts the directive otherwise)
     * @param directive
     * @param value
     */
    public void setDirective(Directive directive, String value) {
        directives.put(directive, value);
    }

    /**
     * Returns the value for the given directive
     * @param name - the directive name
     * @return the directive's value
     */
    public String getDirectiveValue(Directive name) {
        return directives.get(name);
    }

    /**
     * Checks if the directive with the given name exists in the map
     * @param directive name
     * @return true if the directive exists, false otherwise
     */
    public boolean hasDirective(Directive name) {
        if (directives.get(name) != null) {
            return true;
        }
        return false;
    }

    /**
     * Increments the number of matching patterns
     */
    public void incrementMatchingPatternCount() {
        this.matchingPatternCount++;
    }

    /**
     * Creates the map with directives (name and value) from a String of directives, split by ","
     * @param directives
     */
    private void setDirectiveList(String directives) {
        String[] directiveList = directives.replaceAll("\\s", EMPTY_STRING_VALUE).split(",");
        for (String entry : directiveList) {
            String[] nameValueArray = entry.split("=");
            String directiveName = nameValueArray[0];
            if (checkDirective(directiveName)) {
                if (nameValueArray.length > 1) {
                    parseDirectiveWithValue(directiveName, nameValueArray[1]);
                    continue;
                }
                this.directives.put(Directive.get(directiveName), EMPTY_STRING_VALUE);
            }
        }
    }

    /** Adds a directive with the associated value to the directives map, after parsing the value from the configuration file 
     * @param directiveName
     * @param directiveValue
     */
    private void parseDirectiveWithValue(String directiveName, String directiveValue) {
        Directive directive;
        if (directiveName.equals(Directive.MAX_AGE.getName())) {
            if (directiveValue.startsWith(M_PLUS_STRING)) {
                directiveValue = directiveValue.replace(M_PLUS_STRING, EMPTY_STRING_VALUE);
                directive = Directive.MAX_AGE_MPLUS;
            } else {
                directive = Directive.MAX_AGE;
            }
        } else {
            directive = Directive.get(directiveName);
        }
        long value = Utils.parseTimeInterval(directiveValue, TimeUnit.SECONDS, 0);
        directives.put(directive, Long.toString(value));
    }

    /**
     * Checks for duplicate directive values and correct syntax
     * @param directive
     * @return
     */
    private boolean checkDirective(String directive) {
        if (directives.containsKey(directive)) {
            throw new IllegalArgumentException("Duplicate cache-control directive in configuration file");
        } else if (Directive.get(directive) == null) {
            throw new IllegalArgumentException("Missing or incorrect cache-control syntax in the configuration file");
        }
        return true;
    }

}

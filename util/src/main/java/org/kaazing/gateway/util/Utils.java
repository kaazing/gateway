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
package org.kaazing.gateway.util;

import static java.nio.ByteOrder.BIG_ENDIAN;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Resource;

import org.apache.mina.filter.logging.LogLevel;
import org.slf4j.Logger;


public final class Utils {

    private Utils() {
    }

    private static final int NO_OF_DAYS_IN_A_YEAR = 365;
    private static final int NO_OF_DAYS_IN_A_WEEK = 7;

    public static final Charset UTF_8 = Charset.forName("UTF-8");

    public static final Charset US_ASCII = Charset.forName("US-ASCII");

    private static final byte[] LONG_MIN_VALUE_BYTES = "-9223372036854775808".getBytes(UTF_8);

    private static final int LONG_MIN_VALUE_BYTES_LENGTH = LONG_MIN_VALUE_BYTES.length;

    private static final byte[] INTEGER_MIN_VALUE_BYTES = "-2147483648".getBytes(UTF_8);

    private static final int INTEGER_MIN_VALUE_BYTES_LENGTH = INTEGER_MIN_VALUE_BYTES.length;

    private static final String TIME_UNIT_REGEX_STRING =
            "(?i:(ms|milli|millis|millisecond|milliseconds|s|sec|secs|second|seconds|m|min|mins|minute|minutes|"
                    + "h|hour|hours|d|day|days|w|week|weeks|y|year|years)?)";

    private static final String TIME_INTERVAL_REGEX_STRING = "([0-9\\.]+)\\s*" + TIME_UNIT_REGEX_STRING;

    private static final Pattern TIME_INTERVAL_PATTERN = Pattern.compile(TIME_INTERVAL_REGEX_STRING);

    public static final Set<String> SECONDS_UNITS =
            new HashSet<>(Arrays.asList("s", "sec", "secs", "second", "seconds"));

    public static final Set<String> MILLISECONDS_UNITS =
            new HashSet<>(Arrays.asList("ms", "milli", "millis", "millisecond", "milliseconds"));

    public static final Set<String> MINUTES_UNITS =
            new HashSet<>(Arrays.asList("m", "min", "mins", "minute", "minutes"));

    public static final Set<String> HOURS_UNITS =
            new HashSet<>(Arrays.asList("h", "hour", "hours"));

    static final Set<String> DAYS_UNITS =
            new HashSet<>(Arrays.asList("d", "day", "days"));

    static final Set<String> WEEKS_UNITS =
            new HashSet<>(Arrays.asList("w", "week", "weeks"));

    static final Set<String> YEARS_UNITS =
            new HashSet<>(Arrays.asList("y", "year", "years"));

    private static final String[] PERMITTED_DATA_RATE_UNITS =
            new String[] {"MiB/s", "KiB/s", "MB/s", "kB/s", "B/s"};

    // The following must match the above by position:
    private static final long[] DATA_RATE_MULTIPLIERS =
            new long[]{1024 * 1024, 1024, 1000 * 1000, 1000, 1};


    public static String join(String[] array, String separator) {
        if (array == null) {
            throw new NullPointerException("array");
        }

        if (separator == null) {
            throw new NullPointerException("separator");
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < array.length; i++) {
            if (i > 0) {
                sb.append(separator);
            }
            sb.append(array[i]);
        }

        return sb.toString();
    }

    /**
     * Creates a new String filled with size of a repeating character
     *
     * @param c Character to repeat
     * @param size Size of resulting String
     * @return String full of repeated characters
     */
    public static String fill(char c, int size) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < size; i++) {
            builder.append(c);
        }
        return builder.toString();
    }

    private static final byte[] TO_HEX =
            new byte[] {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};

  private static final byte[] FROM_HEX =
          new byte[] {0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F};

    public static String toHex(byte[] data) {
        int len = data.length;
        byte[] out = new byte[len << 1];
        byte cur;
        for (int i = 0; i < len; i++) {
            cur = data[i];
            out[(i << 1) + 1] = TO_HEX[cur & 0xF];
            out[i << 1] = TO_HEX[(cur >> 4) & 0xF];
        }
        return new String(out);
    }

    private static byte hexCharToByte(char c) {
        switch(c) {
            case '0': return FROM_HEX[0];
            case '1': return FROM_HEX[1];
            case '2': return FROM_HEX[2];
            case '3': return FROM_HEX[3];
            case '4': return FROM_HEX[4];
            case '5': return FROM_HEX[5];
            case '6': return FROM_HEX[6];
            case '7': return FROM_HEX[7];
            case '8': return FROM_HEX[8];
            case '9': return FROM_HEX[9];
            case 'A': return FROM_HEX[10];
            case 'B': return FROM_HEX[11];
            case 'C': return FROM_HEX[12];
            case 'D': return FROM_HEX[13];
            case 'E': return FROM_HEX[14];
            case 'F': return FROM_HEX[15];
        }
        return 0x00;
    }

    public static byte[] fromHex(String hex) {

        final int len = hex.length();
        byte[] out = new byte[len >> 1];
        final char[] chars = hex.toCharArray();
        for (int i = 0; i < out.length; i++) {

            final byte i1 = hexCharToByte(chars[i << 1]);
            final byte i2 = hexCharToByte(chars[(i << 1) + 1]);
            out[i] = (byte) ((i1 << 4) + i2);

        }
        return out;
    }

    public static int ENCODED_BOOLEAN_CAPACITY = 4;

    public static void encodeBoolean(boolean b, ByteBuffer buf) {
        buf.put(b ? TRUE_BYTES : FALSE_BYTES);
    }

    public static int ENCODED_INT_CAPACITY = INTEGER_MIN_VALUE_BYTES_LENGTH;

    public static void encodeInt(int i, ByteBuffer buf) {
        if (i == Integer.MIN_VALUE) {
            buf.put(INTEGER_MIN_VALUE_BYTES);
        }

        int size = (i < 0) ? stringSize(-i) + 1 : stringSize(i);
        if (buf.remaining() < size) {
            throw new BufferUnderflowException();
        }

        int position = buf.position();
        int offset = position + size;

        int q, r;
        byte sign = 0;

        int positive = i;
        if (positive < 0) {
            sign = '-';
            positive = -positive;
        }

        // Generate two digits per iteration
        while (positive >= 65536) {
            q = positive / 100;
        // really: r = i - (q * 100);
            r = positive - ((q << 6) + (q << 5) + (q << 2));
            positive = q;
            buf.put(--offset, DigitOnes[r]);
            buf.put(--offset, DigitTens[r]);
        }

        // Fall thru to fast mode for smaller numbers
        // assert(i <= 65536, i);
        for (;;) {
            q = (positive * 52429) >>> (16 + 3);
            r = positive - ((q << 3) + (q << 1));  // r = i-(q*10) ...
            buf.put(--offset, digits[r]);
            positive = q;
            if (positive == 0) {
                break;
            }
        }
        if (sign != 0) {
            buf.put(--offset, sign);
        }

        buf.position(position + size);
    }

    public static int ENCODED_LONG_CAPACITY = LONG_MIN_VALUE_BYTES_LENGTH;

    public static void encodeLong(long i, ByteBuffer buf) {
        if (i == Long.MIN_VALUE) {
            buf.put(LONG_MIN_VALUE_BYTES);
        }

        int size = (i < 0) ? stringSize(-i) + 1 : stringSize(i);
        if (buf.remaining() < size) {
            throw new BufferUnderflowException();
        }

        int position = buf.position();
        int offset = position + size;

        long q;
        int r;
        byte sign = 0;

        long positive = i;
        if (positive < 0L) {
            sign = '-';
            positive = -positive;
        }

        // Get 2 digits/iteration using longs until quotient fits into an int
        while (positive > Integer.MAX_VALUE) {
            q = positive / 100;
            // really: r = i - (q * 100);
            r = (int) (positive - ((q << 6) + (q << 5) + (q << 2)));
            positive = q;
            buf.put(--offset, DigitOnes[r]);
            buf.put(--offset, DigitTens[r]);
        }

        // Get 2 digits/iteration using ints
        int q2;
        int i2 = (int) positive;
        while (i2 >= 65536) {
            q2 = i2 / 100;
            // really: r = i2 - (q * 100);
            r = i2 - ((q2 << 6) + (q2 << 5) + (q2 << 2));
            i2 = q2;
            buf.put(--offset, DigitOnes[r]);
            buf.put(--offset, DigitTens[r]);
        }

        // Fall thru to fast mode for smaller numbers
        // assert(i2 <= 65536, i2);
        for (;;) {
            q2 = (i2 * 52429) >>> (16 + 3);
            r = i2 - ((q2 << 3) + (q2 << 1));  // r = i2-(q2*10) ...
            buf.put(--offset, digits[r]);
            i2 = q2;
            if (i2 == 0) {
                break;
            }
        }
        if (sign != 0) {
            buf.put(--offset, sign);
        }

        buf.position(position + size);
    }

    public static String asString(Map<ByteBuffer, ByteBuffer> headers) {
        if (headers == null) {
            return null;
        }

        Iterator<Entry<ByteBuffer, ByteBuffer>> i = headers.entrySet().iterator();
        if (! i.hasNext()) {
            return "{}";
        }

        StringBuilder sb = new StringBuilder();
        sb.append('{');
        for (;;) {
            Entry<ByteBuffer, ByteBuffer> e = i.next();
            ByteBuffer key = e.getKey();
            ByteBuffer value = e.getValue();
            sb.append(key   == headers ? "(this Map)" : asString(key));
            sb.append('=');
            sb.append(value == headers ? "(this Map)" : asString(value));
            if (! i.hasNext()) {
                return sb.append('}').toString();
            }
            sb.append(", ");
        }
    }

    /**
     * @param  buf         UTF-8 encoded bytes
     * @return String      UTF-8 decoded result
     */
    public static String asString(ByteBuffer buf) {
        if (buf == null) {
            return null;
        }

        if (buf.hasArray()) {
            return new String(buf.array(), buf.arrayOffset() + buf.position(), buf.remaining(), UTF_8);
        }

        try {
            // Direct buffer
            CharsetDecoder decoder = UTF_8.newDecoder();
            CharBuffer charBuf = decoder.decode(buf.duplicate());
            return charBuf.toString();
        }
        catch (CharacterCodingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Gets the content of the ByteBuffer as a byte[] without mutating the buffer
     * (so thread safe) and minimizing GC (i.e. object creation)
     */
    public static byte[] asByteArray(ByteBuffer buf) {
        byte[] result;
        if (buf.hasArray() && buf.arrayOffset() == 0 && buf.capacity() == buf.remaining()) {
            result = buf.array();
        }
        else {
            result = new byte[buf.remaining()];
            if (buf.hasArray()) {
                System.arraycopy(buf.array(), buf.arrayOffset() + buf.position(), result, 0, result.length);
            }
            else {
                // Direct buffer
                ByteBuffer duplicate = buf.duplicate();
                duplicate.mark();
                duplicate.get(result);
                duplicate.reset();
            }
        }
        return result;
    }

    @Deprecated // Use asString now it's been fixed to use UTF-8 CharSet
    public static String asStringUTF8(ByteBuffer buf) {
        return asString(buf);
    }

    public static ByteBuffer asByteBuffer(String s) {
        if (null == s) {
            return null;
        }
        if (s.length() < 1024) { // we will only check isASCII if message less then 1K.
            return isASCII(s) ? US_ASCII.encode(s) : UTF_8.encode(s);
        } else {
            return UTF_8.encode(s);
        }
    }

    public static boolean isASCII(String s) {
        int size = s.length();
        char c;
        for (int i = 0; i < size; i++) {
            c = s.charAt(i);
            if (!(c >= 0 && c <= 127)) {
                return false;
            }
        }
        return true;
    }

    public static ByteBuffer asByteBuffer(byte[] bytes) {
        return (bytes != null) ? ByteBuffer.wrap(bytes) : null;
    }

    /**
     * Reads an int value from the buffer starting at the given index relative to the current
     * position() of the buffer, without mutating the buffer in any way (so it's thread safe).
     */
    public static int getInt(ByteBuffer buf, int index) {
        return buf.order() == BIG_ENDIAN ? getIntB(buf, index) : getIntL(buf, index);
    }

    /**
     * Reads an long value from the buffer starting at the given index relative to the current
     * position() of the buffer, without mutating the buffer in any way (so it's thread safe).
     */
    public static long getLong(ByteBuffer buf, int index) {
        return buf.order() == BIG_ENDIAN ? getLongB(buf, index) : getLongL(buf, index);
    }

    /**
     * Reads a short value from the buffer starting at the given index relative to the current
     * position() of the buffer, without mutating the buffer in any way (so it's thread safe).
     */
    public static short getShort(ByteBuffer buf, int index) {
        return buf.order() == BIG_ENDIAN ? getShortB(buf, index) : getShortL(buf, index);
    }

    /**
     * Converts the first character of the string to uppercase. Does NOT deal with surrogate pairs.
     */
    public static String initCaps(String in) {
        return in.length() < 2 ? in.toUpperCase() : in.substring(0, 1).toUpperCase() + in.substring(1);
    }

    /**
     * Writes the contents of source to target without mutating source (so safe for
     * multithreaded access to source) and without GC (unless source is a direct buffer).
     */
    public static void putByteBuffer(ByteBuffer source, ByteBuffer target) {
        if (source.hasArray()) {
            byte[] array = source.array();
            int arrayOffset = source.arrayOffset();
            target.put(array, arrayOffset + source.position(), source.remaining());
        }
        else {
            target.put(source.duplicate());
        }
    }

    /**
     * Load the specified class, which can be a (public static) inner class provided the physical name is used
     * ("my.package.MyClass$MyInnerClass") rather than the canonical name ("my.package.MyClass.MyInnerClass")
     * @param className Fully qualified class name
     * @return The loaded Class
     * @throws ClassNotFoundException
     * @throws NoSuchFieldException
     * @throws IllegalAccessException
     */
    public static Class<?> loadClass(String className)
    throws ClassNotFoundException, NoSuchFieldException, IllegalAccessException {
      ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
      return classLoader.loadClass(className);
    }

    /**
     * Gets the value of a static int field (static final constant), given its fully qualified name.
     * The owner class can be a (public static) inner class provided the physical name is used
     * (e.g. "my.package.MyClass$MyInnerClass.INT_FIELD").
     * @param fullyQualifiedFieldName
     * @return the value of the field
     * @throws ClassNotFoundException
     * @throws NoSuchFieldException
     * @throws IllegalAccessException
     * @throws IllegalArgumentException - if the given name is invalid
     */
    public static int loadStaticInt(String fullyQualifiedFieldName)
      throws ClassNotFoundException, NoSuchFieldException, IllegalAccessException {
        int lastDotPos = fullyQualifiedFieldName.lastIndexOf('.');
        // Name must contain "." but not as first or last character
        if (lastDotPos <= 0 || lastDotPos + 1 >= fullyQualifiedFieldName.length()) {
            throw new IllegalArgumentException(fullyQualifiedFieldName);
        }
        String className = fullyQualifiedFieldName.substring(0, lastDotPos);
        String fieldName = fullyQualifiedFieldName.substring(lastDotPos + 1);
        Class<?> clazz = loadClass(className);
        Field field = clazz.getField(fieldName);
        int mode = field.getInt(clazz);
        return mode;
    }

    /**
     * Validate that the given string value is a valid boolean ("true" or "false", case insensitive) and convert it to a boolean
     * @param valueName
     * @param value
     * @param defaultValue result to be returned if value is null
     * @return
     * @throws IllegalArgumentException if the value is not a positive integer (including if it is an empty string)
     */
    public static boolean parseBoolean(String valueName, String value, boolean defaultValue) {
        boolean result = defaultValue;
        if (value != null) {
            boolean valid = true;
            result = Boolean.parseBoolean(value);
            // parseBoolean allows any value, e.g. so a typo like "trye" would be silently interpreted as false
            if (!result) {
                if (!Boolean.FALSE.toString().equalsIgnoreCase(value)) {
                    valid = false;
                }
            }
            if (!valid) {
                String message = String.format("Invalid value \"%s\" for %s, must be \"%s\" or \"%s\"",
                        value, valueName, Boolean.TRUE.toString(), Boolean.FALSE.toString());
                throw new IllegalArgumentException(message);
            }
        }
        return result;
    }

    /**
     * Validate that the given string value is a valid positive integer (int or long) and convert it to a long
     * @param valueName
     * @param value
     * @param defaultValue result to be returned if value is null
     * @return
     * @throws IllegalArgumentException if the value is not a positive integer (including if it is an empty string)
     */
    public static long parsePositiveInteger(String valueName, String value, long defaultValue) {
        long result = defaultValue;
        if (value != null) {
            boolean valid = true;
            try {
                result = Long.parseLong(value);
                if (result <= 0) {
                    valid = false;
                }
            }
            catch (NumberFormatException e) {
                valid = false;
            }
            if (!valid) {
                String message = String.format("Invalid value \"%s\" for %s, must be a positive integer",
                        value, valueName);
                throw new IllegalArgumentException(message);
            }
        }
        return result;
    }

    public static long parseTimeInterval(String timeIntervalValue, TimeUnit outputUnit) {
        return parseTimeInterval(timeIntervalValue, outputUnit, 0);
    }

    /**
     * @param timeIntervalValue - The value to be parsed
     * @param outputUnit        - The value will be converted to this unit
     * @param defaultValue      - The default value in seconds
     * @return
     */
    public static long parseTimeInterval(String timeIntervalValue, TimeUnit outputUnit, long defaultValue) {
        return parseTimeInterval(timeIntervalValue, outputUnit, String.valueOf(defaultValue) + "seconds");
    }

    /**
     * @param timeIntervalValue - The value to be passed
     * @param outputUnit        - The value will be converted to this unit
     * @param defaultValue      - The default value (can contain the unit, e.g. "10ms")
     * @return
     * @throws NumberFormatException if the value is negative or an empty string
     */
    public static long parseTimeInterval(String timeIntervalValue, TimeUnit outputUnit, String defaultValue) {
        outputUnit = outputUnit == null ? TimeUnit.SECONDS : outputUnit;
        if (timeIntervalValue == null) {
            return parseTimeInterval(defaultValue, outputUnit, 0);
        }
        if (outputUnit == null) {
            return parseTimeInterval(String.valueOf(timeIntervalValue), TimeUnit.SECONDS, 0);
        }

        int length = timeIntervalValue.length();
        if (length < 1) {
            throw new NumberFormatException("Illegal timeInterval value, empty string not allowed.\n"
                    + TIME_INTERVAL_INFORMATION);
        }

        Matcher matcher = TIME_INTERVAL_PATTERN.matcher(timeIntervalValue);
        if (matcher.matches()) {
            String unit = "seconds";
            double providedMultiplier = Double.parseDouble(matcher.group(1));
            if (providedMultiplier < 0) {
                throw new NumberFormatException("Illegal timeInterval value, negative intervals not allowed.\n"
                        + TIME_INTERVAL_INFORMATION);
            }
            if (matcher.group(2) != null) {
                unit = matcher.group(2);
            }

            long result = 1L;

            try {
                if (SECONDS_UNITS.contains(unit.toLowerCase())) {
                    if (providedMultiplier < 1) {
                    	result = (long) (providedMultiplier * outputUnit.convert(result, TimeUnit.SECONDS));
                    } else {
                    	result = (long) (outputUnit.convert((long)providedMultiplier, TimeUnit.SECONDS));
                    }
                } else if (MILLISECONDS_UNITS.contains(unit.toLowerCase())) {
                    if (providedMultiplier < 1) {
                    	result = (long) (providedMultiplier * outputUnit.convert(result, TimeUnit.MILLISECONDS));
                    } else {
                    	result = (long) (outputUnit.convert((long)providedMultiplier, TimeUnit.MILLISECONDS));
                    }
                } else if (MINUTES_UNITS.contains(unit.toLowerCase())) {
                    if (providedMultiplier < 1) {
                    	result = (long) (providedMultiplier * outputUnit.convert(result, TimeUnit.MINUTES));
                    } else {
                    	result = (long) (outputUnit.convert((long)providedMultiplier, TimeUnit.MINUTES));
                    }
                } else if (HOURS_UNITS.contains(unit.toLowerCase())) {
                    if (providedMultiplier < 1) {
                    	result = (long) (providedMultiplier * outputUnit.convert(result, TimeUnit.HOURS));
                    } else {
                    	result = (long) (outputUnit.convert((long)providedMultiplier, TimeUnit.HOURS));
                    }
                } else if (DAYS_UNITS.contains(unit.toLowerCase())) {
                    if (providedMultiplier < 1) {
                    	result = (long) (providedMultiplier * outputUnit.convert(result, TimeUnit.DAYS));
                    } else {
                    	result = (long) (outputUnit.convert((long)providedMultiplier, TimeUnit.DAYS));
                    }
                } else if (WEEKS_UNITS.contains(unit.toLowerCase())) {
                    if (providedMultiplier < 1) {
                    	result = (long) (providedMultiplier * outputUnit.convert(NO_OF_DAYS_IN_A_WEEK * result, TimeUnit.DAYS));
                    } else {
                    	result = (long) (outputUnit.convert(NO_OF_DAYS_IN_A_WEEK * (long)providedMultiplier, TimeUnit.DAYS));
                    }
                } else if (YEARS_UNITS.contains(unit.toLowerCase())) {
                    if (providedMultiplier < 1) {
                    	result = (long) (providedMultiplier * outputUnit.convert(NO_OF_DAYS_IN_A_YEAR * result, TimeUnit.DAYS));
                    } else {
                    	result = (long) (outputUnit.convert(NO_OF_DAYS_IN_A_YEAR * (long)providedMultiplier, TimeUnit.DAYS));
                    }
                }
                if (result < 0) {
                    throw new NumberFormatException("Expected a non-negative time interval, received \""
                            + timeIntervalValue + "\"");
                }
                return result;
            } catch (Exception e) {
                throw (NumberFormatException)
                        new NumberFormatException("Illegal timeIntervalValue value \"" +
                                timeIntervalValue + "\".\n" + TIME_INTERVAL_INFORMATION).initCause(e);
            }
        } else {
            throw new NumberFormatException("Illegal timeIntervalValue value \"" + timeIntervalValue + "\".\n"
                    + TIME_INTERVAL_INFORMATION);
        }
    }

    private static final String TIME_INTERVAL_INFORMATION =
            "Time intervals can be specified as numeric amounts of the following units: millisecond, second, minute, hour.\n" +
                    "For example, \"1800 second\" or \"30 minutes\" or \"0.5 hour\".";
    /**
     * Converts a data size specified in bytes (all digits), kilobytes (digits followed by k or K)
     * or megabytes (digits followed by m or M), values like 1048, 64k, 10M.
     * @param dataSizeValue   data size
     * @return - data size converted to int number of bytes
     */
    public static int parseDataSize(String dataSizeValue) {
        return parseDataSize(dataSizeValue, "dataSize");
    }

    /**
     * Converts a per second data rate specified in bytes (all digits), decimal kilobytes (digits followed by kB/s),
     * binary kilobytes (digits followed by KiB/s), decimal megabytes (digits followed by MB/s), or
     * binary megabytes (digits followed by KiB/s), values like 1048, 64kB/s, 10MiB/s.
     * @param dataRateValue   data size
     * @return - data rate converted to int number of bytes (implicitly per second)
     */
    public static long parseDataRate(String dataRateValue) {
        int length = dataRateValue.length();
        if (length < 1) {
            throw new NumberFormatException("Illegal dataRate value, empty string not allowed");
        }
        char last = dataRateValue.charAt(length - 1);
        long multiplier = 1;
        String numberPart = dataRateValue;
        if (!Character.isDigit(last)) {
            for (int i = 0; i < PERMITTED_DATA_RATE_UNITS.length; i++) {
                if (dataRateValue.endsWith(PERMITTED_DATA_RATE_UNITS[i])) {
                    multiplier = DATA_RATE_MULTIPLIERS[i];
                    int unitLength = PERMITTED_DATA_RATE_UNITS[i].length();
                    if (length < (unitLength + 1)) {
                        throw new NumberFormatException("Invalid dataRate value \"" + dataRateValue + "\", number part missing");
                    }
                    numberPart  = dataRateValue.substring(0, length - unitLength);
                    break;
                }
            }
        }
        final long result = Long.parseLong(numberPart) * multiplier;
        if (result <= 0) {
            throw new NumberFormatException("Illegal dataRate value \"" + dataRateValue +
                    "\", must be a positive number or overflow occurred");
        }
        return result;
    }

    private static int parseDataSize(String dataSizeValue, String specifier) {
        int length = dataSizeValue.length();
        if (length < 1) {
            throw new NumberFormatException("Illegal " + specifier + " value, empty string not allowed");
        }
        char last = dataSizeValue.charAt(length - 1);
        int multiplier = 1;
        String numberPart = dataSizeValue;
        switch(last) {
            case 'k':
            case 'K':
                multiplier = 1024;
                break;
            case 'm':
            case 'M':
                multiplier = 1024 * 1024;
                break;
        }
        if (multiplier > 1) {
            if (length < 2) {
               throw new NumberFormatException("Illegal " + specifier + " value \"" + dataSizeValue + "\", number part missing");
            }
            else {
                numberPart  = dataSizeValue.substring(0, length - 1);
            }
        }
        final int result = Integer.parseInt(numberPart) * multiplier;
        return result;
    }

    public static boolean sameOrEquals(Object this_, Object that) {
        return (this_ == that) || (this_ != null && this_.equals(that));
    }

    public static <K, V> boolean sameOrEquals(Map<K, V> this_, Map<K, V> that) {
        return (this_ == that) ||
                (this_ == null && that.isEmpty()) || (that == null && this_.isEmpty()) ||
                (this_ != null && this_.equals(that));
    }

    public static <T> boolean sameOrEquals(Collection<T> this_, Collection<T> that) {
        return (this_ == that) ||
                (this_ == null && that.isEmpty()) || (that == null && this_.isEmpty()) ||
                (this_ != null && this_.equals(that));
    }

    private static final byte[] TRUE_BYTES = "true".getBytes(UTF_8);
    private static final byte[] FALSE_BYTES = "false".getBytes(UTF_8);

    static final byte[] digits = {
        '0' , '1' , '2' , '3' , '4' , '5' ,
        '6' , '7' , '8' , '9' , 'a' , 'b' ,
        'c' , 'd' , 'e' , 'f' , 'g' , 'h' ,
        'i' , 'j' , 'k' , 'l' , 'm' , 'n' ,
        'o' , 'p' , 'q' , 'r' , 's' , 't' ,
        'u' , 'v' , 'w' , 'x' , 'y' , 'z'
        };

    static final byte[] DigitTens = {
        '0', '0', '0', '0', '0', '0', '0', '0', '0', '0',
        '1', '1', '1', '1', '1', '1', '1', '1', '1', '1',
        '2', '2', '2', '2', '2', '2', '2', '2', '2', '2',
        '3', '3', '3', '3', '3', '3', '3', '3', '3', '3',
        '4', '4', '4', '4', '4', '4', '4', '4', '4', '4',
        '5', '5', '5', '5', '5', '5', '5', '5', '5', '5',
        '6', '6', '6', '6', '6', '6', '6', '6', '6', '6',
        '7', '7', '7', '7', '7', '7', '7', '7', '7', '7',
        '8', '8', '8', '8', '8', '8', '8', '8', '8', '8',
        '9', '9', '9', '9', '9', '9', '9', '9', '9', '9',
        } ;

    static final byte[] DigitOnes = {
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
        } ;

    static final int [] sizeTable = { 9, 99, 999, 9999, 99999, 999999, 9999999,
        99999999, 999999999, Integer.MAX_VALUE };

    // Requires positive x
    static int stringSize(int x) {
        for (int i = 0;; i++) {
            if (x <= sizeTable[i]) {
                return i + 1;
            }
        }
    }

    static int stringSize(long x) {
        long p = 10;
        for (int i = 1; i < 19; i++) {
            if (x < p) {
                return i;
            }
            p = 10 * p;
        }
        return 19;
    }

    private static final Random rand = new SecureRandom();

    public static String randomHexString(int len) {
        byte[] out = randomHexBytes(len);
        return new String(out);
    }

    public static byte[] randomHexBytes(int len) {
        byte[] out = new byte[len];
        for (int i = 0; i < len; i++) {
            int randomInt = Math.abs(rand.nextInt());
            out[i] = TO_HEX[randomInt % TO_HEX.length];
        }
        return out;
    }

    public static int randomInt() {
        return rand.nextInt();
    }

    /**
     * @deprecated  Use LoggingUtil methods instead (or LoggingFilter)
     */
    @Deprecated
    public static void log(Logger logger, LogLevel eventLevel, String message, Object param) {
        switch (eventLevel) {
            case TRACE : logger.trace(message, param); return;
            case DEBUG : logger.debug(message, param); return;
            case INFO  : logger.info(message, param); return;
            case WARN  : logger.warn(message, param); return;
            case ERROR : logger.error(message, param); return;
            default    : return;
        }
    }

    public static <T> void inject(Object target,
                                  Class<T> injectableType,
                                  T injectableInstance) {
        inject0(target, injectableType, injectableInstance);
    }

    private static void inject0(Object target,
            Class<?> injectableType,
            Object injectableInstance) {

        Class<?> targetClass = target.getClass();
        Method[] methods = targetClass.getMethods();
        for (Method method : methods) {
            String methodName = method.getName();
            Class<?>[] parameterTypes = method.getParameterTypes();
            if (methodName.startsWith("set") &&
                methodName.length() > "set".length() &&
                parameterTypes.length == 1) {

                Resource annotation = method.getAnnotation(Resource.class);
                if (annotation != null) {
                    Class<?> resourceType = annotation.type();
                    if (resourceType == Object.class) {
                        resourceType = parameterTypes[0];
                    }

                    if (resourceType == injectableType) {
                        try {
                            method.invoke(target, injectableInstance);

                        } catch (IllegalArgumentException | InvocationTargetException | IllegalAccessException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();

                        }
                    }
                }
            }
        }
    }

    public static void injectAll(Object target, Map<Class<?>, Object> injectables) {

        for (Map.Entry<Class<?>, Object> entry : injectables.entrySet()) {
            Class<?> injectableType = entry.getKey();
            Object injectableInstance = entry.getValue();
            inject0(target, injectableType, injectableInstance);
        }
    }

    public static String join(Object[] data, String separator) {
        int len = data.length;
        if (len == 0) {
            return null;
        }
        StringBuilder buf = new StringBuilder();
        buf.append(data[0].toString());
        for (int i = 1; i < data.length; i++) {
            buf.append(separator);
            buf.append(data[i].toString());
        }
        return buf.toString();
    }

    public static String asCommaSeparatedString(final Collection<String> strings) {
            return asSeparatedString(strings, ",");
    }



    public static String asSeparatedString(final Collection<String> strings, final String separator) {
            StringBuilder b = new StringBuilder();
            if (strings != null) {
                for (String protocol: strings) {
                    b.append(protocol).append(separator);
                }
                if (strings.size() > 0) {
                    b.deleteCharAt(b.length() - 1);
                }
            }
            return b.toString();
        }

    public static Map<String, Object> rewriteKeys(final Map<String, Object> options, String prefix, String newPrefix) {
        final String separator = ".";
        final String startWith = prefix + separator;
        final int startWithLength = startWith.length();
        if (options == null) {
            return null;
        }
        Map<String, Object> result = new HashMap<>(options);
        for (Entry<String, Object> e: options.entrySet()) {
            if (e.getKey().startsWith(startWith)) {
                String newKey = newPrefix + separator;
                final String suffix = e.getKey().substring(startWithLength);
                result.put(newKey + suffix, result.remove(e.getKey()));
            }
        }
        return result;
    }


    /**
     * A Java 6/7 safe way of looking up a host name from an InetSocketAddress
     * without tickling a name lookup.
     *
     * @param inetSocketAddress the address for which you want a host string
     * @return a hostname for the given address, having not triggered a name service lookup
     */
    public static String getHostStringWithoutNameLookup(InetSocketAddress inetSocketAddress) {
        String newHost;
        if (inetSocketAddress.isUnresolved()) {
            newHost = inetSocketAddress.getHostName();
        } else {
            newHost = inetSocketAddress.getAddress().getHostAddress();
        }
        return newHost;
    }

    public static String stackTrace(Throwable cause) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(byteArrayOutputStream);
        PrintStream printStream = new PrintStream(bufferedOutputStream);
        cause.printStackTrace(printStream);
        printStream.flush();
        return byteArrayOutputStream.toString();

    }

    private static short makeShort(byte b1, byte b0) {
        return (short) ((b1 << 8) | (b0 & 0xff));
    }

    static short getShortL(ByteBuffer bb, int bi) {
        return makeShort(bb.get(bi + 1),
                         bb.get(bi));
    }

    static short getShortB(ByteBuffer bb, int bi) {
        return makeShort(bb.get(bi),
                         bb.get(bi + 1));
    }

    private static int makeInt(byte b3, byte b2, byte b1, byte b0) {
        return b3 << 24 |
                (b2 & 0xff) << 16 |
                (b1 & 0xff) <<  8 |
                b0 & 0xff;
    }

    private static int getIntB(ByteBuffer bb, int index) {
        return makeInt(bb.get(index),
                       bb.get(index + 1),
                       bb.get(index + 2),
                       bb.get(index + 3));
    }

    private static int getIntL(ByteBuffer bb, int index) {
        return makeInt(bb.get(index + 3),
                       bb.get(index + 2),
                       bb.get(index + 1),
                       bb.get(index));
    }

    private static long makeLong(byte b7, byte b6, byte b5, byte b4,
            byte b3, byte b2, byte b1, byte b0) {
        return (long) b7 << 56 |
                ((long) b6 & 0xff) << 48 |
                ((long) b5 & 0xff) << 40 |
                ((long) b4 & 0xff) << 32 |
                ((long) b3 & 0xff) << 24 |
                ((long) b2 & 0xff) << 16 |
                ((long) b1 & 0xff) <<  8 |
                (long) b0 & 0xff;
    }

    private static long getLongL(ByteBuffer bb, int index) {
        return makeLong(bb.get(index + 7),
                        bb.get(index + 6),
                        bb.get(index + 5),
                        bb.get(index + 4),
                        bb.get(index + 3),
                        bb.get(index + 2),
                        bb.get(index + 1),
                        bb.get(index));
    }

    private static long getLongB(ByteBuffer bb, int index) {
        return makeLong(bb.get(index),
                        bb.get(index + 1),
                        bb.get(index + 2),
                        bb.get(index + 3),
                        bb.get(index + 4),
                        bb.get(index + 5),
                        bb.get(index + 6),
                        bb.get(index + 7));
    }

    public static String[] removeStringArrayElement(String[] option, String remove) {

        if (option == null) {
            return option;
        }


        int index = -1;
        for (int i = 0; i < option.length; i++) {
            String element = option[i];
            if (remove == null) {
                if (element == null) {
                    index = i;
                    break;
                }
            } else {
                if (remove.equals(element)) {
                    index = i;
                }
            }
        }

        if (index != -1) {
            String[] result = new String[option.length - 1];
            System.arraycopy(option, 0, result, 0, index);
            System.arraycopy(option, index + 1, result, index, option.length - (index + 1));
            return result;
        }
        return option;
    }
}

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
package org.apache.mina.filter.codec.textline;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;

/**
 * A delimiter which is appended to the end of a text line, such as
 * <tt>CR/LF</tt>.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class LineDelimiter {
    /**
     * the line delimiter constant of the current O/S.
     */
    public static final LineDelimiter DEFAULT;

    static {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        PrintWriter out = new PrintWriter(bout);
        out.println();
        DEFAULT = new LineDelimiter(new String(bout.toByteArray()));
    }

    /**
     * A special line delimiter which is used for auto-detection of
     * EOL in {@link TextLineDecoder}.  If this delimiter is used,
     * {@link TextLineDecoder} will consider both  <tt>'\r'</tt> and
     * <tt>'\n'</tt> as a delimiter.
     */
    public static final LineDelimiter AUTO = new LineDelimiter("");

    /**
     * The CRLF line delimiter constant (<tt>"\r\n"</tt>)
     */
    public static final LineDelimiter CRLF = new LineDelimiter("\r\n");
        
    /**
     * The line delimiter constant of UNIX (<tt>"\n"</tt>)
     */
    public static final LineDelimiter UNIX = new LineDelimiter("\n");

    /**
     * The line delimiter constant of MS Windows/DOS (<tt>"\r\n"</tt>)
     */
    public static final LineDelimiter WINDOWS = CRLF;

    /**
     * The line delimiter constant of Mac OS (<tt>"\r"</tt>)
     */
    public static final LineDelimiter MAC = new LineDelimiter("\r");

    /**
     * The line delimiter constant for NUL-terminated text protocols
     * such as Flash XML socket (<tt>"\0"</tt>)
     */
    public static final LineDelimiter NUL = new LineDelimiter("\0");

    private final String value;

    /**
     * Creates a new line delimiter with the specified <tt>value</tt>.
     */
    public LineDelimiter(String value) {
        if (value == null) {
            throw new NullPointerException("delimiter");
        }
        this.value = value;
    }

    /**
     * Return the delimiter string.
     */
    public String getValue() {
        return value;
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof LineDelimiter)) {
            return false;
        }

        LineDelimiter that = (LineDelimiter) o;
        return this.value.equals(that.value);
    }

    @Override
    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append("delimiter:");
        if (value.length() == 0) {
            buf.append(" auto");
        } else {
            for (int i = 0; i < value.length(); i++) {
                buf.append(" 0x");
                buf.append(Integer.toHexString(value.charAt(i)));
            }
        }
        return buf.toString();
    }
}

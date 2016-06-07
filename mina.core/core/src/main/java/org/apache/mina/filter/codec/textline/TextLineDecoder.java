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

import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;

import org.apache.mina.core.buffer.BufferDataException;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.AttributeKey;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolDecoderException;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;
import org.apache.mina.filter.codec.RecoverableProtocolDecoderException;

/**
 * A {@link ProtocolDecoder} which decodes a text line into a string.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class TextLineDecoder implements ProtocolDecoder {
    private final AttributeKey CONTEXT = new AttributeKey(getClass(), "context");

    private final Charset charset;

    private final LineDelimiter delimiter;

    private IoBuffer delimBuf;

    private int maxLineLength = 1024;

    /**
     * Creates a new instance with the current default {@link Charset}
     * and {@link LineDelimiter#AUTO} delimiter.
     */
    public TextLineDecoder() {
        this(LineDelimiter.AUTO);
    }

    /**
     * Creates a new instance with the current default {@link Charset}
     * and the specified <tt>delimiter</tt>.
     */
    public TextLineDecoder(String delimiter) {
        this(new LineDelimiter(delimiter));
    }

    /**
     * Creates a new instance with the current default {@link Charset}
     * and the specified <tt>delimiter</tt>.
     */
    public TextLineDecoder(LineDelimiter delimiter) {
        this(Charset.defaultCharset(), delimiter);
    }

    /**
     * Creates a new instance with the spcified <tt>charset</tt>
     * and {@link LineDelimiter#AUTO} delimiter.
     */
    public TextLineDecoder(Charset charset) {
        this(charset, LineDelimiter.AUTO);
    }

    /**
     * Creates a new instance with the spcified <tt>charset</tt>
     * and the specified <tt>delimiter</tt>.
     */
    public TextLineDecoder(Charset charset, String delimiter) {
        this(charset, new LineDelimiter(delimiter));
    }

    /**
     * Creates a new instance with the specified <tt>charset</tt>
     * and the specified <tt>delimiter</tt>.
     */
    public TextLineDecoder(Charset charset, LineDelimiter delimiter) {
        if (charset == null) {
            throw new NullPointerException("charset");
        }
        if (delimiter == null) {
            throw new NullPointerException("delimiter");
        }

        this.charset = charset;
        this.delimiter = delimiter;
    }

    /**
     * Returns the allowed maximum size of the line to be decoded.
     * If the size of the line to be decoded exceeds this value, the
     * decoder will throw a {@link BufferDataException}.  The default
     * value is <tt>1024</tt> (1KB).
     */
    public int getMaxLineLength() {
        return maxLineLength;
    }

    /**
     * Sets the allowed maximum size of the line to be decoded.
     * If the size of the line to be decoded exceeds this value, the
     * decoder will throw a {@link BufferDataException}.  The default
     * value is <tt>1024</tt> (1KB).
     */
    public void setMaxLineLength(int maxLineLength) {
        if (maxLineLength <= 0) {
            throw new IllegalArgumentException("maxLineLength: "
                    + maxLineLength);
        }

        this.maxLineLength = maxLineLength;
    }

    public void decode(IoSession session, IoBuffer in,
            ProtocolDecoderOutput out) throws Exception {
        Context ctx = getContext(session);

        if (LineDelimiter.AUTO.equals(delimiter)) {
            decodeAuto(ctx, session, in, out);
        } else {
            decodeNormal(ctx, session, in, out);
        }
    }

    private Context getContext(IoSession session) {
        Context ctx;
        ctx = (Context) session.getAttribute(CONTEXT);
        if (ctx == null) {
            ctx = new Context();
            session.setAttribute(CONTEXT, ctx);
        }
        return ctx;
    }

    public void finishDecode(IoSession session, ProtocolDecoderOutput out)
            throws Exception {
        // Do nothing
    }

    public void dispose(IoSession session) throws Exception {
        Context ctx = (Context) session.getAttribute(CONTEXT);
        if (ctx != null) {
            session.removeAttribute(CONTEXT);
        }
    }

    private void decodeAuto(Context ctx, IoSession session, IoBuffer in, ProtocolDecoderOutput out)
            throws CharacterCodingException, ProtocolDecoderException {

        int matchCount = ctx.getMatchCount();

        // Try to find a match
        int oldPos = in.position();
        int oldLimit = in.limit();
        while (in.hasRemaining()) {
            byte b = in.get();
            boolean matched = false;
            switch (b) {
            case '\r':
                // Might be Mac, but we don't auto-detect Mac EOL
                // to avoid confusion.
                matchCount++;
                break;
            case '\n':
                // UNIX
                matchCount++;
                matched = true;
                break;
            default:
                matchCount = 0;
            }

            if (matched) {
                // Found a match.
                int pos = in.position();
                in.limit(pos);
                in.position(oldPos);

                ctx.append(in);

                in.limit(oldLimit);
                in.position(pos);

                if (ctx.getOverflowPosition() == 0) {
                    IoBuffer buf = ctx.getBuffer();
                    buf.flip();
                    buf.limit(buf.limit() - matchCount);
                    try {
                        writeText(session, buf.getString(ctx.getDecoder()), out);
                    } finally {
                        buf.clear();
                    }
                } else {
                    int overflowPosition = ctx.getOverflowPosition();
                    ctx.reset();
                    throw new RecoverableProtocolDecoderException(
                            "Line is too long: " + overflowPosition);
                }

                oldPos = pos;
                matchCount = 0;
            }
        }

        // Put remainder to buf.
        in.position(oldPos);
        ctx.append(in);

        ctx.setMatchCount(matchCount);
    }

    private void decodeNormal(Context ctx, IoSession session, IoBuffer in, ProtocolDecoderOutput out)
            throws CharacterCodingException, ProtocolDecoderException {

        int matchCount = ctx.getMatchCount();

        // Convert delimiter to ByteBuffer if not done yet.
        if (delimBuf == null) {
            IoBuffer tmp = IoBuffer.allocate(2).setAutoExpand(true);
            tmp.putString(delimiter.getValue(), charset.newEncoder());
            tmp.flip();
            delimBuf = tmp;
        }

        // Try to find a match
        int oldPos = in.position();
        int oldLimit = in.limit();
        while (in.hasRemaining()) {
            byte b = in.get();
            if (delimBuf.get(matchCount) == b) {
                matchCount++;
                if (matchCount == delimBuf.limit()) {
                    // Found a match.
                    int pos = in.position();
                    in.limit(pos);
                    in.position(oldPos);

                    ctx.append(in);

                    in.limit(oldLimit);
                    in.position(pos);
                    if (ctx.getOverflowPosition() == 0) {
                        IoBuffer buf = ctx.getBuffer();
                        buf.flip();
                        buf.limit(buf.limit() - matchCount);
                        try {
                            writeText(session, buf.getString(ctx.getDecoder()), out);
                        } finally {
                            buf.clear();
                        }
                    } else {
                        int overflowPosition = ctx.getOverflowPosition();
                        ctx.reset();
                        throw new RecoverableProtocolDecoderException(
                                "Line is too long: " + overflowPosition);
                    }

                    oldPos = pos;
                    matchCount = 0;
                }
            } else {
                // fix for DIRMINA-506 & DIRMINA-536
                in.position(Math.max(0, in.position() - matchCount));
                matchCount = 0;
            }
        }

        // Put remainder to buf.
        in.position(oldPos);
        ctx.append(in);

        ctx.setMatchCount(matchCount);
    }

    /**
     * By default, this method propagates the decoded line of text to
     * {@code ProtocolDecoderOutput#write(Object)}.  You may override this method to modify
     * the default behavior.
     *
     * @param session  the {@code IoSession} the received data.
     * @param text  the decoded text
     * @param out  the upstream {@code ProtocolDecoderOutput}.
     */
    protected void writeText(IoSession session, String text, ProtocolDecoderOutput out) {
        out.write(text);
    }

    private class Context {
        private final CharsetDecoder decoder;
        private final IoBuffer buf;
        private int matchCount = 0;
        private int overflowPosition = 0;

        private Context() {
            decoder = charset.newDecoder();
            buf = IoBuffer.allocate(80).setAutoExpand(true);
        }

        public CharsetDecoder getDecoder() {
            return decoder;
        }

        public IoBuffer getBuffer() {
            return buf;
        }

        public int getOverflowPosition() {
            return overflowPosition;
        }

        public int getMatchCount() {
            return matchCount;
        }

        public void setMatchCount(int matchCount) {
            this.matchCount = matchCount;
        }

        public void reset() {
            overflowPosition = 0;
            matchCount = 0;
            decoder.reset();
        }

        public void append(IoBuffer in) {
            if (overflowPosition != 0) {
                discard(in);
            } else if (buf.position() > maxLineLength - in.remaining()) {
                    overflowPosition = buf.position();
                    buf.clear();
                    discard(in);
            } else {
                getBuffer().put(in);
            }
        }

        private void discard(IoBuffer in) {
            if (Integer.MAX_VALUE - in.remaining() < overflowPosition) {
                overflowPosition = Integer.MAX_VALUE;
            } else {
                overflowPosition += in.remaining();
            }
            in.position(in.limit());
        }
    }
}
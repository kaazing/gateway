/**
 * Copyright (c) 2007-2014 Kaazing Corporation. All rights reserved.
 * 
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.kaazing.gateway.transport.ssl.bridge.filter;

import java.util.ArrayList;
import java.util.List;

import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.kaazing.mina.core.buffer.IoBufferAllocatorEx;
import org.kaazing.mina.core.buffer.IoBufferEx;
import org.kaazing.mina.filter.codec.CumulativeProtocolDecoderEx;

// Enough of a TLS decoder to decode far enough into ClientHello message.
// How far is "far enough"?  Enough to find out the SSL version requested,
// and enough to know whether SSL_RSA_WITH_RC4_128_{SHA,MD5} are present
// in the ciphersuite list.  See KG-7083.

public class SslClientHelloDecoder
    extends CumulativeProtocolDecoderEx {

    private static final String LOGGER_NAME = "transport.ssl.codec#client_hello";
    private static final Logger LOGGER = LoggerFactory.getLogger(LOGGER_NAME);

    // These values are defined in the IETF TLS cipher suite registry; see:
    //
    //   http://www.iana.org/assignments/tls-parameters/tls-parameters.xml#tls-parameters-3
    private static final int RC4_MD5_HEX = 0x0004;
    private static final int RC4_SHA_HEX = 0x0005;

    private static final short HANDSHAKE_CONTENT_TYPE = 22;
    private static final short CLIENTHELLO_MESSAGE_TYPE = 1;

    private static final short SSLV2_CLIENTHELLO = 128;
    private static final int SSLV2_RC4_MD5_HEX = 0x010080;

    public SslClientHelloDecoder(IoBufferAllocatorEx<?> allocator) {
        super(allocator);
    }

    @Override
    protected boolean doDecode(IoSession session,
                               IoBufferEx original,
                               ProtocolDecoderOutput out)
        throws Exception {

        // Need at least 2 bytes to differentiate between SSLv2 ClientHello
        // messages and SSLv3/TLSv1/+ messages.
        //
        // For SSLv2 ClientHello, we need:
        //
        //  length header (short)
        //  content type (byte)
        //
        // For more details, see:
        //
        //  http://tools.ietf.org/html/draft-hickman-netscape-ssl-00
        //
        // Otherwise, we need:
        //
        //  content type (byte)
        //  version (short)
        //  length (short)
        //
        // So wait for at least 5 bytes, to cover either case.

        if (original.remaining() < 5) {
            return false;
        }

        // Make a copy, so that we can read things non-destructively
        // TODO: use explicit position to be non-destructive rather than allocating a duplicate
        IoBufferEx dup = original.duplicate();

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace(String.format("Received SSL message: %s", original.duplicate()));
        }

        // If not a Handshake record, be done.  Note that we have to
        // successfully handle SSLv2 ClientHello formats as well.

        short contentType = dup.getUnsigned();
        if (contentType == HANDSHAKE_CONTENT_TYPE) {
            // Skip the ProtocolVersion here; we will get it later
            dup.skip(2);

            int recordSize = dup.getUnsignedShort();

            // Now wait until we have the entire record
            if (original.remaining() < (5 + recordSize)) {
                // Keep buffering
                return false;
            }

        } else if (contentType == SSLV2_CLIENTHELLO) {
            short len = dup.getUnsigned();

            // Decode the length
            int recordSize = ((contentType & 0x7f) << 8 | len);

            // Now wait until we have the entire record
            if (original.remaining() < (2 + recordSize)) {
                // Keep buffering
                return false;
            }

        } else { 
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace(String.format("SSL record content type %d is not a Handshake record, ignoring", contentType));
            }

            // We're only interested in Handshake records
            out.write(original.getSlice(original.remaining()));
            return true;
        }

        // For the format of the ClientHello message, see RFC 5246,
        // Section 7.4.1.2.

        short messageType = dup.getUnsigned();
        if (messageType != CLIENTHELLO_MESSAGE_TYPE) {
            // We're only interested in ClientHello messages
            out.write(original.getSlice(original.remaining()));
            return true;
        }

        List<String> appletCiphers = new ArrayList<String>(2);

        if (contentType == HANDSHAKE_CONTENT_TYPE) {
            // If we're not an SSLv2 ClientHello, then skip the ClientHello
            // message size.
            dup.skip(3);

            // Use the ClientHello ProtocolVersion
            SslVersion version = SslVersion.decode(dup.getUnsignedShort());
            SslCipherSelectionFilter.SSL_PROTOCOL_VERSION.set(session, version);

            // Skip ClientRandom
            dup.skip(32);

            // Skip SessionID
            int sessionIDSize = dup.getUnsigned();
            dup.skip(sessionIDSize);

            // Now we get to what we're really after: the ciphersuites supported
            // by the client.
            int cipherSuiteSize = dup.getUnsignedShort();

            // cipherSuiteSize is the number of bytes; each cipher is specified
            // using a short (2 bytes).  Thus the cipher suite count is the half
            // the cipher suite size.
            int cipherSuiteCount = cipherSuiteSize / 2;

            // Iterate through each of the ciphersuites, looking for either/both
            // of the blessed applet ciphers: SSL_RSA_WITH_RC4_128_{SHA,MD5}.

            for (int i = 0; i < cipherSuiteCount; i++) {
                int cipher = dup.getUnsignedShort();

                if (cipher == RC4_SHA_HEX) {
                    appletCiphers.add(SslCipherSelectionFilter.RC4_SHA);

                } else if (cipher == RC4_MD5_HEX) {
                    appletCiphers.add(SslCipherSelectionFilter.RC4_MD5);
                }
            }

        } else {
            // SSLv2 ClientHello.

            // Use the ClientHello ProtocolVersion
            SslVersion version = SslVersion.decode(dup.getUnsignedShort());
            SslCipherSelectionFilter.SSL_PROTOCOL_VERSION.set(session, version);

            // Determine cipher specs size
            short msb = dup.getUnsigned();
            short lsb = dup.getUnsigned();
            int cipherSuiteSize = ((msb << 8) | lsb);

            // Skip the sessionID size
            dup.skip(2);

            // Skip the challenge size
            dup.skip(2);

            // Now we get to what we're really after: the ciphersuites supported
            // by the client.

            // cipherSuiteSize is the number of bytes; each cipher is specified
            // using a medium int (3 bytes).
            int cipherSuiteCount = cipherSuiteSize / 3;

            // Iterate through each of the ciphersuites, looking for 
            // SSL_RSA_WITH_RC4_128_MD5.  (It's the only one supported in
            // SSLv2 ClientHellos).

            for (int i = 0; i < cipherSuiteCount; i++) {
                int cipherKind = dup.getUnsignedMediumInt();

                if (cipherKind == SSLV2_RC4_MD5_HEX) {
                    appletCiphers.add(SslCipherSelectionFilter.RC4_MD5);
                }
            }
        }

        if (appletCiphers.size() > 0) {
            SslCipherSelectionFilter.SSL_APPLET_CIPHERS.set(session, appletCiphers);
        }

        out.write(original.getSlice(original.remaining()));
        return true;
    }
}

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
package org.apache.mina.proxy;

import static org.apache.mina.proxy.utils.ByteUtilities.asHex;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Security;

import org.apache.mina.proxy.utils.MD4Provider;

import junit.framework.TestCase;

/**
 * MD4Test.java - JUnit testcase that tests the rfc 1320 test suite.
 * @see <a href="http://www.ietf.org/rfc/rfc1320.txt">RFC 1320</a>
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 * @since MINA 2.0.0-M3
 */
public class MD4Test extends TestCase {

    /**
     * {@inheritDoc}
     */
    @Override
    protected void setUp() throws Exception {
        if (Security.getProvider(MD4Provider.PROVIDER_NAME) == null) {
            System.out.print("Adding MINA provider...");
            Security.addProvider(new MD4Provider());
            //System.out.println(" [Ok]");
        }
    }

    /**
     * Test suite for the MD4 algorithm. 
     */
    public void testRFCVectors() throws NoSuchAlgorithmException,
            NoSuchProviderException {
        MessageDigest md4 = MessageDigest.getInstance("MD4",
                MD4Provider.PROVIDER_NAME);
        doTest(md4, "31d6cfe0d16ae931b73c59d7e0c089c0", "");
        doTest(md4, "bde52cb31de33e46245e05fbdbd6fb24", "a");
        doTest(md4, "a448017aaf21d8525fc10ae87aa6729d", "abc");
        doTest(md4, "d9130a8164549fe818874806e1c7014b", "message digest");
        doTest(md4, "d79e1c308aa5bbcdeea8ed63df412da9",
                "abcdefghijklmnopqrstuvwxyz");
        doTest(md4, "043f8582f241db351ce627e153e7f0e4",
                "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789");
        doTest(
                md4,
                "e33b4ddc9c38f2199c3e7b164fcc0536",
                "12345678901234567890123456789012345678901234567890123456789012345678901234567890");
    }

    /**
     * Original test vector found on <a href="http://en.wikipedia.org/wiki/MD4">wikipedia(en)</a>
     * and <a href="http://fr.wikipedia.org/wiki/MD4">wikipedia(fr)</a>
     */
    public void testWikipediaVectors() throws NoSuchAlgorithmException,
            NoSuchProviderException {
        MessageDigest md4 = MessageDigest.getInstance("MD4",
                MD4Provider.PROVIDER_NAME);
        doTest(md4, "b94e66e0817dd34dc7858a0c131d4079",
                "Wikipedia, l'encyclopedie libre et gratuite");
        doTest(md4, "1bee69a46ba811185c194762abaeae90",
                "The quick brown fox jumps over the lazy dog");
        doTest(md4, "b86e130ce7028da59e672d56ad0113df",
                "The quick brown fox jumps over the lazy cog");
    }

    /**
     * Performs md4 digesting on the provided test vector and verifies that the
     * result equals to the expected result.
     * 
     * @param md4 the md4 message digester
     * @param expected the expected hex formatted string
     * @param testVector the string message
     */
    private static void doTest(MessageDigest md4, String expected,
            String testVector) {
        System.out.print("Testing vector [" + testVector + "]: ");
        String result = asHex(md4.digest(testVector.getBytes()));
        System.out.print(result);
        assertEquals(expected, result);
        //System.out.println(" ... Ok");
    }
}

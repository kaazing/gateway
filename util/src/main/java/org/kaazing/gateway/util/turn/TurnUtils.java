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
package org.kaazing.gateway.util.turn;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.util.Base64;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class TurnUtils {

    public static final String HMAC_SHA_1 = "HmacSHA1";

    private TurnUtils() {
        // utility class should hide constructor
    }

    public static Key getSharedSecret(KeyStore ks, String alias, File pwFile) {
        char[] password;
        try {
            password = loadKeyStorePassword(pwFile);
        } catch (IOException e) {
            throw new TurnException("Unable to load password from file: " + pwFile, e);
        }
        return getSharedSecret(ks, alias, password);
    }

    public static Key getSharedSecret(KeyStore ks, String alias, char[] password) {
        try {
            return ks.getKey(alias, password);
        } catch (KeyStoreException | NoSuchAlgorithmException | UnrecoverableKeyException e) {
            throw new TurnException("Unable to get shared secret", e);
        }
    }

    public static char[] getPassword(String username, Key sharedSecret, String algorithm) {
        byte[] key = sharedSecret.getEncoded();
        Mac hmac;
        try {
            hmac = Mac.getInstance(algorithm);
            SecretKeySpec signingKey = new SecretKeySpec(key, algorithm);
            hmac.init(signingKey);
        } catch (InvalidKeyException | NoSuchAlgorithmException e) {
            throw new TurnException("Unable to generate password", e);
        }

        return Base64.getEncoder().encodeToString(hmac.doFinal(username.getBytes())).toCharArray();
    }

    private static char[] loadKeyStorePassword(File location) throws IOException {
        try (BufferedReader in = new BufferedReader(new FileReader(location))) {
            String line = in.readLine();
            return line.toCharArray();
        }
    }
}
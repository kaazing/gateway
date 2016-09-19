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

import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
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

    public static Key getSharedSecret(KeyStore ks, String alias, char[] password) {
        try {
            return ks.getKey(alias, password);
        } catch (KeyStoreException | NoSuchAlgorithmException | UnrecoverableKeyException e) {
            throw new TurnException("Unable to get shared secret", e);
        }
    }

    /**
     * This function generates the secret key based on the procedure described in
     *  https://tools.ietf.org/html/draft-uberti-rtcweb-turn-rest-00#section-2.2
     * 
     * @param username - the username of the API 
     * @param sharedSecret - the shared secret used by the TURN server as well
     * @param algorithm - the algorithm. COTURN only supports HmacSHA1 as of 2016/08/18
     * @return - the actual password
     */
    public static char[] generatePassword(String username, Key sharedSecret, String algorithm) {
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


    public static byte[] generateKey(String username, String realm, String password, char separator) {
        String data = String.format("%s%c%s%c%s", username, separator, realm, separator, password);
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            return md.digest(data.getBytes());
        } catch (NoSuchAlgorithmException e) {
            throw new TurnException("Unable to generate key", e);
        }
    }
}
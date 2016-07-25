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
package org.kaazing.gateway.transport.http.acceptor.specification.rfc7235;

import java.io.IOException;
import java.util.Arrays;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.UnsupportedCallbackException;

import org.kaazing.gateway.security.auth.BaseStateDrivenLoginModule;
import org.kaazing.gateway.server.spi.security.AuthenticationTokenCallback;

public class SimpleTestLoginModule extends BaseStateDrivenLoginModule {

    String[] validUserPasswords = {"joe", "welcome", "admin", "admin"};

    @Override
    protected boolean doLogin() {
        AuthenticationTokenCallback atc = new AuthenticationTokenCallback();

        try {
            handler.handle(new Callback[] {atc});
        } catch (IOException e) {
            // TODO: log exception
            System.out.println("AZUCKUT");
            return false;
        }
        catch (UnsupportedCallbackException e) {
            // TODO: log exception
            System.out.println("AZUCKUT");
            return false;
        }

        String up = atc.getAuthenticationToken().get();
        up = new String(decode(up));
        String name = up.substring(0, up.indexOf(':'));
        String passwordCB = up.substring(up.indexOf(':')+1);
        for ( int i = 0; i < validUserPasswords.length; i+=2) {
            String user = validUserPasswords[i];
            String password = validUserPasswords[i+1];
            if ( name.equals(user) &&
                 passwordCB.equals(password)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected boolean doCommit() {
        return true;
    }

    @Override
    protected boolean doLogout() {
        return true;
    }
    
    private final static char[] ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/".toCharArray();

    private static int[]  toInt   = new int[128];

    static {
        for(int i=0; i< ALPHABET.length; i++){
            toInt[ALPHABET[i]]= i;
        }
    }
    
    public static byte[] decode(String s){
        int delta = s.endsWith( "==" ) ? 2 : s.endsWith( "=" ) ? 1 : 0;
        byte[] buffer = new byte[s.length()*3/4 - delta];
        int mask = 0xFF;
        int index = 0;
        for(int i=0; i< s.length(); i+=4){
            int c0 = toInt[s.charAt( i )];
            int c1 = toInt[s.charAt( i + 1)];
            buffer[index++]= (byte)(((c0 << 2) | (c1 >> 4)) & mask);
            if(index >= buffer.length){
                return buffer;
            }
            int c2 = toInt[s.charAt( i + 2)];
            buffer[index++]= (byte)(((c1 << 4) | (c2 >> 2)) & mask);
            if(index >= buffer.length){
                return buffer;
            }
            int c3 = toInt[s.charAt( i + 3 )];
            buffer[index++]= (byte)(((c2 << 6) | c3) & mask);
        }
        return buffer;
    } 
}


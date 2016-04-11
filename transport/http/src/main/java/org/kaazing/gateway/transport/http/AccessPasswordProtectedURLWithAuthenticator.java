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
package org.kaazing.gateway.transport.http;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Authenticator;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.URL;

public class AccessPasswordProtectedURLWithAuthenticator {
    
    public static void main(String[] args) {
        
        try {
            
            // Sets the authenticator that will be used by the networking code
            // when a proxy or an HTTP server asks for authentication.
            Authenticator.setDefault(new CustomAuthenticator());
            
            URL url = new URL("http://localhost:8000/index.html");
            
            // read text returned by server
            BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
            
            String line;
            while ((line = in.readLine()) != null) {
                System.out.println(line);
            }
            in.close();
            
        }
        catch (MalformedURLException e) {
            System.out.println("Malformed URL: " + e.getMessage());
        }
        catch (IOException e) {
            System.out.println("I/O Error: " + e.getMessage());
        }
        
    }
    
    public static class CustomAuthenticator extends Authenticator {
        
        // Called when password authorization is needed
        protected PasswordAuthentication getPasswordAuthentication() {
            
            // Get information about the request
            String prompt = getRequestingPrompt();
            System.out.println("====" + getRequestingPrompt() + "====");
            String hostname = getRequestingHost();
            InetAddress ipaddr = getRequestingSite();
            int port = getRequestingPort();

            String username = "joe";
            String password = "welcome";

            // Return the information (a data holder that is used by Authenticator)
            return new PasswordAuthentication(username, password.toCharArray());
            
        }
        
    }

}
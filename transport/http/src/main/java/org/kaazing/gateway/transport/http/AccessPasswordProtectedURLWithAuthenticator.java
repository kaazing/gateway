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
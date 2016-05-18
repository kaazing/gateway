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
package org.kaazing.gateway.transport.wsn.auth;

import java.io.IOException;
import java.nio.charset.Charset;
import java.security.Principal;
import java.util.Map;
import java.util.Set;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;
import javax.xml.bind.DatatypeConverter;

import org.kaazing.gateway.server.spi.security.AuthenticationToken;
import org.kaazing.gateway.server.spi.security.AuthenticationTokenCallback;
import org.kaazing.gateway.server.spi.security.LoginResult;
import org.kaazing.gateway.server.spi.security.LoginResultCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimpleTokenLoginModule implements LoginModule {

    public static final Charset UTF8 = Charset.forName("UTF-8");

    public static final String CLASS_NAME = SimpleTokenLoginModule.class.getName();

    /**
     * The prefix that appears on all log output making it easier to identify.
     */
    public static final String LOG_PREFIX = "KST";

    public static final Logger logger = LoggerFactory.getLogger(CLASS_NAME);


    /**
     * The subject to be authenticated.
     */
    private Subject subject;

    /**
     * For sharing data between different Login Modules.
     */
    @SuppressWarnings("unused")
    private Map<String, ?> sharedState;

    private CallbackHandler handler;

    /**
     * Options specified from the Gateway configuration.
     */
    private Map<String, ?> options;


    /**
     * The principal that will be attached the LoginResult upon a successful authentication.
     */
    private Principal principal;

    @Override
    public void initialize(Subject subject, CallbackHandler handler, Map<String, ?> sharedState, Map<String, ?> options) {

        logDebug("initialize() WOOT!");

        this.subject = subject;
        this.handler = handler;
        this.sharedState = sharedState;
        this.options = options;

        // Print out all of the options passed in.
        //
        Set<?> keys = this.options.keySet();
        if ( keys.size() > 0 ) {
            logDebug("Options:");
            for (Object key : keys) {
                logDebug("  " + key + "=" + this.options.get(key));
            }
        }

    }

    @Override
    public boolean login() throws LoginException {
        
        logDebug("login()");
        
        LoginResult loginResult = getLoginResultFromCallback();
        if ( loginResult == null ) {
            logDebug("No LoginResult");
            return false;
        }

        // Get the authentication token container which contains any tokens. If it is empty, that means there were
        // no tokens present.
        AuthenticationToken authToken = getTokenFromCallback();
        if ( authToken.isEmpty() ) {
            logDebug("No tokens provided");

            return false;
        }

        logDebug("Obtained AuthenticationToken: " + authToken);

        // Make sure that the token data uses the "Token" authentication
        // scheme. If not, then it is not token data that we want to
        // parse/use.
        String authScheme = authToken.getScheme();
        // Note: Compare to "Token" because the scheme is just the scheme "type" which is Basic, Negotiate, or Token.
        if ( !"Token".equals(authScheme) ) {
            // Since this Login Module is for processing data based on the Token authentication scheme (as opposed to
            // Application Basic, for example), we can ignore this invocation. Return false to indicate this Login
            // Module did not authenticate the client.
            logDebug("Expected authentication token scheme 'Token', but received '" + authToken.getScheme()
                    + "'. Ignoring token");
            return false;
        }

        logDebug("Extracting token from HTTP Authorization header");
        String tokenData = authToken.get();

        if ( tokenData == null ) {
            logDebug("No token data supplied");
            //throw new LoginException("No token data supplied");
            return false;
        }

        // Strip off the authentication scheme prefix due to KG-4635. Once KG-4635 is fixed this can be
        // removed.
        if (tokenData.contains(authToken.getScheme())) {
            tokenData = tokenData.substring(authToken.getScheme().length() + 1);
        }

        logDebug("Extracted token data: [" + tokenData + "]");
        
        try {
            if ( !isTokenValid(tokenData) ) {
                logDebug("Failed to authenticate using Authorization token.");
                
                Integer attemptNumber = getAttemptNumber(tokenData);
                if(attemptNumber == null || attemptNumber >= 3){
                    logDebug("Send 403.");
                    throw new LoginException("Failed to authenticate using Authorization token");
                }else{
                    logDebug("Send 401.");
                    loginResult.challenge("Login failed. Please try again.");
                    return false;
                }
            }
        }
        catch (Exception e) {
            logError("Something went wrong validating the token", e);
            throw new LoginException("Something unexpected went wrong validating the token: "+e);
        }

        // If we got here then the token was successfully validated. Set the principal (i.e. the role) now so it can be
        // returned in the commit() method.
        principal = new Principal() {
            @Override
            public String getName() {
                return "AUTHORIZED";
            }
        };

        return true;
    }

    @Override
    public boolean commit() throws LoginException {
        logDebug("commit()");

        if ( principal == null ) {
            logDebug("No role granted");
            return false;
        }
        logDebug("User granted role: " + principal.getName());

        subject.getPrincipals().add(principal);

        return true;
    }

    @Override
    public boolean logout() throws LoginException {
        logDebug("logout()");
        // To clean up fully, remove any principals that this LoginModule added.
        subject.getPrincipals().remove(principal);
        return true;
    }

    @Override
    public boolean abort() throws LoginException {
        logDebug("abort()");
        // To clean up fully, remove any principals that this LoginModule added.
        subject.getPrincipals().remove(principal);
        return false;
    }

    public Integer getAttemptNumber(String token){
        String parts[] = token.split(":");
        
        if(parts.length != 3){
            return null;
        }
        Integer attemptNumber = null;
        
        try{
           attemptNumber = Integer.parseInt(parts[0],10);
        }catch(Exception e){
            e.printStackTrace();
        }
        
        return attemptNumber;
    }
    
    public String base64DecodeText(String data) {     
        return new String(DatatypeConverter.parseBase64Binary(data));
    }

    /**
     * Retrieve the LoginResult from the Login Context.
     * 
     * @return the LoginResult for this Login Module chain
     */
    private LoginResult getLoginResultFromCallback() {
        final LoginResultCallback loginResultCallback = new LoginResultCallback();
        try {
            handler.handle(new Callback[]{loginResultCallback});

        }
        catch (IOException ioe) {
            logError("Encountered exception handling loginResultCallback", ioe);
            return null;
        }
        catch (UnsupportedCallbackException uce) {
            logError("Encountered exception handling loginResultCallback", uce);
            return null;
        }

        return loginResultCallback.getLoginResult();
    }

    /**
     * Retrieve the authentication token
     * 
     * @return the authentication token from the client
     */
    private AuthenticationToken getTokenFromCallback() {
        final AuthenticationTokenCallback authTokenCallback = new AuthenticationTokenCallback();

        try {
            handler.handle(new Callback[]{authTokenCallback});
        }
        catch (IOException ioe) {
            logError("Encountered exception handling authTokenCallback", ioe);
            return null;
        }
        catch (UnsupportedCallbackException uce) {
            logError("Encountered exception handling authTokenCallback", uce);
            return null;
        }

        AuthenticationToken authToken = authTokenCallback.getAuthenticationToken();

        return authToken;
    }
    
    
    
    /**
     * Verify that the given token is valid.
     * 
     * @param token the token to be validated
     * @return true if this is a valid token, otherwise false
     * @throws Exception
     */
    private boolean isTokenValid(String token) throws Exception {

        String parts[] = token.split(":");
        System.out.println("XXXXXXXXXX parts=" + parts + "    token=" + token);
        if(parts.length != 3){
            return false;
        }
        
        // Connect to the token provider using HTTP which will validate the token.
        if("joe".equals(parts[1]) && "welcome".equals(parts[2])){
            return true;
        }

        return "jane".equals(parts[1]) && "welcome".equals(parts[2]);

    }

    private void logDebug(String message) {
        logger.debug("[" + LOG_PREFIX + "] " + message);
    }

    private void logError(String message, Exception exception) {
        logger.error("[" + LOG_PREFIX + "] " + message, exception);
    }

}

Configure the HTTP Challenge Scheme
==========================================================================================

In this procedure, you will learn how to configure authentication by defining the `security` element and specifying the HTTP challenge scheme that protects the service.

Before You Begin
----------------

This procedure is part of [Configure Authentication and Authorization](o_auth_configure.md):

1.  **Configure the HTTP Challenge Scheme**
2.  [Configure a Chain of Login Modules](p_auth_configure_login_module.md)
3.  [Configure a Challenge Handler on the Client](p_auth_configure_challenge_handler.md)
4.  [Configure Authorization](p_authorization_configure.md)

To Configure the HTTP Challenge Scheme
--------------------------------------

1.  On the server, update the Gateway configuration (for example, by editing `GATEWAY_HOME/conf/gateway-config.xml` in a text editor).
2.  Determine the type of HTTP challenge scheme you want to configure.

    The following table summarizes the schemes you can configure and the affiliated authentication parameters with which the client or browser can respond to the Gateway’s challenge.

    | HTTP Challenge Scheme   | Challenge is Handled By ... | Gateway Challenges the Client to Authenticate Itself Using ...                 | Client or Browser Responds to the Gateway Challenge Using ...                                                  |
    |-------------------------|-----------------------------|--------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------|
    | `Basic`                 | Browser                     | Username and password                                                          | `BasicChallengeHandler`, `ChallengeHandler`                                                                        |
    | `Application Basic`     | Client                      | Username and password                                                          | `BasicChallengeHandler`, `ChallengeHandler`, `LoginHandler`                                                         |
    | `Negotiate`             | Browser                     | A Negotiated scheme per RFC 4559\*                                             | `NegotiateHandler`, `NegotiableHandler`, `LoginHandler`                                                             |
    | `Application Token`     | Client                      | A custom token or HTTP cookies, usually expected by a custom login module.\*\* | A custom-written challenge handler and/or login handler that can generate the expected token or cookie value.\*\* |

    **Note:** Previous versions of Kaazing Gateway allowed the `Application Negotiate` scheme (per RFC 4559). `Application Negotiate` is now **deprecated** in Kaazing Gateway. Please use `Negotiate` instead.

    \* The HTTP Negotiate scheme is based on using Object Identifiers (OIDs) per RFC 4559 to identify kinds of tokens. If you use or register your own OID, then you can use that OID with the `NegotiateHandler` and `NegotiableHandler` challenge handlers.

    \*\* If you are configuring a custom login module on the Gateway, then you must code the accompanying custom challenge handler in the client.

3.  Locate the `security` section of the Gateway configuration and define a realm that includes the `http-challenge-scheme`.

    The `realm` element is a part of the `security` element in the Gateway configuration, and its job is to provide authentication information that associates an authenticated user with a set of authorized roles. You can think of a realm as a logical grouping of users, groups (roles), and access.

    For example, to configure a client to respond to a custom authentication challenge and require authentication with a third-party token for the demo realm, you would configure `Application Token` in the `http-challenge-scheme` element, as shown in the following example:

    ``` xml
    <security>
      <keystore>
       <type>JCEKS</type>
       <file>keystore.db</file>
       <password-file>keystore.pw</password-file>
      </keystore>

      <truststore>
        <file>truststore.db</file>
      </truststore>

      <realm>
        <name>demo</name>
        <description>Demo</description>
        <authentication>
          <http-challenge-scheme>Application Token</http-challenge-scheme>
          <http-header>X-Custom-Authorization-Header</http-header>
          <http-query-parameter>myCustomAuthParam</http-query-parameter>
          <http-cookie>sampleCookie1</http-cookie>
        </authentication>  
      </realm>
    </security>
    ```

4.  Save `gateway-config.xml`.

The Gateway matches the *gateway.hostname* for this domain to look up the authentication scheme. The Gateway uses the cookie name defined by `http-cookie` element as the authentication token to log in. The cookie value become accessible in the login module that reads the cookies using the `AuthenticationToken` class.

Notes
-----

-   Use the `Basic` and `Application Basic` schemes to provide a quick and easy-to-implement method, requiring only a username and password for authentication. However, these are the least secure schemes and are subject to several threats, not least of which is the fact that the username/password can easily be sniffed in transit by an attacker.
-   Use the `Negotiate` scheme when using Kerberos Network Authentication. For more information, see [Configuring Kerberos V5 Network Authentication](o_kerberos.md).
-   Use the `Application Token` scheme when you need a custom token to be presented to your custom login module. See [Create a Custom Login Module](p_auth_configure_custom_login_module.md) for configuration information. `Application Token` provides strong authentication because you can implement your own custom scheme that is cryptographically protected to challenge the client. When you configure custom authentication with the `Application Token` element, you must also:
    -   Configure a custom login module in the Gateway that defines how to encode/decode the token challenge data, and code the matching challenge handler on the client. See [Create a Custom Login Module](p_auth_configure_custom_login_module.md) for more information.
    -   Create a custom challenge handler on the client to support the custom login module. See [Configure a Challenge Handler on the Client](p_auth_configure_challenge_handler.md) for more information.
-   In addition to securing networking with the Gateway as described in [Secure Network Traffic with the Gateway](../security/o_tls.md), using a cryptographic hash function such as [bcrypt](http://en.wikipedia.org/wiki/Bcrypt) or a key derivation function such as [PBKDF2](http://en.wikipedia.org/wiki/PBKDF2) to protect passwords is highly recommended.

Next Steps
----------

[Configure a Chain of Login Modules](p_auth_configure_login_module.md)

See Also
------------------------------

-   [Configure the Gateway](../admin-reference/o_configure_gateway_checklist.md)
-   [About Authentication and Authorization](c_auth_about.md)
-   [What Happens During Authentication](u_authentication_gateway_client_interactions.md)
-   [How Authentication and Authorization Work with the Gateway](u_auth_how_it_works_with_the_gateway.md)

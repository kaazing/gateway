-   [Home](../../index.md)
-   [Documentation](../index.md)
-   Configure a Challenge Handler on the Client

<a name="config_challengehandler"></a>Configure a Challenge Handler on the Client
=======================================================================================================

Client-level (application-level) security consists of challenge handlers and login handlers. A challenge handler on the client receives the authentication challenge from the Gateway, and is responsible for responding to the Gateway using the user credentials in the appropriate format. Conceptually, a challenge handler and a login module are paired, such that a challenge handler in the client is coded to accept and respond to challenges from the login module on the Gateway. For every login module, you must code the accompanying custom challenge handler in the client.

Before You Begin
----------------

This procedure is part of [Configure Authentication and Authorization](o_aaa_config_authentication.md):

1.  [Configure the HTTP Challenge Scheme](p_aaa_config_authscheme.md)
2.  [Configure a Chain of Login Modules](p_aaa_config_lm.md)
3.  **Configure a Challenge Handler on the Client**
4.  [Configure Authorization](p_aaa_config_authorization.md)

To Configure a Challenge Handler on the Client
----------------------------------------------

Continuing our example from the document [What Happens During Authentication](u_aaa_gw_client_interactions.md), when a client requests access to a protected service at the host name, the Gateway:

1.  Consults the configuration parameters that are supplied at startup and looks for the service being accessed and the `Application Token` authentication scheme.
2.  Sends the authorization challenge to the client and includes the` WWW-Authenticate` header that challenges the client to supply login credentials, which in this case happens to be a token.

The client interprets the authorization challenge using a challenge handler and optionally a login handler to respond to the server and provide the requested authentication information:

-   The challenge handler responds to the challenge by:
    -   Identifying the authentication scheme sent by the Gateway (for example, `Basic`, `Negotiate`, `Application Token`)
    -   Replying with the appropriate client credentials placed in an authorization header.
-   A login handler can be part of a challenge handler’s response but responds to the challenge in a more simplistic way. For example, if the authentication scheme is `Basic`, then it is username/password-based and the login handler might collect the user’s credentials by popping up a login window. When the user logs into the pop-up window, the credentials are then encoded, and sent back to the Gateway in an HTTP request by the challenge handler.

To configure the challenge handler on your KAAZING Gateway client platform, see the security topics in [For Developers](../index.md#dev_topics).

Notes
-----

-   You only need to implement a login handler if the client is going to collect credentials. Some applications are customized to handle credentials, so there's no need to write a login handler.
-   You can dynamically configure the client to use login handlers by associating them with challenge handlers, and associating those challenge handlers with WebSocket locations.

Next Steps
----------

[Configure Authorization](p_aaa_config_authorization.md)
<a name="seealso"></a>See Also
------------------------------

-   [About Authentication and Authorization](c_aaa_aaa.md)
-   [How Authentication and Authorization Work with the Gateway](u_aaa_implement.md)
-   [Client API Documentation](../index.md#api_topics)
-   [Server API Documentation](../index.md#server_api_topics)



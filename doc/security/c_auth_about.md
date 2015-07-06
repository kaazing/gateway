About Authentication and Authorization
=======================================================================================

You can configure KAAZING Gateway for secure communication between clients and the Gateway to ensure that only the intended recipient of a message can read the transmitted message and can trust that the message is from the expected source. The Gateway protects your data and authenticates that users are who they say they are, and that they take only authorized actions.

-   Authentication is the mechanism by which a system identifies a user and verifies whether or not the user really is who he represents himself to be. To start the authentication process, the Gateway issues a standard challenge using the `HTTP 401 Authorization Required` code. The browser or client then responds by providing the requested authentication information.
-   Authorization is the mechanism by which a system determines what level of access a particular user has. Even after a user is successfully authenticated for general access, it doesn't mean the user is entitled to perform any operation. Authorization is concerned with individual user rights. For example, what can the user do once authenticated---configure the device or only view the data? In this case you could authorize the user with viewer, moderator, or administrator privileges to the web page. Access rights are typically stored in the policy store that is associated with the application.

KAAZING Gateway supports HTTP authentication and authorization methods and techniques to keep users and information safe over the Web. The Gateway provides client libraries that allow you to integrate HTTP authorization and authentication into your application. Encrypted credentials sent to the Gateway are automatically injected into the protocol before authenticating with the back-end system, eliminating the time and risk of a long, multipassword authentication sequence but without compromising credential storage. The handshake that upgrades the connection looks like a HTTP handshake. Cookies and authorization headers are fully supported.

The Gateway also integrates with Java Authentication and Authorization Service (JAAS), which is a standards-based Java security framework and API that enables services to verify and enforce access controls on users. Being based on JAAS means that you can plug any authentication technology into the Gateway, providing the ability to upgrade your existing authentication technology or move to another provider without requiring changes to your applications.

See Also
------------------------------

-   [Configure Authentication and Authorization](o_auth_configure.md)
-   [What Happens During Authentication](u_authentication_gateway_client_interactions.md)
-   [How Authentication and Authorization Work with the Gateway](u_auth_how_it_works_with_the_gateway.md)

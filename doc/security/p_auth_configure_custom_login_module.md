Create a Custom Login Module
=========================================================================

Even though KAAZING Gateway provides several standard types of `login-module` implementations, you might choose to write a custom implementation. Because the JAAS interface implementation holds the authentication logic, most of the material you need to implement the `LoginModule` interface is provided in the following documentation:

-   [Java Authentication and Authorization Service (JAAS) LoginModule Developer's Guide](http://docs.oracle.com/javase/7/docs/technotes/guides/security/jaas/JAASLMDevGuide.html)
-   [Interface LoginModule class](http://docs.oracle.com/javase/7/docs/api/javax/security/auth/spi/LoginModule.html) (Java SE Developer's Documentation)
-   KAAZING Gateway [Client API Documentation](../index.md)

Before You Begin
----------------

This optional procedure is part of [Configure Authentication and Authorization](o_auth_configure.md):

1.  [Configure the HTTP Challenge Scheme](p_authentication_config_http_challenge_scheme.md)
2.  [Configure a Chain of Login Modules](p_auth_configure_login_module.md)
    -   **Create a Custom Login Module (Optional)**
    -   [Integrate an Existing Custom Login Module into the Gateway (Optional)](p_auth_integrate_custom_login_module.md)

3.  [Configure a Challenge Handler on the Client](p_auth_configure_challenge_handler.md)
4.  [Configure Authorization](p_authorization_configure.md)

To Configure Custom Login Modules
---------------------------------

1.  Write the login module interface (`LoginContext` API).
2.  Write the `CallBackHandler` interface that enables client to pass authentication data to the server.
3.  Configure the `LoginModule` and `CallBackHandler` with the server and application.
4.  Package the application along with module classes.
5.  Integrate the `LoginModule` with the application server.

Next Steps
----------

[Integrate an Existing Custom Login Module into the Gateway](p_auth_integrate_custom_login_module.md)

See Also
------------------------------

-   [Configure the Gateway](../admin-reference/o_configure_gateway_checklist.md)
-   [Server API Documentation](../index.md)

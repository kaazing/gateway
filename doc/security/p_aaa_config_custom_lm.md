-   [Home](../../index.md)
-   [Documentation](../index.md)
-   Create Custom Login Modules

Create a Custom Login Module
=========================================================================

Even though KAAZING Gateway provides several standard types of `login-module` implementations, you might choose to write a custom implementation. Because the JAAS interface implementation holds the authentication logic, most of the material you need to implement the `LoginModule` interface is provided in the following documentation:

-   [Java Authentication and Authorization Service (JAAS) LoginModule Developer's Guide](http://docs.oracle.com/javase/7/docs/technotes/guides/security/jaas/JAASLMDevGuide.html)
-   [Interface LoginModule class](http://docs.oracle.com/javase/7/docs/api/javax/security/auth/spi/LoginModule.html) (Java SE Developer's Documentation)
-   KAAZING Gateway [Client API Documentation](../index.md)

Before You Begin
----------------

This optional procedure is part of [Configure Authentication and Authorization](o_aaa_config_authentication.md):

1.  [Configure the HTTP Challenge Scheme](p_aaa_config_authscheme.md)
2.  [Configure a Chain of Login Modules](p_aaa_config_lm.md)
    -   **Create a Custom Login Module (Optional)**
    -   [Integrate an Existing Custom Login Module into the Gateway (Optional)](p_aaa_integ_custom_lm.md)

3.  [Configure a Challenge Handler on the Client](p_aaa_config_ch.md)
4.  [Configure Authorization](p_aaa_config_authorization.md)

To Configure Custom Login Modules
---------------------------------

1.  Write the login module interface (`LoginContext` API).
2.  Write the `CallBackHandler` interface that enables client to pass authentication data to the server.
3.  Configure the `LoginModule` and `CallBackHandler` with the server and application.
4.  Package the application along with module classes.
5.  Integrate the `LoginModule` with the application server.

Next Steps
----------

[Integrate an Existing Custom Login Module into the Gateway](p_aaa_integ_custom_lm.md)

See Also
------------------------------

-   [Configure the Gateway](../admin-reference/o_conf_checklist.md)
-   [Server API Documentation](../index.md)

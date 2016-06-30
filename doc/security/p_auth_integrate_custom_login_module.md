Integrate an Existing Custom Login Module into the Gateway
=============================================================================================================

KAAZING Gateway supports a plug-in mechanism for integration with custom authentication modules based on the Java `LoginModule` API. This document provides an example of how you can use your existing custom login module with the Gateway.

Before You Begin
----------------

This optional procedure is part of [Configure Authentication and Authorization](o_auth_configure.md):

1.  [Configure the HTTP Challenge Scheme](p_authentication_config_http_challenge_scheme.md)
2.  [Configure a Chain of Login Modules](p_auth_configure_login_module.md)
    -   [Create a Custom Login Module (Optional)](p_auth_configure_custom_login_module.md)
    -   **Integrate an Existing Custom Login Module into the Gateway (Optional)**

3.  [Configure a Challenge Handler on the Client](p_auth_configure_challenge_handler.md)
4.  [Configure Authorization](p_authorization_configure.md)

To Integrate an Existing Custom Login Module into the Gateway
----------------------------------------------------------------

1.  Copy the login module to the `GATEWAY_HOME/web/base` directory.

    Before you begin, you need the Gateway, your Java custom login module (for demo purposes, you can use the sample custom login module called `GATEWAY_HOME/web/extras/samples/security/CustomLoginModule.java`), and the fully qualified class name of your custom login module. Note that the `GATEWAY_HOME/web/extras` directory is read-only. So, to modify the provided `CustomLoginModule.java` file, you must copy the file to your `GATEWAY_HOME/web/base` directory.

2.  Compile your Java custom login module file into a JAR file and place it in `GATEWAY_HOME/lib`. You can also update the `CLASSPATH` to point to the desired directory containing the JAR file.
3.  In the Gateway configuration (for example, `GATEWAY_HOME/conf/gateway-config.xml`), add the custom login module and set the `type` to point to `class:the-fully-qualified-class-name`. For example, if you are using the sample custom login module provided with the Gateway, the fully qualified class name is `org.kaazing.demo.loginmodules.CustomLoginModule`. The following is the `login-module` entry for this sample:

    ``` xml
    <login-module>
      <type>class:org.kaazing.demo.loginmodules.CustomLoginModule</type>
      <success>required</success>
    </login-module>
    ```

4.  Enable configuration for the services that are required to use this custom login module to authenticate with the back-end server. You can do this using the `authorization-constraint` element. The following is an example of the echo service configured to use this custom login module:

    ``` xml
    <service>
      <accept>ws://localhost:8001/echo</accept>
      <accept>wss://localhost:9001/echo</accept>
      <type>echo</type>

      <authorization-constraint>
        <require-role>AUTHORIZED</require-role>
      </authorization-constraint>

      <cross-site-constraint>
        <allow-origin>http://localhost:8000</allow-origin>
      </cross-site-constraint>

      <cross-site-constraint>
        <allow-origin>https://localhost:9000</allow-origin>
      </cross-site-constraint>
    </service>
    ```

5.  Add this login module to the chain, as described in [Configure a Chain of Login Modules](p_auth_configure_login_module.md).
6.  Save `gateway-config.xml` and restart the Gateway.

Next Steps
----------

[Configure a Challenge Handler on the Client](p_auth_configure_challenge_handler.md)

See Also
------------------------------

-   [Configure the Gateway](../admin-reference/o_configure_gateway_checklist.md)
-   [Server API Documentation](../index.md)

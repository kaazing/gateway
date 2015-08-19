Configure a Chain of Login Modules
===================================================================================

You configure one or more login modules, called a *chain of login modules*, to instruct the Gateway on how to validate user credentials with a user database and to determine a set of authorized roles. The chain of login modules decode and verify the credentials, and (along with the challenge handler) handle the challenge/response authentication sequence of events.

Before You Begin
----------------

This procedure is part of [Configure Authentication and Authorization](o_auth_configure.md):

1.  [Configure the HTTP Challenge Scheme](p_authentication_config_http_challenge_scheme.md)
2.  **Configure a Chain of Login Modules**
3.  [Configure a Challenge Handler on the Client](p_auth_configure_challenge_handler.md)
4.  [Configure Authorization](p_authorization_configure.md)

To Configure a Chain of Login Modules
-------------------------------------

1.  Determine the type of login modules you want to define.

    A `login-module` evaluates the encoded login credentials that the Gateway passes to it. The Gateway supports the following types of login modules:

    -   **Built-in login module provided by KAAZING Gateway**

        The Gateway provides the `file` login module for easy-to-implement security that is integrated with the Gateway.

    -   **Standard (public) JDK-provided login modules**

        The Gateway supports `ldap`, `kerberos5`, `gss`, `jndi`, and `keystore` login modules, which are some of the most commonly used login modules for authentication and authorization purposes. In these implementations, you do not need to write your own login module solution. For information about using the `kerberos5` and `gss` `login-module` elements, see [Configuring Kerberos V5 Network Authentication](o_kerberos.md).

    -   **Custom login modules**

        If you use the `Application Token` authentication scheme, you must supply your own custom login module. See [Create a Custom Login Module](p_auth_configure_custom_login_module.md) and [Integrate an Existing Custom Login Module into the Gateway](p_auth_integrate_custom_login_module.md).

2.  Add the `login-modules` element within the `authentication` element that you started in Step 1. The `login-modules` element is the container for one or more login modules and it defines the scope in which security policies are enforced.
3.  Define one or more login modules to make a chain.

    Each login module in the chain is responsible for doing a little piece of work and passing along information. For example, one login module might check a database, another login module might contact an LDAP directory, and so on.

    In the following example, the chain of login modules includes the `file` type (to handle the `jaas-config.xml` that is part of KAAZING Gateway) and the `ldap` type. For a complete security example, see the **Notes** section below.

    ``` xml
          <login-modules>
            <login-module>
              <type>file</type>
              <success>requisite</success>
              <options>
                <filename>jaas-config.xml</filename>
              </options>
            <login-module>

            <login-module>
              <type>ldap</type>
              <success>required</success>
              <options>
                <userProvider>ldap://ldap-svr:389/ou=people,dc=example,dc=com</userProvider>
                <userFilter>
                  <![CDATA[(&amp;(userPrincipalName={USERNAME}@MYCOMPANY.NET)
                    (objectClass=inetOrgPerson))]]>
                </userFilter>
                <authzIdentity>{EMPLOYEENUMBER}</authzIdentity>
              </options>
            </login-module>
          </login-modules>
    ```

    In the example:

    -   The `login-module` at the start of the chain often asks the Gateway for more information, via a *callback* into the Gateway (for example, using a token).
        -   If the `file` login module fails, then authentication processing does not progress down the chain.
        -   If the `file` login module succeeds, then processing continues to the next login module in the example, which is the `ldap` login module.
    -   The `login-module` at the end of the chain verifies the information against a database, such as the `jaas-config.xml` file or the LDAP server. Because the `ldap` login module is the last one in the chain, the user authenticates successfully and the Gateway establishes the client's list of authorized roles and permits the WebSocket connection to proceed (`101 Protocol Upgrade`). Otherwise, it denies the WebSocket creation request (`403 Forbidden`).

4.  Configure a `success` element for each login module.

    If there is more than one login-module configured in a realm, then configure the semantics and processing order for a chain of login modules using the `success` element. In the example shown in the previous step, setting the `requisite` property indicates that the `file` login module must succeed. See the table in the Notes section for more information about setting the `success` element.

5.  Ensure the corresponding `service` element’s `http-challenge-scheme` type aligns with the set of login modules in the Gateway configuration.

    For example, the `file` login module may require a username and password as part of its login needs. Thus, you should probably configure the `basic` authentication scheme that is expecting the user to login with a username and password. If you configure the chain of login modules to expect only tokens (such as with a `custom` login module), then configure the `http-challenge-scheme` element for `Negotiate`, `Application Negotiate`, or `Application Token`.

    **Note:** If you use the `Application Token` scheme, then you must write your own custom login module.

6.  Save the Gateway configuration.

Notes
-------------------------

The `success` element controls the behavior of the individual login modules, but the order of the login modules controls the overall behavior as the process of authentication walks down the stack. When there is more than one login-module configured in a `realm` you should carefully plan the order in which the login modules appear in the Gateway configuration because the login modules are invoked in sequence.

The following table describes how the order of login modules and the setting of the `success` element controls authentication processing:

| `success` values | Description                                    | On success or failure ...                                                                                                                                                                                                            |
|------------------|------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `required`       | The `login-module` must succeed.               | If it succeeds or fails, then authentication continues to proceed down the `login-module` list.                                                                                                                                      |
| `requisite`      | The `login-module` must succeed.               | If it succeeds, then authentication continues down the `login-module` list. If it fails, then authentication stops its process down the `login-module` list and the Gateway denies the WebSocket creation request `(403 Forbidden`). |
| `sufficient`     | The `login-module` is not required to succeed. | If it succeeds, then authentication stops its process down the `login-module` list and the Gateway opens the WebSocket connection. If it fails, authentication continues down the `login-module` list.                               |
| `optional`       | The `login-module` is not required to succeed. | If it succeeds or fails, then authentication proceeds down the `login-module` list.                                                                                                                                                  |

**In addition:**
-   If a `sufficient` login module is configured and succeeds, then only the `required` and `requisite` login modules prior to that `sufficient` login module need to have succeeded for the overall authentication to succeed.
-   If no `required` or `requisite` login modules are configured for an application, then at least one `sufficient` or `optional` login module must succeed.

The following code shows a complete security example:

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
    <authentication>
      <http-challenge-scheme>Basic</http-challenge-scheme>
      <http-header>X-Custom-Authorization-Header</http-header>
      <http-query-parameter>myCustomAuthParam</http-query-parameter>
      <http-cookie>sampleCookie1</http-cookie>

      <login-modules>
        <login-module>
          <type>file</type>
          <success>required</success>
          <options>
            <filename>jaas-config.xml</filename>
          </options>
        <login-module>

        <login-module>
          <type>ldap</type>
          <success>required</success>
          <options>
            <userProvider>ldap://ldap-svr:389/ou=people,dc=example,dc=com</userProvider>
            <userFilter>
              <![CDATA[(&amp;(userPrincipalName={USERNAME}@MYCOMPANY.NET)
              (objectClass=inetOrgPerson))]]>
            </userFilter>
            <authzIdentity>{EMPLOYEENUMBER}</authzIdentity>
          </options>
        </login-module>
      </login-modules>
    </authentication>  
  </realm>
</security>
```

Next Steps
----------

[Configure a Challenge Handler on the Client](p_auth_configure_challenge_handler.md)

See Also
------------------------------

-   [Configure the Gateway](../admin-reference/o_configure_gateway_checklist.md)
-   [About Authentication](c_auth_about.md)
-   [What Happens During Authentication](u_authentication_gateway_client_interactions.md)
-   [How Authentication and Authorization Work with the Gateway](u_auth_how_it_works_with_the_gateway.md)
-   [Server API Documentation](../index.md)

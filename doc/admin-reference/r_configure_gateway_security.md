Security Reference
=========================================

This document describes all of the elements and properties you can use to configure KAAZING Gateway security.

Overview
------------------------------------------

You can use the optional `security` element to configure secure communication with the Gateway.

Structure
--------------------------------------------

The Gateway configuration file (`gateway-config.xml` or `gateway-config-minimal.xml`) defines the `security` configuration element contained in the top-level `gateway-config` element:

- [gateway-config](r_configure_gateway_gwconfig.md)
  - [security](#security)
    - [keystore](#keystore)
      - [type](#keystore)
      - [file](#keystore)
      - [password-file](#keystore)
    - [truststore](#truststore)
      - [type](#truststore)
      - [file](#truststore)
      - [password-file](#truststore)
    - [realm](#realm)
      - [name](#realm)
      - [description](#realm)
      - [authentication](#authentication)
        - [http-challenge-scheme](#authentication)
        - [http-header](#authentication)
        - [http-query-parameter](#authentication)
        - [http-cookie](#authentication)
        - [authorization-mode](#authentication)
        - [authorization-timeout](#authentication) ![This feature is available in KAAZING Gateway - Enterprise Edition](../images/enterprise-feature.png)
        - [session-timeout](#authentication) ![This feature is available in KAAZING Gateway - Enterprise Edition](../images/enterprise-feature.png)
        - [login-modules](#login-module)
           - [login-module](#login-module)
              - [type](#login-module)
              - [success](#login-module)
              - [options](#options-login-module)
                 - [debug](#options-login-module)
                 - [tryFirstToken](#options-login-module)
       - [user-principal-class](#realm)

**Note:** The security elements and properties must be specified in the order shown.

security
-------------------------------

**The** `security` element contains the following elements:

| Element    | Description                                                                                                                         |
|:-----------|:------------------------------------------------------------------------------------------------------------------------------------|
| keystore   | The keystore contains the encryption keys for secure communications with KAAZING Gateway (see [keystore](#keystore))                |
| truststore | The truststore contains digital certificates for certificate authorities trusted by KAAZING Gateway (see [truststore](#truststore)) |
| realm      | The realm associates an authenticated user with a set of authorized roles (see [realm](#realm))                                     |

### keystore

**Required?** Optional; **Occurs:** zero or one

Identifies the keystore file that contains the certificates for the host names accepted by the Gateway over TLS (formerly SSL). When the Gateway is configured to accept `https://` or `wss://` schemes, the Gateway uses the certificates and key pairs in the keystore to establish a TLS session with clients connecting via those schemes. `keystore` contains the following elements:

| Element       | Description                                                                                                                                                                             |
|:--------------|:----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| type          | The type (format) of the keystore file. The supported type is `JCEKS` (Java Cryptography Extension key store). If the `type` element is not specified, the Gateway throws an exception. |
| file          | The location of the keystore file (absolute path or path relative to the `gateway-config.xml` file).                                                                                    |
| password-file | The name of the file containing the password used by the Gateway and other applications to access the keystore file.                                                                    |

#### Example

``` xml
<keystore>
  <type>JCEKS</type>
  <file>keystore.db</file>
  <password-file>keystore.pw</password-file>
</keystore>
```

#### Notes

-   The keystore database file is located in `GATEWAY_HOME/conf` and referenced in the keystore element in the Gateway configuration file.
-   The Gateway supports certificates that are generated for wildcard domains. For example, `*.example.com` represents example.com and all its subdomains, such as my.example.com and secure-mail.example.com.
-   See [Secure Network Traffic with the Gateway](../security/o_tls.md) for more information and examples.

### truststore

**Required?** Optional; **Occurs:** zero or one

Identifies the truststore file that contains certificates for hosts and certificate authorities trusted by the Gateway. To connect to a back-end service or message broker using TLS/SSL, the Gateway must have the certificate for the host name of the back-end service or message broker in the truststore. `truststore` contains the following elements:

| Element                                             | Description                                                                                                                                                                                                                                                                                                          |
|:----------------------------------------------------|:---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| type                  | Not required by default. The type (format) of the truststore file. The default supported type is `JKS` (Java Key Store). If you specify `JCEKS` (Java Cryptography Extension Key Store) when importing a certificate into the truststore, then you must add `<type>JCEKS</type>` or the Gateway throws an exception. |
| file                  | The location of the truststore file (absolute path or path relative to the `gateway-config.xml` file). There is no password file associated with the truststore. The default password is `changeit`. You should modify this password using the `keytool -storepasswd` command.                                       |
| password-file | The name of the file containing the password used by the Gateway and other applications to access the truststore file.                                                                                                                                                                                               |

#### Example

``` xml
<keystore>
  <type>JCEKS</type>
  <file>keystore.db</file>
  <password-file>keystore.pw</password-file>
</keystore>

<truststore>
  <file>truststore.db</file>
</truststore>
```

#### Notes

-   See [Secure Network Traffic with the Gateway](../security/o_tls.md) for more information and examples.

### realm

**Required?** Optional; **Occurs:** zero or more

This is the element that associates an authenticated user with a set of authorized roles. `realm` contains the following elements:

| Element              | Description                                                                                                                                                                                                                                                                                                                                                                                                          |
|:---------------------|:---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| name                 | The name of the realm.                                                                                                                                                                                                                                                                                                                                                                                               |
| description          | The description of the realm.                                                                                                                                                                                                                                                                                                                                                                                        |
| authentication       | The authentication information the Gateway uses to challenge the user, respond to the challenge response, process logins, and govern the authorization and session timeouts (see [authentication](#authentication)).                                                                                                                                                                                                 |
| user-principal-class | The name of the class that represents a user principal that an administrator wants to know about. When a principal of this class is authenticated, a notification of such is sent through the management service. If no management service is configured, then this element is ignored. The class must implement [`java.security.Principal`](http://docs.oracle.com/javase/7/docs/api/java/security/Principal.html). |

#### Example

``` xml
<security>
  ...
  <realm>
    <name>demo</name>
    <description>KAAZING Gateway Demo</description>
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

#### Notes

-   See [Secure Network Traffic with the Gateway](../security/o_tls.md) for more information and examples.
-   See [Monitor with JMX](../management/p_monitor_jmx.md) for an example configuring notifications using `user-principal-class` to extract user information from a session using JMX.

### authentication

**Required?** Required; **Occurs:** at most once

Use `authentication` to configure the authentication parameters for the `realm`, which includes the challenge-scheme and the parts of the request that contain authentication information. `authentication` contains the following elements:

- ##### `http-challenge-scheme`
  This element is required and specifies the method used for authentication: `Basic`, `Application Basic`, `Negotiate`, and `Application Token`:

  - Use `Basic` or `Negotiate` to allow the browser to respond to authentication challenges.
  - Use `Application Basic` to allow the client to respond to authentication challenges. The client in this case is the KAAZING Gateway client that is built based on the KAAZING Gateway client libraries. To use client-level authentication, configure the client to handle the authentication information, as described in [developer how-to](../index.md) documentation.
  - Use `Application Token` to allow the client to present a third-party token or custom token to be presented to your custom login module.  
  - Use `Negotiate` if using Kerberos Network Authentication. For more information, see [Secure Network Traffic with the Gateway](../security/o_tls.md).

- ##### `http-header`
  Specifies the names of the header or headers that carry authentication data for use by the login modules in this realm. This element is optional. If you do not specify it, then the Gateway uses `<http-header>Authorization</http-header>` for the challenge response (see [Custom HTTP Authentication Tokens](#custom-http-authentication-tokens)).

- ##### `http-query-parameter`
  Specifies the name or names of query parameters that carry authentication data for use by the login modules in this realm. This element is optional. If you do not specify it, then the Gateway uses `<http-header>Authorization</http-header>` for the challenge response (see [Custom HTTP Authentication Tokens](#custom-http-authentication-tokens)).

- ##### `http-cookie`
  Specifies the name or names of HTTP cookies that carry authentication data for use by the login modules in this realm. This element is optional. If you do not specify it, then the Gateway uses `<http-header>Authorization</http-header>` for the challenge response (see [Custom HTTP Authentication Tokens](#custom-http-authentication-tokens)).

- ##### `authorization-mode`
  Specifies the challenge or recycle mode the Gateway uses to handle credentials provided when the client logs in.
  - Use `challenge` to enable the Gateway to challenge the client for credentials when none are presented. The Gateway will not write its own authorization session cookie.Use recycle to enable the Gateway to write its own authorization session cookie.
  - Use `recycle` to enable the Gateway to write its own authorization session cookie. The `recycle` option is only applicable to `Basic`, `Application Basic`, and `Negotiate` HTTP challenge schemes.

- ##### `authorization-timeout`![This feature is available in KAAZING Gateway - Enterprise Edition](../images/enterprise-feature.png)
  For directory services, this element specifies the time interval that must elapse without service access before the Gateway challenges the client for credentials.

  For WebSocket services, this is the time interval before which the client must reauthenticate the WebSocket. If reauthentication has not occurred within the specified time, then the Gateway closes the WebSocket connection.

- ##### `session-timeout`![This feature is available in KAAZING Gateway - Enterprise Edition](../images/enterprise-feature.png)
  For WebSocket services only. This is the time interval after which the Gateway closes the WebSocket connection, regardless of other settings. Effectively, the `session-timeout` element specifies the maximum lifetime of the WebSocket connection.

- ##### `login-modules`
  Specifies the container for a chain of individual login modules that communicate with a user database to validate user's credentials and to determine a set of authorized roles (see [login-module](#login-module)).

#### Custom HTTP Authentication Tokens

By default, the HTTP standard ([RFC 2617](http://tools.ietf.org/html/rfc2617)) specifies the use of the authorization header `(<http-header>Authorization</http-header>`) for sending credentials from the client to the server. The Gateway configuration elements `http-header`, `http-query-parameter`, and `http-cookie` provide a means of extending the standard. You use these elements to allow the Gateway configuration to specify other parts of an HTTP request that can carry authentication credentials.

For example, suppose the client's authentication system provides authentication information in an authorization header, a cookie and a query parameter to the server. In this scenario, you might set up the Gateway configuration, as follows:

1.  Declare this authentication information in the Gateway configuration.

    The Gateway always obtains the authentication values from the authorization header (if any) and the Gateway also obtains any declared values from one or more authorization headers, query parameters, or cookies that are explicitly specified in the `http-header`, `http-query-parameter`, and `http-cookie` elements, respectively. For example:

    ``` xml
    <http-header>X-Acme-Authorization</http-header>
    <http-query-parameter>auth</http-query-parameter>
    <http-cookie>Acme</http-cookie>
    ```

2.  Make the values sent via an HTTP request available to your custom login module using the `AuthenticationToken` object.

    To declare explicit values in the login modules (configured as a chain within the [realm](#realm) element), use the `AuthenticationToken` object made available to the login modules through an `AuthenticationTokenCallback`, as follows:

    ``` xml
    AuthenticationToken token = getTokenFromCallback();
    String headerPart = token.get(“X-Acme-Authorization”);
    String parameterPart = token.get(“auth”);
    String cookiePart = token.get(“Acme”);
    ```

For more detailed information about implementation, see [Create a Custom Login Module](../security/p_auth_configure_custom_login_module.md) and [Configure a Chain of Login Modules](../security/p_auth_configure_login_module.md).

#### login-module

**Required?** Required; **Occurs:** one or more

This element configures the login module, which communicates with a user database to validate user's credentials and to determine a set of authorized roles. See [Configure a Chain of Login Modules](../security/p_auth_configure_login_module.md). The `login-module` contains the elements described in the following table.

| Element | Description                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               |
|:--------|:------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| type    | The type of login module: `file`, `ldap`, `kerberos5`, `gss`, `jndi`, `keystore`, and `custom`. See the `login-module` examples that follow this table. Note: You must use the `kerberos5` and `gss` elements together, and in that sequence. For information about using these login-modules, see [Configure a Chain of Login Modules](../security/p_auth_configure_login_module.md).                                                                                                                                                                                                                                                    |
| success | The behavior of the login module at the time it validates the user's credentials. Possible values are: `required`, `requisite`, `sufficient`, and `optional`. The success status options are defined in the `javax.security.auth.login.Configuration` class. Authentication succeeds if all required and requisite login modules succeed, or if a sufficient or optional login module succeeds. The table in [Configure a Chain of Login Modules](../security/p_auth_configure_login_module.md) provides more information about how the order of login modules and the setting of the success element controls authentication processing. |
| options | The configuration options specific to the type of login module (see [options login-module](#options-login-module)).                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       |


##### options (`login-module`)

**Required?** Optional; **Occurs:** zero or one

This is the element for adding options to specific types of login modules. The options listed here are specific to public KAAZING Gateway login modules. See [Configure a Chain of Login Modules](../security/p_auth_configure_login_module.md) for more information about the types of login modules. 

`options` contains the following elements:

| Option        | Applies to login-module type | Description                                                                                                                                                                                                                                               |
|:--------------|:-----------------------------|:----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| debug         | `file`                       | If `true`, then the login module sends debug information (at the DEBUG level) to the logger. If `false` (the default), then the login module disables sending logging information to the logger. This is the default.                                     |
| tryFirstToken | `gss`                        | If `true`, then the login module looks in the JAAS shared state for a token using the key: `com.kaazing.gateway.server.auth.gss.token`. If `false` (the default), then the login module uses a CallbackHandler to discover the token from `LoginContext`. |

##### Example of a `file` Login Module

The following example shows a `file`-based `login-module` element that uses the flat XML file,` jaas-config.xml`:

``` xml
<login-module>
  <type>file</type>
  <success>required</success>
  <options>
    <!-- This <file> element configures the path to the XML file
         that contains the database of users, passwords, and roles. -->
    <file>jaas-config.xml</file>
  </options>
</login-module>
```

For information about the `file` login module options, see the table in the [options (login-module)](#options-login-module) section.

##### Example of a `ldap` login module

The following example shows an LDAP-based `login-module` element:

``` xml
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
```

For information about configuring the LDAP login-module options, see the [Class LDAPLoginModule documentation](http://docs.oracle.com/javase/7/docs/jre/api/security/jaas/spec/com/sun/security/auth/module/LdapLoginModule.html).

##### Example of `kerberos5` login module

The following example shows a `kerberos5`-based `login-module` element. You must use the `kerberos5` and [`gss`](#gss-login-module) elements together, and in that sequence. This login module is required when using the `Negotiate` [scheme](#authentication):

``` xml
<login-module>
  <type>kerberos5</type>
  <success>required</success>
  <options>
    <useKeyTab>true</useKeyTab>
    <keyTab>/etc/krb5.keytab</keyTab>
    <principal>HTTP/localhost@LOCAL.NETWORK</principal>
    <isInitiator>false</isInitiator>
    <doNotPrompt>true</doNotPrompt>
    <storeKey>true</storeKey>
  </options>
</login-module>
```

For information about configuring the Kerberos login module options, see the [Krb5LoginModule](http://docs.oracle.com/javase/7/docs/jre/api/security/jaas/spec/com/sun/security/auth/module/Krb5LoginModule.html "Krb5LoginModule (Java Authentication and Authorization Service )") documentation. For information about how to use KAAZING Gateway with Kerberos, see [Configure Kerberos V5 Network Authentication](../security/o_auth_configure.md).

##### `gss` login module

The following example shows a `gss`-based `login-module` element that you define after the Kerberos login module in the chain to enable the Kerberos tokens to travel over the Web. This login modules is required when using the `Negotiate` [scheme](#authentication):

``` xml
<login-module>
  <type>gss</type>
  <success>required</success>
</login-module>
```

For information about the `gss` login module options, see the table in the [options (login-module)](#options-login-module) section. The `gss` login-module element requires no options but must follow the [kerberos5](#example-of-kerberos5-login-module) login-module element, because the `gss` login-module element uses the credentials obtained by the [kerberos5](#example-of-kerberos5-login-module) login-module element to verify the service ticket presented by the client. See [Configure Kerberos V5 Network Authentication](../security/u_kerberos_configure.md) and [Using Kerberos V5 Network Authentication with the Gateway](../security/u_kerberos_configure.md) for more information.

##### Example of a `jndi` login module

The following example shows a jndi-based `login-module` element. It translates the examples for the login module in the [JndiLoginModule](http://docs.oracle.com/javase/7/docs/jre/api/security/jaas/spec/com/sun/security/auth/module/JndiLoginModule.html) javadoc into the XML you would use in the `security.realm` section of the Gateway configuration:

``` xml
<login-module>
  <type>jndi</type>
  <success>required</success>
  <options>
    <!-- These options come directly from the JndiLoginModule javadoc;
        these are the NIS examples -->
    <user.provider.url>nis://NISServerHostName/NISDomain/user</user.provider.url>
    <group.provider.url>nis://NISServerHostName/NISDomain/system/group</group.provider.url>
  </options>
</login-module>
```

For information about configuring the JNDI login-module options, see the [JndiLoginModule](http://docs.oracle.com/javase/7/docs/jre/api/security/jaas/spec/com/sun/security/auth/module/JndiLoginModule.html) documentation.

##### Example of a `keystore` login module

The following example shows a keystore-based `login-module` element. It translates the examples in the [KeyStoreLoginModule](http://docs.oracle.com/javase/7/docs/jre/api/security/jaas/spec/com/sun/security/auth/module/KeyStoreLoginModule.html) javadoc into the XML you would use in the `security.realm` section of the Gateway configuration:

``` xml
<login-module>
  <type>keystore</type>
  <success>required</success>
  <options>
   <!-- These options come directly from the KeyStoreLoginModule javadoc  -->
    <keyStoreURL>file://path/to/keystore.db</keyStoreURL>
    <keyStorePasswordURL>file://path/to/keystore.pw</keyStorePasswordURL>
    <keyStoreAlias>keystore-alias</keyStoreAlias>
  </options>
</login-module>
```

For information about configuring the keystore login-module options, see the [KeyStoreLoginModule](http://docs.oracle.com/javase/7/docs/jre/api/security/jaas/spec/com/sun/security/auth/module/KeyStoreLoginModule.html) documentation.

##### Example of a `custom` login module

KAAZING Gateway also supports a plugin mechanism for integration with custom authentication modules based on the Java LoginModule API.

For information about creating a custom login module using this API, see:

-   [Java Authentication and Authorization Service (JAAS) LoginModule Developer's Guide](http://docs.oracle.com/javase/7/docs/technotes/guides/security/jaas/JAASLMDevGuide.html).
-   [Create a Custom Login Module](../security/p_auth_configure_custom_login_module.md)
-   [Integrate an Existing Custom Login Module into the Gateway](../security/p_auth_integrate_custom_login_module.md)

##### Example of a Complete Security Element

The following is an example of a complete `security` element that includes a chain of two login modules:

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
    <description>KAAZING Gateway Demo</description>
    <authentication>
      <http-challenge-scheme>Basic<http-challenge-scheme>
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

Summary
-------

In this document, you learned about the Gateway `security` configuration element and how to specify it in your Gateway configuration file. For more information about the location of the configuration files and starting the Gateway, see [Setting Up the Gateway](../about/setup-guide.md). For more information about KAAZING Gateway administration, see the [documentation](../index.md).

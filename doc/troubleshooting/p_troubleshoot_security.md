Troubleshoot KAAZING Gateway Security
=================================================================

When you configure security for KAAZING Gateway, you may encounter one of the following errors. To resolve an issue, follow the guidance provided for the associated error.

Troubleshooting KAAZING Gateway
---------------------------------------------------------------------

This topic is part of [Troubleshoot the Gateway](o_troubleshoot.md) that groups troubleshooting topics into the categories shown in the following table:

| What Problem Are You Having? | Topic or Reference                                                             |
|------------------------------|--------------------------------------------------------------------------------|
| Configuration and startup    | [Troubleshoot KAAZING Gateway Configuration and Startup](p_troubleshoot_gateway_configuration.md) |
| Clusters and load balancing  | [Troubleshoot KAAZING Gateway Cluster and Load Balancing](p_troubleshoot_high_availability.md)    |
| **Security**                 | **Troubleshoot KAAZING Gateway Security**                                |
| Clients                      | [Troubleshoot Your Clients](p_dev_troubleshoot.md)                           |

What Problem Are You Having?
----------------------------

-   [Gateway Cannot Resolve Host Name/Address](#gateway-cannot-resolve-host-nameaddress)
-   [Error Using the auth-constraint Element](#error-using-the-auth-constraint-element)
-   [WebSocket Stays Connected Too Long When It Should Fail Due to an Authorization Timeout](#websocket-stays-connected-too-long-when-it-should-fail-due-to-an-authorization-timeout)
-   [Error: Host is Configured as a Secure Port in a Service](#error-host-is-configured-as-a-secure-port-in-a-service)
-   [No Import Certificate Prompt in Browser](#no-import-certificate-prompt-in-browser)
-   [Missing Certificate Entry in Keystore When Starting the Gateway](#missing-certificate-entry-in-keystore-when-starting-the-gateway)
-   [Keystore, Password, or Truststore Files Not Found](#keystore-password-or-truststore-files-not-found)
-   [Invalid Keystore Format](#invalid-keystore-format)
-   [Keystore Missing Certificate for Host Name](#keystore-missing-certificate-for-host-name)
-   [Errors When Importing Certificates](#errors-when-importing-certificates)

### Gateway Cannot Resolve Host Name/Address

**Cause:** If you use a host name in a secure URL (using HTTPS, WSS, SSL) in the Gateway configuration and the Gateway cannot resolve the host name, then it returns the following exception:

`java.lang.RuntimeException: Unable to resolve address *hostname*.com:9000`

**Solution:** Update the host name resources so the Gateway can resolve a host name in a secure URL using its cache, host file, or DNS.

### Error Using the auth-constraint Element

**Cause:** If the Gateway has detected the deprecated `auth-constraint` element in the configuration file then it returns the following error message:

The preferred name for the auth-constraint configuration option has changed to authorization-constraint for clarity. Please update your configuration file accordingly.

**Solution:** Change your Gateway configuration to use the `authorization-constraint` element as described in the [authorization-constraint](../admin-reference/r_configure_gateway_service.md#authorization-constraint) element for Gateway services.

### WebSocket Stays Connected Too Long When It Should Fail Due to an Authorization Timeout![This feature is available in KAAZING Gateway - Enterprise Edition](../images/enterprise-feature.png)

**Cause:** WebSocket stays connected when you expected it to fail due to an authorization timeout.

**Solution:** Perform the following steps:

1.  Specify the time interval before which the client must reauthenticate itself, lest the WebSocket be closed.

    There are two ways to specify the time interval before which the client must reauthenticate itself:

    -   Configure an [`authorization-timeout`](../admin-reference/r_configure_gateway_security.md#authentication) in your the Gateway configuration, as described in the `security` section of this document.
    -   Specify the maximum age with the `LoginResult.setAuthorizationTimeout` method in your custom login module, as described in the Service Provider Interface in the [Server API Documentation](../index.md).

    You can use one or both of the preceding methods to specify an authorization timeout. If you use both methods, then the Gateway sets the authorization timeout to be the lesser of the two values.

2.  Determine if reauthentication is active for a WebSocket connection:
    1.  Enable the `session.revalidate` logger at the `info` warning level, as shown in the following example:

        ``` xml
              <logger name="session.revalidate">
                  <level value="info"/>
              </logger>

              <root>
                  <priority value="info"/>
                  <appender-ref ref="STDOUT"/>
              </root>
        ```

    2.  Look for the following messages:

        The following message indicates the revalidate system for WebSocket is enabled.

        ``` bash
        2012-02-29 10:03:56,305 INFO   session.revalidate - 
                           [wsx#8] REVALIDATE COMMAND ENABLED [period=2s; timeout=2s]
        ```

        The following message indicates the Gateway sent a revalidate message to the client.

        ``` bash
        2012-02-29 10:03:58,309 INFO   session.revalidate - 
                           [wsx#8] REVALIDATE COMMAND SENT: VALI 
        /echoAuth/;a/f0tCswwNoLNfaKb4cOERYuTT56Ai6UAN?.kl=Y
        ```

### Error: Host is Configured as a Secure Port in a Service

**Cause:** When the same host and port is configured as secure in one service and not secure in another service the following error may occur:

    example.com IP:443 is configured as a secure port in a service already and cannot be bound as an unsecure port in service wss://example.com:443/echo

For example, the error could occur at runtime for the following configuration because there is more than one service in use and one or more of the services is behind an SSL offloader:

Service 1 (where `wss:example.com:443` is configured as secure):

``` xml
        <service>
            <accept>wss:example.com:443/service1</accept>
        ...
        </service>
```

Service 2 (where `wss:example.com:443` is configured as not secure by disabling `ssl.encryption`):

``` xml
      <service>
          <accept>wss:example.com:443/service2</accept>
          <accept-options>
              <ssl.encryption>disabled</ssl.encryption>
          </accept-options>
      </service>
```

**Solution:** Configure a service being offloaded as a separate service (that uses a different port number) and disable [ssl.encryption](../admin-reference/r_configure_gateway_service.md#sslencryption).

### No Import Certificate Prompt in Browser

**Cause:** When using a WebSocket Secure (`wss://`) connection, you navigate to your site that is using a self-signed certificate for testing purposes. When you access the URL, you are not prompted to import the self-signed certificate, as expected. The prompt to import the certificate displays when a web browser accesses an HTTPS address only.

**Solution:** Configure the Gateway with a `directory` service that accepts an HTTPS address (for example, `https://example.com:9000`), and navigate to that address using a web browser.

**See Also:** [Secure the Gateway Using Self-Signed Certificates](../security/p_tls_selfsigned.md) for information about how to import a self-signed certificate into your browser, and see the [directory](../admin-reference/r_configure_gateway_service.md#directory) service reference for information about configuring a directory service.

### Missing Certificate Entry in Keystore When Starting the Gateway

**Cause:** When the `directory` service in the Gateway is configured to access a secure URL for which it does not have a corresponding certificate in the `keystore.db` file (located in `GATEWAY_HOME/conf`), the following error may be returned when starting the Gateway:

`Keystore does not have a certificate entry for *host:port*.`

**Solution:** Ensure that you have imported the appropriate certificate into the keystore and that the `directory` service in the `gateway-config.xml` points to the same host and port as the certificate.

To display the certificates in the keystore, use the following command:

`keytool -list -v -keystore filename -storepass password -storetype JCEKS/JKS`

**See Also:** [Secure Network Traffic with the Gateway](../security/o_tls.md).

### Keystore, Password, or Truststore Files Not Found

**Cause:** If the Gateway cannot locate the keystore database file, the keystore password file, or the truststore file, then any of the following exceptions can occur.

-   If the Gateway cannot locate the keystore database file during start up, then the following exception is thrown:

    `java.io.FileNotFoundException: GATEWAY_HOME/conf/keystore.db (No such file or directory)`

-   If the Gateway cannot locate the keystore password file during start up, then the following exception is thrown:

    `java.io.FileNotFoundException: GATEWAY_HOME/conf/keystore.pw (No such file or directory)`

-   If the Gateway cannot locate the truststore file during start up, then the following exception is thrown:

    `java.io.FileNotFoundException: GATEWAY_HOME/conf/truststore.db (No such file or directory)`

**Solution:** For all exceptions, confirm that the Gateway configuration has the correct name and location for the keystore, password, and truststore files. The keystore database file is located in `GATEWAY_HOME/conf` and referenced in the `keystore` element in `gateway-config.xml`, for example:

``` xml
        <keystore>
            <type>JCEKS</type>
            <file>keystore.db</file>
            <password-file>keystore.pw</password-file>
        </keystore>
```

### Invalid Keystore Format

**Cause:** The `keystore` and `truststore` elements used in the Gateway configuration file must follow the correct format or the Gateway will throw the following exception:

`java.io.IOException: Invalid keystore format`

The error displays if you do not include the `type` parameter in the `keystore` element in `gateway-config.xml`. It might also appear if you include the incorrect `type` parameter.

**Solution:** Use the correct format for the `keystore` or `truststore` elements, as follows:

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

### Keystore Missing Certificate for Host Name

**Cause:** The Gateway configuration file must be able to locate a certificate for the host name used in a secure URL.

If there are any secure protocol schemes in `gateway-config.xml` (HTTPS, WSS, SSL, TLS), the Gateway looks in the keystore or truststore for a certificate that corresponds to the host name used in the URL. If the certificate does not exist, the Gateway throws the following exception:

`java.lang.RuntimeException: org.kaazing.gateway.server.transport.ssl.bridge.filter.CertificateNotFoundException: Keystore does not have a certificate entry for example.com`

Enter the following `keytool` command to see if there is a certificate for the host name in the keystore:

`keytool -list -v -alias example.com -keystore GATEWAY_HOME/conf/keystore.db -storepass password -storetype JCEKS`

You can omit the `-alias` parameter to see all certificates in the keystore or truststore.

If there is no certificate for the alias you entered, keytool responds as follows:

`keytool error: java.lang.Exception: Alias <alias.com> does not exist`

**Solution:** To solve this problem, create the certificate for the host name.

**Note:** When you generate a self-signed certificate, the `CN (Common Name)` and `alias` parameters must match or the Gateway will throw an exception stating that the certificate does not exist.

### Errors When Importing Certificates

**Cause:** Keytool reports errors if you omit required parameters when importing certificates.

`keytool error: java.io.IOException: Invalid keystore format`

This is the most common error when importing certificates into the keystore. The common cause of this error is the omission of the `-storetype JCEKS` parameter and value in the keytool import command.

**Solution:** Always include `-storetype JCEKS` when importing certificates into the keystore. This parameter is not required when importing certificates into the truststore.

**Note:** If you specify `JCEKS` (Java Cryptography Extension Key Store) when importing a certificate into the truststore, then you must add `<type>JCEKS</type>` or the Gateway throws an exception.

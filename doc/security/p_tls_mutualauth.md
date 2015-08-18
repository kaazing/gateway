Require Clients to Provide Certificates to the Gateway
===============================================================================

Typically, TLS/SSL is used to validate the Gateway's identity to the client and encrypt data. However you can also use TLS/SSL to have the Gateway validate a client's identity. When TLS/SSL is used to verify both the Gateway and clients, it is sometimes referred to as mutual verification.

**Note:** You instruct the Gateway to verify the KAAZING Gateway client's identity by including the `ssl.verify-client` element in the Gateway configuration. The Gateway will not establish a TLS/SSL connection (HTTPS or WSS) with the client until the identity of client has been verified.

With client-side certificates, after the KAAZING Gateway client has validated the certificate from the Gateway, the Gateway responds with a challenge to the client. This challenge includes all the public keys included in the [truststore](../admin-reference/r_configure_gateway_security.md#truststore) of the Gateway. The client then attempts to find a matching private key in its keystore (or keystore equivalent based on the client operating system or web browser) and respond with that key.

To prepare your clients for this client-side verification, you must create a new certificate for the client, put the public key of the certificate in the Gateway's truststore, put the private key in the client's keystore.

**Important:** Do not use the same certificate for both the client and Gateway. Doing so would put the connection at risk of TLS/SSL [replay attacks](http://en.wikipedia.org/wiki/Replay_attack "Replay attack - Wikipedia, the free encyclopedia").

This procedure describes how to implement a mutual verification pattern where, in addition to the Gateway presenting a certificate to the client, the client must present a certificate to the Gateway so that the Gateway can verify the client's identity.

Before You Begin
----------------

This procedure is part of [Secure Network Traffic with the Gateway](../security/o_tls.md), that includes the following steps:

-   [Secure the Gateway Using Trusted Certificates](p_tls_trusted.md)
-   [Secure the Gateway Using Self-Signed Certificates](p_tls_selfsigned.md)
-   [Secure Clients and Web Browsers with a Self-Signed Certificate](p_tls_clientapp.md)
-   **Require Clients to Provide Certificates to the Gateway**

To Configure Clients to Provide TLS/SSL Certificates
----------------------------------------------------

1.  On the KAAZING Gateway client, create a new **self-signed certificate**. The public key in this self-signed certificate will be imported into the truststore on the Gateway later. Each operating system has a different method for creating client certificates.
2.  Export the public key certificate from the client. If you are using the Java keytool utility, the command is (substitute your information for *name*, *GATEWAY\_HOME*, *client_certificate.cer*, and *client-keystore.jks*):

    `keytool -export -alias name -file client_certificate.cer -keystore client-keystore.jks`

3.  Import the public key into the Gateway's truststore using the Java keytool utility (substitute your information for *GATEWAY\_HOME*, *name*, *client_certificate.cer*, and *changeit*):

    `keytool -importcert -keystore GATEWAY\_HOME\conf\truststore.db -storepass changeit -trustcacerts -alias name -file client\_certificate.cer`

4.  Add the `ssl.verify-client` option in your Gateway configuration (`GATEWAY_HOME/conf/gateway-config.xml`).

    The following example shows a snippet of a simple Gateway configuration that accepts on a secure URI (`wss://`) and includes the `ssl.verify-client` option to require that all clients connecting to the Gateway on that URI provide a digital certificate verifying their identity.

    ``` xml
          <service>
            <accept>wss://example.com:443</accept>
            <connect>tcp://server1.corp.example.com:5050</connect>

            <type>proxy</type>

            <accept-options>
              <ssl.verify-client>required</ssl.verify-client>
            </accept-options>
          </service>

    ```

5.  Start the Gateway.
6.  Connect to the Gateway from the client using the HTTPS/WSS URI. The Gateway will provide the client with a certificate. Once the client has verified that certificate, the Gateway will request a client certificate from the client. Once the Gateway has verified the client certificate, the secure connection is established.

**Notes:**

-   To use `ssl.verify-client` as an `accept-option` on a service, the service must be accepting on a secure URI (`wss://`, `https://`, `ssl://`). You cannot use `ssl.verify-client` on a unsecured URI (`ws://`, `http://`, `tcp://`, `udp://`).
-   For more examples, including how to require clients to provide certificates in an Enterprise Shield™ topology, see [Configure Enterprise Shield™](../enterprise-shield/p_enterprise_shield_config.md) and the [ssl.verify-client](../admin-reference/r_configure_gateway_service.md#sslverify-client) accept-option.

Next Step
---------

To troubleshoot TLS/SSL errors and exceptions, see [Troubleshooting KAAZING Gateway Security](../troubleshooting/p_troubleshoot_security.md)[].

Notes
-----

-   This procedure is recommended for added security in an Enterprise Shield™ topology. See [About Enterprise Shield™](../enterprise-shield/o_enterprise_shield_checklist.md) for more topics and information.
-   Consider configuring the [socks.ssl.verify-client](../admin-reference/r_configure_gateway_service.md#sockssslverify-client) connect-option for end-to-end security in an Enterprise Shield™ topology.
-   A best practice is to use mutual verification between gateways that are located at different sites. Each gateway can require that the other gateway provide a certificate, thereby ensuring that the connection is secure.
-   To support DSA certificates, you must add `ADH` to the `ssl.ciphers` element as follows: `<ssl.ciphers>HIGH, MEDIUM, ADH</ssl.ciphers>`. Do not use `ADH` with `DEFAULT`. DSA certificates are not recommended. See [Diffie-Hellman key exchange](http://en.wikipedia.org/wiki/Diffie%E2%80%93Hellman_key_exchange#Security) for more information. If you omit the `-keyalg` switch when you create a certificate using keytool, keytool generates a DSA certificate. You should always include `-keyalg RSA` when creating a certificate using keytool.

See Also
--------

-   [Configure Enterprise Shield™ with the Gateway](../enterprise-shield/o_enterprise_shield_checklist.md)
-   [Service Reference](../admin-reference/r_configure_gateway_service.md)

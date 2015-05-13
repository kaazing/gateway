-   [Home](../../index.md)
-   [Documentation](../index.md)
-   Configure a Ticket Granting Gateway

Configure a Ticket Granting Gateway ![This feature is available in KAAZING Gateway - Enterprise Edition](../images/enterprise-feature.png)
=======================================================================================

In this procedure, you will learn how to configure KAAZING Gateway as a Ticket Granting Gateway to proxy Kerberos protocol traffic from clients to a KDC.

Before You Begin
----------------

This procedure is part of [Configure Kerberos V5 Network Authentication](o_krb.md):

1.  [Configuring Kerberos V5 Network Authentication Overview](o_krb_config_kerberos.md)
2.  [Configure a Ticket Protected Gateway](p_krb_config_tpg.md)
3.  **Configure a Ticket Granting Gateway**

To Configure a Ticket Granting Gateway
--------------------------------------

1.  Define a service (preferably using the WebSocket Secure scheme, `wss://`) that provides access to the Kerberos server at the authentication connection location as shown in the `accept` and `type` elements in following example. **Note:** The default port for Kerberos traffic is 88.

    ``` xml
    <service>
      <accept>wss://gateway.example.com:9002/kerberos5</accept>
      <connect>udp://kdc.example.com:88</connect>
      <type>kerberos5.proxy</type>
      <cross-site-constraint>
        <allow-origin>http://gateway.example.com:8000</allow-origin>
      </cross-site-constraint>
      <cross-site-constraint>
        <allow-origin>https://gateway.example.com:9000</allow-origin>
      </cross-site-constraint>
    </service>
    ```

2.  Restart the "ticket-granting" Gateway to let the configuration changes take effect.

This allows the client to access the TGG as required by `Application Negotiate` authentication scheme.

Next Steps
----------

After you configure the Gateway, ensure your clients are also configured for Kerberos. For information on creating KAAZING Gateway client Kerberos challenge handlers, see the [Howto](../index.md) documentation for developers.

See Also
------------------------------

-   [Configure the Gateway](../admin-reference/o_conf_checklist.md)
-   [About Kerberos V5 Network Authentication](c_aaa_kerberos.md)
-   [Using Kerberos V5 Network Authentication with the Gateway](u_krb_config_kerberos.md)

-   [Home](../../index.md)
-   [Documentation](../index.md)
-   Configure a Ticket Protected Gateway

Configure a Ticket Protected Gateway ![This feature is available in KAAZING Gateway - Enterprise Edition](../images/enterprise-feature.png)
====================================================================================================

In this procedure, you will learn how to configure KAAZING Gateway as a Ticket Protected Gateway to accept a Kerberos service ticket from a browser client.

Before You Begin
----------------

This procedure is part of [Configure Kerberos V5 Network Authentication](o_krb.md):

1.  [Configuring Kerberos V5 Network Authentication Overview](o_krb_config_kerberos.md)
2.  **Configure a Ticket Protected Gateway**
3.  [Configure a Ticket Granting Gateway](p_krb_config_tgg.md)

To Configure a Ticket Protected Gateway
---------------------------------------

1.  Ensure that your environment is configured for Kerberos and note down the required values for the Kerberos login-module.
2.  Configure the client browsers, which is typically done on the intranet (refer to the browser's documentation, such as Mozilla Firefox or Microsoft Internet Explorer, for help on configuration).
3.  In the Gateway configuration, create a service entry for `kerberos5.proxy`, which signals the Gateway to communicate with the Kerberos Key Distribution Center in your environment.
4.  Set the `http-challenge-scheme` element (in the `authentication` element in `security`) to use `Application Negotiate` or `Negotiate`, which allows the client (Application Negotiate) or the browser (Negotiate) to respond to SPNEGO challenges.
5.  Add a `kerberos5` login-module element. See the [Krb5LoginModule](http://docs.oracle.com/javase/7/docs/jre/api/security/jaas/spec/com/sun/security/auth/module/Krb5LoginModule.html "Krb5LoginModule (Java Authentication and Authorization Service )") documentation for information on configuring the options. Note that the `principal` option must point to the HTTP service that is being authenticated, and must always use the form: `HTTP/<serverName>@<kerberosDomainName>`. For example: `HTTP/www.example.com@ExampleKerberosDomain`.
6.  Add a `gss` login-module element after the `kerberos5` login-module element. This element requires no options but must follow the `kerberos5` login-module element, as the `gss` login-module element uses the credentials obtained by the `kerberos5` login-module element to verify the service ticket presented by the client.

    The following example shows the Application Negotiate `http-challenge-scheme` element, a `principal` element using the correct format, and a `gss` login-module:

    ``` xml
    <security>
        <realm>
            <name>demo</demo>
            <authentication>
                <http-challenge-scheme>Application Negotiate</http-challenge-scheme>
                <login-modules>
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

                    <login-module>
                        <type>gss</type>
                        <success>required</success>
                    </login-module>
                </login-modules>
            </authentication>
        </realm>
    </security>
    ```

Notes
-----

-   If you choose to use `Application Negotiate`, you must also configure your client to handle the SPNEGO-based Kerberos authentication mechanism. For more information, see [How does Kerberos SPNEGO GSS-API work?](c_aaa_kerberos.md#gssapimech).
-   After you configure the Gateway, ensure your clients are also configured for Kerberos. For information on creating KAAZING Gateway client Kerberos challenge handlers, see the [Howto](../index.md) documentation for developers.

Next Steps
----------

[Configure a Ticket Granting Gateway](p_krb_config_tgg.md)

See Also
------------------------------

-   [Configure the Gateway](../admin-reference/o_conf_checklist.md)
-   [About Kerberos V5 Network Authentication](c_aaa_kerberos.md)
-   [Using Kerberos V5 Network Authentication with the Gateway](u_krb_config_kerberos.md)

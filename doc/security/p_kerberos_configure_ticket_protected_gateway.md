Configure a Ticket Protected Gateway ![This feature is available in KAAZING Gateway - Enterprise Edition](../images/enterprise-feature.png)
====================================================================================================

In this procedure, you will learn how to configure KAAZING Gateway as a Ticket Protected Gateway to accept a Kerberos service ticket from a browser client.

Before You Begin
----------------

This procedure is part of [Configure Kerberos V5 Network Authentication](o_kerberos.md):

1.  [Configuring Kerberos V5 Network Authentication Overview](p_kerberos_configure.md)
2.  **Configure a Ticket Protected Gateway**

To Configure a Ticket Protected Gateway
---------------------------------------

1.  Ensure that your environment is configured for Kerberos and note down the required values for the Kerberos login-module.
2.  Configure the client browsers, which is typically done on the intranet (refer to the browser's documentation, such as Mozilla Firefox or Microsoft Internet Explorer, for help on configuration).
3.  In the Gateway configuration, create any `service` entry, such as `proxy`. Configure that service with a `realm-name` that contains the name of a security `realm` that we will configure below.
4.  In the `realm` that matches the `realm-name`, set the `http-challenge-scheme` element (in the `authentication` element in `security`) to use `Negotiate`, which allows the client or the browser to respond to SPNEGO challenges.
5.  Add a `kerberos5` login-module element. See the [Krb5LoginModule](http://docs.oracle.com/javase/7/docs/jre/api/security/jaas/spec/com/sun/security/auth/module/Krb5LoginModule.html "Krb5LoginModule (Java Authentication and Authorization Service )") documentation for information on configuring the options. Note that the `principal` option must point to the HTTP service that is being authenticated, and must always use the form: `HTTP/<serverName>@<kerberosDomainName>`. For example: `HTTP/www.example.com@ExampleKerberosDomain`.
6.  Add a `gss` login-module element after the `kerberos5` login-module element. This element requires no options but must follow the `kerberos5` login-module element, as the `gss` login-module element uses the credentials obtained by the `kerberos5` login-module element to verify the service ticket presented by the client.

    The following example shows the `Negotiate` `http-challenge-scheme` element, a `principal` element using the correct format, and a `gss` login-module:

    ``` xml
    <service>
        <accept>wss://localhost:9000/kerberos5</accept>
        <connect>tcp://kerberos.example.com:88</connect>
        <type>proxy</type>
        <realm-name>demo</realm-name>
        ...
    </service>
    ...
    <security>
        <realm>
            <name>demo</demo>
            <authentication>
                <http-challenge-scheme>Negotiate</http-challenge-scheme>
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

-   If you choose to use `Application Token`, you must also create a custom token or HTTP cookies for the Gateway to use to challenge the client, and a custom-written challenge handler and/or login handler that the client can use to generate the expected token or cookie value.
-   After you configure the Gateway, ensure your clients are also configured for Kerberos. For information on creating KAAZING Gateway client Kerberos challenge handlers, see the [For Developers](../index.md) documentation on the table of contents.

See Also
------------------------------

-   [Configure the Gateway](../admin-reference/o_configure_gateway_checklist.md)
-   [About Kerberos V5 Network Authentication](c_authentication_kerberos.md)
-   [Using Kerberos V5 Network Authentication with the Gateway](u_kerberos_configure.md)

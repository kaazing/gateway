Configuring Kerberos V5 Network Authentication Overview ![This feature is available in KAAZING Gateway - Enterprise Edition](../images/enterprise-feature.png)
============================================================================================================================

KAAZING Gateway can be configured to accept a Kerberos service ticket (Ticket-Protected Gateway or TPG) from a browser client using a standard `Negotiate` challenge. 

Before You Begin
----------------

This procedure is part of [Configure Kerberos V5 Network Authentication](o_kerberos.md):

1.  **Configuring Kerberos V5 Network Authentication Overview**
2.  [Configure a Ticket Protected Gateway](p_kerberos_configure_ticket_protected_gateway.md)

How Does the Negotiate Authentication Scheme Work?
--------------------------------------------------------------------

In this configuration, access to a Kerberos ticket protected service is achieved as shown in the following figure:

![Negotiate authentication challenge to a Kerberos ticket protected service](../images/f-authentication-web-challenge-web.jpg)

**Figure: Negotiate authentication challenge to a Kerberos ticket protected service**

1.  A client running in the browser tries to access a ticket-protected service through a Ticket Protected Gateway (TPG).
2.  TPG sends the `Negotiate` authentication challenge to the browser. `Negotiate` is a standard feature that programs such as the web browser can leverage to request an encrypted Kerberos ticket.
3.  The browser requests the ticket directly from the KDC.
4.  The KDC returns an encrypted ticket. This negotiation is done over TCP or UDP.
5.  The browser responds to the challenge from the TPG with the encrypted ticket.
6.  If the ticket is valid, the client is granted access the ticket-protected service through a TPG.


##### Ticket Protected Gateway (TPG)

``` xml
<security>
     <realm>
        <name>demo</demo>
        <authentication>
            <http-challenge-scheme>Negotiate</http-challenge-scheme>
          </authentication>
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
    </realm>
</security>
```

Next Steps
----------

[Configure a Ticket Protected Gateway](p_kerberos_configure_ticket_protected_gateway.md)

Notes
-------------------------

-   Previous versions of the Gateway supported the `Application Negotiate` challenge scheme. `Application Negotiate` has been deprecated. If you are migrating a Gateway configuration that uses `Application Negotiate`, change the scheme to `Negotiate`.
-   For information on creating KAAZING Gateway client Kerberos challenge handlers, see the [For Developers](../index.md) documentation on the table of contents.

See Also
------------------------------

-   [Configure the Gateway](../admin-reference/o_configure_gateway_checklist.md) for more configuration information and examples
-   [About Kerberos V5 Network Authentication](c_authentication_kerberos.md)
-   [Using Kerberos V5 Network Authentication with the Gateway](u_kerberos_configure.md)

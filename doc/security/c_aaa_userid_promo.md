-   [Home](../../index.md)
-   [Documentation](../index.md)
-   Security with KAAZING Gateway

About User Identity Promotion![This feature is available in KAAZING Gateway - Enterprise Edition](images/enterprise-feature.png)
================================================

Typically, anonymous connections are not used in an enterprise application. Instead, users must provide credentials to access the AMQP message broker. However, because AMQP is a protocol, the credentials are supplied by the client. That means the credentials are prone to being manipulated.

User identity promotion enables the Gateway to securely propagate (promote) the user identity associated with a WebSocket connection or session from the client to the AMQP message broker. When a client connects to the Gateway, the client must first properly authenticate at the WebSocket transport layer before any AMQP messages are exchanged. Once the user's identity and authentication are established, the Gateway can then promote that identity by injecting the user's AMQP credentials into the protocol.

Thus, the AMQP message broker receives the credentials from the Gateway, which is a trusted source, and the credentials cannot be manipulated by users. The following figure shows a high-level overview of how protocol injection works with the Gateway and your AMQP message broker.

![Promoting User Credentials into the AMQP Protocol](../images/f-amqp-user-creds-web.png)
**Figure: Promoting User Credentials into the AMQP Protocol**
  
The AMQP message broker can perform its own authentication using the promoted identity. Note that it is not necessary for the credentials injected into the AMQP protocol to exactly match the user's identity. You need to supply only what is required for the AMQP message broker. For example:

- The username for AMQP may not match your system's single sign-on (SSO) username.
  Consider the case where the user logs into your web application using a global SSO username of joe.smith but the AMQP message broker username is smithj. In this situation, the Gateway can inject the correct credentials based on the user's identity, even when the credentials do not match the identity exactly.
- The credentials may be consolidated for AMQP.
   For example, all users should connect to the AMQP message broker as “webuser" instead of with their individual identity. In this case, users connect and authenticate to the Gateway using their unique identity, and thereafter are connected to the AMQP message broker as “webuser." Because this activity happens behind the Gateway, users are unaware.

See Also
-------------------------------------------------------

 - [Promote User Identity into the AMQP Protocol](../p_aaa_userid_promo.md)
 - [Configure Authentication and Authorization](o_aaa_config_authentication.md)
 - [About Authentication and Authorization](c_aaa_aaa.html)
 - [What Happens During Authentication](u_aaa_gw_client_interactions.md)
 - [How Authentication and Authorization Work with the Gateway](u_aaa_implement.md)

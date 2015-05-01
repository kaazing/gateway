-   [Home](../../index.md)
-   [Documentation](../index.md)
-   [Configure Authentication and Authorization](../index.md#security)

Configure Authentication and Authorization
=============================================================================================

The following checklist provides the steps necessary to configure KAAZING Gateway to perform authentication and authorization:

| #        | Step                                                                                                                  | Topic or Reference                                                                                                                                                                       |
|----------|-----------------------------------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 1        | Learn about authentication and authorization.                                                                         | [About Security with KAAZING Gateway](c_sec_security.md), [About Authentication and Authorization](c_aaa_aaa.md), and [What's Involved in Secure Communication](u_sec_client_gw_comm.md) |
| 2        | Learn how authentication and authorization work with the Gateway.                                                     | [What Happens During Authentication](u_aaa_gw_client_interactions.md) and [How Authentication and Authorization Work with the Gateway](u_aaa_implement.md)                               |
| 3        | Define the method the Gateway uses to secure back-end systems and respond to security challenges.                     | [Configure the HTTP Challenge Scheme](p_aaa_config_authscheme.md)                                                                                                                        |
| 4        | Configure one or more login modules to handle the challenge/response authentication sequence of events with clients.  | [Configure a Chain of Login Modules](p_aaa_config_lm.md)                                                                                                                                 |
| 5        | Code your client to respond to the Gateway's authentication challenge.                                                | [Configure a Challenge Handler on the Client](p_aaa_config_ch.md)                                                                                                                        |
| 6        | Configure the Gateway to specify the user roles that are authorized to perform operations for Gateway services.       | [Configure Authorization](p_aaa_config_authorization.md)                                                                                                                                 |
| 7        | Configure the Gateway to authorize or deny JMS operations performed by the client, using the JMSAuthorizationFactory. | [Secure the Connection from Each Client to the Gateway](p_client_jms_secure.md)                                                                                                          |
| Optional | Inject bytes into a custom protocol or promote user credentials to the AMQP protocol.                                 |       [Implement Protocol Injection](o_aaa_inject.md) and [Implement User Identity Promotion](p_aaa_inject.md)                                                                                                                                                                                   |


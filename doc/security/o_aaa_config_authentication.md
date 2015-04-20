-   [Home](../../index.md)
-   [Documentation](../index.md)
-   [Configure Authentication and Authorization](../index.md#security)

<a name="authentication"></a>Configure Authentication and Authorization
=============================================================================================

The following checklist provides the steps necessary to configure KAAZING Gateway to perform authentication and authorization:

<table class="checklist">
<tr>
<th scope="col">
\#
</th>
<th scope="col">
Step
</th>
<th scope="col">
Topic or Reference
</th>
</tr>
<tr>
<td>
1
</td>
<td>
Learn about authentication and authorization.
</td>
<td>
[About Security with KAAZING Gateway](c_sec_security.md), [About Authentication and Authorization](c_aaa_aaa.md), and [What's Involved in Secure Communication](u_sec_client_gw_comm.md)
</td>
</tr>
<tr>
<td>
2
</td>
<td>
Learn how authentication and authorization work with the Gateway.
</td>
<td>
[What Happens During Authentication](u_aaa_gw_client_interactions.md) and [How Authentication and Authorization Work with the Gateway](u_aaa_implement.md)
</td>
</tr>
<tr>
<td>
3
</td>
<td>
Define the method the Gateway uses to secure back-end systems and respond to security challenges.
</td>
<td>
[Configure the HTTP Challenge Scheme](p_aaa_config_authscheme.md)
</td>
</tr>
<tr>
<td>
4
</td>
<td>
Configure one or more login modules to handle the challenge/response authentication sequence of events with clients.
</td>
<td>
[Configure a Chain of Login Modules](p_aaa_config_lm.md)
</td>
</tr>
<tr>
<td>
5
</td>
<td>
Code your client to respond to the Gateway's authentication challenge.
</td>
<td>
[Configure a Challenge Handler on the Client](p_aaa_config_ch.md)
</td>
</tr>
<tr>
<td>
6
</td>
<td>
Configure the Gateway to specify the user roles that are authorized to perform operations for Gateway services.
</td>
<td>
[Configure Authorization](p_aaa_config_authorization.md)
</td>
</tr>

<tr>
<td>
7
</td>
<td>
Configure the Gateway to authorize or deny JMS operations performed by the client, using the JMSAuthorizationFactory.
</td>
<td>
[Secure the Connection from Each Client to the Gateway](p_client_jms_secure.md)
</td>
</tr>

<tr>
<td>
Optional
</td>
<td>
Inject bytes into a custom protocol or promote user credentials to the AMQP protocol.
</td>
<td>
[Implement Protocol Injection](o_aaa_inject.md) ![This feature is available in KAAZING Gateway - Enterprise Edition](images/enterprise-feature.png)
 [Implement User Identity Promotion](o_aaa_userid_promo.md) ![This feature is available in KAAZING Gateway - Enterprise Edition](images/enterprise-feature.png)
</td>
</table>



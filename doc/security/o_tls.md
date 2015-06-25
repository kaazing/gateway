Secure Network Traffic with the Gateway
==========================================

This checklist provides the steps necessary to secure network traffic with KAAZING Gateway using TLS/SSL:

| Step                                                                                                                 | Topic or Reference                                                                                               |
|----------------------------------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------|
| Learn about Transport Layer Security (TLS/SSL), and about how TLS/SSL works with the Gateway.                        | [Transport Layer Security (TLS/SSL) Concepts](c_tls.md), [How TLS/SSL Works with the Gateway](u_tls_works.md) |
| (Recommended) Use trusted certificates issued by a Certificate Authority to secure network traffic with the Gateway. | [Secure the Gateway Using Trusted Certificates](p_tls_trusted.md)                                                |
| (Optional) Use self-signed certificates to secure network traffic with the Gateway.                                  | [Secure the Gateway Using Self-Signed Certificates](p_tls_selfsigned.md)                                         |
| (Optional) Use self-signed certificates to secure network traffic between clients, web browsers and the Gateway.     | [Secure Clients and Web Browsers with a Self-Signed Certificate](p_tls_clientapp.md)                             |
| (Optional) Use certificates to validate the client's identity to the Gateway.                                        | [Require Clients to Provide Certificates to the Gateway](p_tls_mutualauth.md)                                    |

**Warning:** Using self-signed certificates can result in unpredictable behavior because various browsers, plug-ins, operating systems, and related run-time systems handle self-signed certificates differently. Resulting issues may include connectivity failures and other security issues which can be difficult to diagnose. Instead, use [trusted certificates](p_tls_trusted.md) issued from a trusted certificate authority (CA) for real-world development, test, and production environments.

**Note:** You are not required to configure TLS/SSL for both the client and back-end server connections to the Gateway. For example, you can choose to configure the client-to-gateway connection over WSS and leave the Gateway to back-end server connection using TCP.

-   [Home](../../index.md)
-   [Documentation](../index.md)
-   Secure Network Traffic with the Gateway

Secure Network Traffic with the Gateway
==========================================

This checklist provides the steps necessary to secure network traffic with KAAZING Gateway using TLS/SSL:

<table>
<colgroup>
<col width="50%" />
<col width="50%" />
</colgroup>
<thead>
<tr class="header">
<th align="left">Step</th>
<th align="left">Topic or Reference</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td align="left">Learn about Transport Layer Security (TLS/SSL), and about how TLS/SSL works with the Gateway.</td>
<td align="left"><ul>
<li><a href="c_tls.md">Transport Layer Security (TLS/SSL) Concepts</a></li>
<li><a href="u_tls_works.md">How TLS/SSL Works with the Gateway</a></li>
</ul></td>
</tr>
<tr class="even">
<td align="left"><strong>(Recommended)</strong> Use trusted certificates issued by a Certificate Authority to secure network traffic with the Gateway.</td>
<td align="left"><a href="p_tls_trusted.md">Secure the Gateway Using Trusted Certificates</a></td>
</tr>
<tr class="odd">
<td align="left">(Optional) Use self-signed certificates to secure network traffic with the Gateway.</td>
<td align="left"><a href="p_tls_selfsigned.md">Secure the Gateway Using Self-Signed Certificates</a></td>
</tr>
<tr class="even">
<td align="left">(Optional) Use self-signed certificates to secure network traffic between clients, web browsers and the Gateway.</td>
<td align="left"><a href="p_tls_clientapp.md">Secure Clients and Web Browsers with a Self-Signed Certificate</a></td>
</tr>
<tr class="odd">
<td align="left">(Optional) Use certificates to validate the client's identity to the Gateway.</td>
<td align="left"><a href="p_tls_mutualauth.md">Require Clients to Provide Certificates to the Gateway</a></td>
</tr>
</tbody>
</table>

<span class="alert">**Warning:** Using self-signed certificates can result in unpredictable behavior because various browsers, plug-ins, operating systems, and related run-time systems handle self-signed certificates differently. Resulting issues may include connectivity failures and other security issues which can be difficult to diagnose. Instead, use [trusted certificates](p_tls_trusted.md) issued from a trusted certificate authority (CA) for real-world development, test, and production environments.</span>

**Note:** You are not required to configure TLS/SSL for both the client and back-end server connections to the Gateway. For example, you can choose to configure the client-to-gateway connection over WSS and leave the Gateway to back-end server connection using TCP.



-   [Home](../../index.md)
-   [Documentation](../index.md)
-   Common ${gateway.name.long} Production Topologies

Common ${gateway.name.long} Production Topologies ${enterprise.logo.jms}
========================================================================

This topic describes some of the most common production topologies for the ${gateway.name.long}. It is intended to familiarize network administrators and systems software developers with general ${gateway.name.long} production deployments. Your actual production deployment of ${the.gateway} will likely be some variation of the topologies covered here.

All of the topologies in this topic begin with clients on one end and the ${message.broker.generic} at the opposite end of the connection. It is the number and configuration of ${gateway.cap} instances between the clients and the ${message.broker.generic} that distinguishes each ${gateway.cap} topology.

Developers that want to develop against ${the.gateway} can run ${the.gateway} locally as described in ${setting.up.inline}.

**Note:** Most topologies use ${gateway.cap} clustering and load balancing. For information on using these features, see [Configure ${the.gateway} for High Availability](../high-availability/o_ha.md).

This topic covers the following information:

-   [DMZ to Trusted Network Topology](#DMZ_to_Internal)
    -   [Deploying the DMZ to Trusted Network Topology](#Deploy_DMZ_to_Internal)
-   [Enterprise Shield™](#Enterprise_Shield)
-   [Virtual Private Connection](#VPC)
-   [TLS/SSL Security Considerations](#TLS)

<a name="DMZ_to_Internal"></a>DMZ to Trusted Network Topology
-------------------------------------------------------------

In this topology, client to ${message.broker.generic} connections are protected by two layers of network security, a firewall-protected DMZ and a firewall-protected Trusted Network. A ${gateway.cap} or ${gateway.cap} cluster is deployed in the firewall-protected DMZ peripheral network to service requests from ${gateway.name.long} clients on the Web. The DMZ ${gateway.cap} connects through a firewall to a second ${gateway.cap} deployed in the internal, Trusted Network. The Internal ${gateway.cap} connects to the ${message.broker.generic}.

Here is an example of the topology.

<figure style="margin-left:0px;">
![](../images/f-dmz-trusted-top.png)
<figcaption>

**Figure: DMZ to Trusted Network Topology**

</figcaption>
</figure>
### <a name="Deploy_DMZ_to_Internal"></a>Deploying the DMZ to Trusted Network Topology

Deploying this topology involves the following:

1.  Configure the Internal ${gateway.cap} to connect with the ${message.broker.generic} using an internal URI. For more information, see the `service` element. The internal URI might be something such as `tcp://broker.internal.com:8080`.
2.  Configure the DMZ ${gateway.cap} to proxy client requests to the Internal ${gateway.cap} using a private, internal URI. The DMZ ${gateway.cap} to Internal ${gateway.cap} is a full duplex, bidirectional communication over WebSocket, TCP, UDP, or SSL.
3.  Configure the DMZ ${gateway.cap} to listen on a public URI for clients, such as `wss://example.com:443/path`.
4.  Configure ${gateway.name.long} clients to connect to the DMZ ${gateway.cap} using the public URI. Once the clients are connected to the DMZ ${gateway.cap}, full duplex, bidirectional communication is established and communication flows in both directions simultaneously.

This topology is typically deployed using ${gateway.cap} clustering and load balancing. For more information, see [Configure ${the.gateway} for High Availability](../high-availability/o_ha.md). Load balancing can also be performed by a third party load balancer.

<a name="Enterprise_Shield"></a>Enterprise Shield™ ${enterprise.logo}
---------------------------------------------------------------------

Enterprise Shield™ protects your enterprise network by using reverse connectivity (a reverse proxy connection) to initiate the connection from ${the.gateway} in the internal, Trusted Network towards a DMZ ${gateway.cap}. The Enterprise Shield™ topology is documented in depth in [About ${enterprise.shield}](../reverse-connectivity/o_rc_checklist.md#whatis).

<a name="VPC"></a>Virtual Private Connection
--------------------------------------------

In this topology, the VPC solution (sometimes called *NoVPN*) transparently uses the Web Tier for all of its secure communication. This topology provides a VPN-style solution without the need for specialist VPN software.

${gateway.name.long} can be configured to allow TCP (and UDP) clients to connect to servers over the Web without the need for any special WebSocket libraries, thus creating a virtual private connection. ${gateway.name.long} is designed not only to proxy TCP protocols using the WebSocket protocol, but to run in a WebSocket protocol to TCP mode. This design allows system and network administrators to configure two or more ${gateway.cap} instances to enable applications to traverse the Web securely through firewalls and proxy servers. The VPC topology delivers sophisticated server-to-server systems and rich client applications over a LAN or WAN web infrastructure in the same manner as conventional distributed applications, all without the expense or complexity of a private line.

<a name="TLS"></a>TLS/SSL Security Considerations
-------------------------------------------------

${gateway.name.long} topologies use TLS/SSL to ensure that network intermediaries, such as transparent proxy servers and firewalls that are unaware of WebSocket, do not drop the WebSocket connection. WebSocket uses the same HTTP upgrade method commonly used to upgrade HTTP connections to HTTPS. Intermediaries unfamiliar with WebSocket might drop the unfamiliar WebSocket upgrade as a security precaution, preventing the WebSocket connection. When using the WSS connection over TLS/SSL, however, intermediaries trust the WSS connection and allow it to pass.

When using TLS/SSL with ${gateway.cap} topologies, ${the.gateway} must provide ${gateway.name.long} clients (built using ${gateway.name.long} libraries) with the certificate for the domain name in the URI that the clients use to connect to ${the.gateway}. If TLS/SSL is deployed between Gateway instances, then certificates must be used for the hostnames in the URIs used by the connected Gateway instances.

For more information, see [TLS/SSL with ${the.gateway} Example](../security/u_tls_works.md#tls_how_works) and [Secure Network Traffic with ${the.gateway}](../security/o_tls.md).



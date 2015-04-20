-   [Home](../../index.md)
-   [Documentation](../index.md)
-   Administration with KAAZING Gateway

Cluster Reference 
========================================

This document describes the elements and properties you can configure in the KAAZING Gateway to achieve high availability.

<a name="configuring"></a>Overview
----------------------------------

You can use the optional `cluster` element to configure a Gateway to participate in a KAAZING Gateway cluster. Once a cluster service is configured on each Gateway cluster member and the members are started, the clustered instances will handle connection-switching when another cluster member unexpectedly terminates. Cluster members share information about activity and which of their services are load balanced. See [Overview of Gateway Clustering](../high-availability/u_ha.md#cluster_overview) for more information about high availability and clustering.

<a name="descelements"></a>Structure
------------------------------------

The Gateway configuration file (`gateway-config.xml` or `gateway-config.xml`) defines the following configuration elements contained in the top-level `gateway-config` element:

-   [gateway-config](r_conf_gwconfig.md)
    -   [cluster](#cluster)
        -   [name](#clstrnameprop)
        -   [accept](#clstracceptopt)
        -   [connect](#clstrconnectopt)

<span id="network2"></span></a>cluster
--------------------------------------

**Required?** Optional; **Occurs:** zero or one

This is the element for cluster configuration. KAAZING Gateway can be clustered to achieve high availability. You can configure a cluster by specifying the local IP address of the Gateway in the `accept` element, and the unicast addresses or multicast group address in the `connect` element. If the `cluster` element is missing, the server cannot participate as a cluster member.

`cluster` contains the following elements:

<table width="95%" border="1">
<tr>
<th width="27%" scope="col">
Element
</th>
<th width="73%" scope="col">
Description
</th>
</tr>
<tr>
<td class="code_inline">
<a name="clstrnameprop"></a>name
</td>
<td>
The name of the cluster. The `name` element can be any name. The name must be the same in the `cluster` service of every Gateway that wishes to join this cluster.
</td>
</tr>
<tr>
<td class="code_inline">
<a name="clstracceptopt"></a>accept
</td>
<td>
The `accept` element contains the URI on which the cluster member listens for other cluster members. The URI contains the local IP address of the member. If the Gateway cannot match the IP address in the `accept` URI to one of the host's network interfaces, then the cluster startup process will fail.
 The URI in the `accept` uses a TCP scheme followed by the IP address of the network interface for the connection, and a port number (the port number in the example below is arbitrary).
</td>
</tr>
<tr>
<td height="144" class="code_inline">
<a name="clstrconnectopt"></a>connect
</td>
<td>
The `connect` element contains the URI used to discover other cluster members. The `cluster` element can contain a single `connect` that uses a multicast address (such as `udp://224.2.2.44:54327`) or multiple `connect` elements that use unicast addresses (such as `tcp://192.168.4.50:5942`).
-   For multicast addressing, the `connect` element URI is known as the *group address*. This URI must be the same in the `cluster` service of every Gateway that wishes to join this cluster. The `connect` element URI uses a multicast address and port number (the port number in this example is a common multicast port number). Each cluster member sends multicast datagrams to the multicast address.
-   For unicast addressing, there can be many `connect` elements, each pointing to a different cluster member. The `cluster` service does not require a `connect` URI for every other cluster member. The minimum requirement is that a `cluster` service have a `connect` for one other cluster member in order to join the cluster.

</td>
</tr>
</tr>
</tr>
</table>
### Example

The following example shows a `cluster` service that accepts on `tcp://192.168.2.10:5942` (its local IP and arbitrary port number) and connects to the cluster using `udp://224.2.2.44:54327`.

``` auto-links:
  <cluster>
    <name>MyCluster</name>
    <accept>tcp://192.168.2.10:5942</accept>
    <connect>udp://224.2.2.44:54327</connect>
  </cluster>
```

### Notes

-   To configure clustering in an Enterprise Shield&trade; topology, see [Configure Enterprise Shield&trade; in a Cluster](../reverse-connectivity/p_rc_cluster.md).

Summary
-------

In this document, you learned about the Gateway `cluster` configuration element and how to specify it in your Gateway configuration file. For more information about the location of the configuration files and starting the Gateway, see [Setting Up KAAZING Gateway](../about/setup-guide.md). For more information about KAAZING Gateway administration, see the [documentation](../index.md).

</div>


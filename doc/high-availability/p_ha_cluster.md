-   [Home](../../index.md)
-   [Documentation](../index.md)
-   Configure a ${gateway.name.long} Cluster

<a name="ha_config"></a>Configure a ${gateway.name.long} Cluster ${enterprise.logo.jms}
=======================================================================================

To create a cluster, you must configure multiple gateways to communicate with each other. This is accomplished by adding a `cluster` service to each ${gateway.cap}. Once a `cluster` service is configured on each ${gateway.cap} cluster member and the members are started, the clustered instances will handle connection-switching when another cluster member unexpectedly terminates. Cluster members share information about activity and which of their services are load balanced.

In the following procedure, two ${gateway.cap} members are clustered. Additional members can be added or removed using the same steps.

Before You Begin
----------------

This procedure is part of [Configure ${the.gateway} for High Availability](o_ha.md):

1.  [Using ${the.gateway} to Support High Availability](u_ha.md)
2.  **Configure a ${gateway.name.long} Cluster**
3.  [Set Up ${gateway.name.long} as a Load Balancer](p_ha_loadbalance.md)
4.  [Troubleshoot ${gateway.name.short} Clusters and Load Balancing](../troubleshooting/ts_ha.md)

<span id="cluster_config"></span></a>To Create a Cluster with ${the.gateway}
----------------------------------------------------------------------------

In this example, there are two ${gateway.cap}s. Each ${gateway.cap} is listening on its IP address for incoming connections (`192.168.2.10` for the first ${gateway.cap} and `192.168.2.11` for the second ${gateway.cap}), and both are connecting to the cluster group address `udp://224.2.2.44:54327`.

1.  On the first ${gateway.cap} (`192.168.2.10`), add the following `cluster` configuration element in ${the.gateway} configuration file (`GATEWAY_HOME/conf/gateway-config.xml`).

    </p>
    ``` auto-links:
          <cluster>
            <name>MyCluster</name>
            <accept>tcp://192.168.2.10:5942</accept>
            <connect>udp://224.2.2.44:54327</connect>
          </cluster>
    ```

    The `cluster` element requires `name`, `accept`, and `connect` elements:

    -   The `name` element can be any name. The name must be the same in the `cluster` service of every ${gateway.cap} that wishes to join this cluster.
    -   The `accept` element contains the URI on which the cluster member listens for other cluster members. The URI contains the local IP address of the member. If ${the.gateway} cannot match the IP address in the `accept` URI to one of the host's network interfaces, then the cluster startup process will fail.
         The URI in the `accept` uses a TCP scheme followed by the IP address of the network interface for the connection, and a port number (the port number in this example is arbitrary).
    -   The `connect` element contains the URI used to discover other cluster members. The `cluster` element can contain a single `connect` that uses a multicast address (such as `udp://224.2.2.44:54327`) or multiple `connect` elements that use unicast addresses (such as `tcp://192.168.4.50:5942`).
        -   For multicast addressing, the `connect` element URI is known as the *group address*. This URI must be the same in the `cluster` service of every ${gateway.cap} that wishes to join this cluster. The `connect` element URI uses a multicast address and port number (the port number in this example is a common multicast port number). Each cluster member sends multicast datagrams to the multicast address. The `connect` element may use a hostname instead of a multicast address, such as `udp://cluster.example.com:9999`, if the hostname resolves in DNS to the IP addresses of all of the cluster members.
        -   For unicast addressing, there can be many `connect` elements, each pointing to a different cluster member. The `cluster` service does not require a `connect` URI for every other cluster member. The minimum requirement is that a `cluster` service have a `connect` for one other cluster member in order to join the cluster.

2.  On the second ${gateway.cap} (`192.168.2.11`), add the following `cluster` service element (note that its `name` and `connect` elements are identical to those in the `cluster` service on the first ${gateway.cap}).

    ``` auto-links:
              <cluster>
                <name>MyCluster</name>
                <accept>tcp://192.168.2.11:5942</accept>
                <connect>udp://224.2.2.44:54327</connect>
              </cluster>
    ```

    Notice that this `accept` element in the second ${gateway.cap} uses the same port number as the `accept` element in the first ${gateway.cap}. These are different hosts and can therefore use the same port number. This also simplifies administration.

3.  Start both cluster members.
4.  Monitor the cluster using the steps in [Monitor with ${console.name}](../management/p_monitor_cc.md). To monitor a cluster, the default URL is: `http://localhost:8080/commandcenter`.
5.  When you shut down one of the instances, you should see the following message in the terminal that was used to start the remaining gateway instance:

    INFO Cluster member /192.168.2.10:5942 is down

Next Step
---------

[Set Up ${gateway.name.long} as a Load Balancer](p_ha_loadbalance.md)

Notes
-----

-   To set up a cluster locally for testing purposes, see [Configure a Two-Member Local Demo Cluster](../high-availability/u_ha.md#demo).
-   To configure clustering in an ${enterprise.shield} topology, see [Configure ${enterprise.shield} in a Cluster](../reverse-connectivity/p_rc_cluster.md).

<a name="summary"></a>Summary
-----------------------------

In this document, you learned about configuring ${the.gateway} for clustering. For more information about ${gateway.cap} administration, see the [documentation](../index.md).



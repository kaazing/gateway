Configure a KAAZING Gateway Cluster
=======================================================================================

To create a cluster, you must configure multiple gateways to communicate with each other. This is accomplished by adding a `cluster` service to each Gateway. Once a `cluster` service is configured on each Gateway cluster member and the members are started, the clustered instances will handle connection-switching when another cluster member unexpectedly terminates. Cluster members share information about activity and which of their services are load balanced.

In the following procedure, two Gateway members are clustered. Additional members can be added or removed using the same steps.

Before You Begin
----------------

This procedure is part of [Configure the Gateway for High Availability](o_high_availability.md):

1.  [Using the Gateway to Support High Availability](u_high_availability.md)
2.  **Configure a KAAZING Gateway Cluster**
3.  [Set Up KAAZING Gateway as a Load Balancer](p_high_availability_loadbalance.md)
4.  [Troubleshoot KAAZING Gateway Clusters and Load Balancing](../troubleshooting/p_troubleshoot_high_availability.md)

To Create a Cluster with the Gateway
----------------------------------------------------------------------------

In this example, there are two Gateways. Each Gateway is listening on its IP address for incoming connections (`192.168.2.10` for the first Gateway and `192.168.2.11` for the second Gateway), and both are connecting to the cluster group address `udp://224.2.2.44:54327`.

1.  On the first Gateway (`192.168.2.10`), add the following `cluster` configuration element in the Gateway configuration file (`GATEWAY_HOME/conf/gateway-config.xml`).

    ``` xml
          <cluster>
            <name>MyCluster</name>
            <accept>tcp://192.168.2.10:5942</accept>
            <connect>udp://224.2.2.44:54327</connect>
          </cluster>
    ```

    The `cluster` element requires `name`, `accept`, and `connect` elements:

    -   The `name` element can be any name. The name must be the same in the `cluster` service of every Gateway that wishes to join this cluster.
    -   The `accept` element contains the URI on which the cluster member listens for other cluster members. The URI contains the local IP address of the member. If the Gateway cannot match the IP address in the `accept` URI to one of the host's network interfaces, then the cluster startup process will fail.
         The URI in the `accept` uses a TCP scheme followed by the IP address of the network interface for the connection, and a port number (the port number in this example is arbitrary).
    -   The `connect` element contains the URI used to discover other cluster members. The `cluster` element can contain a single `connect` that uses a multicast address (such as `udp://224.2.2.44:54327`) or multiple `connect` elements that use unicast addresses (such as `tcp://192.168.4.50:5942`).
        -   For multicast addressing, the `connect` element URI is known as the *group address*. This URI must be the same in the `cluster` service of every Gateway that wishes to join this cluster. The `connect` element URI uses a multicast address and port number (the port number in this example is a common multicast port number). Each cluster member sends multicast datagrams to the multicast address. The `connect` element may use a hostname instead of a multicast address, such as `udp://cluster.example.com:9999`, if the hostname resolves in DNS to the IP addresses of all of the cluster members.
        -   For unicast addressing, there can be many `connect` elements, each pointing to a different cluster member. The `cluster` service does not require a `connect` URI for every other cluster member. The minimum requirement is that a `cluster` service have a `connect` for one other cluster member in order to join the cluster.

2.  On the second Gateway (`192.168.2.11`), add the following `cluster` service element (note that its `name` and `connect` elements are identical to those in the `cluster` service on the first Gateway).

    ``` xml
              <cluster>
                <name>MyCluster</name>
                <accept>tcp://192.168.2.11:5942</accept>
                <connect>udp://224.2.2.44:54327</connect>
              </cluster>
    ```

    Notice that this `accept` element in the second Gateway uses the same port number as the `accept` element in the first Gateway. These are different hosts and can therefore use the same port number. This also simplifies administration.

3.  Start both cluster members.
4.  Monitor the cluster using the steps in [Monitor with Command Center](../management/p_monitor_cc.md). To monitor a cluster, the default URL is: `http://localhost:8080/commandcenter`.
5.  When you shut down one of the instances, you should see the following message in the terminal that was used to start the remaining gateway instance:

    ```
    INFO Cluster member /192.168.2.10:5942 is down
    ```

Next Step
---------

[Set Up KAAZING Gateway as a Load Balancer](p_high_availability_loadbalance.md)

Notes
-----

-   To set up a cluster locally for testing purposes, see [Configure a Two-Member Local Demo Cluster](../high-availability/u_high_availability.md#configure-a-two-member-local-demo-cluster).
-   To configure clustering in an Enterprise Shield™ topology, see [Configure Enterprise Shield™ in a Cluster](../enterprise-shield/p_enterprise_shield_cluster.md).

Summary
-----------------------------

In this document, you learned about configuring the Gateway for clustering. For more information about Gateway administration, see the [documentation](../index.md).

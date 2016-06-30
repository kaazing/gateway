Set Up KAAZING Gateway as a Load Balancer
=============================================================================================

Load balancing is used to balance client connection loads for a group of Gateways that have been configured as a cluster using the `cluster` service. For more information, see [Configure a KAAZING Gateway Cluster](p_high_availability_cluster.md). You can configure load balancing using a third-party load balancer, but this procedure describes how to use the built-in load balancing features of the Gateway.

To configure load balancing, you configure each Gateway in the cluster with a `balancer` service. The `balancer` service has `accept` elements for each URI that will be balanced across the cluster. Each cluster member configured with a `balancer` service acts as a load balancer to the other members.

Next, in the `service` elements on each Gateway, you add `balance` child elements containing the same URIs you configured in the `balancer` service.

Before You Begin
----------------

This procedure is part of [Configure the Gateway for High Availability](o_high_availability.md):

1.  [Using the Gateway to Support High Availability](u_high_availability.md)
2.  [Configure a KAAZING Gateway Cluster](p_high_availability_cluster.md)
3.  **Set Up KAAZING Gateway as a Load Balancer**
4.  [Troubleshoot KAAZING Gateway Clusters and Load Balancing](../troubleshooting/p_troubleshoot_high_availability.md)

To Set Up The Gateway as a Load Balancer
--------------------------------------------------------------------------------

1.  Configure a `balancer` service in each Gateway that will participate in load balancing (each cluster member). The `balancer` service has an `accept` for each URI that will be balanced across participating Gateways. These are the public URIs that clients will use when connecting to the cluster for the first time.

    ``` xml
    <service>
      <name>Echo balancer</name>
      <accept>ws://balancer.example.com:8081/echo</accept>
      <accept>wss://balancer.example.com:9091/echo</accept>

      <type>balancer</type>

      <accept-options>
          <ws.bind>192.168.2.10:8081</ws.bind>
          <wss.bind>192.168.2.10:9091</wss.bind>
      </accept-options>

    </service>
    ```

    **Important:**

    -   The `ws.bind` element in `accept-options` is used to bind the public URI in the `accept` element to the local IP address of the cluster member. This allows the `accept` URIs in the `balancer` service to be identical on every cluster member. Only the `ws.bind` element needs to be unique in each cluster member (contain the local IP address of that cluster member). For more information, see [*protocol*.bind](../admin-reference/r_configure_gateway_service.md#protocolbind).
    -   For production environments, the hostname in the `accept` element must resolve in DNS to the IP addresses of every cluster member. Multiple DNS A resource records should be registered for the hostname in the `accept` URI, with each A record mapping the hostname to the IP address of one cluster member. When a client resolves the hostname of the `accept` URI in DNS, it will receive the IP address of a cluster member and connect. To register these DNS records, you will need access to the public DNS zone for the hostname, or the assistance of your network administrator or Internet Service Provider (ISP). All ISPs provide ways for their customers to update their DNS zones with new hostnames and IP addresses.

2.  Configure the services of each cluster member (for example, the `echo` service) with a `balance` element for each URI accepted by the `balancer` service. Clients will use these URIs to connect to the service.

    ``` xml
    <service>
      <name>Echo</name>
      <accept>ws://node1.example.com:8081/echo</accept>
      <accept>wss://node1.example.com:9091/echo</accept>
      <balance>ws://balancer.example.com:8081/echo</balance>
      <balance>wss://balancer.example.com:9091/echo</balance>

      <type>echo</type>

      <cross-site-constraint>
        <allow-origin>http://directory.example.com:8080</allow-origin>
      </cross-site-constraint>
      <cross-site-constraint>
        <allow-origin>https://directory.example.com:9090</allow-origin>
      </cross-site-constraint>

    </service>
    ```

    **Important:**

    -   The `balance` and `accept` element URIs in a `service` must use the same port number and path. The hostnames in the URIs may be different.
    -   For information on the order of subordinate elements in a service, see the [service](../admin-reference/r_configure_gateway_service.md#service) element reference.

3.  Ensure that each Gateway participating in load balancing is configured with a `cluster` service, as described in [Configure a KAAZING Gateway Cluster](p_high_availability_cluster.md).
4.  Configure client applications to connect to the cluster using the URI(s) configured in the `balancer` service (for example, `ws://balancer.example.com:8081/echo` or `wss://balancer.example.com:9091/echo`).
5.  Start all cluster members.
6.  Monitor the cluster using the steps in [Monitor with Command Center](../management/p_monitor_cc.md). To monitor a cluster, the default URL is: `http://localhost:8080/commandcenter`.

Example Configuration
---------------------

Here is an example of a Gateway configured as a cluster member and load balanced for both secure and unsecured Echo services:

``` xml
<!-- Cluster service -->
<cluster>
  <name>MyCluster</name>
  <accept>tcp://192.168.2.10:5942</accept>
  <connect>udp://224.2.2.44:54327</connect>
</cluster>

<!-- Balancer service for connections -->
<service>
  <name>Echo balancer</name>
  <accept>ws://balancer.example.com:8081/echo</accept>
  <accept>wss://balancer.example.com:9091/echo</accept>

  <type>balancer</type>

  <accept-options>
      <ws.bind>192.168.2.10:8081</ws.bind>
      <wss.bind>192.168.2.10:9091</wss.bind>
  </accept-options>
</service>

<!-- Echo service for connections -->
<service>
  <name>Echo</name>
  <accept>ws://node1.example.com:8081/echo</accept>
  <accept>wss://node1.example.com:9091/echo</accept>

  <balance>ws://balancer.example.com:8081/echo</balance>
  <balance>wss://balancer.example.com:9091/echo</balance>

  <type>echo</type>

  <cross-site-constraint>
    <allow-origin>http://directory.example.com:8080</allow-origin>
  </cross-site-constraint>
  <cross-site-constraint>
    <allow-origin>https://directory.example.com:9090</allow-origin>
  </cross-site-constraint>
</service>
```

Next Step
----------------------------

You have completed high availability configuration using the Gateway.

Notes
-----

-   To set up a cluster locally for testing purposes, see [Configure a Two-Member Local Demo Cluster](../high-availability/u_high_availability.md#configure-a-two-member-local-demo-cluster).

See Also
--------

-   [Common KAAZING Gateway Production Topologies](../admin-reference/c_topologies.md).
-   For information about the order of subordinate elements in a service, see the [service](../admin-reference/r_configure_gateway_service.md#service) element reference.
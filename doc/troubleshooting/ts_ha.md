-   [Home](../../index.md)
-   [Documentation](../index.md)
-   Troubleshooting KAAZING Gateway

Troubleshoot KAAZING Gateway Clusters and Load Balancing
====================================================================================

When you configure KAAZING Gateway for high availability, you may encounter one of the following errors. To resolve an issue, follow the guidance provided for the associated error.

<a name="all_trouble"></a></span>Troubleshooting KAAZING Gateway
---------------------------------------------------------------------

This topic is part of [Troubleshoot KAAZING Gateway](o_ts.md) that groups troubleshooting topics into the categories shown in the following table:

| What Problem Are You Having?    | Topic or Reference                                                             |
|---------------------------------|--------------------------------------------------------------------------------|
| Configuration and startup       | [Troubleshoot KAAZING Gateway Configuration and Startup](ts_config.md) |
| **Clusters and load balancing** | **Troubleshoot KAAZING Gateway Cluster and Load Balancing**              |
| Security                        | [Troubleshoot KAAZING Gateway Security](ts_security.md)                |
| Clients                         | [Troubleshoot Your Clients](p_dev_troubleshoot.md)                           |

What Problem Are You Having?
----------------------------

-   [Accept URI: [URI] does not match balance URI: [URI] in all but hostname. Unable to launch Gateway](#acceptURI)
-   [Balance URI: [URI] does not point to a balancer service's accept URI in the configuration file, unable to launch the Gateway](#balanceURI)
-   [Detected orphaned balancer accept URI: [URI], no balance URIs in the configuration file point to this balancer service. Unable to launch the Gateway](#orphaned)

### <a name="acceptURI"></a>Accept URI: [URI] does not match balance URI: [URI] in all but hostname. Unable to launch Gateway.

**Cause:** The `balance` and `accept` element URIs in a `service` must use the same port number and path. The hostnames in the URIs may be different.

****
Solution:</strong> Correct the `balance` or `accept` URIs by using the same port number and path. In the following example, the `balance` and `accept` URIs have different hostnames but the same port number (`8081`) and path (`echo`):

``` auto-links:
<service>
  <accept>ws://node1.example.com:8081/echo</accept>
  <balance>ws://balancer.example.com:8081/echo</balance>

  <type>echo</type>

  <cross-site-constraint>
    <allow-origin>http://directory.example.com:8080</allow-origin>
  </cross-site-constraint>
</service>
```

### <a name="balanceURI"></a>Balance URI: [URI] does not point to a balancer service's accept URI in the configuration file, unable to launch the Gateway.

**Cause:** The `balance` URI in a `service` must match the `accept` URI in a `balancer` service in the same Gateway configuration file (`GATEWAY_HOME/conf/gateway-config.xml`).

**Solution:** Ensure that the `balance` URI in a `service` matches at least one `accept` URI in a `balancer` service. In the following example, the `accept` URI in the `balancer` service matches the `balance` URI in the Echo `service`:

``` auto-links:
<service>
  <accept>ws://balancer.example.com:8081/echo</accept>

  <type>balancer</type>

  <accept-options>
      <ws.bind>192.168.2.10:8081</ws.bind>
  </accept-options>

</service>

<service>
  <accept>ws://node1.example.com:8081/echo</accept>
  <balance>ws://balancer.example.com:8081/echo</balance>

  <type>echo</type>

  <cross-site-constraint>
    <allow-origin>http://directory.example.com:8080</allow-origin>
  </cross-site-constraint>
</service>
```

### <a name="orphaned"></a>Detected orphaned balancer accept URI: [URI], no balance URIs in the configuration file point to this balancer service. Unable to launch the Gateway.

**Cause:** There is an `accept` URI in a `balancer` service that does not have a corresponding `balance` URI in a `service`. Every `accept` URI in a `balancer` service must have a corresponding `balance` URI in a `service`.

**Solution:** Remove the `accept` from the `balancer` service or add a corresponding `balance` URI to a `service`.

See Also
--------

[Configure the Gateway for High Availability](../high-availability/o_ha.md)



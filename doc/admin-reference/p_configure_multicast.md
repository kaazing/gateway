Configure the Gateway to Use Multicast
=======================================================================================

This procedure demonstrates how to configure a service that is running on KAAZING Gateway to use a multicast address (for example, a broadcast service).

<a name="configure_multicast"></a>To configure a service to use multicast, you must specify the following elements in the Gateway configuration file (for example, `GATEWAY_HOME/conf/gateway-config.xml`):

-   Specify the multicast URI udp://*group-address*:*port-number* in the `accept` or `connect` element.
-   Specify the udp interface name in the `accept-option` or `connect-option`.

The following example shows a service using a multicast address. In the example, data packets sent to `udp://multicast-group:port` with udp-interface `eth0` will be broadcast to all clients connected to `sse://localhost:8000/sse`.

``` xml
<!-- Broadcast multicast messages -->
<service>
  <accept>sse://localhost:8000/sse</accept>

  <type>broadcast</type>
  <properties>
    <accept>udp://224.0.0.1:port-number</accept>
      <accept-option>
        <udp-interface>eth0</udp-interface>
      </accept-option>
  </properties>

  <cross-site-constraint>
    <allow-origin>http://localhost:8000</allow-origin>
  </cross-site-constraint>

  <cross-site-constraint>
    <allow-origin>https://localhost:9000</allow-origin>
  </cross-site-constraint>
</service>
```

To verify this configuration, you can start the newsfeed data source that ships with KAAZING Gateway and pass the multicast URI you configured as a startup argument. To do this, perform the following steps:

1.  Add the above example to `GATEWAY_HOME/conf/gateway-config.xml` but configure ` udp-interface` to `lo0` (or bind to an interface available for your system). You might need to comment out any other `service` elements using the same `accept` or `connect`.
2.  In a command prompt or shell, navigate to `GATEWAY_HOME/bin`.
3.  Run `demo-services.start udp://multicast-group:port-number` (for example, `gateway.demos.start udp://multicast-group`) to start the newsfeed data source.
4.  Run `gateway.start` to start the Gateway.
5.  In a browser, navigate to the server-sent events demo page (by default, this is located at `http://localhost:8001/demo/core/javascript/?d=sse`), enter `http://localhost:8000/sse` and watch the streaming news feed data.

You can now configure and run a second KAAZING Gateway to serve server-sent event streams from a different host and port for the same newsfeed multicast data packets. Note that services cannot send UDP packets to an MCP acceptor, nor MCP packets to a UDP acceptor.

See Also
-------------------------------

-   The [`broadcast`](r_configure_gateway_service.md#broadcast) element in the Service Reference.
-   [Classful Networks](http://en.wikipedia.org/wiki/Classful_network "Follow link") for more information about Class D address classes.

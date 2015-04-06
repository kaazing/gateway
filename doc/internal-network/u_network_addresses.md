-   [Home](../../index.md)
-   [Documentation](../index.md)
-   About Internal Network Integration with ${gateway.name.long}

<span id="addressmapconfig2"></span></a>About Internal Network Integration with ${gateway.name.long} ${enterprise.logo.jms}
===========================================================================================================================

The `accept` element in a service is frequently sufficient for the service to receive messages and requests. However, sometimes the value of the `accept` element does not match the physical description of the host. In some situations, ${gateway.name.short} might be behind a load balancer or in a cloud environment, in which case its local network information may not match the settings specified by the `accept` element. Configuring ${the.gateway} in this scenario requires the [*protocol*.bind](../admin-reference/r_conf_service.md#protocolbind)element.

For instance, your service may accept on `ws://www.example.com`</strong> but that hostname resolves to the IP address of the load balancer, not ${the.gateway} host. In such a case, you can configure ${the.gateway} using the [*protocol*.bind](../admin-reference/r_conf_service.md#protocolbind) element, which enables you to bind the address specified in the `accept` element to an internal address (where the address maps in your network). This option provides a choice of binding the address to a specific *port* or *hostname and port* (if you specify a hostname, you must specify a port). You can specify these options for each Gateway service, as well as set default values for all services.

In an example scenario, a firewall connected directly to the Internet has the IP address `172.16.0.105` and the public DNS name `www.example.com`. ${gateway.name.long} is running on `gateway.dmz.net` with IP address `192.168.0.25`. The firewall forwards network traffic to ${gateway.name.long}, which has to serve HTTP requests for `www.example.com` on port `80`. These requests must be routed through the firewall to ${the.gateway}'s internal IP address (`192.168.0.25`) on port `8000`.

You can use the [*protocol*.bind](../admin-reference/r_conf_service.md#protocolbind) ** element to override ${the.gateway}'s default behavior. Using this element, you can tell ${the.gateway} host machine to bind the external address specified in the `accept` element to an internal network interface or IP address. In the [*protocol*.bind](../admin-reference/r_conf_service.md#protocolbind) element, you can bind external addresses to your internal network interface(s) by specifying the hostname *and* port for the interface on which you wish ${the.gateway} to listen. Alternatively, you can specify only the port number to tell ${the.gateway} to listen on all available network interfaces using the specified port number.

In this example, you can bind the external address of the firewall (the address specified in the `accept` element) , which is `ws://www.example.com:80` to the internal address `gateway.dmz.net:8000` using the WebSocket (`ws`) protocol.

``` auto-links:
<service>
  <accept>ws://www.example.com:80</accept>
  <type>echo</type>
  <accept-options>  
    <ws.bind>gateway.dmz.net:8000</ws.bind>
  </accept-options>
</service>
```

This signals ${gateway.name.long} to listen on the internal address `ws://gateway.dmz.net:8000` to serve requests originally sent to the external address (the address specified in the `accept` element) `ws://www.example.com:80`. Alternatively, the IP address can be used in the configuration parameters (you can also specify an IP address and port for the external address in the `accept` element).

See Also
--------

-   [Integrate ${the.gateway} on an Internal Network](p_network_addresses.md)



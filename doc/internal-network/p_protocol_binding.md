Configure the Gateway on an Internal Network
==============================================================================================================

This document describes how to configure KAAZING Gateway in an internal network using the [*protocol*.bind](../admin-reference/r_configure_gateway_service.md#protocolbind) element. In this procedure you will do the following:

1.  Configure network protocol bindings for all services.
2.  Configure the Gateway to accept traffic on multiple IP addresses.
3.  Start the Gateway with the new configuration.

Before You Begin
----------------

Read the overview of integrating the Gateway into an internal network in [About Internal Network Integration with KAAZING Gateway](u_protocol_binding.md).

To Integrate the Gateway Into an Internal Network
----------------------------------------------------------------------------------

1.  Configure network protocol bindings for all services.

    You can specify default values for the accept options for all services. If you then specify values for the [accept-options](../admin-reference/r_configure_gateway_service.md#accept-options-and-connect-options) on a particular service, then the service `accept-options` supercede the default values with your specified values. If there are no explicit `accept-options` for a particular service, then the service uses these default values. By using the `protocol.bind` element within the [service-defaults](../admin-reference/r_configure_gateway_service_defaults.md) element in the `gateway-config.xml`, you can configure network protocol bindings for all services.

    The following example shows `ssl.encryption` disabled, `ws.maximum.message.size` set to `256k`, and several network protocol bindings (`ws.bind`, `wss.bind`, `http.bind`, and `https.bind`):

    ``` xml
    <service-defaults>
        <accept-options>
            <ssl.encryption>disabled</ssl.encryption>
            <ws.bind>8050</ws.bind>
            <wss.bind>192.168.10.25:8055</wss.bind>
            <http.bind>192.168.10.25:8060</http.bind>
            <https.bind>192.168.10.25:8065</https.bind>
            <ws.maximum.message.size>256k</ws.maximum.message.size>
        </accept-options>

    <mime-mapping>
        <extension>html</extension>
         <mime-type>text/html</mime-type>
    </mime-mapping>
    ...
    </service-defaults>
    ```

    For more information, see the [Service Defaults Reference](../admin-reference/r_configure_gateway_service_defaults.md) documentation.

2.  Configure the Gateway to accept traffic on multiple IP addresses.

    If the Gateway host machine has multiple IP addresses and you want the machine to accept traffic on all addresses, you can either:

    -   Specify *every* hostname and port number on which you wish it to listen, or
    -   Specify *only* the port number on which you wish the Gateway to listen

    The following example shows a configuration where only the port number is specified, so that the Gateway listens to all interfaces using that port. Here, the Gateway binds to the wildcard address (`0.0.0.0`) as the host in the `<wss.bind>` element.

    ``` xml
    <service>
        <accept>wss://www.example.com:443</accept>
        <type>echo</type>
        <accept-options>  
            <wss.bind>8001</wss.bind>
        </accept-options>
    </service>
    ```

    You can only bind compatible protocols to the same host and port combination (unencrypted protocol traffic versus protocol traffic encrypted with Transport Layer Security (TLS, also known as SSL)).

    For example, you cannot bind HTTP and HTTPS to the same address because these protocols are not compatible. You can bind HTTP and WebSocket (`ws`) together, or HTTPS and WebSocket Secure (`wss`). The following example shows compatible protocols bound to the same address. In this example, switching `<http.bind>` to `<wss.bind>` would result in a port conflict error at startup.

    ``` xml
    <service>
        <accept>http://www.example.com:80</accept>
        <accept>ws://www.example.com:80</accept>
        <type>echo</type>
        <accept-options>
            <http.bind>gateway.dmz.net:8000</http.bind>
            <ws.bind>gateway.dmz.net:8000</ws.bind>
        </accept-options>
    </service>
    ```

3.  Start the Gateway with the new configuration.

    Once you save the `gateway-config.xml` file with your changes, you can start up the Gateway. The ` protocol.bind` elements you've added display in the Gateway startup log.

    For example, if you've configured the Gateway with the following:

    ``` xml
    <service>
        <accept>http://www.example.com:80/echo</accept>
        <accept>ws://www.example.com:80/echo2</accept>
        <type>echo</type>
        <accept-options>
            <http.bind>gateway.dmz.net:8000</http.bind>
            <ws.bind>8000</ws.bind>
        </accept-options>
    </service>
    ```

    Then, on startup, the network address protocol bindings you've set display in the log, as shown in the following example:

      ```
      INFO Starting server
      INFO Starting services
      INFO http://localhost:8000/
      INFO https://localhost:9000/
      INFO http://www.example.com:80/echo @ gateway.dmz.net:8000
      INFO ws://www.example.com:80/echo2 @ 0.0.0.0:8000
      INFO Started services
      INFO Starting management
      INFO jmx://localhost:2020/
      INFO Started management
      INFO Started server successfully in 0.345 secs at 2044-06-06 11:32:58
       ```

Next Step
--------------------------------

You have configured the Gateway on an internal network. For more information about Gateway administration, see the [documentation](../index.md).

Notes
-----

-   The [*protocol*.bind](../admin-reference/r_configure_gateway_service.md#protocolbind) element binds on the specified address, but only routes traffic for the given *protocol*. If you specify `ws.bind` but then accept on a different protocol (such as TCP) will not be affected by the `ws.bind` `accept-option`.

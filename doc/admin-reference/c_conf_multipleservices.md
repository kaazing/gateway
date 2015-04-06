-   [Home](../../index.md)
-   [Documentation](../index.md)
-   Configuring Multiple Services on the Same Host and Port

<a name="configmultsrvcs"></a>Configuring Multiple Services on the Same Host and Port ${enterprise.logo.jms}
============================================================================================================

Typically, for a network connection, you configure an IP address or configure a DNS hostname that is resolved to an IP address, and a port. For example:

**Note:** This documentation uses a running example with the URI scheme: `ws://example.com:80/remoteService1` to demonstrate ${gateway.cap} configurations.

``` auto-links:
<service>
  <accept>ws://example.com:80/</accept>
  .
  .
  .
</service>
```

In the example, ${the.gateway} creates a socket for port 80 on the IP address resolved by the hostname `example.com` and listens for requests. Typically, ${the.gateway} binds to (and listens on) only one socket for a given IP address and port. However, you can configure ${the.gateway} for multiple services to the same host and port. For example, you can configure similar services to use the same IP address and port number to organize multiple connection requests on the same server as ${the.gateway} and avoid conflicts.

You configure multiple services by specifying a unique path in the URI of [`accept`](../admin-reference/r_conf_service.md#acceptele) or [`connect`](../admin-reference/r_conf_service.md#connectele) elements. ${the.gateway.cap}, listening for all of these requests, can determine which service a request is targeting based on the combination of the scheme, host, port, and path.

Our configuration example configures the following two services to specify that ${the.gateway} binds to the same URL scheme, host, and port: `ws://example.com:80`. However, notice that each URL specifies a unique path: `remoteService1` and `remoteService2`.

``` auto-links:
<service>
  <accept>ws://example.com:80/remoteService1</accept>
  .
  .
  .
  </service>

<service>
  <accept>ws://example.com:80/remoteService2</accept>
  .
  .
  .
</service>
```

All inbound requests can communicate with multiple services on a specific host and port, and collisions are avoided because the requests use different paths. The example uses `remoteService1` and `remoteService2`, but you can specify any name on the path so long as it conforms internet URL standards ([RFC 3986](http://tools.ietf.org/html/rfc3986)). In fact, you may chose to tailor the path to reflect the actual function of the service.

**Note:** ${gateway.name.long} follows the generic syntax defined for all URI schemes as defined by [RFC 3986](http://tools.ietf.org/html/rfc3986). ${begin.comment}See [About the Documentation](../about/about.md#docconventions) for more information about URI schemes with ${the.gateway}.${end.comment}

${the.gateway.cap} determines which of the two addresses to use when the client sends a request to connect with one of the pathnames. Thus, if a client makes a WebSocket connection to `ws://example.com:80/remoteService1`, then ${the.gateway} reads the network headers and routes the request as configured by the `accept` in `<accept>ws://example.com:80/remoteService1</accept>`.

<a name="notes"></a>Notes
-------------------------

Note the following when configuring ${the.gateway} to listen for multiple services on the same host and port:

-   You can only configure multiple services on ${the.gateway} to use the same hostname and port if the URI scheme supports paths.

    You cannot configure two services with the same URI scheme (for example: `<accept>ws://example.com:80/myService</accept>` because ${the.gateway} will be unable to determine which service is the target of the request. Thus, you must differentiate the services using different paths. This differentiation only applies to URI schemes that support paths such as HTTP, HTTPS, WebSocket, WebSocket Secure, SSE, and so on. Other protocols, such as TCP and UDP do not have the notion of paths and are only aware of the host and port. Thus, only one service can listen for TCP connections such as \<accept\>tcp://example.com:80\</accept\>.

    <p>
    **Note:** You can work around the limitations of schemes that do not support paths by using a bind in the [`accept-options`](../admin-reference/r_conf_service.md#svcacceptopts) element to bind an URL or URLs on which the service accepts connections. See the [`protocol.bind`](../admin-reference/r_conf_service.md#protocolbind) documentation for more information.

-   The path component of the URL is mandatory.

    ${the.gateway.cap} strictly enforces URL semantics. Thus a path is mandatory in your URI scheme and the minimal path you can configure is `/`. If you configure multiple services for the same host and port without a unique path, then ${the.gateway} returns an exception during startup.

-   Client requests must exactly match the URL specified in ${the.gateway} configuration such that the scheme, host, port, and path all match.

    The running example used in this discussion consistently specifies both the host and port in the URI (`ws://example.com:80/remoteService1`). However, there is one caveat to the rule and it is that client requests over standard ports can be defaulted. Thus, the client can actually request either `ws://example.com:80/remoteService1` or `ws://example.com/remoteService1`. In the latter case, ${the.gateway} assumes the client intended to communicate over port 80. You can configure multiple services over standard ports.

-   Clients can connect using either the hostname or the IP address if you configure an `accept` element for each.

    The following example configures two `accept` elements for the service: one with the host’s hostname and the other with its IP address:

    ``` auto-links:
    <service>
      <accept>ws://example.com:80/remoteService1</accept>
      <accept>ws://26.194.83.29:80/remoteService1</accept>
      .
      .
      .
    </service>
    ```

<a name="seealso"></a>
See Also
--------

-   [Service Reference](r_conf_service.md)

</div>


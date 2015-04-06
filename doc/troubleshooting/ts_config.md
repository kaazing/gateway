-   [Home](../../index.md)
-   [Documentation](../index.md)
-   Troubleshooting ${gateway.name.short}

Troubleshoot ${gateway.name.short} Configuration and Startup${enterprise.logo.jms}
==================================================================================

When you set up and configure ${gateway.name.short}, you may encounter one of the errors described in this topic. To resolve an issue, follow the guidance provided for the associated error.

<a name="all_trouble"></a></span>Troubleshooting ${gateway.name.long}
---------------------------------------------------------------------

This topic is part of [Troubleshoot ${gateway.name.long}](o_ts.md) that groups troubleshooting topics into the categories shown in the following table:

| What Problem Are You Having?  | Topic or Reference                                                          |
|-------------------------------|-----------------------------------------------------------------------------|
| **Configuration and startup** | **Troubleshoot ${gateway.name.short} Configuration and Startup**            |
| Clusters and load balancing   | [Troubleshoot ${gateway.name.short} Cluster and Load Balancing](ts_ha.md) |
| Security                      | [Troubleshoot ${gateway.name.short} Security](ts_security.md)             |
| Clients                       | [Troubleshoot Your Clients](p_dev_troubleshoot.md)                        |

What Problem Are You Having?
----------------------------

-   [Port 8000 or 8001 Is Already in Use](#portconflict)
-   [Maximum Number of Connections Reached or Exceeded](#numconnections)
-   [Too Many Open Files Warning](#ulimit)
-   <a href="#outofmemory_error">Out of Memory Error When Starting ${gateway.name.short}
-   [Error Starting ${gateway.name.short} on the Microsoft Vista Operating System](#ipv6_error)
-   [Error: Unable to bind to resource: [network address] @ [network address] cause: Address already in use.](#nobind)
-   [Error When Starting ${the.gateway}: String Value [*'value'*] does not match pattern for DataSize in namespace](#invalidbuffervalue)
-   [Warning: Error on WebSocket connection](#wsmaxmsgwarn)
-   [Warning: ERROR string value '*value*' does not match pattern](#wsmaxbadvalue)
-   [Localhost Is Not Configured](#localhostnot)
-   [Demos Do Not Work When Using Fiddler Web Debugger as an HTTP Proxy](#fiddler)
-   [Error Using ${the.gateway} to Proxy From Back-End Server over TCP to Client over WebSocket](#bytesocket)

### <a name="portconflict"></a>Port 8000 or 8001 Is Already in Use

**Cause:** Port conflict is a common problem that occurs when another web server or process is already using one of the default ${gateway.name.long} ports, either port 8000 (the default HTTP port to which ${the.gateway} binds at startup) or 8001 (includes ${the.gateway} documentation and demos).

**Solution:** To avoid port conflicts, change the port number by performing the following steps:

1.  Open the file `GATEWAY_HOME/conf/gateway-config.xml` and search for instances of 8000 or 8001.
2.  Change the instances to a port number that is not in use and is greater than 1024, because ports less than or equal to 1024 require superuser access to bind under UNIX. For example, you might change the default home-page port from 8000 to 8884.
3.  Restart ${the.gateway}.
4.  Test the port change by accessing ${the.gateway} at `http://localhost:new-port-number/` in your browser. For example, if you changed the default port from 8000 to 8884, you can now access ${the.gateway} home page at `http://localhost:8884/`.

### <a name="numconnections"></a>Maximum Number of Connections Reached or Exceeded

**Cause:** You have exceeded the number of allowed client connections to ${the.gateway}. The terms of your ${gateway.name.short} license specify a maximum number of allowed concurrent client connections. For example, the developer's version of the ${gateway.name.short} bundle allows only a limited number of concurrent client connections.

If you see the following warning message in the file `GATEWAY_HOME/log/error.log` file:

WARN Maximum number of connections reached.

You have exceeded the *soft limit* of allowed connections. To give you time to to upgrade the terms of your license and to avoid closed connections, additional connections are still allowed. However, if you see the following error message:

ERROR Maximum number of connections exceeded. Closing connection.

You have exceeded the *hard limit* of allowed connections and all new incoming connections are closed until you have upgraded the terms of your license.

**Solution:** ${upgrade.license}[](mailto:sales@kaazing.com).

### <span id="ulimit"></span></a>Too Many Open Files Warning

**Cause:** If your server is unable to open more files to handle the number of open client connections, then the following warning message is recorded in the file `GATEWAY_HOME/log/error.log` file on UNIX or Linux:

java.io.IOException: Too many open files

By default, a UNIX or Linux user may only be able to open a maximum of a few hundred or thousand files. A number of file handles are used for each client connection that is established with ${gateway.name.short}: typically one for the client connection (double that in case of WebSocket emulation) and one for the back-end service connection.

**Solution:** To avoid running out of available file handles you should increase the limit of files that the user can open to handle your expected load.

1.  Check the open file limit on UNIX or Linux, use the `ulimit` command as shown in the following example:

    `ulimit -n`

2.  Update the open file limit:
    -   To temporarily update the open file limit, use the `ulimit` command as shown in the following example:

        `ulimit -n <new_value>`

    -   To permanently apply the `ulimit` value, you may need to restart and reconfigure your server settings. Refer to your operating system documentation for more information.

### <a name="outofmemory_error"></a>Out of Memory Error When Starting ${gateway.name.short}

**Cause:** An out-of-memory error can result when starting ${the.gateway} if the maximum heap size is not set appropriately for your physical memory capacity. The default maximum heap size is only 516 MB, which typically is not sufficient for production deployment.

**Solution:** Set the Java `-Xmx` option in your ${gateway.cap} startup file to approximately 70% of the physical memory capacity. For example, if the physical memory capacity of the machine is 4 GB, then edit your ${gateway.cap} startup script and modify the `GATEWAY_OPTS` environment variable to `-Xmx3072m`. (The default startup script is the `GATEWAY_HOME/bin/gateway.start` file.) Then start ${the.gateway}. See [Configure ${gateway.name.short} Using the GATEWAY\_OPTS Environment Variable](../admin-reference/p_conf_gw_opts.md) for more information about setting the `GATEWAY_OPTS` environment variable.

### <a name="ipv6_error"></a>Error Starting ${gateway.name.short} on the Microsoft Vista Operating System

**Cause:** If you are running ${gateway.name.short} on the Microsoft Vista operating system and an IPv6 address is specified for `localhost` in your `hosts` file, then you could receive the following error message:

java.net.SocketException: Address family not supported by protocol family: bind

**Solution:** To start the server successfully, perform the following steps:

1.  Open your `hosts` file (typically `C:\Windows\System32\drivers\etc\hosts`).
2.  Replace:

    `::1 localhost`

    With:

    `127.0.0.1 localhost`

3.  Save the file and restart ${gateway.name.short}.

### <span id="nobind"></span></a>Error: Unable to bind to resource: [network address] @ [network address] cause: Address already in use.

**Cause:** On startup, ${the.gateway} is unable to bind to the resource and returns an exception similar to the following in the log:

`java.lang.RuntimeException: Unable to bind to resource: wss://localhost:9001/echo @ wss://localhost:9111/echo cause: Address already in use               at org.kaazing.gateway.server.transport.nio.AbstractNioAcceptor.bind(AbstractNioAcceptor.java:98)...`

This exception can be caused by the following conditions:

-   The port being bound is in use by another external process running on the host, such as an Apache HTTP server.
-   The port listed in the error has already been bound to another incompatible protocol in the `gateway-config.xml`. For example, you cannot bind HTTP and HTTPS to the same host and port combination.

**Solution:** Check the [*protocol.*bind](../admin-reference/r_conf_service.md#protocolbind) element in your services (you may need to check the [service-defaults](../admin-reference/r_conf_serv_defs.md#servicedefaults) section, as well) to ensure that multiple services are not bound to same port. Also, check that secure and unsecure protocols are not bound to the same port.

### <a name="invalidbuffervalue"></a>Error When Starting ${the.gateway}: String Value [*'value*'] does not match pattern for DataSize in namespace

**Cause:** When the `maximum.pending.bytes` property contains an invalid value, the following errors display when you start ${the.gateway}:

ERROR Validation errors in gateway-config.xml ERROR   Line: 15 Column: 33 ERROR   string value '128KB' does not match pattern for DataSizeString in namespace http://xmlns.kaazing.com/2014/09/gateway ERROR   \<xml-fragment xmlns:xsi=["http://www.w3.org/2001/XMLSchema-instance"](http://www.w3.org/2001/XMLSchema-instance)/\>

**Solution:** Set the `maximum.pending.bytes` property of the ${proxy.service.inline.link} service element to a valid value.

### <a name="wsmaxmsgwarn"></a>Warning: Error on WebSocket connection

**Cause:** When a message incoming from a client to ${the.gateway} exceeds the maximum size either specified by [`ws.maximum.message.size`](../admin-reference/r_conf_service.md#wsmaxmsg) or by the default Gateway configuration of 128k, the following warning is written to ${the.gateway} log:

`2044-06-06 16:19:28,621 WARN  Error on WebSocket connection, closing connection: incoming message size exceeds permitted maximum of 131072 bytes (Hexdump:...)`

**Solution:** To resolve this issue, the client can reconnect to ${the.gateway}. If you wish to adjust the maximum message size that ${the.gateway} can accept from a client, see [ws.maximum.message.size](../admin-reference/r_conf_service.md#wsmaxmsg).

### <a name="wsmaxbadvalue"></a>Warning: ERROR string value '*value*' does not match pattern

**Cause:** When the `ws.maximum.message.size` element is set to an invalid value (indicated by "*value*" in the example warning), the following warning occurs when ${the.gateway} starts:

`ERROR   string value some_value does not match pattern for DataSizeString in namespace http://xmlns.kaazing.com/2014/09/gateway`

**Solution:** Open the `GATEWAY_HOME/conf/gateway-config.xml` and set the [`ws.maximum.message.size`](../admin-reference/r_conf_service.md#wsmaxmsg) to a valid value.

### <a name="localhostnot"></a>Localhost Is Not Configured

**Cause:** “Localhost” is not found when you try to access ${gateway.name.short}. This can happen if your browser is configured to use a proxy server.

**Solution:** Make sure your proxy configuration bypasses the proxy to access `localhost`.

### <a name="fiddler"></a>Demos Do Not Work When Using Fiddler Web Debugger as an HTTP Proxy

**Cause:** When Fiddler is used as an HTTP proxy, it can handle both `http://` and `https://` traffic. However, to monitor the `https://` traffic, Fiddler must be able to decrypt the traffic. Fiddler can do this only if it serves a [fake] SSL certificate. When ${gateway.name.short} detects that traffic is flowing through an HTTP proxy (such as Fiddler), ${the.gateway} attempts to automatically use an SSL downstream for the emulated WebSocket because unencrypted streaming responses can be buffered by the HTTP proxy. This can cause potentially lengthy delays in message delivery. The browser automatically fails when attempting to download an `https://` URL with untrusted (fake) certificate.

**Solution:** When the main URL for the entire page is from the fake URL the page displays some links allowing the user to *trust* the fake certificate anyway. For example, surfing to `https://localhost:9000/` with Fiddler as the HTTP proxy presents the opportunity to trust the fake localhost certificate served by Fiddler. Once the browser has been instructed to trust the fake Fiddler certificate, then the demos should continue to work as before.

**See Also:** [`http://www.fiddler2.com/Fiddler/help/httpsdecryption.asp`](http://www.fiddler2.com/Fiddler/help/httpsdecryption.asp) for more information about Fiddler.

### <a name="bytesocket"></a>Error Using ${the.gateway} to Proxy from Back-End Server over TCP to Client over WebSocket

**Cause:** When ${the.gateway} is configured as a proxy receiving binary data over TCP from a back-end server and sends that data over WebSocket to Flash. .NET, or Silverlight client applications, the client applications must use a ByteSocket client library to send and receive binary data. In this scenario, using a WebSocket client library on the client application will fail. Note: the WebSocket class in ${gateway.name.long} JavaScript, Java, Objective-C, and Android clients supports binary.

**Solution:** Include a ByteBuffer class (included with all of the APIs) that provides an automatically expanding byte buffer for ByteSocket.



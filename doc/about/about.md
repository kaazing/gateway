Documentation Conventions
================================================

This topic contains the following sections:
-   [Text Conventions](#text-conventions)
-   [About URI Syntax](#about-uri-syntax)
-   [About Domain Names](#about-domain-names)
-   [About Ports](#about-ports)
-   [About KAAZING_HOME](#about-kaazing_home)
-   [About GATEWAY_HOME](#about-gateway_home)

Text Conventions
---------------------------------------------

The following text conventions are used in the KAAZING Gateway topics:

| Convention    | Meaning                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                      |
|---------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| ${*variable*} | A variable within the dollar-sign and curly brace format specifies a property default value in the Gateway configuration file, for example `GATEWAY_HOME/conf/gateway-config.xml`. Property default values are propagated throughout the configuration when the Gateway starts. For example, you could specify `gateway.example.com` as the default value for the `${gateway.hostname}` property. Variables shown in italics but without the dollar-sign and curly brace format indicate placeholder variables for which you supply particular values. |
| **bold**      | Bold typeface indicates graphical user interface elements associated with an action, or terms defined in text or the glossary. It also indicates your location in a series of steps such as in the "Before You Begin" section in any procedural topic.                                                                                                                                                                                                                                                                                                       |
| *italic*      | Italic type indicates book titles, emphasis, or placeholder variables for which you supply particular values.                                                                                                                                                                                                                                                                                                                                                                                                                                                |
| `monospace`   | Monospace type indicates commands within a paragraph, URLs, code in examples, text that appears on the screen, or text that you enter.                                                                                                                                                                                                                                                                                                                                                                                                                       |
| ...             | An ellipsis indicates that a portion of an example that would normally be present is not shown.


About URI Syntax
----------------------------------------

The Gateway documentation uses a generic URI scheme that has the following components:

`scheme://host:port/path`
For example, the following URI schemes are used in the documentation:

`tcp://${gateway.hostname}:${gateway.extras.port}`

`ws://gateway.example.com:80/`

`wss://localhost:9000/echo`

`sse://localhost:8000/sse`

-   The **scheme** describes how to connect and is sometimes referred to as the protocol. When specifying URIs in a Gateway configuration, you can use `tcp://{hostname}:{port}` to make a basic TCP connection, or specify any of the supported schemes such as http, https, ws, wss, sse, and so on. See the [supported URI schemes](../admin-reference/r_configure_gateway_service.md#supported-url-schemes) for the complete list.
-   The **host** specifies where to connect and can be a hostname or domain name, or an IP address.
-   The **port** specifies the port number to ask for. This portion of the URI scheme is optional if you are using a default port, such as port 80 for http or port 443 for https. For example, when using the http scheme you do not need to specify port 80.
-   The **path** refers to the path of the resource. At a minimum you must specify the root path (`/`). Thus, `http://example.com/` is a legal address, but `http://example.com` is not, even though in practice the final slash "`/`” is added automatically.

In addition, you can append a query string to the URL to provide non-hierarchical information. The query string follows a question mark (?) appended to the URL. For example, you can use this query string when configuring security for your Gateway (as described in the [Security Reference](../admin-reference/r_configure_gateway_security.md)).

**Note:** In the example URLs, the `${gateway.hostname}` syntax allows you to define property values once and then the values are propagated throughout the configuration when the Gateway starts. You can replace any value in the configuration file that uses the dollar-sign and curly brace format (such as `${gateway.hostname}`) with a property. In the Gateway configuration, you can configure property defaults such as `gateway.hostname`, `gateway.base.port`, `gateway.extras.port`.

About Domain Names
----------------------------------------
Many examples in the documentation use the `.net domain` (such as `tcp://gateway.example.net:8080`) to indicate internal, nonpublic URLs, and use the `.com` domain to indicate public URLs. All domains and URLs are for example purposes only. Simply replace any instances of "example.com" in the configuration with your domain or hostname.

### See Also

-   [Service Reference](../admin-reference/r_configure_gateway_service.md) for information about specifying URLs with the accept and connect elements.
-   Wikipedia description of [URI Scheme](http://en.wikipedia.org/wiki/URI_scheme)
-   [Configuring Multiple Services on the Same Host and Port](../admin-reference/c_configure_gateway_multiple_services.md)

About Ports
------------------------------------

The following table lists ports that are commonly used in the documentation.

| Port Number | Description                                                                                                                                |
|-------------|--------------------------------------------------------------------------------------------------------------------------------------------|
| 21-22       | Default shell and secure shell ports                                                                                                       |
| 80          | Default HTTP and WS port                                                                                                                   |
| 88          | Default Kerberos port                                                                                                                      |
| 443         | Default HTTPS and WSS port                                                                                                                 |
| 1080        | Recommended port for the socks:// transport                                                                                                |
| 1443        | Recommended port for the socks+ssl:// transport                                                                                            |
| 2020        | Default JMX Management Service port                                                                                                        |
| 5222        | Default XMPP port                                                                                                                          |
| 5672        | Default AMQP port                                                                                                                          |
| 7222        | Default TIBCO Enterprise Message Service™ port                                                                                             |
| 8000-8001   | Default Gateway WebSocket Service and echo service port, and for the SNMP Management Service in a single Gateway             |
| 8080-8081   | Default Gateway clustered WebSocket Service and echo service ports, and for the SNMP Management Service in a Gateway cluster |
| 8161        | Default Apache ActiveMQ Admin console                                                                                                      |
| 54327       | Recommended cluster multicast (UDP) port - Hazelcast port number)                                                                          |
| 61613       | Default Apache ActiveMQ JMS ports                                                                                                          |
| 61616-61617 | Default Apache ActiveMQ TCP and SSL ports                                                                                                  |
| 61222       | Default Apache ActiveMQ XMPP port                                                                                                          |

About KAAZING_HOME
---------------------------------------------

By default, when you install or upgrade KAAZING Gateway, the `KAAZING_HOME` directory is created. This top-level directory contains the KAAZING Gateway directory (referred to as `GATEWAY_HOME`)   and Gateway components. The value of `GATEWAY_HOME` depends on the operating system. See [About GATEWAY_HOME](#about-gateway_home) to learn more about Gateway directory destinations.

This documentation assumes you are running the Gateway from the default location. You may override the default and install KAAZING Gateway into a directory of your choice.

About GATEWAY_HOME
---------------------------------------------

This is the directory that contains KAAZING Gateway and its components. The default Gateway home is represented in the documentation as `GATEWAY_HOME` because the actual directory destination depends on your operating system and the method you use to install the Gateway:

-   If you download from [kaazing.org](http://kaazing.org) and unpack the Gateway using the standalone method, then you can unpack the download into a directory of your choice (for example, `C:\kaazing` or `/home/username/kaazing`).
-   If you fork the Gateway GitHub repository (find links to repos at [kaazing.org](http://kaazing.org)), then you can clone to the directory of your choice.

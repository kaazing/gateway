-   [Home](../../index.md)
-   [Documentation](../index.md)
-   Documentation Conventions

Documentation Conventions ${enterprise.logo.jms}
================================================

This topic contains the following sections:
-   [Text Conventions](#docconventions)
-   [About URI Syntax](#urlsyntax)
-   [About Ports](#aboutports)
-   [About KAAZING\_HOME](#kaazinghome)
-   [About GATEWAY\_HOME](#gatewayhome)

<a name="docconventions"></a>Text Conventions
---------------------------------------------

The following text conventions are used in the ${gateway.name.long} topics:

| Convention    | Meaning                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                      |
|---------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| ${*variable*} | A variable within the dollar-sign and curly brace format specifies a property default value in ${the.gateway} configuration file, for example `GATEWAY_HOME/conf/gateway-config.xml`. Property default values are propagated throughout the configuration when ${the.gateway} starts. For example, you could specify `gateway.example.com` as the default value for the `${gateway.hostname}` property. Variables shown in italics but without the dollar-sign and curly brace format indicate placeholder variables for which you supply particular values. |
| **bold**      | Bold typeface indicates graphical user interface elements associated with an action, or terms defined in text or the glossary. It also indicates your location in a series of steps such as in the "Before You Begin" section in any procedural topic.                                                                                                                                                                                                                                                                                                       |
| *italic*      | Italic type indicates book titles, emphasis, or placeholder variables for which you supply particular values.                                                                                                                                                                                                                                                                                                                                                                                                                                                |
| `monospace`   | Monospace type indicates commands within a paragraph, URLs, code in examples, text that appears on the screen, or text that you enter.                                                                                                                                                                                                                                                                                                                                                                                                                       |
| .             
  .             
  .             | A vertical ellipsis indicates that a portion of an example that would normally be present is not shown.                                                                                                                                                                                                                                                                                                                                                                                                                                                      |

<a name="urlsyntax"></a>About URI Syntax
----------------------------------------

${the.gateway.cap} documentation uses a generic URI scheme that has the following components:

`scheme://host:port/path`
For example, the following URI schemes are used in the documentation:

`tcp://${gateway.hostname}:${gateway.extras.port}`

`ws://gateway.example.com:80/`

`wss://localhost:9000/echo`

`sse://localhost:8000/sse`

-   The **scheme** describes how to connect and is sometimes referred to as the protocol. When specifying URIs in a ${gateway.cap} configuration, you can use `tcp://{hostname}:{port}` to make a basic TCP connection, or specify any of the supported schemes such as http, https, ws, wss, sse, and so on. See the [supported URI schemes](../admin-reference/r_conf_service.md#note_supportedURLschemes) for the complete list.
-   The **host** specifies where to connect and can be a hostname or domain name, or an IP address.
-   The **port** specifies the port number to ask for. This portion of the URI scheme is optional if you are using a default port, such as port 80 for http or port 443 for https. For example, when using the http scheme you do not need to specify port 80.
-   The **path** refers to the path of the resource. At a minimum you must specify the root path (`/`). Thus, `http://example.com/` is a legal address, but `http://example.com` is not, even though in practice the final slash "`/`” is added automatically.

In addition, you can append a query string to the URL to provide non-hierarchical information. The query string follows a question mark (?) appended to the URL. For example, you can use this query string when configuring security for your ${gateway.cap} (as described in the [Security Reference](../admin-reference/r_conf_security.md)).

**Note:** In the example URLs, the `${gateway.hostname}` syntax allows you to define property values once and then the values are propagated throughout the configuration when ${the.gateway} starts. You can replace any value in the configuration file that uses the dollar-sign and curly brace format (such as `${gateway.hostname}`) with a property. In ${the.gateway} configuration, you can configure property defaults such as `gateway.hostname`, `gateway.base.port`, `gateway.extras.port`.
### See Also

-   [Service Reference](../admin-reference/r_conf_service.md) for information about specifying URLs with the accept and connect elements.
-   Wikipedia description of [URI Scheme](http://en.wikipedia.org/wiki/URI_scheme)
-   [Configuring Multiple Services on the Same Host and Port](../admin-reference/c_conf_multipleservices.md#configmultsrvcs)

<a name="aboutports"></a>About Ports
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
| 8000-8001   | Default ${gateway.cap} WebSocket Service and echo service port, and for the SNMP Management Service in a single ${gateway.cap}             |
| 8080-8081   | Default ${gateway.cap} clustered WebSocket Service and echo service ports, and for the SNMP Management Service in a ${gateway.cap} cluster |
| 8161        | Default Apache ActiveMQ Admin console                                                                                                      |
| 54327       | Recommended cluster multicast (UDP) port - Hazelcast port number)                                                                          |
| 61613       | Default Apache ActiveMQ JMS ports                                                                                                          |
| 61616-61617 | Default Apache ActiveMQ TCP and SSL ports                                                                                                  |
| 61222       | Default Apache ActiveMQ XMPP port                                                                                                          |

<a name="kaazinghome"></a>About KAAZING\_HOME
---------------------------------------------

By default, when you install or upgrade ${gateway.name.long}, the ${kaazing.home} directory is created. This top-level directory contains the ${gateway.name.long} directory (referred to as *GATEWAY\_HOME*) ${upgrade.broker.home} and ${gateway.cap} components. The value of *GATEWAY\_HOME* depends on the operating system. See [About GATEWAY\_HOME](#gatewayhome) to learn more about ${gateway.cap} directory destinations.

This documentation assumes you are running ${the.gateway} from the default location. You may override the default and install ${gateway.name.long} into a directory of your choice.

<a name="gatewayhome"></a>About GATEWAY\_HOME
---------------------------------------------

This is the directory that contains ${gateway.name.long} and its components. The default ${gateway.cap} home is represented in the documentation as `GATEWAY_HOME` because the actual directory destination depends on your operating system and the method you use to install ${the.gateway}:

-   If you download from [kaazing.org](http://kaazing.org) and unpack ${the.gateway} using the standalone method, then you can unpack the download into a directory of your choice (for example, `C:\kaazing` or `/home/username/kaazing`).
-   If you fork ${the.gateway} GitHub repository from [kaazing.org](http://kaazing.org), then you can clone to the directory of your choice.



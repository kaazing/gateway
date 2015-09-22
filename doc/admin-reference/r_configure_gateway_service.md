Service Reference
========================================

This document describes all of the elements and properties you can use to configure KAAZING Gateway services.

Overview
----------------------------------

You can use the optional `service` element to configure one or more services running on KAAZING Gateway.

Structure
------------------------------------

The Gateway configuration file (`gateway-config.xml` or `gateway-config.xml`) defines the `service` configuration element and its subordinate elements and properties that are contained in the top-level `gateway-config` element:

-   [gateway-config](r_configure_gateway_gwconfig.md)
    -   [service](#service)
        -   [name](#service)
        -   [description](#service)
        -   [accept](#accept)
        -   [connect](#connect)
        -   [balance](#balance)
        -   [type](#type)
            -   [balancer](#balancer)
            -   [broadcast](#broadcast)
                -   accept
            -   [directory](#directory)
                -   directory
                -   options
                -   welcome-file
                -   error-pages-directory
            -   [echo](#echo)
            -   [management.jmx](#managementjmx)
            -   [management.snmp](#managementsnmp)
            -   [kerberos5.proxy](#kerberos5proxy) ![This feature is available in KAAZING Gateway - Enterprise Edition](../images/enterprise-feature.png) (deprecated)
            -   [proxy](#proxy-amqpproxy-and-jmsproxy)
                -   maximum.pending.bytes
                -   maximum.recovery.interval
                -   prepared.connection.count
            -   [amqp.proxy](#proxy-amqpproxy-and-jmsproxy)
                -   maximum.pending.bytes
                -   maximum.recovery.interval
                -   prepared.connection.count
                -   virtual.host
            -   [redis](../brokers/p_integrate_redis.md) ![This feature is available in KAAZING Gateway - Enterprise Edition](../images/enterprise-feature.png)git
            -   [jms](../admin-reference/r_conf_jms.md) ![This feature is available in KAAZING Gateway - Enterprise Edition](../images/enterprise-feature.png)
            -   [jms.proxy](../admin-reference/r_conf_jms.md) ![This feature is available in KAAZING Gateway - Enterprise Edition](../images/enterprise-feature.png)
            -   [http.proxy](#httpproxy)
        -   [properties](#properties)
        -   [accept-options and connect-options](#accept-options-and-connect-options)
            -   [*protocol*.bind](#protocolbind), where *protocol* can be ws, wss, http, https, socks, ssl, tcp, or udp
            -   [*protocol*.transport](#protocoltransport), where *protocol* can be pipe, tcp, ssl, or http
            -   [ws.maximum.message.size](#wsmaximummessagesize)
            -   [http.keepalive](r_configure_gateway_service.md#httpkeepalive)
            -   [http.keepalive.connections](r_configure_gateway_service.md#httpkeepaliveconnections)
            -   [http.keepalive.timeout](#httpkeepalivetimeout)
            -   [ssl.ciphers](#sslciphers)
            -   [ssl.protocols](#sslprotocols-and-sockssslprotocols)
            -   [ssl.encryption](#sslencryption)
            -   [ssl.verify-client](#sslverify-client)
            -   [socks.mode](#socksmode) ![This feature is available in KAAZING Gateway - Enterprise Edition](../images/enterprise-feature.png)
            -   [socks.ssl.ciphers](#sockssslciphers) ![This feature is available in KAAZING Gateway - Enterprise Edition](../images/enterprise-feature.png)
            -   [socks.ssl.protocols](#sslprotocols-and-sockssslprotocols) ![This feature is available in KAAZING Gateway - Enterprise Edition](../images/enterprise-feature.png)
            -   [socks.ssl.verify-client](#sockssslverify-client) ![This feature is available in KAAZING Gateway - Enterprise Edition](../images/enterprise-feature.png)
            -   [socks.retry.maximum.interval](#socksretrymaximuminterval) ![This feature is available in KAAZING Gateway - Enterprise Edition](../images/enterprise-feature.png)
            -   [tcp.maximum.outbound.rate](#tcpmaximumoutboundrate) ![This feature is available in KAAZING Gateway - Enterprise Edition](../images/enterprise-feature.png)
            -   [ws.inactivity.timeout](#wsinactivitytimeout)
            -   [http.server.header](#httpserverheader)
        -   [realm-name](#realm-name)
        -   [authorization-constraint](#authorization-constraint)
            -   require-role
            -   require-valid-user
        -   [mime-mapping](#mime-mapping)
            -   extension
            -   mime-type
        -   [cross-site-constraint](#cross-site-constraint)
            -   allow-origin
            -   allow-methods
            -   allow-headers
            -   maximum-age

service
-------------------------------------

Each `service` can contain any of the subordinate elements listed in the following table.

**Note:** Subordinate elements must be specified in the order shown in the following table.

| Subordinate Element      | Description                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     |
|:-------------------------|:--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| name                     | The name of the service. You must provide a name for the service. The `name` element can be any name.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                           |
| description              | A description of the service is optional.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       |
| accept                   | The URLs on which the service accepts connections.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                              |
| connect                  | The URL of a back-end service or message broker to which the proxy service or [broadcast](#broadcast) service connects.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                         |
| balance                  | The URI that is balanced by a `balancer` service. See [balancer](#balancer) service for details.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                |
| type                     | The type of service. One of the following: [balancer](#balancer), [broadcast](#broadcast), [directory](#directory), [echo](#echo), [kerberos5.proxy](#kerberos5proxy) (deprecated), [management.jmx](#managementjmx), [management.snmp](#managementsnmp), [proxy](#proxy-amqpproxy-and-jmsproxy), [amqp.proxy](#proxy-amqpproxy-and-jmsproxy), [redis](../brokers/p_integrate_redis.md) ![This feature is available in KAAZING Gateway - Enterprise Edition](../images/enterprise-feature.png),  [jms](../admin-reference/r_conf_jms.md)  ![This feature is available in KAAZING Gateway - Enterprise Edition](../images/enterprise-feature.png), [jms.proxy](../admin-reference/r_conf_jms.md) ![This feature is available in KAAZING Gateway - Enterprise Edition](../images/enterprise-feature.png), and [http.proxy](#httpproxy). |
| properties               | The service type-specific properties.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                           |
| accept-options           | Options for the `accept` element. See [accept-options](#accept-options-and-connect-options).                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    |
| connect-options          | Options for the `connect` element. See [connect-options](#accept-options-and-connect-options).                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                  |
| realm-name               | The name of the security realm used for authorization. If you do not include a realm name, then authentication and authorization are not enforced for the service.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                              |
| authorization-constraint | The user roles that are authorized to access the service. See [authorization-constraint](#authorization-constraint).                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            |
| mime-mapping             | Mappings of file extensions to MIME types. Each `mime-mapping` entry defines the HTTP Content-Type header value to be returned when a client or browser requests a file that ends with the specified extension. See [mime-mapping](#mime-mapping).                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                              |
| cross-site-constraint    | The cross-origin sites (and their methods and custom headers) allowed to access the service. See [cross-site-constraint](#cross-site-constraint).                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               |


### Supported URL Schemes:

When specifying URLs for the `accept` or `connect` elements, you can use `tcp://{hostname}:{port}` to make a basic TCP connection, or specify any of the supported protocol schemes:

-   ws
-   wss
-   http
-   https
-   ssl
-   tcp
-   udp

### accept

**Required?** Yes; **Occurs:** At least once.

The URLs on which the service accepts connections (see [Supported URL schemes](#supported-url-schemes)).

#### Example

``` xml
<service>
  <accept>ws://balancer.example.com:8081/echo</accept>
  .
  .
  .
</service>
```

#### Notes

-   There is no default value for the accept element. You must configure it and specify the URI on which the service accepts connections.
-   The Gateway is configured by default to provide services only to users on the same machine (`localhost`) as that on which it is running. To customize the Gateway for your environment, choose the appropriate type of service (for example, `balancer`, `broadcast`, `directory`, and so on) to signal the Gateway to accept incoming connections from clients using any supported URL scheme.
-   Supported URL Schemes:</b> When specifying URLs for the `accept` or `connect` elements, you can use `tcp://{hostname}:{port}` to make a basic TCP connection, or specify any of the supported protocol schemes:

    -   ws
    -   wss
    -   http
    -   https
    -   ssl
    -   tcp
    -   udp

### connect

**Required?** Yes; **Occurs:** At least once.

The URL of a back-end service or message broker to which the proxy service (for example, `proxy` or `amqp.proxy` service) or [broadcast](#broadcast) service connects (see [Supported URL schemes](#supported-url-schemes)).

#### Example

``` xml
<connect>tcp://192.0.2.11:5943</connect>
```

#### Notes

-   There is no default value for the `connect` element. You must configure it and specify the URI on which the service makes a connection to the back-end service or message broker.
-   The Gateway is configured by default to provide services only to users on the same machine (`localhost`) as that on which it is running. To customize the Gateway for your environment, choose the appropriate type of service (for example, `balancer`, `broadcast`, `echo`, and so on) to signal the Gateway to accept incoming connections from clients using any supported URL scheme.
-   Supported URL Schemes:</b> When specifying URLs for the `accept` or `connect` elements, you can use `tcp://{hostname}:{port}` to make a basic TCP connection, or specify any of the supported protocol schemes:

    -   ws
    -   wss
    -   http
    -   https
    -   ssl
    -   tcp
    -   udp

### balance

**Required?** Optional; **Occurs:** one or more.

A URI that matches the `accept` URI in a [`balancer`](#balancer) service. The `balance` element is added to a `service` in order for that `service` to be load balanced between cluster members.

#### Example

The following example shows a Gateway with a `balancer` service and an Echo service that contains a `balance` element. Note that the `accept` URI in the `balancer` service matches the `balance` URI in the Echo service.

**Balancer Service**

``` xml
<service>
  <accept>ws://balancer.example.com:8081/echo</accept>

  <type>balancer</type>

  <accept-options>
    <ws.bind>192.168.2.10:8081</ws.bind>
  </accept-options>

</service>
```

**Gateway Service Participating in Load Balancing**

``` xml
<service>
  <accept>ws://node1.example.com:8081/echo</accept>
  <balance>ws://balancer.example.com:8081/echo</balance>

  <type>echo</type>

  <cross-site-constraint>
    <allow-origin>http://directory.example.com:8080</allow-origin>
  </cross-site-constraint>
</service>
```

#### Notes

-   There is no default value for the balance element. If you configure it, then you must specify a URI for its value.
-   The `balance` and `accept` element URIs in a `service` must use the same port number and path. The hostnames in the URIs may be different.
-   See the [balancer](#balancer) service to configure the balancer.
-   See [Set Up KAAZING Gateway as a Load Balancer](../high-availability/p_high_availability_loadbalance.md) for a complete load balancing description and example.


### type

The type of service. For each service that you configure, you define any of the service types in the following table to customize the Gateway for your environment.

| Type of Service                                                                                                                                       | Description                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                  |
|:------------------------------------------------------------------------------------------------------------------------------------------------------|:---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| [balancer](#balancer)                                                                                                                                 | Configures load balancing using either the built-in load balancing features of the Gateway or a third-party load balancer. When you configure a balancer service, the Gateway balances load requests for any other Gateway service type. Services running on KAAZING Gateway support peer load balancer awareness with the balance element for a cluster of Gateways. See the [Configure the Gateway for High Availability](../high-availability/o_high_availability.md) topic that describes Gateway clusters and load balancing in detail. |
| [broadcast](#broadcast)                                                                                                                               | Configures the Gateway to accept connections initiated by the back-end server or broker and broadcast (or relay) messages that are sent along that connection to clients.                                                                                                                                                                                                                                                                                                                                                                    |
| [directory](#directory)                                                                                                                               | Specifies the directory path of your static files relative to *GATEWAY_HOME*/web, where *GATEWAY_HOME* is the directory where you installed KAAZING Gateway. **Note:** An absolute path cannot be specified.                                                                                                                                                                                                                                                                                                                                 |
| [echo](#echo)                                                                                                                                         | Receives a string of characters through a WebSocket and returns the same characters to the sender. The service echoes any input. This service is used primarily for validating the basic Gateway configuration. The echo service runs a separate port to verify cross-origin access.                                                                                                                                                                                                                                                         |
| [kerberos5.proxy](#kerberos5proxy) (deprecated)             | Connects the Gateway to the Kerberos Key Distribution Center.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                |
| [management.jmx](#managementjmx)                                                                                                                      | Track and monitors user sessions and configuration data using JMX Managed Beans.                                                                                                                                                                                                                                                                                                                                                                                                                                                             |
| [management.snmp](#managementsnmp)                                                                                                                    | Monitors a Gateway or a Gateway cluster through Command Center, which is a browser-based application. Using Command Center is the recommended method for monitoring the Gateway. The `management.snmp` service is enabled by default in the Gateway configuration file.                                                                                                                                                                                                                                                                      |
| [amqp.proxy](#proxy-amqpproxy-and-jmsproxy)                                                                                                           | Enables the use of the Advanced Message Queuing Protocol (AMQP) that is an open standard for messaging middleware and was originally designed by the financial services industry to provide an interoperable protocol for managing the flow of enterprise messages. To guarantee messaging interoperability, AMQP defines both a wire-level protocol and a model, the AMQP Model, of messaging capabilities. An example of a message broker that provides built-in support for AMQP is RabbitMQ.                                             |
| [proxy](#proxy-amqpproxy-and-jmsproxy)                                                                                                                | Enables a client to make a WebSocket connection to a back-end server or broker that cannot natively accept WebSocket connections.                                                                                                                                                                                                                                                                                                                                                                                                            |
| [redis](../brokers/p_integrate_redis.md) ![This feature is available in KAAZING Gateway - Enterprise Edition](../images/enterprise-feature.png)       | Integrates KAAZING Gateway and Redis, an open source, BSD licensed, advanced key-value cache and store. KAAZING Gateway includes a Redis service and integrated Redis driver for publishing and subscribing to Redis topics.                                                                                                                                                                                                                                                                                                                 |
| [jms](../admin-reference/r_conf_jms.md)  ![This feature is available in KAAZING Gateway - Enterprise Edition](../images/enterprise-feature.png)       | Uses the `jms` service, which allows you to configure the Gateway to connect to any back-end JMS-compliant message broker. The `jms` service offloads connections and topic subscriptions using a single connection between the Gateway and your JMS-compliant message broker.                                                                                                                                                                                                                                                               |
| [jms.proxy](../admin-reference/r_conf_jms.md)  ![This feature is available in KAAZING Gateway - Enterprise Edition](../images/enterprise-feature.png) | Establishes a connection between the Gateway and the next Gateway for each client connection. The benefit of using the `jms.proxy` service is that you can control security independently per connection, and enable a fail-fast when a user fails to authenticate correctly. In addition, delta messages can be passed through from `jms` service in the internal Gateway through a DMZ Gateway that is running the `jms.proxy` service in Enterprise Shield™ configurations.                                                               |
| [http.proxy](#httpproxy)                                                                                                                              | Enables a Gateway to serve both WebSocket traffic and proxy HTTP traffic on the same port (for example, port 80 or 443). The `http.proxy` service is used primarily to enable the Gateway to proxy both HTTP traffic plus other services (for example, WebSocket-based services alongside proxy services and AMQP services) to an HTTP server.                                                                                                                                                                                               |

### balancer

Use the `balancer` service to balance load for requests for any other Gateway service type.

#### Example

The following example shows a Gateway with a `balancer` service and an Echo service that contains a `balance` element. Note that the `accept` URI in the `balancer` service matches the `balance` URI in the Echo service.

**Balancer Service**

``` xml
<service>
  <accept>ws://balancer.example.com:8081/echo</accept>

  <type>balancer</type>

  <accept-options>
    <ws.bind>192.168.2.10:8081</ws.bind>
  </accept-options>

  <cross-site-constraint>
    <allow-origin>http://directory.example.com:8080</allow-origin>
  </cross-site-constraint>
</service>
```

**Gateway Service Participating in Load Balancing**

``` xml
<service>
  <accept>ws://node1.example.com:8081/echo</accept>
  <balance>ws://balancer.example.com:8081/echo</balance>

  <type>echo</type>

  <cross-site-constraint>
    <allow-origin>http://directory.example.com:8080</allow-origin>
  </cross-site-constraint>
</service>
```

#### Notes

-   When you configure the Gateway as a load balancer you specify `accept` elements that identify the URLs on which the balancer service listens for client requests. The `balancer` service is used to balance load for a cluster of Gateways.
-   As with all services, the `balancer` service needs to have appropriate cross-site constraints defined.
-   For more information about load balancing and using the `balancer` service, see the [Configure the Gateway for High Availability](../high-availability/o_high_availability.md) topic.

### broadcast

Use the `broadcast` service to relay information from a back-end service or message broker. The `broadcast` service has the following property:

| Property | Description                                                                              |
|:---------|:-----------------------------------------------------------------------------------------|
| `accept` | The URL of the broadcast service to which a back-end service or message broker connects. |

#### Examples

-   The following example configures the `broadcast` service with the `accept` element coded inside the `properties` element. This configures the Gateway to accept connections initiated by the back-end service or message broker and broadcast messages that are sent along that connection to clients.

``` xml
<service>
  <accept>sse://localhost:8000/sse</accept>
  <accept>sse+ssl://localhost:9000/sse</accept>

  <type>broadcast</type>

  <properties>
    <accept>udp://localhost:50505</accept>
  </properties>

  <cross-site-constraint>
    <allow-origin>http://localhost:8000</allow-origin>
  </cross-site-constraint>

  <cross-site-constraint>
    <allow-origin>https://localhost:9000</allow-origin>
  </cross-site-constraint>
</service>
```

-   The following example configures the `broadcast` service with a `connect` element that contains the URL of the back-end service or message broker (`news.example.com:50505`) to which the Gateway connects. If you configure the Gateway in this way, then the Gateway initiates the connection to the back-end service or message broker. When the service receives information, it broadcasts it to all the SSE channels accepted from clients on `www.example.com`.

``` xml
<service>
  <accept>sse://www.example.com:8000/sse</accept>
  <accept>sse+ssl://www.example.com:9000/sse</accept>
  <connect>tcp://news.example.com:50505/</connect>
  <type>broadcast</type>

  <cross-site-constraint>
    <allow-origin>http://www.example.com:8000</allow-origin>
  </cross-site-constraint>
  <cross-site-constraint>
    <allow-origin>https://www.example.com:9000</allow-origin>
  </cross-site-constraint>
</service>
```
-   For an example showing how to configure a `broadcast` service that uses a multicast address, see [Configure the Gateway to Use Multicast](p_configure_multicast.md).

### directory

Use the `directory` service to expose directories or files hosted on the Gateway. The `directory` service has the following properties:

**Note:** The properties must be specified in the order shown in the following table.

| Property                | Required or Optional? | Description                                                                                                                                                                                                                                                                                                                                              |
|:------------------------|:----------------------|:---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `directory`             | Required              | The path to the directory to be exposed on the Gateway.                                                                                                                                                                                                                                                                                                  |
| `options`               | Optional              | Enables directory browsing of the files and folders in the location specified by the `directory` property. The value `indexes` must be entered. For example, `<options>indexes</options>` enables directory browsing. Omitting the `options` property disables directory browsing. Browsing a directory with `welcome-file` will serve the welcome file. |
| `welcome-file`          | Optional              | The path to the file to be exposed on the Gateway.                                                                                                                                                                                                                                                                                                       |
| `error-pages-directory` | Optional              | The path to the directory containing the `404.md` file. By default, the Gateway includes a `404.md` file in `GATEWAY_HOME/error-pages`. See the Notes for more information.                                                                                                                                                                              |

#### Examples

-   The following is an example of a `service` element of type `directory` that accepts connections on localhost by default:

``` xml
<service>
  <accept>http://localhost:8000/</accept>
  <accept>https://localhost:9000/</accept>

  <type>directory</type>
  <properties>
    <directory>/</directory>
    <welcome-file>index.md</welcome-file>
    <error-pages-directory>/error-pages</error-pages-directory>
  </properties>
</service>
```

#### Notes

-   The path you specify for the `directory` service must be relative to the directory `GATEWAY_HOME\web` (where `GATEWAY_HOME` is the directory in which KAAZING Gateway is installed). For example, `C:\gateway\GATEWAY_HOME\web`. An absolute path cannot be specified.
-   KAAZING Gateway services are configured to accept connections on `localhost` by default. The cross-origin sites allowed to access those services are also configured for localhost-only by default. If you want to connect to host names other than localhost you must update your server configuration, and use the fully qualified host name of the host machine.
-   If you use the optional `error-pages-directory` property, you can test it by adding the property, saving the `gateway-config.xml` file, then starting the Gateway. Once the Gateway is running, point your browser to a page that does not exist, such as `http://localhost:8000/nonexistentpage.html`.

### echo

This type of service receives a string of characters through a WebSocket connection and returns, or *echoes* the same characters to the sender.

#### Example

``` xml
<service>
  <accept>ws://localhost:8001/echo</accept>
  <accept>wss://localhost:9001/echo</accept>

  <type>echo</type>

  <cross-site-constraint>
    <allow-origin>http://localhost:8000</allow-origin>
  </cross-site-constraint>

  <cross-site-constraint>
    <allow-origin>https://localhost:9000</allow-origin>
  </cross-site-constraint>
</service>
```

#### Notes

-   The primary use for the `echo` service is to validate the basic gateway configuration.
-   The default `echo` service is configured to run on a separate port to verify cross-origin access.

### kerberos5.proxy (deprecated)

Use the `kerberos5.proxy` service to connect the Gateway to the Kerberos Key Distribution Center.

### management.jmx

Use the `management.jmx` service type to track and monitor user sessions and configuration data using JMX Managed Beans.

#### Example

The following example is a snippet from the default Gateway configuration file showing the JMX Management section:

``` xml
<service>
  <name>JMX Management</name>
  <description>JMX Management Service</description>

  <type>management.jmx</type>

  <properties>
    <connector.server.address>jmx://${gateway.hostname}:2020/</connector.server.address>
  </properties>

  <!-- secure monitoring using a security realm -->
 <realm-name>demo</realm-name>

  <!-- configure the authorized user roles -->
  <authorization-constraint>
    <require-role>ADMINISTRATOR</require-role>
 </authorization-constraint>
</service>
```

#### Notes

-   See [Monitor the Gateway](../management/o_monitor.md) for an introduction to monitoring the Gateway.
-   See [Monitor with JMX](../management/p_monitor_jmx.md) for more information and examples of monitoring with JMX.
-   The `management.jmx` service is enabled by default in the Gateway configuration file.
-   KAAZING Gateway supports Java Management Extension (JMX) access through any JMX-compliant console that supports communication using the JMX protocol.

### management.snmp

Use the `management.snmp` service type to monitor a Gateway or a Gateway cluster through Command Center, which is a browser-based application. Using Command Center is the recommended method for monitoring the Gateway.

#### Example

The following example is a snippet from the default Gateway configuration file showing the SNMP Management section:

``` xml
<service>
  <name>SNMP Management</name>
  <description>SNMP Management Service</description>
  <accept>ws://${gateway.hostname}:${gateway.base.port}/snmp</accept>

  <type>management.snmp</type>

  <!-- secure monitoring using a security realm -->
  <realm-name>commandcenter</realm-name>

  <!-- configure the authorized user roles -->
  <authorization-constraint>
    <require-role>ADMINISTRATOR</require-role>
 </authorization-constraint>

  <cross-site-constraint>
    <allow-origin>*</allow-origin>
  </cross-site-constraint>
</service>
```

#### Notes

-   The `management.snmp` service is enabled by default in the Gateway configuration file. The `management.snmp` service is designed for use with Command Center.
-   See [Monitor the Gateway](../management/o_monitor.md) for an introduction to monitoring the Gateway.
-   See [Monitor with Command Center](../management/p_monitor_cc.md) for more information about monitoring with Command Center.


### proxy, amqp.proxy, and jms.proxy

Use the `proxy`, `amqp.proxy`, or `jms.proxy` service to enable a client to make a WebSocket connection to a back-end service or message broker that cannot natively accept WebSocket connections.

The following descriptions will help you understand when and how to configure properties for the `proxy` service and  `amqp.proxy` service. See the [jms.proxy](../admin-reference/r_conf_jms.md) ![This feature is available in KAAZING Gateway - Enterprise Edition](../images/enterprise-feature.png) reference for details about that feature.

#### <a name="maximumpendingbytes"></a>`maximum.pending.bytes`

**Required?** Optional

The size of data the service buffers for one client connection before slowing incoming data. The value must be a positive integer with either no specified unit or appended with *kB* or *MB* (the unit is case insensitive) to indicate kilobytes or megabytes. If no unit is specified, the default unit is bytes. If you do not specify this property, its default value is *64kB*. For example:
- A value of *2048* sets the buffer to **2048 bytes**.
- A value of *128kB* sets the buffer to **128 kilobytes**.
- No instance of the *maximum.pending.bytes* property in the *gateway-config.xml* sets the buffer to **64 kilobytes**.

The Gateway uses this buffer when the speed of the data coming into the service is faster than the speed of the data being consumed by the receiving end, which is either the client or the back-end service or message broker. The buffer stores the data up to the limit you specify in this property per client connection, then slows the incoming data, until either the client or the back-end service or message broker (whichever is consuming the data) has consumed more than 50% of the outgoing data flow.

For example, suppose you set this property to `128kB`. If the back-end service or message broker sends 256kB of data to a client and the client has only consumed 128kB, the remaining 128kB (the limit you set in the property) is buffered. At this time, the Gateway suspends reading the data from the back-end service or message broker; as the client consumes the buffered data, the size of the buffered data decreases. When the buffered data falls below 64kB, the Gateway resumes reading the data from the back-end service or message broker.

#### <a name="maximumrecoveryinterval"></a>`maximum.recovery.interval`

**Required?** Optional

The maximum interval (in seconds) between attempts of the service to establish a connection to the back-end service or message broker specified by the [`connect`](#connect) element.
- If specified, the value must be a non-negative integer. You must enable this property to ensure the client can reconnect to another service that has a good connection to an available back-end service or message broker.
    - If unspecified or if the value is set to `0`, then the property is disabled but the Gateway will start and run successfully. However, when the property is not enabled, the load balancer does not detect when a service is disconnected from the back-end service or message broker or when the broker is unavailable.

If the back-end service or message broker becomes unavailable or the Gateway cannot establish a connection to it, the Gateway triggers a recovery. The Gateway attempts to reestablish a connection to the back-end service or message broker. Initially, the interval between attempts is short, but grows until it reaches the specified value. From that point on, the Gateway attempts to reestablish a connection only at the interval specified.

During this recovery phase, the Gateway unbinds the service, and clients attempting to connect to this service receive a "404 Not Found" error. Once the back-end service or message broker recovers and the Gateway establishes a connection, the Gateway binds the service and clients can connect to the service. See the "Examples" section below the table for a code snippet using this property.

#### <a name="preparedconnectioncount"></a>`prepared.connection.count`

**Required?** Optional

Set this property in either of the following use cases:
- Set this property when configuring your proxy service, which is the most common use case for `prepared.connection.count`. In this case, setting `prepared.connection.count` sets the number of connections the Gateway creates (or *prepares*) to the back-end service or message broker specified by the [`connect`](#connect) element in addition to the client connections. When the Gateway starts, it creates the specified number of connections to the back-end service or message broker, thus creating a *prepared connection*. When an incoming client connection uses a prepared connection, the Gateway creates another connection to the back-end service or message broker, thus maintaining the specified number of prepared connections to the back-end service or message broker.
- Set this property when configuring Enterprise Shield™. See [Configure Enterprise Shield™](https://github.com/kaazing/enterprise.gateway/blob/develop/doc/enterprise-shield/p_enterprise_shield_config.md) for detailed configuration information. If you do not set this property, then the Gateway does not prepare connections to the back-end service or message broker.

#### <a name="virtualhost"></a>`virtual.host`

**Required?** Optional

Specifies the AMQP virtual host to which the Gateway can proxy clients that are connected to this service.

After the Gateway authenticates the client, the virtual host is injected into the AMQP protocol and messages can be exchanged. This ensures the target virtual host comes from a validated and trusted source such as the Gateway, rather than relying on what is set by the client, which can be manipulated.

You may choose to configure a virtual host when you want to:
- Isolate exchanges, queues, and topics created under one virtual-host from those in the other virtual hosts residing in the same AMQP broker instance.
- Ensure each client can only see their data and not the data of other clients.
- Handle a configuration in which your AMQP broker is part of a multitenant application and you do not want clients from one tenant sharing data and exchanges with clients from another tenant.

See the "Examples" section below this table for a code snippet using this property.

#### Examples

-   The following example of the `proxy` service configures the `accept` element to signal the Gateway to accept incoming connections from clients using WebSocket (`ws`) and WebSocket Secure (`wss`), and it configures the `connect` element to connect to the back-end service or message broker using TCP. The example uses the `proxy` service, which is common, but not required. See the [type](#type) element for a list of service types.
  The example also configures the following properties:
  - `maximum.pending.bytes` property, which sets the buffer to `128kB`.
  - `maximum.recovery.interval `property, which tells the Gateway to ping the service at `tcp://internal.example.com:port-number` every 30 seconds if the back-end service or message broker becomes unavailable.
  - `prepared.connection.count` property, which tells the Gateway to prepare 10 connections to the back-end service or message broker specified by the `connect` element, then maintain 10 connections as clients consume each connection.

``` xml
<service>
  <accept>ws://${gateway.hostname}:${gateway.base.port}/remoteService</accept>
  <accept>wss://${gateway.hostname}:${gateway.base.port}/remoteService</accept>
  <connect>tcp://internal.example.com:port-number</connect>

  <type>proxy</type>

  <properties>
    <maximum.pending.bytes>128kB</maximum.pending.bytes>
    <maximum.recovery.interval>30</maximum.recovery.interval>
    <prepared.connection.count>10</prepared.connection.count>
  </properties>

  <cross-site-constraint>
    <allow-origin>http://${gateway.hostname}:${gateway.base.port}</allow-origin>
  </cross-site-constraint>
  <cross-site-constraint>
    <allow-origin>https://${gateway.hostname}:${gateway.base.port}</allow-origin>
  </cross-site-constraint>
</service>
```

-   The following example shows a service configuration section that is used to accept connections for the `proxy` service at the URL `ws://www.example.com` on port `80`. (The port is included for the sake of example. You can omit the default ports 80 or 443.).

``` xml
<service>
  <accept>ws://www.example.com:80/service</accept>
  <connect>tcp://internal.example.com:port-number</connect>

  <type>proxy</type>

</service>
```

-   The following example configures two `amqp.proxy` services, `app1` and `app2`, and each service connects to a virtual host, `vhost1` and `vhost2`. Clients connecting via the `app1` service can access only `vhost1`, and clients on the `app2` service can access only `vhost2`.

``` xml
<!-- This service connects only to the AMQP virtual host vhost1. -->
<service>  
  <accept>ws://${gateway.hostname}:${gateway.port}/app1</accept>
  <connect>pipe://vhost1</connect>

  <type>amqp.proxy</type>

  <properties>
    <virtual.host>/vhost1</virtual.host>
  </properties>
</service>

  <!-- This service connects only to the AMQP virtual host vhost2.-->
<service>
  <accept>ws://${gateway.hostname}:${gateway.port}/app2</accept>
  <connect>pipe://vhost2</connect>

  <type>amqp.proxy</type>

  <properties>
    <virtual.host>/vhost2</virtual.host>
  </properties>
</service>

  <!-- Proxy service accepts on named pipes to connect to the AMQP broker.-->
<service>
  <accept>pipe://vhost1</accept>
  <accept>pipe://vhost2</accept>
  <connect>tcp://${gateway.hostname}:5672</connect>

  <type>proxy</type>

</service>
```

#### Notes

-   You must also configure the following elements:
    -   [`accept`](#accept): the URL at which the service accepts connections
    -   [`connect`](#connect): the URL of the service to which the Gateway connects
-   The WebSocket protocol used by the client connection is converteded into a protocol that you specify (for example, TCP) to connect to the back-end service or message broker. Upon client connection, the Gateway establishes a full-duplex connection between itself and the client, and between itself and the back-end service or message broker. The result is a full-duplex connection between the client and the back-end service or message broker.
-   The Gateway services are configured to accept connections on `localhost` by default. The cross-origin sites allowed to access those services are also configured for localhost by default. If you want to connect to host names other than `localhost`, then you must update your server configuration and use the fully qualified host name of the host machine, as shown in the example.
-   When there are multiple `amqp.proxy` services in the Gateway configuration that are connecting to the same AMQP broker instance, all AMQP proxy services should pipe their `connect` elements to a common service as shown in the previous configuration example. This is recommended due to a current restriction with JMX monitoring.
-   See the [Promote User Identity into the AMQP Protocol](../security/p_auth_user_identity_promotion.md) topic for more information about injecting AMQP credentials into the protocol in a trusted manner.

### http.proxy

Use the `http.proxy` service to enable a Gateway to serve both WebSocket traffic and proxy HTTP traffic on the same port (for example, port 80 or 443). The `http.proxy` service is used primarily to enable the Gateway to proxy HTTP traffic to an HTTP server while other services handle non-HTTP traffic at the same time on the same port. For example, you might proxy HTTP, AMQP, and WebSocket at the same time, all on the same port.

Typically, you use the `http.proxy` service to:

- Enable the Gateway to proxy both HTTP traffic and traffic from other services, allowing you to run more than one service on the same port.

    The Gateway’s `http.proxy` service acts as a reverse proxy. Its primary intent is to protect internal servers, allowing the Gateway to serve HTTP and WebSocket requests on the same host on the same port (such as port 80 and 443). If the Gateway is running on port 80, then a separate HTTP server cannot also bind to port 80 on the same network interface. Conversely, if Apache or Tomcat, for example, are bound to port 80, then the Gateway cannot listen on port 80.

- Enable you to close ports in the firewall for applications serving HTTP requests in Enterprise Shield topologies.

   Using `http.proxy` with Enterprise Shield lets you make REST (Representational State Transfer) requests while still keeping inbound firewall ports closed. With the ability to proxy HTTP, the Gateway can protect not only WebSocket traffic, but all HTTP traffic in an enterprise architecture.

   For example, typically, HTTP requests occur when your application uses KAAZING Gateway to stream data, and uses REST for upstream requests. With the ability to proxy HTTP, your REST requests can go through Enterprise Shield, letting you keep ports closed for REST requests.

Consider configuring the [`http.keepalive.connections`](#httpkeepaliveconnections) in  `connect-options` to specify a maximum number of idle keep-alive connections to upstream servers.

#### Examples

**Example 1: http.proxy Service - Basic Use Case running both Gateway and Apache on one machine.**

The following example configures the accept URI with a public HTTP server address and configures the connect URI as a private server IP address. The example configuration specifies two accept URIs:

```
http://www.websocket.org
http://websocket.org
```

The configuration example that follows requires DNS to resolve [www.websocket.org](http://www.websocket.org) and [websocket.org](http://www.websocket.org) to the IP address of the Gateway. Inbound requests are then proxied to a Web server (such as the Apache or Tomcat). Notice also that the connect URI proxies to the private IP address of the back-end server. The following example specifies one connection is kept alive until it is reused or timed out.

``` xml
<service>
  <name>http-proxy</name>
  <description>Http Proxy to websocket.org</description>

  <accept>http://www.websocket.org/</accept>
  <accept>http://websocket.org/</accept>
  <connect>http://174.129.224.73/</connect>

  <type>http.proxy</type>

  <connect-options>
    <http.keepalive.connections>1</http.keepalive.connections>
  </connect-options>  

  …

</service>
```

### properties

The service's type-specific properties.

#### Example

``` xml
<service>
  <accept>http://${gateway.hostname}:${gateway.extras.port}/</accept>

  <type>directory</type>

  <properties>
    <directory>/</directory>
    <welcome-file>index.md</welcome-file>
  </properties>
    .
    .
    .
</service>
```

#### Notes

-   The `properties` subelement to used to configure properties that are specific to each service.
-   See also the `property` subelement that you specify in the Property Defaults section of the Gateway configuration file to define property values once and that are then propagated throughout the configuration when the Gateway starts. See the [Configure KAAZING Gateway](p_configure_gateway_files.md) topic for more information about configuring the property defaults section of the Gateway configuration file.

### accept-options and connect-options

**Required?** Optional; **Occurs:** zero or one

Use the `accept-options` element to add options to all accepts for the service (see also the [`accept`](#accept) element).

You can configure `accept-options` on the [service](#service) or the [service-defaults](r_configure_gateway_service_defaults.md) elements:

-   Setting `accept-options` on the `service` element only affects the specified service and overrides any defaults.
-   Setting `accept-options` on `service-defaults` affects all services that can use that option and is overridden by any `accept-options` applied at the service level.

Use the `connect-options` element to add options to all connections for the service (see also the `connect` element).

-   Setting `connect-options` can only be done for the *service* element; You cannot configure `connect-options` at the [service-defaults](r_configure_gateway_service_defaults.md) level.
-   Setting `connect-options` affects only the specified service and override any defaults.

| Option                                                                                                                               | accept-options | connect-options | Description                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               |
|:-------------------------------------------------------------------------------------------------------------------------------------|:---------------|:----------------|:--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| *protocol*.bind                                                                                                                      | yes            | no              | Binds the URL(s) on which the service accepts connections (defined bythe accept element). Set *protocol* to one of the following: ws, wss, http, https, ssl, tcp,udp. See [*protocol*.bind](#protocolbind).                                                                                                                                                                                                                                                                                                                                                                                                               |
| *protocol*.transport                                                                                                                 | yes            | yes             | Specifies the URI for use as a transport layer (defined by the accept element). Set *protocol*.transport to any of the following: http.transport, ssl.transport, tcp.transport, pipe.transport. See [*protocol*.transport](#protocoltransport).                                                                                                                                                                                                                                                                                                                                                                           |
| ws.maximum.message.size                                                                                                              | yes            | no              | Specifies the maximum incoming WebSocket message size allowed by the Gateway. See [ws.maximum.message.size](#wsmaximummessagesize).                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       |
| http.keepalive                                                                                                                       | no             | yes             | Enables or disables HTTP keep-alive (persistent) connections, allowing you to reuse the same TCP connection for multiple HTTP requests or responses. This improves HTTP performance especially for services like [`http proxy`](#httpproxy). `http.keepalive` is enabled by default. See [http.keepalive](#httpkeepalive).                                                                                                                                                                                                                                                                                                |
| http.keepalive.connections                                                                                                           | no             | yes             | Specifies the maximum number of idle keep-alive connections to upstream servers that can be cached. The connections time out based on the setting for the `http.keepalive.timeout` configuration option.  See [http.keepalive.connections](#httpkeepaliveconnections).                                                                                                                                                                                                                                                                                                                                                    |
| http.keepalive.timeout                                                                                                               | yes            | yes             | Specifies how much time the Gateway waits after responding to an HTTP or HTTPS request and receiving a subsequent request. See [http.keepalive.timeout](#httpkeepalivetimeout).                                                                                                                                                                                                                                                                                                                                                                                                                                           |
| ssl.ciphers                                                                                                                          | yes            | yes             | Lists the cipher strings and cipher suite names used by the secure connection. See [ssl.ciphers](#sslciphers).                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            |
| ssl.protocols                                                                                                                        | yes            | yes             | Lists the TLS/SSL protocol names on which the Gateway can accept connections. See [ssl.protocols and socks.ssl.protocols](#sslprotocols-and-sockssslprotocols).                                                                                                                                                                                                                                                                                                                                                                                                                                                           |
| ssl.encryption                                                                                                                       | yes            | yes             | Signals KAAZING Gateway to enable or disable encryption on incoming traffic.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                              |
| ssl.verify-client                                                                                                                    | yes            | no              | Signals KAAZING Gateway to require a client to provide a digital certificate that the Gateway can use to verify the client’s identity.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    |
| socks.mode ![This feature is available in KAAZING Gateway - EnterpriseEdition.](../images/enterprise-feature.png)                    | yes            | yes             | The mode that you can optionally set to forward or reverse to tell the Gateway how to interpret SOCKS URIs to initiate the connection. See [socks.mode](#socksmode).                                                                                                                                                                                                                                                                                                                                                                                                                                                      |
| socks.timeout ![This feature is available in KAAZING Gateway -Enterprise Edition.](../images/enterprise-feature.png)                 | no             | yes             | Specifies the length of time (in seconds) to wait for SOCKS connectionsto form. If the connection does not succeed within the specified time, then the connection fails and is closed and the client must reconnect. For more information, see [socks.timeout](#sockstimeout).                                                                                                                                                                                                                                                                                                                                            |
| socks.ssl.ciphers ![This feature is available in KAAZING Gateway - Enterprise Edition.](../images/enterprise-feature.png)            | yes            | yes             | Lists the cipher strings and cipher suite names used by the secure SOCKS connection.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                      |
| socks.ssl.protocols ![This feature is available in KAAZING Gateway - Enterprise Edition.](../images/enterprise-feature.png)          | yes            | yes             | Lists the TLS/SSL protocol names on which the Gateway can accept connections for Enterprise Shield™ configurations that are running the SOCKS protocol over SSL. See [ssl.protocols and socks.ssl.protocols](#sslprotocols-and-sockssslprotocols).                                                                                                                                                                                                                                                                                                                                                                        |
| socks.ssl.verify-client ![This feature is available in KAAZING Gateway - Enterprise Edition.](../images/enterprise-feature.png)      | yes            | yes             | A connect mode you can set to required, optional, or none to verify how to secure the SOCKS proxy against unauthorized use by forcing the use of TLS/SSL connections with a particular certificate. When required,the DMZ Gateway expects the internal Gateway to prove its trustworthiness by presenting certificates during the TLS/SSL handshake.                                                                                                                                                                                                                                                                      |
| socks.retry.maximum.interval ![This feature is available in KAAZING Gateway - Enterprise Edition.](../images/enterprise-feature.png) | yes            | no              | The maximum interval the Gateway waits before retrying if an attempt toconnect to the SOCKS proxy fails. The Gateway initially retries afterwaiting for 500ms; the subsequent wait intervals are as follows: 1s, 2s, 4s, and so on up to the value of socks.retry.maximum.interval. After the maximum interval is reached, the Gateway continues to reconnect to the SOCKS proxy at the maximum interval.                                                                                                                                                                                                                 |
| tcp.maximum.outbound.rate ![This feature is available in KAAZING Gateway- Enterprise Edition.](../images/enterprise-feature.png)     | yes            | no              | Specifies the maximum bandwidth rate at which bytes can be written from the Gateway (outbound) to each client session. This option controls the rate of outbound traffic being sent per client connection for clients connecting to a service (see [tcp.maximum.outbound.rate](#tcpmaximumoutboundrate)).                                                                                                                                                                                                                                                                                                                 |
| ws.inactivity.timeout                                                                                                                | yes            | yes             | Specifies the maximum number of seconds that the network connection can be inactive (seconds is the default time interval syntax). The Gateway drops the connection if it cannot communicate with the client in the number of seconds specified (see [ws.inactivity.timeout](#wsinactivitytimeout)). You can specify your preferred time interval syntax in milliseconds, seconds, minutes, or hours (spelled out or abbreviated). For example, all of the following are valid: 1800s, 1800sec, 1800 secs, 1800 seconds, 1800seconds, 3m, 3min, or 3 minutes. If you do not specify a time unit then seconds are assumed. |
| http.server.header                                                                                                                   | yes            | no              | Controls the inclusion of the HTTP Server header. By default, the Gateway writes a HTTP Server header. See [http.server.header](#httpserverheader).                                                                                                                                                                                                                                                                                                                                                                                                                                                                       |
| ws.version (deprecated)                                                                                                              | no             | yes             | The `ws.version` element has been deprecated.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                             |

#### *protocol*.bind

**Required?** Optional; **Occurs:** zero or more; **Where** `protocol` can be ws, wss, http, https, socks, ssl, tcp, or udp

Use the `protocol.bind` element to configure network protocol bindings for your Gateway services. Configure `protocol.bind` as an accept-option or a connect-option to bind a URI or URIs on which the Gateway can accept or make connections. The Gateway binds the URI or port or IP address specified in the `protocol.bind` element to bind the public URI in the `accept` or `connect` element to the URI or port or IP address

Specify any of the following protocol schemes:

-   `ws`: Specifies the WebSocket (WS) protocol.
-   `wss`: Specifies the WebSocket Secure (WSS) protocol, which is the secure WS protocol.
-   `http`: Specifies the Hypertext Transfer Protocol (HTTP).
-   `https`: Specifies the HTTP Secure (HTTPS), which is the secure HTTP protocol.
-   `socks`: Specifies the SOCKet Secure (SOCKS) protocol.
-   `ssl`: Specifies the Transport Layer Security (TLS), previously known as Secure Sockets Layer (SSL).
-   `tcp`: Specifies the Transmission Control Protocol (TCP).
-   `udp`: Specifies the User Datagram Protocol (UDP).

See the [Configure the Gateway on an Internal Network](../internal-network/p_protocol_binding.md) document for more information about configuring the `protocol.bind` element.

##### Example: Binding to Specific Ports

The following example shows external addresses (that users will see) for the WebSocket (`ws`) and WebSocket Secure (`wss`) protocols on `localhost:8000` and `localhost:9000`. Internally, however, these addresses are bound to ports 8001 and 9001 respectively.

``` xml
<service>
  <name>Echo Config</name>
  <accept>ws://localhost:8000/echo</accept>
  <accept>wss://localhost:9000/echo</accept>

  <type>echo</type>

  <accept-options>
    <ws.bind>8001</ws.bind>
    <wss.bind>9001</wss.bind>
  </accept-options>
</service>
```

##### Example: Binding a Public URI to IP Addresses in a Cluster Configuration

In the following example, the `ws.bind` and `wss.bind`elements in accept-options are used to bind the public URI in the `accept` elements to the local IP address of the cluster member. This allows the accept URIs in the balancer service to be identical on every cluster member. Only the `ws.bind` element needs to be unique in each cluster member (contain the local IP address of that cluster member).

``` xml
<service>
  <accept>ws://balancer.example.com:8081/echo</accept>
  <accept>wss://balancer.example.com:9091/echo</accept>

  <type>balancer</type>

  <accept-options>
    <ws.bind>192.168.2.10:8081</ws.bind>
    <wss.bind>192.168.2.10:9091</wss.bind>
  </accept-options>
</service>
```

#### *protocol*.transport

**Required?** Optional; **Occurs:** zero or more; **Where** `protocol` can be http, ssl, tcp, pipe, and socks.

Use the `protocol.transport` accept-option or connect-option to replace the default transport with a new transport. This allows you to change the behavior of the connection without affecting the protocol stack above the transport. For example, a TCP transport normally connects to a remote IP address and port number. However, you could replace that, for instance, with an in-memory (pipe) transport that communicates with another service in the same Gateway.

Specify any of the following transports:

-   `http.transport`: Specifies a URI for use as a transport layer under the HTTP transport or WebSocket transport (because  WebSocket is always over HTTP). This is the most frequently used transport option.
-   `ssl.transport`: Specifies a URI for use as a transport layer under the TLS/SSL transport.
-   `tcp.transport`: Specifies a URI for use as a transport layer under the TCP/IP (tcp) transport.
-   `pipe.transport`: Specifies a URI for use as a transport layer under the pipe transport.
-   `socks.transport`: Specifies a URI for use as a transport layer under the SOCKS transport.

##### Example: Configuring the Transport in accept-options

In the following example, the HTTP transport is replaced with a new (`socks+ssl`) transport that is capable of doing a reverse connection using the SOCKS protocol over TLS/SSL.

``` xml
<service>
   <accept>wss://gateway.example.com:443/path</accept>
 <connect>tcp://internal.example.com:1080</connect>
    .
    .
    .
  <accept-options>
    <http.transport>socks+ssl://gateway.dmz.net:1080</http.transport>
    <socks.mode>reverse</socks.mode>
    <socks.retry.maximum.interval>1 second</socks.retry.maximum.interval>
  </accept-options>
</service>
```

##### Example: Configuring the Transport in connect-options

In the following example, the `socks+ssl` transport performs a reverse connection using the SOCKS protocol over TLS/SSL.

``` xml
<service>
  <accept>wss://gateway.example.com:443/path</accept>
  <connect>wss://gateway.example.com:443/path</connect>
     .
     .
     .
  <connect-options>
    <http.transport>socks+ssl://gateway.dmz.net:1080</http.transport>
    <socks.mode>reverse</socks.mode>
    <socks.timeout>2 seconds</socks.timeout>
    <ssl.verify-client>required</ssl.verify-client>
  </connect-options>
</service>
```

For more examples, see [Configure Enterprise Shield™ with the Gateway](https://github.com/kaazing/enterprise.gateway/blob/develop/doc/enterprise-shield/o_enterprise_shield_checklist.md).

#### ws.maximum.message.size

**Required?** Optional; **Occurs:** zero or one

Configures the maximum message size the service can accept from a WebSocket client connection.

Although the `ws.maximum.message.size` is optional, you should configure this element to keep clients from accidentally or deliberately causing the Gateway to spend resources processing large messages. Setting this element is useful in preventing *denial of service* attacks because you can limit the size of the message (such as a particularly large message) incoming to the Gateway from a client.

The actual maximum message size that the Gateway can handle is influenced by the JVM settings (such as maximum heap size), available memory on the system, network resources, available disk space and other operating system resources. The maximum message size is also influenced by the configuration and capabilities of back-end services to which the Gateway might be forwarding these messages. The best way to determine the true maximum message size for your environment and use case is to perform some testing.

If you do not specify `ws.maximum.message.size` in the gateway-config.xml file, then the default maximum incoming message is limited to `128k`.

If you specify `ws.maximum.message.size` in the gateway-config.xml file, then specify a positive integer. You can append a `k`, `K`, `m`, or `M` to indicate kilobytes or megabytes (the unit is case insensitive). If a unit is not included, then `ws.maximum.message.size` defaults to bytes. For example:

-   A value of `2048` sets the buffer to **2048 bytes**
-   A value of `128K` sets the buffer to **128 kilobytes**
-   A value of `128M` sets the buffer to **128 megabytes**

If an incoming message from a client exceeds the value of `ws.maximum.message.size`, then the Gateway terminates the connection with the client and disconnects the client, and records a message in the Gateway log.

##### Example

The following example sets a maximum incoming message limit of 64 kilobytes:

``` xml
<service>
  <accept>ws://localhost:8000/echo</accept>
  <accept>wss://localhost:9000/echo</accept>
  <accept-options>
    <ssl.encryption>disabled</ssl.encryption>
    <ws.bind>8001</ws.bind>
    <wss.bind>9001</wss.bind>
    <ws.maximum.message.size>64k</ws.maximum.message.size>
  </accept-options>
</service>
```

#### http.keepalive

**Required?** Optional; **Occurs:** zero or one

Use the `http.keepalive` element in connect-options to enable or disable HTTP keep-alive (persistent) connections, allowing you to reuse the same TCP connection for multiple HTTP requests or responses. This improves HTTP performance especially for services like [`http proxy`](#httpproxy). `http.keepalive` is enabled by default.  Consider configuring this element in conjunction with the [http.proxy](#httpproxy) element.

##### Example

``` xml
<service>
   . . .
  <accept>http://example.com:8000/</accept>
  <connect>http://internal.example.com:7233/</connect>

  <type>http.proxy</type>

  <connect-options>
    <http.keepalive>disabled</http.keepalive>
  </connect-options>
   . . .
</service>
```

#### http.keepalive.connections

**Required?** Optional; **Occurs:** zero or one

Use the `http.keepalive.connections` element in connect-options to specify the maximum number of idle keep-alive connections to upstream servers to upstream servers that can be cached. This element is often used in conjunction with the [http.proxy](#httpproxy) element.

The connection times out based on the setting for the [`http.keepalive.timeout`](#httpkeepalivetimeout) configuration option. The best practice is to specify a value small enough to allow upstream servers to process new incoming connections as well. The following example specifies one connection is cached in worker until it is reused or timed out.

##### Example

``` xml
<service>
   . . .
  <accept>http://example.com:8000/</accept>
  <connect>http://internal.example.com:7233/</connect>

  <type>http.proxy</type>

  <connect-options>
    <http.keepalive.connections>1</http.keepalive.connections>
  </connect-options>
   . . .
</service>
```

#### http.keepalive.timeout

**Required?** Optional; **Occurs:** zero or one

Use the `http.keepalive.timeout` element in either accept-options or connect-options to set the number of seconds the Gateway waits after responding to a request and receiving a subsequent request on an HTTP or HTTPS connection before closing the connection. The default value is `30` seconds.

Typically, you specify the `http.keepalive.timeout` element to conserve resources because it avoids idle connections remaining open. You can specify your preferred time interval syntax in milliseconds, seconds, minutes, or hours (spelled out or abbreviated). For example, all of the following are valid: 1800s, 1800sec, 1800 secs, 1800 seconds, 1800seconds, 3m, 3min, or 3 minutes. If you do not specify a time unit then seconds are assumed.

##### Example

The following example shows a `service` element with an HTTP or HTTPS connection time limit of `120` seconds:

``` xml
<service>
  <accept>ws://localhost:8000/echo</accept>
  <accept>wss://localhost:9000/echo</accept>
  . . .
  <accept-options>
    <http.keepalive.timeout>120 seconds</http.keepalive.timeout>
  </accept-options>
</service>
```

#### ssl.ciphers

**Required?** Optional; **Occurs:** zero or one; **Values:** cipher strings and cipher suite names for [OPENSSL](http://www.openssl.org/docs/apps/ciphers.html#CIPHER_STRINGS) and [Java 7](http://docs.oracle.com/javase/7/docs/technotes/guides/security/SunProviders.html#SunJSSEProvider).

Use `ssl.ciphers` to list the encryption algorithms used by TLS/SSL on the secure connection (WSS, HTTPS or SSL). By default (or if you do not specify this element on a secure connection), the Gateway uses `HIGH,MEDIUM,!ADH,!KRB5`.

##### Examples

-   The following example shows a `proxy` service accepting a secure connection over WSS and connecting to the back-end service or message broker over TCP with the ciphers used by the encryption algorithm specified in `ssl.ciphers`. The example uses the `proxy` service, which is common, but not required. See the [type](#type) element for a list of service types.

    ``` xml
    <service>
      <accept>wss://www.example.com:443/remoteService</accept>
      <connect>tcp://localhost:6163</connect>

      <type>proxy</type>

      <accept-options>
        <ssl.ciphers>DEFAULT</ssl.ciphers>
      </accept-options>

      <connect-options>
        <ssl.ciphers>LOW</ssl.ciphers>
      </connect-options>
    </service>
    ```

    By default (or if you do not specify `ssl.ciphers` on a secure connection), the Gateway uses the equivalent of the following ciphers:

    ``` xml
    <ssl.ciphers>HIGH,MEDIUM,!ADH,!KRB5</ssl.ciphers>
    ```

    -   `HIGH` and `MEDIUM` aliases are included because they do not contain any weak (LOW or EXPORT) cipher suites.
    -   `ADH` (Anonymous Diffie-Hellman) cipher suites are excluded because they are not secure against man-in-the-middle attacks.

        To support DSA certificates, you must add `ADH` to the `ssl.ciphers` element as follows: `<ssl.ciphers>HIGH,MEDIUM,ADH</ssl.ciphers>`. Do not use `ADH` with `DEFAULT`. DSA certificates are not recommended. See [Diffie-Hellman key exchange](http://en.wikipedia.org/wiki/Diffie%E2%80%93Hellman_key_exchange#Security) for more information. If you omit the `-keyalg` switch when you create a certificate using keytool, keytool generates a DSA certificate. You should always include `-keyalg RSA` when creating a certificate using keytool.

    -   `KRB5` (Kerberos) cipher suites are excluded because most sites do not have the related Kerberose libraries installed for supporting those cipher suites.

-   **NULL cipher suite**

    Use the `NULL` cipher suite when you want the Gateway to accept connections from known authenticated TLS clients but you do not want those connections to be encrypted:

    ``` xml
    <ssl.ciphers>NULL</ssl.ciphers>
    ```

    This is analogous to configuring an HTTP service for authentication using `authorization-constraint`, only now you are doing it for a TLS/SSL service.

-   **ALL cipher suite**

    Use the `ALL` cipher suite to enable all of the cipher suites (such as for testing purposes):

    ``` xml
    <ssl.ciphers>ALL</ssl.ciphers>
    ```

    Note that because NULL encryption cipher suites are so insecure they are not enabled even by using the `ALL` cipher suite. You must explicitly include NULL ciphers by configuring them, as follows:

    ``` xml
    <ssl.ciphers>HIGH,MEDIUM,LOW,EXPORT,NULL</ssl.ciphers>
    ```

##### Notes

-   The [OpenSSL Project documentation](http://www.openssl.org/docs/apps/ciphers.html#CIPHER_STRINGS) lists all permitted OpenSSL cipher strings. The Gateway supports ciphers that are implemented by Java, as described in the [Java Secure Socket Extension (JSSE) documentation.](http://docs.oracle.com/javase/7/docs/technotes/guides/security/SunProviders.html#SunJSSEProvider)
-   For configurations running KAAZING Gateway 4.0.5 or earlier releases, you can disable the SSLv3 protocol by configuring SSLv3 ciphers using `<ssl.ciphers>!SSLv3<ssl.ciphers>`.
-   Values for cipher strings are case-sensitive.
-   Typos or incorrect strings (or unsupported ciphers) in `ssl.ciphers` are noticed by the Gateway when a connection is made, not when the Gateway is started. These errors are only discoverable by looking at the Gateway log.
-   The `ssl.ciphers` property does not configure the ciphers used on a secure connection. It merely specified the ciphers used in the TLS/SSL certificate used to establish the secure connection.
-   For secure SOCKS connections, use [socks.ssl.ciphers](#sockssslciphers).
-   TLS/SSL is used to verify the Gateway to the client. To use TLS/SSL to verify the client using the connection, use `ssl.verify-client`.
-   Two or more services can have TLS/SSL `accept` elements with the same address and port (for example, one service might accept on `wss://example.com:9000/echo` and another service might accept on `https://example.com:9000/directory`). If `accept` elements listening on the same address and port number are also configured with the `ssl.ciphers` accept option, the values for `ssl.ciphers` must be identical.

#### ssl.protocols and socks.ssl.protocols

**Required?** Optional; **Occurs:** zero or one; **Values:** SSLv2Hello, SSLv3, TLSv1, TLSv1.1, TLSv1.2

Specify a comma-separated list of the TLS/SSL protocol names on which the Gateway can accept or make connections. The list of protocols you specify are negotiated during the TLS/SSL handshake when the connection is created. See [How TLS/SSL Works with the Gateway](../security/u_tls_works.md) to learn more about secure communication between clients and the Gateway. See the [Java Documentation](http://docs.oracle.com/javase/8/docs/technotes/guides/security/StandardNames.html#SSLContext) for a list of valid protocol names.

The `ssl.protocols` and `socks.ssl.protocols` elements are optional, and in general, there is no need to configure either element except to prevent usage of specific TLS/SSL protocols for which a vulnerability has been discovered. A good example is the [POODLE attack](http://en.wikipedia.org/wiki/POODLE) that exploited a vulnerability in SSLv3.

If you configure these elements, then you must explicitly name the TLS/SSL protocols you want to enable. If you do not configure the `ssl.protocols` or `socks.ssl.protocols` element, or you configure either element but do not specify any protocols, then the default value is taken from the underlying JVM. The protocol values are case-sensitive.

Typically, you configure the `ssl.protocols` or `socks.ssl.protocols` in the `accept-options` for inbound requests from clients. You might also specify these elements in the `connect-options` for an Enterprise Shield™ configuration, although this is less common because Gateway-to-Gateway communication usually occurs in a controlled environment and the TLS/SSL protocol you use is controlled. The `ssl.protocols` and `socks.ssl.protocols` elements are more useful in the `accept-options` when accepting requests from clients that are not in your direct control.

**Note:** These elements were introduced in KAAZING Gateway release 4.0.6 and can be used for configurations running KAAZING Gateway 4.0.6 or later releases. For configurations running KAAZING Gateway 4.0.5 or earlier releases, you can disable the SSLv3 protocol by disabling SSLv3 ciphers with `><ssl.ciphers>!SSLv3</ssl.ciphers>`. See [ssl.ciphers](#sslciphers) for more information.

If you configure the `ssl.protocols` or the `socks.ssl.protocols` element to enable SSLv3, but disable SSLv3 cipher suites with the `ssl.ciphers` or `socks.ssl.ciphers` elements, then the connection does not occur and the Gateway will not accept SSLv3 connections. Similarly, if you enable TLSv1 with the `ssl.protocols` or the `socks.ssl.protocols` element, but disable the TLSv1 ciphers, then the handshake will not succeed and the connection cannot go through.

##### Example: Simple Configuration Using ssl.protocols to Accept TLSv1, TLSv1.2, and TLSv1.1 Connections

The following example shows a `proxy` service. Because the `accept` URL the `wss://` scheme, we know that this is a secure connection. The `ssl.protocols` element in the following example indicates that we want the Gateway to accept only TLSv1, TLSv1.2, and TLSv1.1 protocols, say, from clients over this secure connection.

``` xml
<service>
  <name>DMZ Gateway</name>
  <accept>wss://example.com:443/myapp</accept>
    ...
  <type>proxy</type>

  <properties>
    ...
  </properties>

  <accept-options>
    <ssl.protocols>TLSv1,TLSv1.2,TLSv1.1</ssl.protocols>
    ...
  </accept-options>

</service>
```

##### Example: Enterprise Shield™ Configuration Using socks.ssl.protocols to Accept Reverse Connections on TLSv1.2 ![This feature is available in KAAZING Gateway - Enterprise Edition](../images/enterprise-feature.png)

This example shows a `proxy` service in the DMZ configured for Enterprise Shield™, for which the <connect> behavior is reversed. Instead of connecting to another host, the Gateway accepts connections instead. Thus, the setting is configured as `connect-options` in this example. For more information about Enterprise Shield™ and forward and reverse connectivity, see [Configure Enterprise Shield™ for KAAZING Gateway](https://github.com/kaazing/enterprise.gateway/blob/develop/doc/enterprise-shield/o_enterprise_shield_checklist.md).

Because this configuration connects a Gateway to another Gateway in a controlled data center, the example only configures the TLSv1.2 protocol for secure connections. For this type of topology we don't expect to make any other kinds of connections.

The prefix for this example is `socks.ssl`, rather than just `ssl` to explicitly reference the SSL layer that is transporting the SOCKS protocol.

``` xml
<service>
  <name>DMZ Gateway</name>
  <accept>wss://example.com:443/myapp</accept>
  <connect>wss://example.com:443/myapp</connect>
    ...
  <type>proxy</type>

  <properties>
    ...
  </properties>

  <connect-options>
    <http.transport>socks://internal.example.com:1080</http.transport>
    <socks.mode>reverse</socks.mode>
    <socks.transport>ssl://internal.example.com:1080</socks.transport>
    <socks.ssl.protocols>TLSv1.2</socks.ssl.protocols>
  </connect-options>
     ...
</service>
```

##### Example: Enterprise Shield™ Configuration Using ssl.protocols and socks.ssl.protocols ![This feature is available in KAAZING Gateway - Enterprise Edition](../images/enterprise-feature.png)

This example combines the previous two examples to show an Enterprise Shield™ configuration in which `ssl.protocols` is specified in the accept-options, and `socks.ssl.protocols` is specified in the connect-options.

On the frontplane, the Gateway accepts connections from clients only using the TLSv1, TLSv1.2, and TLSv1.1 protocols. On the backplane, the Gateway only accepts (reverse) connections using the protocol TLSv1.2 (from another Gateway).

``` xml
<service>
  <name>DMZ Gateway</name>
  <accept>wss://example.com:443/myapp</accept>
  <connect>wss://example.com:443/myapp</connect>
    ...
  <type>proxy</type>

  <properties>
    ...
  </properties>

  <accept-options>
    <ssl.protocols>TLSv1,TLSv1.2,TLSv1.1</ssl.protocols>
    ...
  </accept-options>

  <connect-options>
    <http.transport>socks://internal.example.com:1080</http.transport>
    <socks.mode>reverse</socks.mode>
    <socks.transport>ssl://internal.example.com:1080</socks.transport>
    <socks.ssl.protocols>TLSv1.2</socks.ssl.protocols>
  </connect-options>
     ...

</service>
```

#### ssl.encryption

**Required?** Optional; **Occurs:** zero or one; **Values:** enabled, disabled

This element allows you to enable or disable TLS/SSL encryption on incoming traffic to the Gateway, turning off TLS/SSL certificate verification for an HTTPS or WSS accept. By default (or if you do not specify this element), encryption is enabled for HTTPS and WSS.

For example, if the Gateway is deployed behind a TLS/SSL offloader (a network device designed specifically for handling a company's TLS/SSL certificate traffic), where the incoming traffic to the TLS/SSL offloader is secured over HTTPS and the outgoing traffic from the TLS/SSL offloader to the Gateway is *not* secure, you can disable encryption so that the Gateway accepts the unsecured traffic on a connection that uses HTTPS/WSS. Basically, the Gateway trusts traffic from the TLS/SSL offloader and therefore the Gateway does not need to verify the connection itself.

You can include the [accept-options](#accept-options-and-connect-options) element on a service that accepts over HTTPS or WSS, then disable encryption by setting the `ssl.encryption` element to `disabled`. Even when encryption is disabled, the Gateway returns the response as HTTPS/WSS. If you do not include these elements or set the `ssl.encryption` element to `enabled`, the Gateway treats incoming traffic on HTTPS or WSS as secure and handles the TLS/SSL certificate verification itself.

See [Secure Network Traffic with the Gateway](../security/o_tls.md) for more information about HTTPS/WSS.

##### Example: Using ssl.encrption in accept-options

The following example shows a `service` element containing the `accept-options` and `ssl.encryption` elements, which signal the Gateway to listen on address `www.example.com`, with encryption disabled. The example uses the `proxy` service, which is common, but not required. See the [type](#type) element for a list of service types.

``` xml
<service>
  <accept>wss://www.example.com/remoteService</accept>
  <connect>tcp://localhost:6163</connect>

  <type>proxy</type>
   .
   .
   .
  <accept-options>
    <ssl.encryption>disabled</ssl.encryption>
  </accept-options>
</service>

```

Alternatively, the IP address can be used in the configuration parameters. You can also specify an IP address and port for the external address. Typically when you disable encryption on the incoming traffic, as the Gateway is behind a TLS/SSL offloader, you will also have a network mapping section mapping `www.example.com` to internal address `gateway.dmz.net:9000`.

##### Example: Using ssl.encrption in connect-options

The following example for an Enterprise Shield™ topology shows a `service` element containing several `connect-options` including an `ssl.encryption` option that disables encryption.

``` xml
<service>
  <accept>wss://dmz.example.com:443/remoteService</accept>
  <connect>tcp://internal.example.com:8010</connect>

  <type>proxy</type>

  <properties>
    <prepared.connection.count>1</prepared.connection.count>
  </properties>

  <accept-options>
    <ssl.ciphers>DEFAULT</ssl.ciphers>
    <ssl.verify-client>none</ssl.verify-client>
  </accept-options>

  <connect-options>
    <tcp.transport>socks+ssl://dmz.example.com:1443</tcp.transport>
    <socks.mode>reverse</socks.mode>
    <socks.ssl.ciphers>NULL</socks.ssl.ciphers>
    <ssl.encryption>disabled</ssl.encryption>
    <socks.ssl.verify-client>required</socks.ssl.verify-client>
  </connect-options>
</service>
```

##### Notes

-   If you have set up KAAZING Gateway behind a TLS/SSL offloader, where the front-end traffic is secure over HTTPS and the back-end traffic behind the TLS/SSL offloader to the Gateway is *not* secure, then you can disable encryption so that the connection can occur. You can include the [accept-options](#accept-options-and-connect-options) element, then disable encryption by setting the `ssl.encryption` element to `disabled`. When encryption is disabled, the Gateway returns the response as HTTPS. If you do not include these elements or set the `ssl.encryption` element to `enabled`, the Gateway treats incoming traffic on `www.example.com:443` as secure and handles the TLS/SSL itself.
-   See [Secure Network Traffic with the Gateway](../security/o_tls.md) for more information about HTTPS.

#### ssl.verify-client

**Required?** Optional; **Occurs:** zero or one; **Values:** required, optional, none

By default, when the Gateway accepts a secure URI (for example, WSS, HTTPS, SSL), the Gateway provides its digital certificate to connecting clients but does not require that the clients provide a certificate of their own — the Gateway trusts all clients. For added security, implement a mutual verification pattern where, in addition to the Gateway presenting a certificate to the client, the client also presents a certificate to the Gateway so that the Gateway can verify the client's authenticity.

To configure that, you can use the `ssl.verify-client` on an `accept` to specify that the Gateway requires a client to provide a digital certificate that the Gateway can use to verify the client’s identity. This configuration ensures that both the clients and the Gateway are verified via TLS/SSL before transmitting data, establishing a mutually-verified connection.

| If you configure the ssl.verify-client option with the value ... | Then ...                                                                                                                                                                                                                                                                                                            |
|:-----------------------------------------------------------------|:--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `required`                                                       | A client certificate is required. The Gateway requires that the client connecting to the Gateway over the secure URI in the `accept` must provide a digital certificate to verify the client’s identity. After the Gateway has verified the client certificate, then the client can connect to the Gateway service. |
| `optional`                                                       | The client certificate is not required, but if a client provides a certificate, the Gateway attempts to verify it. If the client provides a certificate and verification fails, then the client is not allowed to connect.                                                                                          |
| `none`                                                           | The client recognizes that a certificate is not required and it does not send a certificate. All clients can connect to the secure service on the Gateway.                                                                                                                                                          |

##### Example

In the following example, the Gateway accepts on a secure URI (`wss://`) and requires that all clients connecting to the Gateway on that URI provide a digital certificate verifying their identity.

``` xml
<service>
  <accept>wss://example.com:443</accept>
  <connect>tcp://server1.corp.example.com:5050</connect>

  <type>proxy</type>

  <accept-options>
    <ssl.verify-client>required</ssl.verify-client>
  </accept-options>
</service>
```

##### Notes

-   To use `ssl.verify-client` as an accept-option on a service, the service must be accepting on a secure URI (`wss://`, `https://`, `ssl://`). You cannot use `ssl.verify-client` on a unsecured URI (`ws://`, `http://`, `tcp://`, `udp://`).
-   If you have set up KAAZING Gateway behind a TLS/SSL offloader, where the front-end traffic is secure over HTTPS and the back-end traffic behind the TLS/SSL offloader to the Gateway is *not* secure, then you can disable encryption so that the connection can occur. You can include the [accept-options](#accept-options-and-connect-options) element, then disable encryption by setting the `ssl.encryption` element to `disabled`. When encryption is disabled, the Gateway returns the response as HTTPS. If you do not include these elements or set the `ssl.encryption` element to `enabled`, the Gateway treats incoming traffic on `www.example.com:443` as secure and handles the TLS/SSL itself.
-   See [Secure Network Traffic with the Gateway](../security/o_tls.md) for more information about HTTPS.
-   This configuration ensures that both the clients and the Gateway are verified via TLS/SSL before transmitting data, establishing mutual verification. A best practice is to use mutual verification between gateways that are located at different sites. Each gateway can require that the other gateway provide a certificate, thereby ensuring that the connection is secure.
-   This configuration ensures that both the clients and the Gateway are verified via TLS/SSL before transmitting data, establishing mutual verification. A best practice is to use mutual verification between gateways that are located at different sites. Each gateway can require that the other gateway provide a certificate, thereby ensuring that the connection is secure.
-   Two or more services can have TLS/SSL `accept` elements with the same address and port (for example, one service might accept on `wss://example.com:9000/echo` and another service might accept on `https://example.com:9000/directory`). If `accept` elements listening on the same address and port number are also configured with the `ssl.verify-client` accept option, the values for `ssl.verify-client` must be identical.

#### socks.mode![This feature is available in KAAZING Gateway - Enterprise Edition](../images/enterprise-feature.png)

**Required?** Optional; **Occurs:** zero or one

Use the `socks.mode` in accept-options or connect-options to initiate the Gateway connection using the SOCKet Secure (SOCKS) protocol in one of the following modes:

-   `forward`: initiates forward connectivity from the DMZ Gateway to the internal Gateway on the trusted network. Once connected, a regular full-duplex connection is established. You typically use the `forward` mode to ensure the SOCKS settings are correctly configured before you attempt to reverse the connection.
-   `reverse`: configures the connection mode in *reverse* so that the connection is initiated from the internal Gateway on the trusted network to the DMZ Gateway to allow a connection between the client and server that is otherwise blocked by the firewall. With the reverse mode, the Gateway interprets `accept` URIs as `connect` URIs.

For more information about Enterprise Shield™ and forward and reverse connectivity, see [Configure Enterprise Shield™ with the Gateway](https://github.com/kaazing/enterprise.gateway/blob/develop/doc/enterprise-shield/o_enterprise_shield_checklist.md).

##### Example

The following example shows a `service` element with the `socks.mode` set to `reverse`. This configuration causes the Gateway to interpret the SOCKS URI as a connect URI:

``` xml
<service>
  <accept>pipe://pipe-1</accept>
  <connect>tcp://broker.example.com:8010/</connect>

  <type>proxy</type>

  <accept-options>
    <pipe.transport>socks+ssl://dmz.example.com:1443</pipe.transport>
    <socks.mode>reverse</socks.mode>
    <socks.retry.maximum.interval>45 seconds</socks.retry.maximum.interval>
  </accept-options>
</service>
```

##### Example

The following example shows a `connect-options` element with the `socks.mode` set to `reverse`.

``` xml
<service>
  <accept>tcp://dmz.example.com:8000/</accept>
  <connect>pipe://pipe-1</connect>

  <type>proxy</type>

  <connect-options>
    <pipe.transport>socks+ssl://dmz.example.com:1443</pipe.transport>
    <socks.mode>reverse</socks.mode>
  </connect-options>
</service>
```

#### socks.timeout![This feature is available in KAAZING Gateway - Enterprise Edition](../images/enterprise-feature.png)

**Required?** Optional; **Occurs:** zero or one

Use the `socks.timeout` connect-option to specify the length of time (in seconds) to wait for a SOCKS connection to form before closing the connection. If you do not specify `socks.timeout` for your Gateway configuration, then a timeout is not enforced.

Note the following behavior for reverse and forward SOCKS connections:

-   For *reverse* connections (`socks.mode` is set to *reverse*), the time you specify for `socks.timeout` determines how long pending connection requests on the DMZ Gateway wait for the internal Gateway to initiate a reverse connection.

    Connect requests are queued until the internal Gateway pulls the requests from the queue and consumes them. If the connect requests are not consumed within the specified time, then the connection to the client times out and is closed.

-   For *forward* connections (`socks.mode` is set to *forward*), the time you specify for `socks.timeout` determines how long to wait for confirmation that the connection succeeded.

    If the network connection or the SOCKS handshake does not succeed within the time specified by `socks.timeout`, then the connection to the client fails and the connection is closed.

##### Example

The following example shows a `socks.timeout` that is set to 10 seconds. If the forward connection is not formed within 10 seconds, then the connection is closed and the client must initiate another connection.

``` xml
<service>
  <accept>wss://www.example.com:443/remoteService</accept>
  <connect>tcp://localhost:6163</connect>

  <type>proxy</type>

  <accept-options>
    <ssl.ciphers>DEFAULT</ssl.ciphers>
    <ssl.verify-client>none</ssl.verify-client>
  </accept-options>

  <connect-options>
    <pipe.transport>socks+ssl://dmz.example.com:1443</pipe.transport>
    <socks.mode>reverse</socks.mode>
    <socks.timeout>10 sec</socks.timeout>
  </connect-options>
</service>
```

#### socks.ssl.ciphers![This feature is available in KAAZING Gateway - Enterprise Edition](../images/enterprise-feature.png)

**Required?** Optional; **Occurs:** zero or one; **Values:** cipher strings and cipher suite names for [OPENSSL](http://www.openssl.org/docs/apps/ciphers.html#CIPHER_STRINGS) and [Java 7](http://docs.oracle.com/javase/7/docs/technotes/guides/security/SunProviders.html#SunJSSEProvider).

Use `socks.ssl.ciphers` to list the encryption algorithms used by TLS/SSL on the secure connection (WSS, HTTPS or SSL). By default (or if you do not specify this element on a secure connection), the Gateway uses `HIGH,MEDIUM,!ADH,!KRB5`.

##### Example for SOCKS Ciphers

The following example shows a `proxy` service for the DMZ Gateway in an Enterprise Shield™ topology. The Gateway receives secure client connections (`wss://`) and specifies the ciphers used on the accept URI (`DEFAULT`), but does not require mutual verification from the clients (`ssl.verify-client`). In addition, the internal Gateway connects over SOCKS and TLS/SSL (`socks+ssl://`) to the DMZ Gateway, specifies the ciphers used (`NULL`), and requires mutual verification (`socks.ssl.verify-client`). For more information about forward and reverse connectivity, see [Configure Enterprise Shield™ with the Gateway](https://github.com/kaazing/enterprise.gateway/blob/develop/doc/enterprise-shield/o_enterprise_shield_checklist.md).

``` xml
<service>
  <accept>wss://dmz.example.com:443/remoteService</accept>
  <connect>tcp://internal.example.com:8000</connect>

  <type>proxy</type>

  <properties>
    <prepared.connection.count>1</prepared.connection.count>
  </properties>

  <accept-options>
    <ssl.ciphers>DEFAULT</ssl.ciphers>
    <ssl.verify-client>none</ssl.verify-client>
  </accept-options>

  <connect-options>
    <tcp.transport>socks+ssl://dmz.example.com:1443</tcp.transport>
    <socks.mode>reverse</socks.mode>
    <socks.ssl.ciphers>NULL</socks.ssl.ciphers>
    <socks.ssl.verify-client>required</socks.ssl.verify-client>
  </connect-options>
</service>
```

##### Notes

-   Values are case-sensitive.
-   The `socks.ssl.ciphers` property does not configure the ciphers used on a secure connection. It merely specified the ciphers used in the TLS/SSL certificate used to establish the secure connection.
-   OpenSSL aliases and names listed as *Not implemented* in the [OPENSSL](http://www.openssl.org/docs/apps/ciphers.html#CIPHER_STRINGS "OpenSSL: Documents, ciphers(1)") documentation are not supported by the Gateway.
-   Typos or incorrect strings (or unsupported ciphers) in `socks.ssl.ciphers` are noticed by the Gateway when a connection is made, not on startup. These errors are only discoverable by looking at the Gateway log.
-   TLS/SSL is used to verify the Gateway to the client. To use TLS/SSL to verify the client using the connection, use `ssl.verify-client`.

#### socks.ssl.verify-client![This feature is available in KAAZING Gateway - Enterprise Edition](../images/enterprise-feature.png)

**Required?** Optional; **Occurs:** zero or one; **Values:** required, optional, none

In an Enterprise Shield™ topology over `socks+ssl://`, the DMZ Gateway provides the internal Gateway with a digital certificate that the internal Gateway uses to verify the DMZ Gateway’s identity before establishing the secure connection. For added security, you can use the `socks.ssl.verify-client` option on the DMZ Gateway to require that the internal Gateway provide a digital certificate to establish a secure connection. This configuration ensures that both the DMZ Gateway and internal Gateway are verified via TLS/SSL before transmitting data, establishing mutual verification.

| If you configure the socks.ssl.verify-client option with the value ... | Then ...                                                                                                                                                                                                                                                                                                    |
|:-----------------------------------------------------------------------|:------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `required`                                                             | A certificate is required. The DMZ Gateway requires that the client connecting from the internal Gateway over the SOCKS transport must provide a digital certificate to verify the client’s identity. After the DMZ Gateway has verified the client certificate, then the reverse connection can be formed. |
| `optional`                                                             | A certificate is not required, but if a client provides a certificate then the DMZ Gateway attempts to verify it. If the verification fails, then the client is not allowed to connect.                                                                                                                     |
| `none`                                                                 | The client recognizes that a certificate is not required and it does not send a certificate. All clients can connect to the secure service on the DMZ Gateway.                                                                                                                                              |

For more information, see [Configure Enterprise Shield™ with the Gateway](https://github.com/kaazing/enterprise.gateway/blob/develop/doc/enterprise-shield/o_enterprise_shield_checklist.md).

##### Example

In the following example, the DMZ Gateway accepts on a WebSocket URI and connects over a named pipe. The DMZ Gateway also listens for connections on port 1443 as `pipe.transport` URI over SOCKS and TLS/SSL (`socks+ssl://`). To increase security, the `socks.ssl.verify-client` is set to `required`, which specifies that the internal Gateway URI must provide a digital certificate to the DMZ Gateway.

``` xml
<service>
  <accept>wss://dmz.example.com:443/remoteService</accept>
  <connect>pipe://pipe-1</connect>

  <type>proxy</type>

  <properties>
    <prepared.connection.count>1</prepared.connection.count>
  </properties>

  <accept-options>
    <ssl.ciphers>DEFAULT</ssl.ciphers>
    <ssl.verify-client>none</ssl.verify-client>
  </accept-options>

  <connect-options>
    <pipe.transport>socks+ssl://dmz.example.com:1443</pipe.transport>
    <socks.mode>reverse</socks.mode>
    <socks.ssl.ciphers>NULL</socks.ssl.ciphers>
    <socks.ssl.verify-client>required</socks.ssl.verify-client>
  </connect-options>
</service>
```

##### Notes

-   If you have set up KAAZING Gateway behind a TLS/SSL offloader, where the front-end traffic is secure over HTTPS and the back-end traffic behind the TLS/SSL offloader to the Gateway is *not* secure, then you can disable encryption so that the connection can occur. You can include the [accept-options](#accept-options-and-connect-options) element, then disable encryption by setting the `ssl.encryption` element to `disabled`. When encryption is disabled, the Gateway returns the response as HTTPS. If you do not include these elements or set the `ssl.encryption` element to `enabled`, the Gateway treats incoming traffic on `www.example.com:443` as secure and handles the TLS/SSL itself.
-   See [Secure Network Traffic with the Gateway](../security/o_tls.md) for more information about HTTPS.
-   See [Configure Enterprise Shield™ with the Gateway](https://github.com/kaazing/enterprise.gateway/blob/develop/doc/enterprise-shield/p_enterprise_shield_config.md) to learn how to require the internal Gateway to provide TLS/SSL certificates.</a>

#### socks.retry.maximum.interval![This feature is available in KAAZING Gateway - Enterprise Edition](../images/enterprise-feature.png)

**Required?** Optional; **Occurs:** zero or one

Use the `socks.retry.maximum.interval` accept-option in an Enterprise Shield™ topology to set the maximum interval of time that the internal Gateway waits to retry a reverse connection to the DMZ Gateway after a failed attempt. The internal Gateway initially retries after waiting for 500ms; the subsequent wait intervals are as follows: 1s, 2s, 4s, and so on up to the value of `socks.retry.maximum.interval`. Once the maximum interval is reached, the Gateway continues to reconnect to the SOCKS proxy at the maximum interval. If no maximum is specified, then the default retry interval is 30 seconds. For more information about configuring the SOCKS proxy, see [Configure Enterprise Shield™ with the Gateway](https://github.com/kaazing/enterprise.gateway/blob/develop/doc/enterprise-shield/o_enterprise_shield_checklist.md).

##### Example

The following example shows a `service` element containing a SOCKS proxy connection retry interval time limit of 60 seconds:

``` xml
<service>
  <accept>pipe://pipe-1</accept>
  <connect>tcp://broker.example.com:8010/</connect>

  <type>proxy</type>

  <accept-options>
    <pipe.transport>socks+ssl://dmz.example.com:1443</pipe.transport>
    <socks.mode>reverse</socks.mode>
    <socks.retry.maximum.interval>60 seconds</socks.retry.maximum.interval>
  </accept-options>
</service>
```

#### tcp.maximum.outbound.rate![This feature is available in KAAZING Gateway - Enterprise Edition](../images/enterprise-feature.png)

**Required?** Optional; **Occurs:** zero or one

Use the `tcp.maximum.outbound.rate` accept option to specify the maximum bandwidth rate at which bytes can be written from the Gateway to a client session. This option delays outbound messages as a way to control the maximum rate, per client session, at which the Gateway can send data to clients connecting to a service.

You must specify the value of `tcp.maximum.outbound.rate` as a positive integer with either no specified unit or appended with B/s (byte), kB/s (kilobyte), KiB/s (kibibyte), MB/s (megabyte), or MiB/s (Mebibytes) per second. (See the [NIST Reference](http://physics.nist.gov/cuu/Units/binary.html) for more information about these units.) Do not use spaces between the numeric portion and the units (for example, 40MB/s is supported but 40 MB/s is not supported).

You must specify the value of `tcp.maximum.outbound.rate` as a positive integer with either no specified unit or appended with a unit of measurement from the following table. (See the [NIST Reference](http://physics.nist.gov/cuu/Units/binary.html) for more information about these units.) Do not use spaces between the numeric portion and the units (for example, 40MB/s is supported but 40 MB/s is not supported).

| Unit                | Abbreviation | Bytes per Second per Unit | Notes                                                       |
|:--------------------|:-------------|:--------------------------|:------------------------------------------------------------|
| Byte per second     | B/s          | 1                         | Example: `512B/s`                                           |
| kilobyte per second | kB/s         | 1000 (10^3)               | Decimal kilobytes per second. Example: `1000kB/s`           |
| kilobyte per second | KiB/s        | 1024 (2^10)               | Kibibytes per second (kilobytes binary). Example: `1KiB/s`  |
| megabyte per second | MB/s         | 1,000,000 (10^6)          | Decimal megabytes per second. Example: `1MB/s`              |
| megabyte per second | MiB/s        | 1,048,576 (2^20)          | Mebibytes per second (megabytes binary) Example: `512MiB/s` |

##### Example

The following example shows a portion of a Gateway configuration file containing three services, each with a different bandwidth constraint: VIP, premium, and freemium. The VIP service has the best bandwidth at 1 megabyte per second (line 5). The premium service is slower at 1 kibibyte per second (line 13), and the free service is the slowest at only 512 bytes per second (line 21). The example shows these variations configured for the `[proxy](r_configure_gateway_service.md)` service, which is common, but not required. See the [type](#type) element for a list of service types.

``` xml
<service>
  <accept>ws://service.example.com/vip</accept>
  <type>proxy</type>
  <accept-options>
    <tcp.maximum.outbound.rate>1MB/s</tcp.maximum.outbound.rate>
  </accept-options>
</service>

<service>
  <accept>ws://service.example.com/premium</accept>
  <type>proxy</type>
  <accept-options>
    <tcp.maximum.outbound.rate>1KiB/s</tcp.maximum.outbound.rate>
  </accept-options>
</service>

<service>
  <accept>ws://service.example.com/freemium</accept>
  <type>proxy</type>
  <accept-options>
    <tcp.maximum.outbound.rate>512B/s</tcp.maximum.outbound.rate>
  </accept-options>
</service>
```

##### Notes

-   If no unit is specified, the default unit is in bytes per second (B/s).
-   If you do not specify `tcp.maximum.outbound.rate`, the bandwidth rate is unrestricted.
-   The Gateway follows the conventions for units defined in the Conversion formula table on the Wikipedia [Data rate units page](http://en.wikipedia.org/wiki/Data_rate_units#Kilobyte_per_second).

#### ws.inactivity.timeout

**Required?** Optional; **Occurs:** zero or one

Specifies the maximum number of seconds that the network connection can be inactive (seconds is the default time interval syntax). The Gateway will drop the connection if it cannot communicate with the client in the number of seconds specified (see [ws.inactivity.timeout](#wsinactivitytimeout)). You can specify your preferred time interval syntax in milliseconds, seconds, minutes, or hours (spelled out or abbreviated). For example, all of the following are valid: 1800s, 1800sec, 1800 secs, 1800 seconds, 1800seconds, 3m, 3min, or 3 minutes. If you do not specify a time unit then seconds are assumed. An inactive connection can result from a network failure (such as a lost cellular or Wi-Fi connection) that prevents network communication from being received on any established connection. Thus, when `ws.inactivity.timeout` is set to a nonzero time interval, the Gateway will drop the connection if it cannot communicate with the client in the number of seconds specified.

Some use cases for the `ws.inactivity.timeout` property include:

-   Detect a lost cellular (or Mobile) connection or a lost Wi-Fi connection.
-   Detect a half-closed connection over WebSocket, such as a silent network failure over WebSocket between Gateways.
-   Detect a network failure between the DMZ and Internal Gateway over WebSocket, such as for an Enterprise Shield™ topology using forward and reverse connectivity.

##### Example

In the following example, the `ws.inactivity.timeout` property specifies that if the Gateway cannot communicate with a client over the past five-seconds, then the connection to that client will be dropped.

``` xml
<service>
  <accept>ws://gateway.example.com/echo</accept>
  <connect>ws://internal.example.com/echo</connect>

  <type>echo</type>

 <accept-options>
   <ws.inactivity.timeout>5s</ws.inactivity.timeout>
 </accept-options>
   .
   .
   .
</service>
```

##### Notes

-   Set the time interval to a value that is at least double the expected maximum network round-trip time between the Gateway and any client. Otherwise, clients may be disconnected unexpectedly.
-   You can specify your preferred time interval syntax in milliseconds, seconds, minutes, or hours (spelled out or abbreviated). For example, all of the following are valid: 1800s, 1800sec, 1800 secs, 1800 seconds, 1800seconds, 3m, 3min, or 3 minutes. If you do not specify a time unit then seconds are assumed.

#### http.server.header

**Required?** Optional; **Occurs:** zero or more; **Values** `enabled`or `disabled`

Enables or disables the inclusion of the HTTP server header. By default, the Gateway writes a HTTP server header. In general, there is no need to configure this accept option unless you want to obscure server header information.

This setting is ignored for services that do not accept HTTP or WebSocket connections.

**Hint:** Instead of specifying this setting on every service, consider adding it using the [service-defaults](../admin-reference/r_configure_gateway_service_defaults.md) element to globally apply the setting across all services running on the Gateway.

##### Example

``` xml
<service>
  ...
  <accept-options>
    <http.server.header>disabled</http.server.header>
  </accept-options>
    ...
</service>
```

#### ws.version (deprecated)

**Required?** Optional; **Occurs:** zero or more; **Where** `version` can be rfc6455 or draft-75

The `ws.version` element has been deprecated. If you are using an existing configuration that includes the `ws.version` element, you can continue to use it. However, if the scheme of the URI inside the `connect` element is ws:// or wss://, then the WebSocket version defaults to rfc6455 and there is no need to explicitly set `ws.version`.

The `ws.version` element was used to tell the Gateway which version of the WebSocket protocol to use for the service connections. You would specify this element only if the scheme of the URI inside the `connect` element is `ws:` or `wss:` (to indicate that the WebSocket protocol was being used). If you did not specify the `ws.version` in connect-options, then the WebSocket version defaults to `rfc6455`.

##### Example

The following example shows addresses for the WebSocket (`ws`) and WebSocket Secure (`wss`) protocols and uses WebSocket version `draft-75` to connect to a service running on release 3.2 of the Gateway. The example uses the `[proxy](r_configure_gateway_service.md)` service, which is common, but not required. See the [type](#type) element for a list of service types.

``` xml
<service>
  <accept>ws://${gateway.hostname}:8000/proxy</accept>
    <connect>wss://${gateway.hostname}:5566/data</connect>
  <connect-options>
    <ws.version>draft-75</ws.version>
  </connect-options>
</service>
```

### realm-name

The name of the security realm used for authorization.

#### Example

``` xml
<service>
  <accept>wss://localhost:9000/path</accept>
  <connect>tcp://corp.example.com:88</connect>
  <type>proxy</type>
  <realm-name>demo</realm-name>
    .
    .
    .
</service>
```

#### Notes

-   If you do not configure `realm-name`, then authentication and authorization are not enforced for the service.

### auth-constraint

This element has been deprecated. Use the [authorization-constraint](#authorization-constraint) element instead. 

### authorization-constraint

**Required?** Optional; **Occurs:** zero or more

Use the `authorization-constraint` element to configure the user roles that are authorized to access the service. `authorization-constraint` contains the following subordinate element:

| Subordinate Element | Description                                                                                                  |
|:--------------------|:-------------------------------------------------------------------------------------------------------------|
| require-role        | The name of the user role to be included in the `authorization-constraint` or `*` to indicate any valid user |
| require-valid-user  | Grants access any user whose credentials have been successfully authenticated.                               |

#### Example

The following example of a `proxy` service element is configured with an `authorization-constraint`. The example uses the `[proxy](r_configure_gateway_service.md)` service, which is common, but not required. See the [type](#type) element for a list of service types.

``` xml
<service>
  <accept>ws://localhost:8000/remoteService</accept>
  <connect>tcp://localhost:6163</connect>

  <type>proxy</type>

  <authorization-constraint>
    <require-role>AUTHORIZED</require-role>
  </authorization-constraint>
</service>
```

### mime-mapping

**Required?** Optional; **Occurs:** zero or more

The `mime-mapping` element defines the way the Gateway maps a file extension to a MIME type. See the the main description for [mime-mapping (service-defaults)](r_configure_gateway_service_defaults.md). You can override the default configuration or add a new MIME type mapping for a particular service by adding a `mime-mapping` element to the `service` entry. You can only add `mime-mapping` elements immediately *before* any cross-site constraints for a service.

#### Example

The following example shows a `directory` service that includes two mime-mapping elements for files with the `PNG` and and `HTML` extensions. The Gateway sets the content or MIME type for files with the `PNG` extension as a PNG image and files with the `HTML` extension as an HTML text file:

``` xml
<service>
  <accept>ws://localhost:8000</accept>
  <accept>wss://localhost:9000</accept>

  <type>directory</type>

  <accept-options>
    <ws.bind>8001</ws.bind>
    <wss.bind>9001</wss.bind>
  </accept-options>

  <mime-mapping>
    <extension>png</extension>
    <mime-type>image/png</mime-type>
  </mime-mapping>
  <mime-mapping>
    <extension>html</extension>
    <mime-type>text/html</mime-type>
  </mime-mapping>

  <cross-site-constraint>
    <allow-origin>http://localhost:8000</allow-origin>
  </cross-site-constraint>
  <cross-site-constraint>
    <allow-origin>https://localhost:9000</allow-origin>
  </cross-site-constraint>
  </service>
```

#### Notes

-   If your client does not properly render a file type as expected, ensure that the MIME type is properly mapped and that you do not have multiple entries for the same extension type within the same section.
-   If two or more `mime-mapping` entries for the same extension are given in a single `service` or in `service-defaults`, then the Gateway applies the last `mime-mapping` entry. That is, within a given `service` or `service-defaults` section, the Gateway applies the `mime-mapping` entry closest to the end of the `service` (or `service-defaults`) section.

### cross-site-constraint

**Required?** Optional; **Occurs:** zero or more

Use cross-site-constraint to configure how a cross-origin site is allowed to access a service. `cross-site-constraint` contains the following subordinate elements:

**Note:** You must specify the properties for the cross-site-constraint element in the order shown in the table.

| Subordinate Element | Description                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               |
|:--------------------|:--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| allow-origin        | Specifies the cross-origin site or sites that are allowed to access this service: To allow access to a specific cross-site origin site, specify the protocol scheme, fully qualified host name, and port number of the cross-origin site in the format: `<scheme>://<hostname>:<port>`. For example: `<allow-origin>http://localhost:8000</allow-origin>`. To allow access to all cross-site origin sites, including connections to gateway services from pages loaded from the file system rather than a web site, specify the value `*`. For example: `<allow-origin>*</allow-origin>`. Specifying `*` may be appropriate for services that restrict HTTP methods or custom headers, but not the origin of the request. |
| allow-methods       | A comma-separated list of methods that can be invoked by the cross-origin site. For example: `<allow-methods>POST,DELETE</allow-methods>`.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                |
| allow-headers       | A comma-separated list of custom header names that can be sent by the cross-origin site when it accesses the service. For example, `<allow-headers>X-Custom</allow-headers>`.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                             |
| maximum-age         | Specifies the number of seconds that the results of a preflight request can be cached in a preflight result cache. See the W3C [Access-Control-Max-Age header](http://www.w3.org/TR/cors/#access-control-max-age-response-header) response header for more information. For example, `<maximum-age>1 second</maximum-age>`.                                                                                                                                                                                                                                                                                                                                                                                               |

#### Example

The following example of a `proxy` service element includes a cross-site-constraint, allowing access to the back-end service or message broker by the site `http://localhost:8000` (note the different port number).

``` xml
<service>
  <accept>ws://localhost:8001/remoteService</accept>
  <connect>tcp://localhost:6163</connect>

  <type>proxy</type>

  <authorization-constraint>
    <require-role>AUTHORIZED</require-role>
  </authorization-constraint>

  <cross-site-constraint>
    <allow-origin>http://localhost:8000</allow-origin>
  </cross-site-constraint>
</service>
```

#### Notes

-   Cross-site access to the back-end is denied by default. However, by defining a cross-site constraint, you can override the default behavior and effectively "white-list" cross-origin sites.

Summary
-------

In this document, you learned about the Gateway service element and how to specify it in your Gateway configuration file. For more information about the location of the configuration files and starting the Gateway, see [Setting Up the Gateway](../about/setup-guide.md). For more information about KAAZING Gateway administration, see the [documentation](../index.md).

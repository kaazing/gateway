-   [Home](../../index.md)
-   [Documentation](../index.md)
-   Configure ${enterprise.shield}

Configure ${enterprise.shield}${enterprise.logo}${enterprise.logo.jms}
======================================================================

In this procedure you will learn how to configure ${enterprise.shield} for ${gateway.name.long} to allow a connection from the DMZ to the trusted network without the need to open any inbound firewall ports. By configuring ${enterprise.shield}, you can close all externally facing ports entirely, providing maximum security and zero attack vectors for malicious users seeking to exploit ports in your firewall.

Before you get started with configuration tasks, it is important to understand the concepts behind ${enterprise.shield} by reading the topics in [About ${enterprise.shield}](o_rc_checklist.md#whatis) and to have a basic understanding of common ${gateway.cap} topologies, as described in [DMZ ${gateway.cap} to Trusted Network Topology](../admin-reference/c_topologies.md#DMZ_to_Internal).

The procedure in this topic creates a topology similar to the following figure.

<figure style="margin-left:0px;">
![${enterprise.shield} Topology Configured with a Reverse Connection](../images/f-dmz-trustednetwork-860-06.png)
<figcaption>

**Figure: ${enterprise.shield} Topology**

</figcaption>
</figure>
In the figure, the client needs to communicate with the ${message.broker.generic} located on a trusted network, but the client is not authorized to communicate through the firewall. In the ${enterprise.shield} topology, interactions between the client, the DMZ ${gateway.cap} and internal ${gateway.cap}, and the ${message.broker.generic} on a trusted network occur as described in the following list:

1.  The internal ${gateway.cap} in the trusted network accepts connections for the proxy service to the ${message.broker.generic}, as usual. However because it is configured for a *reverse* connection, the `accept` does not listen for incoming connections as usual, but rather initiates a connection—remember that it's reversed! **Note:** Normally an `accept` element in a service definition instructs the internal ${gateway.cap} to listen on the port for incoming connections. However, with ${enterprise.shield}, instead of listening, the internal ${gateway.cap} initiates a reverse connection to the DMZ ${gateway.cap}. The reverse connection is achieved by configuring the internal ${gateway.cap} service to act as a SOCKS client, sending remote bind requests to the DMZ ${gateway.cap}. This tells the remote side to listen for connections on a particular host and port. That way, when clients' connection requests come in to the DMZ ${gateway.cap}, the ${gateway.cap} matches them up with SOCKS bind requests.
2.  The DMZ ${gateway.cap} starts to connect to the internal ${gateway.cap}. Typically, the `connect` initiates a connection but because this is configured to be in *reverse* the DMZ ${gateway.cap} listens for connections from the internal ${gateway.cap}. **Note:** Normally a `connect` element in a service definition instructs the DMZ ${gateway.cap} to establish an outgoing physical network connection to the specified URI on a remote host machine for each client as it connects to the service. However, with a reverse connection, instead of connecting, the DMZ ${gateway.cap} listens for an incoming bind request from the internal ${gateway.cap}. If a bind request matching the specified connect URI is received, then a reverse connection is formed. The reverse connection is achieved by configuring the DMZ ${gateway.cap} service to act as a SOCKS server, receiving remote bind requests from the internal ${gateway.cap}.
3.  A client initiates a request to the ${message.broker.generic}.
4.  After the client connects to the DMZ ${gateway.cap}, a full-duplex connection between the client and the ${message.broker.generic} is established through the DMZ and internal ${gateway.cap}s.

    When the DMZ ${gateway.cap} receives an inbound request, an end-to-end logical connection from the client to the ${message.broker.generic} is in place, and the application functions as usual. The only difference to the ${message.broker.generic} is that its connection comes from the internal ${gateway.cap} rather than directly from the DMZ ${gateway.cap}.

The configuration for ${enterprise.shield} is virtually transparent to other areas in your architecture. The end points—the client and the ${message.broker.generic}—remain unchanged and are even unaware that the connection is reversed between the DMZ and internal ${gateway.cap}s. After you add a second ${gateway.cap}, the only change to your existing architectural components is a minor configuration change on the DMZ ${gateway.cap}, adding the internal ${gateway.cap}, and closing the inbound ports in your firewall.

Before You Begin
----------------

This procedure is part of [Configure ${enterprise.shield} with ${the.gateway}](o_rc_checklist.md)

1.  Become familiar with the [DMZ-to-trusted network ${gateway.cap} topology](../admin-reference/c_topologies.md#DMZ_to_Internal)
2.  [About ${enterprise.shield}](o_rc_checklist.md#whatis)
3.  **Configure ${enterprise.shield}**
4.  [Configure Enterprise Shield in a Cluster](p_rc_cluster.md)

To Configure ${enterprise.shield}
---------------------------------

To configure ${enterprise.shield}, you set up the internal ${gateway.cap} and DMZ ${gateway.cap} configurations to use the SOCKet Secure (SOCKS) protocol and set the other configuration elements as described for each ${gateway.cap} in the following procedure.

**Note:** The following examples use the .net domain (for example, `tcp://broker.internal.net:1080`) to indicate internal, nonpublic URLs, and use the .com domain to indicate public URLs. All domains and URLs are for example purposes only.
### <a name="Step1"></a>Configure the Internal Gateway

The following procedure walks you through the steps to configure the internal ${gateway.cap} for ${enterprise.shield}. See [Configuration Examples](#configex) for a snapshot of the completed service configuration.

1.  Set up a secure connection in the `accept` element by using WebSocket Secure and port 443, for example: `<accept>wss://gateway.example.com:443/path</accept>`
    <p>
    The public URI `wss://gateway.example.com:443/path` is configured to accept a connection from the client. In subsequent steps you will see that this URI is used again to configure a logical connection through the DMZ ${gateway.cap} and to the internal ${gateway.cap}. **Note:** Once you configure a secure connection on one Gateway then you must configure every ${gateway.cap} (including every member in a ${gateway.cap} cluster) in the same fashion to achieve secure end-to-end connectivity.
2.  Connect to the ${message.broker.generic} by configuring the `connect` element using the URI for the ${message.broker.generic}. For example: `<connect>tcp://internal.example.com:1080</connect>`

    The example uses port 1080, which is commonly used, but not required. See [About Ports](../about/about.md#aboutports) for a list of commonly used ports.

3.  Connect the internal ${gateway.cap} to a ${message.broker.generic} by adding `properties` that name the ConnectionFactory, describe the format for queue and topic names, and provide the URI for the JMS-compliant message broker. For example:

    ``` auto-links:
          <properties>
            <connection.factory.name>
              ConnectionFactory
            </connection.factory.name>
            <context.lookup.topic.format>
              dynamicTopics/%s
            </context.lookup.topic.format>
            <context.lookup.queue.format>
              dynamicQueues/%s
            </context.lookup.queue.format>
            <env.java.naming.factory.initial>
              ${message.broker.init.context.factory}
            </env.java.naming.factory.initial>
            <env.java.naming.provider.url>
              tcp://internal.example.com:${port}
            </env.java.naming.provider.url>
          </properties>
    ```

    The internal ${gateway.cap} connects to the ${message.broker.generic} using `tcp://broker.internal.net:${port}`.

4.  Add the following [`accept-options`](../admin-reference/r_conf_service.md#svcacceptopts):
    -   Set the HTTP transport to use SOCKS+SSL (recommended) or SOCKS protocol, for example: `<http.transport>socks+ssl://gateway.dmz.net:1080</http.transport>`

        ${enterprise.shield} requires that you use a transport option (`http.transport` for WebSocket connections) using SOCKS or SOCKS+SSL protocol to establish the network connections from the DMZ Gateway to the internal ${gateway.cap} at the center of the Enterprise.

    -   Specify the [`socks.mode`](../admin-reference/r_conf_service.md#socksmode) option in reverse mode so the internal ${gateway.cap} listens for connections from the DMZ ${gateway.cap}:
         `<socks.mode>reverse</socks.mode>`
    -   Set `socks.retry.maximum.interval` to a short time interval, for example:
         `<socks.retry.maximum.interval>1second</socks.retry.maximum.interval>`

        Setting `socks.retry.maximum.interval` handles cases where the DMZ ${gateway.cap} has not started but the internal ${gateway.cap} keeps trying to connect to the DMZ ${gateway.cap}. Use this property to specify the maximum time interval that you want the internal ${gateway.cap} to wait before retrying the connection to the DMZ ${gateway.cap}. The setting is the maximum (backoff) interval. Thus, the retry will actually occur at 100ms, then 200ms, then 400ms, and so on until the maximum is reached.

</li>
</ol>
See the [Service Reference](../admin-reference/r_conf_service.md) for more information about the configuration elements.

You've completed configuring the internal ${gateway.cap}. Now, let's configure the DMZ ${gateway.cap}.

### <a name="Step2"></a>Configure the DMZ Gateway

The following procedure walks you through the steps to configure the DMZ ${gateway.cap} for ${enterprise.shield}. In such a configuration, a DMZ ${gateway.cap} or ${gateway.cap} cluster is deployed in the firewall-protected DMZ peripheral network to service requests from ${gateway.name.long} clients on the Web. See [Configuration Examples](#configex) for a snapshot of the completed service configuration.

1.  Set up a secure connection in the `accept` and `connect` elements by using WebSocket Secure and port 443, for example: `<accept>wss://gateway.example.com:443/path</accept><connect>wss://gateway.example.com:443/path</connect>`

    In the example, the public URI of the `connect` on the DMZ ${gateway.cap} matches the URI of the accept on the internal ${gateway.cap}. This is a logical connection. This continues the usage of the public WebSocket Secure URI `wss://gateway.example.com:443/path` to create a logical connection from the client through the DMZ ${gateway.cap} and to the internal ${gateway.cap}.

    Once you configure secure connections on one ${gateway.cap} then you must configure every ${gateway.cap} (including every member in a ${gateway.cap} cluster) in the same fashion to achieve secure end-to-end connectivity.

2.  Set up [properties](../admin-reference/r_conf_service.md#propertiesele) that are specific to the service:
    -   Prepare a connection to the ${message.broker.generic} in advance of the first incoming client connection by setting the `prepared.connection.count` property to at least 1. For example:

        `<prepared.connection.count>1</prepared.connection.count>`

        In this example, the `prepared.connection.count` property creates one connection to the ${message.broker.generic} when ${the.gateway} starts.

    -   Set the `maximum.recovery.interval` property to at least 1 second:

        `<maximum.recovery.interval>1second</maximum.recovery.interval>`

3.  Configure the service [`type`](../admin-reference/r_conf_service.md#typeele) to the proxy service that you use to enable a WebSocket connection to the ${message.broker.generic} on the DMZ ${gateway.cap}. This connects to the ${rc.jms.config.example.begin} `${proxy.service.jms.inline}` service ${rc.jms.config.example.end} ${rc.nonjms.config.example.begin} proxy service ${rc.nonjms.config.example.end} on the internal ${gateway.cap}, establishing a connection between the DMZ ${gateway.cap} and the internal ${gateway.cap} for each client connection. The following example configures the `proxy` service type because that is the service type we configured earlier for the internal Gateway. See the reference information for the [`type`](../admin-reference/r_conf_service.md#typeele) property to learn more about the types of services you can configure:

    `<type>${proxy.service.inline}</type>`

4.  In the `accept-options`, bind the URI in the accept element to the IP address of the network interface. Only the port number is required. For example:

    `         <tcp.bind>443</tcp.bind>     `

5.  Set the following `connect-options`:
    -   Set the HTTP transport to use SOCKS+SSL (recommended) or SOCKS protocol. For example:

        `         <http.transport>socks+ssl://gateway.dmz.net:1080</http.transport>     `

        ${enterprise.shield} requires that you use a transport option (`http.transport` for WebSocket connections) using SOCKS or SOCKS+SSL protocol to establish the network connections from the DMZ Gateway to the internal Gateway at the center of the Enterprise. Port 1080 is commonly used, but not required. See [About Ports](../about/about.md#aboutports) for a list of commonly used ports.

        </p>
    -   Configure a reverse connection by setting the [`socks.mode`](../admin-reference/r_conf_service.md#conn_sockstimeout) option to `reverse`:

        `         <socks.mode>reverse</socks.mode>     `

    -   Set the [`socks.timeout`](../admin-reference/r_conf_service.md#conn_sockstimeout) property to 1 higher than the value you set for `socks.retry.maximum.interval` on the internal ${gateway.cap}.

        `         <socks.timeout>2seconds</socks.timeout>     `

6.  Optionally, require that the internal ${gateway.cap} provide a digital certificate to establish a secure connection. You can achieve this higher level of security by setting the [`socks.ssl.verify-client`](../admin-reference/r_conf_service.md#sockssslverifyclient) to `required` in the connect options:

    </p>
    `          <socks.ssl.verify-client>required</socks.ssl.verify-client>      `

    In an ${enterprise.shield} topology over `socks+ssl://`, the DMZ ${gateway.cap} provides the internal ${gateway.cap} with a digital certificate that the internal ${gateway.cap} uses to verify the DMZ ${gateway.cap}’s identity before establishing the secure connection. For added security, you can use the [`socks.ssl.verify-client`](../admin-reference/r_conf_service.md#sockssslverifyclient) connect option on the DMZ ${gateway.cap} to require the internal ${gateway.cap} to provide a digital certificate to establish a secure connection. This configuration ensures that both the DMZ ${gateway.cap} and internal ${gateway.cap} are verified via TLS/SSL before transmitting data, establishing mutual authentication.

    **Note:** For added security on the client side, use the information and instructions in [Require Clients to Provide Certificates to ${the.gateway}](../security/p_tls_mutualauth.md) to require the client to present a certificate to the DMZ ${gateway.cap} so that the DMZ ${gateway.cap} can validate the client's identity.[](../security/p_tls_mutualauth.md)

</li>
See the [Service Reference](../admin-reference/r_conf_service.md) for more information about the configuration elements. You've completed configuring the DMZ ${gateway.cap}.

### <a name="configex"></a>Configuration Examples

<p>
Here are configuration examples for ${enterprise.shield} in a topology with a DMZ ${gateway.cap} proxying client connections for an internal ${gateway.cap} that connects to a ${message.broker.generic}.
</ol>
</li>
</ol>
#### Example of an Internal Gateway Configuration

${rc.nonjms.config.example.begin}
The following example configuration uses the `proxy` service type to enable clients to make a WebSocket connection to a back-end Gateway service on port 1080.

``` auto-links:
    <service>
      <accept>wss://gateway.example.com:443/path</accept>
      <connect>tcp://internal.example.com:1080</connect>
      
      <type>proxy</type>
      
      <accept-options>
        <http.transport>socks+ssl://gateway.dmz.net:1080</http.transport>
        <socks.mode>reverse</socks.mode>
        <socks.retry.maximum.interval>1second</socks.retry.maximum.interval>        
      </accept-options>
    </service>
```

${rc.nonjms.config.example.end} ${rc.jms.config.example.begin}
``` auto-links:
    <service>
      <accept>wss://gateway.example.com:443/path</accept>
      
      <type>${proxy.service.jms.inline}</type>
             
      <properties>
        <connection.factory.name>
          ConnectionFactory
        </connection.factory.name>
        <context.lookup.topic.format>
          dynamicTopics/%s
        </context.lookup.topic.format>
        <context.lookup.queue.format>
          dynamicQueues/%s
        </context.lookup.queue.format>
        <env.java.naming.factory.initial>
          ${message.broker.init.context.factory}
        </env.java.naming.factory.initial>
        <env.java.naming.provider.url>
          tcp://internal.example.com:${port}
        </env.java.naming.provider.url>
      </properties>
      
      <accept-options>
        <http.transport>socks+ssl://gateway.dmz.net:1080</http.transport>
        <socks.mode>reverse</socks.mode>
        <socks.retry.maximum.interval>1second</socks.retry.maximum.interval>        
      </accept-options>
    </service>
```

${rc.jms.config.example.end}
#### Example of a DMZ Gateway Configuration

``` auto-links:
  <service>
    <accept>wss://gateway.example.com:443/path</accept>
    <connect>wss://gateway.example.com:443/path</connect>

    <type>proxy</type>
  
    <properties>
      <prepared.connection.count>1</prepared.connection.count>
      <maximum.recovery.interval>1second</maximum.recovery.interval>
    </properties>

    <accept-options>
      <tcp.bind>443</tcp.bind>        
    </accept-options>

    <connect-options>
      <http.transport>socks+ssl://gateway.dmz.net:1080</http.transport>
      <socks.mode>reverse</socks.mode>
      <socks.timeout>2seconds</socks.timeout>
      <socks.ssl.verify-client>required</socks.ssl.verify-client>    
    </connect-options>
  </service>
```

### <a name="Step3"></a>Verify Your Configuration

Verify your configuration is working properly${begin.comment} by following the "How do I verify that the ${gateway.cap} is running?" instructions in ${setting.up.inline}${end.comment}.

### <a name="Step4"></a>Close Inbound Ports

Close the inbound ports on your firewall using the documentation provided by your system.

### <a name="Step5"></a>Verify the End-to-End Configuration

Use a client within the DMZ to test out the internal ${gateway.cap} before deploying ${enterprise.shield} in your production environment. ${begin.comment}For help verifying your configuration, follow the instructions in "How do I verify that ${the.gateway} is running?" in ${setting.up.inline}.${end.comment}

### Next Step

Congratulations, you got ${enterprise.shield} working! All inbound ports on your firewall are closed so there is no access to the trusted network from the DMZ. No physical address information from the trusted network is exposed in the DMZ configuration. But there is an extra step that must be done if you want to add ${enterprise.shield} to your cluster configuration. See [Configure ${enterprise.shield} in a Cluster](p_rc_cluster.md) for more information.

See Also
--------

-   [About ${enterprise.shield}](o_rc_checklist.md#whatis)
-   [Service Element Reference](../admin-reference/r_conf_service.md) for more information about the elements, properties, and options used in the configuration examples
-   [Delta Messaging](../admin-reference/r_stomp_service.md#deltamsg) to configure ${the.gateway} to send delta messages through the `jms` service in the internal ${gateway.cap} through a DMZ ${gateway.cap} that is running the `jms.proxy` service.



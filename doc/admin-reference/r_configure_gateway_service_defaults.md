-   [Home](../../index.md)
-   [Documentation](../index.md)
-   Administration with KAAZING Gateway

Service-Defaults Reference 
=================================================

This document describes all of the elements and properties you can use to configure KAAZING Gateway service-defaults.

Overview
----------------------------------

You can use the optional `service-defaults` element to configure certain default options across all services running on the Gateway.

Structure
------------------------------------

The Gateway configuration file (`gateway-config.xml` or `gateway-config-minimal.xml`) defines the `service-defaults` configuration element contained in the top-level `gateway-config` element:

-   [gateway-config](r_conf_gwconfig.md)
    -   [service-defaults](#service-defaults)
        -   [accept-options](#accept-options-service-defaults)
            -   [*protocol*.bind](r_conf_service.md#protocolbind), where *protocol* can be ws, wss, http, https, socks, ssl, tcp, or udp
            -   [*protocol*.transport](r_conf_service.md#protocoltransport), where *protocol* can be pipe, tcp, ssl, or http
            -   [ws.maximum.message.size](r_conf_service.md#wsmaximummessagesize)
            -   [http.keepalive.timeout](r_conf_service.md#httpkeepalivetimeout)
            -   [ssl.ciphers](r_conf_service.md#sslciphers)
            -   [ssl.protocols](r_conf_service.md#sslprotocols-and-sockssslprotocols)
            -   [ssl.encryption](r_conf_service.md#sslencryption)
            -   [ssl.verify-client](r_conf_service.md#sslverify-client)
            -   [socks.mode](r_conf_service.md#socksmode) ![This feature is available in KAAZING Gateway - Enterprise Edition](../images/enterprise-feature.png)
            -   [socks.ssl.ciphers](r_conf_service.md#sockssslciphers) ![This feature is available in KAAZING Gateway - Enterprise Edition](../images/enterprise-feature.png)
            -   [socks.ssl.protocols](r_conf_service.md#sslprotocols-and-sockssslprotocols)
            -   [socks.ssl.verify-client](r_conf_service.md#sockssslverify-client)
            -   [socks.retry.maximum.interval](r_conf_service.md#socksretrymaximuminterval) ![This feature is available in KAAZING Gateway - Enterprise Edition](../images/enterprise-feature.png)
            -   [tcp.maximum.outbound.rate](r_conf_service.md#tcpmaximumoutboundrate) ![This feature is available in KAAZING Gateway - Enterprise Edition](../images/enterprise-feature.png)
            -   [ws.inactivity.timeout](r_conf_service.md#wsinactivitytimeout)
            -   [http.server.header](r_conf_service.md#httpserverheader)
        -   [mime-mapping](#mime-mapping-service-defaults)
            -   [extension](#mime-mapping-service-defaults)
            -   [mime-type](#mime-mapping-service-defaults)

service-defaults
----------------------------------------------

Each `service-defaults` element can contain any of the following subordinate elements:

| Subordinate Element                         | Description                                                                                                                                                                                                                                                                |
|---------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| accept-options                              | Options for the [accept](r_conf_service.md#accept) element (see [accept-options (service-defaults)](#accept-options-service-defaults))                                                                                                                                            |
| <a name="mimemapextension"></a>mime-mapping | Mappings of file extensions to MIME types. Each `mime-mapping` entry defines the HTTP Content-Type header value to be returned when a client or browser requests a file that ends with the specified extension (see [mime-mapping (service-defaults)](#mime-mapping-service-defaults)) |

### accept-options (service-defaults)

The `service-defaults` section can contain the following accept-options:

-   [*protocol*.bind](r_conf_service.md#protocolbind), where *protocol* can be ws, wss, http, https, socks, ssl, tcp, or udp. This option binds the URL(s) on which the service accepts connections defined by the accept element.
-   [*protocol*.transport](r_conf_service.md#protocoltransport), where *protocol* can be pipe, tcp, ssl, or http
-   [ws.maximum.message.size](r_conf_service.md#wsmaximummessagesize): configures the maximum incoming WebSocket message size allowed by the Gateway
-   [http.keepalive.timeout](r_conf_service.md#httpkeepalivetimeout): configures the duration the Gateway waits between responding to an HTTP or HTTPS connection request and the subsequent request
-   [ssl.ciphers](r_conf_service.md#sslciphers): specifies the TLS/SSL ciphers used by KAAZING Gateway on secure connections.
-   [ssl.protocols](r_conf_service.md#sslprotocols-and-sockssslprotocols): specifies a comma-separated list of the TLS/SSL protocol names on which the Gateway can accept or make connections.
-   [ssl.encryption](r_conf_service.md#sslencryption): signals KAAZING Gateway to enable or disable encryption on incoming traffic.
-   [ssl.verify-client](r_conf_service.md#sslverify-client): implements a mutual verification pattern where, in addition to the Gateway presenting a certificate to the client, the client also presents a certificate to the Gateway so that the Gateway can verify the client's authenticity.
-   [socks.mode](r_conf_service.md#socksmode) ![This feature is available in KAAZING Gateway - Enterprise Edition](../images/enterprise-feature.png): initiates the Gateway connection using the SOCKet Secure (SOCKS) protocol in forward or reverse mode.
-   [socks.ssl.ciphers](r_conf_service.md#sockssslciphers) ![This feature is available in KAAZING Gateway - Enterprise Edition](../images/enterprise-feature.png): uses `socks.ssl.ciphers` to list the encryption algorithms used by TLS/SSL on the secure connection (WSS, HTTPS or SSL).
-   [socks.ssl.protocols](r_conf_service.md#sslprotocols-and-sockssslprotocols): specifies a comma-separated list of the TLS/SSL protocol names on which the Gateway can accept or make connections using the SOCKS protocol.
-   [socks.ssl.verify-client](r_conf_service.md#sockssslverify-client): implements a mutual verification pattern (same as `ssl.verify-client`) over the SOCKS protocol.
-   [socks.retry.maximum.interval](r_conf_service.md#socksretrymaximuminterval) ![This feature is available in KAAZING Gateway - Enterprise Edition](../images/enterprise-feature.png): sets the maximum interval of time that the internal Gateway waits to retry a reverse connection to the DMZ Gateway after a failed attempt in an Enterprise Shield™ topology.
-   [tcp.maximum.outbound.rate](r_conf_service.md#tcpmaximumoutboundrate) ![This feature is available in KAAZING Gateway - Enterprise Edition](../images/enterprise-feature.png): specifies the maximum bandwidth rate at which bytes can be written from the Gateway to a client session.
-   [ws.inactivity.timeout](r_conf_service.md#wsinactivitytimeout): specifies the maximum number of seconds that the network connection can be inactive (seconds is the default time interval syntax). The Gateway drops the connection if it cannot communicate with the client in the number of seconds specified.
-   [http.server.header](r_conf_service.md#httpserverheader): enables or disables the inclusion of the HTTP server header. By default, the Gateway writes a HTTP Server header.

These `accept-options` plus examples are fully documented in the [Service Reference](r_conf_serv_defs.md#accept-options-service-defaults). Note that if you specify `accept-options` on a particular [service](r_conf_service.md), then those `accept-options` supercede any default values configured in `service-defaults`. Thus, if there are no explicit `accept-options` configured for a particular service, then the Gateway uses the values configured in service-defaults.

#### Example

The following example shows `ssl.encryption` disabled, sample network protocol bindings, `ws.maximum.message.size` set to `256k`, and `http.keepalive.timeout` set to `90` seconds, just above the default `mime-mapping` entries, as shown in lines 2-10:

``` xml
<service-defaults>
  <accept-options>
    <ssl.encryption>disabled</ssl.encryption>
    <ws.bind>8050</ws.bind>
    <wss.bind>192.168.10.25:8055</wss.bind>
    <http.bind>192.168.10.25:8060</http.bind>
    <https.bind>192.168.10.25:8065</https.bind>
    <ws.maximum.message.size>256k</ws.maximum.message.size>
    <http.keepalive.timeout>90</http.keepalive.timeout>
  </accept-options>

  <mime-mapping>
    <extension>html</extension>
    <mime-type>text/html</mime-type>
  </mime-mapping>
  ...
  </service-defaults>
```

### mime-mapping (service-defaults)

The `mime-mapping` element defines the way the Gateway maps a file extension to a MIME type. The `service-defaults` section in the default Gateway configuration file contains default MIME type mappings that apply to all services on the Gateway.

#### Example

The following example shows two entries for the same file extension; in this case, when the Gateway receives a request for a file with an `HTML` extension, the Gateway will respond with a Content-Type header value of `text/html` (not `image/png`) as shown in lines 1-4 and 9-12:

``` xml
  <mime-mapping>
    <extension>html</extension>
    <mime-type>image/png</mime-type>
  </mime-mapping>
  <mime-mapping>
    <extension>js</extension>
    <mime-type>text/javascript</mime-type>
  </mime-mapping>
  <mime-mapping>
    <extension>html</extension>
    <mime-type>text/html</mime-type>
  </mime-mapping>
```

Notes
-----

-   When the Gateway responds to a file request, such as from the `directory` service, the response includes a Content-Type header based on the filename extension of the requested file. The Content-Type header value is the specified MIME type for that extension. If the file extension is not mapped to a MIME type by a `mime-mapping` element, the Gateway does not include a Content-Type header in its response.
-   You can specify MIME types for file extensions either in the `service-defaults` section or in a `service`. Specifying MIME types for file extensions in a `service` overrides any existing corresponding `mime-mapping` entries in the `service-defaults` section. See [service](#service) for more information.
-   If you specify two or more `mime-mapping` entries for the same extension in a single `service` or in `service-defaults`, the Gateway only applies the last `mime-mapping` entry for that extension.
-   The `service-defaults` section in the default Gateway configuration includes the following standard mappings. You can modify these entries, but keep in mind that all `mime-mapping` entries must come after any [accept-options](#svcdftacceptoptions) you add to this section.

    **Note:** The Gateway has hard-coded internal MIME mappings that are equivalent to those provided in the `service-defaults` section of the `gateway-config.xml`, for backward compatibility with earlier releases of KAAZING Gateway. You cannot remove these internal settings. You can, however, override them with new MIME-type values.

    The default Gateway `mime-mapping` entries are:

    ``` xml
    <service-defaults>
      <mime-mapping>
        <extension>html</extension>
        <mime-type>text/html</mime-type>
      </mime-mapping>
      <mime-mapping>
        <extension>htm</extension>
        <mime-type>text/html</mime-type>
      </mime-mapping>
      <mime-mapping>
        <extension>js</extension>
        <mime-type>text/javascript</mime-type>
      </mime-mapping>
      <mime-mapping>
        <extension>png</extension>
        <mime-type>image/png</mime-type>
      </mime-mapping>
      <mime-mapping>
        <extension>gif</extension>
        <mime-type>image/gif</mime-type>
      </mime-mapping>
      <mime-mapping>
        <extension>jpg</extension>
        <mime-type>image/jpeg</mime-type>
      </mime-mapping>
      <mime-mapping>
        <extension>jpeg</extension>
        <mime-type>image/jpeg</mime-type>
      </mime-mapping>
      <mime-mapping>
        <extension>svg</extension>
        <mime-type>image/svg+xml</mime-type>
      </mime-mapping>
      <mime-mapping>
        <extension>css</extension>
        <mime-type>text/css</mime-type>
      </mime-mapping>
      <mime-mapping>
        <extension>swf</extension>
        <mime-type>application/x-shockwave-flash</mime-type>
      </mime-mapping>
      <mime-mapping>
        <extension>xap</extension>
        <mime-type>application/x-silverlight-app</mime-type>
      </mime-mapping>
      <mime-mapping>
        <extension>htc</extension>
        <mime-type>text/x-component</mime-type>
      </mime-mapping>
      <mime-mapping>
        <extension>jnlp</extension>
        <mime-type>application/x-java-jnlp-file</mime-type>
      </mime-mapping>
      <mime-mapping>
        <extension>manifest</extension>
        <mime-type>text/cache-manifest</mime-type>
      </mime-mapping>
      <mime-mapping>
        <extension>appcache</extension>
        <mime-type>text/cache-manifest</mime-type>
      </mime-mapping>
      <mime-mapping>
        <extension>vtt</extension>
        <mime-type>text/vtt</mime-type>
      </mime-mapping>
      <mime-mapping>
        <extension>aspx</extension>
        <mime-type>text/html</mime-type>
      </mime-mapping>
      </service-defaults>
    ```

Summary
-------

In this document, you learned about the Gateway service-defaults configuration element and how to specify it in your Gateway configuration file. For more information about the location of the configuration files and starting the Gateway, see [Setting Up the Gateway](../about/setup-guide.md). For more information about KAAZING Gateway administration, see the [documentation](../index.md).


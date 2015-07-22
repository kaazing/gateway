Service-Defaults Reference
=================================================

The `service-defaults` section of the Gateway configuration file allows you to configure default values that apply to all services running on the Gateway. 

Overview
----------------------------------

Use the optional `service-defaults` element to configure certain default options across all services running on the Gateway. However, note that any elements you specify in individual services (that you configure in the [`service`](r_configure_gateway_service.md) section of the Gateway configuration file) override the defaults you specify in the `service-defaults` section of the Gateway configuration file. For example, if there are no explicit `accept-options` configured for a particular service, then the Gateway uses the values configured in `service-defaults`.

Structure
------------------------------------

The Gateway configuration file (`gateway-config.xml` or `gateway-config-minimal.xml`) defines the `service-defaults` configuration element contained in the top-level `gateway-config` element:

-   [gateway-config](r_configure_gateway_gwconfig.md)
    -   [service-defaults](#service-defaults)
        -   accept-options
            -   . . . (Listed in the [Service Reference](r_configure_gateway_service.md#accept-options-and-connect-options))
        -   connect-options
            -   . . . (Listed in the [Service Reference](r_configure_gateway_service.md#accept-options-and-connect-options))
        -   mime-mapping
            -   . . . (Listed in the [Service Reference](r_configure_gateway_service.md#accept-options-and-connect-options))

service-defaults
----------------------------------------------

Each `service-defaults` element can contain any of the following subordinate elements:

| Subordinate Element                         | Description                                                                                                                                                                                                                                                                |
|---------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| accept-options  | Use the `accept-options` element to add options that apply to all services on the Gateway. The complete list of `accept-options` are fully documented in the [Service Reference](r_configure_gateway_service.md#accept-options-and-connect-options). |
| connect-options  | Use the `connect-options` element to add options that apply to all services on the Gateway. The complete list of  `connect-options` are fully documented in the [Service Reference](r_configure_gateway_service.md#accept-options-and-connect-options).  |
| mime-mapping | Use the `mime-mappings` element to define the way the Gateway maps a file extension to a MIME type. Each `mime-mapping` entry defines the HTTP Content-Type header value to be returned when a client or browser requests a file that ends with the specified extension. The `service-defaults` section in the default Gateway configuration file contains default MIME type mappings that apply to all services on the Gateway. The complete list of `mime-mapping` extensions and `mime-type`'s are documented in the [Service Reference](r_configure_gateway_service.md#mime-mapping). |

#### Example of setting default accept-options

The following example shows `ssl.encryption` disabled, sample network protocol bindings, `ws.maximum.message.size` set to `256k`, and `http.keepalive.timeout` set to `90` seconds, just above the default `mime-mapping` entries. Note that wss and https can both be bound to the same port (8055) because they are compatible protocols. However, if you try to bind incompatible protocols to the same port, you will receive a port conflict error when the Gateway starts up. For instance, you cannot bind ws and https to the same port.

``` xml
<service-defaults>
  <accept-options>
    <ssl.encryption>disabled</ssl.encryption>
    <ws.bind>8050</ws.bind>
    <wss.bind>192.168.10.25:8055</wss.bind>
    <http.bind>192.168.10.25:8060</http.bind>
    <https.bind>192.168.10.25:8055</https.bind>
    <ws.maximum.message.size>256k</ws.maximum.message.size>
    <http.keepalive.timeout>90 seconds</http.keepalive.timeout>
  </accept-options>

  <mime-mapping>
    <extension>html</extension>
    <mime-type>text/html</mime-type>
  </mime-mapping>
  ...
  </service-defaults>
```

#### Example of Mime Mapping

A service can return files of various types to a client.  Generally, HTTP mandates that a response containing a file also specify a Content-Type header describing the file contents.  You may use a <mime-mapping> tag to specify the Content-Type value to be returned for files with a particular name extension. 

The following example shows two entries for the same file extension; in this case, when the Gateway receives a request for a file with an `HTML` extension, the Gateway will respond with a Content-Type header value of `text/html` (not `image/png`). This example indicates that for files with names ending in '.png', the header 'Content-Type: image/png' should be returned by the Gateway, and 'Content-Type: text/html' should be returned for .html files. You can specify mappings in the both the `service-defaults` block and in any `service` blocks.  If a mapping for a given extension is specified in both the `service-defaults` block and a `service` block, the `service`-level mapping will be used when providing files from that service.

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
-   You can specify MIME types for file extensions either in the `service-defaults` section or in a `service`. Specifying MIME types for file extensions in a `service` overrides any existing corresponding `mime-mapping` entries in the `service-defaults` section. See [service](#service-defaults) for more information.
-   If you specify two or more `mime-mapping` entries for the same extension in a single `service` or in `service-defaults`, the Gateway only applies the last `mime-mapping` entry for that extension.
-   The `service-defaults` section in the default Gateway configuration includes the following standard mappings. You can modify these entries, but keep in mind that all `mime-mapping` entries must come after any [accept-options](#service-defaults) you add to this section.

    **Note:** The Gateway has hard-coded internal MIME mappings that are equivalent to those provided in the `service-defaults` section of the `gateway-config.xml`, for backward compatibility with earlier releases of KAAZING Gateway. You can edit these mappings, as needed (to override them with new MIME-type values), but you cannot remove the initial internal set of mappings.

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

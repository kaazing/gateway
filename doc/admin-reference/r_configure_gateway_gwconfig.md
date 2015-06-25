Gateway-Config Reference
===============================================

This document describes the top-level `gateway-config` element.

Overview
----------------------------------

You must specify the `gateway-config` element in the Gateway configuration file (`gateway-config.xml` or `gateway-config-minimal.xml`) to configure KAAZING Gateway.

Structure
-----------------------------------------

Define the `gateway-config` element as the top-level element in your configuration file, under which you can specify any of the subordinate configuration elements:

gateway-config
-   [service](r_configure_gateway_service.md)
-   [service-defaults](r_configure_gateway_service_defaults.md)
-   [security](r_configure_gateway_security.md)
-   [cluster](r_configure_gateway_cluster.md)

**Required?** Required; **Occurs:** exactly one

`gateway-config` element is the root-level element for gateway configuration. `gateway-config` contains the following elements:

| Element          | Description                                                                                                           |
|------------------|-----------------------------------------------------------------------------------------------------------------------|
| service          | The element for configuring a gateway service (see [service reference](r_configure_gateway_service.md))                          |
| service-defaults | The element for configuring default options gateway service (see [service-defaults reference](r_configure_gateway_service_defaults.md)) |
| security         | The element for configuring gateway security (see [security reference](r_configure_gateway_security.md))                         |
| cluster          | The element for configuring gateway clustering (see [cluster reference](r_configure_gateway_cluster.md))                         |

### Example

The following is an example `gateway-config` element:

``` xml
<?xml version="1.0" encoding="UTF-8" ?>

<gateway-config
    xmlns="http://xmlns.kaazing.com/2014/09/gateway">
    ...

</gateway-config>
```

Notes
-----

KAAZING Gateway configuration files must use the namespace: ` http://xmlns.kaazing.com/2014/09/gateway`. See [About KAAZING Gateway Namespace Declarations](c_configure_gateway_concepts.md#about-kaazing-gateway-namespace-declarations) for more information about namespaces.

Summary
-------

In this document, you learned about the Gateway `gateway-config` configuration element and how to specify it in your Gateway configuration file. For more information about the location of the configuration files and starting the Gateway, see [Setting Up the Gateway](../about/setup-guide.md). For more information about KAAZING Gateway administration, see the [documentation](../index.md).

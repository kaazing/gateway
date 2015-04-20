-   [Home](../../index.md)
-   [Documentation](../index.md)
-   Administration with KAAZING Gateway

Gateway-Config Reference 
===============================================

This document describes the top-level `gateway-config` element.

<a name="configuring"></a>Overview
----------------------------------

You must specify the `gateway-config` element in the Gateway configuration file (`gateway-config.xml` or `gateway-config-minimal.xml`) to configure KAAZING Gateway.

<a name="gwconfig_elements"></a>Structure
-----------------------------------------

Define the `gateway-config` element as the top-level element in your configuration file, under which you can specify any of the subordinate configuration elements:

<ul>
<li>
gateway-config
</li>
-   [service](r_conf_service.md)
-   [service-defaults](r_conf_serv_defs.md)
-   [security](r_conf_security.md)
-   [cluster](r_conf_cluster.md)

</li>
</ul>
**Required?** Required; **Occurs:** exactly one

`gateway-config` element is the root-level element for gateway configuration. `gateway-config` contains the following elements:

| Element          | Description                                                                                                           |
|------------------|-----------------------------------------------------------------------------------------------------------------------|
| service          | The element for configuring a gateway service (see [service reference](r_conf_service.md))                          |
| service-defaults | The element for configuring default options gateway service (see [service-defaults reference](r_conf_serv_defs.md)) |
| security         | The element for configuring gateway security (see [security reference](r_conf_security.md))                         |
| cluster          | The element for configuring gateway clustering (see [cluster reference](r_conf_cluster.md))                         |

### Example

The following is an example `gateway-config` element:

``` auto-links:
<?xml version="1.0" encoding="UTF-8" ?>

<gateway-config
    xmlns="http://xmlns.kaazing.com/2014/09/gateway">
    .
    .
    .

</gateway-config>
```

Notes
-----

KAAZING Gateway configuration files must use the namespace: ` http://xmlns.kaazing.com/2014/09/gateway`. See [About KAAZING Gateway Namespace Declarations](c_conf_concepts.md#aboutnamespace) for more information about namespaces.

Summary
-------

In this document, you learned about the Gateway `gateway-config` configuration element and how to specify it in your Gateway configuration file. For more information about the location of the configuration files and starting the Gateway, see [Setting Up KAAZING Gateway](../about/setup-guide.md). For more information about KAAZING Gateway administration, see the [documentation](../index.md).

</div>


About Gateway Configuration
========================================================

After you install the Gateway, you can configure it by modifying the settings in the configuration file `GATEWAY_HOME/conf/gateway-config.xml` file. The actual location of the `GATEWAY_HOME` directory depends on your operating system and the method (standalone or installer) used to install the Gateway. You can find more information about `GATEWAY_HOME` and the directory structure that is set up during installation in [Setting Up the Gateway](../about/setup-guide.md). This document describes the types of configuration files and [Configuration Element Index](r_configure_gateway_element_index.md) provides a list of the individual configuration elements.

By default, the Gateway uses the values in the Gateway configuration file when you start the Gateway. Optionally, you can override one or more Gateway configuration settings by specifying the `GATEWAY_OPTS` environment variable before you start the Gateway. This method is described in [Configure KAAZING Gateway Using the GATEWAY\_OPTS Environment Variable](p_configure_gateway_opts.md).

This topic covers the following information:

-   [About KAAZING Gateway Configuration Files](#about-kaazing-gateway-configuration-files)
-   [About KAAZING Gateway Configuration File Elements and Properties](#about-kaazing-gateway-configuration-file-elements-and-properties)
-   [About KAAZING Gateway Namespace Declarations](#about-kaazing-gateway-namespace-declarations)

About KAAZING Gateway Configuration Files
----------------------------------------------------------------------------

The Gateway provides two configuration file options:

-   `gateway-config.xml` contains a complete set of gateway properties, including the properties and services needed to run the Gateway documentation and out of the box demos.

    If you installed the **Gateway + Documentation + Demos** product, then both the `gateway-config.xml` and `gateway-config-minimal.xml` files are included in your `GATEWAY_HOME/conf` directory.

-   `gateway-config-minimal.xml` (recommended for production) contains the minimal set of properties necessary to run the Gateway. The recommended practice for configuring the Gateway for production purposes is to edit the `gateway-config-minimal.xml` file and configure only the properties necessary to customize the Gateway for your specific needs.

    If you installed the **Gateway Only** product, then your `GATEWAY_HOME/conf` directory includes only the `gateway-config-minimal.xml` file.

When you start the Gateway:

-   If you installed the **Gateway Only** product, then the Gateway uses the properties in the `gateway-config-minimal.xml` file when it is started.
-   If you installed the **Gateway + Documentation + Demos** product, then the Gateway uses the properties in the `gateway-config.xml` file, by default, when it is started. If there is no `gateway-config.xml` file, then the Gateway is started using `gateway-config-minimal.xml`.

Notes
-----

-   By default, the Gateway configuration accepts connections on localhost, and the cross-origin sites allowed to access those services are also configured for localhost by default. The default configuration is convenient for quickly trying the Gateway out of the box, but the default configuration is not production ready nor is it secure. You should customize the Gateway configuration for your needs by modifying settings in the `GATEWAY_HOME/conf/gateway-config.xml` file. For example, you might customize the Gateway to accept connections on a non-localhost host name or IP address and add authorization constraints for services. After you modify the `gateway-config.xml` file, you must restart the Gateway for the changes to take effect.
-   Consider using the `gateway-config.xml` file during your development and testing phase. Then, when you are ready to deploy the Gateway to your production environment, create a copy, remove any unnecessary elements from the file, and rename the file to `gateway-config-minimal.xml`. To ensure that the `gateway-config.xml` file is not used in production, rename the file (for example: `test-gateway-config.xml`).
-   For more information about setting up additional, and more advanced configurations, see the descriptions and examples of services for the Gateway configuration in the [Service Reference](../admin-reference/r_configure_gateway_service.md).
-   For additional information about the location of your `GATEWAY_HOME`, the configuration files and starting the Gateway, see [Setting Up the Gateway](../about/setup-guide.md).

About KAAZING Gateway Configuration File Elements and Properties
------------------------------------------------------------------------------------------------------

KAAZING Gateway configuration file (`gateway-config.xml` or `gateway-config.xml`) can include a number of configuration elements and properties contained within the top-level `gateway-config` element.

At a minimum, the Gateway configuration file must contain the following components (which are included in the default configuration files):

-   The Gateway namespace declaration, as described in [About KAAZING Gateway Namespace Declarations](#about-kaazing-gateway-namespace-declarations)
-   The Gateway [`name`](../admin-reference/r_configure_gateway_service.md#service) element.
-   The [`service`](../admin-reference/r_configure_gateway_service.md) element with the [`directory`](r_configure_gateway_service.md#directory) type to specify the path of your static files relative to `GATEWAY_HOME/web`, where *GATEWAY\_HOME* is the directory where you installed KAAZING Gateway.

For example, the following is an example of a minimally configured Gateway configuration file:

``` xml
<?xml version="1.0" encoding="UTF-8" ?>

<gateway-config
    xmlns="http://xmlns.kaazing.com/2014/09/gateway">
    ...
    <properties>
        <property>
            <name>gateway.hostname</name>
            <value>localhost</value>
        </property>
        <property>
            <name>gateway.base.port</name>
            <value>8000</value>
        </property>
    </properties>
    ...
    <service>
        <name>Base Directory Service</name>
        <accept>http://${gateway.hostname}:${gateway.base.port}/</accept>

        <type>directory</type>
        <properties>
            <directory>/base</directory>
            <welcome-file>index.md</welcome-file>
            <error-pages-directory>/error-pages</error-pages-directory>
            <options>indexes</options>
        </properties>
    </service>
    ...
</gateway-config>
```

To customize your configuration, see the [Configure KAAZING Gateway](p_configure_gateway_files.md) topic for a step-by-step procedure to configure the Gateway. Also, you can find a complete list of elements in the [Configuration Element Index.](r_configure_gateway_element_index.md)

About KAAZING Gateway Namespace Declarations
-------------------------------------------------------------------------------

Your KAAZING Gateway configuration file must contain a namespace declaration. Each release of the Gateway has a unique namespace declaration. The namespace is defined by the `xmlns` attribute in the [gateway-config](r_configure_gateway_gwconfig.md) element and has the syntax: `xmlns=**"URI"**` as shown in the following examples.

#### Example: gateway-config.xml file

The namespace for `gateway-config.xml` is shown in line 1:

``` xml
<gateway-config xmlns="http://xmlns.kaazing.com/2014/09/gateway">
  ...
</gateway-config>
```

#### Example: jaas-config.xml file

The namespace for `jaas-config.xml` is shown in line 1:

``` xml
<jaas-config xmlns="http://xmlns.kaazing.com/jaas-config/centurion">
  ...
</jaas-config>
```

When the Gateway starts, it uses the namespace declaration (which corresponds to a particular XML schema definition) to determine how to parse the XML file. Thus, when you start the Gateway:

-   If you use the default `gateway-config.xml` or `gateway-config-minimal.xml` configuration file, and the `jaas-config.xml` configuration file that are installed in `GATEWAY_HOME/conf/`, then you can start the Gateway without making any changes to the namespace declaration. This is because the default configuration files that are installed with the Gateway explicitly set the namespace declaration to the most current namespace.
-   If you create your own configuration files, then you must ensure that the files contain a namespace declaration that is appropriate for your Gateway release before you start the Gateway. See [About KAAZING Gateway Namespace Declarations](c_configure_gateway_concepts.md#about-kaazing-gateway-namespace-declarations) to ensure the elements used in your configuration file match what is supported by the namespace specified in the configuration file.

**Notes:**
Configuration files that contain a wrong or an out-of-date namespace declaration can result in one of the following scenarios when the Gateway starts:

-   If the Gateway configuration file contains an out-of-date namespace declaration, then the Gateway automatically upgrades the configuration in memory and writes the modified configuration file to disk and appends a '.new' extension (for example, `gateway-config.xml.new`). The Gateway starts using the up-to-date set of in-memory configuration objects.

    If you run the Gateway again with the same out-of-date namespace, then the translation of the configuration file occurs again (the modified configuration `.new` file is written again, and so on). You can avoid going through the translation process again if, once a `.new` file is created, you delete the current (old) configuration file and rename the `.new` configuration file (for example, rename `gateway-config.xml.new` to be `gateway-config.xml`) so that the Gateway uses the up-to-date configuration the next time it starts.

-   If the Gateway configuration file contains a completely wrong namespace declaration, then the Gateway returns an error and stops. For example: `ERROR Error upgrading XML: Unknown/unsupported XML namespace URI 'http://xmlns.kaazing.com/2011/zzz/gateway'`.
-   If the Gateway configuration file contains a very old namespace declaration, then the Gateway returns an error and stops. For example: `ERROR Gateway config file 'gateway-config.xml' from prior release in use`. The Gateway writes the configuration file to disk and appends a ".migrated" extension (for example, `gateway-config.xml.migrated`.

See Also
--------

-   [Configuration Skeleton](r_configure_gateway_element_skeleton.md)
-   [Service Reference](../admin-reference/r_configure_gateway_service.md#service), and in particular, see the service [`type`](../admin-reference/r_configure_gateway_service.md#type) element

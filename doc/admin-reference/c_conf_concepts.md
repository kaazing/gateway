-   [Home](../../index.md)
-   [Documentation](../index.md)
-   About ${gateway.cap} Configuration

About ${gateway.cap} Configuration ${enterprise.logo.jms}
=========================================================

After you install ${the.gateway}, you can configure it by modifying the settings in the configuration file `GATEWAY_HOME/conf/gateway-config.xml` file. The actual location of the `GATEWAY_HOME` directory depends on your operating system and the method (standalone or installer) used to install ${the.gateway}. You can find more information about `GATEWAY_HOME` and the directory structure that is set up during installation in ${setting.up.inline}. [About ${gateway.cap} Configuration](c_conf_concepts.md) describes the types of configuration files and [Configuration Element Index](r_conf_elementindex.md) provides a list of the individual configuration elements.

By default, ${the.gateway} uses the values in ${the.gateway} configuration file when you start ${the.gateway}. Optionally, you can override one or more Gateway configuration settings by specifying the `GATEWAY_OPTS` environment variable before you start ${the.gateway}. This method is described in [Using the GATEWAY\_OPTS Environment Variable](p_conf_gw_opts.md).

This topic covers the following information:

-   [About ${gateway.name.short} Configuration Files](#aboutconffiles)
-   [About ${gateway.name.short} Configuration File Elements and Properties](#aboutconfelements)
-   [About ${gateway.name.short} Namespace Declarations](#aboutnamespace)

<a name="aboutconffiles"></a>About ${gateway.name.short} Configuration Files
----------------------------------------------------------------------------

${the.gateway.cap} provides two configuration file options:

-   `gateway-config.xml` contains a complete set of gateway properties, including the properties and services needed to run ${the.gateway} documentation and out of the box demos. ${begin.comment}

    If you installed the **Gateway + Documentation + Demos** product, then both the `gateway-config.xml` and `gateway-config-minimal.xml` files are included in your `GATEWAY_HOME/conf` directory.

    ${end.comment}

-   `gateway-config-minimal.xml` (recommended for production) contains the minimal set of properties necessary to run ${the.gateway}. The recommended practice for configuring ${the.gateway} for production purposes is to edit the `gateway-config-minimal.xml` file and configure only the properties necessary to customize ${the.gateway} for your specific needs. ${begin.comment}

    If you installed the **Gateway Only** product, then your `GATEWAY_HOME/conf` directory includes only the `gateway-config-minimal.xml` file.

    ${end.comment}

${begin.comment}
When you start ${the.gateway}:

-   If you installed the **Gateway Only** product, then ${the.gateway} uses the properties in the `gateway-config-minimal.xml` file when it is started.
-   If you installed the **Gateway + Documentation + Demos** product, then ${the.gateway} uses the properties in the `gateway-config.xml` file, by default, when it is started. If there is no `gateway-config.xml` file, then ${the.gateway} is started using `gateway-config-minimal.xml`.

${end.comment}
Notes
-----

-   By default, ${the.gateway} configuration accepts connections on localhost, and the cross-origin sites allowed to access those services are also configured for localhost by default. The default configuration is convenient for quickly trying ${the.gateway} out of the box, but the default configuration is not production ready nor is it secure. You should customize ${the.gateway} configuration for your needs by modifying settings in the `GATEWAY_HOME/conf/gateway-config.xml` file. For example, you might customize ${the.gateway} to accept connections on a non-localhost host name or IP address and add authorization constraints for services. After you modify the `gateway-config.xml` file, you must restart ${the.gateway} for the changes to take effect.
-   Consider using the `gateway-config.xml` file during your development and testing phase. Then, when you are ready to deploy ${the.gateway} to your production environment, create a copy, remove any unnecessary elements from the file, and rename the file to `gateway-config-minimal.xml`. To ensure that the `gateway-config.xml` file is not used in production, rename the file (for example: `test-gateway-config.xml`).
-   For more information about setting up additional, and more advanced configurations, see the descriptions and examples of services for ${the.gateway} configuration in the [Service Reference](../admin-reference/r_conf_service.md).
-   For additional information about the location of your `GATEWAY_HOME`, the configuration files and starting ${the.gateway}, see ${setting.up.inline}.

<a name="aboutconfelements"></a>About ${gateway.name.short} Configuration File Elements and Properties
------------------------------------------------------------------------------------------------------

${gateway.name.short} configuration file (`gateway-config.xml` or `gateway-config.xml`) can include a number of configuration elements and properties contained within the top-level `gateway-config` element.

At a minimum, ${the.gateway} configuration file must contain the following components (which are included in the default configuration files):

-   ${the.gateway.cap} namespace declaration, as described in [About ${gateway.name.short} Namespace Declarations](#aboutnamespace)
-   The `service` element with the [directory](r_conf_service.md#directory) tag to specify the path of your static files relative to `GATEWAY_HOME/web`, where *GATEWAY\_HOME* is the directory where you installed ${gateway.name.short}.

For example, the following is an example of a minimally configured ${gateway.cap} configuration file:

``` auto-links:
<?xml version="1.0" encoding="UTF-8" ?>

<gateway-config
    xmlns="http://xmlns.kaazing.com/2014/09/gateway">
    .
    .
    .
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
    .
    .
    .
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
    .
    .
    .
</gateway-config>
```

To customize your configuration, see the [Configure ${gateway.name.long}](p_conf_files.md) topic for a step-by-step procedure to configure ${the.gateway}. Also, you can find a complete list of elements in the [Configuration Element Index.](r_conf_elementindex.md)

<a name="aboutnamespace"></a>About ${gateway.name.short} Namespace Declarations
-------------------------------------------------------------------------------

Your ${gateway.name.short} configuration file must contain a namespace declaration. Each release of ${the.gateway} has a unique namespace declaration. The namespace is defined by the `xmlns` attribute in the [gateway-config](r_conf_gwconfig.md) element and has the syntax: <span class="code">xmlns=</span>*<span class="code">*"URI"*</span>* as shown in the following examples.

#### Example: gateway-config.xml file

The namespace for `gateway-config.xml` is shown in line 1:

``` auto-links:
<gateway-config xmlns="http://xmlns.kaazing.com/2014/09/gateway">
  .
  .
  .
</gateway-config>
  
```

#### Example: jaas-config.xml file

The namespace for `jaas-config.xml` is shown in line 1:

``` auto-links:
<jaas-config xmlns="http://xmlns.kaazing.com/jaas-config/centurion">
  .
  .
  .
</jaas-config>
```

When ${the.gateway} starts, it uses the namespace declaration (which corresponds to a particular XML schema definition) to determine how to parse the XML file. Thus, when you start ${the.gateway}:

-   If you use the default `gateway-config.xml` or `gateway-config-minimal.xml` configuration file, and the `jaas-config.xml` configuration file that are installed in `GATEWAY_HOME/conf/`, then you can start ${the.gateway} without making any changes to the namespace declaration. This is because the default configuration files that are installed with ${the.gateway} explicitly set the namespace declaration to the most current namespace.
-   If you create your own configuration files, then you must ensure that the files contain a namespace declaration that is appropriate for your ${gateway.cap} release before you start ${the.gateway}. See [About ${gateway.name.short} Namespace Declarations](c_conf_concepts.md#aboutnamespace) to ensure the elements used in your configuration file match what is supported by the namespace specified in the configuration file.

**Notes:** 
Configuration files that contain a wrong or an out-of-date namespace declaration can result in one of the following scenarios when ${the.gateway} starts:

-   If ${the.gateway} configuration file contains an out-of-date namespace declaration, then ${the.gateway} automatically upgrades the configuration in memory and writes the modified configuration file to disk and appends a '.new' extension (for example, `gateway-config.xml.new`). ${the.gateway.cap} starts using the up-to-date set of in-memory configuration objects.

    If you run ${the.gateway} again with the same out-of-date namespace, then the translation of the configuration file occurs again (the modified configuration` .new` file is written again, and so on). You can avoid going through the translation process again if, once a `.new` file is created, you delete the current (old) configuration file and rename the `.new` configuration file (for example, rename `gateway-config.xml.new` to be `gateway-config.xml`) so that ${the.gateway} uses the up-to-date configuration the next time it starts.

-   If ${the.gateway} configuration file contains a completely wrong namespace declaration, then ${the.gateway} returns an error and stops. For example: `ERROR Error upgrading XML: Unknown/unsupported XML namespace URI 'http://xmlns.kaazing.com/2011/zzz/gateway'`.
-   If ${the.gateway} configuration file contains a very old namespace declaration, then ${the.gateway} returns an error and stops. For example: `ERROR Gateway config file 'gateway-config.xml' from prior release in use`. ${the.gateway.cap} writes the configuration file to disk and appends a ".migrated" extension (for example, `gateway-config.xml.migrated`). 

</span>

</div>


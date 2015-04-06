-   [Home](../../index.md)
-   [Documentation](../index.md)
-   Configure ${gateway.name.short}

Configure ${gateway.name.short} ${enterprise.logo.jms}
======================================================

By default, ${the.gateway} uses the values in the `gateway-config.xml` and `gateway-config-minimal.xml` files that are located in the `GATEWAY_HOME/conf/` directory when you start ${the.gateway}. Although you do not need to update the configuration file to run ${the.gateway} on your local host, you must make some changes to provide services on non localhost machines.

Before You Begin
----------------

This procedure is part of [Configure ${gateway.name.short}](o_conf_checklist.md):

-   **Configure ${gateway.name.short}**
-   [Configure ${gateway.name.short} Using the `GATEWAY_OPTS` Environment Variables](p_conf_gw_opts.md)
-   Verify ${the.gateway} configuration following the instructions for your deployment in ${setting.up.inline}

To Configure ${the.gateway}
---------------------------

The standard way to set up and maintain your ${gateway.cap} configuration is by editing the settings in ${the.gateway} configuration file in the `GATEWAY_HOME/conf/` directory. The following steps describe how to modify one of the default configuration files `gateway-config.xml` and `gateway-config-minimal.xml,`, or you can create and edit your own configuration file.
1.  Before you configure ${the.gateway}, ensure you have followed the steps in ${setting.up.inline} to download and install ${gateway.name.short}.
2.  Configure ${the.gateway} using one of the following configuration files:
    -   Modify the settings in the configuration file `GATEWAY_HOME/conf/gateway-config.xml` file.

        The `gateway-config.xml` contains a complete set of ${the.gateway} properties, including the properties and services needed to run ${the.gateway} documentation and out of the box demos.

    -   Create a customized Gateway configuration by adding specific elements to the configuration file `GATEWAY_HOME/conf/gateway-config-minimal.xml` file.

        The `gateway-config-minimal.xml` (recommended) contains the minimal set of properties necessary to run ${the.gateway}.

    -   Create and modify your own custom copy of a configuration file.

3.  At a minimum, ${the.gateway} configuration file must contain the following components (which are included in the default configuration files):

    -   ${the.gateway.cap} namespace declaration, as described in [About ${gateway.name.short} Namespace Declarations](c_conf_concepts.md#aboutnamespace).
    -   The `service` element with the [directory](r_conf_service.md#directory) type to specify the path of your static files relative to `GATEWAY_HOME/web`, where *GATEWAY\_HOME* is the directory where you installed ${gateway.name.short}.

    Here's an example of a simple ${gateway.cap} configuration file that uses the default (supplied) ports to bind the `/base` (port 8000) to the ${gateway.cap} host:

    ``` brush:
    <?xml version="1.0" encoding="UTF-8" ?>
    <gateway-config xmlns="http://xmlns.kaazing.com/2014/09/gateway">

    <service>
      <accept>http://${gateway.hostname}:${gateway.base.port}/</accept>

      <type>directory</type>
      <properties>
        <directory>/base</directory>
        <welcome-file>index.md</welcome-file>
        <error-pages-directory>/error-pages</error-pages-directory>
        <options>indexes</options>
      </properties>
    </service> 

    </gateway-config>
    ```

4.  Customize your ${gateway.cap} configuration, as necessary.

    The recommended practice for configuring ${the.gateway} for production purposes is to edit ${the.gateway} configuration file and configure only the properties necessary to customize ${the.gateway} for your specific needs. Use the following guidelines to help you customize ${the.gateway} configuration to meet your needs:

    -   Use the `gateway-config.xml` file while developing and testing your configuration. This file can serve as a template for building your custom configuration file because it contains the services and configuration elements and provides detailed comments describing how and why to use them.
    -   Assess what other services you might want to configure, or if you need to configure security, clustering and load balancing, internal network access, and so on.

        The `gateway-config.xml`configuration file is composed of several sections as described in the following table.

        | Section             | Description                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        |
        |---------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
        | Property defaults   | Specify default values for configuration elements. Configuring property defaults is optional but recommended because configuring the property defaults allows you to define some property values once and have the value propagated throughout the configuration when ${the.gateway} starts. You can replace any value in the configuration file with a property using the dollar-sign and curly brace format (such as `${gateway.hostname}`).                                                                                     |
        | Service             | Define how ${the.gateway} manages communication for that service. ${the.gateway.cap} is configured by default to provide services only to users on the same machine (localhost) as that on which it is running. You can define one or more services to customize ${the.gateway} for your environment, choosing the appropriate type of service (for example, balancer, broadcast, ${proxy.service.inline} service, and so on) to signal ${the.gateway} to accept incoming connections from clients using any supported URL scheme. |
        | Service Defaults    | Configure default values that apply to all services. Note that any options you set at the service level overrides options you set at the service defaults level. You can configure SSL encryption, protocol bindings, WebSocket message size, keep-alive timeouts, mime-type messages, and more.                                                                                                                                                                                                                                   |
        | Security            | Configure security for the service and specify authentication and authorization for users. To better understand how the security parameters that you specify in ${gateway.name.short} configuration work together, see [What's Involved in Secure Communication](../security/u_sec_client_gw_comm.md).                                                                                                                                                                                                                           |
        | Management          | Use the management section to configure a management agent, such as Java's built-in Java Management and Monitoring Console (JConsole), to monitor, track and manage user sessions. For secure management, you can specify the protocol, network interface, and the port number on which the management agent accepts connections, and you can define the user roles that are authorized to perform management operations.                                                                                                          |
        | Additional Services | UDP broadcast service, ${proxy.service.inline} service, session service, multicast addressing, network address mappings, and more.                                                                                                                                                                                                                                                                                                                                                                                                 |

    -   Consider configuring multiple services on ${the.gateway} to use the same hostname and port, for example, as a way to organize multiple connection requests on the same server as the Gateway and avoid conflicts. See [Configuring Multiple Services on the Same Host and Port](c_conf_multipleservices.md) for complete information.

5.  Test ${the.gateway} configuration using the customized configuration file. For detailed instructions about starting and stopping ${the.gateway}, see "How do I start and stop ${the.gateway}?" in ${setting.up.inline}.
6.  Prepare the configuration file for production.

    When you are ready to deploy ${the.gateway} to your production environment, create a copy of your configuration file, remove any unnecessary elements from the file, and rename the file to `gateway-config-minimal.xml`. By default, if there is no `gateway-config.xml` file in the *GATEWAY\_HOME* directory when ${the.gateway} is started, then ${the.gateway} is started using `gateway-config-minimal.xml`.

    To ensure that the `gateway-config.xml` file is not used in production, rename the file (for example, rename the file to `test-gateway-config.xml`).

<a name="_"></a>Next Step
-------------------------

[Configure ${gateway.name.short} Using the `GATEWAY_OPTS` Environment Variables](p_conf_gw_opts.md) is optional. It is an alternative method for configuration ${the.gateway}.

Notes
-----

-   The actual location of the `GATEWAY_HOME` directory depends on your operating system and the method (standalone or installer) used to install ${the.gateway}. To learn more about `GATEWAY_HOME`, see:
    -   ${setting.up.inline} for information about the directory structure that is set up during installation
    -   [About ${gateway.cap} Configuration](c_conf_concepts.md) for more information about the types of configuration files and their contents
-   Optionally, you can override one or more ${gateway.cap} configuration settings by specifying the `GATEWAY_OPTS` environment variable before you start ${the.gateway}. This is described in [Configure ${the.gateway} Using the `GATEWAY_OPTS` Environment Variable](p_conf_gw_opts.md).
-   See [Troubleshoot ${gateway.name.long} Configuration and Startup](../troubleshooting/ts_config.md) for help resolving issues when you set up and configure ${the.gateway}.

See Also
--------

-   [Configuration Element Index](r_conf_elementindex.md)
-   [About ${gateway.name.short} Configuration](c_conf_concepts.md)

</div>


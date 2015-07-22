Configure KAAZING Gateway
======================================================

By default, the Gateway uses the values in the `gateway-config.xml` and `gateway-config-minimal.xml` files that are located in the `GATEWAY_HOME/conf/` directory when you start the Gateway. Although you do not need to update the configuration file to run the Gateway on your local host, you must make some changes to provide services on non localhost machines.

Before You Begin
----------------

This procedure is part of [Configure the Gateway](o_configure_gateway_checklist.md):

-   **Configure KAAZING Gateway**
-   [Configure KAAZING Gateway Using the `GATEWAY_OPTS` Environment Variables](p_configure_gateway_opts.md)
-   Verify the Gateway configuration following the instructions for your deployment in [Setting Up the Gateway](../about/setup-guide.md)

To Configure the Gateway
---------------------------

The standard way to set up and maintain your Gateway configuration is by editing the settings in the Gateway configuration file in the `GATEWAY_HOME/conf/` directory. The following steps describe how to modify one of the default configuration files `gateway-config.xml` and `gateway-config-minimal.xml,`, or you can create and edit your own configuration file.

1.  Before you configure the Gateway, ensure you have followed the steps in [Setting Up the Gateway](../about/setup-guide.md) to download and install KAAZING Gateway.
2.  Configure the Gateway using one of the following configuration files:
  -   Modify the settings in the configuration file `GATEWAY_HOME/conf/gateway-config.xml` file.
        The `gateway-config.xml` contains a complete set of the Gateway properties, including the properties and services needed to run the Gateway documentation and out of the box demos.

    -   Create a customized Gateway configuration by adding specific elements to the configuration file `GATEWAY_HOME/conf/gateway-config-minimal.xml` file.

        The `gateway-config-minimal.xml` (recommended) contains the minimal set of properties necessary to run the Gateway.

    -   Create and modify your own custom copy of a configuration file.

      **Note:** Consider using the `gateway-config.xml` file during your development and testing phase. Then, when you are ready to deploy the Gateway to your production environment, create a copy, remove any unnecessary elements from the file, and rename the file to `gateway-config-minimal.xml`. To ensure that the `gateway-config.xml` file is not used in production, rename the file (for example: `test-gateway-config.xml`).

3.  At a minimum, the Gateway configuration file must contain the following components (which are included in the default configuration files):

    -   The Gateway namespace declaration, as described in [About KAAZING Gateway Namespace Declarations](c_configure_gateway_concepts.md#about-kaazing-gateway-namespace-declarations).
    -   The Gateway [`name`](../admin-reference/r_configure_gateway_service.md#service) element.
    -   The [`service`](../admin-reference/r_configure_gateway_service.md) element with the [`directory`](r_configure_gateway_service.md#directory) type to specify the path of your static files relative to `GATEWAY_HOME/web`, where *GATEWAY\_HOME* is the directory where you installed KAAZING Gateway.

    Here's an example of a simple Gateway configuration file that uses the default (supplied) ports to bind the `/base` (port 8000) to the Gateway host:

    ``` xml
    <?xml version="1.0" encoding="UTF-8" ?>
    <gateway-config xmlns="http://xmlns.kaazing.com/2014/09/gateway">

    <service>
      <name>Directory Service</name>
      <description>Simple directory service</description></description>
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

4.  Customize your Gateway configuration, as necessary.

    The recommended practice for configuring the Gateway for production purposes is to edit the Gateway configuration file and configure only the properties necessary to customize the Gateway for your specific needs. Use the following guidelines to help you customize the Gateway configuration to meet your needs:

    -   Use the `gateway-config.xml` file while developing and testing your configuration. This file can serve as a template for building your custom configuration file because it contains the services and configuration elements and provides detailed comments describing how and why to use them.
    -   Assess what other services you might want to configure, or if you need to configure security, clustering and load balancing, internal network access, and so on.

        The `gateway-config.xml`configuration file is composed of several sections as described in the following table.

        | Section             | Description                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        |
        |---------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
        | [Property defaults](../about/about.md#text-conventions)  | Specify default values for configuration elements. Configuring property defaults is optional but recommended because configuring the property defaults allows you to define some property values once and have the value propagated throughout the configuration when the Gateway starts. You can replace any value in the configuration file with a property using the dollar-sign and curly brace format (such as `${gateway.hostname}`) See [Service Reference][Service](../admin-reference/r_configure_gateway_service.md) for more configuration examples with the `properties` element and see  [Documentation Conventions](../about/about.md) for more information about using variables.                                                                                     |
        | [Service](../admin-reference/r_configure_gateway_service.md#service) | Define how the Gateway manages communication for that service. The Gateway is configured by default to provide services only to users on the same machine (localhost) as that on which it is running. You can define one or more services to customize the Gateway for your environment, choosing the appropriate [type](../admin-reference/r_configure_gateway_service.md#type) of service (for example,`balancer`, `broadcast`, `proxy`, `jms`, `jms.proxy`, `http.proxy`, `redis`, and so on) to signal the Gateway to accept incoming connections from clients using any supported URL scheme.  The Service section is also where you configure a management agent, such as management.snmp for Command Center or management.jmx for Java's built-in Java Management and Monitoring Console (JConsole), to monitor, track and manage user sessions.|
        | [Service Defaults](../admin-reference/r_configure_gateway_service_defaults.md)| Configure default values that apply to all services. Note that any options you set at the service level overrides options you set at the service defaults level. You can configure SSL encryption, protocol bindings, WebSocket message size, keep-alive timeouts, mime-type messages, and more.|
        | [Security](../admin-reference/r_configure_gateway_security.md)  | Configure security for the service and specify authentication and authorization for users. To better understand how the security parameters that you specify in KAAZING Gateway configuration work together, see [What's Involved in Secure Communication](../security/u_secure_client_gateway_communication.md).|
        | [Cluster](../admin-reference/r_configure_gateway_cluster.md) | Use the `cluster` section to configure one or more Gateway's to participate in a KAAZING Gateway cluster, and describe the elements and properties you can configure to achieve high availability. |
        | Additional Services | [Configure the Gateway to Use Multicast](p_configure_multicast.md), [Configure the Gateway on an Internal Network](../internal-network/p_protocol_binding.md), [Implement Protocol Injection](../security/p_auth_protocol_injection.md), [Implement User Identity Promotion](../security/p_auth_user_identity_promotion.md), and more can be found on the [KAAZING Documentation](../index.md) page.  |           
        
    -   Consider configuring multiple services on the Gateway to use the same hostname and port, for example, as a way to organize multiple connection requests on the same server as the Gateway and avoid conflicts. See [Configuring Multiple Services on the Same Host and Port](c_configure_gateway_multiple_services.md) for complete information.

5.  Test the Gateway configuration using the customized configuration file. For detailed instructions about starting and stopping the Gateway, see "How do I start and stop the Gateway?" in [Setting Up the Gateway](../about/setup-guide.md).
6.  Prepare the configuration file for production.

    When you are ready to deploy the Gateway to your production environment, create a copy of your configuration file, remove any unnecessary elements from the file, and rename the file to `gateway-config-minimal.xml`. By default, if there is no `gateway-config.xml` file in the *GATEWAY\_HOME* directory when the Gateway is started, then the Gateway is started using `gateway-config-minimal.xml`.

    To ensure that the `gateway-config.xml` file is not used in production, rename the file (for example, rename the file to `test-gateway-config.xml`).

Next Step
-------------------------

[Configure KAAZING Gateway Using the `GATEWAY_OPTS` Environment Variables](p_configure_gateway_opts.md) is optional. It is an alternative method for configuration the Gateway.

Notes
-----

-   The actual location of the `GATEWAY_HOME` directory depends on your operating system and the method (standalone or installer) used to install the Gateway. To learn more about `GATEWAY_HOME`, see:
    -   [Setting Up the Gateway](../about/setup-guide.md) for information about the directory structure that is set up during installation
    -   [About Gateway Configuration](c_configure_gateway_concepts.md) for more information about the types of configuration files and their contents
-   Optionally, you can override one or more Gateway configuration settings by specifying the `GATEWAY_OPTS` environment variable before you start the Gateway. This is described in [Configure KAAZING Gateway Using the `GATEWAY_OPTS` Environment Variable](p_configure_gateway_opts.md).
-   See [Troubleshoot KAAZING Gateway Configuration and Startup](../troubleshooting/p_troubleshoot_gateway_configuration.md) for help resolving issues when you set up and configure the Gateway.

See Also
--------

-   [Configuration Skeleton](r_configure_gateway_element_skeleton.md)
-   [About Gateway Configuration](c_configure_gateway_concepts.md)

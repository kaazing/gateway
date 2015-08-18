Monitor with Command Center
===================================================================================

You can monitor a Gateway or a Gateway cluster through Command Center, which is a browser-based application enabled in the default Gateway configuration. This procedure describes how to launch Command Center and begin monitoring a Gateway or a Gateway cluster.

**Note:** You can monitor a Gateway or Gateway cluster through Command Center as long as the Gateway where you launch Command Center uses the KAAZING Gateway load balancing technology. See [Configure the Gateway for High Availability](../high-availability/o_high_availability.md) to learn more about the Gateway's integrated clustering and load balancing features.

Before You Begin
----------------

This procedure is part of [Monitor the Gateway](o_monitor.md):

1.  [Introduction to Monitoring KAAZING Gateway](o_monitor.md#introduction-to-monitoring-kaazing-gateway)
2.  [Secure KAAZING Gateway Monitoring](p_monitor_configure_secure.md)
3.  Monitor a Gateway or Gateway cluster
    -   **Monitor with Command Center** (Recommended)
    -   [Monitor with JMX](p_monitor_jmx.md)

4.  [Troubleshoot the Gateway](../troubleshooting/o_troubleshoot.md)

To launch Command Center
--------------------------------------------------------

1.  If the Gateway is not already installed, then download the Gateway distribution from [kaazing.org](http://kaazing.org) and see [Setting Up KAAZING Gateway](../about/setup-guide.md) for help with Gateway set-up and configuration.
2.  Ensure secure monitoring by verifying that your configuration specifies a security realm name and an authorization constraint. This is set up automatically if you use the default Gateway configuration. See [Secure KAAZING Gateway Monitoring](p_monitor_configure_secure.md) for more information.
3.  Start KAAZING Gateway.
    -   To start the Gateway using the default Gateway configuration file supplied in `GATEWAY_HOME/conf/gateway.config.xml`, follow the instructions in [Setting Up the Gateway](../about/setup-guide.md).

        Command Center does not require any custom coding and it works immediately if you start the Gateway using the default Gateway configuration file. Once the Gateway starts, then skip to step 4.

    -   To start the Gateway using a custom Gateway configuration file requires that you copy these pieces from the default `GATEWAY_HOME/conf/gateway-config.xml` file:

        -   The `management.snmp` management service
        -   The directory service `commandcenter-directory`
        -   The `commandcenter` security realm

        See the [Service Reference](../admin-reference/r_configure_gateway_service.md) documentation for information and examples. For more information about default and custom Gateway configuration files, see [Configure KAAZING Gateway](../admin-reference/p_configure_gateway_files.md).

4.  Open a Web browser and enter one of the following URLs:
    -   To monitor a cluster, the default URL is: `http://localhost:8080/commandcenter`.
    -   For single Gateway monitoring, the default URL is: `http://localhost:8000/commandcenter`

    **Note:** On startup, Command Center verifies that your browser supports HTML5 and CSS3. If you are using an older browser that does not meet the requirements for Command Center, then you may receive an error message from Command Center. See the README.txt file in the *`GATEWAY_HOME`* directory where you unpacked the KAAZING Gateway distribution.

5.  Log into Command Center using Administrator credentials and provide the administrator's username and password (by default, `admin/admin`). If necessary, you can find the login credentials in `GATEWAY_HOME/conf/jaas-config.xml`.

    **Note:** If you are running the Gateway on an Amazon Web Services (AWS) Marketplace instance, the password is set to the public instance ID by default. You can replace the default password by configuring the `cloud.instanceid` parameter in the `GATEWAY_HOME/conf/jaas-config.xml` file. See [Configure KAAZING Gateway](../admin-reference/p_configure_gateway_files.md) for more information about the `cloud.instanceid` parameter.

    When you log into Command Center, you can enter a URL for any Gateway running the management service (including localhost) or take the default URL provided by Command Center:

    -   By default, Command Center uses the SNMP management URL to the Gateway that you accessed to start the Command Center itself. For example, if you launched Command Center using `http://localhost:8000/commandcenter`, then Command Center defaults to `ws://localhost:8000/snmp`. In this example, the `ws` scheme is the address specified in the `accept` configuration element for the `management.snmp` management service.
    -   You can override the default and enter a Management URL during the login dialog by entering the path to any Gateway to which you have network access.

Get Started with Command Center
------------------------------------------------------------

Command Center provides information about your cluster, its cluster members, and services and sessions through the following main pages:

-   [Dashboard Page](#dashboard-page)
-   [Configuration Page](#configuration-page)
-   [Overview Page](#overview-page-general-configuration-information)
-   [Monitoring Page](#monitoring-pages)

**Note:** In a Gateway cluster, it is recommended that all cluster members have identical configurations. However, if a cluster member’s configuration does not match the other cluster members, then the dissimilar member is quarantined and remains running and joined to the cluster but all of its services will be stopped (unbound), except for its management services. See [Configure the Gateway for High Availability](../high-availability/o_high_availability.md) for complete information about configuring clustering and load balancing.

The following sections provide a high-level description of the information you can view with Command Center.

### Dashboard Page

The Dashboard page provides an overview of the Gateway cluster by means of a user-selectable set of dynamic, scrolling charts The charts show current performance metrics (such as sessions, system information, I/O, JVM memory, and so on). Some of the charts you can view include:

-   CPU%
-   Current Sessions
-   JVM Heap
-   Total Read/Write Throughput
-   Read Throughput Combined
-   Write Throughput Combined
-   Write Throughput Per Interface Card

You can personalize the Dashboard by adding and removing charts to include only the charts you need. The menu at the top of the page allows you to dynamically add and remove any of the graphs to customize your view. Plus, you can drag and drop the charts to organize them in a way that makes sense to you. Command Center stores the chart order in the browser so that you get the same display the next time you log in.

In addition, the Dashboard helps to catch and diagnose cluster misconfigurations at a glance. Metrics just above the charts indicate the number of running cluster members and if any members are quarantined.

### Configuration Page

Command Center shows all of the Gateway configuration details, from a graphical overview of services and realms through all configuration settings in all Gateways in the cluster. Command Center helps you monitor all cluster members from a single view, and when viewing a cluster, the Configuration pages help you quickly find where cluster member configurations match and do not match.

All configuration settings in all Gateway cluster members are available through the various Configuration pages to help you determine if there are configuration differences between members. The Configuration pages provide a more detailed look at all of the configuration types and show the Gateway configuration elements that have been configured and their current settings. In short, anything that you have in your Gateway configuration file can be viewed on the Configuration pages, including:

-   Service configuration defaults such as supported [MIME types](../admin-reference/r_configure_gateway_service.md#mime-mapping) and [bind](../admin-reference/r_configure_gateway_service.md#protocolbind) information
-   Specific security information about the [realms](../admin-reference/r_configure_gateway_security.md#realm), [truststores](../admin-reference/r_configure_gateway_security.md#truststore), [keystores](../admin-reference/r_configure_gateway_security.md#keystore), and login details
-   Licensing information for a single Gateway and cluster licensing

The Configuration pages can be instrumental in diagnosing issues you may have seen on either the Overview or Dashboard pages. For example, if you have configured each cluster member identically but Command Center shows that two services in your cluster have the same name (for example, on the Command Center Overview page), then you can view the Configuration pages to find out why the services are different. If you discover that there is a configuration issue, then you can also look at all the service options available and decide how to correct the issue. See [Configure the Gateway for High Availability](../high-availability/o_high_availability.md) for complete information about configuring and using clusters.

### Overview Page (General Configuration Information)

Command Center presents a graphical display of all the services for a single Gateway or all Gateway members in a cluster. The Overview page is an option under the Configuration menu that presents a live graphical display of the available services and their security realms for your installation, whether you are viewing a single Gateway or an entire cluster.

As with the Configuration page, the overview information displays configuration objects that match or differ to help you find issues (such as with the service and realm definitions) in the configuration. Links embedded in the actual objects provide a direct path to other pages in the Command Center that have more detailed information.

Whether you are viewing a single Gateway or a cluster, the overview page facilitates navigation between service and realm views, and shows:

-   Details of services, realms, and other configuration settings such as properties, global defaults, security settings, and more
-   The [accept](../admin-reference/r_configure_gateway_service.md#accept) and [connect](../admin-reference/r_configure_gateway_service.md#connect) elements configured for your services
-   High level view of security and items in the keystore and truststore
-   License information
-   All public Gateway configuration settings

When viewing a cluster, the Overview page helps you quickly find where cluster member configurations match or do not match. The Overview page helps you determine if there are configuration differences and on which cluster members. For example, the gray box in the previous screenshot shows the demo realm twice, and, in a cluster, there should only be one demo realm. If the Overview page shows two realms with the same name, then you immediately know that there are differences between the cluster members. Other information on the Overview Page can help you determine where the differences occur. With this information, you know at a glance where the problem lies for fast evaluation to quickly fix differences. See [Configure the Gateway for High Availability](../high-availability/o_high_availability.md) for more information about configuring clusters.

### Monitoring Pages

Command Center provides live metrics about higher-level concepts than Dashboard. The monitoring pages indicate what is going on in the cluster right now, including details for individual services, sessions, and each Gateway overall.

In particular, you can view a number of session details or you can view all of the services that are running. The Monitoring pages also provide high-level totals about what is coming in and going out across the entire cluster.

The following list describes some of the tabular information you see in the Monitoring pages:

-   Service Monitoring
    -   Displays services as a “top-level” item.
    -   Sets a filter to show only certain service lines.
    -   Allows you to sort service data.
    -   No session information is displayed in the service monitoring pages.
-   Session Monitoring
    -   Allows you to search for sessions that meet certain criteria, within a certain scope, and displays basic information about the sessions that match.
    -   Shows sessions anywhere in the cluster and filter them to view only particular sessions.
    -   Allows you to set a filter to show only certain session data.
    -   Allows you to sort session data.
    -   Can stop a session.
    -   Shows the session type - WebSocket (native or emulated), TCP, and so on.
    -   Differentiates between inbound (accept) sessions and outbound (connect) sessions.
-   Security Configuration
    -   [realms](../admin-reference/r_configure_gateway_security.md#realm)
    -   [keystores](../admin-reference/r_configure_gateway_security.md#keystore)
    -   [truststores](../admin-reference/r_configure_gateway_security.md#truststore)
    -   License
-   Cluster Member Monitoring

    Gathers data from each cluster member that has a direct connection and displays the data in a single instance of Command Center. See the [Notes](#notes) for more information about configuring cluster member connections.

Next Step
---------

You are done getting started with Command Center monitoring. For more information about Gateway administration, see the [documentation](../index.md).

Notes
---------------------------------

-   Command Center connects to each cluster member using that Gateway's `management.snmp` service. If that service is not defined in the Gateway's configuration file (such as `gateway-config.xml`), then Command Center cannot connect to the cluster member. See the [Service Reference](../admin-reference/r_configure_gateway_service.md#service) documentation for help configuring the `management.snmp` service for Command Center.
-   In a Gateway cluster, if a cluster member’s configuration is different than the other members of the cluster, the dissimilar member is quarantined. Thus, a cluster member with no management section in the Gateway's configuration (such as `gateway-config.xml` file) will be quarantined. But because the member has no management section, Command Center cannot interact with that member. To troubleshoot common problems with startup, security, clusters, and clients, see [Troubleshoot the Gateway](../troubleshooting/o_troubleshoot.md).
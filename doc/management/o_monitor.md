Monitor the Gateway
============================================

This checklist provides the steps necessary to monitor KAAZING Gateway:

| \#  | Step                                                                        | Topic or Reference                                                                                         |
|-----|-----------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------|
| 1   | Learn about monitoring KAAZING Gateway.                                | [Introduction to Monitoring KAAZING Gateway](#introduction-to-monitoring-kaazing-gateway)                                           |
| 2   | Configure secure monitoring services.                                       | [Secure KAAZING Gateway Monitoring](p_monitor_configure_secure.md)                                    |
| 3   | Monitor a Gateway or Gateway cluster.                         | [Monitor with Command Center](p_monitor_cc.md) (**Recommended**) [Monitor with JMX](p_monitor_jmx.md) |
| 4   | Troubleshoot common problems with startup, security, clusters, and clients. | [Troubleshoot the Gateway](../troubleshooting/o_troubleshoot.md)                                                |

Introduction to Monitoring KAAZING Gateway
--------------------------------------------------------------------------

You can track and monitor sessions of KAAZING Gateway using Command Center or Java Management Extension (JMX). The Gateway provides this ability through the Gateway management services and both monitoring services are enabled automatically in the default Gateway configuration file. Thus, monitoring services are configured on the Gateway *out of the box* for Command Center and JMX. You can use either of these services to collect and view:

-   Statistics about performance
-   Resource usage
-   Configuration and property information
-   Notification events such as state changes

You can also terminate Gateway sessions using either tool.

A major benefit of monitoring with Command Center or JMX is the ability to view all members of a cluster in a single interface.

### Command Center

KAAZING Gateway provides monitoring through the Command Center graphical-user interface (GUI). This browser-based application is the recommended method for monitoring, identifying problems, and terminating sessions. Command Center does not require any custom coding and it works immediately if you start the Gateway using the default Gateway configuration file. If you use a custom Gateway configuration, then some extra steps are required (as described in [Monitor with Command Center](p_monitor_cc.md)) to enable and use Command Center.

Here’s a high-level overview of monitoring with Command Center:

-   Cluster monitoring - quickly find where cluster member configurations do not match
-   Service monitoring - monitor services across all Gateways in a cluster
-   Session monitoring - examine sessions; stop sessions
-   Configuration visualization - view the configuration, including cluster configuration and security configuration
-   View license data

See also:

-   [Service Reference](../admin-reference/r_configure_gateway_service.md) to configure the `management.snmp` service for Command Center.
-   [Monitor with Command Center](p_monitor_cc.md) for detailed information about monitoring a Gateway or a Gateway cluster.

### JMX

KAAZING Gateway supports monitoring user sessions and configuration data using JMX Managed Beans. Although Command Center is the recommended method for monitoring single and clustered Gateways, you can also use the `management.jmx` service that is available by default with the Gateway.

You can use JMX through any JMX-compliant console such as Java's built-in Java Management and Monitoring Console (JConsole) or MC4J, or through any program that supports communication using the JMX protocol. This documentation uses Java's built-in Java Management and Monitoring Console (JConsole) in its examples. With JMX, you can program a management console using a JMX API to:

-   Examine available Gateway sessions
-   Configure notifications
-   Terminate sessions on the Gateway
-   Configuration visualization - view the configuration, including cluster configuration and security configuration
-   View license data

See also:

-   [Service Reference](../admin-reference/r_configure_gateway_service.md) to configure the `management.jmx` service in the Gateway configuration.
-   [Monitor with JMX](p_monitor_jmx.md) for detailed information about monitoring with JMX.

### Secure Gateway Monitoring

For secure monitoring, the Gateway provides a way to specify a security realm and authorization constraint on which each management service accepts connections. See [Secure KAAZING Gateway Monitoring](p_monitor_configure_secure.md) for detailed information and an example configuration.

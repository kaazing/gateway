-   [Home](../../index.md)
-   [Documentation](../index.md)
-   Monitor ${the.gateway}

Monitor ${the.gateway}${enterprise.logo.jms}
============================================

This checklist provides the steps necessary to monitor ${gateway.name.short}:

| \#  | Step                                                                        | Topic or Reference                                                                                         |
|-----|-----------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------|
| 1   | Learn about monitoring ${gateway.name.long}.                                | [Introduction to Monitoring ${gateway.name.long}](#intromonitor)                                           |
| 2   | Configure secure monitoring services.                                       | [Secure ${gateway.name.long} Monitoring](p_mgt_config_secure_mgmt.md)                                    |
| 3   | Monitor a ${gateway.cap} or ${gateway.cap} cluster.                         | [Monitor with ${console.name}](p_monitor_cc.md) (**Recommended**) [Monitor with JMX](p_monitor_jmx.md) |
| 4   | Troubleshoot common problems with startup, security, clusters, and clients. | [Troubleshoot ${the.gateway}](../troubleshooting/o_ts.md)                                                |

<a name="intromonitor"></a>Introduction to Monitoring ${gateway.name.long}
--------------------------------------------------------------------------

You can track and monitor sessions of ${gateway.name.long} using ${console.name} or Java Management Extension (JMX). ${the.gateway.cap} provides this ability through ${the.gateway} management services and both monitoring services are enabled automatically in the default ${gateway.cap} configuration file. Thus, monitoring services are configured on ${the.gateway} *out of the box* for ${console.name} and JMX. You can use either of these services to collect and view:

-   Statistics about performance
-   Resource usage
-   Configuration and property information
-   Notification events such as state changes

You can also terminate ${gateway.cap} sessions using either tool.

A major benefit of monitoring with ${console.name} or JMX is the ability to view all members of a cluster in a single interface.

### <a name="introcc"></a>${console.name}

${gateway.name.long} provides monitoring through the ${console.name} graphical-user interface (GUI). This browser-based application is the recommended method for monitoring, identifying problems, and terminating sessions. ${console.name} does not require any custom coding and it works immediately if you start ${the.gateway} using the default ${gateway.cap} configuration file. If you use a custom ${gateway.cap} configuration, then some extra steps are required (as described in [Monitor with ${console.name}](p_monitor_cc.md)) to enable and use ${console.name}.

Here’s a high-level overview of monitoring with ${console.name}:

-   Cluster monitoring - quickly find where cluster member configurations do not match
-   Service monitoring - monitor services across all ${gateway.cap}s in a cluster
-   Session monitoring - examine sessions; stop sessions
-   Configuration visualization - view the configuration, including cluster configuration and security configuration
-   View license data

See also:

-   [Service Reference](../admin-reference/r_conf_service.md#service) to configure the `management.snmp` service for ${console.name}.
-   [Monitor with ${console.name}](p_monitor_cc.md) for detailed information about monitoring a ${gateway.cap} or a ${gateway.cap} cluster.

### <a name="introjmx"></a>JMX

${gateway.name.long} supports monitoring user sessions and configuration data using JMX Managed Beans. Although ${console.name} is the recommended method for monitoring single and clustered ${gateway.cap}s, you can also use the `management.jmx` service that is available by default with ${the.gateway}.

You can use JMX through any JMX-compliant console such as Java's built-in Java Management and Monitoring Console (JConsole) or MC4J, or through any program that supports communication using the JMX protocol. This documentation uses Java's built-in Java Management and Monitoring Console (JConsole) in its examples. With JMX, you can program a management console using a JMX API to:

-   Examine available ${gateway.cap} sessions
-   Configure notifications
-   Terminate sessions on ${the.gateway}
-   Configuration visualization - view the configuration, including cluster configuration and security configuration
-   View license data

See also:

-   [Service Reference](../admin-reference/r_conf_service.md#service) to configure the `management.jmx` service in ${the.gateway} configuration.
-   [Monitor with JMX](p_monitor_jmx.md) for detailed information about monitoring with JMX.

### <a name="introsecuremonitoring"></a>Secure ${gateway.cap} Monitoring

For secure monitoring, ${the.gateway} provides a way to specify a security realm and authorization constraint on which each management service accepts connections. See [Secure ${gateway.name.long} Monitoring](p_mgt_config_secure_mgmt.md) for detailed information and an example configuration.


